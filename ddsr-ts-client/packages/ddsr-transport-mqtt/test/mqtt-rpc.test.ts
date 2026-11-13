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
import { DdsrServiceError, dataAsText, readStructured } from '@ddsr/client';
import type { MessagingFlavor, MessagingOperationFlavor, ServiceOperation } from '@ddsr/model';
import { MqttFlavorPlugin } from '../src/mqtt-flavor-plugin';
import { MqttOperationServer } from '../src/mqtt-operation-server';
import type { MqttRpcClientLike } from '../src/mqtt-rpc';
import {
  qosFor,
  replyBaseFor,
  namedReplyBaseFor,
  requestTopicFor,
  topicSegment,
  tlsConnectOptions,
  encodeRequest,
} from '../src/mqtt-rpc';
import { encodeControl } from '../src/messaging-session';

function firstOpFlavor(flavor: MessagingFlavor): MessagingOperationFlavor {
  const list = flavor.operationFlavors as unknown as
    { get?: (i: number) => MessagingOperationFlavor } & Iterable<MessagingOperationFlavor>;
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

function paymentMqttFlavor(): { flavor: MessagingFlavor; charge: ServiceOperation } {
  const charge = factory.createServiceOperation();
  charge.name = 'charge';
  const chargeResult = factory.createParameter();
  chargeResult.name = 'result';
  chargeResult.type = 'double';
  charge.returnValue = chargeResult;

  const flavor = factory.createMessagingFlavor();
  flavor.name = 'payments-mqtt';
  flavor.brokers.push('tcp://fake:1883');
  flavor.channel = 'ddsr/rpc/payments';

  const chargeFlavor = factory.createMessagingOperationFlavor();
  chargeFlavor.name = 'charge';
  chargeFlavor.operation = charge;
  // The flavor names where answers to this operation go — a tree a
  // broker's ACL can be written against.
  chargeFlavor.replyChannel = 'ddsr/rpc/payments/charge/reply';
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
    expect(namedReplyBaseFor(flavor, opFlavor)).toBe('ddsr/rpc/payments/charge/reply');
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

  it('answers under the node\'s name and connects with its certificate (SECURITY.md §7d)', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const { broker, clientFactory } = testHarness();
    const published: Array<{ topic: string; payload: Uint8Array }> = [];
    broker.onPublish((topic, payload) => published.push({ topic, payload }));
    const handed: unknown[] = [];
    const tls = { pfx: Buffer.from('not a real store'), passphrase: 'changeit' };

    const plugin = new MqttFlavorPlugin({
      clientFactory: (url, clientId, given) => { handed.push(given); return clientFactory(); },
      log: () => undefined, timeoutMs: 40, originLabel: 'probe', consumer: 'java-client', tls,
    });
    await plugin.invoke(charge, {}, flavor, firstOpFlavor(flavor)).catch(() => undefined);

    expect(handed).toEqual([tls]);
    const request = published.find(p => p.topic === 'ddsr/rpc/payments/charge');
    expect(readStructured(request!.payload).attributes.extensions.replyto)
      .toMatch(/^ddsr\/rpc\/payments\/charge\/reply\/java-client\//);
    await plugin.close();
  });

  it('hands mqtt.js the certificate, its password and the authorities, and nothing unset', () => {
    expect(tlsConnectOptions(undefined)).toEqual({});
    expect(tlsConnectOptions({ pfx: Buffer.from('p12'), passphrase: 'pw', ca: 'PEM' }))
      .toEqual({ pfx: Buffer.from('p12'), passphrase: 'pw', ca: 'PEM', rejectUnauthorized: true });
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

  // #185: MQTT has no status to bind an error to. The answer carries a
  // Diagnostic, and its code is the declared error it is.
  it('a declared error travels under its code and is told to the caller', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const declared = factory.createServiceException();
    declared.name = 'InsufficientFunds';
    declared.type = 'payment.InsufficientFunds';
    const code = factory.createIntProperty();
    code.name = 'code';
    code.value = 402;
    declared.properties.push(code);
    charge.exceptions.push(declared);

    class InsufficientFundsException extends Error {}
    const { clientFactory } = testHarness();
    const server = new MqttOperationServer(flavor, {
      charge: () => { throw new InsufficientFundsException('only 3.50 left'); },
    }, { clientFactory, log: () => undefined });
    await server.start();

    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });
    const failure = await plugin.invoke(charge, { amount: 5 }, flavor, firstOpFlavor(flavor))
      .then(() => undefined, (error: unknown) => error);

    expect(failure).toBeInstanceOf(DdsrServiceError);
    expect((failure as DdsrServiceError).exception).toBe(declared);
    expect((failure as DdsrServiceError).message).toBe('only 3.50 left');
    await plugin.close();
    await server.stop();
  });

  it('an error the contract does not declare stays a plain failure', async () => {
    const { flavor, charge } = paymentMqttFlavor();
    const declared = factory.createServiceException();
    declared.name = 'InsufficientFunds';
    declared.type = 'payment.InsufficientFunds';
    const code = factory.createIntProperty();
    code.name = 'code';
    code.value = 402;
    declared.properties.push(code);
    charge.exceptions.push(declared);

    const { clientFactory } = testHarness();
    const server = new MqttOperationServer(flavor, {
      charge: () => { throw new RangeError('boom'); },
    }, { clientFactory, log: () => undefined });
    await server.start();

    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });
    const failure = await plugin.invoke(charge, { amount: 5 }, flavor, firstOpFlavor(flavor))
      .then(() => undefined, (error: unknown) => error);

    expect(failure).not.toBeInstanceOf(DdsrServiceError);
    expect(String(failure)).toMatch(/boom/);
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

  it('canHandle answers by the protocol the flavor speaks', () => {
    const plugin = new MqttFlavorPlugin({ log: () => undefined });
    const { flavor } = paymentMqttFlavor();
    expect(plugin.canHandle(flavor)).toBe(true);
    const rest = factory.createRestFlavor();
    expect(plugin.canHandle(rest)).toBe(false);

    // One class for every protocol: an AMQP flavor is a MessagingFlavor
    // too, and not for a plugin built for MQTT — by its kind, or by
    // where it says its broker is. The same plugin built for AMQP takes
    // it and leaves the MQTT one (#138); a consumer says the kinds it
    // speaks, one per plugin.
    const amqp = factory.createMessagingFlavor();
    amqp.brokers.push('amqp://rabbit:5672');
    expect(plugin.canHandle(amqp)).toBe(false);
    (amqp as Record<string, unknown>).kind = 'AMQP';
    expect(plugin.canHandle(amqp)).toBe(false);

    const overAmqp = new MqttFlavorPlugin({ protocol: 'AMQP', log: () => undefined });
    expect(overAmqp.flavorKind).toBe('AMQP');
    expect(overAmqp.canHandle(amqp)).toBe(true);
    expect(overAmqp.canHandle(flavor)).toBe(false);
  });

  it('listens on a tree of its own where the flavor names no place for answers', () => {
    const { flavor } = paymentMqttFlavor();
    const opFlavor = firstOpFlavor(flavor);
    opFlavor.replyChannel = undefined as unknown as string;

    // A statement, not a gap: the flavor leaves the reply channel to the
    // consumer, and the call names it — so the provider needs no rule.
    expect(namedReplyBaseFor(flavor, opFlavor)).toBeUndefined();
    expect(replyBaseFor(flavor, opFlavor)).toBe('ddsr/reply');

    flavor.replyChannel = 'ddsr/rpc/answers/';
    expect(replyBaseFor(flavor, opFlavor)).toBe('ddsr/rpc/answers');
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

// Parameter.delivery = STREAM: the answer to one call is many messages -
// one per value, and one marked complete that ends it. The same names
// Gecko Messaging's reply-to-many uses, so a Java provider answers a
// TS consumer and the other way round.
describe('a stream a service returns travels over the messaging broker', () => {
  function tickerFlavor(named = true): { flavor: MessagingFlavor; count: ServiceOperation } {
    const count = factory.createServiceOperation();
    count.name = 'count';
    const ticks = factory.createParameter();
    ticks.name = 'result';
    ticks.type = 'int';
    ticks.upperBound = -1;
    ticks.delivery = 'STREAM';
    count.returnValue = ticks;

    const flavor = factory.createMessagingFlavor();
    flavor.name = 'ticker-mqtt';
    flavor.brokers.push('tcp://fake:1883');
    flavor.channel = 'ddsr/rpc/ticker';
    const countFlavor = factory.createMessagingOperationFlavor();
    countFlavor.name = 'count';
    countFlavor.operation = count;
    if (named) countFlavor.replyChannel = 'ddsr/rpc/ticker/count/reply';
    flavor.operationFlavors.push(countFlavor);
    return { flavor, count };
  }

  async function* upTo(n: number): AsyncGenerator<number> {
    for (let i = 1; i <= n; i++) yield i;
  }

  async function collect(values: unknown): Promise<unknown[]> {
    const seen: unknown[] = [];
    for await (const value of values as AsyncIterable<unknown>) seen.push(value);
    return seen;
  }

  for (const named of [true, false]) {
    it(`hands the caller every value and then ends (${named ? 'the flavor names' : 'the consumer brings'} the reply tree)`,
      async () => {
        const { flavor, count } = tickerFlavor(named);
        const { clientFactory } = testHarness();
        const server = new MqttOperationServer(flavor, {
          count: (args) => upTo(Number(args.upTo)),
        }, { clientFactory, log: () => undefined });
        await server.start();
        const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });

        const values = await plugin.invoke(count, { upTo: 5 }, flavor, firstOpFlavor(flavor));

        expect(await collect(values)).toEqual([1, 2, 3, 4, 5]);
        await plugin.close();
        await server.stop();
      });
  }

  it('travels in a session: accepted, ready, one message per value, then closed', async () => {
    const { flavor, count } = tickerFlavor();
    const { broker, clientFactory } = testHarness();
    const seen: string[] = [];
    const topics: Record<string, string> = {};
    broker.onPublish((topic, payload) => {
      const message = readStructured(payload).attributes;
      const kind = message.extensions.replysession ?? (message.type.endsWith('invoke.reply') ? 'value' : 'call');
      seen.push(kind);
      topics[kind] = topic;
    });
    const server = new MqttOperationServer(flavor, { count: () => upTo(2) }, { clientFactory, log: () => undefined });
    await server.start();
    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });

    expect(await collect(await plugin.invoke(count, {}, flavor, firstOpFlavor(flavor)))).toEqual([1, 2]);

    expect(seen.filter(kind => kind !== 'ping')).toEqual(['open', 'accept', 'ready', 'value', 'value', 'close']);
    // Both sides in trees a broker's ACL can be written against
    // (org.gecko.messaging#31): the consumer's subtree under the reply
    // tree the flavor names, and the provider's below the request channel.
    expect(topics.value).toMatch(/^ddsr\/rpc\/ticker\/count\/reply\/[^/]+\/session\/[^/]+$/);
    expect(topics.ready).toMatch(/^ddsr\/rpc\/ticker\/count\/session\/[^/]+$/);
    await plugin.close();
    await server.stop();
  });

  it('stops the provider\'s stream when the caller leaves', async () => {
    const { flavor, count } = tickerFlavor();
    const { clientFactory } = testHarness();
    let produced = 0;
    let stopped!: () => void;
    const providerStopped = new Promise<void>(resolve => { stopped = resolve; });
    const server = new MqttOperationServer(flavor, {
      count: async function* () {
        try {
          for (;;) {
            produced++;
            yield produced;
            await new Promise(resolve => setTimeout(resolve, 5));
          }
        } finally {
          stopped();
        }
      },
    }, { clientFactory, log: () => undefined });
    await server.start();
    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });

    const taken: unknown[] = [];
    for await (const value of await plugin.invoke(count, {}, flavor, firstOpFlavor(flavor)) as AsyncIterable<unknown>) {
      taken.push(value);
      if (taken.length === 3) break;
    }
    await providerStopped;

    expect(taken).toEqual([1, 2, 3]);
    const atStop = produced;
    await new Promise(resolve => setTimeout(resolve, 50));
    expect(produced).toBe(atStop);
    await plugin.close();
    await server.stop();
  });

  it('ends a session whose caller went silent, after the lease', async () => {
    const { flavor, count } = tickerFlavor();
    const { broker, clientFactory } = testHarness();
    let stopped!: () => void;
    const providerStopped = new Promise<void>(resolve => { stopped = resolve; });
    const server = new MqttOperationServer(flavor, {
      count: async function* () {
        try {
          for (let i = 1; ; i++) {
            yield i;
            await new Promise(resolve => setTimeout(resolve, 20));
          }
        } finally {
          stopped();
        }
      },
    }, { clientFactory, log: () => undefined });
    await server.start();
    // A caller that opens a session by hand, says it is ready and then
    // never speaks again - no pings, no close.
    const silent = broker.connect();
    const replyChannel = 'osgi/messaging/reply/silent/1';
    await silent.subscribeAsync(replyChannel);
    silent.on('message', (_topic, payload) => {
      const message = readStructured(payload).attributes.extensions;
      if (message.replysession === 'accept') {
        void silent.publishAsync(message.replysessionchannel!,
          encodeControl('session-1', '/consumer/silent', 'ready'));
      }
    });
    const open = encodeRequest(count, {}, replyChannel, '/consumer/silent', firstOpFlavor(flavor),
      { correlationid: 'session-1', replysession: 'open', replysessionlease: '1000' });
    await silent.publishAsync(requestTopicFor(flavor, firstOpFlavor(flavor)), open.payload);

    await providerStopped;
    await server.stop();
  }, 10_000);

  it('gives up a stream the caller does not keep up with, and says so', async () => {
    const { flavor, count } = tickerFlavor();
    const { clientFactory } = testHarness();
    const server = new MqttOperationServer(flavor, { count: () => upTo(20) }, { clientFactory, log: () => undefined });
    await server.start();
    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined, streamBuffer: { size: 4 } });

    const values = (await plugin.invoke(count, {}, flavor, firstOpFlavor(flavor)) as AsyncIterable<unknown>)[Symbol.asyncIterator]();
    await values.next();
    await new Promise(resolve => setTimeout(resolve, 100));
    const failure = await (async () => { for (;;) { if ((await values.next()).done) return undefined; } })()
      .then(() => undefined, (error: unknown) => error);

    expect(String(failure)).toMatch(/more than 4 values the caller had not taken/);
    await plugin.close();
    await server.stop();
  });

  it('keeps the latest where the caller asks for that, and drops the oldest', async () => {
    const { flavor, count } = tickerFlavor();
    const { clientFactory } = testHarness();
    const server = new MqttOperationServer(flavor, { count: () => upTo(20) }, { clientFactory, log: () => undefined });
    await server.start();
    const plugin = new MqttFlavorPlugin({
      clientFactory, log: () => undefined, streamBuffer: { size: 4, policy: 'DISCARD_OLDEST' },
    });

    const values = (await plugin.invoke(count, {}, flavor, firstOpFlavor(flavor)) as AsyncIterable<unknown>)[Symbol.asyncIterator]();
    const first = await values.next();
    await new Promise(resolve => setTimeout(resolve, 100));
    const rest: unknown[] = [];
    for (;;) {
      const next = await values.next();
      if (next.done) break;
      rest.push(next.value);
    }

    expect(first.value).toBe(1);
    expect(rest).toEqual([17, 18, 19, 20]);
    await plugin.close();
    await server.stop();
  });

  it('sends nothing before the first value is asked for', async () => {
    const { flavor, count } = tickerFlavor();
    const { broker, clientFactory } = testHarness();
    let published = 0;
    broker.onPublish(() => { published++; });
    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });

    await plugin.invoke(count, {}, flavor, firstOpFlavor(flavor));

    expect(published).toBe(0);
    await plugin.close();
  });

  it('ends the iteration with the failure, after the values that came before it', async () => {
    const { flavor, count } = tickerFlavor();
    const { clientFactory } = testHarness();
    const server = new MqttOperationServer(flavor, {
      count: async function* () {
        yield 1;
        yield 2;
        throw new Error('the sensor went away');
      },
    }, { clientFactory, log: () => undefined });
    await server.start();
    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });
    const seen: unknown[] = [];

    const values = await plugin.invoke(count, {}, flavor, firstOpFlavor(flavor)) as AsyncIterable<unknown>;
    const failure = await (async () => {
      for await (const value of values) seen.push(value);
    })().then(() => undefined, (error: unknown) => error);

    expect(seen).toEqual([1, 2]);
    expect(String(failure)).toMatch(/the sensor went away/);
    await plugin.close();
    await server.stop();
  });

  it('tells a declared error that ends a stream as the declared error', async () => {
    const { flavor, count } = tickerFlavor();
    const declared = factory.createServiceException();
    declared.name = 'OutOfRange';
    declared.type = 'ticker.OutOfRange';
    const code = factory.createIntProperty();
    code.name = 'code';
    code.value = 416;
    declared.properties.push(code);
    count.exceptions.push(declared);
    class OutOfRangeException extends Error {}
    const { clientFactory } = testHarness();
    const server = new MqttOperationServer(flavor, {
      count: async function* () {
        yield 1;
        throw new OutOfRangeException('no more than one');
      },
    }, { clientFactory, log: () => undefined });
    await server.start();
    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });

    const failure = await collect(await plugin.invoke(count, {}, flavor, firstOpFlavor(flavor)))
      .then(() => undefined, (error: unknown) => error);

    expect(failure).toBeInstanceOf(DdsrServiceError);
    expect((failure as DdsrServiceError).exception).toBe(declared);
    expect((failure as DdsrServiceError).message).toBe('no more than one');
    await plugin.close();
    await server.stop();
  });

  it('fails a stream whose provider does not accept the session in time', async () => {
    const { flavor, count } = tickerFlavor();
    const { clientFactory } = testHarness();
    const plugin = new MqttFlavorPlugin({ clientFactory, timeoutMs: 50, log: () => undefined });

    await expect(collect(await plugin.invoke(count, {}, flavor, firstOpFlavor(flavor))))
      .rejects.toThrow(/did not accept the session within 50ms/);
    await plugin.close();
  });

  it('keeps two streams of one consumer apart by correlation', async () => {
    const { flavor, count } = tickerFlavor();
    const { clientFactory } = testHarness();
    const server = new MqttOperationServer(flavor, {
      count: (args) => upTo(Number(args.upTo)),
    }, { clientFactory, log: () => undefined });
    await server.start();
    const plugin = new MqttFlavorPlugin({ clientFactory, log: () => undefined });
    const opFlavor = firstOpFlavor(flavor);

    const [three, two] = await Promise.all([
      plugin.invoke(count, { upTo: 3 }, flavor, opFlavor).then(collect),
      plugin.invoke(count, { upTo: 2 }, flavor, opFlavor).then(collect),
    ]);

    expect(three).toEqual([1, 2, 3]);
    expect(two).toEqual([1, 2]);
    await plugin.close();
    await server.stop();
  });
});
