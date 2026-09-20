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
import {
  ANONYMOUS,
  ORIGIN_HEADER,
  UNNAMED,
  clientOrigin,
  parseOrigin,
  withOrigin,
} from '../src/internal/client-origin';

const UUID_LIKE = '5f2b8c1e-0f4a-4a6e-9b3d-7c1a2e8f4d60';

/**
 * The TypeScript half of the client origin (#132). Mirrors the Java
 * `ClientOriginTest` case for case, because the two only agree if they
 * are checked against the same expectations — the token is a wire
 * format, not an implementation detail of either side.
 */
describe('clientOrigin', () => {
  it('carries the label and the runtime id', () => {
    const origin = clientOrigin('payments-prod-eu', UUID_LIKE);

    expect(origin.label).toBe('payments-prod-eu');
    expect(origin.runtimeId).toBe(UUID_LIKE);
    expect(origin.token).toBe(`payments-prod-eu/${UUID_LIKE}`);
  });

  it('uses one id per process, stable across calls', () => {
    const first = clientOrigin('a-client');
    const second = clientOrigin('a-client');

    expect(first.runtimeId).not.toBe('');
    expect(second.runtimeId).toBe(first.runtimeId);
  });

  it('a client that did not name itself is still distinguishable from one that is not there', () => {
    expect(clientOrigin(undefined, UUID_LIKE).label).toBe(UNNAMED);
    expect(clientOrigin('   ', UUID_LIKE).label).toBe(UNNAMED);
  });

  it('round-trips through the wire token', () => {
    const origin = clientOrigin('payments-prod-eu', UUID_LIKE);

    expect(parseOrigin(origin.token)).toEqual(origin);
  });

  it('nothing sent is nothing to record', () => {
    expect(parseOrigin(undefined)).toBeUndefined();
    expect(parseOrigin(null)).toBeUndefined();
    expect(parseOrigin('  ')).toBeUndefined();
  });

  it('a token without a runtime id is kept as a label, not refused', () => {
    const origin = parseOrigin('some-old-client');

    expect(origin?.label).toBe('some-old-client');
    expect(origin?.runtimeId).toBe('');
    expect(origin?.token).toBe('some-old-client');
  });

  it('a label containing the separator cannot make the token unreadable', () => {
    const origin = clientOrigin('team/payments', UUID_LIKE);

    expect(origin.label).toBe('team_payments');
    expect(parseOrigin(origin.token)).toEqual(origin);
  });

  it('a label with a newline cannot smuggle a second header', () => {
    const origin = clientOrigin('evil\r\nX-Other: value', UUID_LIKE);

    expect(origin.token).not.toContain('\r');
    expect(origin.token).not.toContain('\n');
    expect(parseOrigin(origin.token)).toEqual(origin);
  });

  it('the runtime id splits off at the first separator, whatever follows', () => {
    const origin = parseOrigin(`a-label/${UUID_LIKE}/trailing`);

    expect(origin?.label).toBe('a-label');
    expect(origin?.runtimeId).toBe(`${UUID_LIKE}/trailing`);
  });
});

describe('withOrigin', () => {
  /** Records what the wrapped fetch actually sent. */
  function recording() {
    const seen: Array<{ headers: unknown }> = [];
    const inner = (async (_input: unknown, init?: RequestInit) => {
      seen.push({ headers: init?.headers });
      return new Response('', { status: 200 });
    }) as typeof fetch;
    return { inner, seen };
  }

  const origin = clientOrigin('payments-prod-eu', UUID_LIKE);

  it('stamps the origin on a request that had no headers at all', async () => {
    const { inner, seen } = recording();

    await withOrigin(inner, origin)('http://broker.test/x');

    expect((seen[0].headers as Record<string, string>)[ORIGIN_HEADER]).toBe(origin.token);
  });

  it('keeps the headers the caller set, in the spelling the caller used', async () => {
    const { inner, seen } = recording();

    await withOrigin(inner, origin)('http://broker.test/x', {
      headers: { 'Content-Type': 'application/xml', 'X-DDSR-Requestor': 'ts-tests' },
    });

    const sent = seen[0].headers as Record<string, string>;
    expect(sent['Content-Type']).toBe('application/xml');
    expect(sent['X-DDSR-Requestor']).toBe('ts-tests');
    expect(sent[ORIGIN_HEADER]).toBe(origin.token);
  });

  it('does not overwrite an origin the caller set on purpose', async () => {
    const { inner, seen } = recording();

    await withOrigin(inner, origin)('http://broker.test/x', {
      headers: { [ORIGIN_HEADER]: 'somebody-else/abc' },
    });

    expect((seen[0].headers as Record<string, string>)[ORIGIN_HEADER]).toBe('somebody-else/abc');
  });

  it('works when the caller passes a Headers instance', async () => {
    const { inner, seen } = recording();

    await withOrigin(inner, origin)('http://broker.test/x', {
      headers: new Headers({ Accept: 'text/event-stream' }),
    });

    const sent = seen[0].headers as Headers;
    expect(sent.get('Accept')).toBe('text/event-stream');
    expect(sent.get(ORIGIN_HEADER)).toBe(origin.token);
  });

  it('does not mutate the Headers object it was given', async () => {
    const { inner } = recording();
    const given = new Headers({ Accept: 'text/event-stream' });

    await withOrigin(inner, origin)('http://broker.test/x', { headers: given });

    expect(given.has(ORIGIN_HEADER)).toBe(false);
  });

  it('works when the caller passes header tuples', async () => {
    const { inner, seen } = recording();

    await withOrigin(inner, origin)('http://broker.test/x', {
      headers: [['Accept', 'application/xml']],
    });

    const sent = seen[0].headers as Array<[string, string]>;
    expect(sent).toContainEqual([ORIGIN_HEADER, origin.token]);
    expect(sent).toContainEqual(['Accept', 'application/xml']);
  });

  it('leaves the rest of the request init alone', async () => {
    const seen: RequestInit[] = [];
    const inner = (async (_input: unknown, init?: RequestInit) => {
      seen.push(init ?? {});
      return new Response('', { status: 200 });
    }) as typeof fetch;

    await withOrigin(inner, origin)('http://broker.test/x', { method: 'POST', body: 'hello' });

    expect(seen[0].method).toBe('POST');
    expect(seen[0].body).toBe('hello');
  });

  it('anonymous is a word the broker uses, and never what this client sends', () => {
    expect(clientOrigin().token).not.toBe(ANONYMOUS);
    expect(clientOrigin().label).toBe(UNNAMED);
  });
});
