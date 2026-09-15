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
import type { RestFlavor, RestOperationFlavor, ServiceOperation } from '@ddsr/model';
import { DdsrTransportError } from '@ddsr/client';
import { RestFlavorPlugin } from '../src/rest-flavor-plugin';

function charge(): { operation: ServiceOperation; flavor: RestFlavor; opFlavor: RestOperationFlavor } {
  const f = DDSRFactory.eINSTANCE;
  const operation = f.createServiceOperation();
  operation.name = 'charge';
  const amount = f.createParameter();
  amount.name = 'amount';
  amount.type = 'double';
  operation.parameters.push(amount);
  const flavor = f.createRestFlavor();
  flavor.name = 'rest';
  flavor.host = 'http://provider.test:9091';
  flavor.basePath = '/payments';
  const opFlavor = f.createRestOperationFlavor();
  opFlavor.name = 'charge';
  opFlavor.method = 'POST';
  opFlavor.path = '/charge';
  opFlavor.operation = operation;
  flavor.operationFlavors.push(opFlavor);
  return { operation, flavor, opFlavor };
}

/** #59: a provider that does not answer is a DdsrTransportError, not a hang. */
describe('RestFlavorPlugin timeouts', () => {
  it('aborts a call that never answers after timeoutMillis', async () => {
    const neverAnswers = ((_url: any, init?: any) => new Promise<Response>((_resolve, reject) => {
      init.signal.addEventListener('abort', () => reject(init.signal.reason));
    })) as typeof fetch;
    const plugin = new RestFlavorPlugin({ fetchFn: neverAnswers, timeoutMillis: 30 });
    const { operation, flavor, opFlavor } = charge();

    const started = Date.now();
    await expect(plugin.invoke(operation, { amount: 1 }, flavor, opFlavor))
      .rejects.toSatisfy((error: unknown) => {
        expect(error).toBeInstanceOf(DdsrTransportError);
        expect((error as Error).message).toContain('timeout');
        return true;
      });
    expect(Date.now() - started).toBeLessThan(2000);
  });

  it('an unreachable peer (fetch rejects) is a transport error too', async () => {
    const refused = (async () => { throw new TypeError('fetch failed'); }) as unknown as typeof fetch;
    const plugin = new RestFlavorPlugin({ fetchFn: refused });
    const { operation, flavor, opFlavor } = charge();

    await expect(plugin.invoke(operation, { amount: 1 }, flavor, opFlavor))
      .rejects.toBeInstanceOf(DdsrTransportError);
  });

  it('an HTTP error answered by the provider is NOT a transport error (no rebind, no retry)', async () => {
    const serverError = (async () => new Response('boom', { status: 500 })) as unknown as typeof fetch;
    const plugin = new RestFlavorPlugin(serverError);
    const { operation, flavor, opFlavor } = charge();

    await expect(plugin.invoke(operation, { amount: 1 }, flavor, opFlavor))
      .rejects.toSatisfy((error: unknown) => {
        expect(error).not.toBeInstanceOf(DdsrTransportError);
        expect((error as Error).message).toContain('500');
        return true;
      });
  });

  it('timeoutMillis 0 disables the signal', async () => {
    let seenSignal: unknown = 'unset';
    const answers = (async (_url: any, init?: any) => {
      seenSignal = init?.signal;
      return new Response('<?xml version="1.0"?><x/>', { status: 200, headers: { 'Content-Type': 'application/xml' } });
    }) as typeof fetch;
    const plugin = new RestFlavorPlugin({ fetchFn: answers, timeoutMillis: 0 });
    const { operation, flavor, opFlavor } = charge();

    await plugin.invoke(operation, { amount: 1 }, flavor, opFlavor).catch(() => undefined);
    expect(seenSignal).toBeUndefined();
  });
});
