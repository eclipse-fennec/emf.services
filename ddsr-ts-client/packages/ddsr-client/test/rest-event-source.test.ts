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
import type { ServiceEvent } from '@ddsr/model';
import { RestEventSource } from '../src/events/rest-event-source';
import { UNREGISTERING_XMI } from './fixtures';

function sseFrame(payload: string): string {
  const data = payload.split('\n').map(l => `data: ${l}`).join('\n');
  return `event: ddsr-service-event\n${data}\n\n`;
}

/** fetch fake whose response body is a stream we control per connect. */
function streamingFetch(framesPerConnect: string[][]): {
  fetchFn: typeof fetch;
  connects: () => number;
  urls: string[];
} {
  let connects = 0;
  const urls: string[] = [];
  const fetchFn = (async (input: any) => {
    urls.push(String(input));
    const frames = framesPerConnect[Math.min(connects, framesPerConnect.length - 1)];
    connects++;
    const encoder = new TextEncoder();
    const body = new ReadableStream<Uint8Array>({
      start(controller) {
        for (const frame of frames) controller.enqueue(encoder.encode(frame));
        controller.close();
      },
    });
    return new Response(body, { status: 200, headers: { 'Content-Type': 'text/event-stream' } });
  }) as typeof fetch;
  return { fetchFn, connects: () => connects, urls };
}

async function until(condition: () => boolean, timeoutMillis = 3000): Promise<void> {
  const start = Date.now();
  while (!condition()) {
    if (Date.now() - start > timeoutMillis) throw new Error('condition not met in time');
    await new Promise(resolve => setTimeout(resolve, 5));
  }
}

describe('RestEventSource', () => {
  it('signals establish before delivering events, then decodes them', async () => {
    const { fetchFn, urls } = streamingFetch([[sseFrame(UNREGISTERING_XMI)]]);
    const order: string[] = [];
    const events: ServiceEvent[] = [];

    const source = new RestEventSource({
      brokerUrl: 'http://broker.test/ddsr/rest',
      flavors: 'REST',
      reconnectSeconds: 10,
      fetchFn,
      log: () => undefined,
    });
    const subscription = source.open({
      onStreamEstablished: () => {
        order.push('established');
      },
      onEvent: (event) => {
        order.push('event');
        events.push(event);
      },
    });

    await until(() => events.length === 1);
    await subscription.close();

    expect(order[0]).toBe('established');
    expect(events[0].type).toBe('UNREGISTERING');
    expect(events[0].reference?.id).toBe('ref-42');
    expect(urls[0]).toBe('http://broker.test/ddsr/rest/events?flavors=REST');
  });

  it('awaits the establish handler before pumping (snapshot-before-events)', async () => {
    const { fetchFn } = streamingFetch([[sseFrame(UNREGISTERING_XMI)]]);
    const order: string[] = [];
    const source = new RestEventSource({
      brokerUrl: 'http://b.test',
      reconnectSeconds: 10,
      fetchFn,
      log: () => undefined,
    });
    const subscription = source.open({
      onStreamEstablished: async () => {
        await new Promise(resolve => setTimeout(resolve, 30));
        order.push('snapshot-done');
      },
      onEvent: () => order.push('event'),
    });
    await until(() => order.includes('event'));
    await subscription.close();
    expect(order).toEqual(['snapshot-done', 'event']);
  });

  it('reconnects with the flat delay after the server closes the stream', async () => {
    const { fetchFn, connects } = streamingFetch([[], []]);
    const establishes: number[] = [];
    const source = new RestEventSource({
      brokerUrl: 'http://b.test',
      reconnectSeconds: 0.02,
      fetchFn,
      log: () => undefined,
    });
    const subscription = source.open({
      onStreamEstablished: () => {
        establishes.push(Date.now());
      },
      onEvent: () => undefined,
    });
    await until(() => establishes.length >= 2);
    await subscription.close();
    expect(connects()).toBeGreaterThanOrEqual(2);
  });

  it('an undecodable payload is skipped without tearing the stream down', async () => {
    const { fetchFn } = streamingFetch([
      [sseFrame('<not-xmi>'), sseFrame(UNREGISTERING_XMI)],
    ]);
    const events: ServiceEvent[] = [];
    const source = new RestEventSource({
      brokerUrl: 'http://b.test',
      reconnectSeconds: 10,
      fetchFn,
      log: () => undefined,
    });
    const subscription = source.open({
      onStreamEstablished: () => undefined,
      onEvent: e => {
        events.push(e);
      },
    });
    await until(() => events.length === 1);
    await subscription.close();
    expect(events[0].reference?.id).toBe('ref-42');
  });

  it('close() stops the reconnect loop', async () => {
    const { fetchFn, connects } = streamingFetch([[]]);
    const source = new RestEventSource({
      brokerUrl: 'http://b.test',
      reconnectSeconds: 0.01,
      fetchFn,
      log: () => undefined,
    });
    const subscription = source.open({
      onStreamEstablished: () => undefined,
      onEvent: () => undefined,
    });
    await until(() => connects() >= 1);
    await subscription.close();
    const after = connects();
    await new Promise(resolve => setTimeout(resolve, 60));
    expect(connects()).toBe(after);
  });

  it('an empty flavors filter omits the query parameter', async () => {
    const { fetchFn, urls } = streamingFetch([[]]);
    const source = new RestEventSource({
      brokerUrl: 'http://b.test/rest/',
      flavors: '',
      reconnectSeconds: 10,
      fetchFn,
      log: () => undefined,
    });
    const subscription = source.open({
      onStreamEstablished: () => undefined,
      onEvent: () => undefined,
    });
    await until(() => urls.length >= 1);
    await subscription.close();
    expect(urls[0]).toBe('http://b.test/rest/events');
  });
});
