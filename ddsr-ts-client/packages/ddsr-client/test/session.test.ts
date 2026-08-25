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
import { BrokerHttp } from '../src/internal/broker-http';
import { DdsrClientImpl } from '../src/internal/ddsr-client-impl';
import type { DdsrEventSource, EventSubscription } from '../src/events/event-source';
import { OK_DIAGNOSTIC_XMI, fakeFetch, lookupResultXmi } from './fixtures';

const BROKER = 'http://broker.test/ddsr/rest';

async function until(condition: () => boolean, timeoutMillis = 3000): Promise<void> {
  const start = Date.now();
  while (!condition()) {
    if (Date.now() - start > timeoutMillis) throw new Error('condition not met in time');
    await new Promise(resolve => setTimeout(resolve, 10));
  }
}

describe('BrokerHttp consumer sessions (ACQUISITION §4)', () => {
  it('putConsumerSession PUTs the session with reference id-stub siblings', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'PUT', urlIncludes: '/consumers/', body: OK_DIAGNOSTIC_XMI },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });

    const diagnostic = await broker.putConsumerSession('consumer-1', ['ref-1', 'ref-2']);

    expect(diagnostic.severity).toBe('OK');
    expect(requests[0].method).toBe('PUT');
    expect(requests[0].url).toBe(`${BROKER}/consumers/consumer-1`);
    const body = requests[0].body ?? '';
    expect(body).toContain('services:ConsumerSession');
    expect(body).toContain('consumerId="consumer-1"');
    expect(body).toContain('id="ref-1"');
    expect(body).toContain('id="ref-2"');
  });

  it('an empty acquisition list is a pure heartbeat', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'PUT', urlIncludes: '/consumers/', body: OK_DIAGNOSTIC_XMI },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });
    await broker.putConsumerSession('consumer-1', []);
    expect(requests[0].body).toContain('services:ConsumerSession');
    expect(requests[0].body).not.toContain('services:ServiceReference');
  });

  it('deleteConsumerSession is the shutdown-notify', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'DELETE', urlIncludes: '/consumers/', body: OK_DIAGNOSTIC_XMI },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });
    const diagnostic = await broker.deleteConsumerSession('consumer-1');
    expect(diagnostic.severity).toBe('OK');
    expect(requests[0].method).toBe('DELETE');
    expect(requests[0].url).toBe(`${BROKER}/consumers/consumer-1`);
  });

  it('getConsumerSession parses the symmetric wire form and 404s to undefined', async () => {
    const sessionXmi = `<?xml version="1.0" encoding="UTF-8"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:services="http://eclipse.org/fennec/services/1.0">
  <services:ConsumerSession consumerId="consumer-1"/>
  <services:ServiceReference id="ref-1"/>
  <services:ServiceReference id="ref-2"/>
</xmi:XMI>`;
    const { fetchFn } = fakeFetch([
      { urlIncludes: '/consumers/consumer-1', body: sessionXmi },
      { urlIncludes: '/consumers/ghost', status: 404, body: 'no session', contentType: 'text/plain' },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });

    const snapshot = await broker.getConsumerSession('consumer-1');
    expect(snapshot?.session.consumerId).toBe('consumer-1');
    expect(snapshot?.acquiredReferenceIds).toEqual(['ref-1', 'ref-2']);
    expect(await broker.getConsumerSession('ghost')).toBeUndefined();
  });

  it('getReferences forwards the contract fingerprint (§11.2)', async () => {
    const { fetchFn, requests } = fakeFetch([
      { urlIncludes: '/references', body: lookupResultXmi() },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });
    await broker.getReferences('Payment', undefined, 'REST', 'c-1', 'sd1:abc');
    const url = new URL(requests[0].url);
    expect(url.searchParams.get('fingerprint')).toBe('sd1:abc');
  });
});

describe('DdsrClientImpl session lifecycle', () => {
  function eventSource(): { source: DdsrEventSource; closedAt: () => number } {
    let closed = 0;
    const source: DdsrEventSource = {
      open: (): EventSubscription => ({
        close: async () => {
          closed = Date.now();
        },
      }),
    };
    return { source, closedAt: () => closed };
  }

  it('renews the session with the reference ids learned from lookups', async () => {
    const { fetchFn, requests } = fakeFetch([
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-9') },
      { method: 'PUT', urlIncludes: '/consumers/', body: OK_DIAGNOSTIC_XMI },
    ]);
    const client = DdsrClientImpl.create({
      brokerUrl: BROKER,
      fetchFn,
      consumerId: 'session-tester',
      sessionIntervalSeconds: 0, // timer off — renew explicitly
      eventSource: eventSource().source,
    });
    client.consumer.addServiceListener('Payment', undefined, () => undefined);
    await client.consumer.find('Payment');

    await client.renewSession();

    const put = requests.find(r => r.method === 'PUT');
    expect(put?.url).toBe(`${BROKER}/consumers/session-tester`);
    expect(put?.body).toContain('id="ref-9"');
  });

  it('the interval timer renews on its own', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'PUT', urlIncludes: '/consumers/', body: OK_DIAGNOSTIC_XMI },
    ]);
    const client = DdsrClientImpl.create({
      brokerUrl: BROKER,
      fetchFn,
      consumerId: 'timer-tester',
      sessionIntervalSeconds: 0.05,
      eventSource: eventSource().source,
    });
    await until(() => requests.filter(r => r.method === 'PUT').length >= 2);
    await client.close();
  });

  it('close(): withdraw, then session DELETE, then stream close (FR-P3 order)', async () => {
    const order: string[] = [];
    const { fetchFn } = fakeFetch([
      { urlIncludes: '/references', body: lookupResultXmi() },
    ]);
    const trackingFetch = (async (input: any, init?: any) => {
      const url = String(input);
      if (init?.method === 'DELETE' && url.includes('/consumers/')) {
        order.push('session-deleted');
        return new Response(OK_DIAGNOSTIC_XMI, {
          status: 200,
          headers: { 'Content-Type': 'application/xml' },
        });
      }
      return fetchFn(input, init);
    }) as typeof fetch;

    let streamOpen = false;
    const source: DdsrEventSource = {
      open: (): EventSubscription => {
        streamOpen = true;
        return {
          close: async () => {
            streamOpen = false;
            order.push('stream-closed');
          },
        };
      },
    };
    const client = DdsrClientImpl.create({
      brokerUrl: BROKER,
      fetchFn: trackingFetch,
      consumerId: 'order-tester',
      sessionIntervalSeconds: 0,
      eventSource: source,
    });
    client.consumer.addServiceListener('Payment', undefined, () => undefined);
    expect(streamOpen).toBe(true);

    await client.close();

    expect(order).toEqual(['session-deleted', 'stream-closed']);
  });

  it('a failing session DELETE never stalls the shutdown', async () => {
    const failingFetch = (async (input: any, init?: any) => {
      if (init?.method === 'DELETE') throw new Error('broker gone');
      return new Response('', { status: 200 });
    }) as typeof fetch;
    let streamClosed = false;
    const client = DdsrClientImpl.create({
      brokerUrl: BROKER,
      fetchFn: failingFetch,
      consumerId: 'unlucky',
      sessionIntervalSeconds: 0,
      eventSource: {
        open: () => ({
          close: async () => {
            streamClosed = true;
          },
        }),
      },
    });
    client.consumer.addServiceListener('Payment', undefined, () => undefined);
    await client.close();
    expect(streamClosed).toBe(true);
  });
});
