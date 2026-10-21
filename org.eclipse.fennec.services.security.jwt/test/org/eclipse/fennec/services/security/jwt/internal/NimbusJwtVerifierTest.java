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
package org.eclipse.fennec.services.security.jwt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.fennec.services.Principal;
import org.eclipse.fennec.services.security.jwt.JwtVerifier;
import org.eclipse.fennec.services.security.jwt.JwtVerifier.TokenRejectedException;
import org.eclipse.fennec.services.security.jwt.TestTokens;
import org.eclipse.fennec.services.security.jwt.VerifiedPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;

import com.nimbusds.jose.JOSEException;

class NimbusJwtVerifierTest {

	private TestTokens tokens;
	private NimbusJwtVerifier verifier;

	@BeforeEach
	void setUp() throws JOSEException {
		tokens = new TestTokens();
		verifier = new NimbusJwtVerifier();
		verifier.init(tokens.keys(), TestTokens.ISSUER, Set.of(TestTokens.AUDIENCE), "scope", 5);
	}

	@Test
	void aValidTokenYieldsItsSubjectIssuerAndScopes() throws Exception {
		VerifiedPrincipal p = verifier.verify(tokens.token("payments-node", "payment:write payment:read", null));

		assertThat(p.subject()).isEqualTo("payments-node");
		assertThat(p.issuer()).isEqualTo(TestTokens.ISSUER);
		assertThat(p.scopes()).containsExactlyInAnyOrder("payment:write", "payment:read");
		assertThat(p.hasScope("payment:write")).isTrue();
		assertThat(p.hasScope("admin")).isFalse();
	}

	@Test
	void theScopeClaimMayBeAKeycloakRoleList() throws Exception {
		verifier.init(tokens.keys(), TestTokens.ISSUER, Set.of(TestTokens.AUDIENCE), "realm_access.roles", 5);

		VerifiedPrincipal p = verifier.verify(tokens.token("node", "ignored", List.of("provider", "consumer")));

		assertThat(p.scopes()).containsExactlyInAnyOrder("provider", "consumer");
	}

	@Test
	void aTokenWithoutTheClaimHasNoScopesButIsStillAPrincipal() throws Exception {
		VerifiedPrincipal p = verifier.verify(tokens.token("node", null, null));

		assertThat(p.scopes()).isEmpty();
		assertThat(p.subject()).isEqualTo("node");
	}

	@Test
	void theModelPrincipalIsAFreshCopyEachTime() throws Exception {
		VerifiedPrincipal p = verifier.verify(tokens.token("node", "b a", null));

		Principal first = p.model();
		Principal second = p.model();

		assertThat(first).isNotSameAs(second);
		assertThat(first.getSubject()).isEqualTo("node");
		assertThat(first.getIssuer()).isEqualTo(TestTokens.ISSUER);
		assertThat(first.getScopes()).as("sorted, so two copies render alike").containsExactly("a", "b");
	}

	@Test
	void anyOfTheConfiguredAudiencesIsAccepted() throws Exception {
		verifier.init(tokens.keys(), TestTokens.ISSUER, Set.of("ddsr-broker", "payments"), "scope", 5);
		Date exp = new Date(System.currentTimeMillis() + 60_000);

		assertThat(verifier.verify(tokens.token(tokens.key, "n", null, null, exp, TestTokens.ISSUER, "payments")).subject())
				.isEqualTo("n");
	}

	@Test
	void anUnknownSignerIsRejected() throws Exception {
		String forged = tokens.token(tokens.otherKey, "node", "admin", null,
				new Date(System.currentTimeMillis() + 60_000), TestTokens.ISSUER, TestTokens.AUDIENCE);

		assertThatThrownBy(() -> verifier.verify(forged)).isInstanceOf(TokenRejectedException.class);
	}

	@Test
	void anExpiredTokenIsRejected() throws Exception {
		String expired = tokens.token(tokens.key, "node", null, null,
				new Date(System.currentTimeMillis() - 600_000), TestTokens.ISSUER, TestTokens.AUDIENCE);

		assertThatThrownBy(() -> verifier.verify(expired)).isInstanceOf(TokenRejectedException.class);
	}

	@Test
	void aWrongIssuerOrAudienceIsRejected() throws Exception {
		Date exp = new Date(System.currentTimeMillis() + 60_000);
		String otherIssuer = tokens.token(tokens.key, "node", null, null, exp, "http://evil/realms/dim", TestTokens.AUDIENCE);
		String otherAudience = tokens.token(tokens.key, "node", null, null, exp, TestTokens.ISSUER, "account");

		assertThatThrownBy(() -> verifier.verify(otherIssuer)).isInstanceOf(TokenRejectedException.class);
		assertThatThrownBy(() -> verifier.verify(otherAudience))
				.as("a token minted for somebody else is not replayable here")
				.isInstanceOf(TokenRejectedException.class);
	}

	@Test
	void garbageAndAlgNoneAreRejectedNotThrown() {
		assertThatThrownBy(() -> verifier.verify("not.a.jwt")).isInstanceOf(TokenRejectedException.class);
		assertThatThrownBy(() -> verifier.verify("eyJhbGciOiJub25lIn0.e30.")).isInstanceOf(TokenRejectedException.class);
	}

	@Test
	void theSubjectCanBeTakenFromAnotherClaim() throws Exception {
		// A Keycloak client-credentials token: sub is the service account's
		// generated id, azp the client — the name a node is configured by.
		verifier.init(tokens.keys(), TestTokens.ISSUER, Set.of(TestTokens.AUDIENCE), "scope", "azp", 5);

		VerifiedPrincipal principal = verifier.verify(tokens.token("4f1c-service-account-id", "provider", null));

		assertThat(principal.subject()).isEqualTo("payments-node");
	}

	// ------------------------------------------------------------------
	// configuration from the environment: empty means no verifier
	// ------------------------------------------------------------------

	@Test
	void withoutAnIssuerThereIsNoVerifier() throws Exception {
		BundleContext context = mock(BundleContext.class);
		NimbusJwtVerifier component = new NimbusJwtVerifier();

		component.activate(context, config("", "", ""));

		verify(context, never()).registerService(eq(JwtVerifier.class), any(JwtVerifier.class), any());
	}

	@Test
	void anIssuerWithoutKeysOrAudiencesIsRefusedNotHalfStarted() {
		BundleContext context = mock(BundleContext.class);

		assertThatThrownBy(() -> new NimbusJwtVerifier().activate(context, config(TestTokens.ISSUER, "", "")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new NimbusJwtVerifier()
				.activate(context, config(TestTokens.ISSUER, "http://idp.example/certs", "")))
				.as("an empty environment variable is one blank audience, which is none")
				.isInstanceOf(IllegalArgumentException.class);
		verify(context, never()).registerService(eq(JwtVerifier.class), any(JwtVerifier.class), any());
	}

	@Test
	void aCompleteConfigurationRegistersTheVerifier() throws Exception {
		BundleContext context = mock(BundleContext.class);
		NimbusJwtVerifier component = new NimbusJwtVerifier();

		component.activate(context, config(TestTokens.ISSUER, "http://idp.example/certs", TestTokens.AUDIENCE));

		verify(context).registerService(eq(JwtVerifier.class), same(component), any());
	}

	private static NimbusJwtVerifier.Config config(String issuer, String jwks, String audience) {
		NimbusJwtVerifier.Config config = mock(NimbusJwtVerifier.Config.class);
		when(config.issuer()).thenReturn(issuer);
		when(config.jwks_uri()).thenReturn(jwks);
		when(config.audiences()).thenReturn(new String[] { audience });
		when(config.scope_claim()).thenReturn("scope");
		when(config.subject_claim()).thenReturn("sub");
		when(config.clock_skew_seconds()).thenReturn(5);
		return config;
	}
}
