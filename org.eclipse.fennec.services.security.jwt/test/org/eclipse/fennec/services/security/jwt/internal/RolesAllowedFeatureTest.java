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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Set;

import org.eclipse.fennec.services.security.jwt.SecurityNames;
import org.eclipse.fennec.services.security.jwt.VerifiedPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * The annotations of a resource method, enforced the way the model means
 * them — the same answers the generic dispatcher gives.
 */
class RolesAllowedFeatureTest {

	@RolesAllowed("**")
	static class Resource {

		public String anyCaller() {
			return "";
		}

		@RolesAllowed({ "person-reader", "auditor" })
		public String both() {
			return "";
		}

		@PermitAll
		public String open() {
			return "";
		}

		@DenyAll
		public String nobody() {
			return "";
		}
	}

	static class Plain {

		public String unannotated() {
			return "";
		}
	}

	private ContainerRequestContext request;

	@BeforeEach
	void setUp() {
		request = mock(ContainerRequestContext.class);
		UriInfo uri = mock(UriInfo.class);
		when(uri.getPath()).thenReturn("/directory/persons");
		when(request.getUriInfo()).thenReturn(uri);
		when(request.getMethod()).thenReturn("GET");
	}

	@Test
	void anUnannotatedResourceIsLeftAlone() throws Exception {
		assertThat(RolesAllowedFeature.filterFor(method(Plain.class, "unannotated"), Plain.class)).isNull();
	}

	@Test
	void permitAllOnTheMethodLiftsTheClassesRequirement() throws Exception {
		assertThat(RolesAllowedFeature.filterFor(method(Resource.class, "open"), Resource.class)).isNull();
	}

	@Test
	void theClassesRequirementIsTheDefault() throws Exception {
		ContainerRequestFilter check = RolesAllowedFeature.filterFor(method(Resource.class, "anyCaller"), Resource.class);

		assertThat(((RolesAllowedFeature.Check) check).scopes()).as("** names no scope").isEmpty();
	}

	@Test
	void withoutAPrincipalTheCallIs401() throws Exception {
		check("anyCaller").filter(request);

		assertThat(refusal().getStatus()).isEqualTo(401);
		assertThat(refusal().getHeaderString("WWW-Authenticate")).isEqualTo("Bearer");
	}

	@Test
	void anyVerifiedCallerPassesTheStarStar() throws Exception {
		holding();

		check("anyCaller").filter(request);

		verify(request, never()).abortWith(any());
	}

	@Test
	void everyRoleIsRequiredNotAnyOfThem() throws Exception {
		// Jakarta reads {a, b} as "either"; the model, and so this filter,
		// as "both" — what the contract said is what is enforced.
		holding("person-reader");

		check("both").filter(request);

		assertThat(refusal().getStatus()).isEqualTo(403);
		assertThat(refusal().getEntity()).isEqualTo("/directory/persons needs auditor");
	}

	@Test
	void aCallerHoldingAllOfThemPasses() throws Exception {
		holding("person-reader", "auditor");

		check("both").filter(request);

		verify(request, never()).abortWith(any());
	}

	@Test
	void denyAllIsRefusedToEverybody() throws Exception {
		holding("person-reader", "auditor");

		check("nobody").filter(request);

		assertThat(refusal().getStatus()).isEqualTo(403);
	}

	private ContainerRequestFilter check(String name) throws Exception {
		return RolesAllowedFeature.filterFor(method(Resource.class, name), Resource.class);
	}

	private void holding(String... scopes) {
		when(request.getProperty(SecurityNames.REQUEST_PRINCIPAL))
				.thenReturn(new VerifiedPrincipal("java-client", "https://issuer.test", Set.of(scopes)));
	}

	private Response refusal() {
		ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
		verify(request).abortWith(response.capture());
		return response.getValue();
	}

	private static Method method(Class<?> type, String name) throws NoSuchMethodException {
		return type.getMethod(name);
	}
}
