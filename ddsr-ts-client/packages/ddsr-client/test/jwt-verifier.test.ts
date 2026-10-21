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

import { beforeAll, describe, expect, it } from 'vitest';
import { TokenRejectedError, createJwtVerifier } from '../src/internal/jwt-verifier';

/**
 * The TypeScript verifier, held to what the Java NimbusJwtVerifierTest
 * asks: signature, issuer, audience, lifetime, the subject and scope
 * claims — and that `none` and a foreign key fail.
 */

const ISSUER = 'http://localhost:18080/realms/fennec';
const JWKS = 'http://localhost:18080/realms/fennec/protocol/openid-connect/certs';

let signer: CryptoKeyPair;
let stranger: CryptoKeyPair;
let jwks: unknown;

const b64 = (bytes: Uint8Array) => btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
const json = (value: unknown) => b64(new TextEncoder().encode(JSON.stringify(value)));

async function token(claims: Record<string, unknown>, options: { key?: CryptoKey; alg?: string; kid?: string } = {}) {
  const head = json({ alg: options.alg ?? 'RS256', kid: options.kid ?? 'k1', typ: 'JWT' });
  const body = json(claims);
  const signature = new Uint8Array(await crypto.subtle.sign('RSASSA-PKCS1-v1_5', options.key ?? signer.privateKey,
    new TextEncoder().encode(`${head}.${body}`)));
  return `${head}.${body}.${b64(signature)}`;
}

function claims(extra: Record<string, unknown> = {}): Record<string, unknown> {
  const now = Math.floor(Date.now() / 1000);
  return { iss: ISSUER, aud: ['ts-ledger', 'account'], azp: 'rsa-example-consumer', sub: '4f1c-generated',
    iat: now, exp: now + 300, realm_access: { roles: ['ledger-reader'] }, scope: 'ts-ledger email', ...extra };
}

let fetched = 0;
const fetchFn = (async () => {
  fetched++;
  return new Response(JSON.stringify(jwks), { status: 200 });
}) as unknown as typeof fetch;

const verifier = (extra: Partial<Parameters<typeof createJwtVerifier>[0]> = {}) => createJwtVerifier({
  issuer: ISSUER, jwksUri: JWKS, audiences: ['ts-ledger'], scopeClaim: 'realm_access.roles', subjectClaim: 'azp',
  fetchFn, ...extra,
});

beforeAll(async () => {
  const params = { name: 'RSASSA-PKCS1-v1_5', modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' };
  signer = await crypto.subtle.generateKey(params, true, ['sign', 'verify']) as CryptoKeyPair;
  stranger = await crypto.subtle.generateKey(params, true, ['sign', 'verify']) as CryptoKeyPair;
  const publicJwk = await crypto.subtle.exportKey('jwk', signer.publicKey);
  jwks = { keys: [{ ...publicJwk, kid: 'k1', use: 'sig', alg: 'RS256' }] };
});

describe('createJwtVerifier', () => {
  it('verifies a token and names the caller by the configured claim', async () => {
    const principal = await verifier().verify(await token(claims()));

    expect(principal.subject).toBe('rsa-example-consumer');
    expect(principal.issuer).toBe(ISSUER);
    expect(principal.scopes).toEqual(['ledger-reader']);
  });

  it('reads OAuth scopes as a space-separated string', async () => {
    const principal = await verifier({ scopeClaim: 'scope', subjectClaim: 'sub' }).verify(await token(claims()));

    expect(principal.subject).toBe('4f1c-generated');
    expect(principal.scopes).toEqual(['ts-ledger', 'email']);
  });

  it('turns away another issuer, another audience and an expired token', async () => {
    const v = verifier();
    const now = Math.floor(Date.now() / 1000);

    await expect(v.verify(await token(claims({ iss: 'http://evil/realms/fennec' })))).rejects.toThrow(TokenRejectedError);
    await expect(v.verify(await token(claims({ aud: 'ddsr-broker' }))))
      .rejects.toThrow(/audience/);
    await expect(v.verify(await token(claims({ exp: now - 120 })))).rejects.toThrow(/expired/);
  });

  it('turns away a foreign signature, alg none and garbage', async () => {
    const v = verifier();
    const forged = await token(claims(), { key: stranger.privateKey });
    const none = `${json({ alg: 'none', kid: 'k1' })}.${json(claims())}.`;

    await expect(v.verify(forged)).rejects.toThrow(/signature/);
    await expect(v.verify(none)).rejects.toThrow(/algorithm/);
    await expect(v.verify('not.a.jwt')).rejects.toThrow(TokenRejectedError);
  });

  it('fetches the keys once, and again only for a key it has not seen', async () => {
    fetched = 0;
    const v = verifier();

    await v.verify(await token(claims()));
    await v.verify(await token(claims()));
    await expect(v.verify(await token(claims(), { kid: 'rotated' }))).rejects.toThrow(/no signing key/);

    expect(fetched).toBe(2);
  });

  it('refuses to be built without an audience', () => {
    expect(() => createJwtVerifier({ issuer: ISSUER, jwksUri: JWKS, audiences: [] })).toThrow(/audience/);
  });
});
