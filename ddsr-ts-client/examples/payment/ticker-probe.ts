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
 * A stream across the language border (Parameter.delivery = STREAM).
 *
 * The provider is a plain OSGi service a Java framework exports through
 * Remote Service Admin: `PushStream<Integer> count(int upTo)`, with no
 * contract document - the contract is derived from the interface, and
 * its return value says STREAM. This consumer knows none of that. It
 * looks the contract up, sees an operation whose answer is a stream and
 * iterates it: one message per value over the messaging broker, and the
 * one that ends the stream.
 *
 * TICKER_PROTOCOL says which protocol this consumer speaks (MQTT or
 * AMQP) - the broker lists only what a consumer can call.
 *
 * Then it takes three numbers of `forever`, a stream that does not end
 * by itself, and leaves: closing the session is what stops the Java
 * service's stream.
 *
 * Then it listens to `beats`, the stream every receiver shares (#252),
 * twice at once: a fast listener hears fifty beats in a row, a slow one
 * (a buffer of four, a tenth of a second per beat) skips to the latest,
 * as the contract's DISCARD_OLDEST allows - and the source does not slow
 * down for it.
 *
 * Output contract (parsed by the harness): a line `ticks [1, 2, 3] — the
 * stream ended`, a line `took [1, 2, 3] of forever and left`, a line
 * `heard 50 beats in a row`, a line `slowly heard 10 beats, skipped N`,
 * then PROBE_OK, or PROBE_FAIL <reason>.
 */

import { MqttFlavorPlugin } from '@ddsr/transport-mqtt';
import { DdsrClientImpl, serviceProxy, toArray } from '@ddsr/client';
import type { ServiceOperation } from '@ddsr/model';
import { brokerTokens, consumerId, nodeFetch, nodeTls } from './broker-auth';

const BROKER_URL = process.env.BROKER_URL ?? 'http://localhost:8887/ddsr/rest';
const PROTOCOL = process.env.TICKER_PROTOCOL === 'AMQP' ? 'AMQP' : 'MQTT';
const UP_TO = 3;

function fail(reason: string): never {
  console.log(`PROBE_FAIL ${reason}`);
  process.exit(1);
}

/** What the generator would write for the derived contract. */
interface Ticker {
  count(upTo: number): AsyncIterable<number>;
  forever(everyMillis: number): AsyncIterable<number>;
  beats(): AsyncIterable<number>;
}

async function main(): Promise<void> {
  const plugin = new MqttFlavorPlugin({
    protocol: PROTOCOL, timeoutMs: 15000, tls: nodeTls(), consumer: consumerId('harness-probe-ts'),
    // Small on purpose: a slow caller holds no more than four values, and
    // the credit it grants is what keeps the provider from sending more.
    streamBuffer: { size: 4 },
  });
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    flavorPlugins: [plugin],
    consumerId: consumerId('harness-probe-ts'),
    tokenSupplier: brokerTokens(),
    fetchFn: nodeFetch(),
    reconnectSeconds: 1,
    sessionIntervalSeconds: 0,
  });

  let locator;
  for (let attempt = 0; attempt < 60 && !locator; attempt++) {
    locator = await client.consumer.findOne('Ticker').catch(() => undefined);
    if (!locator) await new Promise(r => setTimeout(r, 500));
  }
  if (!locator) fail(`no Ticker over ${PROTOCOL} within 30s`);

  const count = toArray<ServiceOperation>(locator.serviceInterface.operations)
    .find(operation => operation.name === 'count');
  const delivery = String(count?.returnValue?.delivery ?? '');
  console.log(`  ${delivery === 'STREAM' ? '✓' : '✗'} contract-says-stream: count returns ${delivery}`);
  if (delivery !== 'STREAM') fail(`the derived contract says ${delivery}, not STREAM`);

  const ticks: number[] = [];
  for await (const tick of serviceProxy<Ticker>(locator).count(UP_TO)) ticks.push(Number(tick));
  console.log(`ticks [${ticks.join(', ')}] — the stream ended`);

  // A stream that does not end by itself: three numbers, then this side
  // leaves. Leaving closes the session, and the provider's stream stops.
  const some: number[] = [];
  for await (const tick of serviceProxy<Ticker>(locator).forever(100)) {
    some.push(Number(tick));
    if (some.length === 3) break;
  }
  console.log(`took [${some.join(', ')}] of forever and left`);

  // A slow caller (org.gecko.messaging#33): thirty numbers through a buffer
  // of four. Without credit the provider would overrun it and the stream
  // would be given up; with it, the Java service waits for this side.
  const slow: number[] = [];
  for await (const tick of serviceProxy<Ticker>(locator).count(30)) {
    slow.push(Number(tick));
    await new Promise(resolve => setTimeout(resolve, 20));
  }
  console.log(`slowly took ${slow.length} of 30 through a buffer of 4 — the provider waited`);
  if (slow.join(',') !== Array.from({ length: 30 }, (_, i) => i + 1).join(',')) {
    fail(`the slow stream brought [${slow.join(', ')}]`);
  }

  // The beats every receiver shares: one source, two listeners here and
  // whoever else listens (the Java consumer of the same scenario).
  const beats = toArray<ServiceOperation>(locator.serviceInterface.operations)
    .find(operation => operation.name === 'beats');
  const shared = `${String(beats?.returnValue?.delivery ?? '')}/${String(beats?.returnValue?.overflow ?? 'FAIL')}`;
  console.log(`  ${shared === 'BROADCAST/DISCARD_OLDEST' ? '✓' : '✗'} contract-says-broadcast: beats returns ${shared}`);
  if (shared !== 'BROADCAST/DISCARD_OLDEST') fail(`the derived contract says ${shared} for beats`);
  const listen = async (count: number, pause: number): Promise<number[]> => {
    const heard: number[] = [];
    for await (const beat of serviceProxy<Ticker>(locator).beats()) {
      heard.push(Number(beat));
      if (heard.length === count) break;
      if (pause > 0) await new Promise(resolve => setTimeout(resolve, pause));
    }
    return heard;
  };
  const [fast, slowly] = await Promise.all([listen(50, 0), listen(10, 100)]);
  const gaps = (heard: number[]) => heard.slice(1).filter((beat, i) => beat !== heard[i] + 1).length;
  const skipped = slowly[slowly.length - 1] - slowly[0] + 1 - slowly.length;
  console.log(gaps(fast) === 0 ? `heard ${fast.length} beats in a row` : `heard beats with gaps [${fast.join(', ')}]`);
  console.log(`slowly heard ${slowly.length} beats, skipped ${skipped}`);
  if (gaps(fast) !== 0) fail(`the fast listener missed beats: [${fast.join(', ')}]`);
  if (skipped <= 0) fail(`the slow listener skipped nothing: [${slowly.join(', ')}]`);

  await plugin.close();
  await client.close();
  if (ticks.join(',') !== '1,2,3') fail(`expected 1, 2, 3 and got [${ticks.join(', ')}]`);
  console.log('PROBE_OK');
  process.exit(0);
}

main().catch(error => fail(String(error)));
