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

import type { ServiceEvent } from '@ddsr/model';
import type { DdsrEventSource, EventSourceHandler, EventSubscription } from '@ddsr/client';
import {
  asRoots, dataAsText, deserializeFromXmi, firstOfClass, lifecycleEventTypeOf, readStructured,
} from '@ddsr/client';

/**
 * The slice of the mqtt.js client this source needs — injectable for
 * tests (mirror of the Java MqttEventSource's Subscriber seam).
 */
export interface MqttClientLike {
  on(event: 'connect', listener: () => void): unknown;
  on(event: 'message', listener: (topic: string, payload: Uint8Array) => void): unknown;
  on(event: 'error', listener: (error: Error) => void): unknown;
  subscribeAsync(topicFilter: string, options?: { qos?: 0 | 1 | 2 }): Promise<unknown>;
  endAsync(force?: boolean): Promise<void>;
}

export interface MqttEventSourceOptions {
  /** MQTT broker URL, e.g. mqtt://localhost:1883 */
  brokerUrl: string;
  /** Topic prefix the DDSR broker publishes under; default ddsr/events. */
  topicPrefix?: string;
  clientId?: string;
  qos?: 0 | 1 | 2;
  /** Injectable client factory (tests). Defaults to mqtt.js connect(). */
  clientFactory?: (url: string, clientId: string) => Promise<MqttClientLike> | MqttClientLike;
  log?: (message: string) => void;
}

export const DEFAULT_TOPIC_PREFIX = 'ddsr/events';

/**
 * Topic segment on which the broker says an event did not reach the
 * wire and subscribers have to take a fresh snapshot (#124). Named on
 * the broker side in `MqttEventSink`; one string on each side of a
 * wire, like the topic prefix itself.
 */
export const RESYNC_TOPIC_SEGMENT = '_resync';

/**
 * MQTT implementation of the event source — the TS mirror of the Java
 * client.mqtt transport, with the same contracts:
 *
 * - Subscribes `<prefix>/#`: EventSource.open carries no interest set
 *   (documented A2 finding), routing happens in the SDK's listener
 *   registry — including the `_unknown` topic for events whose
 *   interface could not be determined.
 * - The message is a CloudEvent in structured mode (#101), carrying the
 *   same self-contained XMI document as on SSE; the first ServiceEvent
 *   root is delivered. Empty payloads are ignored, undecodable ones and
 *   events of a type that is not ours are logged and skipped.
 * - onStreamEstablished() fires after every (re)connect+subscribe —
 *   mqtt.js emits 'connect' again after its automatic reconnect, which
 *   triggers the SDK's snapshot refresh (FR-Sync-Reconnect), and it is
 *   awaited before messages of that connection are delivered.
 * - A message on `<prefix>/_resync` means the broker lost an event and
 *   the only recovery is to read everything again (#124). It fires the
 *   same hook a reconnect does, because that is the same recovery.
 */
export class MqttEventSource implements DdsrEventSource {
  private readonly options: MqttEventSourceOptions;
  private readonly log: (message: string) => void;

  constructor(options: MqttEventSourceOptions) {
    this.options = options;
    this.log = options.log ?? ((m) => console.error(`[ddsr-mqtt] ${m}`));
  }

  /** Is this the topic on which the broker admits a loss? */
  private isResync(topic: string | undefined): boolean {
    return typeof topic === 'string' && topic.endsWith(`/${RESYNC_TOPIC_SEGMENT}`);
  }

  topicFilter(): string {
    const prefix = (this.options.topicPrefix ?? DEFAULT_TOPIC_PREFIX).replace(/\/+$/, '');
    return `${prefix}/#`;
  }

  open(handler: EventSourceHandler): EventSubscription {
    let closed = false;
    let client: MqttClientLike | undefined;
    // Serializes establish-then-deliver: messages arriving while the
    // snapshot refresh still runs are queued behind it.
    let pipeline: Promise<void> = Promise.resolve();

    const connect = async (): Promise<void> => {
      const factory = this.options.clientFactory ?? defaultFactory;
      client = await factory(this.options.brokerUrl, this.options.clientId ?? 'ddsr-consumer-ts');
      client.on('error', (error) => {
        if (!closed) this.log(`mqtt error: ${String(error)}`);
      });
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
          if (this.isResync(topic)) {
            // Something was lost between the broker and here, and there
            // is no way to find out what: the broker keeps no per-client
            // history and the stream carries no sequence numbers.
            // Reading everything again is the recovery, and it is
            // exactly what a reconnect already does.
            this.log('the broker lost an event — taking a fresh snapshot');
            await handler.onStreamEstablished();
            return;
          }
          this.deliver(payload, handler);
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

  private deliver(payload: Uint8Array, handler: EventSourceHandler): void {
    const text = new TextDecoder().decode(payload);
    if (!text.trim()) return;
    let event: ServiceEvent | undefined;
    try {
      // Structured mode: the envelope and the document in one JSON
      // message, which is what a transport carrying nothing but
      // messages gets (#101).
      const message = readStructured(text);
      if (!lifecycleEventTypeOf(message.attributes.type)) {
        this.log(`ignoring a '${message.attributes.type}' message — this subscription is for`
          + ' lifecycle events');
        return;
      }
      const document = dataAsText(message);
      if (!document) {
        this.log('a lifecycle event arrived without its document, ignoring');
        return;
      }
      event = firstOfClass<ServiceEvent>(asRoots(deserializeFromXmi(document)), 'ServiceEvent');
    } catch (error) {
      this.log(`undecodable event payload skipped: ${String(error)}`);
      return;
    }
    if (!event) {
      this.log('event payload without ServiceEvent root ignored');
      return;
    }
    try {
      handler.onEvent(event);
    } catch (error) {
      this.log(`event handler failed: ${String(error)}`);
    }
  }
}

async function defaultFactory(url: string, clientId: string): Promise<MqttClientLike> {
  const { connect } = await import('mqtt');
  return connect(url, { clientId, clean: true }) as unknown as MqttClientLike;
}
