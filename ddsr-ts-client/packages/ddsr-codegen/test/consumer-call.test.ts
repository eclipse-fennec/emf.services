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

import { afterAll, describe, expect, it, vi } from 'vitest';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { DDSRFactory } from '@ddsr/model';
import type { ServiceInterface } from '@ddsr/model';
import { fingerprint, serviceProxy } from '@ddsr/client';
import type { DdsrConsumer, ServiceLocator } from '@ddsr/client';
import { emitConsumer } from '../src/emit';

const factory = DDSRFactory.eINSTANCE;
const here = dirname(fileURLToPath(import.meta.url));
const workspace = mkdtempSync(join(here, '.generated-'));
afterAll(() => rmSync(workspace, { recursive: true, force: true }));

/**
 * The contract as the broker's catalog has it: the second parameter of
 * charge is a currency, and it is optional.
 */
function paymentInterface(): ServiceInterface {
  const serviceInterface = factory.createServiceInterface();
  serviceInterface.name = 'Payment';
  serviceInterface.version = '1.0.0';

  const charge = factory.createServiceOperation();
  charge.name = 'charge';
  charge.returnType = 'double';
  const amount = factory.createParameter();
  amount.name = 'amount';
  amount.type = 'double';
  charge.parameters.push(amount);
  const currency = factory.createParameter();
  currency.name = 'currency';
  currency.type = 'string';
  currency.index = 1;
  currency.optional = true;
  charge.parameters.push(currency);
  serviceInterface.operations.push(charge);

  return serviceInterface;
}

async function loadGenerated(contract: ServiceInterface, name: string): Promise<Record<string, any>> {
  const file = join(workspace, name);
  writeFileSync(file, emitConsumer(contract).content, 'utf8');
  return import(pathToFileURL(file).href);
}

/**
 * A consumer that hands out the real proxy, so a call travels the way it
 * does in production: generated getPayment() -> getService() ->
 * serviceProxy() -> locator.invoke().
 */
function fakeConsumer(contract: ServiceInterface, answer: unknown = 3800) {
  const invoke = vi.fn(async () => answer);
  const locator = {
    reference: factory.createServiceReference(),
    implementation: factory.createServiceImplementation(),
    serviceInterface: contract,
    restFlavor: () => undefined,
    flavors: () => [],
    invoke,
  } as unknown as ServiceLocator;

  const findOne = vi.fn(async () => locator);
  const consumer = {
    find: async () => [locator],
    findOne,
    getService: async <T>() => serviceProxy<T>(locator),
    addServiceListener: () => () => undefined,
    removeServiceListener: () => undefined,
  } as unknown as DdsrConsumer;

  return { consumer, invoke, findOne };
}

describe('a call through the generated consumer', () => {
  it('charge(4200, "EUR") arrives as invoke("charge", { amount, currency })', async () => {
    const contract = paymentInterface();
    const { consumer, invoke } = fakeConsumer(contract);
    const module = await loadGenerated(contract, 'Payment.ts');

    const payment = await module.getPayment(consumer);
    const answer = await payment.charge(4200, 'EUR');

    // The positional-to-named mapping is the whole point. Written by
    // hand, this file once declared charge(amount, accountId) — and an
    // account id went out as a currency with nothing to catch it.
    expect(invoke).toHaveBeenCalledWith('charge', { amount: 4200, currency: 'EUR' });
    expect(answer).toBe(3800);
  });

  it('leaves out what the caller left out', async () => {
    const contract = paymentInterface();
    const { consumer, invoke } = fakeConsumer(contract);
    const module = await loadGenerated(contract, 'PaymentOptional.ts');

    const payment = await module.getPayment(consumer);
    await payment.charge(99);

    expect(invoke).toHaveBeenCalledWith('charge', { amount: 99 });
  });

  it('looks the service up by its fingerprint, not by name alone', async () => {
    const contract = paymentInterface();
    const { consumer, findOne } = fakeConsumer(contract);
    const module = await loadGenerated(contract, 'PaymentLookup.ts');

    await module.getPayment(consumer, '(payments.sandbox=true)');

    expect(findOne).toHaveBeenCalledWith('Payment', '(payments.sandbox=true)', fingerprint(contract));
  });

  it('answers undefined when the broker knows no such contract', async () => {
    const contract = paymentInterface();
    const module = await loadGenerated(contract, 'PaymentMissing.ts');
    const consumer = {
      findOne: async () => undefined,
      getService: async () => undefined,
    } as unknown as DdsrConsumer;

    expect(await module.getPayment(consumer)).toBeUndefined();
  });
});
