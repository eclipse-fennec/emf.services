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
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { DDSRFactory } from '@ddsr/model';
import type { ServiceInterface } from '@ddsr/model';
import { fingerprint, serviceProxy } from '@ddsr/client';
import type { DdsrConsumer, ServiceLocator } from '@ddsr/client';
import { emitConsumer } from '../src/emit';
import { interfacesOf } from '../src/catalog';
import type * as PaymentStub from '@ddsr/stubs';

/**
 * What a generated consumer module exports, taken from the stubs
 * checked in under packages/ddsr-stubs — generated from this same
 * catalog by this same emitter.
 *
 * Typing the dynamic import against it does two things: a slip in this
 * file is a compile error rather than a surprise at run time, and the
 * freshly generated code is held to the shape the checked-in one has.
 * If a change here made the two diverge, this stops compiling.
 */
type GeneratedPaymentModule = Pick<
  typeof PaymentStub,
  'getPayment' | 'PaymentInterfaceName' | 'PaymentVersion' | 'PaymentFingerprint'
>;

const factory = DDSRFactory.eINSTANCE;
const here = dirname(fileURLToPath(import.meta.url));
const workspace = mkdtempSync(join(here, '.generated-'));
afterAll(() => rmSync(workspace, { recursive: true, force: true }));

/**
 * The Payment contract as the broker actually published it.
 *
 * Read from the catalog document checked in beside the generated stubs
 * rather than rebuilt here. A contract typed out in a test is an
 * assumption about the catalog, and an assumption is what put an account
 * id where a currency belongs in the first place — a test carrying its
 * own copy of the mistake would have agreed with it.
 */
function paymentInterface(): ServiceInterface {
  const catalog = readFileSync(
    join(here, '..', '..', 'ddsr-stubs', 'catalog', 'services-catalog.xmi'), 'utf8');
  const payment = interfacesOf(catalog).find(entry => entry.name === 'Payment');
  if (!payment) throw new Error('the catalog document holds no Payment interface');
  return payment;
}

async function loadGenerated(contract: ServiceInterface, name: string): Promise<GeneratedPaymentModule> {
  const file = join(workspace, name);
  writeFileSync(file, emitConsumer(contract).content, 'utf8');
  return await import(pathToFileURL(file).href) as GeneratedPaymentModule;
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
    const answer = await payment!.charge(4200, 'EUR');

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
    await payment!.charge(99);

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
