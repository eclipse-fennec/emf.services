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
 * Harness scenarios F and G (#58): what a TS consumer with a tracked
 * locator experiences while the Java provider behind it changes.
 *
 *   SCENARIO=F — same identity restarts on a new port. The harness starts
 *     a second instance of the SAME (name, version) on NEW_PORT after
 *     PROBE_READY. Since #55 that is a MODIFY in place: expected is a
 *     MODIFIED for the bound reference (same id), the locator refreshes
 *     its endpoint from the event, the next call reaches the new port and
 *     the lease is unchanged. No UNREGISTERING, no rebind.
 *
 *   SCENARIO=G — update policy DEPRECATE_AND_DRAIN. The harness starts
 *     version 2.0.0 with replaces=1.0.0 after PROBE_READY. Expected:
 *     UPGRADE_AVAILABLE for the held reference, lookups return only the
 *     successor, the predecessor stays invokable while the lease is held,
 *     releasing the session retires it (UNREGISTERING/REPLACED + RETIRED)
 *     and the locator rebinds to the successor.
 *
 * Output: `  ✓/✗ name: detail` lines, `EVENT <type>[/<reason>] <refId>`
 * per event, PROBE_READY, PROBE_OK / PROBE_FAIL <reason>.
 */
import { RestFlavorPlugin } from '@ddsr/flavor-rest';
import { BrokerHttp, DdsrClientImpl } from '@ddsr/client';
import type { TrackedServiceLocator } from '@ddsr/client';

const BROKER_URL = process.env.BROKER_URL ?? 'http://localhost:8887/ddsr/rest';
const SCENARIO = process.env.SCENARIO ?? 'F';
const NEW_PORT = process.env.NEW_PORT ?? '9092';
const CONSUMER_ID = `harness-lifecycle-${SCENARIO.toLowerCase()}`;

interface Seen { type: string; reason: string | undefined; id: string | undefined; }
const events: Seen[] = [];
const failures: string[] = [];

function check(name: string, condition: boolean, detail: string): void {
  console.log(`  ${condition ? '✓' : '✗'} ${name}: ${detail}`);
  if (!condition) failures.push(`${name}: ${detail}`);
}

function fail(reason: string): never {
  console.log(`PROBE_FAIL ${reason}`);
  process.exit(1);
}

async function waitFor(what: string, condition: () => boolean, timeoutMs: number): Promise<boolean> {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    if (condition()) return true;
    await new Promise(r => setTimeout(r, 250));
  }
  console.log(`  ✗ ${what}: not within ${timeoutMs} ms — events so far: ${JSON.stringify(events)}`);
  failures.push(`${what}: timeout`);
  return false;
}

const has = (type: string, id: string | undefined, reason?: string) =>
  events.some(e => e.type === type && e.id === id && (reason === undefined || e.reason === reason));

async function main(): Promise<void> {
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    flavorPlugins: [new RestFlavorPlugin({ timeoutMillis: 5000 })],
    consumerId: CONSUMER_ID,
    reconnectSeconds: 1,
    sessionIntervalSeconds: 0, // the lease is renewed and released explicitly below
  });
  const brokerHttp = new BrokerHttp({ brokerUrl: BROKER_URL });

  client.consumer.addServiceListener('Payment', undefined, event => {
    const seen: Seen = { type: String(event.type), reason: event.reasonCode ?? undefined, id: event.reference?.id };
    events.push(seen);
    console.log(`EVENT ${seen.type}${seen.reason ? '/' + seen.reason : ''} ${seen.id}`);
  });

  let locator: TrackedServiceLocator | undefined;
  for (let attempt = 0; attempt < 60 && !locator; attempt++) {
    locator = (await client.consumer.findOne('Payment').catch(() => undefined)) as TrackedServiceLocator | undefined;
    if (!locator) await new Promise(r => setTimeout(r, 500));
  }
  if (!locator) fail('no Payment locator within 30s');
  const firstId = locator.reference.id;
  const firstHost = locator.restFlavor()?.host;
  check('bound', locator.state === 'LIVE' && !!firstHost, `${firstId} at ${firstHost}`);

  const before = Number(await locator.invoke('getBalance', { accountId: 'lifecycle' }));
  check('invoke-before', Number.isFinite(before), `${before}`);

  await client.renewSession();
  const held = await brokerHttp.getConsumerSession(CONSUMER_ID);
  check('lease-held', held?.acquiredReferenceIds.includes(firstId ?? '') === true,
    `broker lists ${JSON.stringify(held?.acquiredReferenceIds)}`);

  console.log('PROBE_READY');

  if (SCENARIO === 'F') {
    // Since #55 a same-identity restart on a new port is a MODIFY: the SDK's
    // reconnect check sees the same contract (sd1) with a drifted endpoint
    // (im1) and modifies the registration in place — same reference id, the
    // consumer refreshes instead of rebinding.
    await waitFor('modified-in-place', () => has('MODIFIED', firstId), 90_000);
    check('same-reference-id', locator.reference.id === firstId, `${locator.reference.id}`);
    check('locator-refreshed-not-rebound', locator.state === 'LIVE', `${locator.state}`);
    const hostAfterEvent = locator.restFlavor()?.host ?? '';
    check('endpoint-followed-the-event', hostAfterEvent.includes(`:${NEW_PORT}`), `${hostAfterEvent}`);

    const after = Number(await locator.invoke('getBalance', { accountId: 'lifecycle' }));
    check('invoke-against-new-instance', Number.isFinite(after), `${after} via ${locator.restFlavor()?.host}`);

    await client.renewSession();
    const session = await brokerHttp.getConsumerSession(CONSUMER_ID);
    const ids = session?.acquiredReferenceIds ?? [];
    check('lease-unchanged', ids.length === 1 && ids[0] === firstId, `broker lists ${JSON.stringify(ids)}`);
    check('no-unregistering-seen', !events.some(e => e.type === 'UNREGISTERING'),
      `${events.map(e => e.type).join(',')}`);
  } else if (SCENARIO === 'G') {
    await waitFor('upgrade-available', () => has('UPGRADE_AVAILABLE', firstId), 90_000);
    const successorVisible = await client.consumer.find('Payment');
    check('lookup-prefers-successor',
      successorVisible.length === 1 && successorVisible[0].implementation.version === '2.0.0',
      `${successorVisible.map(l => `${l.reference.id}@${l.implementation.version}`).join(',')}`);
    check('predecessor-still-bound', locator.state === 'LIVE' && locator.reference.id === firstId,
      `${locator.state} ${locator.reference.id}`);
    const stillServing = Number(await locator.invoke('getBalance', { accountId: 'lifecycle' }));
    check('predecessor-still-invokable', Number.isFinite(stillServing) && (locator.restFlavor()?.host ?? '') === firstHost,
      `${stillServing} at ${locator.restFlavor()?.host}`);

    const released = await brokerHttp.deleteConsumerSession(CONSUMER_ID);
    check('lease-released', released.severity !== 'ERROR', `${released.severity}`);

    await waitFor('drain-retire', () => has('UNREGISTERING', firstId, 'REPLACED') && has('RETIRED', firstId, 'REPLACED'), 45_000);
    check('locator-marked-rebind', locator.state === 'REBIND', `${locator.state}`);
    const viaSuccessor = Number(await locator.invoke('getBalance', { accountId: 'lifecycle' }));
    check('rebound-to-successor',
      Number.isFinite(viaSuccessor) && locator.implementation.version === '2.0.0' && locator.reference.id !== firstId,
      `${locator.reference.id}@${locator.implementation.version} at ${locator.restFlavor()?.host}`);
  } else {
    fail(`unknown SCENARIO ${SCENARIO}`);
  }

  await client.close();
  if (failures.length > 0) fail(failures.join(' | '));
  console.log('PROBE_OK');
  process.exit(0);
}

main().catch(error => fail(String(error)));
