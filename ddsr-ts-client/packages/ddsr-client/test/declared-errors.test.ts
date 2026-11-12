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
import type { ServiceException, ServiceOperation } from '@ddsr/model';
import { declaredByCode, declaredCode, declaredRaisedBy, raisesDeclared } from '../src/declared-errors';

const factory = DDSRFactory.eINSTANCE;

function error(type: string, code?: number): ServiceException {
  const declared = factory.createServiceException();
  declared.name = type.slice(type.lastIndexOf('.') + 1);
  declared.type = type;
  if (code !== undefined) {
    const property = factory.createIntProperty();
    property.name = 'code';
    property.value = code;
    declared.properties.push(property);
  }
  return declared;
}

function operation(...raises: ServiceException[]): ServiceOperation {
  const get = factory.createServiceOperation();
  get.name = 'get';
  for (const declared of raises) get.exceptions.push(declared);
  return get;
}

// The twin of DeclaredErrorsTest in the Java api bundle (#185).
describe('a declared error is recognised the same way on every transport', () => {
  it('is the code its contract gives it', () => {
    const notFound = error('example.PersonNotFound', 404);
    const get = operation(error('example.Conflict', 409), notFound);

    expect(declaredCode(notFound)).toBe(404);
    expect(declaredByCode(get, 404)).toBe(notFound);
    expect(declaredByCode(get, 500)).toBeUndefined();
  });

  it('cannot be recognised in a Diagnostic without a code', () => {
    const nameless = error('example.Mystery');

    expect(declaredCode(nameless)).toBeUndefined();
    expect(declaredByCode(operation(nameless), 0)).toBeUndefined();
  });

  it('takes zero for no code, even where an error declares it', () => {
    expect(declaredByCode(operation(error('example.Zero', 0)), 0)).toBeUndefined();
  });

  it('recognises a thrown error by the name of its class', () => {
    class PersonNotFoundException extends Error {}
    class PersonNotFound extends Error {}
    const notFound = error('example.PersonNotFound', 404);
    const get = operation(notFound);

    expect(declaredRaisedBy(get, new PersonNotFoundException('p-17'))).toBe(notFound);
    expect(declaredRaisedBy(get, new PersonNotFound('p-17'))).toBe(notFound);
    expect(declaredRaisedBy(get, new RangeError('no'))).toBeUndefined();
    expect(raisesDeclared('not an error', notFound)).toBe(false);
  });

  it('recognises a generated class by the symbolic type it carries', () => {
    class Whatever extends Error {
      static readonly type = 'example.PersonNotFound';
    }

    expect(raisesDeclared(new Whatever('p-17'), error('example.PersonNotFound', 404))).toBe(true);
  });
});
