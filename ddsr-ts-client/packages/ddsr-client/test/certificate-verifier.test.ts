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
import { CertificateRejectedError, createCertificateVerifier } from '../src/tls/certificate-verifier.js';

/**
 * A client certificate becomes the principal a token would (SECURITY.md
 * §7d): the CN, the authority's DN, the roles this node maps. The
 * fixtures are public certificates only, valid until 2126 — except the
 * one that expired on purpose.
 */
const pem = (name: string) => readFileSync(new URL(`./fixtures/tls/${name}.pem`, import.meta.url));
const certificate = (name: string) => new X509Certificate(pem(name));

describe('createCertificateVerifier', () => {
  const verifier = createCertificateVerifier({
    trustedIssuers: ['CN=Fennec Test CA,O=Fennec'],
    roles: ['java-client=ledger-reader,service-consumer', 'harness=service-admin'],
  });

  it('names the holder by its CN and maps its roles', () => {
    const principal = verifier.verify(certificate('java-client'));

    expect(principal.subject).toBe('java-client');
    expect(principal.issuer).toBe('O=Fennec, CN=Fennec Test CA');
    expect(principal.scopes).toEqual(['ledger-reader', 'service-consumer']);
  });

  it('reads a TLS peer certificate by its DER, as a host has it', () => {
    expect(verifier.verify({ raw: certificate('java-client').raw }).subject).toBe('java-client');
  });

  it('compares issuers whatever the attribute order and spacing', () => {
    const spaced = createCertificateVerifier({ trustedIssuers: [' o=Fennec ,  CN=Fennec Test CA '] });

    expect(spaced.verify(certificate('java-client')).subject).toBe('java-client');
  });

  it('refuses another authority, a certificate without a CN, and one out of its validity', () => {
    expect(() => verifier.verify(certificate('stranger'))).toThrow(CertificateRejectedError);
    expect(() => verifier.verify(certificate('nameless'))).toThrow(/no CN/);
    expect(() => verifier.verify(certificate('expired'))).toThrow(/validity/);
  });

  it('gives an unmapped subject no roles, and takes a map as well as entries', () => {
    expect(createCertificateVerifier().verify(certificate('java-client')).scopes).toEqual([]);
    expect(createCertificateVerifier({ roles: { 'java-client': ['ledger-writer'] } })
      .verify(certificate('java-client')).scopes).toEqual(['ledger-writer']);
  });
});
