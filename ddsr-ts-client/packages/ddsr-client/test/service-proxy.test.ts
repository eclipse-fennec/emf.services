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
import { DdsrServiceError, serviceProxy } from '../src/index';
import type { ServiceLocator } from '../src/index';
import type { ServiceInterface, ServiceOperation } from '@ddsr/model';

class PersonNotFoundException extends Error {
  constructor(message: string, readonly cause: DdsrServiceError) {
    super(message);
  }
}

function failingWith(error: unknown): ServiceLocator {
  const serviceInterface = DDSRFactory.eINSTANCE.createServiceInterface();
  return { serviceInterface, invoke: async () => { throw error; } } as unknown as ServiceLocator;
}

function declared(type: string): DdsrServiceError {
  const exception = DDSRFactory.eINSTANCE.createServiceException();
  exception.name = 'PersonNotFound';
  exception.type = type;
  return new DdsrServiceError('no person 42', exception, 404);
}

describe('serviceProxy and the declared errors of a contract', () => {
  const exceptions = { 'fennec.services.example.PersonNotFound': PersonNotFoundException };

  it('throws the generated class for a declared error it knows', async () => {
    const service = serviceProxy<{ get(id: string): Promise<unknown> }>(
      failingWith(declared('fennec.services.example.PersonNotFound')), exceptions);

    const error = await service.get('42').catch((e: unknown) => e);
    expect(error).toBeInstanceOf(PersonNotFoundException);
    expect((error as Error).message).toBe('no person 42');
    expect((error as PersonNotFoundException).cause.status).toBe(404);
  });

  it('leaves a declared error it has no class for as it is', async () => {
    const original = declared('org.example.Other');
    const service = serviceProxy<{ get(id: string): Promise<unknown> }>(failingWith(original), exceptions);

    await expect(service.get('42')).rejects.toBe(original);
  });

  it('leaves every other error alone', async () => {
    const original = new Error('boom');
    const service = serviceProxy<{ get(id: string): Promise<unknown> }>(failingWith(original), exceptions);

    await expect(service.get('42')).rejects.toBe(original);
  });
});

describe('serviceProxy and a slot that is a stream', () => {
  // Parameter.delivery: the contract can say a slot is a stream. One
  // that comes back is iterated, and nothing is sent before the first
  // value is asked for.
  function readingsContract(): { serviceInterface: ServiceInterface; readings: ServiceOperation } {
    const factory = DDSRFactory.eINSTANCE;
    const serviceInterface = factory.createServiceInterface();
    const readings = factory.createServiceOperation();
    readings.name = 'readings';
    const result = factory.createParameter();
    result.name = 'result';
    result.type = 'double';
    result.upperBound = -1;
    result.delivery = 'STREAM';
    readings.returnValue = result;
    serviceInterface.operations.push(readings);
    return { serviceInterface, readings };
  }

  it('hands back a stream to iterate, and calls when the first value is asked for', async () => {
    const { serviceInterface } = readingsContract();
    let invoked = 0;
    const locator = {
      serviceInterface,
      invoke: async () => {
        invoked++;
        return (async function* () { yield 1.5; yield 2.5; })();
      },
    } as unknown as ServiceLocator;

    const values = serviceProxy<{ readings(): AsyncIterable<number> }>(locator).readings();

    expect(invoked).toBe(0);
    const seen: number[] = [];
    for await (const value of values) seen.push(value);
    expect(seen).toEqual([1.5, 2.5]);
    expect(invoked).toBe(1);
  });

  it('says so where the transport that carried the call carries no streams', async () => {
    const { serviceInterface } = readingsContract();
    const locator = { serviceInterface, invoke: async () => [1.5, 2.5] } as unknown as ServiceLocator;

    const values = serviceProxy<{ readings(): AsyncIterable<number> }>(locator).readings();

    await expect((async () => { for await (const value of values) void value; })())
      .rejects.toThrow(/carries no streams/);
  });

  it('refuses a stream handed over as an argument, and says which', () => {
    const { serviceInterface, readings } = readingsContract();
    const samples = DDSRFactory.eINSTANCE.createParameter();
    samples.name = 'samples';
    samples.type = 'double';
    samples.upperBound = -1;
    samples.delivery = 'STREAM';
    readings.parameters.push(samples);
    let invoked = false;
    const locator = { serviceInterface, invoke: async () => { invoked = true; } } as unknown as ServiceLocator;

    const service = serviceProxy<{ readings(samples: AsyncIterable<number>): AsyncIterable<number> }>(locator);

    expect(() => service.readings((async function* () { yield 1; })()))
      .toThrow(/readings takes streams as arguments \[samples\]/);
    expect(invoked).toBe(false);
  });
});
