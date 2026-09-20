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
import type { FlavorPlugin } from '@ddsr/client';
import { toArray } from '@ddsr/client';
import type {
  MqttFlavor,
  MqttOperationFlavor,
  ServiceFlavor,
  ServiceOperation,
  ServiceOperationFlavor,
} from '@ddsr/model';
import {
  MqttRpcClientLike,
  connectMqtt,
  decodeResponse,
  encodeRequest,
  qosFor,
  replyBaseFor,
  requestTopicFor,
} from './mqtt-rpc';

export interface MqttFlavorPluginOptions {
  /**
   * Broker URL override. Without it the plugin connects to the FIRST
   * entry of the announced MqttFlavor.brokers list — the provider says
   * where it listens, exactly like RestFlavor.host says where it
   * serves.
   */
  brokerUrl?: string;
  clientId?: string;
  /** Reply wait in milliseconds; default 10s. */
  timeoutMs?: number;
  /** Injectable client factory (tests). Defaults to mqtt.js. */
  clientFactory?: (url: string, clientId: string) => Promise<MqttRpcClientLike> | MqttRpcClientLike;
  /** Which system this is, for the CloudEvents `source` of every call (#101). */
  originLabel?: string;
  log?: (message: string) => void;
}

/**
 * MQTT FlavorPlugin: translates ServiceOperation invocations into the
 * request/response convention of mqtt-rpc.ts — subscribe the
 * per-request reply topic FIRST, publish the call as a CloudEvent,
 * await exactly the answer that correlates with it, unsubscribe.
 * The invocation path is peer-to-peer between consumer and provider
 * over the MQTT broker the flavor announces; the DDSR broker is not
 * involved (Discovery/Acquisition only — ACQUISITION.md §1).
 */
export class MqttFlavorPlugin implements FlavorPlugin {
  readonly flavorKind = 'MQTT';

  private readonly options: MqttFlavorPluginOptions;
  private readonly log: (message: string) => void;
  private readonly clientsByUrl = new Map<string, Promise<MqttRpcClientLike>>();
  private readonly pending = new Map<string, (payload: Uint8Array) => void>();

  constructor(options: MqttFlavorPluginOptions = {}) {
    this.options = options;
    this.log = options.log ?? ((m) => console.error(`[ddsr-mqtt-rpc] ${m}`));
  }

  canHandle(flavor: ServiceFlavor): boolean {
    return (
      flavor.kind === 'MQTT'
      || (flavor as { eClass?: () => { name?: string } }).eClass?.()?.name === 'MqttFlavor'
    );
  }

  async invoke(
    operation: ServiceOperation,
    params: Record<string, unknown>,
    flavor: ServiceFlavor,
    operationFlavor: ServiceOperationFlavor
  ): Promise<unknown> {
    const mqttFlavor = flavor as MqttFlavor;
    const mqttOp = operationFlavor as MqttOperationFlavor;
    const client = await this.clientFor(mqttFlavor);

    const requestTopic = requestTopicFor(mqttFlavor, mqttOp);
    const qos = qosFor(mqttFlavor, mqttOp);
    const timeoutMs = this.options.timeoutMs ?? 10_000;
    // The reply topic is chosen before the request exists, because the
    // request has to carry it. The event id is what the answer
    // correlates with, so the two are built together.
    const replyBase = replyBaseFor(mqttFlavor, mqttOp);
    const source = `/consumer/${this.options.originLabel ?? 'ts'}`;
    const provisional = randomUUID();
    const replyTo = `${replyBase}/${provisional}`;
    const request = encodeRequest(operation, params, replyTo, source, mqttOp);

    await client.subscribeAsync(replyTo, { qos });
    try {
      const answer = new Promise<Uint8Array>((resolve, reject) => {
        this.pending.set(request.id, resolve);
        setTimeout(() => {
          if (this.pending.delete(request.id)) {
            reject(new Error(
              `MQTT call timed out after ${timeoutMs}ms: ${operation.name} via ${requestTopic}`));
          }
        }, timeoutMs).unref?.();
      });
      await client.publishAsync(requestTopic, request.payload, { qos, retain: false });
      const payload = await answer;
      const response = decodeResponse(payload);
      if (response.error !== undefined) {
        throw new Error(`MQTT call failed: ${operation.name} — ${response.error}`);
      }
      return response.value;
    } finally {
      this.pending.delete(request.id);
      await client.unsubscribeAsync(replyTo).catch(() => undefined);
    }
  }

  /** Closes every broker connection this plugin opened. */
  async close(): Promise<void> {
    for (const clientPromise of this.clientsByUrl.values()) {
      const client = await clientPromise.catch(() => undefined);
      await client?.endAsync().catch(() => undefined);
    }
    this.clientsByUrl.clear();
  }

  private clientFor(flavor: MqttFlavor): Promise<MqttRpcClientLike> {
    const brokers = toArray<string>(flavor.brokers);
    const url = this.options.brokerUrl ?? brokers[0];
    if (!url) {
      throw new Error('MqttFlavor announces no brokers and the plugin has no brokerUrl override');
    }
    let clientPromise = this.clientsByUrl.get(url);
    if (!clientPromise) {
      const factory = this.options.clientFactory ?? connectMqtt;
      clientPromise = Promise.resolve(
        factory(url, this.options.clientId ?? `ddsr-rpc-${randomUUID().slice(0, 8)}`)
      ).then(client => {
        client.on('message', (topic, payload) => this.onMessage(topic, payload));
        return client;
      });
      this.clientsByUrl.set(url, clientPromise);
    }
    return clientPromise;
  }

  private onMessage(_topic: string, payload: Uint8Array): void {
    try {
      const response = decodeResponse(payload);
      const waiter = this.pending.get(response.correlationId);
      if (waiter) {
        this.pending.delete(response.correlationId);
        waiter(payload);
      }
      // No waiter: a late answer after timeout/unsubscribe — drop it.
    } catch (error) {
      this.log(`undecodable rpc response skipped: ${String(error)}`);
    }
  }
}
