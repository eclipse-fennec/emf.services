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

/**
 * A remote DDSR service, reached the way a local one would be.
 *
 *   pnpm start                                  # with the built-in stub
 *   BROKER_URL=http://localhost:8887/ddsr/rest pnpm start
 *
 * Three modules, none importing another: one owns the broker, one is
 * the REST transport, one wants to charge an account. What connects
 * them is the service registry, and the last one never learns that its
 * Payment service is not in this process.
 */

import { ModuleLoader } from '@eclipse-daanse/tsm';
import { pathToFileURL } from 'node:url';
import { Payment } from './contracts';
import { stub } from './broker-stub-instance';

const entry = (file: string) => pathToFileURL(new URL(file, import.meta.url).pathname).href;
const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));
const say = (message: string) => console.log(`\n=== ${message}`);

async function main(): Promise<void> {
  const loader = new ModuleLoader();

  loader.register([
    { id: 'ddsr', version: '1.0.0', entry: entry('./modules/ddsr.ts'),
      provides: [{ id: 'ddsr.client' }] },
    { id: 'flavor.rest', version: '1.0.0', entry: entry('./modules/flavor-rest.ts'),
      provides: [{ id: 'ddsr.flavor' }] },
    // The consumer does not start without a Payment service, and does
    // not keep running when it goes.
    { id: 'payment.consumer', version: '1.0.0', entry: entry('./modules/payment-consumer.ts'),
      requiresService: [{ id: String(Payment) }] },
  ]);

  say(`starting${stub ? ' with the built-in broker stub' : ' against a real broker'}`);
  await loader.loadAll();

  const registry = loader.getServiceRegistry();
  const active = () => loader.getManifests()
    .map(m => `${m.id}=${loader.isLoaded(m.id) ? 'active' : 'waiting'}`).join('  ');

  say('after startup');
  console.log('modules :', active());
  console.log('mirrored:', registry.countProviders(String(Payment)), 'Payment provider(s)');
  console.log('picked  :', JSON.stringify(
    registry.getServiceReferences(String(Payment))[0]?.properties ?? {}, null, 0));

  if (!stub) return;

  // The provider shuts down. Nobody tells the consumer module: the
  // registry withdraws the service and the loader stops it.
  say('the provider goes away');
  stub.withdraw();
  await wait(50);
  console.log('modules :', active());
  console.log('mirrored:', registry.countProviders(String(Payment)), 'Payment provider(s)');

  // And comes back. The consumer starts again on its own.
  say('the provider comes back');
  stub.restore();
  await wait(50);
  console.log('modules :', active());
  console.log('mirrored:', registry.countProviders(String(Payment)), 'Payment provider(s)');

  say('done');
  process.exit(0);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
