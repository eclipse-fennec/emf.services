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
package org.eclipse.fennec.services.security.mtls.internal;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import org.eclipse.fennec.services.security.jwt.VerifiedPrincipal;

/**
 * What a client certificate says about its holder, as this node reads
 * it (SECURITY.md §7d).
 *
 * <ul>
 * <li>the <b>subject</b> is the certificate's CN — readable and stable,
 * what a consumer id and the owner of a registration are held against,
 * the twin of a token's {@code azp};</li>
 * <li>the <b>issuer</b> is the issuing authority's DN, so the same CN
 * from another authority is somebody else;</li>
 * <li>the <b>roles</b> are this node's mapping of subjects: a
 * certificate carries none, and putting them in one would make the
 * authority decide what a node lets a caller do.</li>
 * </ul>
 *
 * <p>Trusted issuers narrow what the trust store already admitted: with
 * none named, whatever the TLS layer accepted is accepted here.
 */
final class CertificatePrincipals {

	private final Set<X500Principal> trustedIssuers;
	private final Map<String, Set<String>> roles;

	CertificatePrincipals(String[] trustedIssuers, String[] roles) {
		this.trustedIssuers = new LinkedHashSet<>();
		for (String issuer : trustedIssuers == null ? new String[0] : trustedIssuers) {
			if (issuer != null && !issuer.isBlank()) {
				// X500Principal compares canonically: spacing and case of
				// attribute names do not make two DNs different.
				this.trustedIssuers.add(new X500Principal(issuer.strip()));
			}
		}
		this.roles = rolesOf(roles == null ? new String[0] : roles);
	}

	/**
	 * The principal of the certificate's holder.
	 *
	 * @throws CertificateRejectedException when the certificate is out of
	 *         its validity, from an issuer this node does not trust, or
	 *         names nobody
	 */
	VerifiedPrincipal principalOf(X509Certificate certificate) throws CertificateRejectedException {
		try {
			// The TLS layer checked this already; a certificate handed on by
			// a gateway has been checked by the gateway. Checked again,
			// because it costs nothing and a stale one is the cheap mistake.
			certificate.checkValidity();
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			throw new CertificateRejectedException("certificate outside its validity: " + e.getMessage());
		}
		X500Principal issuer = certificate.getIssuerX500Principal();
		if (!trustedIssuers.isEmpty() && !trustedIssuers.contains(issuer)) {
			throw new CertificateRejectedException("issuer " + issuer.getName() + " is not trusted");
		}
		String subject = commonName(certificate.getSubjectX500Principal());
		if (subject == null || subject.isBlank()) {
			throw new CertificateRejectedException("the certificate names no CN: "
					+ certificate.getSubjectX500Principal().getName());
		}
		return new VerifiedPrincipal(subject, issuer.getName(), roles.getOrDefault(subject, Set.of()));
	}

	/** The CN of a distinguished name, or {@code null} when it has none. */
	static String commonName(X500Principal name) {
		try {
			for (Rdn rdn : new LdapName(name.getName()).getRdns()) {
				if ("CN".equalsIgnoreCase(rdn.getType())) {
					return String.valueOf(rdn.getValue());
				}
			}
			return null;
		} catch (InvalidNameException e) {
			return null;
		}
	}

	/**
	 * {@code subject=role,role} per entry. A subject named twice gets the
	 * roles of both entries; an entry without {@code =} names a subject
	 * with no roles, which is a principal that can be authenticated and
	 * nothing more.
	 */
	private static Map<String, Set<String>> rolesOf(String[] entries) {
		Map<String, Set<String>> mapped = new HashMap<>();
		for (String entry : entries) {
			if (entry == null || entry.isBlank()) {
				continue;
			}
			int split = entry.indexOf('=');
			String subject = (split < 0 ? entry : entry.substring(0, split)).strip();
			Set<String> granted = mapped.computeIfAbsent(subject, s -> new LinkedHashSet<>());
			if (split >= 0) {
				Arrays.stream(entry.substring(split + 1).split("[,\\s]+"))
						.filter(role -> !role.isBlank())
						.forEach(granted::add);
			}
		}
		return mapped;
	}

	/** A certificate that does not prove anybody here. */
	static final class CertificateRejectedException extends Exception {

		private static final long serialVersionUID = 1L;

		CertificateRejectedException(String message) {
			super(message);
		}
	}
}
