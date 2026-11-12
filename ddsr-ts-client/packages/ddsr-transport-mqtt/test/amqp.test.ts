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
import type { MessagingFlavor, MessagingOperationFlavor, ServiceOperation } from '@ddsr/model';
import { AmqpClient, isAmqpUrl } from '../src/amqp-client';
import type { AmqpChannelLike, AmqpConnectionLike } from '../src/amqp-client';
import { MqttEventSource } from '../src/mqtt-event-source';
import { MqttFlavorPlugin } from '../src/mqtt-flavor-plugin';
import { MqttOperationServer } from '../src/mqtt-operation-server';
import {
  isAmqpFlavor, replySubtreeFor, replyTopicFor, requestTopicFor, separatorFor, topicSegment,
} from '../src/mqtt-rpc';

const factory = DDSRFactory.eINSTANCE;

/** An AMQP topic exchange: `.` separates words, `#` is any number of them, `*` exactly one. */
function matches(pattern: string, key: string): boolean {
  const p = pattern.split('.');
  const k = key.split('.');
  for (let i = 0; i < p.length; i++) {
    if (p[i] === '#') return true;
    if (i >= k.length) return false;
    if (p[i] !== '*' && p[i] !== k[i]) return false;
  }
  return p.length === k.length;
}

/** One exchange, the queues bound to it and what was published - the broker's side of amqplib. */
class FakeExchange {
  readonly published: Array<{ exchange: string; key: string }> = [];
  readonly queues = new Map<string, {
    patterns: Set<string>;
    deliver?: (message: { fields: { routingKey: string }; content: Uint8Array }) => void;
  }>();
  private next = 0;

  channel(): AmqpChannelLike {
    return {
      assertQueue: async () => {
        const queue = `amq.gen-${++this.next}`;
        this.queues.set(queue, { patterns: new Set() });
        return { queue };
      },
      bindQueue: async (queue, _exchange, pattern) => { this.queues.get(queue)!.patterns.add(pattern); },
      unbindQueue: async (queue, _exchange, pattern) => { this.queues.get(queue)!.patterns.delete(pattern); },
      consume: async (queue, onMessage) => { this.queues.get(queue)!.deliver = onMessage; },
      publish: (exchange, key, content) => {
        this.published.push({ exchange, key });
        queueMicrotask(() => {
          for (const queue of this.queues.values()) {
            if ([...queue.patterns].some(pattern => matches(pattern, key))) {
              queue.deliver?.({ fields: { routingKey: key }, content });
            }
          }
        });
        return true;
      },
    };
  }
}

/** A recovering connection the test connects and disconnects by hand. */
class FakeConnection implements AmqpConnectionLike {
  private readonly listeners = new Map<string, Array<(value: never) => void>>();
  closed = false;

  constructor(private readonly exchange: FakeExchange) {}

  on(event: string, listener: (value: never) => void): unknown {
    this.listeners.set(event, [...(this.listeners.get(event) ?? []), listener]);
    return this;
  }

  connect(): void {
    const model = { createChannel: async () => this.exchange.channel() };
    for (const listener of this.listeners.get('connect') ?? []) listener(model as never);
  }

  disconnect(): void {
    for (const listener of this.listeners.get('disconnect') ?? []) listener(new Error('gone') as never);
  }

  async close(): Promise<void> {
    this.closed = true;
  }
}

async function connected(exchange: FakeExchange, name = 'amq.topic'): Promise<{ client: AmqpClient; connection: FakeConnection }> {
  const connection = new FakeConnection(exchange);
  const client = new AmqpClient(connection, name);
  connection.connect();
  await client.whenReady();
  return { client, connection };
}

function paymentAmqpFlavor(): { flavor: MessagingFlavor; charge: ServiceOperation; opFlavor: MessagingOperationFlavor } {
  const charge = factory.createServiceOperation();
  charge.name = 'charge';
  const result = factory.createParameter();
  result.name = 'result';
  result.type = 'double';
  charge.returnValue = result;

  const flavor = factory.createMessagingFlavor();
  flavor.name = 'payments-amqp';
  flavor.brokers.push('amqp://fake:5672');
  flavor.channel = 'ddsr.rpc.payments';

  const opFlavor = factory.createMessagingOperationFlavor();
  opFlavor.name = 'charge';
  opFlavor.operation = charge;
  flavor.operationFlavors.push(opFlavor);
  return { flavor, charge, opFlavor };
}

describe('a channel is spelled the way the flavor\'s protocol spells it (#138)', () => {
  it('reads the protocol off the kind, or off the broker for a flavor without one', () => {
    const { flavor } = paymentAmqpFlavor();
    expect(isAmqpFlavor(flavor)).toBe(true);
    expect(separatorFor(flavor)).toBe('.');
    expect(isAmqpFlavor({ kind: 'MQTT', brokers: ['amqp://h'] })).toBe(false);
    expect(isAmqpFlavor({ kind: 'AMQP', brokers: ['tcp://h'] })).toBe(true);
    expect(separatorFor({ brokers: ['tcp://h:1883'] })).toBe('/');
    expect(isAmqpUrl('AMQPS://h:5671/vhost')).toBe(true);
    expect(isAmqpUrl('mqtt://h:1883')).toBe(false);
  });

  it('writes an operation as a routing key under the flavor\'s channel', () => {
    const { flavor, opFlavor } = paymentAmqpFlavor();
    expect(requestTopicFor(flavor, opFlavor)).toBe('ddsr.rpc.payments.charge');
  });

  it('gives a consumer a subtree of its own, in words instead of levels', () => {
    const { flavor, opFlavor } = paymentAmqpFlavor();
    expect(replySubtreeFor(flavor, opFlavor, 'node a')).toBe('ddsr.reply.node_a');
    expect(replyTopicFor(flavor, opFlavor, 'node-a', 'r-1')).toBe('ddsr.reply.node-a.r-1');
  });

  it('keeps the separators and wildcards of both protocols out of a name', () => {
    expect(topicSegment('a.b/c#d*e+f')).toBe('a_b_c_d_e_f');
  });
});

describe('the AMQP client behind the seam of the MQTT pieces', () => {
  it('binds what is subscribed to one exclusive queue and hands on the routing key', async () => {
    const exchange = new FakeExchange();
    const { client } = await connected(exchange);
    const heard: string[] = [];
    client.on('message', (topic, payload) => heard.push(`${topic}=${new TextDecoder().decode(payload)}`));

    await client.subscribeAsync('ddsr.events.#');
    await client.publishAsync('ddsr.events.Payment', 'registered');
    await client.publishAsync('ddsr.rpc.payments.charge', 'not for this queue');
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(heard).toEqual(['ddsr.events.Payment=registered']);
    expect(exchange.published.map(p => p.exchange)).toEqual(['amq.topic', 'amq.topic']);
    expect(exchange.queues.size).toBe(1);
  });

  it('publishes to the exchange the flavor names', async () => {
    const exchange = new FakeExchange();
    const { client } = await connected(exchange, 'fennec.calls');

    await client.publishAsync('ddsr.rpc.payments.charge', new Uint8Array([1]));

    expect(exchange.published).toEqual([{ exchange: 'fennec.calls', key: 'ddsr.rpc.payments.charge' }]);
  });

  it('binds again on a queue of its own after the connection came back', async () => {
    const exchange = new FakeExchange();
    const { client, connection } = await connected(exchange);
    const events: string[] = [];
    client.on('connect', () => events.push('connect'));
    client.on('close', () => events.push('close'));
    await client.subscribeAsync('ddsr.events.#');
    await new Promise(resolve => setTimeout(resolve, 0));

    connection.disconnect();
    connection.connect();
    await new Promise(resolve => setTimeout(resolve, 0));

    // The first 'connect' is told to a listener attached after the fact,
    // as mqtt.js would have told it; the second is the recovery.
    expect(events).toEqual(['connect', 'close', 'connect']);
    const queues = [...exchange.queues.values()];
    expect(queues).toHaveLength(2);
    expect([...queues[1].patterns]).toEqual(['ddsr.events.#']);
  });

  it('says so instead of losing a message while the connection is away', async () => {
    const exchange = new FakeExchange();
    const { client, connection } = await connected(exchange);
    connection.disconnect();

    await expect(client.publishAsync('ddsr.rpc.payments.charge', 'x')).rejects.toThrow(/not connected/);
  });
});

describe('a call over AMQP is the call over MQTT on another spelling', () => {
  it('reaches the provider under a routing key and comes back on the consumer\'s own', async () => {
    const exchange = new FakeExchange();
    const clientFactory = async () => (await connected(exchange)).client;
    const { flavor, charge, opFlavor } = paymentAmqpFlavor();

    const server = new MqttOperationServer(flavor, {
      charge: args => 1000 - Number(args.amount),
    }, { clientFactory, log: () => undefined });
    await server.start();
    const plugin = new MqttFlavorPlugin({ protocol: 'AMQP', clientFactory, consumer: 'node-a', log: () => undefined });
    const amount = factory.createParameter();
    amount.name = 'amount';
    amount.type = 'double';
    charge.parameters.push(amount);

    const result = await plugin.invoke(charge, { amount: 12.5 }, flavor, opFlavor);

    expect(result).toBe(987.5);
    const keys = exchange.published.map(p => p.key);
    expect(keys[0]).toBe('ddsr.rpc.payments.charge');
    expect(keys[1]).toMatch(/^ddsr\.reply\.node-a\.[0-9a-f-]+$/);
    await plugin.close();
    await server.stop();
  });
});

describe('the event source over AMQP', () => {
  it('listens under the prefix as a routing key and says what it is', () => {
    const source = new MqttEventSource({ brokerUrl: 'amqp://rabbit:5672' });

    expect(source.topicFilter()).toBe('ddsr.events.#');
    expect(source.transport).toBe('amqp');
    expect(new MqttEventSource({ brokerUrl: 'mqtt://mosquitto:1883' }).topicFilter()).toBe('ddsr/events/#');
    expect(new MqttEventSource({ brokerUrl: 'mqtt://mosquitto:1883' }).transport).toBe('mqtt');
  });
});
