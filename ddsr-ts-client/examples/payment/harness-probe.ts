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
 * Scripted TS consumer for the cross-language harness (FR-P4).
 * Asserts against a running broker + Payment provider:
 *
 *  1. discovery: find('Payment') yields a locator (with retry)
 *  2. FR-P6: the broker's ddsr.fingerprint equals the locally computed
 *     sd1 hash of the received description (and, since the description
 *     is the shared golden Payment contract, the golden hash itself)
 *  3. FR-P5: all eight property types arrive TYPED with the agreed values
 *  4. invocation: getBalance/charge return numbers over the wire
 *  5. FR-P3: after PROBE_READY the harness stops the provider; the
 *     UNREGISTERING event must arrive, and its receive time is printed
 *     so the harness can order it against the provider's exit.
 *
 * Output contract (parsed by the harness): lines PROBE_READY,
 * UNREGISTERING_AT <epoch-ms>, PROBE_OK / PROBE_FAIL <reason>.
 */

import { RestFlavorPlugin } from '@ddsr/flavor-rest';
import {
  BrokerHttp,
  DdsrClientImpl,
  FINGERPRINT_PROPERTY,
  fingerprint,
  propertiesOf,
  propertyOf,
} from '@ddsr/client';

const BROKER_URL = process.env.BROKER_URL ?? 'http://localhost:8887/ddsr/rest';
const EXPECTED_LANG = process.env.EXPECT_LANG ?? 'java';
const GOLDEN = 'sd1:baafb26e152b76e0e6e713bb86a5e418c2d014d855b24df4d938a517c59807c0';

const failures: string[] = [];
function check(name: string, condition: boolean, detail: string): void {
  console.log(`  ${condition ? '✓' : '✗'} ${name}: ${detail}`);
  if (!condition) failures.push(`${name}: ${detail}`);
}

function fail(reason: string): never {
  console.log(`PROBE_FAIL ${reason}`);
  process.exit(1);
}

async function main(): Promise<void> {
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    flavorPlugins: [new RestFlavorPlugin()],
    consumerId: 'harness-probe-ts',
    reconnectSeconds: 1,
    sessionIntervalSeconds: 0, // renewed explicitly below, deterministic
  });
  const brokerHttp = new BrokerHttp({ brokerUrl: BROKER_URL });

  let unregistering: number | undefined;
  client.consumer.addServiceListener('Payment', undefined, event => {
    if (event.type === 'UNREGISTERING') {
      unregistering = Date.now();
      console.log(`UNREGISTERING_AT ${unregistering}`);
    }
  });

  // 1. discovery (retry: provider may still be coming up)
  let locator;
  for (let attempt = 0; attempt < 60 && !locator; attempt++) {
    locator = await client.consumer.findOne('Payment').catch(() => undefined);
    if (!locator) await new Promise(r => setTimeout(r, 500));
  }
  if (!locator) fail('no Payment locator within 30s');
  check('discovery', true, `reference ${locator.reference.id}`);

  // 2. fingerprints
  const brokerFp = propertyOf(locator.reference, FINGERPRINT_PROPERTY);
  const localFp = fingerprint(locator.serviceInterface);
  check('fingerprint-match', brokerFp === localFp, `broker=${brokerFp} local=${localFp}`);
  check('fingerprint-golden', localFp === GOLDEN, `${localFp}`);
  // im1 rides beside the sd1 (ACQUISITION §11.1) — the probe holds no
  // impl model to recompute it, but its presence proves the broker
  // decorates the reconnect anchor across the wire.
  const implFp = propertyOf(locator.reference, 'ddsr.impl.fingerprint');
  check('impl-fingerprint-present', typeof implFp === 'string' && implFp.startsWith('im1:'), `${implFp}`);

  // 3. typed properties (the agreed cross-language property set, D8)
  const p = propertiesOf(locator.reference);
  check('prop-lang', p.get('ddsr.provider.lang') === EXPECTED_LANG, `${p.get('ddsr.provider.lang')}`);
  check('prop-int', p.get('service.ranking') === 10, `${JSON.stringify(p.get('service.ranking'))}`);
  check('prop-long', p.get('payments.maxAmountCents') === 5000000000, `${JSON.stringify(p.get('payments.maxAmountCents'))}`);
  check('prop-double', p.get('payments.feeRate') === 0.025, `${JSON.stringify(p.get('payments.feeRate'))}`);
  check('prop-float', p.get('payments.timeoutSeconds') === 1.5, `${JSON.stringify(p.get('payments.timeoutSeconds'))}`);
  check('prop-short', p.get('payments.maxRetries') === 3, `${JSON.stringify(p.get('payments.maxRetries'))}`);
  check('prop-bool', p.get('payments.sandbox') === true, `${JSON.stringify(p.get('payments.sandbox'))}`);
  check('prop-stringlist', JSON.stringify(p.get('payments.tags')) === JSON.stringify(['demo', 'payments']),
    `${JSON.stringify(p.get('payments.tags'))}`);

  // 4. invocation
  const balance = Number(await locator.invoke('getBalance', { accountId: 'harness-account' }));
  check('invoke-getBalance', Number.isFinite(balance), `${balance}`);
  const remaining = Number(await locator.invoke('charge', { amount: 12.5, currency: 'EUR' }));
  check('invoke-charge', Number.isFinite(remaining), `${remaining}`);

  // 4b. acquisition stage (ACQUISITION §3/§4): after the session PUT the
  // broker must list our lease on the Payment reference; contract
  // addressing must find the impl by its sd1 and reject a foreign hash.
  await client.renewSession();
  const brokerSession = await brokerHttp.getConsumerSession('harness-probe-ts');
  check('session-visible',
    brokerSession !== undefined
      && brokerSession.acquiredReferenceIds.includes(locator.reference.id ?? ''),
    `broker lists ${JSON.stringify(brokerSession?.acquiredReferenceIds)}`);
  const byFingerprint = await client.consumer.find('Payment', undefined, localFp);
  check('lookup-by-fingerprint', byFingerprint.length >= 1, `${byFingerprint.length} hit(s)`);
  const byWrongFingerprint = await client.consumer.find('Payment', undefined,
    'sd1:0000000000000000000000000000000000000000000000000000000000000000');
  check('lookup-wrong-fingerprint-empty', byWrongFingerprint.length === 0,
    `${byWrongFingerprint.length} hit(s)`);

  if (failures.length > 0) fail(failures.join(' | '));

  // 5. lifecycle: harness stops the provider after this marker
  console.log('PROBE_READY');
  for (let waited = 0; waited < 60000 && unregistering === undefined; waited += 250) {
    await new Promise(r => setTimeout(r, 250));
  }
  if (unregistering === undefined) fail('no UNREGISTERING within 60s of PROBE_READY');

  await client.close();
  // 5b. FR-P3 shutdown-notify: close() released the session at the broker.
  const gone = await brokerHttp.getConsumerSession('harness-probe-ts');
  if (gone !== undefined) fail('session still present at broker after close()');
  console.log('PROBE_OK');
  process.exit(0);
}

main().catch(error => fail(String(error)));
