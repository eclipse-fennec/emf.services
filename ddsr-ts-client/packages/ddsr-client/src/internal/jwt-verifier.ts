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
 * The serving half of bearer authentication in TypeScript — the twin of
 * the Java `NimbusJwtVerifier` (SECURITY.md §7a), so a TypeScript provider
 * can hold a call to its contract's requirements like a Java one does.
 *
 * Deliberately without a dependency: RS256 is pinned and checked with
 * WebCrypto, which Node and every browser carry, and the issuer's keys
 * are fetched from its JWKS and kept by `kid`. A token that names a key
 * this verifier has not seen triggers one fresh fetch — a key rotation is
 * followed, a forged `kid` costs one request and nothing else.
 */

/** Who is calling, as the verifier established it. */
export interface VerifiedPrincipal {
  readonly subject: string;
  readonly issuer: string;
  /** The scopes — or roles, read from `realm_access.roles` — the token carried. */
  readonly scopes: readonly string[];
}

export interface JwtVerifierOptions {
  /** The expected `iss`, exactly. */
  issuer: string;
  /** Where the issuer's signing keys are fetched. */
  jwksUri: string;
  /** The audiences a token must carry one of — this node's own. */
  audiences: string[];
  /** Dot path of the scopes: `scope` (OAuth, space-separated) or e.g. `realm_access.roles`. Default `scope`. */
  scopeClaim?: string;
  /** Which claim names the caller: `sub`, or `azp` for Keycloak client credentials. Default `sub`. */
  subjectClaim?: string;
  /** Tolerance on `exp`, `nbf` and `iat`, in seconds. Default 30. */
  clockSkewSeconds?: number;
  /** Injectable transport seam (tests). */
  fetchFn?: typeof fetch;
  /** Injectable clock in milliseconds (tests). */
  now?: () => number;
}

/** A token that does not verify; the reason is for the log, never for the caller. */
export class TokenRejectedError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'TokenRejectedError';
  }
}

export interface JwtVerifier {
  verify(compactJwt: string): Promise<VerifiedPrincipal>;
}

export function createJwtVerifier(options: JwtVerifierOptions): JwtVerifier {
  if (!options.issuer || !options.jwksUri || options.audiences.filter(a => a).length === 0) {
    throw new Error('a verifier needs an issuer, its JWKS URI and at least one audience');
  }
  const fetchFn = options.fetchFn ?? globalThis.fetch.bind(globalThis);
  const now = options.now ?? Date.now;
  const skew = options.clockSkewSeconds ?? 30;
  const scopePath = (options.scopeClaim ?? 'scope').split('.');
  const subjectClaim = options.subjectClaim ?? 'sub';
  const audiences = new Set(options.audiences.filter(a => a));
  const keys = new Map<string, Promise<CryptoKey>>();
  let refreshed: Promise<void> | undefined;

  async function loadKeys(): Promise<void> {
    const response = await fetchFn(options.jwksUri, { headers: { Accept: 'application/json' } });
    if (!response.ok) {
      throw new TokenRejectedError(`the issuer's keys are not reachable: ${response.status}`);
    }
    const set = await response.json() as { keys?: (JsonWebKey & { kid?: string; use?: string; alg?: string })[] };
    for (const jwk of set.keys ?? []) {
      if (jwk.kty !== 'RSA' || !jwk.kid || (jwk.use && jwk.use !== 'sig') || (jwk.alg && jwk.alg !== 'RS256')) {
        continue;
      }
      const { kid, ...material } = jwk;
      keys.set(kid, crypto.subtle.importKey('jwk', { ...material, alg: 'RS256', ext: true },
        { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['verify']));
    }
  }

  async function keyFor(kid: string): Promise<CryptoKey> {
    if (!keys.has(kid)) {
      refreshed ??= loadKeys().finally(() => {
        refreshed = undefined;
      });
      await refreshed;
    }
    const key = keys.get(kid);
    if (!key) {
      throw new TokenRejectedError(`no signing key '${kid}' at the issuer`);
    }
    return key;
  }

  return {
    async verify(compactJwt: string): Promise<VerifiedPrincipal> {
      const parts = compactJwt.split('.');
      if (parts.length !== 3) {
        throw new TokenRejectedError('not a JWT');
      }
      const header = decodeJson(parts[0]) as { alg?: string; kid?: string };
      // Pinned: `none` and HMAC fail here, before any key is looked at.
      if (header.alg !== 'RS256') {
        throw new TokenRejectedError(`algorithm ${String(header.alg)} is not accepted`);
      }
      if (!header.kid) {
        throw new TokenRejectedError('the token names no key');
      }
      const valid = await crypto.subtle.verify('RSASSA-PKCS1-v1_5', await keyFor(header.kid),
        base64url(parts[2]), new TextEncoder().encode(`${parts[0]}.${parts[1]}`));
      if (!valid) {
        throw new TokenRejectedError('the signature does not verify');
      }
      const claims = decodeJson(parts[1]) as Record<string, unknown>;
      if (claims.iss !== options.issuer) {
        throw new TokenRejectedError(`issuer ${String(claims.iss)} is not trusted here`);
      }
      const aud = Array.isArray(claims.aud) ? claims.aud : [claims.aud];
      if (!aud.some(a => typeof a === 'string' && audiences.has(a))) {
        throw new TokenRejectedError(`audience ${JSON.stringify(claims.aud)} is not this node`);
      }
      const seconds = now() / 1000;
      if (typeof claims.exp !== 'number' || claims.exp + skew < seconds) {
        throw new TokenRejectedError('the token has expired');
      }
      if (typeof claims.nbf === 'number' && claims.nbf - skew > seconds) {
        throw new TokenRejectedError('the token is not valid yet');
      }
      if (typeof claims.iat !== 'number' || claims.iat - skew > seconds) {
        throw new TokenRejectedError('the token was issued in the future');
      }
      const subject = claims[subjectClaim];
      if (typeof subject !== 'string' || subject === '') {
        throw new TokenRejectedError(`the subject claim '${subjectClaim}' is not a name`);
      }
      return { subject, issuer: options.issuer, scopes: scopesOf(claims, scopePath) };
    },
  };
}

/** A list is taken as is, a string is split on whitespace, anything else is no scope. */
function scopesOf(claims: Record<string, unknown>, path: string[]): string[] {
  let node: unknown = claims;
  for (const segment of path) {
    node = node && typeof node === 'object' ? (node as Record<string, unknown>)[segment] : undefined;
  }
  if (Array.isArray(node)) {
    return node.filter((s): s is string => typeof s === 'string' && s !== '');
  }
  return typeof node === 'string' ? node.trim().split(/\s+/).filter(s => s) : [];
}

function base64url(segment: string): Uint8Array {
  const padded = segment.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(segment.length / 4) * 4, '=');
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function decodeJson(segment: string): unknown {
  try {
    return JSON.parse(new TextDecoder().decode(base64url(segment)));
  } catch {
    throw new TokenRejectedError('not a JWT');
  }
}
