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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.fennec.services.common.TokenSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;

import com.sun.net.httpserver.HttpServer;

/**
 * The client-credentials grant against a token endpoint that answers
 * like an identity provider does.
 */
class ClientCredentialsTokensTest {

	private HttpServer endpoint;

	private final List<String> authorizations = new ArrayList<>();

	private final List<String> bodies = new ArrayList<>();

	private final AtomicInteger issued = new AtomicInteger();

	private volatile int status = 200;

	private volatile long expiresIn = 300;

	private final MutableClock clock = new MutableClock();

	@BeforeEach
	void start() throws IOException {
		endpoint = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		endpoint.createContext("/token", exchange -> {
			authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
			bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			String answer = status == 200
					? "{\"access_token\":\"token-" + issued.incrementAndGet() + "\",\"token_type\":\"Bearer\",\"expires_in\":"
							+ expiresIn + "}"
					: "{\"error\":\"invalid_client\"}";
			byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
		endpoint.start();
	}

	@AfterEach
	void stop() {
		endpoint.stop(0);
	}

	@Test
	void theClientAuthenticatesWithBasicAndAsksForItsScope() {
		ClientCredentialsTokens tokens = configured("payments-node", "s3cr3t", "provider consumer");

		assertThat(tokens.token()).isEqualTo("token-1");
		assertThat(authorizations).containsExactly(
				"Basic " + Base64.getEncoder().encodeToString("payments-node:s3cr3t".getBytes(StandardCharsets.UTF_8)));
		assertThat(bodies).containsExactly("grant_type=client_credentials&scope=provider+consumer");
	}

	@Test
	void aTokenIsReusedUntilShortlyBeforeItExpires() {
		ClientCredentialsTokens tokens = configured("payments-node", "s3cr3t", "");

		assertThat(tokens.token()).isEqualTo("token-1");
		clock.advance(Duration.ofSeconds(300).minus(ClientCredentialsTokens.RENEW_BEFORE).minusSeconds(1));
		assertThat(tokens.token()).as("still well within its lifetime").isEqualTo("token-1");
		clock.advance(Duration.ofSeconds(1));
		assertThat(tokens.token()).as("renewed before it lapses").isEqualTo("token-2");
		assertThat(bodies).allSatisfy(body -> assertThat(body).isEqualTo("grant_type=client_credentials"));
	}

	@Test
	void aShortLivedTokenIsRenewedHalfWay() {
		expiresIn = 40;
		ClientCredentialsTokens tokens = configured("payments-node", "s3cr3t", "");

		tokens.token();
		clock.advance(Duration.ofSeconds(19));
		assertThat(tokens.token()).isEqualTo("token-1");
		clock.advance(Duration.ofSeconds(1));
		assertThat(tokens.token()).isEqualTo("token-2");
	}

	@Test
	void aRefusalFailsWithTheProvidersReasonAndNotTheSecret() {
		status = 401;
		ClientCredentialsTokens tokens = configured("payments-node", "s3cr3t", "");

		assertThatThrownBy(tokens::token)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("401")
				.hasMessageContaining("invalid_client")
				.hasMessageNotContaining("s3cr3t");
	}

	@Test
	void anUnreachableEndpointFailsTheCallWithItsAddress() {
		ClientCredentialsTokens tokens = new ClientCredentialsTokens(clock);
		tokens.configure(config("http://127.0.0.1:1/token", "payments-node", "s3cr3t", ""));

		assertThatThrownBy(tokens::token)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("127.0.0.1:1");
	}

	@Test
	void withoutATokenEndpointThereIsNoSupplier() {
		List<Object> registered = new ArrayList<>();

		new ClientCredentialsTokens(clock).activate(recording(registered), config("", "", "", ""));

		assertThat(registered).isEmpty();
	}

	@Test
	void anEndpointWithoutCredentialsIsRefusedNotHalfStarted() {
		List<Object> registered = new ArrayList<>();

		assertThatThrownBy(() -> new ClientCredentialsTokens(clock)
				.activate(recording(registered), config(uri(), "payments-node", "", "")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(registered).isEmpty();
	}

	@Test
	void aCompleteConfigurationRegistersTheSupplier() {
		List<Object> registered = new ArrayList<>();
		ClientCredentialsTokens tokens = new ClientCredentialsTokens(clock);

		tokens.activate(recording(registered), config(uri(), "payments-node", "s3cr3t", ""));

		assertThat(registered).containsExactly(TokenSupplier.class);
	}

	// ------------------------------------------------------------------

	private ClientCredentialsTokens configured(String client, String secret, String scope) {
		ClientCredentialsTokens tokens = new ClientCredentialsTokens(clock);
		tokens.configure(config(uri(), client, secret, scope));
		return tokens;
	}

	private String uri() {
		return "http://127.0.0.1:" + endpoint.getAddress().getPort() + "/token";
	}

	private static ClientCredentialsTokens.Config config(String tokenUri, String client, String secret, String scope) {
		return new ClientCredentialsTokens.Config() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return ClientCredentialsTokens.Config.class;
			}

			@Override
			public String token_uri() {
				return tokenUri;
			}

			@Override
			public String client_id() {
				return client;
			}

			@Override
			public String client_secret() {
				return secret;
			}

			@Override
			public String scope() {
				return scope;
			}
		};
	}

	/** A BundleContext that only records which types were registered. */
	private static BundleContext recording(List<Object> registered) {
		return (BundleContext) Proxy.newProxyInstance(BundleContext.class.getClassLoader(),
				new Class<?>[] { BundleContext.class }, (proxy, method, args) -> {
					if ("registerService".equals(method.getName())) {
						registered.add(args[0]);
					}
					return null;
				});
	}

	private static final class MutableClock extends Clock {

		private Instant now = Instant.parse("2026-10-21T10:00:00Z");

		void advance(Duration by) {
			now = now.plus(by);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
	}
}
