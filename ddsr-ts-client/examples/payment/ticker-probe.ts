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
 * Output contract (parsed by the harness): a line `ticks [1, 2, 3] — the
 * stream ended`, a line `took [1, 2, 3] of forever and left`, then
 * PROBE_OK, or PROBE_FAIL <reason>.
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
}

async function main(): Promise<void> {
  const plugin = new MqttFlavorPlugin({
    protocol: PROTOCOL, timeoutMs: 15000, tls: nodeTls(), consumer: consumerId('harness-probe-ts'),
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

  await plugin.close();
  await client.close();
  if (ticks.join(',') !== '1,2,3') fail(`expected 1, 2, 3 and got [${ticks.join(', ')}]`);
  console.log('PROBE_OK');
  process.exit(0);
}

main().catch(error => fail(String(error)));
