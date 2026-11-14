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
import { DdsrCatalogImpl } from '../src/internal/catalog-impl';
import { BrokerHttp } from '../src/internal/broker-http';
import { DdsrClientError } from '../src/api/errors';
import { OK_DIAGNOSTIC_XMI, errorDiagnosticXmi, fakeFetch, paymentInterface } from './fixtures';

const BROKER = 'http://broker.test/ddsr/rest';

function catalogWith(routes: Parameters<typeof fakeFetch>[0]) {
  const { fetchFn, requests } = fakeFetch(routes);
  return { catalog: new DdsrCatalogImpl(new BrokerHttp({ brokerUrl: BROKER, fetchFn })), requests };
}

/** #271: a contract is put into the catalog on its own and taken out again. */
describe('the catalog API', () => {
  it('ensureEntry adds a contract without any implementation', async () => {
    const { catalog, requests } = catalogWith([{ method: 'POST', urlIncludes: '/catalog', body: OK_DIAGNOSTIC_XMI }]);

    await catalog.ensureEntry(paymentInterface(), 'shop');

    expect(requests).toHaveLength(1);
    expect(requests[0].method).toBe('POST');
  });

  it('ensureEntry counts an entry that already exists as success', async () => {
    const { catalog } = catalogWith([
      { method: 'POST', urlIncludes: '/catalog', status: 409, body: errorDiagnosticXmi(202, 'already present') },
    ]);

    const diagnostic = await catalog.ensureEntry(paymentInterface());

    expect(diagnostic.code).toBe(202);
  });

  it('ensureEntry throws on any other refusal', async () => {
    const { catalog } = catalogWith([
      { method: 'POST', urlIncludes: '/catalog', status: 422, body: errorDiagnosticXmi(201, 'invariants') },
    ]);

    await expect(catalog.ensureEntry(paymentInterface())).rejects.toBeInstanceOf(DdsrClientError);
  });

  it('removeEntry returns the refusal while something implements the contract', async () => {
    const { catalog } = catalogWith([
      { method: 'DELETE', urlIncludes: '/catalog/Payment', status: 409, body: errorDiagnosticXmi(200, 'live implementations') },
    ]);

    const diagnostic = await catalog.removeEntry('Payment');

    expect(diagnostic.code).toBe(200);
  });

  it('removeEntry, deprecateEntry and getEntry address one contract by fingerprint', async () => {
    const { catalog, requests } = catalogWith([
      { method: 'DELETE', urlIncludes: '/catalog/Payment', body: OK_DIAGNOSTIC_XMI },
      { method: 'PUT', urlIncludes: '/catalog/Payment/deprecate', body: OK_DIAGNOSTIC_XMI },
      { method: 'GET', urlIncludes: '/catalog/Payment', status: 404, body: '' },
    ]);

    await catalog.removeEntry('Payment', 'admin', 'sd1:abc');
    await catalog.deprecateEntry('Payment', undefined, 'admin', 'sd1:abc');
    await catalog.getEntry('Payment', 'sd1:abc');

    expect(requests.map(r => r.url)).toEqual(requests.map(() => expect.stringContaining('fingerprint=sd1%3Aabc')));
  });
});
