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
  encodeResponse,
  qosFor,
  requestTopicFor,
} from './mqtt-rpc.js';
import type { AmqpConnectOptions } from './amqp-client.js';
import { respondSession } from './messaging-session.js';

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
      this.topicToOperation.set(topic, operationName);
      await this.client.subscribeAsync(topic, { qos: qosFor(this.flavor, mqttOp) });
      this.log(`serving ${operationName} on ${topic}`);
    }
  }

  async stop(): Promise<void> {
    const client = this.client;
    this.client = undefined;
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
        // A declared error travels under its code, as in a single answer.
        const declared = declaredRaisedBy(opFlavor?.operation, error);
        return declared
          ? encodeResponse(session.id, source, undefined,
              (error as Error).message || declared.name || 'declared error', {}, declaredCode(declared) ?? 0)
          : encodeResponse(session.id, source, undefined, String(error));
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
