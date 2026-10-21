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
package org.eclipse.fennec.services.security.oauth.internal;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.logging.Logger;

import org.eclipse.fennec.services.common.TokenSupplier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/**
 * A node's bearer token, by the OAuth 2.0 client-credentials grant
 * (RFC 6749 §4.4) — the way SECURITY.md §1 says a peer is identified:
 * as a client with a service account, not as a user.
 *
 * <p>The token is fetched on first use, kept, and fetched again shortly
 * before it expires; a call never waits for a token it could have
 * reused. The client authenticates with HTTP Basic, which every
 * identity provider accepts and which keeps the secret out of a body
 * that might be logged.
 *
 * <p>Like the verifier it pairs with, it registers its service itself
 * and only when a token endpoint is configured. A node's configuration
 * can therefore carry it unconditionally, filled from the environment
 * ({@code DDSR_OAUTH_*}): against an open broker the variables stay
 * empty and the node shows no token.
 */
@Component(name = ClientCredentialsTokens.PID, configurationPolicy = ConfigurationPolicy.REQUIRE, service = {},
		immediate = true)
@Designate(ocd = ClientCredentialsTokens.Config.class)
public class ClientCredentialsTokens implements TokenSupplier {

	public static final String PID = "org.eclipse.fennec.services.security.oauth";

	@ObjectClassDefinition(name = "Fennec Services OAuth client credentials",
			description = "Obtains the bearer token a node shows the broker.")
	public @interface Config {

		@AttributeDefinition(name = "Token URI",
				description = "The identity provider's token endpoint, e.g. "
						+ "http://keycloak:8080/realms/fennec/protocol/openid-connect/token. Empty means "
						+ "this node shows no token.")
		String token_uri() default "";

		@AttributeDefinition(name = "Client id", description = "The node's client id — its identity.")
		String client_id() default "";

		@AttributeDefinition(name = "Client secret", type = AttributeType.PASSWORD,
				description = "The client's secret. Never logged.")
		String client_secret() default "";

		@AttributeDefinition(name = "Scope",
				description = "Space-separated scopes to ask for; empty asks for the client's defaults.")
		String scope() default "";
	}

	/** How long before its expiry a token is replaced, so no call leaves with one about to lapse. */
	static final Duration RENEW_BEFORE = Duration.ofSeconds(30);

	private static final Logger LOG = Logger.getLogger(ClientCredentialsTokens.class.getName());

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	private final Clock clock;

	private URI tokenUri;

	private String authorization;

	private String scope;

	private String current;

	private Instant renewAt = Instant.MIN;

	private ServiceRegistration<TokenSupplier> registration;

	public ClientCredentialsTokens() {
		this(Clock.systemUTC());
	}

	ClientCredentialsTokens(Clock clock) {
		this.clock = clock;
	}

	@Activate
	void activate(BundleContext context, Config config) {
		if (config.token_uri().isBlank()) {
			LOG.info("[DDSR] no token endpoint configured — this node shows no bearer token");
			return;
		}
		configure(config);
		registration = context.registerService(TokenSupplier.class, this, null);
		LOG.info("[DDSR] bearer tokens for client '" + config.client_id() + "' from " + tokenUri);
	}

	/** Package-visible so a test can configure without a framework. */
	void configure(Config config) {
		if (config.client_id().isBlank() || config.client_secret().isBlank()) {
			throw new IllegalArgumentException("token endpoint " + config.token_uri()
					+ " is configured, but client.id and client.secret are needed as well");
		}
		this.tokenUri = URI.create(config.token_uri());
		this.authorization = "Basic " + Base64.getEncoder().encodeToString(
				(form(config.client_id()) + ":" + form(config.client_secret())).getBytes(StandardCharsets.UTF_8));
		this.scope = config.scope().trim();
	}

	@Deactivate
	void deactivate() {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
	}

	@Override
	public synchronized String token() {
		if (current == null || !clock.instant().isBefore(renewAt)) {
			fetch();
		}
		return current;
	}

	private void fetch() {
		String body = "grant_type=client_credentials" + (scope.isEmpty() ? "" : "&scope=" + form(scope));
		HttpRequest request = HttpRequest.newBuilder(tokenUri)
				.timeout(Duration.ofSeconds(10))
				.header("Authorization", authorization)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> response;
		try {
			response = http.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException e) {
			throw new IllegalStateException("no bearer token: " + tokenUri + " is not reachable (" + e + ")", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("no bearer token: interrupted while asking " + tokenUri, e);
		}
		if (response.statusCode() != 200) {
			// The body of a refusal names the reason (invalid_client,
			// unauthorized_client) and never the secret.
			throw new IllegalStateException("no bearer token: " + tokenUri + " answered " + response.statusCode()
					+ " " + response.body());
		}
		JsonObject answer;
		try (JsonReader json = Json.createReader(new StringReader(response.body()))) {
			answer = json.readObject();
		}
		String token = answer.getString("access_token", null);
		if (token == null || token.isBlank()) {
			throw new IllegalStateException("no bearer token: " + tokenUri + " answered without an access_token");
		}
		long lifetime = answer.getJsonNumber("expires_in") == null ? 60 : answer.getJsonNumber("expires_in").longValue();
		Instant now = clock.instant();
		// Renew RENEW_BEFORE ahead, or half-way for a short-lived token.
		Duration margin = lifetime > 2 * RENEW_BEFORE.toSeconds() ? RENEW_BEFORE : Duration.ofSeconds(lifetime / 2);
		this.current = token;
		this.renewAt = now.plusSeconds(lifetime).minus(margin);
		LOG.fine(() -> "[DDSR] bearer token renewed, valid for " + lifetime + " s");
	}

	private static String form(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
