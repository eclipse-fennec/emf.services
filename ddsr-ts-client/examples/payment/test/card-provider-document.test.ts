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

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { deserializeFromXmi, fingerprint, toArray } from '@ddsr/client';
import type { ServiceImplementation, ServiceInterface, ServiceProvider } from '@ddsr/model';
import type { EObject } from '@emfts/core';

const golden = fileURLToPath(new URL('../../../../itest/fixtures/fingerprint/payment.sd1', import.meta.url));
const document = new URL('../card-provider.xmi', import.meta.url);

function roots(): EObject[] {
  const read = deserializeFromXmi(readFileSync(document, 'utf8'), document.href);
  return (Array.isArray(read) ? read : [read]) as EObject[];
}

/** The card provider of the tag example (#281). */
describe('the card provider document', () => {
  it('serves Payment unchanged — the golden sd1 — and InstallmentPlan, from one implementation', () => {
    const all = roots();
    const contracts = all.filter(root => root.eClass().getName() === 'ServiceInterface') as unknown as ServiceInterface[];
    const payment = contracts.find(contract => contract.name === 'Payment')!;
    expect(fingerprint(payment)).toBe(readFileSync(golden, 'utf8').trim());
    expect(contracts.map(contract => contract.name)).toEqual(['Payment', 'InstallmentPlan']);

    const provider = all.find(root => root.eClass().getName() === 'ServiceProvider') as unknown as ServiceProvider;
    const implementation = toArray<ServiceImplementation>(provider.implementations)[0];
    expect(toArray<ServiceInterface>(implementation.serviceInterfaces).map(contract => contract.name))
      .toEqual(['Payment', 'InstallmentPlan']);
  });

  it('asks for the tags of its method and its terms', () => {
    const all = roots();
    const provider = all.find(root => root.eClass().getName() === 'ServiceProvider') as unknown as ServiceProvider;
    expect([...toArray<ServiceImplementation>(provider.implementations)[0].tags]).toEqual(['payment/method/card']);
    const contracts = all.filter(root => root.eClass().getName() === 'ServiceInterface') as unknown as ServiceInterface[];
    expect(contracts.map(contract => [...contract.tags])).toEqual([['payment/terms/instant'], ['payment/terms/installment']]);
  });
});
