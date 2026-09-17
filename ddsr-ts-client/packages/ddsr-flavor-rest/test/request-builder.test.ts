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
import { DDSRFactory, ParameterBinding } from '@ddsr/model';
import type { Parameter, RestFlavor, RestOperationFlavor, ServiceOperation } from '@ddsr/model';
import { buildRequest } from '../src/request-builder';

const f = DDSRFactory.eINSTANCE;

function operationWith(names: string[]): { operation: ServiceOperation; parameters: Parameter[] } {
  const operation = f.createServiceOperation();
  operation.name = 'get';
  const parameters = names.map((name, index) => {
    const p = f.createParameter();
    p.name = name;
    p.type = 'string';
    p.index = index;
    operation.parameters.push(p);
    return p;
  });
  return { operation, parameters };
}

function flavorFor(operation: ServiceOperation, path: string): { flavor: RestFlavor; opFlavor: RestOperationFlavor } {
  const flavor = f.createRestFlavor();
  flavor.host = 'http://provider.test:9091';
  flavor.basePath = '/payments';
  const opFlavor = f.createRestOperationFlavor();
  opFlavor.name = operation.name;
  opFlavor.method = 'GET';
  opFlavor.path = path;
  opFlavor.operation = operation;
  flavor.operationFlavors.push(opFlavor);
  return { flavor, opFlavor };
}

function bind(opFlavor: RestOperationFlavor, parameter: Parameter,
    where: ParameterBinding, wireName?: string): void {
  const binding = f.createRestParameterBinding();
  binding.parameter = parameter;
  binding.binding = where;
  if (wireName) binding.wireName = wireName;
  opFlavor.parameterBindings.push(binding);
}

// The mirror of RestServiceInvokerPlacementTest on the Java side (#74):
// where an argument travels is the provider's statement, and both SDKs
// have to read that statement the same way.
describe('buildRequest — the flavor decides where an argument travels', () => {
  it('sends each argument to the place its binding names', () => {
    const { operation, parameters } = operationWith(['id', 'currency', 'tenant']);
    const { flavor, opFlavor } = flavorFor(operation, '/{id}');
    bind(opFlavor, parameters[0], ParameterBinding.PATH);
    bind(opFlavor, parameters[1], ParameterBinding.QUERY);
    bind(opFlavor, parameters[2], ParameterBinding.HEADER, 'X-Tenant');

    const request = buildRequest(operation, { id: '42', currency: 'EUR', tenant: 'acme' }, flavor, opFlavor);

    expect(request.url).toBe('http://provider.test:9091/payments/42?currency=EUR');
    expect((request.init.headers as Record<string, string>)['X-Tenant']).toBe('acme');
  });

  it('renames an argument on the wire without touching the contract', () => {
    const { operation, parameters } = operationWith(['accountId']);
    const { flavor, opFlavor } = flavorFor(operation, '/balance');
    bind(opFlavor, parameters[0], ParameterBinding.QUERY, 'account_id');

    const request = buildRequest(operation, { accountId: '7' }, flavor, opFlavor);

    expect(request.url).toBe('http://provider.test:9091/payments/balance?account_id=7');
  });

  it('keeps an undeclared argument in the query string', () => {
    const { operation } = operationWith(['amount']);
    const { flavor, opFlavor } = flavorFor(operation, '/charge');

    const request = buildRequest(operation, { amount: 12.5 }, flavor, opFlavor);

    expect(request.url).toBe('http://provider.test:9091/payments/charge?amount=12.5');
    expect(request.init.body).toBeUndefined();
  });
});
