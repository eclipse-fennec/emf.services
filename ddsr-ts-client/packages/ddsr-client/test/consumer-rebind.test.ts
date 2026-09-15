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
import type { ServiceEvent, ServiceFlavor, ServiceOperation, ServiceOperationFlavor } from '@ddsr/model';
import { DdsrConsumerImpl } from '../src/internal/consumer-impl';
import { ServiceListenerRegistry } from '../src/internal/service-listener-registry';
import { BrokerHttp } from '../src/internal/broker-http';
import { DdsrClientError, DdsrTransportError } from '../src/api/errors';
import type { FlavorPlugin } from '../src/api/flavor-plugin';
import type { TrackedServiceLocator } from '../src/api/service-locator';
import { deserializeFromXmi } from '../src/xmi/xmi-support';
import { asRoots, firstOfClass } from '../src/internal/emf-util';
import { fakeFetch, lookupResultXmi, registeredEventXmi, unregisteringEventXmi } from './fixtures';

const BROKER = 'http://broker.test/ddsr/rest';

function eventOf(xmi: string): ServiceEvent {
  const event = firstOfClass<ServiceEvent>(asRoots(deserializeFromXmi(xmi)), 'ServiceEvent');
  if (!event) throw new Error('no ServiceEvent in fixture');
  return event;
}

function unregistering(refId: string, reason?: string): ServiceEvent {
  const xmi = unregisteringEventXmi(refId).replace(
    'type="UNREGISTERING"',
    reason ? `type="UNREGISTERING" reasonCode="${reason}"` : 'type="UNREGISTERING"'
  );
  return eventOf(xmi);
}

function bare(type: string, refId: string): ServiceEvent {
  return eventOf(unregisteringEventXmi(refId).replace('type="UNREGISTERING"', `type="${type}"`));
}

/** Java twin: ConsumerImplRebindTest (#57). Locators follow their service. */
describe('tracked locators', () => {
  /** A mutable lookup route: the "broker state" the tests change between calls. */
  function brokerWith(initialBody: string, plugins: FlavorPlugin[] = [], greedy = false) {
    const routes = [{ urlIncludes: '/references', body: initialBody }];
    const { fetchFn, requests } = fakeFetch(routes);
    const listeners = new ServiceListenerRegistry(undefined, () => undefined);
    const consumer = new DdsrConsumerImpl(
      new BrokerHttp({ brokerUrl: BROKER, fetchFn }), plugins, ['REST'], listeners, 'c1', greedy);
    return {
      consumer,
      listeners,
      requests,
      brokerNowLists(body: string) { routes[0].body = body; },
    };
  }

  const at = (refId: string, host: string) =>
    lookupResultXmi('payments-ts', refId).replace('http://localhost:9090', host);

  it('a locator from find is tracked, LIVE and bound to its interface', async () => {
    const b = brokerWith(at('ref-1', 'http://a:1'));
    const locator = (await b.consumer.find('Payment')).at(0) as TrackedServiceLocator;
    expect(locator.state).toBe('LIVE');
    expect(locator.interfaceName).toBe('Payment');
    expect(b.consumer.trackedLocatorCount()).toBe(1);
    expect(b.listeners.subscribedInterfaces()).toEqual(['Payment']);
  });

  it('MODIFIED refreshes the locator in place without a lookup', async () => {
    const b = brokerWith(at('ref-1', 'http://a:1'));
    const locator = (await b.consumer.find('Payment')).at(0) as TrackedServiceLocator;
    const lookups = b.requests.length;

    const modified = registeredEventXmi('Payment', 'ref-1')
      .replace('type="REGISTERED"', 'type="MODIFIED"')
      .replace('http://localhost:9090', 'http://a:2');
    b.listeners.onEvent(eventOf(modified));

    expect(locator.state).toBe('LIVE');
    expect(locator.reference.id).toBe('ref-1');
    expect(locator.restFlavor()?.host).toBe('http://a:2');
    expect(b.requests.length).toBe(lookups);
  });

  it('COLDIFIED is stale, not gone — the next rebind rehydrates under a fresh id', async () => {
    const b = brokerWith(at('ref-1', 'http://a:1'));
    const locator = (await b.consumer.find('Payment')).at(0) as TrackedServiceLocator;

    b.listeners.onEvent(unregistering('ref-1', 'COLDIFIED'));
    expect(locator.state).toBe('STALE');
    expect(locator.reference.id).toBe('ref-1');

    b.brokerNowLists(at('ref-1b', 'http://a:1'));
    expect(await locator.rebind(false)).toBe(true);
    expect(locator.state).toBe('LIVE');
    expect(locator.reference.id).toBe('ref-1b');
  });

  it('WITHDRAWN rebinds to another registration, or fails loudly when there is none', async () => {
    const b = brokerWith(at('ref-1', 'http://a:1'));
    const locator = (await b.consumer.find('Payment')).at(0) as TrackedServiceLocator;

    b.brokerNowLists(at('ref-2', 'http://b:1'));
    b.listeners.onEvent(unregistering('ref-1', 'WITHDRAWN'));
    expect(locator.state).toBe('REBIND');
    expect(await locator.rebind(true)).toBe(true);
    expect(locator.reference.id).toBe('ref-2');
    expect(locator.restFlavor()?.host).toBe('http://b:1');

    b.brokerNowLists(lookupResultXmi('payments-ts', 'ref-2').replace(/<references[\s\S]*?<\/references>/, '').replace(/<providers[\s\S]*?<\/providers>/, ''));
    b.listeners.onEvent(bare('RETIRED', 'ref-2'));
    await expect(locator.invoke('charge', {})).rejects.toBeInstanceOf(DdsrClientError);
  });

  it('UPGRADE_AVAILABLE is a hint for a non-greedy and a rebind for a greedy consumer', async () => {
    const lazy = brokerWith(at('ref-v1', 'http://v1:1'));
    const kept = (await lazy.consumer.find('Payment')).at(0) as TrackedServiceLocator;
    lazy.listeners.onEvent(bare('UPGRADE_AVAILABLE', 'ref-v1'));
    expect(kept.state).toBe('LIVE');

    const greedy = brokerWith(at('ref-v1', 'http://v1:1'), [], true);
    const switching = (await greedy.consumer.find('Payment')).at(0) as TrackedServiceLocator;
    greedy.listeners.onEvent(bare('UPGRADE_AVAILABLE', 'ref-v1'));
    expect(switching.state).toBe('REBIND');
  });

  it('RETIRED forgets the reference in the routing memo', async () => {
    const b = brokerWith(at('ref-1', 'http://a:1'));
    await b.consumer.find('Payment');
    expect(b.listeners.knownReferenceIds()).toContain('ref-1');
    b.listeners.onEvent(bare('RETIRED', 'ref-1'));
    expect(b.listeners.knownReferenceIds()).not.toContain('ref-1');
  });

  it('after a rebind, events for the new reference reach the locator', async () => {
    const b = brokerWith(at('ref-1', 'http://a:1'));
    const locator = (await b.consumer.find('Payment')).at(0) as TrackedServiceLocator;
    b.brokerNowLists(at('ref-2', 'http://b:1'));
    await locator.rebind(true);
    expect(locator.reference.id).toBe('ref-2');

    b.listeners.onEvent(unregistering('ref-2', 'WITHDRAWN'));
    expect(locator.state).toBe('REBIND');
    expect(b.consumer.trackedLocatorCount()).toBe(1);
  });

  describe('invocation (#59)', () => {
    /** A flavor plugin whose first calls fail like an unreachable provider. */
    function flakyPlugin(failures: number) {
      const calls: string[] = [];
      const plugin: FlavorPlugin = {
        flavorKind: 'REST',
        canHandle: () => true,
        async invoke(operation: ServiceOperation, _params, flavor: ServiceFlavor, _of: ServiceOperationFlavor) {
          calls.push(String((flavor as any).host));
          if (calls.length <= failures) throw new DdsrTransportError('connect ECONNREFUSED');
          return `ok from ${(flavor as any).host} (${operation.name})`;
        },
      };
      return { plugin, calls };
    }

    it('a transport failure rebinds away from the failed registration and retries once', async () => {
      const { plugin, calls } = flakyPlugin(1);
      const b = brokerWith(at('ref-1', 'http://a:1'), [plugin]);
      const locator = (await b.consumer.find('Payment')).at(0) as TrackedServiceLocator;
      b.brokerNowLists(at('ref-2', 'http://b:1'));

      const result = await locator.invoke('charge', { amount: 1, accountId: 'x' });

      expect(result).toBe('ok from http://b:1 (charge)');
      expect(calls).toEqual(['http://a:1', 'http://b:1']);
      expect(locator.reference.id).toBe('ref-2');
    });

    it("a second transport failure is the caller's", async () => {
      const { plugin, calls } = flakyPlugin(2);
      const b = brokerWith(at('ref-1', 'http://a:1'), [plugin]);
      const locator = (await b.consumer.find('Payment')).at(0) as TrackedServiceLocator;
      b.brokerNowLists(at('ref-2', 'http://b:1'));

      await expect(locator.invoke('charge', {})).rejects.toBeInstanceOf(DdsrTransportError);
      expect(calls).toHaveLength(2);
    });

    it('with nothing else registered the failure propagates without a retry', async () => {
      const { plugin, calls } = flakyPlugin(1);
      const b = brokerWith(at('ref-1', 'http://a:1'), [plugin]);
      const locator = (await b.consumer.find('Payment')).at(0) as TrackedServiceLocator;

      await expect(locator.invoke('charge', {})).rejects.toBeInstanceOf(DdsrTransportError);
      expect(calls).toHaveLength(1);
      expect(locator.reference.id).toBe('ref-1');
    });

    it('the getService proxy goes through the same tracked locator', async () => {
      const { plugin, calls } = flakyPlugin(1);
      const b = brokerWith(at('ref-1', 'http://a:1'), [plugin]);
      const payment = await b.consumer.getService<{ charge(amount: number, accountId: string): Promise<string> }>('Payment');
      b.brokerNowLists(at('ref-2', 'http://b:1'));

      expect(await payment!.charge(1, 'x')).toBe('ok from http://b:1 (charge)');
      expect(calls).toEqual(['http://a:1', 'http://b:1']);
    });
  });
});
