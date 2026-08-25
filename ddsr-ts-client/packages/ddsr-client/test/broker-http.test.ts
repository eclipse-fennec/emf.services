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
import {
  OK_DIAGNOSTIC_XMI,
  errorDiagnosticXmi,
  fakeFetch,
  lookupResultXmi,
  paymentInterface,
  paymentProvider,
} from './fixtures';

const BROKER = 'http://broker.test/ddsr/rest';

describe('BrokerHttp', () => {
  it('publish sends multi-root XMI (provider + interface stub) via POST /implementations', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'POST', urlIncludes: '/implementations', body: OK_DIAGNOSTIC_XMI },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });
    const { provider, serviceInterface } = paymentProvider();

    const diagnostic = await broker.publishImplementation(provider, [serviceInterface]);

    expect(requests).toHaveLength(1);
    expect(requests[0].url).toBe(`${BROKER}/implementations`);
    expect(requests[0].headers['Content-Type']).toBe('application/xml');
    const body = requests[0].body ?? '';
    expect(body).toContain('<xmi:XMI');
    expect(body).toContain('services:ServiceProvider');
    expect(body).toContain('services:ServiceInterface');
    expect(body).not.toContain('file:/');
    // EMF default omission: bare diagnostic means OK/0
    expect(diagnostic.severity).toBe('OK');
    expect(diagnostic.code).toBe(0);
  });

  it('withdraw is a POST /implementations/withdraw with the same XMI body shape', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'POST', urlIncludes: '/implementations/withdraw', body: OK_DIAGNOSTIC_XMI },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });
    const { provider, serviceInterface } = paymentProvider();

    await broker.withdrawImplementation(provider, [serviceInterface]);

    expect(requests[0].method).toBe('POST');
    expect(requests[0].url).toBe(`${BROKER}/implementations/withdraw`);
    expect(requests[0].body).toContain('services:ServiceProvider');
  });

  it('reads an ERROR diagnostic from a 4xx body', async () => {
    const { fetchFn } = fakeFetch([
      {
        method: 'POST',
        urlIncludes: '/implementations',
        status: 422,
        body: errorDiagnosticXmi(210, 'interface not in catalog'),
      },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });
    const { provider, serviceInterface } = paymentProvider();

    const diagnostic = await broker.publishImplementation(provider, [serviceInterface]);
    expect(diagnostic.severity).toBe('ERROR');
    expect(diagnostic.code).toBe(210);
    expect(diagnostic.message).toBe('interface not in catalog');
  });

  it('synthesizes an ERROR diagnostic from a plain-text error body', async () => {
    const { fetchFn } = fakeFetch([
      { method: 'POST', urlIncludes: '/implementations', status: 400, body: 'exactly one ServiceProvider root expected', contentType: 'text/plain' },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });
    const { provider, serviceInterface } = paymentProvider();

    const diagnostic = await broker.publishImplementation(provider, [serviceInterface]);
    expect(diagnostic.severity).toBe('ERROR');
    expect(diagnostic.code).toBe(400);
    expect(diagnostic.message).toContain('exactly one ServiceProvider');
  });

  it('getReferences builds the query with interface, filter, flavors, consumerId', async () => {
    const { fetchFn, requests } = fakeFetch([
      { urlIncludes: '/references', body: lookupResultXmi() },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });

    const roots = await broker.getReferences('Payment', '(lang=ts)', 'REST', 'consumer-1');
    expect(roots.length).toBeGreaterThan(0);
    const url = new URL(requests[0].url);
    expect(url.pathname.endsWith('/references')).toBe(true);
    expect(url.searchParams.get('interface')).toBe('Payment');
    expect(url.searchParams.get('filter')).toBe('(lang=ts)');
    expect(url.searchParams.get('flavors')).toBe('REST');
    expect(url.searchParams.get('consumerId')).toBe('consumer-1');
  });

  it('catalog operations carry X-DDSR-Requestor', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'POST', urlIncludes: '/catalog', body: OK_DIAGNOSTIC_XMI },
      { method: 'DELETE', urlIncludes: '/catalog/Payment', body: OK_DIAGNOSTIC_XMI },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, requestor: 'ts-tests', fetchFn });

    await broker.addCatalogEntry(paymentInterface());
    await broker.removeCatalogEntry('Payment');

    expect(requests[0].headers['X-DDSR-Requestor']).toBe('ts-tests');
    expect(requests[1].headers['X-DDSR-Requestor']).toBe('ts-tests');
    expect(requests[1].url).toBe(`${BROKER}/catalog/Payment`);
  });

  it('an explicit per-call requestor wins over the client default', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'POST', urlIncludes: '/catalog', body: OK_DIAGNOSTIC_XMI },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, requestor: 'default', fetchFn });
    await broker.addCatalogEntry(paymentInterface(), 'special');
    expect(requests[0].headers['X-DDSR-Requestor']).toBe('special');
  });

  it('getCatalogEntry returns undefined on the plain-text 404', async () => {
    const { fetchFn } = fakeFetch([
      { urlIncludes: '/catalog/Nope', status: 404, body: "no catalog entry named 'Nope'", contentType: 'text/plain' },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });
    expect(await broker.getCatalogEntry('Nope')).toBeUndefined();
  });

  it('catalog names are URL-encoded in the path', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'DELETE', urlIncludes: '/catalog/', body: OK_DIAGNOSTIC_XMI },
    ]);
    const broker = new BrokerHttp({ brokerUrl: BROKER, fetchFn });
    await broker.removeCatalogEntry('a/b c');
    expect(requests[0].url).toBe(`${BROKER}/catalog/a%2Fb%20c`);
  });
});
