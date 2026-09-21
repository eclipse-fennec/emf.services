/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { describe, expect, it } from 'vitest';
import type { CallSpan, CallTracer, TraceCarrier } from '@ddsr/telemetry';
import { brokerOperationOf, withTracing } from '../src/internal/broker-tracing';

/** A tracer that injects a fixed context and remembers the rest. */
function watching() {
  const state = {
    called: undefined as string | undefined,
    attributes: {} as Record<string, string | undefined>,
    failure: undefined as string | undefined,
    ended: false,
  };
  const tracer: CallTracer = {
    calling(operation: string, outbound: TraceCarrier): CallSpan {
      state.called = operation;
      outbound.set('traceparent', '00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01');
      const span: CallSpan = {
        attribute(name, value) {
          state.attributes[name] = value;
          return span;
        },
        failed(error) {
          state.failure = String((error as Error).message ?? error);
        },
        end() {
          state.ended = true;
        },
      };
      return span;
    },
    serving(): CallSpan {
      throw new Error('the client calls, it does not serve');
    },
  };
  return { state, tracer };
}

describe('the client half of a trace, on the calls the SDK makes to the broker', () => {
  const base = 'http://localhost:8887/ddsr/rest';

  it('puts the context on the request and names the call as the broker names it', async () => {
    const { state, tracer } = watching();
    let sent: RequestInit | undefined;
    const fetchFn = (async (_input: RequestInfo | URL, init?: RequestInit) => {
      sent = init;
      return new Response('', { status: 200 });
    }) as typeof fetch;

    const response = await withTracing(fetchFn, tracer, base)(`${base}/references?interface=Payment`, {
      headers: { Accept: 'application/xml' },
    });

    expect(response.status).toBe(200);
    expect(state.called).toBe('BrokerLookup/getServiceReferences');
    expect((sent?.headers as Record<string, string>).traceparent)
      .toBe('00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01');
    expect((sent?.headers as Record<string, string>).Accept)
      .toBe('application/xml');
    expect(state.attributes['http.response.status_code']).toBe('200');
    expect(state.ended).toBe(true);
  });

  it('marks a broker that cannot be reached, and still rejects', async () => {
    const { state, tracer } = watching();
    const fetchFn = (async () => {
      throw new TypeError('fetch failed');
    }) as unknown as typeof fetch;

    await expect(withTracing(fetchFn, tracer, base)(`${base}/catalog`, { method: 'POST' }))
      .rejects.toThrow('fetch failed');

    expect(state.failure).toBe('fetch failed');
    expect(state.ended)
      .toBe(true);
  });

  it('knows which operation a URL is, in the names the Java side uses', () => {
    expect(brokerOperationOf('/references?interface=Payment', 'GET'))
      .toBe('BrokerLookup/getServiceReferences');
    expect(brokerOperationOf('/references/abc-1/heartbeat?intervalSeconds=30', 'PUT'))
      .toBe('BrokerImplementations/heartbeat');
    expect(brokerOperationOf('/implementations', 'POST'))
      .toBe('BrokerImplementations/publishImplementation');
    expect(brokerOperationOf('/implementations', 'PUT'))
      .toBe('BrokerImplementations/modifyImplementation');
    expect(brokerOperationOf('/implementations/withdraw', 'POST'))
      .toBe('BrokerImplementations/withdrawImplementation');
    expect(brokerOperationOf('/catalog', 'POST')).toBe('BrokerCatalog/addCatalogEntry');
    expect(brokerOperationOf('/catalog/Payment/deprecate', 'PUT'))
      .toBe('BrokerCatalog/deprecateCatalogEntry');
    expect(brokerOperationOf('/catalog/Payment', 'DELETE'))
      .toBe('BrokerCatalog/removeCatalogEntry');
    expect(brokerOperationOf('/catalog/Payment', 'GET')).toBe('BrokerCatalog/getCatalogEntry');
    expect(brokerOperationOf('/consumers/c-1', 'PUT')).toBe('BrokerSessions/putSession');
    expect(brokerOperationOf('/consumers/c-1', 'DELETE')).toBe('BrokerSessions/deleteSession');
    expect(brokerOperationOf('/consumers/c-1', 'GET')).toBe('BrokerSessions/getSession');
  });

  it('names something new by its method rather than by a path full of ids', () => {
    expect(brokerOperationOf('/events', 'GET')).toBe('broker GET');
  });
});
