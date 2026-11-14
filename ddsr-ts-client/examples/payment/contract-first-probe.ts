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
 * Harness scenario V (#271): a contract stands on its own.
 *
 * The probe puts a contract into the catalog with nothing behind it,
 * looks it up as a consumer (none, not an error), then serves it itself
 * and checks that the waiting listener hears the arrival and a call
 * succeeds. It checks that the catalog refuses to remove a contract
 * something still implements, withdraws the implementation, checks that
 * the contract stays and that a call now fails as "unavailable", and
 * finally removes the contract.
 *
 * The contract is the Payment document under a name of its own, so the
 * Payment entry the other scenarios use is never touched. Needs the
 * three broker roles: service-provider to add and serve, service-consumer
 * to look up, service-admin to remove.
 *
 *   BROKER_URL=http://localhost:8887/ddsr/rest SERVICE_URL=http://localhost:9093/contract-first \
 *     pnpm exec tsx contract-first-probe.ts
 */

import { createServer } from 'node:http';
import type { RestFlavor, ServiceEvent } from '@ddsr/model';
import { RestFlavorPlugin, restDispatcher } from '@ddsr/flavor-rest';
import { DdsrClientImpl, DdsrServiceUnavailableError, toArray } from '@ddsr/client';
import { loadPaymentProvider } from './payment-api';
import { brokerTokens, consumerId, nodeFetch } from './broker-auth';

const BROKER_URL = process.env.BROKER_URL ?? 'http://localhost:8887/ddsr/rest';
const SERVICE_URL = process.env.SERVICE_URL ?? 'http://localhost:9093/contract-first';
const CONTRACT = 'PaymentContractFirst';
const CATALOG_HAS_LIVE_IMPLS = 200;

const failures: string[] = [];
function check(name: string, condition: boolean, detail: string): void {
  console.log(`  ${condition ? '✓' : '✗'} ${name}: ${detail}`);
  if (!condition) failures.push(`${name}: ${detail}`);
}

function until(heard: ServiceEvent[], type: string, seconds: number): Promise<ServiceEvent | undefined> {
  return new Promise(resolve => {
    const started = Date.now();
    const look = () => {
      const event = heard.find(e => e.type === type);
      if (event || Date.now() - started > seconds * 1000) resolve(event);
      else setTimeout(look, 100);
    };
    look();
  });
}

async function main(): Promise<void> {
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    flavorPlugins: [new RestFlavorPlugin()],
    requestor: 'contract-first-probe',
    consumerId: consumerId('contract-first-ts'),
    tokenSupplier: brokerTokens(),
    fetchFn: nodeFetch(),
    reconnectSeconds: 1,
  });

  const { provider, implementation, serviceInterface } =
    loadPaymentProvider('contract-first-ts', SERVICE_URL);
  serviceInterface.name = CONTRACT;

  // A run that died half-way leaves its contract behind; start clean.
  await client.catalog.removeEntry(CONTRACT);

  // 1. the contract alone
  await client.catalog.ensureEntry(serviceInterface);
  check('contract-registered-alone', (await client.catalog.getEntry(CONTRACT)) !== undefined,
    `${CONTRACT} is in the catalog with no implementation`);
  const before = await client.consumer.find(CONTRACT);
  check('lookup-answers-none', before.length === 0, `find returned ${before.length} locator(s), no error`);

  const heard: ServiceEvent[] = [];
  client.consumer.addServiceListener(CONTRACT, undefined, event => heard.push(event));

  // 2. an implementation arrives later
  const balances = new Map<string, number>();
  const dispatch = restDispatcher(
    toArray<RestFlavor>(implementation.flavors).find(f => (f as { eClass?: () => { name?: string } }).eClass?.()?.name === 'RestFlavor')!,
    {
      charge(amount: number): number {
        const balance = (balances.get('default') ?? 100) - amount;
        balances.set('default', balance);
        return balance;
      },
      getBalance(): number {
        return balances.get('default') ?? 100;
      },
    },
    { basePath: new URL(SERVICE_URL).pathname });
  const server = createServer((request, response) => {
    const chunks: Buffer[] = [];
    request.on('data', chunk => chunks.push(chunk as Buffer));
    request.on('end', () => {
      void dispatch({
        method: request.method ?? 'GET',
        url: request.url ?? '/',
        headers: request.headers,
        body: chunks.length > 0 ? Buffer.concat(chunks).toString('utf8') : undefined,
      }).then(answer => {
        response.writeHead(answer?.status ?? 404, { 'Content-Type': answer?.contentType ?? 'text/plain' });
        response.end(answer?.body ?? 'not found');
      });
    });
  });
  await new Promise<void>(resolve => server.listen(Number(new URL(SERVICE_URL).port), resolve));
  const registration = await client.provider.publish(provider, implementation);

  const registered = await until(heard, 'REGISTERED', 30);
  check('listener-hears-the-arrival', registered !== undefined,
    registered ? `REGISTERED ${registered.reference?.id}` : 'no REGISTERED within 30 s');
  const locator = await client.consumer.findOne(CONTRACT);
  const balance = locator ? Number(await locator.invoke('getBalance', { accountId: 'a' })) : NaN;
  check('call-succeeds-once-implemented', balance === 100, `getBalance = ${balance}`);

  // 3. the catalog refuses to drop what is implemented
  const refused = await client.catalog.removeEntry(CONTRACT);
  check('removal-refused-while-implemented', refused.code === CATALOG_HAS_LIVE_IMPLS,
    `code ${refused.code}: ${refused.message ?? ''}`);

  // 4. the implementation goes, the contract stays
  await registration.withdraw();
  const unregistering = await until(heard, 'UNREGISTERING', 30);
  check('listener-hears-the-departure', unregistering !== undefined,
    unregistering ? `UNREGISTERING ${unregistering.reasonCode ?? '-'}` : 'no UNREGISTERING within 30 s');
  check('contract-outlives-its-implementation', (await client.catalog.getEntry(CONTRACT)) !== undefined,
    `${CONTRACT} still in the catalog`);
  const failure = locator ? await locator.invoke('getBalance', { accountId: 'a' }).catch((e: unknown) => e) : undefined;
  check('call-fails-as-unavailable', failure instanceof DdsrServiceUnavailableError,
    String((failure as Error | undefined)?.message ?? failure));
  await new Promise<void>(resolve => server.close(() => resolve()));

  // 5. the contract is removed
  const removed = await client.catalog.removeEntry(CONTRACT);
  check('contract-removed', removed.severity !== 'ERROR' && (await client.catalog.getEntry(CONTRACT)) === undefined,
    `code ${removed.code ?? 0}, entry gone`);

  await client.close();
  if (failures.length > 0) {
    console.log(`PROBE_FAIL ${failures.join('; ')}`);
    process.exit(1);
  }
  console.log('PROBE_OK');
}

main().catch(error => {
  console.log(`PROBE_FAIL ${String(error)}`);
  process.exit(1);
});
