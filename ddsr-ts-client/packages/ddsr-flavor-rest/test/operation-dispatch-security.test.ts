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
import type { RestFlavor, SecurityRequirement, ServiceInterface } from '@ddsr/model';
import { TokenRejectedError } from '@ddsr/client';
import type { JwtVerifier, VerifiedPrincipal } from '@ddsr/client';
import { restDispatcher } from '../src/operation-dispatch';

/**
 * A TypeScript provider holds a call to its contract's requirements like
 * a Java one does (SECURITY.md §7b) — the twin of RestDispatcherSecurityTest:
 * 401 without a valid token, 403 naming the missing role, and a contract
 * that cannot be checked is not served.
 */

const f = DDSRFactory.eINSTANCE;

function requirement(authenticated: boolean, ...scopes: string[]): SecurityRequirement {
  const r = f.createSecurityRequirement();
  r.authenticated = authenticated;
  r.scopes.push(...scopes);
  return r;
}

/** The TS ledger: reader by default, writer to record, version for anybody. */
function ledger(withScheme = true, permitAll = true): RestFlavor {
  const contract: ServiceInterface = f.createServiceInterface();
  contract.name = 'TsLedger';
  contract.security = requirement(true, 'ledger-reader');
  const flavor = f.createRestFlavor();
  flavor.name = 'ts-ledger-rest';
  flavor.basePath = '/ts-ledger';
  const ops: [string, string, SecurityRequirement | undefined][] = [
    ['balance', '/balance', undefined],
    ['record', '/record', requirement(true, 'ledger-writer')],
  ];
  if (permitAll) {
    ops.push(['version', '/version', requirement(false)]);
  }
  for (const [name, path, security] of ops) {
    const operation = f.createServiceOperation();
    operation.name = name;
    if (security) {
      operation.security = security;
    }
    contract.operations.push(operation);
    const opFlavor = f.createRestOperationFlavor();
    opFlavor.name = name;
    opFlavor.method = 'GET';
    opFlavor.path = path;
    opFlavor.produces.push('text/plain');
    opFlavor.operation = operation;
    flavor.operationFlavors.push(opFlavor);
  }
  if (withScheme) {
    const scheme = f.createBearerTokenScheme();
    scheme.issuer = 'http://localhost:18080/realms/fennec';
    scheme.audiences.push('ts-ledger');
    flavor.securitySchemes.push(scheme);
  }
  return flavor;
}

const implementation = { balance: () => 100, record: () => 105, version: () => 'ts-ledger 1.0' };

/** "reader" and "writer" are tokens; anything else does not verify. */
const verifier: JwtVerifier = {
  async verify(token: string): Promise<VerifiedPrincipal> {
    const roles: Record<string, string[]> = { reader: ['ledger-reader'], writer: ['ledger-reader', 'ledger-writer'] };
    if (!roles[token]) {
      throw new TokenRejectedError('forged');
    }
    return { subject: token, issuer: 'http://localhost:18080/realms/fennec', scopes: roles[token] };
  },
};

const call = (dispatch: ReturnType<typeof restDispatcher>, path: string, token?: string) =>
  dispatch({ method: 'GET', url: `/ts-ledger${path}`, headers: token ? { authorization: `Bearer ${token}` } : {} });

describe('restDispatcher security', () => {
  it('turns a guarded call without a token away with 401 and the challenge', async () => {
    const answer = await call(restDispatcher(ledger(), implementation, { verifier }), '/balance');

    expect(answer?.status).toBe(401);
    expect(answer?.headers?.['WWW-Authenticate']).toBe('Bearer');
  });

  it('turns a token that does not verify away with 401', async () => {
    expect((await call(restDispatcher(ledger(), implementation, { verifier }), '/version', 'forged'))?.status)
      .toBe(401);
  });

  it('serves a caller holding the role and refuses one without it, naming it', async () => {
    const dispatch = restDispatcher(ledger(), implementation, { verifier });

    expect((await call(dispatch, '/balance', 'reader'))?.body).toBe('100');
    const refused = await call(dispatch, '/record', 'reader');
    expect(refused?.status).toBe(403);
    expect(refused?.body).toBe('record needs ledger-writer');
    expect((await call(dispatch, '/record', 'writer'))?.status).toBe(200);
  });

  it('lets an operation that permits all be called without a token', async () => {
    expect((await call(restDispatcher(ledger(), implementation, { verifier }), '/version'))?.body)
      .toBe('ts-ledger 1.0');
  });

  it('is strict when every operation is guarded', async () => {
    const dispatch = restDispatcher(ledger(true, false), implementation, { verifier });

    expect((await call(dispatch, '/balance'))?.status).toBe(401);
  });

  it('is not built when its flavor names a scheme it cannot verify', () => {
    expect(() => restDispatcher(ledger(), implementation)).toThrow(/no verifier/);
  });

  it('is not built for a guarded contract without a scheme, unless open is said', async () => {
    expect(() => restDispatcher(ledger(false), implementation)).toThrow(/no scheme/);

    const open = restDispatcher(ledger(false), implementation, { auth: 'none' });
    expect((await call(open, '/record'))?.status).toBe(200);
  });
});
