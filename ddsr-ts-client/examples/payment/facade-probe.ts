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
 * Harness scenario W (#279): a TypeScript consumer whose broker URL is the
 * tag facade, <broker>/scoped/<tag>. It needs no code of its own — the
 * facade answers the broker's consumer API — and sees only what the tag
 * covers.
 *
 *   BROKER_URL=http://localhost:8897/ddsr/rest/scoped/payment EXPECT_PAYMENT=1 pnpm exec tsx facade-probe.ts
 */

import { RestFlavorPlugin } from '@ddsr/flavor-rest';
import { DdsrClientImpl, toArray } from '@ddsr/client';
import type { RestFlavor } from '@ddsr/model';
import { brokerTokens, consumerId, nodeFetch } from './broker-auth';

const BROKER_URL = process.env.BROKER_URL ?? 'http://localhost:8887/ddsr/rest/scoped/payment';
const EXPECT_PAYMENT = Number(process.env.EXPECT_PAYMENT ?? '1');

async function main(): Promise<void> {
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    flavorPlugins: [new RestFlavorPlugin()],
    consumerId: consumerId('facade-probe'),
    tokenSupplier: brokerTokens(),
    fetchFn: nodeFetch(),
  });
  const failures: string[] = [];
  const check = (name: string, ok: boolean, detail: string) => {
    console.log(`  ${ok ? '✓' : '✗'} ${name}: ${detail}`);
    if (!ok) failures.push(name);
  };

  const payments = await client.consumer.find('Payment');
  check('facade-lookup', payments.length === EXPECT_PAYMENT,
    `${payments.length} Payment implementation(s) through ${new URL(BROKER_URL).pathname}, expected ${EXPECT_PAYMENT}`);
  if (EXPECT_PAYMENT > 0 && payments[0]) {
    const balance = Number(await payments[0].invoke('getBalance', { accountId: 'facade' }));
    check('facade-call', Number.isFinite(balance), `getBalance = ${balance}`);
  }
  const lookups = await client.consumer.find('BrokerLookup');
  const base = lookups[0] ? toArray<RestFlavor>(lookups[0].implementation.flavors)[0]?.basePath : undefined;
  check('facade-self', !!base && base.includes('/scoped/'), `BrokerLookup announces ${base}`);

  await client.close();
  console.log(failures.length === 0 ? 'PROBE_OK' : `PROBE_FAIL ${failures.join(', ')}`);
  process.exit(failures.length === 0 ? 0 : 1);
}

main().catch(error => {
  console.log(`PROBE_FAIL ${String(error)}`);
  process.exit(1);
});
