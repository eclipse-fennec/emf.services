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
import org.eclipse.fennec.services.security.jwt.SecurityNames;
import org.osgi.service.component.annotations.Component;
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
 * The filter of mode {@code none}: no credential is asked for, and the
 * call carries no principal.
 *
 * <p>It exists so that turning authentication off is a stated choice
 * with the same shape as turning it on — an application says
 * {@code fennec.auth=none} and requires this filter by name, exactly as
 * a bearer application requires the bearer filter — and not the
 * absence of a property, which a forgotten line produces just as well.
 *
 * <p>What it does is the half of the bearer filter that does not depend
 * on a credential: it clears {@link CallPrincipal} on the way in and
 * out, so a pooled thread that served an authenticated application a
 * moment ago does not lend that caller's identity to this one.
 */
@Component(service = { ContainerRequestFilter.class, ContainerResponseFilter.class })
@JakartarsExtension
@JakartarsName(SecurityNames.NO_AUTH_FILTER)
@JakartarsApplicationSelect("(" + SecurityNames.AUTH + "=" + SecurityNames.NONE + ")")
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class NoAuthFilter implements ContainerRequestFilter, ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext request) {
		CallPrincipal.clear();
	}

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) {
		CallPrincipal.clear();
	}
}
