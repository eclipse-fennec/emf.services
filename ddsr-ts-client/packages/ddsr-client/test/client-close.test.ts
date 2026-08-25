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
import { DdsrClientImpl } from '../src/internal/ddsr-client-impl';
import type { DdsrEventSource, EventSubscription } from '../src/events/event-source';
import { OK_DIAGNOSTIC_XMI, fakeFetch, lookupResultXmi, paymentProvider } from './fixtures';

const BROKER = 'http://broker.test/ddsr/rest';

describe('DdsrClientImpl.close()', () => {
  it('withdraws registrations and waits for confirmation BEFORE closing the stream (FR-P3)', async () => {
    const order: string[] = [];

    const { fetchFn } = fakeFetch([
      { method: 'POST', urlIncludes: '/implementations', body: OK_DIAGNOSTIC_XMI },
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-1') },
    ]);
    const trackingFetch = (async (input: any, init?: any) => {
      if (String(input).endsWith('/withdraw')) {
        // simulate broker fan-out latency before the confirmation
        await new Promise(resolve => setTimeout(resolve, 20));
        order.push('withdraw-confirmed');
        return new Response(OK_DIAGNOSTIC_XMI, {
          status: 200,
          headers: { 'Content-Type': 'application/xml' },
        });
      }
      return fetchFn(input, init);
    }) as typeof fetch;

    let streamOpen = false;
    const eventSource: DdsrEventSource = {
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
      eventSource,
    });
    client.consumer.addServiceListener('Payment', undefined, () => undefined);
    expect(streamOpen).toBe(true);

    const fixture = paymentProvider('payments-ts');
    await client.provider.publish(fixture.provider, fixture.implementation);

    await client.close();

    expect(order).toEqual(['withdraw-confirmed', 'stream-closed']);
    expect(streamOpen).toBe(false);
  });

  it('closes the stream even when a withdraw fails, then rethrows', async () => {
    const { fetchFn } = fakeFetch([
      { method: 'POST', urlIncludes: '/implementations', body: OK_DIAGNOSTIC_XMI },
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-1') },
    ]);
    const failingFetch = (async (input: any, init?: any) => {
      if (String(input).endsWith('/withdraw')) throw new Error('broker gone');
      return fetchFn(input, init);
    }) as typeof fetch;

    let streamClosed = false;
    const client = DdsrClientImpl.create({
      brokerUrl: BROKER,
      fetchFn: failingFetch,
      eventSource: {
        open: () => ({
          close: async () => {
            streamClosed = true;
          },
        }),
      },
    });
    client.consumer.addServiceListener('Payment', undefined, () => undefined);
    const fixture = paymentProvider('payments-ts');
    await client.provider.publish(fixture.provider, fixture.implementation);

    await expect(client.close()).rejects.toThrow(/withdraw on close failed/);
    expect(streamClosed).toBe(true);
  });
});
