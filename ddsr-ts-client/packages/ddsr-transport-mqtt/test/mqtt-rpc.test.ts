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
import { dataAsText, readStructured } from '@ddsr/client';
import type { MqttFlavor, MqttOperationFlavor, ServiceOperation } from '@ddsr/model';
import { MqttFlavorPlugin } from '../src/mqtt-flavor-plugin';
import { MqttOperationServer } from '../src/mqtt-operation-server';
import type { MqttRpcClientLike } from '../src/mqtt-rpc';
import { qosFor, replyBaseFor, requestTopicFor, topicSegment } from '../src/mqtt-rpc';

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
/**
 * MQTT topic matching, as a real broker does it: `+` is one level, `#`
 * is the rest. The consumer subscribes its whole reply subtree since
 * the topics gained a consumer segment, so an exact-match fake would
 * be a fake that cannot see what the code now does.
 */
function matches(filter: string, topic: string): boolean {
  const f = filter.split('/');
  const t = topic.split('/');
  for (let i = 0; i < f.length; i++) {
    if (f[i] === '#') return true;
    if (i >= t.length) return false;
    if (f[i] !== '+' && f[i] !== t[i]) return false;
  }
  return f.length === t.length;
}

class FakeBroker {
  private readonly subscriptions = new Map<string, Set<FakeClient>>();

  private readonly watchers: Array<(topic: string, payload: Uint8Array) => void> = [];

  connect(): FakeClient {
    return new FakeClient(this);
  }

  /** Everything that is published, delivered or not — what the wire sees. */
  onPublish(watcher: (topic: string, payload: Uint8Array) => void): void {
    this.watchers.push(watcher);
  }

  route(topic: string, payload: Uint8Array): void {
    for (const watcher of this.watchers) watcher(topic, payload);
    for (const [filter, clients] of this.subscriptions) {
      if (!matches(filter, topic)) continue;
      for (const client of clients) client.deliver(topic, payload);
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
  const chargeResult = factory.createParameter();
  chargeResult.name = 'result';
  chargeResult.type = 'double';
  charge.returnValue = chargeResult;

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
    // The operation states no QoS, so the flavor's default applies —
    // and since #81 that is real unset state rather than a value that
    // happens to look like silence.
    expect(qosFor(flavor, opFlavor)).toBe(1);
  });

  /**
   * #81: an operation can say that it does not override, and it can
   * also override downwards. Both need unset state, which the model
   * has (`unsettable="true"`) and the generated TypeScript carries
   * since `@emfts/codegen` 0.0.2-next.6.
   */
  it('lets an operation step down to at-most-once under a higher default', () => {
    const { flavor } = paymentMqttFlavor();
    const opFlavor = firstOpFlavor(flavor);
    expect(flavor.defaultQos).toBe('AT_LEAST_ONCE');

    opFlavor.qos = 'AT_MOST_ONCE' as typeof opFlavor.qos;

    expect(qosFor(flavor, opFlavor))
      .toBe(0);
  });

  it('takes an operation that says nothing as saying nothing, whatever the enum starts with', () => {
    const { flavor } = paymentMqttFlavor();
    const opFlavor = firstOpFlavor(flavor);
    flavor.defaultQos = 'EXACTLY_ONCE' as typeof flavor.defaultQos;

    expect(qosFor(flavor, opFlavor)).toBe(2);
  });

  it('reads a plain object as stating what it holds — it has no unset state to ask about', () => {
    const flavor = { defaultQos: 'EXACTLY_ONCE' };
    expect(qosFor(flavor as never, { qos: 'AT_MOST_ONCE' } as never)).toBe(0);
    expect(qosFor(flavor as never, {} as never)).toBe(2);
  });

  it('puts the call on the wire as a CloudEvent carrying a ServiceInvocation', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const amount = factory.createParameter();
    amount.name = 'amount';
    amount.type = 'int';
    amount.index = 0;
    charge.parameters.push(amount);
    const { broker, clientFactory } = testHarness();
    const published: Array<{ topic: string; payload: Uint8Array }> = [];
    broker.onPublish((topic, payload) => published.push({ topic, payload }));

    const plugin = new MqttFlavorPlugin({
      clientFactory, log: () => undefined, timeoutMs: 40, originLabel: 'probe',
    });
    await plugin.invoke(charge, { amount: 12 }, flavor, firstOpFlavor(flavor))
      .catch(() => undefined); // nobody answers; the request is the point

    const request = published.find(p => p.topic === 'ddsr/rpc/payments/charge');
    expect(request).toBeDefined();
    const message = readStructured(request!.payload);
    expect(message.attributes.type).toBe('org.eclipse.fennec.services.invoke');
    expect(message.attributes.source).toBe('/consumer/probe');
    expect(message.attributes.subject).toBe('charge');
    expect(message.attributes.datacontenttype).toBe('application/xml');
    expect(message.attributes.extensions.replyto)
      .toMatch(/^ddsr\/rpc\/payments\/charge\/reply\/.+/);
    const document = dataAsText(message);
    expect(document).toContain('services:ServiceInvocation');
    expect(document)
      .toContain('xsi:type="services:IntProperty" name="amount" value="12"');
    await plugin.close();
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

/**
 * #100 on the MQTT path: the encoding is the contract's choice there
 * too, and a declaration this side cannot honour is refused rather
 * than quietly ignored. The failure mode it prevents is the quiet one:
 * both ends writing XMI for a contract that says protobuf, agreeing
 * with each other and with nothing else.
 */
describe('MQTT invocation honours the declared encoding', () => {
  it('writes the encoding the contract declares when it can', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const opFlavor = firstOpFlavor(flavor);
    opFlavor.consumes.push('application/xml');
    const { broker, clientFactory } = testHarness();
    const published: Array<{ topic: string; payload: Uint8Array }> = [];
    broker.onPublish((topic, payload) => published.push({ topic, payload }));

    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined, timeoutMs: 30 });
    await plugin.invoke(charge, { amount: 1 }, flavor, opFlavor).catch(() => undefined);

    const request = published.find(p => p.topic === 'ddsr/rpc/payments/charge');
    expect(readStructured(request!.payload).attributes.datacontenttype).toBe('application/xml');
    await plugin.close();
  });

  it('refuses a call it would have to lie about', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const opFlavor = firstOpFlavor(flavor);
    opFlavor.consumes.push('application/x-protobuf');
    const { clientFactory } = testHarness();

    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined, timeoutMs: 30 });

    await expect(plugin.invoke(charge, { amount: 1 }, flavor, opFlavor))
      .rejects.toThrow(/x-protobuf.*can only write application\/xml/s);
    await plugin.close();
  });

  it('tells the caller when it cannot answer in what the contract declares', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const opFlavor = firstOpFlavor(flavor);
    opFlavor.produces.push('application/x-protobuf');
    const { clientFactory } = testHarness();
    const server = new MqttOperationServer(flavor, { charge: () => 1 },
      { clientFactory, log: () => undefined });
    await server.start();

    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined, timeoutMs: 200 });

    // The Diagnostic itself travels as XMI: it is not the operation's
    // declared result, and a caller that learns why beats one that
    // waits out its timer.
    await expect(plugin.invoke(charge, { amount: 1 }, flavor, opFlavor))
      .rejects.toThrow(/x-protobuf/);

    await plugin.close();
    await server.stop();
  });
});

/**
 * Who may read what. The reply subtree carries the consumer's name
 * because that is the only thing a broker ACL can be written against —
 * an unguessable id separates by obscurity, which is not a separation
 * a deployment can enforce.
 */
describe('MQTT reply isolation', () => {
  it('puts a consumer\'s answers under its own subtree', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const { broker, clientFactory } = testHarness();
    const published: Array<{ topic: string; payload: Uint8Array }> = [];
    broker.onPublish((topic, payload) => published.push({ topic, payload }));

    const plugin = new MqttFlavorPlugin({
      clientFactory, log: () => undefined, timeoutMs: 40, originLabel: 'probe-a',
    });
    await plugin.invoke(charge, { amount: 1 }, flavor, firstOpFlavor(flavor)).catch(() => undefined);

    const request = published.find(p => p.topic === 'ddsr/rpc/payments/charge');
    expect(readStructured(request!.payload).attributes.extensions.replyto)
      .toMatch(/^ddsr\/rpc\/payments\/charge\/reply\/probe-a\/.+/);
    await plugin.close();
  });

  it('a name with a wildcard in it cannot claim a subtree it was not given', () => {
    expect(topicSegment('a/#')).toBe('a__');
    expect(topicSegment('+')).toBe('_');
    expect(topicSegment(undefined)).toBe('anonymous');
  });

  it('two consumers of one provider do not share a reply subtree', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const { broker, clientFactory } = testHarness();
    const published: Array<{ topic: string; payload: Uint8Array }> = [];
    broker.onPublish((topic, payload) => published.push({ topic, payload }));
    const server = new MqttOperationServer(flavor, {
      charge: (args) => 1000 - Number(args.amount),
    }, { clientFactory, log: () => undefined });
    await server.start();

    const one = new MqttFlavorPlugin({ clientFactory, log: () => undefined, originLabel: 'one' });
    const two = new MqttFlavorPlugin({ clientFactory, log: () => undefined, originLabel: 'two' });
    const [first, second] = await Promise.all([
      one.invoke(charge, { amount: 1 }, flavor, firstOpFlavor(flavor)),
      two.invoke(charge, { amount: 2 }, flavor, firstOpFlavor(flavor)),
    ]);

    expect(first).toBe(999);
    expect(second).toBe(998);
    const answers = published.filter(p => p.topic.includes('/reply/'));
    expect(answers.some(p => p.topic.startsWith('ddsr/rpc/payments/charge/reply/one/'))).toBe(true);
    expect(answers.some(p => p.topic.startsWith('ddsr/rpc/payments/charge/reply/two/'))).toBe(true);

    await one.close();
    await two.close();
    await server.stop();
  });

  it('several calls of one consumer share the subscription and are told apart by correlation', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const { clientFactory } = testHarness();
    const server = new MqttOperationServer(flavor, {
      charge: async (args) => {
        // Answered out of order on purpose: the correlation decides, not
        // the order and not a topic per call.
        await new Promise(resolve => setTimeout(resolve, Number(args.amount) === 1 ? 30 : 1));
        return 1000 - Number(args.amount);
      },
    }, { clientFactory, log: () => undefined });
    await server.start();

    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined, originLabel: 'busy' });
    const [slow, quick] = await Promise.all([
      plugin.invoke(charge, { amount: 1 }, flavor, firstOpFlavor(flavor)),
      plugin.invoke(charge, { amount: 2 }, flavor, firstOpFlavor(flavor)),
    ]);

    expect(slow).toBe(999);
    expect(quick).toBe(998);
    await plugin.close();
    await server.stop();
  });
});
