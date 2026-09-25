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
import type { DdsrEventSource, EventSourceHandler, EventSubscription } from '../src/events/event-source';
import { OK_DIAGNOSTIC_XMI, fakeFetch, lookupResultXmi, paymentProvider } from './fixtures';

const BROKER = 'http://broker.test/ddsr/rest';

/** An event source the test drives by hand, as the transport would. */
function controlledSource(): { source: DdsrEventSource; handler: () => EventSourceHandler } {
  let handler: EventSourceHandler | undefined;
  const source: DdsrEventSource = {
    transport: 'rest',
    open: (h): EventSubscription => {
      handler = h;
      return { close: async () => undefined };
    },
  };
  return {
    source,
    handler: () => {
      if (!handler) throw new Error('the stream was never opened');
      return handler;
    },
  };
}

function client(fetchFn: typeof fetch, source: DdsrEventSource): DdsrClientImpl {
  return DdsrClientImpl.create({
    brokerUrl: BROKER,
    fetchFn,
    consumerId: 'shop',
    sessionIntervalSeconds: 0,
    providerHeartbeatSeconds: 0,
    eventSource: source,
  });
}

describe('ClientRuntime (#167)', () => {
  it('reports what a lookup bound to, the way the Java runtime does', async () => {
    const { fetchFn } = fakeFetch([{ urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-9') }]);
    const runtime = client(fetchFn, controlledSource().source);

    await runtime.consumer.find('Payment');
    const snapshot = runtime.runtime.snapshot();

    expect(snapshot.consumerId).toBe('shop');
    expect(snapshot.bindings).toEqual([{
      contract: 'Payment',
      filter: undefined,
      referenceId: 'ref-9',
      endpoint: 'http://localhost:9090/payments',
      state: 'LIVE',
    }]);
    expect(snapshot.published).toEqual([]);
    expect(snapshot.takenAt).toBeGreaterThan(0);
  });

  it('reports what it published, and that the broker holds it', async () => {
    const { fetchFn } = fakeFetch([
      { method: 'POST', urlIncludes: '/implementations', body: OK_DIAGNOSTIC_XMI },
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-77') },
    ]);
    const runtime = client(fetchFn, controlledSource().source);
    const fixture = paymentProvider('payments-ts');

    await runtime.provider.publish(fixture.provider, fixture.implementation);
    const [published] = runtime.runtime.snapshot().published;

    expect(published.referenceId).toBe('ref-77');
    expect(published.live).toBe(true);
    expect(published.contracts).toEqual(['Payment']);
    expect(published.failure).toBeUndefined();
  });

  it('says whether the event stream is being heard, and over which transport', async () => {
    const { fetchFn } = fakeFetch([{ urlIncludes: '/references', body: lookupResultXmi() }]);
    const { source, handler } = controlledSource();
    const runtime = client(fetchFn, source);
    runtime.consumer.addServiceListener('Payment', undefined, () => undefined);

    expect(runtime.runtime.snapshot().eventStreamConnected).toBe(false);
    await handler().onStreamEstablished();
    expect(runtime.runtime.snapshot()).toMatchObject({ eventStreamConnected: true, eventTransport: 'rest' });
    handler().onStreamLost?.();
    expect(runtime.runtime.snapshot().eventStreamConnected).toBe(false);
  });

  it('counts changes by what a snapshot would say, not by being told', async () => {
    const { fetchFn } = fakeFetch([{ urlIncludes: '/references', body: lookupResultXmi() }]);
    const { source, handler } = controlledSource();
    const runtime = client(fetchFn, source);
    runtime.consumer.addServiceListener('Payment', undefined, () => undefined);

    const before = runtime.runtime.snapshot().changeCount;
    expect(runtime.runtime.snapshot().changeCount).toBe(before);

    await handler().onStreamEstablished();
    const connected = runtime.runtime.snapshot().changeCount;
    expect(connected).toBe(before + 1);

    await runtime.consumer.find('Payment');
    expect(runtime.runtime.snapshot().changeCount).toBe(connected + 1);
  });
});
