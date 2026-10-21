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

import { X509Certificate } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { DDSRFactory } from '@ddsr/model';
import type { RestFlavor, SecurityRequirement } from '@ddsr/model';
import { createCertificateVerifier } from '@ddsr/client';
import { restDispatcher } from '../src/operation-dispatch';

/**
 * A TypeScript provider served by client certificate (SECURITY.md §7d):
 * the same 401 / 403 as in bearer, the caller named by its certificate
 * instead of its token — and no call without one.
 */
const f = DDSRFactory.eINSTANCE;
const fixture = (name: string) => new X509Certificate(readFileSync(
  new URL(`../../ddsr-client/test/fixtures/tls/${name}.pem`, import.meta.url)));

function requirement(authenticated: boolean, ...scopes: string[]): SecurityRequirement {
  const r = f.createSecurityRequirement();
  r.authenticated = authenticated;
  r.scopes.push(...scopes);
  return r;
}

function ledger(): RestFlavor {
  const contract = f.createServiceInterface();
  contract.name = 'TsLedger';
  contract.security = requirement(true, 'ledger-reader');
  const flavor = f.createRestFlavor();
  flavor.name = 'ts-ledger-rest';
  flavor.basePath = '/ts-ledger';
  for (const [name, security] of [
    ['balance', undefined], ['record', requirement(true, 'ledger-writer')], ['version', requirement(false)],
  ] as [string, SecurityRequirement | undefined][]) {
    const operation = f.createServiceOperation();
    operation.name = name;
    if (security) operation.security = security;
    contract.operations.push(operation);
    const opFlavor = f.createRestOperationFlavor();
    opFlavor.name = name;
    opFlavor.method = 'GET';
    opFlavor.path = `/${name}`;
    opFlavor.produces.push('text/plain');
    opFlavor.operation = operation;
    flavor.operationFlavors.push(opFlavor);
  }
  const scheme = f.createMutualTlsScheme();
  scheme.trustedIssuers.push('CN=Fennec Test CA,O=Fennec');
  flavor.securitySchemes.push(scheme);
  return flavor;
}

const implementation = { balance: () => 100, record: () => 105, version: () => 'ts-ledger 1.0' };
const certificates = createCertificateVerifier({
  trustedIssuers: ['CN=Fennec Test CA,O=Fennec'], roles: ['java-client=ledger-reader'],
});

describe('restDispatcher in mode mtls', () => {
  const dispatch = restDispatcher(ledger(), implementation, { certificates });
  const call = (path: string, certificate?: X509Certificate) =>
    dispatch({ method: 'GET', url: `/ts-ledger/${path}`, clientCertificate: certificate });

  it('turns a call without a certificate away, even to an open operation', async () => {
    expect((await call('version'))?.status).toBe(401);
    expect((await call('balance'))?.status).toBe(401);
  });

  it('lets the holder of a mapped role through', async () => {
    expect((await call('balance', fixture('java-client')))?.status).toBe(200);
    expect((await call('version', fixture('java-client')))?.status).toBe(200);
  });

  it('names the role a holder lacks', async () => {
    const answer = await call('record', fixture('java-client'));

    expect(answer?.status).toBe(403);
    expect(answer?.body).toBe('record needs ledger-writer');
  });

  it('turns away a certificate of another authority', async () => {
    expect((await call('balance', fixture('stranger')))?.status).toBe(401);
  });

  it('is not built without a certificate verifier when the flavor asks for certificates', () => {
    expect(() => restDispatcher(ledger(), implementation)).toThrow(/certificate verifier/);
  });
});
