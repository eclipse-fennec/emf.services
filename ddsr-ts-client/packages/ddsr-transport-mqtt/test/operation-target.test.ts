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
import { DDSRFactory } from '@ddsr/model';
import { operationHandlers } from '../src/operation-target';

function paymentContract() {
  const f = DDSRFactory.eINSTANCE;
  const charge = f.createServiceOperation();
  charge.name = 'charge';
  const amount = f.createParameter();
  amount.name = 'amount';
  amount.type = 'double';
  amount.index = 0;
  const currency = f.createParameter();
  currency.name = 'currency';
  currency.type = 'string';
  currency.index = 1;
  charge.parameters.push(amount, currency);
  const getBalance = f.createServiceOperation();
  getBalance.name = 'getBalance';
  const contract = f.createServiceInterface();
  contract.name = 'Payment';
  contract.operations.push(charge, getBalance);
  return contract;
}

describe('an object as the implementation of a contract', () => {
  it('applies named arguments in the order the contract declares', async () => {
    const seen: unknown[] = [];
    const handlers = operationHandlers(paymentContract(), {
      charge: (amount: number, currency: string) => {
        seen.push(amount, currency);
        return 987.5;
      },
      getBalance: () => 1000,
    });

    // The invocation document carries names; the contract says the order.
    const answer = await handlers.charge({ currency: 'EUR', amount: 12.5 });

    expect(seen).toEqual([12.5, 'EUR']);
    expect(answer).toBe(987.5);
    expect(Object.keys(handlers).sort()).toEqual(['charge', 'getBalance']);
  });

  it('subscribes to nothing it cannot answer', () => {
    const handlers = operationHandlers(paymentContract(), { getBalance: () => 1000 });

    // A provider may serve part of a contract while the rest is
    // somebody else's; a handler that throws is a worse answer than no
    // subscription.
    expect(Object.keys(handlers)).toEqual(['getBalance']);
  });

  it('says so instead, when a caller asks it to', () => {
    expect(() => operationHandlers(paymentContract(), { getBalance: () => 1000 }, { onMissing: 'fail' }))
      .toThrow("has no operation 'charge'");
  });
});
