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

import type { TlsOptions } from '@ddsr/client';

/**
 * AMQP 0-9-1 behind the seam the MQTT pieces already have.
 *
 * Over a messaging broker everything here is one transport: the
 * envelope, the request/reply convention and the event stream do not
 * know which protocol carries them. What differs is the spelling of a
 * channel - a routing key on a topic exchange instead of a topic - and
 * the client that talks to the broker. This is that client, shaped like
 * the slice of mqtt.js the RPC plugin, the operation server and the
 * event source use, so none of them grows an AMQP branch (#138).
 *
 * The mapping is the one the Java side's messaging instance uses, which
 * is what makes the two understand each other:
 *
 *   - a channel is a routing key on the topic exchange (`amq.topic`
 *     unless the flavor names another);
 *   - subscribing binds that key - `#` and `*` are AMQP's own wildcards
 *     - to ONE server-named, exclusive, auto-delete queue of this
 *     connection, so nobody else can read what arrives there;
 *   - a message is handed on under the routing key it was published
 *     with, the way mqtt.js hands on the topic.
 *
 * The connection recovers on its own (amqplib's opt-in recovery). The
 * queue is exclusive and goes with the connection, so every recovery
 * declares a new one and binds what was subscribed again - and says
 * `connect`, which is when an event source takes a fresh snapshot.
 */

/** The topic exchange every AMQP 0-9-1 broker declares. */
export const DEFAULT_EXCHANGE = 'amq.topic';

export interface AmqpConnectOptions {
  /** The exchange channels are routing keys on; `amq.topic` when not given. */
  exchange?: string;
  /** Who this node is to a broker that asks; never part of the URL. */
  username?: string;
  password?: string;
}

/** The slice of amqplib this adapter uses - injectable for tests. */
export interface AmqpChannelLike {
  assertQueue(queue: string, options: { exclusive: boolean; autoDelete: boolean; durable: boolean }):
    Promise<{ queue: string }>;
  bindQueue(queue: string, exchange: string, pattern: string): Promise<unknown>;
  unbindQueue(queue: string, exchange: string, pattern: string): Promise<unknown>;
  consume(queue: string,
    onMessage: (message: { fields: { routingKey: string }; content: Uint8Array } | null) => void,
    options: { noAck: boolean }): Promise<unknown>;
  publish(exchange: string, routingKey: string, content: Buffer): boolean;
}

export interface AmqpConnectionLike {
  on(event: 'connect', listener: (model: { createChannel(): Promise<AmqpChannelLike> }) => void): unknown;
  on(event: 'disconnect' | 'error', listener: (error: Error) => void): unknown;
  close(): Promise<void>;
}

type Listener = (...args: never[]) => void;

/** One AMQP connection, speaking the client interface of the MQTT pieces. */
export class AmqpClient {
  private readonly listeners = new Map<string, Listener[]>();
  private readonly bound = new Set<string>();
  private channel: AmqpChannelLike | undefined;
  private queue: string | undefined;
  private connected = false;
  private readonly ready: Promise<void>;
  private markReady: () => void = () => undefined;
  private failReady: (error: Error) => void = () => undefined;

  constructor(private readonly connection: AmqpConnectionLike, private readonly exchange: string) {
    this.ready = new Promise<void>((resolve, reject) => {
      this.markReady = resolve;
      this.failReady = reject;
    });
    // Never an unhandled rejection: a caller that does not wait for the
    // first connection hears about a failure through 'error'.
    this.ready.catch(() => undefined);
    connection.on('connect', model => void this.established(model));
    connection.on('disconnect', () => {
      this.connected = false;
      this.channel = undefined;
      this.queue = undefined;
      this.emit('close');
    });
    connection.on('error', error => {
      this.failReady(error);
      this.emit('error', error);
    });
  }

  /** Resolves when the queue is declared and consumed for the first time. */
  whenReady(): Promise<void> {
    return this.ready;
  }

  private async established(model: { createChannel(): Promise<AmqpChannelLike> }): Promise<void> {
    try {
      const channel = await model.createChannel();
      const { queue } = await channel.assertQueue('', { exclusive: true, autoDelete: true, durable: false });
      for (const pattern of this.bound) {
        await channel.bindQueue(queue, this.exchange, pattern);
      }
      await channel.consume(queue, message => {
        if (message) this.emit('message', message.fields.routingKey, message.content);
      }, { noAck: true });
      this.channel = channel;
      this.queue = queue;
      this.connected = true;
      this.markReady();
      this.emit('connect');
    } catch (error) {
      this.failReady(error as Error);
      this.emit('error', error);
    }
  }

  on(event: 'connect' | 'close', listener: () => void): this;
  on(event: 'message', listener: (topic: string, payload: Uint8Array) => void): this;
  on(event: 'error', listener: (error: Error) => void): this;
  on(event: string, listener: Listener): this {
    this.listeners.set(event, [...(this.listeners.get(event) ?? []), listener]);
    // mqtt.js connects after connect() returned, so a 'connect' listener
    // attached then still hears the first connection. This client may
    // be connected already - it is told at once, which is the same thing.
    if (event === 'connect' && this.connected) {
      queueMicrotask(() => (listener as () => void)());
    }
    return this;
  }

  private emit(event: string, ...args: unknown[]): void {
    for (const listener of this.listeners.get(event) ?? []) {
      try {
        (listener as (...values: unknown[]) => void)(...args);
      } catch {
        // A listener's failure is the listener's.
      }
    }
  }

  /** Binds a routing key, wildcards allowed, to this connection's queue. */
  async subscribeAsync(pattern: string): Promise<void> {
    this.bound.add(pattern);
    if (this.channel && this.queue) {
      await this.channel.bindQueue(this.queue, this.exchange, pattern);
    }
  }

  async unsubscribeAsync(pattern: string): Promise<void> {
    this.bound.delete(pattern);
    if (this.channel && this.queue) {
      await this.channel.unbindQueue(this.queue, this.exchange, pattern);
    }
  }

  /** Publishes to the exchange under the channel as its routing key. */
  async publishAsync(routingKey: string, payload: Uint8Array | string): Promise<void> {
    const channel = this.channel;
    if (!channel) throw new Error(`not connected to the AMQP broker - ${routingKey} was not published`);
    channel.publish(this.exchange, routingKey,
      typeof payload === 'string' ? Buffer.from(payload, 'utf8') : Buffer.from(payload));
  }

  async endAsync(): Promise<void> {
    await this.connection.close();
  }
}

/** Whether a broker URL names an AMQP broker. */
export function isAmqpUrl(brokerUrl: string | undefined): boolean {
  const url = (brokerUrl ?? '').trim().toLowerCase();
  return url.startsWith('amqp:') || url.startsWith('amqps:');
}

/**
 * A live AMQP connection with amqplib, ready when this resolves. With
 * `tls` on an amqps:// URL the node shows its certificate and believes
 * the given authorities - the same `TlsOptions` the MQTT factory takes.
 */
export async function connectAmqp(
  brokerUrl: string, clientId: string, tls?: TlsOptions, options: AmqpConnectOptions = {},
): Promise<AmqpClient> {
  const amqplib = await import('amqplib');
  const socketOptions: Record<string, unknown> = {
    clientProperties: { connection_name: clientId },
    recovery: { waitForConnect: false },
  };
  if (tls) {
    for (const [key, value] of Object.entries({
      pfx: tls.pfx, passphrase: tls.passphrase, cert: tls.cert, key: tls.key, ca: tls.ca,
    })) {
      if (value !== undefined) socketOptions[key] = value;
    }
  }
  if (options.username) {
    socketOptions.credentials = amqplib.credentials.plain(options.username, options.password ?? '');
  }
  const connection = await (amqplib.connect as unknown as
    (url: string, socket: Record<string, unknown>) => Promise<AmqpConnectionLike>)(brokerUrl, socketOptions);
  const client = new AmqpClient(connection, options.exchange || DEFAULT_EXCHANGE);
  await client.whenReady();
  return client;
}
