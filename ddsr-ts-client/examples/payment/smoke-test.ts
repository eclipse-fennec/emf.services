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
 * DDSR Smoke Test — catalog roundtrip through the SDK against a RUNNING
 * broker (BROKER_URL, default http://localhost:8887/ddsr/rest):
 *
 * 1. ensure a "SmokeTestService" catalog entry
 * 2. read it back (typed, via GET /catalog/{name})
 * 3. verify its sd1 fingerprint matches the local description
 * 4. remove it, verify it is gone
 *
 * Usage: pnpm --filter @ddsr/example-payment smoke
 */

import { DdsrClientImpl, fingerprint } from '@ddsr/client';
import { DDSRFactory } from '@ddsr/model';
import { BROKER_URL } from './payment-api';

const factory = DDSRFactory.eINSTANCE;
let passed = 0;
let failed = 0;

function check(step: string, condition: boolean, message: string): void {
  if (condition) {
    passed++;
    console.log(`  ✓ ${step}: ${message}`);
  } else {
    failed++;
    console.error(`  ✗ ${step}: ${message}`);
  }
}

function smokeInterface() {
  const si = factory.createServiceInterface();
  si.name = 'SmokeTestService';
  si.version = '0.0.1';
  const ping = factory.createServiceOperation();
  ping.name = 'ping';
  ping.returnType = 'string';
  si.operations.push(ping);
  return si;
}

async function main(): Promise<void> {
  console.log(`DDSR smoke test against ${BROKER_URL}`);
  const client = DdsrClientImpl.create({ brokerUrl: BROKER_URL, requestor: 'ts-smoke-test' });

  const local = smokeInterface();
  const added = await client.catalog.ensureEntry(local);
  check('add', added.severity !== 'ERROR' || added.code === 202, `diagnostic ${added.severity}/${added.code}`);

  const remote = await client.catalog.getEntry('SmokeTestService');
  check('read', remote?.name === 'SmokeTestService', `entry present: ${remote?.name}`);
  check(
    'fingerprint',
    fingerprint(remote) === fingerprint(local),
    `broker copy hashes to ${fingerprint(remote)}`
  );

  const removed = await client.catalog.removeEntry('SmokeTestService');
  check('remove', removed.severity !== 'ERROR', `diagnostic ${removed.severity}/${removed.code}`);
  check('gone', (await client.catalog.getEntry('SmokeTestService')) === undefined, 'entry no longer served');

  await client.close();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed === 0 ? 0 : 1);
}

main().catch(error => {
  console.error(`smoke test failed: ${String(error)}`);
  process.exit(1);
});
