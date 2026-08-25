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
 * TS Payment consumer: subscribes to lifecycle events (SSE, with
 * snapshot-on-reconnect), finds the Payment service, verifies the
 * broker's sd1 fingerprint against the received description, dumps the
 * typed service properties, and invokes charge/getBalance.
 *
 *   BROKER_URL=http://localhost:8887/ddsr/rest pnpm exec tsx consumer.ts [--watch]
 */

import { RestFlavorPlugin } from '@ddsr/flavor-rest';
import {
  DdsrClientImpl,
  FINGERPRINT_PROPERTY,
  fingerprint,
  propertiesOf,
  propertyOf,
} from '@ddsr/client';
import { BROKER_URL } from './payment-api';

const log = (m: string) => console.log(`[ts-consumer] ${m}`);
const watch = process.argv.includes('--watch');

async function main(): Promise<void> {
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    flavorPlugins: [new RestFlavorPlugin()],
    consumerId: 'ts-consumer-demo',
  });

  client.consumer.addServiceListener('Payment', undefined, event => {
    log(`event: ${event.type} for reference ${event.reference?.id}`);
  });

  const locator = await client.consumer.findOne('Payment');
  if (!locator) {
    log('no Payment provider available');
    if (!watch) {
      await client.close();
      process.exit(1);
    }
    return;
  }

  // FR-P6: does everyone see the same contract?
  const brokerFingerprint = propertyOf(locator.reference, FINGERPRINT_PROPERTY);
  const localFingerprint = fingerprint(locator.serviceInterface);
  log(`fingerprint broker=${brokerFingerprint}`);
  log(`fingerprint local =${localFingerprint}`);
  log(brokerFingerprint === localFingerprint
    ? 'fingerprints MATCH — same contract on both ends'
    : 'fingerprints DIFFER — the description diverged in transit!');

  log('reference properties:');
  for (const [name, value] of propertiesOf(locator.reference)) {
    log(`  ${name} = ${JSON.stringify(value)} (${typeof value})`);
  }

  const before = await locator.invoke('getBalance', { accountId: 'default' });
  log(`getBalance -> ${before}`);
  const after = await locator.invoke('charge', { amount: 12.5, currency: 'EUR' });
  log(`charge(12.5 EUR) -> ${after}`);

  if (!watch) {
    await client.close();
    log('done');
  } else {
    log('watching for lifecycle events — Ctrl-C to exit');
  }
}

main().catch(error => {
  console.error(`[ts-consumer] failed: ${String(error)}`);
  process.exit(1);
});
