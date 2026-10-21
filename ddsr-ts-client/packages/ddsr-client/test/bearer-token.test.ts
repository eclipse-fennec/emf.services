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
import { BrokerHttp } from '../src/internal/broker-http';
import { RENEW_BEFORE_SECONDS, clientCredentials, withBearer } from '../src/internal/bearer-token';

/**
 * The calling half of bearer authentication (SECURITY.md §4), held to
 * the same expectations as the Java `ClientCredentialsTokensTest` and
 * `RestTransportAuthorizationTest`.
 */

interface Seen {
  url: string;
  headers: Headers;
  body?: string;
}

function recordingFetch(seen: Seen[], answer: () => Response): typeof fetch {
  return (async (input: RequestInfo | URL, init?: RequestInit) => {
    seen.push({ url: String(input), headers: new Headers(init?.headers), body: init?.body as string | undefined });
    return answer();
  }) as typeof fetch;
}

function tokenEndpoint(seen: Seen[], state: { issued: number; status: number; expiresIn: number }): typeof fetch {
  return recordingFetch(seen, () => state.status === 200
    ? new Response(JSON.stringify({ access_token: `token-${++state.issued}`, expires_in: state.expiresIn }), { status: 200 })
    : new Response('{"error":"invalid_client"}', { status: state.status }));
}

describe('withBearer', () => {
  it('puts the token on the call and keeps the other headers', async () => {
    const seen: Seen[] = [];
    const wrapped = withBearer(recordingFetch(seen, () => new Response('')), async () => 't0k3n');

    await wrapped('http://broker.test/ddsr/rest/catalog', { headers: { Accept: 'application/xml' } });

    expect(seen[0].headers.get('Authorization')).toBe('Bearer t0k3n');
    expect(seen[0].headers.get('Accept')).toBe('application/xml');
  });

  it('is the plain fetch without a supplier', () => {
    const inner = recordingFetch([], () => new Response(''));
    expect(withBearer(inner, undefined)).toBe(inner);
  });

  it('rides on every broker call of BrokerHttp', async () => {
    const seen: Seen[] = [];
    const broker = new BrokerHttp({
      brokerUrl: 'http://broker.test/ddsr/rest',
      fetchFn: recordingFetch(seen, () => new Response('<xmi:XMI xmlns:xmi="http://www.omg.org/XMI"/>', { status: 200 })),
      tokenSupplier: async () => 't0k3n',
    });

    await broker.getCatalogEntry('Payment').catch(() => undefined);
    await broker.heartbeat('ref-1', 30).catch(() => undefined);

    expect(seen).not.toHaveLength(0);
    expect(seen.every((call) => call.headers.get('Authorization') === 'Bearer t0k3n')).toBe(true);
  });
});

describe('clientCredentials', () => {
  const options = (fetchFn: typeof fetch, now: () => number) => ({
    tokenUri: 'http://idp.test/token', clientId: 'payments-node', clientSecret: 's3cr3t', fetchFn, now,
  });

  it('authenticates with Basic and asks for its scope', async () => {
    const seen: Seen[] = [];
    const state = { issued: 0, status: 200, expiresIn: 300 };
    const tokens = clientCredentials({ ...options(tokenEndpoint(seen, state), () => 0), scope: 'provider consumer' });

    expect(await tokens()).toBe('token-1');
    expect(seen[0].headers.get('Authorization')).toBe(`Basic ${btoa('payments-node:s3cr3t')}`);
    expect(seen[0].body).toBe('grant_type=client_credentials&scope=provider+consumer');
  });

  it('reuses a token until shortly before it expires', async () => {
    const seen: Seen[] = [];
    const state = { issued: 0, status: 200, expiresIn: 300 };
    let now = 0;
    const tokens = clientCredentials(options(tokenEndpoint(seen, state), () => now));

    expect(await tokens()).toBe('token-1');
    now = (300 - RENEW_BEFORE_SECONDS - 1) * 1000;
    expect(await tokens()).toBe('token-1');
    now += 1000;
    expect(await tokens()).toBe('token-2');
  });

  it('lets concurrent callers share one request', async () => {
    const seen: Seen[] = [];
    const state = { issued: 0, status: 200, expiresIn: 300 };
    const tokens = clientCredentials(options(tokenEndpoint(seen, state), () => 0));

    const all = await Promise.all([tokens(), tokens(), tokens()]);

    expect(all).toEqual(['token-1', 'token-1', 'token-1']);
    expect(seen).toHaveLength(1);
  });

  it('keeps one token per audience, the broker\'s apart from a provider\'s', async () => {
    const seen: Seen[] = [];
    const state = { issued: 0, status: 200, expiresIn: 300 };
    const tokens = clientCredentials({ ...options(tokenEndpoint(seen, state), () => 0), scope: 'ddsr-broker' });

    const broker = await tokens();
    const provider = await tokens('payments');

    expect(broker).not.toBe(provider);
    expect(seen.map((call) => call.body)).toEqual([
      'grant_type=client_credentials&scope=ddsr-broker',
      'grant_type=client_credentials&scope=payments',
    ]);
    expect(await tokens('payments')).toBe(provider);
    expect(await tokens()).toBe(broker);
    expect(seen).toHaveLength(2);
  });

  it('reports a refusal with the reason and never the secret', async () => {
    const state = { issued: 0, status: 401, expiresIn: 300 };
    const tokens = clientCredentials(options(tokenEndpoint([], state), () => 0));

    const failure = await tokens().then(() => undefined, (error: Error) => error);

    expect(failure?.message).toContain('401');
    expect(failure?.message).toContain('invalid_client');
    expect(failure?.message).not.toContain('s3cr3t');
  });
});
