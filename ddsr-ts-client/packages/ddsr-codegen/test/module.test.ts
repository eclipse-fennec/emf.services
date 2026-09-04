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
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { basename, dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { DefaultServiceRegistry } from '@eclipse-daanse/tsm';
import type { ModuleContext } from '@eclipse-daanse/tsm';
import { DDSRFactory } from '@ddsr/model';
import type { ServiceInterface } from '@ddsr/model';
import { fingerprint, serviceProxy } from '@ddsr/client';
import type { DdsrClient, DdsrConsumer, ServiceLocator } from '@ddsr/client';
import { publishClient } from '@ddsr/tsm';
import { emitConsumer, emitModule } from '../src/emit';
import { interfacesOf } from '../src/catalog';

const factory = DDSRFactory.eINSTANCE;
const here = dirname(fileURLToPath(import.meta.url));
const workspace = mkdtempSync(join(here, '.generated-'));
afterAll(() => rmSync(workspace, { recursive: true, force: true }));

function paymentInterface(): ServiceInterface {
  const catalog = readFileSync(
    join(here, '..', '..', 'ddsr-stubs', 'catalog', 'services-catalog.xmi'), 'utf8');
  const payment = interfacesOf(catalog).find(entry => entry.name === 'Payment');
  if (!payment) throw new Error('the catalog document holds no Payment interface');
  return payment;
}

/** A client whose consumer answers one lookup, and records the invoke. */
function fakeClient(contract: ServiceInterface, present = true) {
  const invoke = vi.fn(async () => 3800);
  const reference = factory.createServiceReference();
  reference.id = 'ref-1';
  const locator = {
    reference,
    implementation: factory.createServiceImplementation(),
    serviceInterface: contract,
    restFlavor: () => undefined,
    flavors: () => [],
    invoke,
  } as unknown as ServiceLocator;

  const find = vi.fn(async () => (present ? [locator] : []));
  const consumer = {
    find,
    findOne: async () => (present ? locator : undefined),
    getService: async <T>() => serviceProxy<T>(locator),
    addServiceListener: () => () => undefined,
    removeServiceListener: () => undefined,
  } as unknown as DdsrConsumer;

  return { client: { consumer } as unknown as DdsrClient, invoke, find };
}

/** The context a loader hands a module. */
function moduleContext(registry: DefaultServiceRegistry): ModuleContext {
  return {
    services: registry,
    log: { info: () => undefined, warn: () => undefined, error: () => undefined, debug: () => undefined },
  } as unknown as ModuleContext;
}

interface GeneratedModule {
  Payment: string;
  activate(context: ModuleContext): Promise<void>;
  deactivate(): Promise<void>;
}

/**
 * A module and the contract it imports, in a directory of their own.
 *
 * The generated module does `import { PaymentFingerprint } from
 * './Payment.js'` — the two are written as a pair and only work as one,
 * which is itself worth having a test walk through.
 */
async function loadModule(contract: ServiceInterface, caseName: string): Promise<GeneratedModule> {
  const directory = join(workspace, caseName);
  mkdirSync(directory, { recursive: true });
  const consumer = emitConsumer(contract);
  const module = emitModule(contract);
  writeFileSync(join(directory, basename(consumer.path)), consumer.content, 'utf8');
  const modulePath = join(directory, basename(module.path));
  writeFileSync(modulePath, module.content, 'utf8');
  return await import(pathToFileURL(modulePath).href) as unknown as GeneratedModule;
}

describe('the generated TSM module', () => {
  it('puts the service in the registry, where a component injects it', async () => {
    const contract = paymentInterface();
    const registry = new DefaultServiceRegistry();
    const { client, invoke } = fakeClient(contract);
    publishClient(registry, client);

    const module = await loadModule(contract, 'inject');
    await module.activate(moduleContext(registry));

    // What @inject(Payment) would hand a component — no consumer, no
    // findOne, no undefined to deal with.
    const payment = registry.getRequired(module.Payment) as {
      charge(amount: number, currency?: string): Promise<number>;
    };
    await expect(payment.charge(4200, 'EUR')).resolves.toBe(3800);
    expect(invoke).toHaveBeenCalledWith('charge', { amount: 4200, currency: 'EUR' });
  });

  it('mirrors only implementations that answer to this contract', async () => {
    const contract = paymentInterface();
    const registry = new DefaultServiceRegistry();
    const { client, find } = fakeClient(contract);
    publishClient(registry, client);

    const module = await loadModule(contract, 'fingerprint');
    await module.activate(moduleContext(registry));

    // Without the fingerprint the broker would answer with whatever
    // carries the name, including a provider whose operations have moved.
    expect(find).toHaveBeenCalledWith('Payment', undefined, fingerprint(contract));
  });

  it('registers nothing when the broker holds no such contract', async () => {
    const contract = paymentInterface();
    const registry = new DefaultServiceRegistry();
    const { client } = fakeClient(contract, false);
    publishClient(registry, client);

    const module = await loadModule(contract, 'empty');
    await module.activate(moduleContext(registry));

    // A component depending on it stays unsatisfied rather than being
    // handed something that is not there.
    expect(registry.has(module.Payment)).toBe(false);
  });

  it('takes the service back when the module is deactivated', async () => {
    const contract = paymentInterface();
    const registry = new DefaultServiceRegistry();
    const { client } = fakeClient(contract);
    publishClient(registry, client);

    const module = await loadModule(contract, 'lifecycle');
    await module.activate(moduleContext(registry));
    expect(registry.has(module.Payment)).toBe(true);

    await module.deactivate();
    expect(registry.has(module.Payment)).toBe(false);
  });
});
