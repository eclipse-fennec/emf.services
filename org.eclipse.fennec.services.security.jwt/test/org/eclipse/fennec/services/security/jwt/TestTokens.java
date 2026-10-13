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
package org.eclipse.fennec.services.security.jwt;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/** An issuer in a test: a key pair, a JWKS over it, tokens signed with it — or with a stranger's key. */
public final class TestTokens {

	public static final String ISSUER = "http://localhost:9080/auth/realms/dim";
	public static final String AUDIENCE = "ddsr-broker";

	public final RSAKey key;
	public final RSAKey otherKey;

	public TestTokens() throws JOSEException {
		key = new RSAKeyGenerator(2048).keyID("k1").generate();
		otherKey = new RSAKeyGenerator(2048).keyID("k2").generate();
	}

	public JWKSource<SecurityContext> keys() {
		return new ImmutableJWKSet<>(new JWKSet(key.toPublicJWK()));
	}

	/** A valid token with an OAuth {@code scope} string and Keycloak-style realm roles. */
	public String token(String sub, String scope, List<String> realmRoles) throws JOSEException {
		return token(key, sub, scope, realmRoles, new Date(System.currentTimeMillis() + 300_000), ISSUER, AUDIENCE);
	}

	public String token(RSAKey signer, String sub, String scope, List<String> realmRoles, Date exp, String iss,
			String aud) throws JOSEException {
		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
				.issuer(iss).subject(sub).audience(aud).issueTime(new Date()).expirationTime(exp)
				.claim("azp", "payments-node");
		if (scope != null) {
			claims.claim("scope", scope);
		}
		if (realmRoles != null) {
			claims.claim("realm_access", Map.of("roles", realmRoles));
		}
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signer.getKeyID()).build(),
				claims.build());
		jwt.sign(new RSASSASigner(signer));
		return jwt.serialize();
	}
}
