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
 * The calling half of bearer authentication (SECURITY.md §4), the twin
 * of the Java `TokenSupplier` and its client-credentials implementation.
 *
 * A token goes to the broker and nowhere else: `withBearer` wraps only
 * the broker's own fetch (BrokerHttp, the event stream), never the one a
 * flavor plugin invokes providers with. A token minted for the broker's
 * audience must not reach a provider, where it could be replayed.
 */

/**
 * Where a node gets its tokens; called on every call. Without an
 * audience it answers the broker's token; with one, a token of that
 * target (SECURITY.md §1) — a provider must not receive a token it could
 * replay against the broker.
 */
export type TokenSupplier = (audience?: string) => Promise<string>;

/** The token on every call through `inner`; without a supplier, `inner` itself. */
export function withBearer(inner: typeof fetch, tokens?: TokenSupplier): typeof fetch {
  if (!tokens) {
    return inner;
  }
  return (async (input: RequestInfo | URL, init?: RequestInit) => {
    const headers = new Headers(init?.headers);
    headers.set('Authorization', `Bearer ${await tokens()}`);
    return inner(input, { ...init, headers });
  }) as typeof fetch;
}

export interface ClientCredentialsOptions {
  /** The identity provider's token endpoint. */
  tokenUri: string;
  /** The node's client id — its identity. */
  clientId: string;
  clientSecret: string;
  /** Space-separated scopes; empty asks for the client's defaults. */
  scope?: string;
  /** Injectable transport seam (tests). */
  fetchFn?: typeof fetch;
  /** Injectable clock in milliseconds (tests). */
  now?: () => number;
}

/** How long before its expiry a token is replaced — the Java supplier's margin. */
export const RENEW_BEFORE_SECONDS = 30;

/**
 * A node's token by the OAuth 2.0 client-credentials grant: fetched on
 * first use, kept, fetched again shortly before it expires. Concurrent
 * callers share one request. The client authenticates with HTTP Basic,
 * like the Java supplier, and a refusal reports the provider's reason,
 * never the secret.
 */
export function clientCredentials(options: ClientCredentialsOptions): TokenSupplier {
  const fetchFn = options.fetchFn ?? globalThis.fetch.bind(globalThis);
  const now = options.now ?? Date.now;
  const basic = btoa(`${encodeURIComponent(options.clientId)}:${encodeURIComponent(options.clientSecret)}`);
  const scope = options.scope?.trim() ?? '';
  // One token per scope asked for — the configured one for the broker,
  // one per provider audience — like the Java supplier.
  const held = new Map<string, { token: string; renewAt: number }>();
  const pending = new Map<string, Promise<string>>();

  async function fetchToken(scope: string): Promise<string> {
    const body = new URLSearchParams({ grant_type: 'client_credentials' });
    if (scope) {
      body.set('scope', scope);
    }
    let response: Response;
    try {
      response = await fetchFn(options.tokenUri, {
        method: 'POST',
        headers: {
          Authorization: `Basic ${basic}`,
          'Content-Type': 'application/x-www-form-urlencoded',
          Accept: 'application/json',
        },
        body: body.toString(),
      });
    } catch (error) {
      throw new Error(`no bearer token: ${options.tokenUri} is not reachable (${String(error)})`);
    }
    const text = await response.text();
    if (response.status !== 200) {
      throw new Error(`no bearer token: ${options.tokenUri} answered ${response.status} ${text}`);
    }
    const answer = JSON.parse(text) as { access_token?: string; expires_in?: number };
    if (!answer.access_token) {
      throw new Error(`no bearer token: ${options.tokenUri} answered without an access_token`);
    }
    const lifetime = typeof answer.expires_in === 'number' ? answer.expires_in : 60;
    const margin = lifetime > 2 * RENEW_BEFORE_SECONDS ? RENEW_BEFORE_SECONDS : lifetime / 2;
    held.set(scope, { token: answer.access_token, renewAt: now() + (lifetime - margin) * 1000 });
    return answer.access_token;
  }

  // A target's token asks for the scope of the audience's name, which the
  // identity provider maps to that audience (Keycloak: an optional client
  // scope with an audience mapper).
  return (audience?: string) => {
    const wanted = audience?.trim() || scope;
    const current = held.get(wanted);
    if (current !== undefined && now() < current.renewAt) {
      return Promise.resolve(current.token);
    }
    let request = pending.get(wanted);
    if (!request) {
      request = fetchToken(wanted).finally(() => pending.delete(wanted));
      pending.set(wanted, request);
    }
    return request;
  };
}
