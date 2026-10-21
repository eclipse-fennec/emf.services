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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.common.CallPrincipal;
import org.eclipse.fennec.services.security.jwt.SecurityNames;
import org.eclipse.fennec.services.security.jwt.TestTokens;
import org.eclipse.fennec.services.security.jwt.VerifiedPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

class BearerAuthFilterTest {

	private TestTokens tokens;
	private BearerAuthFilter filter;
	private ContainerRequestContext request;

	@BeforeEach
	void setUp() throws Exception {
		tokens = new TestTokens();
		NimbusJwtVerifier verifier = new NimbusJwtVerifier();
		verifier.init(tokens.keys(), TestTokens.ISSUER, Set.of(TestTokens.AUDIENCE), "scope", 5);
		filter = new BearerAuthFilter();
		filter.verifier = verifier;
		request = mock(ContainerRequestContext.class);
		UriInfo uri = mock(UriInfo.class);
		when(uri.getPath()).thenReturn("/implementations");
		when(request.getUriInfo()).thenReturn(uri);
		when(request.getMethod()).thenReturn("POST");
	}

	@AfterEach
	void clearThread() {
		CallPrincipal.clear();
	}

	@Test
	void noTokenIs401WithoutABodyAndWithTheChallenge() {
		filter.filter(request);

		ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
		verify(request).abortWith(response.capture());
		assertThat(response.getValue().getStatus()).isEqualTo(401);
		assertThat(response.getValue().getHeaderString("WWW-Authenticate")).startsWith("Bearer");
		assertThat(response.getValue().hasEntity()).as("the reason is for the log, not the caller").isFalse();
		assertThat(CallPrincipal.current()).isNull();
	}

	@Test
	void aRejectedTokenIs401() throws Exception {
		String forged = tokens.token(tokens.otherKey, "node", null, null,
				new java.util.Date(System.currentTimeMillis() + 60_000), TestTokens.ISSUER, TestTokens.AUDIENCE);
		when(request.getHeaderString("Authorization")).thenReturn("Bearer " + forged);

		filter.filter(request);

		ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
		verify(request).abortWith(response.capture());
		assertThat(response.getValue().getStatus()).isEqualTo(401);
		assertThat(CallPrincipal.current()).isNull();
	}

	@Test
	void aVerifiedTokenReachesAllThreeReaders() throws Exception {
		when(request.getHeaderString("Authorization"))
				.thenReturn("Bearer " + tokens.token("payments-node", "provider", null));

		filter.filter(request);

		verify(request, never()).abortWith(any());
		// the next filter: the request property
		verify(request).setProperty(eq(SecurityNames.REQUEST_PRINCIPAL), any(VerifiedPrincipal.class));
		// the resource: the security context
		ArgumentCaptor<SecurityContext> security = ArgumentCaptor.forClass(SecurityContext.class);
		verify(request).setSecurityContext(security.capture());
		assertThat(security.getValue().getUserPrincipal().getName()).isEqualTo("payments-node");
		assertThat(security.getValue().isUserInRole("provider")).isTrue();
		assertThat(security.getValue().isUserInRole("admin")).isFalse();
		assertThat(security.getValue().getAuthenticationScheme()).isEqualTo("Bearer");
		// the broker core: the thread
		assertThat(CallPrincipal.subject()).isEqualTo("payments-node");
		assertThat(CallPrincipal.current().getScopes()).containsExactly("provider");
	}

	@Test
	void theThreadIsClearedWhenTheResponseLeaves() throws Exception {
		when(request.getHeaderString("Authorization")).thenReturn("Bearer " + tokens.token("node", null, null));
		filter.filter(request);
		assertThat(CallPrincipal.current()).isNotNull();

		filter.filter(request, mock(ContainerResponseContext.class));

		assertThat(CallPrincipal.current()).as("a pooled thread lends nobody's identity to the next call").isNull();
	}

	@Test
	void theBearerPrefixIsCaseInsensitiveAndAnEmptyTokenIsNoToken() throws Exception {
		when(request.getHeaderString("Authorization")).thenReturn("bearer " + tokens.token("node", null, null));
		filter.filter(request);
		verify(request, never()).abortWith(any());

		ContainerRequestContext empty = mock(ContainerRequestContext.class);
		UriInfo uri = request.getUriInfo();
		when(empty.getUriInfo()).thenReturn(uri);
		when(empty.getHeaderString("Authorization")).thenReturn("Bearer ");
		filter.filter(empty);
		verify(empty).abortWith(any());
	}

	// ------------------------------------------------------------------
	// mode bearer-optional

	@Test
	void optionalModeLetsARequestWithoutATokenThroughWithNoPrincipal() {
		CallPrincipal.set(ServicesFactory.eINSTANCE.createPrincipal());

		optional().filter(request);

		verify(request, never()).abortWith(any());
		assertThat(CallPrincipal.current()).as("nobody is lent the previous caller's identity").isNull();
	}

	@Test
	void optionalModeStillTurnsABadTokenAway() throws Exception {
		String forged = tokens.token(tokens.otherKey, "node", null, null,
				new java.util.Date(System.currentTimeMillis() + 60_000), TestTokens.ISSUER, TestTokens.AUDIENCE);
		when(request.getHeaderString("Authorization")).thenReturn("Bearer " + forged);

		optional().filter(request);

		ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
		verify(request).abortWith(response.capture());
		assertThat(response.getValue().getStatus()).as("a bad credential is never the same as none").isEqualTo(401);
	}

	@Test
	void optionalModeVerifiesAGoodToken() throws Exception {
		when(request.getHeaderString("Authorization"))
				.thenReturn("Bearer " + tokens.token("payments-node", "provider", null));

		optional().filter(request);

		verify(request, never()).abortWith(any());
		assertThat(CallPrincipal.subject()).isEqualTo("payments-node");
	}

	private OptionalBearerAuthFilter optional() {
		OptionalBearerAuthFilter optional = new OptionalBearerAuthFilter();
		optional.verifier = filter.verifier;
		return optional;
	}
}
