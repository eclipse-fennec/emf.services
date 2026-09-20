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
import type { ServiceOperation } from '@ddsr/model';
import {
  decodeInvocation, decodeResult, encodeFailure, encodeInvocation, encodeResult,
} from '../src/invocation/invocation';

const f = DDSRFactory.eINSTANCE;

function charge(): ServiceOperation {
  const operation = f.createServiceOperation();
  operation.name = 'charge';
  const amount = f.createParameter();
  amount.name = 'amount';
  amount.type = 'double';
  amount.index = 0;
  const times = f.createParameter();
  times.name = 'times';
  times.type = 'int';
  times.index = 1;
  operation.parameters.push(amount);
  operation.parameters.push(times);
  const contract = f.createServiceInterface();
  contract.name = 'Payment';
  contract.operations.push(operation);
  return operation;
}

describe('invocation as a message', () => {
  it('carries the call description with the call, so the references resolve', () => {
    const document = encodeInvocation(charge(), { amount: 10, times: 3 });
    expect(document).toContain('<services:ServiceInvocation operation="/1"');
    expect(document).toContain('parameter="/1/@parameters.0"');
    expect(document).toContain('services:ServiceOperation');
    expect(document).not.toContain('href');
  });

  it('leaves the contract it was called on alone', () => {
    const operation = charge();

    encodeInvocation(operation, { amount: 1 });

    // A call must not cost the caller its own model: the description
    // that travels is a copy, so the operation is still where it was,
    // with the parameters it had.
    expect(operation.name).toBe('charge');
    expect(operation.parameters.size?.() ?? (operation.parameters as unknown[]).length).toBe(2);
  });

  it('round-trips the values under the names they fill', () => {
    const decoded = decodeInvocation(encodeInvocation(charge(), { amount: 10.5, times: 3 }));

    expect(decoded.operation).toBe('charge');
    expect(decoded.args).toEqual({ amount: 10.5, times: 3 });
  });

  it('keeps an int an int, because the contract said so', () => {
    const document = encodeInvocation(charge(), { amount: 10, times: 3 });

    expect(document).toContain('xsi:type="services:DoubleProperty" name="amount"');
    expect(document).toContain('xsi:type="services:IntProperty" name="times"');
  });

  it('carries a modelled argument inside the message', () => {
    const store = f.createServiceOperation();
    store.name = 'store';
    const person = f.createParameter();
    person.name = 'person';
    store.parameters.push(person);
    const value = f.createServiceInterface();
    value.name = 'Payment';

    const decoded = decodeInvocation(encodeInvocation(store, { person: value }));
    expect((decoded.args.person as { name?: string })?.name).toBe('Payment');
  });

  it('round-trips a result', () => {
    expect(decodeResult(encodeResult(990))).toEqual({ value: 990 });
    expect(decodeResult(encodeResult('ok'))).toEqual({ value: 'ok' });
    expect(decodeResult(encodeResult(undefined))).toEqual({ value: undefined });
  });

  it('answers a failure with a diagnostic, not with a missing value', () => {
    const document = encodeFailure('account-1 is not known', 404);

    expect(document).toContain('severity="ERROR"');
    expect(decodeResult(document)).toEqual({ error: 'account-1 is not known' });
  });

  it('refuses a document that is not a call', () => {
    expect(() => decodeInvocation('<?xml version="1.0"?><nothing/>')).toThrow();
    expect(() => decodeResult('<?xml version="1.0"?><nothing/>')).toThrow();
  });
});
