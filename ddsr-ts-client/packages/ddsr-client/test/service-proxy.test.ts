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
  // Parameter.delivery: the contract can say a slot is a stream, and no
  // flavor plugin carries one yet. The caller is told, by name, instead
  // of being handed a Promise where it iterates.
  it('says that nothing carries the stream, and which slot it is', () => {
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
    let invoked = false;
    const locator = { serviceInterface, invoke: async () => { invoked = true; } } as unknown as ServiceLocator;

    const service = serviceProxy<{ readings(): AsyncIterable<number> }>(locator);

    expect(() => service.readings()).toThrow(/readings has stream slots \[return\]/);
    expect(invoked).toBe(false);
  });
});
