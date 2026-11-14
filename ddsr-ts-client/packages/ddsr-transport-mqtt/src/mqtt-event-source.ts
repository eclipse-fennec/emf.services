/********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Data In Motion Consulting - initial implementation
 ********************************************************************/

import { randomUUID } from 'node:crypto';
import { tlsConnectOptions } from './mqtt-rpc.js';
import { connectAmqp, isAmqpUrl } from './amqp-client.js';
import type { AmqpConnectOptions } from './amqp-client.js';
import type { TlsOptions } from '@ddsr/client';
import type { ServiceEvent } from '@ddsr/model';
import type { DdsrEventSource, EventSourceHandler, EventSubscription } from '@ddsr/client';
import { readBroadcastEvent } from '@ddsr/client';

/**
 * The slice of the mqtt.js client this source needs — injectable for
 * tests (mirror of the Java MqttEventSource's Subscriber seam).
 */
export interface MqttClientLike {
  on(event: 'connect', listener: () => void): unknown;
  on(event: 'message', listener: (topic: string, payload: Uint8Array) => void): unknown;
  on(event: 'error', listener: (error: Error) => void): unknown;
  on(event: 'close', listener: () => void): unknown;
  subscribeAsync(topicFilter: string, options?: { qos?: 0 | 1 | 2 }): Promise<unknown>;
  endAsync(force?: boolean): Promise<void>;
}

export interface MqttEventSourceOptions {
  /** Messaging broker URL: mqtt://localhost:1883, or amqp://localhost:5672 for AMQP. */
  brokerUrl: string;
  /** Topic prefix the DDSR broker publishes under; default ddsr/events. */
  topicPrefix?: string;
  clientId?: string;
  qos?: 0 | 1 | 2;
  /** Injectable client factory (tests). Defaults to mqtt.js connect(). */
  clientFactory?: (url: string, clientId: string, tls?: TlsOptions, amqp?: AmqpConnectOptions)
    => Promise<MqttClientLike> | MqttClientLike;
  /** Who this node is to a messaging broker that asks; never part of the broker URL. */
  username?: string;
  password?: string;
  /** The node's certificate and the authorities it believes, for mqtts:// brokers. */
  tls?: TlsOptions;
  log?: (message: string) => void;
}

export const DEFAULT_TOPIC_PREFIX = 'ddsr/events';


/**
 * MQTT implementation of the event source — the TS mirror of the Java
 * client.messaging event transport, with the same contracts:
 *
 * - Subscribes `<prefix>/#`: EventSource.open carries no interest set
 *   (documented A2 finding), routing happens in the SDK's listener
 *   registry. The broker publishes every event on the one channel.
 * - The message is a value of the broker's broadcast (#252): a CloudEvent
 *   `broadcast` in structured mode, the result whose value is the event.
 *   Empty payloads are ignored, undecodable ones and
 *   events of a type that is not ours are logged and skipped.
 * - onStreamEstablished() fires after every (re)connect+subscribe —
 *   mqtt.js emits 'connect' again after its automatic reconnect, which
 *   triggers the SDK's snapshot refresh (FR-Sync-Reconnect), and it is
 *   awaited before messages of that connection are delivered.
 * - A `broadcast.end` means the broker lost an event and the only
 *   recovery is to read everything again (#124). It fires the same hook
 *   a reconnect does, because that is the same recovery.
 */
export class MqttEventSource implements DdsrEventSource {
  private readonly options: MqttEventSourceOptions;
  private readonly log: (message: string) => void;

  constructor(options: MqttEventSourceOptions) {
    this.options = options;
    this.log = options.log ?? ((m) => console.error(`[ddsr-mqtt] ${m}`));
  }


  /**
   * `/` on MQTT, `.` on AMQP - said by the scheme of the broker URL
   * (#138). The prefix is given the way MQTT writes it, because that is
   * the spelling the wire name has, and is a routing key on AMQP: the
   * rule the Java event transports follow (Channels).
   */
  private separator(): '/' | '.' {
    return isAmqpUrl(this.options.brokerUrl) ? '.' : '/';
  }

  topicFilter(): string {
    const prefix = (this.options.topicPrefix ?? DEFAULT_TOPIC_PREFIX).replace(/\/+$/, '');
    const separator = this.separator();
    return `${separator === '/' ? prefix : prefix.replaceAll('/', separator)}${separator}#`;
  }

  /** What this source is, as a client's event transport is named: mqtt or amqp. */
  get transport(): string {
    return isAmqpUrl(this.options.brokerUrl) ? 'amqp' : 'mqtt';
  }

  open(handler: EventSourceHandler): EventSubscription {
    let closed = false;
    let client: MqttClientLike | undefined;
    // Serializes establish-then-deliver: messages arriving while the
    // snapshot refresh still runs are queued behind it.
    let pipeline: Promise<void> = Promise.resolve();

    const connect = async (): Promise<void> => {
      const factory = this.options.clientFactory ?? defaultFactory;
      // An id of this node's own: a broker takes the older of two
      // connections with one client id off the wire, so a fixed default
      // made two consumers throw each other out.
      client = await factory(this.options.brokerUrl,
        this.options.clientId ?? `ddsr-consumer-ts-${randomUUID().slice(0, 8)}`, this.options.tls,
        { username: this.options.username, password: this.options.password });
      client.on('error', (error) => {
        if (!closed) this.log(`mqtt error: ${String(error)}`);
      });
      // Every disconnect, including the one close() causes: the client
      // library reconnects on its own and says so with 'connect', which
      // is when the stream counts as established again.
      client.on('close', () => handler.onStreamLost?.());
      client.on('connect', () => {
        if (closed) return;
        pipeline = pipeline.then(async () => {
          try {
            await client!.subscribeAsync(this.topicFilter(), { qos: this.options.qos ?? 0 });
            await handler.onStreamEstablished();
          } catch (error) {
            this.log(`subscribe failed: ${String(error)}`);
          }
        });
      });
      client.on('message', (topic, payload) => {
        if (closed) return;
        pipeline = pipeline.then(async () => {
          if (this.deliver(payload, handler) === 'end') {
            // The broker ended the broadcast: something was lost between
            // it and here, and there is no way to find out what - no
            // per-client history, no sequence numbers. Reading everything
            // again is the recovery, and it is what a reconnect does (#124).
            this.log('the broker lost an event — taking a fresh snapshot');
            await handler.onStreamEstablished();
          }
        });
      });
    };

    const connecting = connect().catch(error => {
      this.log(`mqtt connect failed: ${String(error)}`);
    });

    return {
      close: async () => {
        closed = true;
        await connecting;
        await pipeline.catch(() => undefined);
        if (client) {
          await client.endAsync().catch(() => undefined);
        }
      },
    };
  }

  /** One message of the broadcast (#252); says when it was the end. */
  private deliver(payload: Uint8Array, handler: EventSourceHandler): 'end' | undefined {
    const text = new TextDecoder().decode(payload);
    if (!text.trim()) return undefined;
    let event: ServiceEvent;
    try {
      const read = readBroadcastEvent(text);
      if (read.kind === 'end') return 'end';
      if (read.kind === 'other') {
        this.log(`ignoring a '${read.type}' message — this subscription is for the broker's events`);
        return undefined;
      }
      event = read.event;
    } catch (error) {
      this.log(`undecodable event payload skipped: ${String(error)}`);
      return undefined;
    }
    try {
      handler.onEvent(event);
    } catch (error) {
      this.log(`event handler failed: ${String(error)}`);
    }
    return undefined;
  }
}

async function defaultFactory(
  url: string, clientId: string, tls?: TlsOptions, amqp: AmqpConnectOptions = {},
): Promise<MqttClientLike> {
  if (isAmqpUrl(url)) return await connectAmqp(url, clientId, tls, amqp);
  const { connect } = await import('mqtt');
  return connect(url, { clientId, clean: true, ...tlsConnectOptions(tls) }) as unknown as MqttClientLike;
}
