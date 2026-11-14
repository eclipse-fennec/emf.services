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
import type { MessagingFlavor, ServiceOperation } from '@ddsr/model';
import { MqttFlavorPlugin } from '../src/mqtt-flavor-plugin';
import { MqttOperationServer } from '../src/mqtt-operation-server';
import type { MqttRpcClientLike } from '../src/mqtt-rpc';

// #252: one source, many receivers, each protected by a buffer of its own.

class Broker {
  private readonly subscriptions = new Map<string, Set<Client>>();

  connect(): Client {
    return new Client(this);
  }

  route(topic: string, payload: Uint8Array): void {
    for (const client of this.subscriptions.get(topic) ?? []) client.deliver(topic, payload);
  }

  subscribe(topic: string, client: Client): void {
    let set = this.subscriptions.get(topic);
    if (!set) this.subscriptions.set(topic, set = new Set());
    set.add(client);
  }

  unsubscribe(topic: string, client: Client): void {
    this.subscriptions.get(topic)?.delete(client);
  }

  subscribers(topic: string): number {
    return this.subscriptions.get(topic)?.size ?? 0;
  }
}

class Client implements MqttRpcClientLike {
  private readonly listeners: Array<(topic: string, payload: Uint8Array) => void> = [];

  constructor(private readonly broker: Broker) {}

  on(_event: 'message', listener: (topic: string, payload: Uint8Array) => void): unknown {
    this.listeners.push(listener);
    return this;
  }

  async subscribeAsync(topic: string): Promise<unknown> {
    this.broker.subscribe(topic, this);
    return undefined;
  }

  async unsubscribeAsync(topic: string): Promise<unknown> {
    this.broker.unsubscribe(topic, this);
    return undefined;
  }

  async publishAsync(topic: string, payload: Uint8Array | string): Promise<unknown> {
    const bytes = typeof payload === 'string' ? new TextEncoder().encode(payload) : payload;
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

function sensor(overflow: 'FAIL' | 'DISCARD_OLDEST'): { flavor: MessagingFlavor; readings: ServiceOperation } {
  const f = DDSRFactory.eINSTANCE;
  const readings = f.createServiceOperation();
  readings.name = 'readings';
  const result = f.createParameter();
  result.name = 'result';
  result.type = 'int';
  result.upperBound = -1;
  result.delivery = 'BROADCAST';
  result.overflow = overflow;
  readings.returnValue = result;
  const flavor = f.createMessagingFlavor();
  flavor.name = 'sensor-mqtt';
  flavor.brokers.push('tcp://fake:1883');
  flavor.channel = 'ddsr/rpc/sensor';
  const op = f.createMessagingOperationFlavor();
  op.name = 'readings';
  op.operation = readings;
  flavor.operationFlavors.push(op);
  return { flavor, readings };
}

const until = async (condition: () => boolean) => {
  for (let i = 0; i < 200 && !condition(); i++) await new Promise(resolve => setTimeout(resolve, 5));
};

async function scene(overflow: 'FAIL' | 'DISCARD_OLDEST') {
  const broker = new Broker();
  const { flavor, readings } = sensor(overflow);
  let asked = 0;
  let release!: () => void;
  const go = new Promise<void>(resolve => { release = resolve; });
  const server = new MqttOperationServer(flavor, {
    readings: async function* () {
      asked++;
      await go;
      for (let i = 0; i < 10; i++) yield i;
    } as never,
  }, { clientFactory: () => broker.connect(), log: () => undefined });
  await server.start();
  const receiver = (size: number) => new MqttFlavorPlugin({
    clientFactory: () => broker.connect(), streamBuffer: { size, policy: 'FAIL' }, log: () => undefined,
  });
  const fast = await receiver(256).invoke(readings, {}, flavor, flavor.operationFlavors[0]) as AsyncIterable<unknown>;
  const slow = (await receiver(2).invoke(readings, {}, flavor, flavor.operationFlavors[0]) as AsyncIterable<unknown>)
    [Symbol.asyncIterator]();
  const fastRead: unknown[] = [];
  const fastDone = (async () => { for await (const value of fast) fastRead.push(value); })();
  const slowFirst = slow.next();
  await until(() => broker.subscribers('ddsr/rpc/sensor/readings') === 2);
  release();
  await fastDone;
  return { slow, slowFirst, fastRead, asked: () => asked, server };
}

describe('a broadcast over messaging', () => {
  it('latest wins: the fast receiver gets everything, the slow one what its buffer held', async () => {
    const { slow, slowFirst, fastRead, asked, server } = await scene('DISCARD_OLDEST');
    const slowRead = [(await slowFirst).value];
    for (let next = await slow.next(); !next.done; next = await slow.next()) slowRead.push(next.value);

    expect(fastRead).toEqual([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]);
    expect(slowRead[0]).toBe(0);
    expect(slowRead.length).toBeLessThanOrEqual(3);
    expect(slowRead[slowRead.length - 1]).toBe(9);
    expect(asked()).toBe(1);
    await server.stop();
  });

  it('must not lose: the slow receiver fails, after what it holds, and the fast one goes on', async () => {
    const { slow, slowFirst, fastRead, server } = await scene('FAIL');
    const slowRead = [(await slowFirst).value];
    await expect((async () => {
      for (let next = await slow.next(); !next.done; next = await slow.next()) slowRead.push(next.value);
    })()).rejects.toThrow(/fell behind/);

    expect(fastRead).toEqual([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]);
    expect(slowRead).toEqual([0, 1, 2]);
    await server.stop();
  });
});
