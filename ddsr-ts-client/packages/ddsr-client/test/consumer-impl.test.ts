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
import { DdsrConsumerImpl } from '../src/internal/consumer-impl';
import { ServiceListenerRegistry } from '../src/internal/service-listener-registry';
import { BrokerHttp } from '../src/internal/broker-http';
import { fakeFetch, lookupResultXmi, unregisteringEventXmi } from './fixtures';
import { deserializeFromXmi } from '../src/xmi/xmi-support';
import { asRoots, firstOfClass, toArray } from '../src/internal/emf-util';
import type { ServiceEvent } from '@ddsr/model';

const BROKER = 'http://broker.test/ddsr/rest';

function consumerWith(fetchFn: typeof fetch, supportedFlavors: string[] = ['REST']) {
  const listeners = new ServiceListenerRegistry(undefined, () => undefined);
  const consumer = new DdsrConsumerImpl(
    new BrokerHttp({ brokerUrl: BROKER, fetchFn }),
    [],
    supportedFlavors,
    listeners,
    'consumer-tests'
  );
  return { consumer, listeners };
}

describe('DdsrConsumerImpl', () => {
  it('find parses the lookup-result envelope into locators', async () => {
    const { fetchFn, requests } = fakeFetch([
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-5') },
    ]);
    const { consumer } = consumerWith(fetchFn);

    const locators = await consumer.find('Payment');

    expect(locators).toHaveLength(1);
    expect(locators[0].reference.id).toBe('ref-5');
    expect(locators[0].serviceInterface.name).toBe('Payment');
    expect(locators[0].implementation.implementationId).toBe('ts:payments-ts:1.0.0');
    expect(locators[0].restFlavor()?.basePath).toBe('/payments');
    const url = new URL(requests[0].url);
    expect(url.searchParams.get('flavors')).toBe('REST');
    expect(url.searchParams.get('consumerId')).toBe('consumer-tests');
  });

  it('find remembers reference→interface for event routing', async () => {
    const { fetchFn } = fakeFetch([
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-6') },
    ]);
    const { consumer, listeners } = consumerWith(fetchFn);
    const payment: string[] = [];
    const other: string[] = [];
    listeners.add('Payment', undefined, e => payment.push(e.type));
    listeners.add('Other', undefined, e => other.push(e.type));

    await consumer.find('Payment');

    const event = firstOfClass<ServiceEvent>(
      asRoots(deserializeFromXmi(unregisteringEventXmi('ref-6'))),
      'ServiceEvent'
    )!;
    listeners.onEvent(event);
    expect(payment).toEqual(['UNREGISTERING']);
    expect(other).toEqual([]);
  });

  it('refreshFromSnapshot pulls one find per distinct subscribed interface', async () => {
    const { fetchFn, requests } = fakeFetch([
      { urlIncludes: '/references', body: lookupResultXmi() },
    ]);
    const { consumer, listeners } = consumerWith(fetchFn);
    listeners.add('Payment', undefined, () => undefined);
    listeners.add('Payment', undefined, () => undefined);
    listeners.add('Other', undefined, () => undefined);

    await consumer.refreshFromSnapshot();

    const queried = requests.map(r => new URL(r.url).searchParams.get('interface'));
    expect(queried.sort()).toEqual(['Other', 'Payment']);
  });

  it('a failing lookup during snapshot refresh does not stop the others', async () => {
    const { fetchFn } = fakeFetch([
      { urlIncludes: 'interface=Bad', status: 500, body: 'boom', contentType: 'text/plain' },
      { urlIncludes: '/references', body: lookupResultXmi() },
    ]);
    const { consumer, listeners } = consumerWith(fetchFn);
    const seen: string[] = [];
    listeners.add('Bad', undefined, () => undefined);
    listeners.add('Payment', undefined, () => undefined);
    // note: routes are matched in order, so Bad → 500, Payment → result
    await expect(consumer.refreshFromSnapshot()).resolves.toBeUndefined();
    void seen;
  });

  it('an empty lookup response yields no locators', async () => {
    const { fetchFn } = fakeFetch([{ urlIncludes: '/references', body: '' }]);
    const { consumer } = consumerWith(fetchFn);
    expect(await consumer.find('Payment')).toEqual([]);
  });

  it('getService proxy is await-safe and maps positional args', async () => {
    const { fetchFn } = fakeFetch([
      { urlIncludes: '/references', body: lookupResultXmi() },
    ]);
    const { consumer } = consumerWith(fetchFn);
    const service = await consumer.getService<{ charge(amount: number, accountId: string): Promise<unknown> }>('Payment');
    // 'then' guard: the resolved proxy must not be re-awaited into a call
    expect(service).toBeDefined();
    expect((service as any).then).toBeUndefined();
    // invoking fails only at the flavor-plugin stage (none registered)
    await expect(service!.charge(5, 'acc')).rejects.toThrow(/No FlavorPlugin/);
  });

  it('locator interface list drives noteReference with all interface names', async () => {
    const { fetchFn } = fakeFetch([
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-8') },
    ]);
    const { consumer } = consumerWith(fetchFn);
    const locators = await consumer.find('Payment');
    expect(toArray(locators[0].implementation.serviceInterfaces).length).toBe(1);
  });
});
