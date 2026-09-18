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
import type { Parameter, RestFlavor } from '@ddsr/model';
import { DDSRFactory, HttpMethod, ParameterBinding } from '@ddsr/model';
import { DdsrProviderImpl } from '../src/internal/provider-impl';
import { BrokerHttp } from '../src/internal/broker-http';
import { OK_DIAGNOSTIC_XMI, fakeFetch, lookupResultXmi, paymentProvider } from './fixtures';
import { toArray } from '../src/internal/emf-util';

const factory = DDSRFactory.eINSTANCE;
const BROKER = 'http://broker.test/ddsr/rest';

/**
 * What a TypeScript provider puts on the wire when it declares where its
 * arguments go (#82).
 *
 * A binding that does not say WHICH parameter it places is inert: a
 * consumer reading it cannot know what to bind, so it falls back to the
 * undeclared placement and the provider's statement is lost. That is
 * what happened while the serializer dropped single-valued
 * cross-references — the bindings travelled, their subjects did not.
 * These tests read the published body, not the object graph, because
 * the body is the only thing the other language ever sees.
 */
describe('a published flavor carries its references', () => {
  function providerWithMixedBindings() {
    const fixture = paymentProvider('payments-ts');
    const contract = fixture.serviceInterface;
    const charge = toArray<{ name?: string; parameters: unknown }>(contract.operations)
      .find(o => o.name === 'charge')!;

    const flavor = toArray<RestFlavor>(fixture.implementation.flavors)[0];
    const opFlavor = factory.createRestOperationFlavor();
    opFlavor.name = 'charge';
    opFlavor.method = HttpMethod.POST;
    opFlavor.path = '/charge/{amount}';
    opFlavor.operation = charge as never;

    const [amount, currency] = toArray<Parameter>(charge.parameters);
    for (const [parameter, binding, wireName] of [
      [amount, ParameterBinding.PATH, undefined],
      [currency, ParameterBinding.HEADER, 'X-Currency'],
    ] as const) {
      const placement = factory.createRestParameterBinding();
      placement.parameter = parameter;
      placement.binding = binding;
      if (wireName) placement.wireName = wireName;
      opFlavor.parameterBindings.push(placement);
    }
    flavor.operationFlavors.push(opFlavor);
    return fixture;
  }

  async function publishedBody(): Promise<string> {
    const { fetchFn, requests } = fakeFetch([
      { method: 'POST', urlIncludes: '/implementations', body: OK_DIAGNOSTIC_XMI },
      { urlIncludes: '/references', body: lookupResultXmi('payments-ts', 'ref-82') },
    ]);
    const provider = new DdsrProviderImpl(new BrokerHttp({ brokerUrl: BROKER, fetchFn }));
    const fixture = providerWithMixedBindings();

    await provider.publish(fixture.provider, fixture.implementation);

    return requests.find(r => r.method === 'POST' && r.url.includes('/implementations'))!.body!;
  }

  it('names the operation it is a flavor of', async () => {
    expect(await publishedBody()).toContain('operation="/1/@operations.0"');
  });

  it('names the parameter each binding places, and where it places it', async () => {
    const body = await publishedBody();
    expect(body).toContain('parameter="/1/@operations.0/@parameters.0" binding="PATH"');
    expect(body).toContain('parameter="/1/@operations.0/@parameters.1" binding="HEADER" wireName="X-Currency"');
  });

  it('leaves no binding without a subject — an inert binding is worse than none', async () => {
    const body = await publishedBody();
    const bindings = body.match(/<parameterBindings[^>]*>/g) ?? [];
    expect(bindings).toHaveLength(2);
    for (const binding of bindings) {
      expect(binding).toMatch(/parameter="[^"]+"/);
    }
  });
});
