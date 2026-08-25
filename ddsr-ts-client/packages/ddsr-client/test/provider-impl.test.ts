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
import { DdsrProviderImpl } from '../src/internal/provider-impl';
import { BrokerHttp } from '../src/internal/broker-http';
import { DdsrClientError } from '../src/api/errors';
import { implementationFingerprint } from '../src/fingerprint/service-implementation-fingerprint';
import {
  OK_DIAGNOSTIC_XMI,
  errorDiagnosticXmi,
  fakeFetch,
  lookupResultXmi,
  paymentProvider,
} from './fixtures';

const BROKER = 'http://broker.test/ddsr/rest';

describe('DdsrProviderImpl', () => {
  it('publish returns a registration with the broker-assigned reference', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'POST', urlIncludes: '/implementations', body: OK_DIAGNOSTIC_XMI },
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-77') },
    ]);
    const provider = new DdsrProviderImpl(new BrokerHttp({ brokerUrl: BROKER, fetchFn }));
    const fixture = paymentProvider('payments-ts');

    const registration = await provider.publish(fixture.provider, fixture.implementation);

    expect(registration.reference.id).toBe('ref-77');
    expect(registration.diagnostic().severity).toBe('OK');
    expect(provider.registrationOf('ref-77')).toBe(registration);
    // reconnect pre-check + publish roundtrip + discovery roundtrip
    // (the fake's held reference carries no im1 decoration, so the
    // pre-check reports drift and the publish goes through)
    expect(requests.map(r => r.method)).toEqual(['GET', 'POST', 'GET']);
  });

  it('publish is idempotent on reconnect — im1 match skips the roundtrip', async () => {
    const fixture = paymentProvider('payments-ts');
    const localIm1 = implementationFingerprint(fixture.implementation)!;
    // the broker-held reference advertises exactly our im1
    const heldXmi = lookupResultXmi('payments-ts', 'ref-held').replace(
      '<properties xsi:type="services:StringProperty" name="ddsr.fingerprint"',
      `<properties xsi:type="services:StringProperty" name="ddsr.impl.fingerprint" value="${localIm1}"/>` +
      '<properties xsi:type="services:StringProperty" name="ddsr.fingerprint"'
    );
    const { fetchFn, requests } = fakeFetch([
      { urlIncludes: '/references', body: heldXmi },
    ]);
    const provider = new DdsrProviderImpl(new BrokerHttp({ brokerUrl: BROKER, fetchFn }));

    const registration = await provider.publish(fixture.provider, fixture.implementation);

    expect(registration.reference.id).toBe('ref-held');
    expect(registration.diagnostic().message).toContain('im1 match');
    // reconnect pre-check only — no publish POST, no discovery GET
    expect(requests.map(r => r.method)).toEqual(['GET']);
  });

  it('publish throws a DdsrClientError carrying the diagnostic on ERROR', async () => {
    const { fetchFn } = fakeFetch([
      {
        method: 'POST',
        urlIncludes: '/implementations',
        status: 422,
        body: errorDiagnosticXmi(210, 'interface not in catalog'),
      },
    ]);
    const provider = new DdsrProviderImpl(new BrokerHttp({ brokerUrl: BROKER, fetchFn }));
    const fixture = paymentProvider();

    await expect(provider.publish(fixture.provider, fixture.implementation))
      .rejects.toSatisfy((error: unknown) => {
        expect(error).toBeInstanceOf(DdsrClientError);
        expect((error as DdsrClientError).diagnostic?.code).toBe(210);
        return true;
      });
  });

  it('publish validates containment before any network call', async () => {
    const { fetchFn, requests } = fakeFetch([]);
    const provider = new DdsrProviderImpl(new BrokerHttp({ brokerUrl: BROKER, fetchFn }));
    const a = paymentProvider('a');
    const b = paymentProvider('b');

    await expect(provider.publish(a.provider, b.implementation)).rejects.toThrow(/contained/);
    expect(requests).toHaveLength(0);
  });

  it('withdraw is idempotent and hits POST /implementations/withdraw once', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'POST', urlIncludes: '/implementations', body: OK_DIAGNOSTIC_XMI },
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-1') },
      { method: 'POST', urlIncludes: '/implementations/withdraw', body: OK_DIAGNOSTIC_XMI },
    ]);
    const provider = new DdsrProviderImpl(new BrokerHttp({ brokerUrl: BROKER, fetchFn }));
    const fixture = paymentProvider('payments-ts');
    const registration = await provider.publish(fixture.provider, fixture.implementation);

    const first = await registration.withdraw();
    const second = await registration.withdraw();

    expect(first.severity).toBe('OK');
    expect(second.severity).toBe('OK');
    expect(requests.filter(r => r.url.endsWith('/withdraw'))).toHaveLength(1);
  });

  it('a failed withdraw does not mark the registration withdrawn', async () => {
    let failWithdraw = true;
    const { fetchFn, requests } = fakeFetch([
      { method: 'POST', urlIncludes: '/implementations', body: OK_DIAGNOSTIC_XMI },
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-1') },
    ]);
    const routedFetch = (async (input: any, init?: any) => {
      if (String(input).endsWith('/withdraw')) {
        const body = failWithdraw
          ? errorDiagnosticXmi(500, 'persist failed')
          : OK_DIAGNOSTIC_XMI;
        const status = failWithdraw ? 503 : 200;
        failWithdraw = false;
        return new Response(body, { status, headers: { 'Content-Type': 'application/xml' } });
      }
      return fetchFn(input, init);
    }) as typeof fetch;

    const provider = new DdsrProviderImpl(new BrokerHttp({ brokerUrl: BROKER, fetchFn: routedFetch }));
    const fixture = paymentProvider('payments-ts');
    const registration = await provider.publish(fixture.provider, fixture.implementation);

    const failed = await registration.withdraw();
    expect(failed.severity).toBe('ERROR');
    const retried = await registration.withdraw();
    expect(retried.severity).toBe('OK');
    void requests;
  });

  it('withdrawAll withdraws every live registration', async () => {
    const { fetchFn, requests } = fakeFetch([
      { method: 'POST', urlIncludes: '/implementations', body: OK_DIAGNOSTIC_XMI },
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-1') },
      { method: 'POST', urlIncludes: '/implementations/withdraw', body: OK_DIAGNOSTIC_XMI },
    ]);
    const provider = new DdsrProviderImpl(new BrokerHttp({ brokerUrl: BROKER, fetchFn }));
    const fixture = paymentProvider('payments-ts');
    await provider.publish(fixture.provider, fixture.implementation);

    await provider.withdrawAll();
    expect(requests.filter(r => r.url.endsWith('/withdraw'))).toHaveLength(1);
    // second call is a no-op
    await provider.withdrawAll();
    expect(requests.filter(r => r.url.endsWith('/withdraw'))).toHaveLength(1);
  });
});
