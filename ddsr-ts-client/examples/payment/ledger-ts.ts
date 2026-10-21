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
 * A TypeScript provider of a guarded contract — the TypeScript half of
 * what the RSA Ledger shows in Java (SECURITY.md §7b): reading needs
 * ledger-reader, recording ledger-writer, the version nobody. The
 * dispatcher holds every call to that with a verifier built from the
 * same DDSR_JWT_* variables a Java node reads, and the flavor tells
 * consumers which issuer and audience a token must have.
 *
 * Run by the host harness (scenario T); without DDSR_JWT_ISSUER it
 * refuses to serve, because a guarded contract is never served open by
 * accident.
 */
import { createServer } from 'node:http';
import { DdsrClientImpl, attachShutdownHooks, createJwtVerifier } from '@ddsr/client';
import { DDSRFactory } from '@ddsr/model';
import type { SecurityRequirement } from '@ddsr/model';
import { restDispatcher } from '@ddsr/flavor-rest';
import { brokerTokens, consumerId } from './broker-auth';

const BROKER_URL = process.env.BROKER_URL ?? 'http://localhost:8887/ddsr/rest';
const SERVICE_URL = process.env.SERVICE_URL ?? 'http://localhost:9094/ts-ledger';
const AUDIENCE = process.env.DDSR_JWT_AUDIENCES ?? 'ts-ledger';
const log = (m: string) => console.log(`[ts-ledger] ${m}`);

const f = DDSRFactory.eINSTANCE;

function requirement(authenticated: boolean, ...scopes: string[]): SecurityRequirement {
  const r = f.createSecurityRequirement();
  r.authenticated = authenticated;
  r.scopes.push(...scopes);
  return r;
}

const issuer = process.env.DDSR_JWT_ISSUER;
if (!issuer) {
  throw new Error('TsLedger demands authentication: set DDSR_JWT_ISSUER, DDSR_JWT_JWKS_URI and DDSR_JWT_AUDIENCES');
}

// --- the contract: who may call is part of it -------------------------
const contract = f.createServiceInterface();
contract.name = 'TsLedger';
contract.version = '1.0.0';
contract.security = requirement(true, 'ledger-reader');

const serviceUrl = new URL(SERVICE_URL);
const flavor = f.createRestFlavor();
flavor.name = 'ts-ledger-rest';
flavor.host = `${serviceUrl.protocol}//${serviceUrl.host}`;
flavor.basePath = serviceUrl.pathname.replace(/\/+$/, '');
for (const [name, security] of [
  ['balance', undefined],
  ['record', requirement(true, 'ledger-writer')],
  ['version', requirement(false)],
] as [string, SecurityRequirement | undefined][]) {
  const operation = f.createServiceOperation();
  operation.name = name;
  if (security) {
    operation.security = security;
  }
  const result = f.createParameter();
  result.name = 'result';
  result.type = name === 'version' ? 'string' : 'double';
  operation.returnValue = result;
  contract.operations.push(operation);
  const opFlavor = f.createRestOperationFlavor();
  opFlavor.name = name;
  opFlavor.method = 'GET';
  opFlavor.path = `/${name}`;
  opFlavor.produces.push('text/plain');
  opFlavor.operation = operation;
  flavor.operationFlavors.push(opFlavor);
}
const scheme = f.createBearerTokenScheme();
scheme.name = 'bearer';
scheme.issuer = issuer;
scheme.audiences.push(AUDIENCE);
scheme.jwksUri = process.env.DDSR_JWT_JWKS_URI ?? '';
scheme.scopeClaim = process.env.DDSR_JWT_SCOPE_CLAIM ?? 'scope';
flavor.securitySchemes.push(scheme);

const provider = f.createServiceProvider();
provider.name = 'ts-ledger';
provider.version = '1.0.0';
const implementation = f.createServiceImplementation();
implementation.name = 'ts-ledger-rest';
implementation.version = '1.0.0';
implementation.implementationId = 'ts-ledger:TsLedger:1.0.0';
implementation.serviceInterfaces.push(contract);
implementation.flavors.push(flavor);
provider.implementations.push(implementation);

// --- the service: nothing in it checks a caller -----------------------
let balance = 100;
const ledger = {
  balance: () => balance,
  record: () => (balance += 5),
  version: () => 'ts-ledger 1.0',
};

const dispatch = restDispatcher(flavor, ledger, {
  verifier: createJwtVerifier({
    issuer,
    jwksUri: scheme.jwksUri,
    audiences: [AUDIENCE],
    scopeClaim: process.env.DDSR_JWT_SCOPE_CLAIM,
    subjectClaim: process.env.DDSR_JWT_SUBJECT_CLAIM,
  }),
  log: (m) => log(m),
});

const server = createServer((request, response) => {
  void dispatch({ method: request.method ?? 'GET', url: request.url ?? '/', headers: request.headers })
    .then(answer => {
      if (!answer) {
        response.writeHead(404, { 'Content-Type': 'text/plain' });
        response.end('not found');
        return;
      }
      response.writeHead(answer.status, { 'Content-Type': answer.contentType, ...(answer.headers ?? {}) });
      response.end(answer.body ?? '');
    })
    .catch(error => {
      response.writeHead(500, { 'Content-Type': 'text/plain' });
      response.end(String(error));
    });
});

async function main(): Promise<void> {
  await new Promise<void>(resolve => server.listen(Number(serviceUrl.port || 80), resolve));
  log(`serving ${SERVICE_URL}`);
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    originLabel: 'ts-ledger-harness',
    tokenSupplier: brokerTokens(),
    consumerId: consumerId(),
    sessionIntervalSeconds: 0,
  });
  await client.catalog.ensureEntry(contract);
  const registration = await client.provider.publish(provider, implementation);
  log(`published as reference ${registration.reference.id}`);
  attachShutdownHooks(client, { afterClose: async () => { server.close(); } });
}

main().catch(error => {
  log(`failed: ${String(error)}`);
  process.exit(1);
});
