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

import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DefaultServiceRegistry } from '@eclipse-daanse/tsm';
import { DDSRFactory } from '@ddsr/model';
import type { ServiceEvent } from '@ddsr/model';
import { props } from '@ddsr/client';
import type { DdsrConsumer, DdsrServiceListener, ServiceLocator } from '@ddsr/client';
import { mirrorRemoteServices } from '../src/remote-mirror';
import { remoteServiceId } from '../src/contracts';

const factory = DDSRFactory.eINSTANCE;

/** A located service the broker could have answered with. */
function locator(referenceId: string, properties: Record<string, string | number>): ServiceLocator {
  const reference = factory.createServiceReference();
  reference.id = referenceId;
  for (const [name, value] of Object.entries(properties)) {
    reference.properties.push(
      typeof value === 'number' ? props.int(name, value) : props.string(name, value)
    );
  }

  const serviceInterface = factory.createServiceInterface();
  serviceInterface.name = 'Payment';
  const charge = factory.createServiceOperation();
  charge.name = 'charge';
  const amount = factory.createParameter();
  amount.name = 'amount';
  charge.parameters.push(amount);
  serviceInterface.operations.push(charge);

  return {
    reference,
    serviceInterface,
    implementation: factory.createServiceImplementation(),
    restFlavor: () => undefined,
    flavors: () => [],
    invoke: vi.fn(async (operation: string, params?: Record<string, unknown>) =>
      `${referenceId}:${operation}(${JSON.stringify(params ?? {})})`),
  } as unknown as ServiceLocator;
}

/** A consumer that answers with what the test put in front of it. */
class FakeConsumer implements DdsrConsumer {
  locators: ServiceLocator[] = [];
  listener: DdsrServiceListener | undefined;
  unsubscribed = false;
  findCalls = 0;

  async find(): Promise<ServiceLocator[]> {
    this.findCalls++;
    return this.locators;
  }
  async findOne(): Promise<ServiceLocator | undefined> { return this.locators[0]; }
  async getService<T>(): Promise<T | undefined> { return undefined; }
  addServiceListener(_i: string, _f: string | undefined, listener: DdsrServiceListener): () => void {
    this.listener = listener;
    return () => { this.unsubscribed = true; };
  }
  removeServiceListener(): void { this.listener = undefined; }

  emit(type: string, referenceId?: string): void {
    const event = factory.createServiceEvent();
    event.type = type as never;
    if (referenceId) {
      const reference = factory.createServiceReference();
      reference.id = referenceId;
      event.reference = reference;
    }
    this.listener?.(event as ServiceEvent);
  }
}

describe('remote services in a TSM registry', () => {
  let registry: DefaultServiceRegistry;
  let consumer: FakeConsumer;

  beforeEach(() => {
    registry = new DefaultServiceRegistry();
    consumer = new FakeConsumer();
  });

  const id = remoteServiceId('Payment');

  it('registers every implementation on its own, so several coexist', async () => {
    consumer.locators = [
      locator('ref-1', { 'payments.sandbox': 'true' }),
      locator('ref-2', { 'payments.sandbox': 'false' }),
    ];
    const mirror = await mirrorRemoteServices({ registry, consumer, interfaceName: 'Payment' });

    expect(mirror.mirrored()).toEqual(['ref-1', 'ref-2']);
    expect(registry.countProviders(id)).toBe(2);
  });

  it('carries the DDSR properties over, so a target filter selects on them', async () => {
    consumer.locators = [
      locator('ref-1', { 'payments.sandbox': 'true' }),
      locator('ref-2', { 'payments.sandbox': 'false' }),
    ];
    await mirrorRemoteServices({ registry, consumer, interfaceName: 'Payment' });

    const sandbox = registry.getMatching(id, '(payments.sandbox=true)') as { charge(a: number): Promise<string> };
    await expect(sandbox.charge(100)).resolves.toContain('ref-1');
    expect(registry.getMatching(id, '(ddsr.interface=Payment)')).toBeDefined();
  });

  it('service.ranking decides which one get() answers with', async () => {
    consumer.locators = [
      locator('ref-low', { 'service.ranking': 1 }),
      locator('ref-high', { 'service.ranking': 10 }),
    ];
    await mirrorRemoteServices({ registry, consumer, interfaceName: 'Payment' });

    const best = registry.get(id) as { charge(a: number): Promise<string> };
    await expect(best.charge(1)).resolves.toContain('ref-high');
  });

  it('calls reach the service through the locator, with named parameters', async () => {
    const one = locator('ref-1', {});
    consumer.locators = [one];
    await mirrorRemoteServices({ registry, consumer, interfaceName: 'Payment' });

    const service = registry.get(id) as { charge(a: number): Promise<string> };
    await expect(service.charge(250)).resolves.toBe('ref-1:charge({"amount":250})');
    expect(one.invoke).toHaveBeenCalledWith('charge', { amount: 250 });
  });

  it('withdraws a provider on UNREGISTERING, without asking the broker again', async () => {
    consumer.locators = [locator('ref-1', {}), locator('ref-2', {})];
    const mirror = await mirrorRemoteServices({ registry, consumer, interfaceName: 'Payment' });
    const callsAfterStart = consumer.findCalls;

    consumer.emit('UNREGISTERING', 'ref-1');

    expect(mirror.mirrored()).toEqual(['ref-2']);
    expect(registry.countProviders(id)).toBe(1);
    expect(consumer.findCalls).toBe(callsAfterStart);
  });

  it('picks up a provider that appears while it is running', async () => {
    consumer.locators = [locator('ref-1', {})];
    const mirror = await mirrorRemoteServices({ registry, consumer, interfaceName: 'Payment' });

    consumer.locators = [...consumer.locators, locator('ref-2', {})];
    consumer.emit('REGISTERED', 'ref-2');
    await vi.waitFor(() => expect(mirror.mirrored()).toEqual(['ref-1', 'ref-2']));
    expect(registry.countProviders(id)).toBe(2);
  });

  it('drops what the broker stopped listing, even with no event for it', async () => {
    consumer.locators = [locator('ref-1', {}), locator('ref-2', {})];
    const mirror = await mirrorRemoteServices({ registry, consumer, interfaceName: 'Payment' });

    // A reconnect can swallow an UNREGISTERING; the snapshot is the truth.
    consumer.locators = [consumer.locators[1]];
    await mirror.refresh();

    expect(mirror.mirrored()).toEqual(['ref-2']);
  });

  it('close() takes everything back and stops listening', async () => {
    consumer.locators = [locator('ref-1', {}), locator('ref-2', {})];
    const mirror = await mirrorRemoteServices({ registry, consumer, interfaceName: 'Payment' });

    await mirror.close();

    expect(mirror.mirrored()).toEqual([]);
    expect(registry.countProviders(id)).toBe(0);
    expect(registry.get(id)).toBeUndefined();
    expect(consumer.unsubscribed).toBe(true);
  });
});
