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

import { afterAll, describe, expect, it } from 'vitest';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
import { DDSRFactory } from '@ddsr/model';
import type { ServiceInterface } from '@ddsr/model';
import { fingerprint, serializeToXmi } from '@ddsr/client';
import { emitProvider, emitConsumer } from '../src/emit';
import { interfacesOf } from '../src/catalog';

const factory = DDSRFactory.eINSTANCE;
// Inside the package, not in /tmp: the generated code imports @ddsr/model,
// and module resolution only finds it from within the workspace.
const workspace = mkdtempSync(join(__dirname, '.generated-'));
afterAll(() => rmSync(workspace, { recursive: true, force: true }));

/** A contract with the shapes that matter: optional, ordered, documented. */
function paymentInterface(): ServiceInterface {
  const serviceInterface = factory.createServiceInterface();
  serviceInterface.name = 'Payment';
  serviceInterface.version = '1.0.0';
  serviceInterface.description = 'Payment processing service.';

  const charge = factory.createServiceOperation();
  charge.name = 'charge';
  charge.returnType = 'double';
  charge.description = 'Charge an amount.';
  const amount = factory.createParameter();
  amount.name = 'amount';
  amount.type = 'double';
  charge.parameters.push(amount);
  const currency = factory.createParameter();
  currency.name = 'currency';
  currency.type = 'string';
  currency.index = 1;
  currency.optional = true;
  currency.defaultValue = 'EUR';
  charge.parameters.push(currency);
  serviceInterface.operations.push(charge);

  const balance = factory.createServiceOperation();
  balance.name = 'getBalance';
  balance.returnType = 'double';
  const accountId = factory.createParameter();
  accountId.name = 'accountId';
  accountId.type = 'string';
  balance.parameters.push(accountId);
  serviceInterface.operations.push(balance);

  return serviceInterface;
}

/** Write generated source into the workspace and import it. */
async function load(source: string, name: string): Promise<Record<string, any>> {
  const file = join(workspace, name);
  writeFileSync(file, source, 'utf8');
  return import(pathToFileURL(file).href);
}

describe('generated provider code reproduces the contract', () => {
  it('build<Name>Interface() hashes to what it was generated from', async () => {
    const original = paymentInterface();
    const expected = fingerprint(original);

    const generated = emitProvider(original);
    const module = await load(generated.content, 'PaymentProvider.ts');
    const rebuilt = module.buildPaymentInterface() as ServiceInterface;

    // The point of generating this rather than writing it: a detail that
    // drifts from the catalog changes the fingerprint, and consumers
    // addressing the contract stop finding the service.
    expect(fingerprint(rebuilt)).toBe(expected);
  });

  it('survives a trip through the wire form the broker would send', async () => {
    const original = paymentInterface();
    const fromWire = interfacesOf(serializeToXmi(original as never))[0];

    const module = await load(emitProvider(fromWire).content, 'WireProvider.ts');
    expect(fingerprint(module.buildPaymentInterface())).toBe(fingerprint(original));
  });

  it('names the operations it declares', async () => {
    const module = await load(emitProvider(paymentInterface()).content, 'NamesProvider.ts');
    expect(module.PaymentOperationNames).toEqual(['charge', 'getBalance']);
  });
});

describe('generated consumer code', () => {
  it('carries the fingerprint of the contract it was generated from', async () => {
    const original = paymentInterface();
    const module = await load(emitConsumer(original).content, 'Payment.ts');

    expect(module.PaymentFingerprint).toBe(fingerprint(original));
    expect(module.PaymentInterfaceName).toBe('Payment');
    expect(module.PaymentVersion).toBe('1.0.0');
  });

  it('declares optional parameters as optional and keeps their order', () => {
    const source = emitConsumer(paymentInterface()).content;
    expect(source).toContain('charge(amount: number, currency?: string): Promise<number>;');
    expect(source).toContain('getBalance(accountId: string): Promise<number>;');
  });

  it('passes documentation through to the call site', () => {
    const source = emitConsumer(paymentInterface()).content;
    expect(source).toContain('Charge an amount.');
  });

  it('imports a model type once, even when an operation also takes it', () => {
    // BrokerCatalog.addCatalogEntry(serviceInterface: ServiceInterface)
    // put ServiceInterface into the imports while the provider file
    // always needs it for the return type of build<Name>Interface().
    const serviceInterface = factory.createServiceInterface();
    serviceInterface.name = 'Catalog';
    const add = factory.createServiceOperation();
    add.name = 'addCatalogEntry';
    add.returnType = 'Diagnostic';
    const entry = factory.createParameter();
    entry.name = 'serviceInterface';
    entry.type = 'ServiceInterface';
    add.parameters.push(entry);
    serviceInterface.operations.push(add);

    const source = emitProvider(serviceInterface).content;
    const importLine = source.split('\n').find(line => line.startsWith('import type'))!;

    expect(importLine).toBe("import type { Diagnostic, ServiceInterface } from '@ddsr/model';");
    expect(importLine.match(/ServiceInterface/g)).toHaveLength(1);
  });
});
