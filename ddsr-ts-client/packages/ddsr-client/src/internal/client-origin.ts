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
 * Where a call came from, as far as the registry is entitled to know.
 *
 * The TypeScript mirror of the Java `ClientOrigin` (issue #125/#132).
 * The broker records what arrives in `X-DDSR-Origin`; until this
 * existed, everything the TypeScript client published was recorded as
 * `anonymous`, which made a cross-language registry only half able to
 * say which system told which system what.
 *
 * An origin is two things and deliberately not a third:
 *
 * - a **label**, configured per deployment, naming the SYSTEM
 *   (`payments-prod-eu`), not a machine;
 * - a **runtime id**, new on every start, because an origin explains
 *   where a session came from and is not a durable identity. Java uses
 *   the OSGi framework UUID; the equivalent here is one id per process;
 * - and **not** a host name or an IP. In some deployments those are
 *   personal data, and the question is which system talked to which,
 *   not which person sat behind it.
 *
 * On the wire it is one token, `label/runtimeId`, because it travels in
 * a header. Both halves are kept free of `/` so the token always splits
 * at the first and only separator.
 */

/** The header that carries it. Additive, so none of the frozen wire names of #4 move. */
export const ORIGIN_HEADER = 'X-DDSR-Origin';

/** What a client without a configured label calls itself. */
export const UNNAMED = 'unnamed';

/**
 * What the broker records for a call that carried no origin, and what
 * the catalog's `requestor` has always defaulted to. It is the absence
 * of an identity, not one — the Java side had to learn that the hard
 * way, because the catalog contract declares this word as the
 * parameter's default and it silently beat a real origin.
 */
export const ANONYMOUS = 'anonymous';

const SEPARATOR = '/';

/** One id per process, for as long as it lives. */
const PROCESS_RUNTIME_ID = newRuntimeId();

/** A fresh id, unique within a deployment; also what an unnamed consumer is called. */
export function newRuntimeId(): string {
  const cryptoObject = globalThis.crypto;
  if (cryptoObject?.randomUUID) {
    return cryptoObject.randomUUID();
  }
  // Older runtimes without WebCrypto. Uniqueness within a deployment is
  // all this needs: it separates one run of a system from the next, it
  // is not a secret and nothing authenticates with it.
  return `r${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`;
}

/**
 * Keeps a half of the token free of the separator and of anything a
 * header cannot carry. A label is configuration and a runtime id is a
 * UUID, so this normally changes nothing; it exists so that a careless
 * label cannot make the token unparseable for everyone who reads it, or
 * smuggle a second header.
 */
function sanitize(value: string): string {
  let clean = '';
  for (const character of value) {
    const code = character.charCodeAt(0);
    clean += character === SEPARATOR || character === ',' || code < 0x20 ? '_' : character;
  }
  return clean;
}

export interface ClientOrigin {
  /** Which system this is, as the deployment named it. */
  readonly label: string;
  /** Which run of it. Empty when a sender only gave a label. */
  readonly runtimeId: string;
  /** The token as it travels: `label/runtimeId`. */
  readonly token: string;
}

function make(label: string, runtimeId: string): ClientOrigin {
  return {
    label,
    runtimeId,
    token: runtimeId === '' ? label : `${label}${SEPARATOR}${runtimeId}`,
  };
}

/**
 * The origin of this runtime.
 *
 * @param label the configured deployment label; blank or absent becomes
 *   {@link UNNAMED}, so a client that forgot to name itself is still
 *   distinguishable from one that is not there
 * @param runtimeId overrides the per-process id; for tests
 */
export function clientOrigin(label?: string, runtimeId?: string): ClientOrigin {
  const name = label && label.trim() !== '' ? label : UNNAMED;
  const run = runtimeId && runtimeId.trim() !== '' ? runtimeId : PROCESS_RUNTIME_ID;
  return make(sanitize(name), sanitize(run));
}

/**
 * Reads an origin token as it arrived.
 *
 * Forgiving on purpose, like the flavor CSV parser: a token that cannot
 * be made sense of becomes a label with no runtime id rather than an
 * error. Losing the audit detail of one call is recoverable; refusing
 * the call is not.
 *
 * @returns the origin, or `undefined` when nothing was sent — which the
 *   caller reads as {@link ANONYMOUS}
 */
export function parseOrigin(token: string | null | undefined): ClientOrigin | undefined {
  if (token === null || token === undefined || token.trim() === '') {
    return undefined;
  }
  const trimmed = token.trim();
  const separator = trimmed.indexOf(SEPARATOR);
  return separator < 0
    ? make(trimmed, '')
    : make(trimmed.slice(0, separator), trimmed.slice(separator + 1));
}

/**
 * Wraps a fetch so every request it makes carries the origin.
 *
 * One wrapper rather than a header at each call site: #132 asks for the
 * origin on EVERY call, and a wrapper is the only place where that is
 * true by construction — a new request cannot forget it. The Java
 * client does the same job with a ClientRequestFilter on its shared
 * JAX-RS client.
 *
 * A header the caller set itself is left alone. Nothing else writes
 * this one, so a caller that did set it meant to.
 */
export function withOrigin(inner: typeof fetch, origin: ClientOrigin): typeof fetch {
  return ((input: RequestInfo | URL, init?: RequestInit) => {
    return inner(input, { ...init, headers: addOrigin(init?.headers, origin.token) });
  }) as typeof fetch;
}

/**
 * Adds the origin without changing the shape the caller chose.
 *
 * Normalising everything to `Headers` would be shorter and is wrong:
 * `Headers` lower-cases every name, so a caller — or a test fake —
 * that looks for `X-DDSR-Requestor` afterwards finds nothing. The
 * wrapper's job is to add one header, not to rewrite the other ones.
 */
function addOrigin(headers: HeadersInit | undefined, token: string): HeadersInit {
  if (headers instanceof Headers) {
    const copy = new Headers(headers);
    if (!copy.has(ORIGIN_HEADER)) {
      copy.set(ORIGIN_HEADER, token);
    }
    return copy;
  }
  if (Array.isArray(headers)) {
    return headers.some(([name]) => name.toLowerCase() === ORIGIN_HEADER.toLowerCase())
      ? headers
      : [...headers, [ORIGIN_HEADER, token]];
  }
  const record = { ...(headers ?? {}) } as Record<string, string>;
  const alreadySet = Object.keys(record).some(
    (name) => name.toLowerCase() === ORIGIN_HEADER.toLowerCase());
  if (!alreadySet) {
    record[ORIGIN_HEADER] = token;
  }
  return record;
}
