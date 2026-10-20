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
import type { RestFlavor } from '@ddsr/model';
import { restDispatcher } from '../src/operation-dispatch';
import { RestFlavorPlugin } from '../src/rest-flavor-plugin';
import { DdsrServiceError } from '@ddsr/client';

/**
 * The provider side of a flavor, read back from where the consumer
 * wrote it (#154).
 *
 * The contract here is the harness's BindingProbe, and for its reason:
 * its three arguments travel in three different places, so a dispatcher
 * that reads any of them from the wrong one is caught by the answer.
 */
function bindingProbe(): RestFlavor {
  const f = DDSRFactory.eINSTANCE;
  const echo = f.createServiceOperation();
  echo.name = 'echo';
  for (const [index, name] of ['id', 'currency', 'tenant'].entries()) {
    const parameter = f.createParameter();
    parameter.name = name;
    parameter.type = 'string';
    parameter.index = index;
    echo.parameters.push(parameter);
  }
  const contract = f.createServiceInterface();
  contract.name = 'BindingProbe';
  contract.operations.push(echo);

  const flavor = f.createRestFlavor();
  flavor.name = 'binding-probe-rest';
  flavor.basePath = '/probe';
  const opFlavor = f.createRestOperationFlavor();
  opFlavor.name = 'echo';
  opFlavor.method = 'GET';
  opFlavor.path = '/echo/{id}';
  opFlavor.returnCodes.push('200');
  opFlavor.produces.push('text/plain');
  opFlavor.operation = echo;
  const path = f.createRestParameterBinding();
  path.parameter = echo.parameters.get(0);
  path.binding = 'PATH';
  const query = f.createRestParameterBinding();
  query.parameter = echo.parameters.get(1);
  query.binding = 'QUERY';
  const header = f.createRestParameterBinding();
  header.parameter = echo.parameters.get(2);
  header.binding = 'HEADER';
  header.wireName = 'X-Tenant';
  opFlavor.parameterBindings.push(path, query, header);
  flavor.operationFlavors.push(opFlavor);
  return flavor;
}

const probe = {
  echo: (id: string, currency: string, tenant: string) => `${id}|${currency}|${tenant}`,
};

describe('serving a contract from its flavor', () => {
  it('reads each argument from where the flavor says it travels', async () => {
    const dispatch = restDispatcher(bindingProbe(), probe);

    const answer = await dispatch({
      method: 'GET',
      url: '/probe/echo/acct-42?currency=EUR',
      headers: { 'x-tenant': 'acme' },
    });

    expect(answer).toEqual({ status: 200, body: 'acct-42|EUR|acme', contentType: 'text/plain' });
  });

  it('is not the router for anything else', async () => {
    const dispatch = restDispatcher(bindingProbe(), probe);

    expect(await dispatch({ method: 'GET', url: '/elsewhere' })).toBeUndefined();
    expect(await dispatch({ method: 'GET', url: '/probe/unknown/1' })).toBeUndefined();
    expect(await dispatch({ method: 'POST', url: '/probe/echo/acct-42' }))
      .toBeUndefined();
  });

  it('says what is missing rather than calling with a hole in the arguments', async () => {
    const dispatch = restDispatcher(bindingProbe(), probe);

    const answer = await dispatch({ method: 'GET', url: '/probe/echo/acct-42', headers: {} });

    expect(answer?.status).toBe(400);
    expect(answer?.body).toContain('currency');
  });

  it('answers a failing implementation rather than leaving the caller waiting', async () => {
    const dispatch = restDispatcher(bindingProbe(), {
      echo: () => {
        throw new Error('no such tenant');
      },
    });

    const answer = await dispatch({
      method: 'GET',
      url: '/probe/echo/acct-42?currency=EUR',
      headers: { 'X-Tenant': 'acme' },
    });

    expect(answer?.status).toBe(500);
    expect(answer?.body).toBe('no such tenant');
  });

  describe('a declared error', () => {
    function withDeclaredError(): RestFlavor {
      const f = DDSRFactory.eINSTANCE;
      const flavor = bindingProbe();
      const unknownTenant = f.createServiceException();
      unknownTenant.name = 'UnknownTenant';
      unknownTenant.type = 'org.example.UnknownTenant';
      const binding = f.createRestExceptionBinding();
      binding.status = 404;
      binding.exception = unknownTenant;
      flavor.operationFlavors.get(0).exceptionBindings.push(binding);
      return flavor;
    }

    const request = { method: 'GET', url: '/probe/echo/acct-42?currency=EUR', headers: { 'X-Tenant': 'acme' } };

    it('answers with the status the flavor binds, matched by class name', async () => {
      class UnknownTenantException extends Error {}
      const dispatch = restDispatcher(withDeclaredError(), {
        echo: () => { throw new UnknownTenantException('no tenant acme'); },
      });

      expect(await dispatch(request)).toEqual({ status: 404, body: 'no tenant acme', contentType: 'text/plain' });
    });

    it('matches a generated class by the symbolic type it carries', async () => {
      class Whatever extends Error {
        static readonly type = 'org.example.UnknownTenant';
      }
      const dispatch = restDispatcher(withDeclaredError(), { echo: () => { throw new Whatever('x'); } });

      expect((await dispatch(request))?.status).toBe(404);
    });

    it('arrives at the caller as that error, end to end', async () => {
      // The provider throws, the dispatcher answers with the bound status,
      // the consumer's plugin recognises it - both halves in one process.
      class UnknownTenantException extends Error {}
      const flavor = withDeclaredError();
      flavor.host = 'http://provider.test';
      const dispatch = restDispatcher(flavor, {
        echo: () => { throw new UnknownTenantException('no tenant acme'); },
      });
      const fetchFn = (async (url: string, init?: RequestInit) => {
        const target = new URL(url);
        const answer = await dispatch({
          method: init?.method ?? 'GET',
          url: target.pathname + target.search,
          headers: init?.headers as Record<string, string>,
        });
        return new Response(answer?.body ?? '', { status: answer?.status ?? 404 });
      }) as unknown as typeof fetch;
      const opFlavor = flavor.operationFlavors.get(0);

      const error = await new RestFlavorPlugin(fetchFn)
        .invoke(opFlavor.operation!, { id: 'acct-42', currency: 'EUR', tenant: 'acme' }, flavor, opFlavor)
        .catch((e: unknown) => e);

      expect(error).toBeInstanceOf(DdsrServiceError);
      expect((error as DdsrServiceError).exception.type).toBe('org.example.UnknownTenant');
      expect((error as Error).message).toBe('no tenant acme');
    });
  });

  it('coerces to the types the contract declares, and answers 204 for nothing', async () => {
    const f = DDSRFactory.eINSTANCE;
    const charge = f.createServiceOperation();
    charge.name = 'charge';
    const amount = f.createParameter();
    amount.name = 'amount';
    amount.type = 'double';
    charge.parameters.push(amount);
    const contract = f.createServiceInterface();
    contract.name = 'Payment';
    contract.operations.push(charge);
    const flavor = f.createRestFlavor();
    flavor.basePath = '/payments';
    const opFlavor = f.createRestOperationFlavor();
    opFlavor.name = 'charge';
    opFlavor.method = 'POST';
    opFlavor.path = '/charge/{amount}';
    opFlavor.operation = charge;
    const binding = f.createRestParameterBinding();
    binding.parameter = amount;
    binding.binding = 'PATH';
    opFlavor.parameterBindings.push(binding);
    flavor.operationFlavors.push(opFlavor);

    let seen: unknown;
    const dispatch = restDispatcher(flavor, {
      charge: (value: number) => {
        seen = value;
        return undefined;
      },
    });

    const answer = await dispatch({ method: 'POST', url: '/payments/charge/12.5' });

    expect(seen).toBe(12.5);
    expect(typeof seen).toBe('number');
    expect(answer?.status).toBe(204);
    expect(answer?.body).toBeUndefined();
  });
});
