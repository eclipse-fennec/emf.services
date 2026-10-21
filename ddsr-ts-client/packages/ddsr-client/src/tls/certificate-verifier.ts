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
import type { VerifiedPrincipal } from '../internal/jwt-verifier.js';

/** A certificate as a host has it: Node's X509Certificate, or a TLS peer certificate with its DER. */
export type ClientCertificate = X509Certificate | { raw: Buffer };

export interface CertificateVerifierOptions {
  /**
   * DNs of the authorities whose client certificates are accepted, in
   * any attribute order — `CN=Fennec CA,O=Fennec` and `O=Fennec, CN=Fennec CA`
   * are one authority. Empty: whatever the TLS layer admitted.
   */
  trustedIssuers?: readonly string[];
  /**
   * The roles a certificate's CN holds here — a certificate carries none.
   * Either a map, or entries `subject=role,role` as the Java node's
   * DDSR_MTLS_ROLES spells them.
   */
  roles?: Readonly<Record<string, readonly string[]>> | readonly string[];
}

/** A certificate that does not prove anybody here. */
export class CertificateRejectedError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'CertificateRejectedError';
  }
}

export interface CertificateVerifier {
  /** The holder, or a CertificateRejectedError saying why not. */
  verify(certificate: ClientCertificate): VerifiedPrincipal;
}

/**
 * What a client certificate says about its holder, as this node reads it
 * (SECURITY.md §7d) — the twin of the Java node's mapping:
 *
 * - the subject is the certificate's CN, the twin of a token's azp;
 * - the issuer is the issuing authority's DN, so the same CN from
 *   another authority is somebody else;
 * - the roles are this node's mapping of subjects.
 *
 * Verifying the chain is the TLS layer's job (the server's `ca` with
 * `requestCert` and `rejectUnauthorized`); this narrows by issuer, checks
 * the validity again, and names the holder.
 */
export function createCertificateVerifier(options: CertificateVerifierOptions = {}): CertificateVerifier {
  const trusted = (options.trustedIssuers ?? []).filter(dn => dn.trim() !== '').map(canonicalName);
  const roles = rolesOf(options.roles);
  return {
    verify(certificate: ClientCertificate): VerifiedPrincipal {
      const x509 = certificate instanceof X509Certificate ? certificate : new X509Certificate(certificate.raw);
      const now = Date.now();
      if (now < Date.parse(x509.validFrom) || now > Date.parse(x509.validTo)) {
        throw new CertificateRejectedError(`certificate outside its validity (${x509.validFrom} – ${x509.validTo})`);
      }
      const issuer = canonicalName(x509.issuer);
      if (trusted.length > 0 && !trusted.includes(issuer)) {
        throw new CertificateRejectedError(`issuer ${oneLine(x509.issuer)} is not trusted`);
      }
      const subject = attribute(x509.subject, 'CN');
      if (!subject) {
        throw new CertificateRejectedError(`the certificate names no CN: ${oneLine(x509.subject)}`);
      }
      return { subject, issuer: oneLine(x509.issuer), scopes: roles.get(subject) ?? [] };
    },
  };
}

/** `type=value` pairs, attribute types upper-cased and order dropped — one authority, however it is written. */
function canonicalName(dn: string): string {
  return dn.split(/[,\n]/)
    .map(part => part.trim())
    .filter(part => part !== '')
    .map(part => {
      const at = part.indexOf('=');
      return at < 0 ? part : `${part.slice(0, at).trim().toUpperCase()}=${part.slice(at + 1).trim()}`;
    })
    .sort()
    .join(',');
}

function attribute(dn: string, type: string): string | undefined {
  for (const line of dn.split('\n')) {
    const at = line.indexOf('=');
    if (at > 0 && line.slice(0, at).trim().toUpperCase() === type) {
      return line.slice(at + 1).trim();
    }
  }
  return undefined;
}

function oneLine(dn: string): string {
  return dn.split('\n').map(line => line.trim()).filter(line => line !== '').join(', ');
}

function rolesOf(roles: CertificateVerifierOptions['roles']): Map<string, string[]> {
  const mapped = new Map<string, string[]>();
  if (!roles) return mapped;
  if (!Array.isArray(roles)) {
    for (const [subject, granted] of Object.entries(roles as Record<string, readonly string[]>)) {
      mapped.set(subject, [...granted]);
    }
    return mapped;
  }
  // subject=role,role per entry; a subject named twice gets both entries' roles.
  for (const entry of roles as readonly string[]) {
    if (entry.trim() === '') continue;
    const at = entry.indexOf('=');
    const subject = (at < 0 ? entry : entry.slice(0, at)).trim();
    const granted = mapped.get(subject) ?? [];
    if (at >= 0) {
      granted.push(...entry.slice(at + 1).split(/[,\s]+/).filter(role => role !== ''));
    }
    mapped.set(subject, granted);
  }
  return mapped;
}
