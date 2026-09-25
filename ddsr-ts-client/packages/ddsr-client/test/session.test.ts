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

import { afterEach, describe, expect, it, vi } from 'vitest';
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

  it('a client without a consumerId names itself consumer-<uuid>, as the Java client does (#167)', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'PUT', urlIncludes: '/consumers/', body: OK_DIAGNOSTIC_XMI },
    ]);
    const unnamed = (consumerId?: string) => DdsrClientImpl.create({
      brokerUrl: BROKER,
      fetchFn,
      consumerId,
      sessionIntervalSeconds: 0,
      eventSource: eventSource().source,
    });
    const first = unnamed();
    const blank = unnamed('  ');

    const id = first.runtime.snapshot().consumerId;
    expect(id).toMatch(/^consumer-[0-9a-f-]{36}$/);
    expect(blank.runtime.snapshot().consumerId).toMatch(/^consumer-/);
    expect(blank.runtime.snapshot().consumerId).not.toBe(id);

    // And it has a session under that name, which is the point: its
    // leases are the broker's to see.
    await first.renewSession();
    expect(requests.find(r => r.method === 'PUT')?.url).toBe(`${BROKER}/consumers/${id}`);
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

  describe('schedule (#170)', () => {
    afterEach(() => {
      vi.useRealTimers();
      vi.restoreAllMocks();
    });

    it('the first PUT goes out after 5 s, not after a whole interval, as in Java', async () => {
      vi.useFakeTimers();
      const { fetchFn, requests } = fakeFetch([
        { method: 'PUT', urlIncludes: '/consumers/', body: OK_DIAGNOSTIC_XMI },
      ]);
      const client = DdsrClientImpl.create({
        brokerUrl: BROKER,
        fetchFn,
        consumerId: 'early',
        providerHeartbeatSeconds: 0,
        eventSource: eventSource().source,
      });
      const puts = () => requests.filter(r => r.method === 'PUT').length;

      await vi.advanceTimersByTimeAsync(4_999);
      expect(puts()).toBe(0);
      await vi.advanceTimersByTimeAsync(1);
      expect(puts()).toBe(1);
      await vi.advanceTimersByTimeAsync(600_000);
      expect(puts()).toBe(2);
      await client.close();
    });

    it('an interval shorter than 5 s starts after that interval', async () => {
      vi.useFakeTimers();
      const { fetchFn, requests } = fakeFetch([
        { method: 'PUT', urlIncludes: '/consumers/', body: OK_DIAGNOSTIC_XMI },
      ]);
      const client = DdsrClientImpl.create({
        brokerUrl: BROKER,
        fetchFn,
        consumerId: 'quick',
        sessionIntervalSeconds: 2,
        providerHeartbeatSeconds: 0,
        eventSource: eventSource().source,
      });

      await vi.advanceTimersByTimeAsync(2_000);
      expect(requests.filter(r => r.method === 'PUT')).toHaveLength(1);
      await client.close();
    });

    it('the provider heartbeat starts after 5 s as well', async () => {
      vi.useFakeTimers();
      const { fetchFn } = fakeFetch([]);
      const client = DdsrClientImpl.create({
        brokerUrl: BROKER,
        fetchFn,
        sessionIntervalSeconds: 0,
        eventSource: eventSource().source,
      });
      const heartbeat = vi.spyOn(client, 'heartbeatRegistrations').mockResolvedValue(0);

      await vi.advanceTimersByTimeAsync(5_000);
      expect(heartbeat).toHaveBeenCalledTimes(1);
      await vi.advanceTimersByTimeAsync(30_000);
      expect(heartbeat).toHaveBeenCalledTimes(2);
      await client.close();
    });

    it('close() stops a schedule that has not fired yet', async () => {
      vi.useFakeTimers();
      const { fetchFn, requests } = fakeFetch([
        { method: 'DELETE', urlIncludes: '/consumers/', body: OK_DIAGNOSTIC_XMI },
      ]);
      const client = DdsrClientImpl.create({
        brokerUrl: BROKER,
        fetchFn,
        consumerId: 'short-lived',
        providerHeartbeatSeconds: 0,
        eventSource: eventSource().source,
      });
      await client.close();

      await vi.advanceTimersByTimeAsync(10_000);
      expect(requests.filter(r => r.method === 'PUT')).toHaveLength(0);
    });
  });

  it('a refused renewal is reported, not only a network failure (#170)', async () => {
    const { fetchFn } = fakeFetch([
      { method: 'PUT', urlIncludes: '/consumers/', status: 502, body: 'bad gateway', contentType: 'text/plain' },
    ]);
    const errors = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const client = DdsrClientImpl.create({
      brokerUrl: BROKER,
      fetchFn,
      consumerId: 'refused',
      sessionIntervalSeconds: 0,
      providerHeartbeatSeconds: 0,
      eventSource: eventSource().source,
    });

    await client.renewSession();

    expect(errors).toHaveBeenCalledWith(expect.stringMatching(/session renewal refused \(502\).*bad gateway/));
    errors.mockRestore();
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
