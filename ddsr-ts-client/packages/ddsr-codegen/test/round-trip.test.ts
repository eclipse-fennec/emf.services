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
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

import { DDSRFactory } from '@ddsr/model';
import type { ServiceInterface } from '@ddsr/model';
import { fingerprint, props, serializeToXmi } from '@ddsr/client';
import { emitProvider, emitConsumer } from '../src/emit';
import { interfacesOf } from '../src/catalog';
import type * as PaymentStub from '@ddsr/stubs';

/**
 * The exports a generated module carries, taken from the stubs checked
 * in under packages/ddsr-stubs — same catalog, same emitter. Typing the
 * dynamic imports against them turns a slip in this file into a compile
 * error, and holds freshly generated code to the shape the checked-in
 * one has.
 */
type GeneratedProviderModule = Pick<
  typeof PaymentStub,
  'buildPaymentInterface' | 'PaymentOperationNames'
>;
type GeneratedConsumerModule = Pick<
  typeof PaymentStub,
  'PaymentInterfaceName' | 'PaymentVersion' | 'PaymentFingerprint'
>;

const factory = DDSRFactory.eINSTANCE;
// Inside the package, not in /tmp: the generated code imports @ddsr/model,
// and module resolution only finds it from within the workspace.
const here = dirname(fileURLToPath(import.meta.url));
const workspace = mkdtempSync(join(here, '.generated-'));
afterAll(() => rmSync(workspace, { recursive: true, force: true }));

/**
 * Contracts as the broker published them, read from the catalog document
 * checked in beside the generated stubs.
 *
 * Not rebuilt here: a contract typed out in a test only ever proves the
 * generator agrees with the test author. Payment carries what matters —
 * an optional parameter, an explicit index, documentation.
 */
function fromCatalog(name: string): ServiceInterface {
  const catalog = readFileSync(
    join(here, '..', '..', 'ddsr-stubs', 'catalog', 'services-catalog.xmi'), 'utf8');
  const found = interfacesOf(catalog).find(entry => entry.name === name);
  if (!found) throw new Error(`the catalog document holds no ${name} interface`);
  return found;
}

const paymentInterface = () => fromCatalog('Payment');

/** Write generated source into the workspace and import it. */
async function load<T>(source: string, name: string): Promise<T> {
  const file = join(workspace, name);
  writeFileSync(file, source, 'utf8');
  return await import(pathToFileURL(file).href) as T;
}

describe('generated provider code reproduces the contract', () => {
  it('build<Name>Interface() hashes to what it was generated from', async () => {
    const original = paymentInterface();
    const expected = fingerprint(original);

    const generated = emitProvider(original);
    const module = await load<GeneratedProviderModule>(generated.content, 'PaymentProvider.ts');
    const rebuilt = module.buildPaymentInterface();

    // The point of generating this rather than writing it: a detail that
    // drifts from the catalog changes the fingerprint, and consumers
    // addressing the contract stop finding the service.
    expect(fingerprint(rebuilt)).toBe(expected);
  });

  // The broker's own contracts carry what Payment does not: return slots
  // and parameters typed by eType, as proxies into the services model,
  // and multi-valued slots (#41).
  it.each(['BrokerCatalog', 'BrokerImplementations', 'BrokerLookup'])(
    'reproduces %s, eTypes and bounds included', async name => {
      const original = fromCatalog(name);
      const module = await load<Record<string, () => ServiceInterface>>(
        emitProvider(original).content, `${name}Provider.ts`);
      expect(fingerprint(module[`build${name}Interface`]())).toBe(fingerprint(original));
    });

  // What no published contract carries yet, but sd1 does: a security
  // requirement on both levels, a deprecated status, and an exception
  // with a property of every type. Added to Payment from the catalog.
  it('reproduces security, status and every property type', async () => {
    const original = paymentInterface();
    original.status = 'DEPRECATED';
    const interfaceSecurity = factory.createSecurityRequirement();
    interfaceSecurity.authenticated = true;
    interfaceSecurity.scopes.push('payments:read', 'payments:write');
    original.security = interfaceSecurity;
    const operation = original.operations.at(0)!;
    const operationSecurity = factory.createSecurityRequirement();
    operationSecurity.scopes.push('payments:charge');
    operation.security = operationSecurity;
    const exception = factory.createServiceException();
    exception.name = 'Declined';
    exception.type = 'org.example.Declined';
    exception.properties.push(
      props.string('reason', 'a "quoted" reason'), props.int('code', 402), props.long('since', 1700000000000),
      props.short('retries', 3), props.double('limit', 0.1), props.float('ratio', 0.3),
      props.bool('final', true), props.stringList('tags', ['a b', '']));
    original.exceptions.push(exception);
    operation.exceptions.push(exception);

    const module = await load<GeneratedProviderModule>(emitProvider(original).content, 'SecuredProvider.ts');
    expect(fingerprint(module.buildPaymentInterface())).toBe(fingerprint(original));
  });

  it('survives a trip through the wire form the broker would send', async () => {
    const original = paymentInterface();
    const fromWire = interfacesOf(serializeToXmi(original as never))[0];

    const module = await load<GeneratedProviderModule>(emitProvider(fromWire).content, 'WireProvider.ts');
    expect(fingerprint(module.buildPaymentInterface())).toBe(fingerprint(original));
  });

  it('names the operations it declares', async () => {
    const module = await load<GeneratedProviderModule>(emitProvider(paymentInterface()).content, 'NamesProvider.ts');
    expect(module.PaymentOperationNames).toEqual(['charge', 'getBalance']);
  });
});

describe('generated consumer code', () => {
  it('carries the fingerprint of the contract it was generated from', async () => {
    const original = paymentInterface();
    const module = await load<GeneratedConsumerModule>(emitConsumer(original).content, 'Payment.ts');

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
    // puts ServiceInterface into the imports, while the provider file
    // always needs it for the return type of build<Name>Interface().
    const source = emitProvider(fromCatalog('BrokerCatalog')).content;
    const importLine = source.split('\n').find(line => line.startsWith('import type') && line.endsWith("from '@ddsr/model';"))!;

    expect(importLine.match(/\bServiceInterface\b/g)).toHaveLength(1);
    expect(importLine).toContain('Diagnostic');
  });
});
