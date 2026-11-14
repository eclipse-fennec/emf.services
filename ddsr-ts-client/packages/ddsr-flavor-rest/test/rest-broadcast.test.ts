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
import type { RestFlavor } from '@ddsr/model';
import { dataAsText, decodeResult, readStructured } from '@ddsr/client';
import { restDispatcher } from '../src/operation-dispatch';
import { eventsOf } from '../src/rest-streams';

// #252: one source, many receivers over REST, each with a buffer of its own.

function sensor(overflow: 'FAIL' | 'DISCARD_OLDEST'): RestFlavor {
  const f = DDSRFactory.eINSTANCE;
  const readings = f.createServiceOperation();
  readings.name = 'readings';
  const result = f.createParameter();
  result.name = 'result';
  result.type = 'int';
  result.upperBound = -1;
  result.delivery = 'BROADCAST';
  result.overflow = overflow;
  readings.returnValue = result;
  const flavor = f.createRestFlavor();
  flavor.name = 'sensor-rest';
  const op = f.createRestOperationFlavor();
  op.name = 'readings';
  op.method = 'GET';
  op.path = '/readings';
  op.operation = readings;
  flavor.operationFlavors.push(op);
  return flavor;
}

/** The values and the last event name of a receiver's frames. */
async function read(frames: AsyncIterable<string>, take?: (seen: number) => Promise<void>) {
  const values: unknown[] = [];
  let last = '';
  const bytes = (async function* () {
    for await (const frame of frames) {
      if (take) await take(values.length);
      yield new TextEncoder().encode(frame);
    }
  })();
  for await (const { event, data } of eventsOf(bytes)) {
    last = event;
    if (event === 'value') values.push(decodeResult(dataAsText(readStructured(data))).value);
    if (event === 'error') last = `error: ${decodeResult(dataAsText(readStructured(data))).error}`;
  }
  return { values, last };
}

async function scene(overflow: 'FAIL' | 'DISCARD_OLDEST') {
  let asked = 0;
  let release!: () => void;
  const go = new Promise<void>(resolve => { release = resolve; });
  const dispatch = restDispatcher(sensor(overflow), {
    readings: async function* () {
      asked++;
      await go;
      for (let i = 0; i < 300; i++) yield i;
    },
  }, { auth: 'none' });
  const fast = (await dispatch({ method: 'GET', url: '/readings' }))!;
  const slow = (await dispatch({ method: 'GET', url: '/readings' }))!;
  expect(fast.contentType).toBe('text/event-stream');
  let letSlowGo!: () => void;
  const slowMayGo = new Promise<void>(resolve => { letSlowGo = resolve; });
  const fastRead = read(fast.events!);
  const slowRead = read(slow.events!, async seen => { if (seen >= 1) await slowMayGo; });
  setTimeout(release, 20);
  const fastResult = await fastRead;
  letSlowGo();
  return { fast: fastResult, slow: await slowRead, asked };
}

describe('a broadcast over REST', () => {
  it('latest wins: the fast receiver gets everything, the slow one what its buffer held', async () => {
    const { fast, slow, asked } = await scene('DISCARD_OLDEST');
    expect(fast.values).toHaveLength(300);
    expect(fast.last).toBe('end');
    expect(slow.values.length).toBeLessThan(300);
    expect(slow.values[slow.values.length - 1]).toBe(299);
    expect(slow.last).toBe('end');
    expect(asked).toBe(1);
  });

  it('must not lose: the slow receiver fails, the fast one goes on', async () => {
    const { fast, slow } = await scene('FAIL');
    expect(fast.values).toHaveLength(300);
    expect(slow.last).toMatch(/^error: .*fell behind/);
    expect(slow.values).toEqual(slow.values.map((_, i) => i));
  });
});
