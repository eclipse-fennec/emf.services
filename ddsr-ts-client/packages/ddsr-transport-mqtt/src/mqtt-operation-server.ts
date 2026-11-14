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
import { randomUUID } from 'node:crypto';
import { declaredCode, declaredRaisedBy, toArray } from '@ddsr/client';
import type { MessagingFlavor, MessagingOperationFlavor, ServiceOperationFlavor } from '@ddsr/model';
import {
  MqttRpcClientLike,
  connectMessaging,
  decodeRequest,
  encodeBroadcast,
  encodeBroadcastEnd,
  encodeResponse,
  qosFor,
  requestTopicFor,
  sessionPrefixFor,
} from './mqtt-rpc.js';
import type { AmqpConnectOptions } from './amqp-client.js';
import { SESSION_REASON, respondSession } from './messaging-session.js';

/** One handler per operation name; receives the named args, returns the result. */
export type OperationHandlers = Record<string, (args: Record<string, unknown>) => unknown | Promise<unknown>>;

export interface MqttOperationServerOptions {
  /** Broker to listen on; defaults to the flavor's first brokers entry. */
  brokerUrl?: string;
  clientId?: string;
  clientFactory?: (url: string, clientId: string, tls?: TlsOptions, amqp?: AmqpConnectOptions)
    => Promise<MqttRpcClientLike> | MqttRpcClientLike;
  /** Who this node is to a messaging broker that asks; never part of a broker URL. */
  username?: string;
  password?: string;
  /** The node's certificate and the authorities it believes, for mqtts:// brokers. */
  tls?: TlsOptions;
  /** Which system this is, for the CloudEvents `source` of every answer (#101). */
  originLabel?: string;
  log?: (message: string) => void;
}

/**
 * The provider side of the MQTT request/response convention:
 * subscribes each operation's request topic of the given MessagingFlavor,
 * dispatches to the matching handler, and publishes the answer — a
 * CloudEvent of its own, correlated with the call — to the reply topic
 * the call named. A handler failure answers with a Diagnostic, so the
 * consumer sees the failure instead of a timeout. A message that is
 * not a readable call is logged and dropped: there is nothing to
 * answer to.
 */
export class MqttOperationServer {
  private readonly flavor: MessagingFlavor;
  private readonly handlers: OperationHandlers;
  private readonly options: MqttOperationServerOptions;
  private readonly log: (message: string) => void;
  private client: MqttRpcClientLike | undefined;
  private readonly topicToOperation = new Map<string, string>();
  /** The channels of the sessions open now, and who listens on each. */
  private readonly sessions = new Map<string, (payload: Uint8Array) => void>();

  constructor(flavor: MessagingFlavor, handlers: OperationHandlers, options: MqttOperationServerOptions = {}) {
    this.flavor = flavor;
    this.handlers = handlers;
    this.options = options;
    this.log = options.log ?? ((m) => console.error(`[ddsr-mqtt-server] ${m}`));
  }

  async start(): Promise<void> {
    const brokers = toArray<string>(this.flavor.brokers);
    const url = this.options.brokerUrl ?? brokers[0];
    if (!url) throw new Error('MessagingFlavor announces no brokers and the server has no brokerUrl override');
    // MQTT or AMQP is the scheme of the broker (#138): the same calls,
    // the same envelope, on a channel spelled the way that broker does.
    const factory = this.options.clientFactory ?? connectMessaging;
    this.client = await factory(url, this.options.clientId ?? `ddsr-provider-${randomUUID().slice(0, 8)}`,
      this.options.tls,
      { exchange: this.flavor.exchange || undefined, username: this.options.username, password: this.options.password });
    this.client.on('message', (topic, payload) => void this.dispatch(topic, payload));

    for (const opFlavor of toArray<ServiceOperationFlavor>(this.flavor.operationFlavors)) {
      const mqttOp = opFlavor as MessagingOperationFlavor;
      const operationName = mqttOp.operation?.name ?? mqttOp.name;
      if (!operationName || !this.handlers[operationName]) {
        this.log(`no handler for operation flavor '${mqttOp.name}' — not subscribing`);
        continue;
      }
      const topic = requestTopicFor(this.flavor, mqttOp);
      if (String(mqttOp.operation?.returnValue?.delivery ?? '') === 'BROADCAST') {
        // A stream every receiver shares (#252): nobody calls it, the
        // handler is asked once and what it sends is published.
        this.broadcast(operationName, topic, mqttOp);
        continue;
      }
      this.topicToOperation.set(topic, operationName);
      await this.client.subscribeAsync(topic, { qos: qosFor(this.flavor, mqttOp) });
      this.log(`serving ${operationName} on ${topic}`);
    }
  }

  /**
   * Publishes what the handler of a broadcast sends, on the operation's
   * channel, at the source's pace - the next value is taken when the
   * broker has the one before. The failure is the last value, then the
   * end. Stopping the server returns the handler's stream.
   */
  private broadcast(operationName: string, topic: string, opFlavor: MessagingOperationFlavor): void {
    const source = `/provider/${this.options.originLabel ?? operationName}`;
    const qos = qosFor(this.flavor, opFlavor);
    const run = async () => {
      const client = this.client;
      if (!client) return;
      try {
        const values = await this.handlers[operationName]({});
        if (!isAsyncIterable(values)) {
          throw new Error(`${operationName} broadcasts by its contract, and its handler returned no async iterable`);
        }
        const iterator = values[Symbol.asyncIterator]();
        this.running.push(iterator);
        for (let next = await iterator.next(); !next.done; next = await iterator.next()) {
          if (!this.client) return;
          await client.publishAsync(topic, encodeBroadcast(source, next.value, undefined, opFlavor),
            { qos, retain: false })
            .catch(error => this.log(`a value of the broadcast on ${topic} was lost: ${String(error)}`));
        }
        await client.publishAsync(topic, encodeBroadcastEnd(source), { qos, retain: false });
      } catch (error) {
        if (!this.client) return;
        this.log(`the broadcast on ${topic} ended with a failure: ${String(error)}`);
        const declared = declaredRaisedBy(opFlavor.operation, error);
        await client.publishAsync(topic, declared
          ? encodeBroadcast(source, undefined, (error as Error).message || declared.name || 'declared error', opFlavor,
            declaredCode(declared) ?? 0)
          : encodeBroadcast(source, undefined, String(error), opFlavor), { qos, retain: false })
          .catch(() => undefined);
      }
    };
    this.log(`broadcasting ${operationName} on ${topic}`);
    void run();
  }

  /** The streams of the broadcasts running now, returned when the server stops. */
  private readonly running: AsyncIterator<unknown>[] = [];

  async stop(): Promise<void> {
    const client = this.client;
    this.client = undefined;
    for (const values of this.running.splice(0)) {
      void values.return?.(undefined);
    }
    if (!client) return;
    for (const topic of this.topicToOperation.keys()) {
      await client.unsubscribeAsync(topic).catch(() => undefined);
    }
    await client.endAsync().catch(() => undefined);
  }

  /** The flavor of one operation, for what its answer is encoded in (#100). */
  private operationFlavorFor(operationName: string): ServiceOperationFlavor | undefined {
    return toArray<ServiceOperationFlavor>(this.flavor.operationFlavors)
      .find(of => (of.operation?.name ?? of.name) === operationName);
  }

  private async dispatch(topic: string, payload: Uint8Array): Promise<void> {
    const client = this.client;
    const session = this.sessions.get(topic);
    if (session) {
      session(payload);
      return;
    }
    const operationName = this.topicToOperation.get(topic);
    if (!client || !operationName) return; // a reply or foreign topic
    let request;
    try {
      request = decodeRequest(payload);
    } catch (error) {
      this.log(`undecodable request on ${topic} dropped: ${String(error)}`);
      return;
    }
    const source = `/provider/${this.options.originLabel ?? operationName}`;
    const opFlavor = this.operationFlavorFor(operationName);
    if (request.session) {
      // A stream it answers with (Parameter.delivery = STREAM): a session
      // of the messaging library, accepted on a channel of this side's.
      this.serveSession(client, request, request.session, source, opFlavor, operationName);
      return;
    }
    let answer: Uint8Array;
    try {
      const result = await this.handlers[operationName](request.args);
      if (isAsyncIterable(result)) {
        throw new Error(`${operationName} answered with a stream, and the call opened no session for one`);
      }
      answer = encodeResponse(request.id, source, result, undefined, opFlavor);
    } catch (error) {
      // A failure is an answer: the consumer sees why, instead of
      // waiting out its timeout for a message that never comes. This
      // catches two different failures on purpose — the handler's, and
      // a contract declaring an encoding this side cannot write (#100).
      // The Diagnostic goes out in XMI either way: it is not the
      // operation's declared result, so `produces` does not describe
      // it, and a consumer that learns why beats one that learns
      // nothing until its timer runs out.
      //
      // An error the contract declares travels under its code - over
      // MQTT that code is all a declared error is, and the caller
      // recognises it by it (#185). Anything else has no code.
      const declared = declaredRaisedBy(opFlavor?.operation, error);
      answer = declared
        ? encodeResponse(request.id, source, undefined,
            (error as Error).message || declared.name || 'declared error', {}, declaredCode(declared) ?? 0)
        : encodeResponse(request.id, source, undefined, String(error));
    }
    try {
      await client.publishAsync(request.replyTo, answer, { retain: false });
    } catch (error) {
      this.log(`could not answer ${request.id} on ${request.replyTo}: ${String(error)}`);
    }
  }

  private serveSession(
    client: MqttRpcClientLike, request: { args: Record<string, unknown>; replyTo: string },
    session: { id: string; lease?: number }, source: string, opFlavor: ServiceOperationFlavor | undefined,
    operationName: string,
  ): void {
    const qos = opFlavor ? qosFor(this.flavor, opFlavor as MessagingOperationFlavor) : 1;
    respondSession({
      client,
      sessionId: session.id,
      replyTo: request.replyTo,
      askedLease: session.lease,
      owner: this.options.originLabel ?? operationName,
      channelPrefix: opFlavor ? sessionPrefixFor(this.flavor, opFlavor as MessagingOperationFlavor) : undefined,
      source,
      qos,
      start: async () => {
        const result = await this.handlers[operationName](request.args);
        if (!isAsyncIterable(result)) {
          throw new Error(`${operationName} streams by its contract, and its handler returned no async iterable`);
        }
        return result;
      },
      value: value => encodeResponse(session.id, source, value, undefined, opFlavor),
      failure: error => {
        // A declared error travels under its code, as in a single answer,
        // and the reason that ends the session says which.
        const declared = declaredRaisedBy(opFlavor?.operation, error);
        const code = declared ? declaredCode(declared) ?? 0 : 0;
        const reason = { [SESSION_REASON]: declared ? `declared:${code}` : 'failed' };
        return declared
          ? encodeResponse(session.id, source, undefined,
              (error as Error).message || declared.name || 'declared error', {}, code, reason)
          : encodeResponse(session.id, source, undefined, String(error), {}, 0, reason);
      },
      listen: (channel, deliver) => { this.sessions.set(channel, deliver); },
      unlisten: channel => { this.sessions.delete(channel); },
      log: this.log,
    }).catch(error => this.log(`could not open the session ${session.id}: ${String(error)}`));
  }
}

/** Whether a handler's result is a stream to be sent value by value. */
function isAsyncIterable(value: unknown): value is AsyncIterable<unknown> {
  return typeof value === 'object' && value !== null
    && typeof (value as { [Symbol.asyncIterator]?: unknown })[Symbol.asyncIterator] === 'function';
}
