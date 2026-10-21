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

import org.eclipse.fennec.services.common.CallPrincipal;
import org.eclipse.fennec.services.security.jwt.JwtVerifier;
import org.eclipse.fennec.services.security.jwt.SecurityNames;
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

/**
 * The filter of mode {@code bearer-optional}: a token that is there must
 * verify, and a request without one goes on with no principal.
 *
 * <p>For applications in which every request is answered by the generic
 * dispatcher, which holds each operation to the contract's requirement —
 * {@code 401} for a guarded operation without a principal, {@code 403}
 * for a missing role. That is what lets a {@code @PermitAll} operation sit
 * next to guarded ones in one contract: the strict filter of mode
 * {@code bearer} would turn its anonymous callers away before routing.
 *
 * <p>Like the strict filter it exists only while a verifier does, so an
 * application of this mode is not deployed on a node without one.
 */
@Component(service = { ContainerRequestFilter.class, ContainerResponseFilter.class })
@JakartarsExtension
@JakartarsName(SecurityNames.OPTIONAL_BEARER_FILTER)
@JakartarsApplicationSelect("(" + SecurityNames.AUTH + "=" + SecurityNames.BEARER_OPTIONAL + ")")
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class OptionalBearerAuthFilter implements ContainerRequestFilter, ContainerResponseFilter {

	@Reference
	JwtVerifier verifier;

	@Override
	public void filter(ContainerRequestContext request) {
		BearerAuthFilter.authenticate(request, verifier, false);
	}

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) {
		CallPrincipal.clear();
	}
}
