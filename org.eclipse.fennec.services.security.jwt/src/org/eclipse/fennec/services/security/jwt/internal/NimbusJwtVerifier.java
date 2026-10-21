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

import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.fennec.services.security.jwt.JwtVerifier;
import org.eclipse.fennec.services.security.jwt.VerifiedPrincipal;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

/**
 * The verifier on Nimbus JOSE+JWT.
 *
 * <p>RS256 is pinned in the key selector, so a token claiming
 * {@code none} or an HMAC algorithm fails before any key is looked at.
 * The issuer is matched exactly, the audience must contain one of the
 * configured values, {@code sub}, {@code exp} and {@code iat} are
 * required, and the lifetime is checked with a clock skew.
 *
 * <p>Configured the way a {@code BearerTokenScheme} of the model is
 * written, and only by configuration ({@code ConfigurationPolicy.REQUIRE}):
 * a node without one has no verifier, and an application of mode bearer,
 * which requires the bearer filter, is not deployed — the fail-closed half
 * of the design.
 *
 * <p><strong>A configuration without an issuer is no verifier
 * either.</strong> The component registers its service itself, and only
 * when an issuer is configured. That lets a deployment carry the verifier's
 * configuration unconditionally, filled from the environment
 * ({@code DDSR_JWT_ISSUER} and friends): where the variables are empty —
 * a broker in mode none — there is simply no verifier, said once at INFO,
 * instead of a component failing to activate on every start.
 */
@Component(name = NimbusJwtVerifier.PID, configurationPolicy = ConfigurationPolicy.REQUIRE, service = {},
		immediate = true)
@Designate(ocd = NimbusJwtVerifier.Config.class)
public class NimbusJwtVerifier implements JwtVerifier {

	public static final String PID = "org.eclipse.fennec.services.security.jwt";

	@ObjectClassDefinition(name = "Fennec Services bearer token verifier",
			description = "Verifies bearer tokens of one issuer — the shape of a BearerTokenScheme in the model.")
	public @interface Config {
		@AttributeDefinition(name = "Issuer",
				description = "The expected iss claim, e.g. http://localhost:9080/auth/realms/dim. "
						+ "Empty means no verifier on this node.")
		String issuer() default "";

		@AttributeDefinition(name = "JWKS URI",
				description = "Where the signing keys are fetched; may be an internal address, e.g. "
						+ "http://keycloak:8080/auth/realms/dim/protocol/openid-connect/certs.")
		String jwks_uri() default "";

		@AttributeDefinition(name = "Audiences",
				description = "Accepted aud values; a token must carry at least one of them.")
		String[] audiences() default {};

		@AttributeDefinition(name = "Scope claim",
				description = "Dot path of the claim that carries the scopes: 'scope' (OAuth, one "
						+ "space-separated string) or e.g. 'realm_access.roles' (Keycloak, a list).")
		String scope_claim() default "scope";

		@AttributeDefinition(name = "Subject claim",
				description = "The claim that names the caller — what a registration's owner and a "
						+ "session's consumerId are held against. 'sub' is the standard; a client-credentials "
						+ "token from Keycloak carries the service account's generated id there, and 'azp' — "
						+ "the client id — is the stable, readable name of the node.")
		String subject_claim() default "sub";

		@AttributeDefinition(name = "Clock skew (seconds)",
				description = "Tolerance on exp, nbf and iat.")
		int clock_skew_seconds() default 30;
	}

	private static final Logger LOG = Logger.getLogger(NimbusJwtVerifier.class.getName());

	private DefaultJWTProcessor<SecurityContext> processor;
	private String issuer;
	private String[] scopePath;
	private String subjectClaim = "sub";

	private ServiceRegistration<JwtVerifier> registration;

	@Activate
	void activate(BundleContext context, Config config) throws MalformedURLException {
		if (config.issuer().isBlank()) {
			LOG.info("[DDSR] no issuer configured — no bearer verifier on this node");
			return;
		}
		if (config.jwks_uri().isBlank() || audiencesOf(config).isEmpty()) {
			// An issuer without keys or audiences is a half-done
			// configuration. Refusing to start says so; a verifier that
			// accepted every audience would not.
			throw new IllegalArgumentException("issuer " + config.issuer()
					+ " is configured, but jwks.uri and audiences are needed as well");
		}
		JWKSource<SecurityContext> keys = JWKSourceBuilder.create(URI.create(config.jwks_uri()).toURL()).build();
		init(keys, config.issuer(), audiencesOf(config), config.scope_claim(), config.subject_claim(),
				config.clock_skew_seconds());
		registration = context.registerService(JwtVerifier.class, this, null);
		LOG.info("[DDSR] bearer verifier ready: issuer=" + config.issuer()
				+ " audiences=" + audiencesOf(config) + " jwks=" + config.jwks_uri()
				+ " subject=" + config.subject_claim());
	}

	@Deactivate
	void deactivate() {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
	}

	/** Blank entries dropped: an empty environment variable yields one empty audience. */
	private static Set<String> audiencesOf(Config config) {
		Set<String> audiences = new LinkedHashSet<>();
		for (String audience : config.audiences()) {
			if (audience != null && !audience.isBlank()) {
				audiences.add(audience.trim());
			}
		}
		return audiences;
	}

	/** Package-visible so a test can hand in keys without a JWKS endpoint. */
	void init(JWKSource<SecurityContext> keys, String issuer, Set<String> audiences, String scopeClaim,
			int skewSeconds) {
		init(keys, issuer, audiences, scopeClaim, "sub", skewSeconds);
	}

	void init(JWKSource<SecurityContext> keys, String issuer, Set<String> audiences, String scopeClaim,
			String subjectClaim, int skewSeconds) {
		this.issuer = issuer;
		this.scopePath = scopeClaim.split("\\.");
		this.subjectClaim = subjectClaim == null || subjectClaim.isBlank() ? "sub" : subjectClaim;
		processor = new DefaultJWTProcessor<>();
		processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys));
		JWTClaimsSet exact = new JWTClaimsSet.Builder().issuer(issuer).build();
		// Whatever names the caller has to be there: a token without it
		// would verify and then own nothing, or everything.
		Set<String> required = new HashSet<>(List.of(this.subjectClaim, "exp", "iat"));
		// Nimbus asks the audience set whether it contains null (its
		// spelling of "no audience accepted"); an immutable Set.of throws
		// on that question, so it gets a copy that does not.
		DefaultJWTClaimsVerifier<SecurityContext> claims = new DefaultJWTClaimsVerifier<>(new HashSet<>(audiences),
				exact, required, new HashSet<>());
		claims.setMaxClockSkew(skewSeconds);
		processor.setJWTClaimsSetVerifier(claims);
	}

	@Override
	public VerifiedPrincipal verify(String compactJwt) throws TokenRejectedException {
		JWTClaimsSet claims;
		try {
			claims = processor.process(compactJwt, null);
		} catch (BadJOSEException | JOSEException | ParseException e) {
			throw new TokenRejectedException(e.getMessage(), e);
		}
		Object subject = claims.getClaim(subjectClaim);
		if (!(subject instanceof String name) || name.isBlank()) {
			throw new TokenRejectedException("the subject claim '" + subjectClaim + "' is not a name");
		}
		return new VerifiedPrincipal(name, issuer, scopesOf(claims));
	}

	/**
	 * The scopes under the configured path: a list is taken as is, a
	 * string is split on whitespace (OAuth {@code scope}), anything
	 * else is no scope at all.
	 */
	private Set<String> scopesOf(JWTClaimsSet claims) {
		Object node = claims.getClaims();
		for (String segment : scopePath) {
			if (node instanceof Map<?, ?> map) {
				node = map.get(segment);
			} else {
				return Set.of();
			}
		}
		Set<String> scopes = new LinkedHashSet<>();
		if (node instanceof List<?> list) {
			for (Object element : list) {
				if (element instanceof String s && !s.isBlank()) {
					scopes.add(s);
				}
			}
		} else if (node instanceof String s) {
			for (String element : s.trim().split("\\s+")) {
				if (!element.isBlank()) {
					scopes.add(element);
				}
			}
		}
		return scopes;
	}
}
