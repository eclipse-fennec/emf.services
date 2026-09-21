/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { carrierOver, type CallTracer } from '@ddsr/telemetry';

/**
 * The client half of a trace for the calls this SDK makes to the
 * broker (#146).
 *
 * Wrapped around the fetch once, for the reason the origin wrapper
 * gives one class over: what has to be true of every call belongs
 * where every call passes, not at ten call sites that can each forget
 * it. The Java client does the same job in `RestTransport.send`.
 */
export function withTracing(
  inner: typeof fetch,
  tracer: CallTracer,
  base: string,
  origin: string
): typeof fetch {
  return (async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? 'GET';
    const context: Record<string, string> = {};
    const span = tracer.calling(brokerOperationOf(pathOf(url, base), method), carrierOver(context));
    span
      .attribute('rpc.system', 'fennec.services')
      .attribute('http.request.method', method)
      .attribute('server.address', base)
      .attribute('fennec.flavor', 'REST')
      // Who is calling (#125) — the same identity the origin header
      // carries, so a trace answers which system told which system
      // what and not only what happened.
      .attribute('fennec.origin', origin);
    try {
      const response = await inner(input, { ...init, headers: withContext(init?.headers, context) });
      span.attribute('http.response.status_code', String(response.status));
      return response;
    } catch (error) {
      // fetch rejects when the broker is unreachable, which is the one
      // call a trace should certainly show.
      span.failed(error);
      throw error;
    } finally {
      span.end();
    }
  }) as typeof fetch;
}

/**
 * Which broker operation a request is.
 *
 * The URL shapes are this module's neighbour's own — `BrokerHttp`
 * documents them as the wire details it owns — so naming them here is
 * saying the same thing once more in the same place, not knowledge
 * kept twice. The names match what the Java side calls the serving
 * half, so the two halves of a call read as one.
 */
export function brokerOperationOf(path: string, method: string): string {
  if (path === '/references' || path.startsWith('/references?')) {
    return 'BrokerLookup/getServiceReferences';
  }
  if (/^\/references\/[^/]+\/heartbeat/.test(path)) {
    return 'BrokerImplementations/heartbeat';
  }
  if (path === '/implementations') {
    return method === 'PUT'
      ? 'BrokerImplementations/modifyImplementation'
      : 'BrokerImplementations/publishImplementation';
  }
  if (path === '/implementations/withdraw') {
    return 'BrokerImplementations/withdrawImplementation';
  }
  if (path === '/catalog' || path.startsWith('/catalog?')) {
    return method === 'POST' ? 'BrokerCatalog/addCatalogEntry' : 'BrokerCatalog/listCatalog';
  }
  if (/^\/catalog\/[^/?]+\/deprecate/.test(path)) {
    return 'BrokerCatalog/deprecateCatalogEntry';
  }
  if (/^\/catalog\/[^/?]+/.test(path)) {
    return method === 'DELETE' ? 'BrokerCatalog/removeCatalogEntry' : 'BrokerCatalog/getCatalogEntry';
  }
  if (/^\/consumers\/[^/?]+/.test(path)) {
    if (method === 'PUT') return 'BrokerSessions/putSession';
    if (method === 'DELETE') return 'BrokerSessions/deleteSession';
    return 'BrokerSessions/getSession';
  }
  // Something new, or the event stream. Named by its method rather
  // than by its path: a path with an id in it is a name nobody can
  // group by.
  return `broker ${method}`;
}

function pathOf(url: string, base: string): string {
  return url.startsWith(base) ? url.slice(base.length) : url;
}

/**
 * Adds the context without changing the shape the caller chose — the
 * same care the origin wrapper takes, and for the same reason: a test
 * fake that looks for its own header should still find it.
 */
function withContext(headers: HeadersInit | undefined, context: Record<string, string>): HeadersInit {
  if (Object.keys(context).length === 0) {
    return headers ?? {};
  }
  if (headers instanceof Headers) {
    const copy = new Headers(headers);
    for (const [name, value] of Object.entries(context)) {
      copy.set(name, value);
    }
    return copy;
  }
  if (Array.isArray(headers)) {
    return [...headers, ...Object.entries(context)];
  }
  return { ...(headers ?? {}), ...context } as Record<string, string>;
}
