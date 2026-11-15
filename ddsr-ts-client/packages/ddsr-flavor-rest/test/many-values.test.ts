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
import { DDSRFactory, DDSRPackage } from '@ddsr/model';
import type { RestFlavor, RestOperationFlavor, ServiceOperation, ServiceProvider } from '@ddsr/model';
import { deserializeFromXmi } from '@ddsr/client';
import { restDispatcher } from '../src/operation-dispatch';
import { RestFlavorPlugin } from '../src/rest-flavor-plugin';

/**
 * A many-valued model answer over REST (#302): one document whose roots
 * are the values in order, followed by their context; the reader takes
 * every root of the return type.
 */
function providers(upperBound: number): { operation: ServiceOperation; flavor: RestFlavor; opFlavor: RestOperationFlavor } {
  const f = DDSRFactory.eINSTANCE;
  const operation = f.createServiceOperation();
  operation.name = 'providers';
  const result = f.createParameter();
  result.name = 'result';
  result.upperBound = upperBound;
  result.eType = DDSRPackage.eINSTANCE.getEClassifier('ServiceProvider') as never;
  operation.returnValue = result;
  const flavor = f.createRestFlavor();
  flavor.name = 'rest';
  flavor.host = 'http://provider.test:9091';
  flavor.basePath = '/registry';
  const opFlavor = f.createRestOperationFlavor();
  opFlavor.name = 'providers';
  opFlavor.method = 'GET';
  opFlavor.path = '/providers';
  opFlavor.produces.push('application/xml');
  opFlavor.operation = operation;
  flavor.operationFlavors.push(opFlavor);
  return { operation, flavor, opFlavor };
}

function provider(name: string): ServiceProvider {
  const provider = DDSRFactory.eINSTANCE.createServiceProvider();
  provider.name = name;
  return provider;
}

describe('a list of model objects over REST (#302)', () => {
  it('is served as one document, the values in order', async () => {
    const { flavor } = providers(-1);
    const dispatch = restDispatcher(flavor, { providers: () => [provider('first'), provider('second')] });

    const answer = await dispatch({ method: 'GET', url: '/registry/providers', headers: {} });

    expect(answer.status).toBe(200);
    expect(answer.contentType).toBe('application/xml');
    const roots = deserializeFromXmi(answer.body as string) as ServiceProvider[];
    expect(roots.map(root => root.name)).toEqual(['first', 'second']);
  });

  it('an empty list is no content', async () => {
    const { flavor } = providers(-1);
    const dispatch = restDispatcher(flavor, { providers: () => [] });

    expect((await dispatch({ method: 'GET', url: '/registry/providers', headers: {} })).status).toBe(204);
  });

  it('is read back as every root of the return type, from the dispatcher it came from', async () => {
    const { operation, flavor, opFlavor } = providers(-1);
    const dispatch = restDispatcher(flavor, { providers: () => [provider('first'), provider('second')] });
    const served = await dispatch({ method: 'GET', url: '/registry/providers', headers: {} });
    const fetchFn = (async () => new Response(served.body as string,
      { status: 200, headers: { 'Content-Type': served.contentType } })) as typeof fetch;

    const values = await new RestFlavorPlugin({ fetchFn }).invoke(operation, {}, flavor, opFlavor);

    expect((values as ServiceProvider[]).map(value => value.name)).toEqual(['first', 'second']);
  });

  it('one value of a many-valued answer is a list of one', async () => {
    const { operation, flavor, opFlavor } = providers(-1);
    const dispatch = restDispatcher(flavor, { providers: () => [provider('only')] });
    const served = await dispatch({ method: 'GET', url: '/registry/providers', headers: {} });
    const fetchFn = (async () => new Response(served.body as string,
      { status: 200, headers: { 'Content-Type': served.contentType } })) as typeof fetch;

    const values = await new RestFlavorPlugin({ fetchFn }).invoke(operation, {}, flavor, opFlavor);

    expect((values as ServiceProvider[]).map(value => value.name)).toEqual(['only']);
  });
});
