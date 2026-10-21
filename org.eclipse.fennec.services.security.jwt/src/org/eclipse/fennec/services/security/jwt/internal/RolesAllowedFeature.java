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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.fennec.services.security.jwt.SecurityNames;
import org.eclipse.fennec.services.security.jwt.VerifiedPrincipal;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsExtension;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;

import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/**
 * Holds a resource method to the Jakarta security annotations it
 * carries — the ones the m2t resource template writes from the
 * contract, and the ones a hand-written resource is annotated with
 * (SECURITY.md §4).
 *
 * <p>Read the way the model means them, which is what the generic
 * dispatcher enforces too: {@code @RolesAllowed} names scopes that are
 * all required, not alternatives; {@code "**"} is any verified caller;
 * {@code @PermitAll} lets the call through; the method's annotation
 * overrides its class's. A call without a principal is {@code 401}, one
 * missing scopes is {@code 403} naming them. {@code @DenyAll} is
 * {@code 403} for everybody.
 *
 * <p>Only on applications of a bearer mode: under {@code none} the
 * deployment has chosen to run open, and the annotations are a
 * statement nobody is asked to check. Resources that carry no
 * annotation — the generic dispatcher's, the broker's — are left alone.
 */
@Component(service = DynamicFeature.class)
@JakartarsExtension
@JakartarsName(SecurityNames.ROLES_FILTER)
@JakartarsApplicationSelect("(" + SecurityNames.AUTH + "=" + SecurityNames.BEARER + "*)")
public class RolesAllowedFeature implements DynamicFeature {

	private static final Logger LOG = Logger.getLogger(RolesAllowedFeature.class.getName());

	/** The role Jakarta Security reserves for "any authenticated caller". */
	static final String ANY_AUTHENTICATED = "**";

	@Override
	public void configure(ResourceInfo resource, FeatureContext context) {
		ContainerRequestFilter filter = filterFor(resource.getResourceMethod(), resource.getResourceClass());
		if (filter != null) {
			context.register(filter);
		}
	}

	/** The check a method needs, or {@code null} when it carries no security annotation. */
	static ContainerRequestFilter filterFor(Method method, Class<?> resourceClass) {
		ContainerRequestFilter own = method == null ? null : filterOf(method);
		if (own != null || (method != null && stated(method))) {
			return own;
		}
		return resourceClass == null ? null : filterOf(resourceClass);
	}

	private static boolean stated(AnnotatedElement element) {
		return element.isAnnotationPresent(PermitAll.class) || element.isAnnotationPresent(RolesAllowed.class)
				|| element.isAnnotationPresent(DenyAll.class);
	}

	private static ContainerRequestFilter filterOf(AnnotatedElement element) {
		if (element.isAnnotationPresent(DenyAll.class)) {
			return new Check(null);
		}
		if (element.isAnnotationPresent(PermitAll.class)) {
			return null;
		}
		RolesAllowed roles = element.getAnnotation(RolesAllowed.class);
		if (roles == null) {
			return null;
		}
		return new Check(Arrays.stream(roles.value()).filter(role -> !ANY_AUTHENTICATED.equals(role)).toList());
	}

	/** One method's check: the scopes it needs, or {@code null} for nobody. */
	@Priority(Priorities.AUTHORIZATION)
	static final class Check implements ContainerRequestFilter {

		private final List<String> scopes;

		Check(List<String> scopes) {
			this.scopes = scopes;
		}

		@Override
		public void filter(ContainerRequestContext request) {
			if (scopes == null) {
				refuse(request, Response.status(Response.Status.FORBIDDEN).build(), "denied to everybody");
				return;
			}
			// Put there by the bearer filter of either mode, after it
			// verified the token; under bearer-optional a call without one
			// arrives here with none.
			if (!(request.getProperty(SecurityNames.REQUEST_PRINCIPAL) instanceof VerifiedPrincipal principal)) {
				refuse(request, Response.status(Response.Status.UNAUTHORIZED)
						.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").build(), "no principal");
				return;
			}
			List<String> missing = scopes.stream().filter(scope -> !principal.hasScope(scope)).toList();
			if (!missing.isEmpty()) {
				refuse(request, Response.status(Response.Status.FORBIDDEN)
						.entity(request.getUriInfo().getPath() + " needs " + String.join(", ", missing)).build(),
						principal.subject() + " lacks " + missing);
			}
		}

		List<String> scopes() {
			return scopes;
		}

		private static void refuse(ContainerRequestContext request, Response answer, String reason) {
			LOG.info("[DDSR] " + answer.getStatus() + " " + request.getMethod() + " " + request.getUriInfo().getPath()
					+ ": " + reason);
			request.abortWith(answer);
		}
	}
}
