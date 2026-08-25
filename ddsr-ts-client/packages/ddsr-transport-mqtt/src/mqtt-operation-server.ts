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
import { toArray } from '@ddsr/client';
import type { MqttFlavor, MqttOperationFlavor, ServiceOperationFlavor } from '@ddsr/model';
import {
  MqttRpcClientLike,
  connectMqtt,
  decodeRequest,
  encodeResponse,
  qosFor,
  requestTopicFor,
} from './mqtt-rpc';

/** One handler per operation name; receives the named args, returns the result. */
export type OperationHandlers = Record<string, (args: Record<string, unknown>) => unknown | Promise<unknown>>;

export interface MqttOperationServerOptions {
  /** Broker to listen on; defaults to the flavor's first brokers entry. */
  brokerUrl?: string;
  clientId?: string;
  clientFactory?: (url: string, clientId: string) => Promise<MqttRpcClientLike> | MqttRpcClientLike;
  log?: (message: string) => void;
}

/**
 * The provider side of the MQTT request/response convention (A2
 * Etappe 2): subscribes each operation's request topic of the given
 * MqttFlavor, dispatches to the matching handler, and publishes the
 * response envelope to the per-request replyTo. A handler failure
 * answers with the error envelope — the consumer sees the failure
 * instead of a timeout. Requests without a decodable envelope are
 * logged and dropped (nothing to answer to).
 */
export class MqttOperationServer {
  private readonly flavor: MqttFlavor;
  private readonly handlers: OperationHandlers;
  private readonly options: MqttOperationServerOptions;
  private readonly log: (message: string) => void;
  private client: MqttRpcClientLike | undefined;
  private readonly topicToOperation = new Map<string, string>();

  constructor(flavor: MqttFlavor, handlers: OperationHandlers, options: MqttOperationServerOptions = {}) {
    this.flavor = flavor;
    this.handlers = handlers;
    this.options = options;
    this.log = options.log ?? ((m) => console.error(`[ddsr-mqtt-server] ${m}`));
  }

  async start(): Promise<void> {
    const brokers = toArray<string>(this.flavor.brokers);
    const url = this.options.brokerUrl ?? brokers[0];
    if (!url) throw new Error('MqttFlavor announces no brokers and the server has no brokerUrl override');
    const factory = this.options.clientFactory ?? connectMqtt;
    this.client = await factory(url, this.options.clientId ?? `ddsr-provider-${randomUUID().slice(0, 8)}`);
    this.client.on('message', (topic, payload) => void this.dispatch(topic, payload));

    for (const opFlavor of toArray<ServiceOperationFlavor>(this.flavor.operationFlavors)) {
      const mqttOp = opFlavor as MqttOperationFlavor;
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

  private async dispatch(topic: string, payload: Uint8Array): Promise<void> {
    const client = this.client;
    const operationName = this.topicToOperation.get(topic);
    if (!client || !operationName) return; // a reply or foreign topic
    let request;
    try {
      request = decodeRequest(payload);
    } catch (error) {
      this.log(`undecodable request on ${topic} dropped: ${String(error)}`);
      return;
    }
    let response;
    try {
      const result = await this.handlers[operationName](request.args);
      response = { correlationId: request.correlationId, result };
    } catch (error) {
      response = { correlationId: request.correlationId, error: String(error) };
    }
    try {
      await client.publishAsync(request.replyTo, encodeResponse(response), { retain: false });
    } catch (error) {
      this.log(`could not answer ${request.correlationId} on ${request.replyTo}: ${String(error)}`);
    }
  }
}
