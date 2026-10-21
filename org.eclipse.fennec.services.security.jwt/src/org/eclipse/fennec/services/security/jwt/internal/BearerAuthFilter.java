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

import java.security.Principal;
import java.util.logging.Logger;

import org.eclipse.fennec.services.common.CallPrincipal;
import org.eclipse.fennec.services.security.jwt.JwtVerifier;
import org.eclipse.fennec.services.security.jwt.SecurityNames;
import org.eclipse.fennec.services.security.jwt.VerifiedPrincipal;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsExtension;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * The bearer filter: every request on an application of mode
 * {@code fennec.auth=bearer} carries a token the verifier accepts, or
 * it is {@code 401} before matching.
 *
 * <p>A verified principal is handed on three ways, for three readers:
 * as the request property {@link SecurityNames#REQUEST_PRINCIPAL} for
 * other filters, as the {@link SecurityContext} for resources, and on
 * the thread in {@link CallPrincipal} for code that knows nothing of
 * JAX-RS — the broker core, which has to say "only your own
 * registration". The response filter clears the thread, so a pooled
 * thread never lends one caller's identity to the next.
 *
 * <p>The filter exists only while a verifier does: the reference is
 * mandatory and static. A node without a configured verifier has no
 * filter, and an application of mode bearer, which requires it by
 * name, is not deployed — the whiteboard does the fail-closed part.
 */
@Component(service = { ContainerRequestFilter.class, ContainerResponseFilter.class })
@JakartarsExtension
@JakartarsName(SecurityNames.BEARER_FILTER)
@JakartarsApplicationSelect("(" + SecurityNames.AUTH + "=" + SecurityNames.BEARER + ")")
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class BearerAuthFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static final Logger LOG = Logger.getLogger(BearerAuthFilter.class.getName());
	private static final String BEARER = "Bearer ";

	@Reference
	JwtVerifier verifier;

	@Override
	public void filter(ContainerRequestContext request) {
		authenticate(request, verifier, true);
	}

	/**
	 * The one verification both bearer modes share.
	 *
	 * @param required whether a request without a token is turned away
	 *        here ({@code bearer}), or passed on without a principal for
	 *        the dispatcher to decide per operation ({@code bearer-optional}).
	 *        A token that is there and does not verify is turned away
	 *        either way: a bad credential is never the same as none.
	 */
	static void authenticate(ContainerRequestContext request, JwtVerifier verifier, boolean required) {
		// Unconditionally first: whatever the previous request left on
		// this thread must not survive into this one.
		CallPrincipal.clear();
		String header = request.getHeaderString(HttpHeaders.AUTHORIZATION);
		if (header == null) {
			if (required) {
				reject(request, "no bearer token");
			}
			return;
		}
		if (!header.regionMatches(true, 0, BEARER, 0, BEARER.length()) || header.length() <= BEARER.length()) {
			reject(request, "no bearer token");
			return;
		}
		VerifiedPrincipal principal;
		try {
			principal = verifier.verify(header.substring(BEARER.length()).trim());
		} catch (JwtVerifier.TokenRejectedException e) {
			reject(request, e.getMessage());
			return;
		}
		boolean secure = request.getSecurityContext() != null && request.getSecurityContext().isSecure();
		request.setProperty(SecurityNames.REQUEST_PRINCIPAL, principal);
		request.setSecurityContext(new BearerSecurityContext(principal, secure));
		CallPrincipal.set(principal.model());
	}

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) {
		CallPrincipal.clear();
	}

	/** 401 without a body: the reason goes to the log, never to the caller. */
	private static void reject(ContainerRequestContext request, String reason) {
		LOG.info("[DDSR] 401 " + request.getMethod() + " " + request.getUriInfo().getPath() + ": " + reason);
		request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
				.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
				.build());
	}

	/** What a resource sees: the subject as the user principal, scopes as roles. */
	static final class BearerSecurityContext implements SecurityContext {
		private final VerifiedPrincipal principal;
		private final boolean secure;

		BearerSecurityContext(VerifiedPrincipal principal, boolean secure) {
			this.principal = principal;
			this.secure = secure;
		}

		@Override
		public Principal getUserPrincipal() {
			return principal::subject;
		}

		@Override
		public boolean isUserInRole(String role) {
			return principal.hasScope(role);
		}

		@Override
		public boolean isSecure() {
			return secure;
		}

		@Override
		public String getAuthenticationScheme() {
			return "Bearer";
		}
	}
}
