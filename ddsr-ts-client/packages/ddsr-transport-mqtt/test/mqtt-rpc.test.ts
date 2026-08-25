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

import { describe, expect, it } from 'vitest';
import { DDSRFactory } from '@ddsr/model';
import type { MqttFlavor, MqttOperationFlavor, ServiceOperation } from '@ddsr/model';
import { MqttFlavorPlugin } from '../src/mqtt-flavor-plugin';
import { MqttOperationServer } from '../src/mqtt-operation-server';
import type { MqttRpcClientLike } from '../src/mqtt-rpc';
import { qosFor, replyBaseFor, requestTopicFor } from '../src/mqtt-rpc';

function firstOpFlavor(flavor: MqttFlavor): MqttOperationFlavor {
  const list = flavor.operationFlavors as unknown as
    { get?: (i: number) => MqttOperationFlavor } & Iterable<MqttOperationFlavor>;
  if (typeof list.get === 'function') return list.get(0);
  return [...list][0];
}

/**
 * In-memory MQTT broker fake: exact-topic routing between any number
 * of connected clients — enough for the request/response convention,
 * which never uses wildcards (one reply topic per request).
 */
class FakeBroker {
  private readonly subscriptions = new Map<string, Set<FakeClient>>();

  connect(): FakeClient {
    return new FakeClient(this);
  }

  route(topic: string, payload: Uint8Array): void {
    for (const client of this.subscriptions.get(topic) ?? []) {
      client.deliver(topic, payload);
    }
  }

  subscribe(topic: string, client: FakeClient): void {
    let set = this.subscriptions.get(topic);
    if (!set) this.subscriptions.set(topic, set = new Set());
    set.add(client);
  }

  unsubscribe(topic: string, client: FakeClient): void {
    this.subscriptions.get(topic)?.delete(client);
  }
}

class FakeClient implements MqttRpcClientLike {
  private readonly broker: FakeBroker;
  private readonly listeners: Array<(topic: string, payload: Uint8Array) => void> = [];

  constructor(broker: FakeBroker) {
    this.broker = broker;
  }

  on(_event: 'message', listener: (topic: string, payload: Uint8Array) => void): unknown {
    this.listeners.push(listener);
    return this;
  }

  async subscribeAsync(topicFilter: string): Promise<unknown> {
    this.broker.subscribe(topicFilter, this);
    return undefined;
  }

  async unsubscribeAsync(topicFilter: string): Promise<unknown> {
    this.broker.unsubscribe(topicFilter, this);
    return undefined;
  }

  async publishAsync(topic: string, payload: Uint8Array | string): Promise<unknown> {
    const bytes = typeof payload === 'string' ? new TextEncoder().encode(payload) : payload;
    // Async boundary like a real broker — the answer never arrives
    // inside the publish call.
    queueMicrotask(() => this.broker.route(topic, bytes));
    return undefined;
  }

  async endAsync(): Promise<void> {
    // nothing held
  }

  deliver(topic: string, payload: Uint8Array): void {
    for (const listener of this.listeners) listener(topic, payload);
  }
}

// --------------------------------------------------------------------

const factory = DDSRFactory.eINSTANCE;

function paymentMqttFlavor(): { flavor: MqttFlavor; charge: ServiceOperation } {
  const charge = factory.createServiceOperation();
  charge.name = 'charge';
  charge.returnType = 'double';

  const flavor = factory.createMqttFlavor();
  flavor.name = 'payments-mqtt';
  flavor.brokers.push('tcp://fake:1883');
  flavor.requestTopic = 'ddsr/rpc/payments';

  const chargeFlavor = factory.createMqttOperationFlavor();
  chargeFlavor.name = 'charge';
  chargeFlavor.operation = charge;
  flavor.operationFlavors.push(chargeFlavor);

  return { flavor, charge };
}

function testHarness() {
  const broker = new FakeBroker();
  const clientFactory = () => broker.connect();
  return { broker, clientFactory };
}

describe('MQTT request/response convention (A2 Etappe 2)', () => {
  it('derives topics and qos from the flavor pair as frozen', () => {
    const { flavor } = paymentMqttFlavor();
    const opFlavor = firstOpFlavor(flavor);
    expect(requestTopicFor(flavor, opFlavor)).toBe('ddsr/rpc/payments/charge');
    expect(replyBaseFor(flavor, opFlavor)).toBe('ddsr/rpc/payments/charge/reply');
    expect(qosFor(flavor, opFlavor)).toBe(1); // model default AT_LEAST_ONCE
  });

  it('round-trips an invocation: consumer plugin -> provider server -> answer', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const { clientFactory } = testHarness();

    const server = new MqttOperationServer(flavor, {
      charge: (args) => 1000 - Number(args.amount),
    }, { clientFactory, log: () => undefined });
    await server.start();

    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });
    const opFlavor = firstOpFlavor(flavor);
    const result = await plugin.invoke(charge, { amount: 12.5, currency: 'EUR' }, flavor, opFlavor);

    expect(result).toBe(987.5);
    await plugin.close();
    await server.stop();
  });

  it('a handler failure answers with the error envelope, not a timeout', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const { clientFactory } = testHarness();
    const server = new MqttOperationServer(flavor, {
      charge: () => { throw new Error('insufficient funds'); },
    }, { clientFactory, log: () => undefined });
    await server.start();

    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });
    const opFlavor = firstOpFlavor(flavor);

    await expect(plugin.invoke(charge, { amount: 1 }, flavor, opFlavor))
      .rejects.toThrow(/insufficient funds/);
    await plugin.close();
    await server.stop();
  });

  it('two concurrent calls are answered by correlation, not by order', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const { clientFactory } = testHarness();
    const resolvers: Array<() => void> = [];
    const server = new MqttOperationServer(flavor, {
      charge: async (args) => {
        // hold the FIRST call until the second one is answered
        if (Number(args.amount) === 1) {
          await new Promise<void>(resolve => resolvers.push(resolve));
        }
        return Number(args.amount);
      },
    }, { clientFactory, log: () => undefined });
    await server.start();

    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });
    const opFlavor = firstOpFlavor(flavor);

    const first = plugin.invoke(charge, { amount: 1 }, flavor, opFlavor);
    const second = plugin.invoke(charge, { amount: 2 }, flavor, opFlavor);
    await expect(second).resolves.toBe(2);
    resolvers.forEach(r => r());
    await expect(first).resolves.toBe(1);
    await plugin.close();
    await server.stop();
  });

  it('times out with a telling error when nobody answers', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const { clientFactory } = testHarness();
    const plugin = new MqttFlavorPlugin({ clientFactory, timeoutMs: 50, log: () => undefined });
    const opFlavor = firstOpFlavor(flavor);

    await expect(plugin.invoke(charge, { amount: 1 }, flavor, opFlavor))
      .rejects.toThrow(/timed out/);
    await plugin.close();
  });

  it('canHandle answers by flavor kind', () => {
    const plugin = new MqttFlavorPlugin({ log: () => undefined });
    const { flavor } = paymentMqttFlavor();
    expect(plugin.canHandle(flavor)).toBe(true);
    const rest = factory.createRestFlavor();
    expect(plugin.canHandle(rest)).toBe(false);
  });
});
