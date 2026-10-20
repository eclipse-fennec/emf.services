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
import { BasicEClass, URI } from '@emfts/core';
import { DDSRFactory } from '@ddsr/model';
import { emitConsumer } from '../src/emit';
import { mapEType } from '../src/type-mapping';

const ECORE = 'http://www.eclipse.org/emf/2002/Ecore';
const SERVICES = 'http://eclipse.org/fennec/services/1.0';

describe('an eType maps the way java-types.mtl maps it', () => {
  it('maps an Ecore data type along its instance class', () => {
    expect(mapEType(`${ECORE}#//EString`).text).toBe('string');
    expect(mapEType(`${ECORE}#//EIntegerObject`).text).toBe('number');
    expect(mapEType(`${ECORE}#//EBoolean`).text).toBe('boolean');
  });

  it('keeps a data type without a faithful counterpart unknown', () => {
    expect(mapEType(`${ECORE}#//EBigDecimal`).text).toBe('unknown');
    expect(mapEType(`${ECORE}#//EDate`).text).toBe('unknown');
  });

  it('imports a class of the services model', () => {
    expect(mapEType(`${SERVICES}#//Diagnostic`)).toEqual({ text: 'Diagnostic', modelImport: 'Diagnostic' });
  });

  it('does not take a foreign class for a services type of the same name', () => {
    expect(mapEType('http://example.org/other#//Diagnostic').text).toBe('unknown');
  });
});

describe('the eType wins over the type string', () => {
  it('renders the eType where both are given', () => {
    const factory = DDSRFactory.eINSTANCE;
    const serviceInterface = factory.createServiceInterface();
    serviceInterface.name = 'Probe';
    const operation = factory.createServiceOperation();
    operation.name = 'count';
    const result = factory.createParameter();
    result.name = 'result';
    result.type = 'string';
    const eType = new BasicEClass();
    eType.eSetProxyURI(URI.createURI(`${ECORE}#//EInt`));
    result.eType = eType;
    operation.returnValue = result;
    serviceInterface.operations.push(operation);

    expect(emitConsumer(serviceInterface).content).toContain('count(): Promise<number>;');
  });
});
