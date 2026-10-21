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
package org.eclipse.fennec.services.rsa.config;

import java.util.List;

/**
 * Who an RSA node is, as its identity provider and its certificate
 * authority know it (SECURITY.md §6, §7d): the client credentials it
 * calls with, the certificate it shows, and — for a node that serves —
 * the issuer whose tokens it accepts.
 *
 * <p>Said once in the role configuration and derived from there into the
 * token supplier (calls to the broker and to providers), the verifier
 * (calls this node serves) and the REST distribution (the scheme a
 * guarded contract's flavor names). Without it the node is what it was:
 * open, and calling without a token.
 *
 * @param tokenUri the identity provider's token endpoint; empty means no token
 * @param clientId the node's client id — and, unless said otherwise, its consumer id
 * @param clientSecret the client's secret
 * @param scope space-separated scopes to ask for; empty for the client's defaults
 * @param issuer the issuer of the tokens this node accepts; empty means it verifies none
 * @param jwksUri where that issuer's signing keys are
 * @param audiences what a token for this node must carry one of
 * @param scopeClaim where the scopes are read from — {@code realm_access.roles} for roles
 * @param subjectClaim which claim names a caller — {@code azp} for Keycloak client credentials
 * @param keyStore the node's certificate and key (PKCS12); empty means it shows none
 * @param keyStorePassword the password of the key store and of its key
 * @param trustStore the authorities whose server certificates it believes; empty means the JVM's
 * @param trustStorePassword the password of the trust store
 * @param brokerByCertificate whether the node proves itself to the broker by certificate rather than
 *        by token — discovery over mTLS while calls to providers keep their tokens
 */
public record NodeIdentity(
		String tokenUri,
		String clientId,
		String clientSecret,
		String scope,
		String issuer,
		String jwksUri,
		List<String> audiences,
		String scopeClaim,
		String subjectClaim,
		String keyStore,
		String keyStorePassword,
		String trustStore,
		String trustStorePassword,
		boolean brokerByCertificate) {

	/** A node without an identity: open, and calling without a token. */
	public static final NodeIdentity NONE = new NodeIdentity("", "", "", "", "", "", List.of(), "scope", "sub");

	/** An identity of tokens only: no certificate, the broker shown the token. */
	public NodeIdentity(String tokenUri, String clientId, String clientSecret, String scope, String issuer,
			String jwksUri, List<String> audiences, String scopeClaim, String subjectClaim) {
		this(tokenUri, clientId, clientSecret, scope, issuer, jwksUri, audiences, scopeClaim, subjectClaim, "", "",
				"", "", false);
	}

	public NodeIdentity {
		tokenUri = blankIfNull(tokenUri);
		clientId = blankIfNull(clientId);
		clientSecret = blankIfNull(clientSecret);
		scope = blankIfNull(scope);
		issuer = blankIfNull(issuer);
		jwksUri = blankIfNull(jwksUri);
		audiences = audiences == null ? List.of()
				: audiences.stream().filter(a -> a != null && !a.isBlank()).map(String::strip).toList();
		scopeClaim = scopeClaim == null || scopeClaim.isBlank() ? "scope" : scopeClaim.strip();
		subjectClaim = subjectClaim == null || subjectClaim.isBlank() ? "sub" : subjectClaim.strip();
		keyStore = blankIfNull(keyStore);
		keyStorePassword = keyStorePassword == null ? "" : keyStorePassword;
		trustStore = blankIfNull(trustStore);
		trustStorePassword = trustStorePassword == null ? "" : trustStorePassword;
	}

	/** Whether this node has TLS settings for its client: a certificate to show or authorities to believe. */
	public boolean hasTls() {
		return !keyStore.isBlank() || !trustStore.isBlank();
	}

	/** Whether this node shows a token when it calls. */
	public boolean callsWithToken() {
		return !tokenUri.isBlank();
	}

	/** Whether this node verifies the tokens of the calls it serves. */
	public boolean verifies() {
		return !issuer.isBlank();
	}

	private static String blankIfNull(String value) {
		return value == null ? "" : value.strip();
	}
}
