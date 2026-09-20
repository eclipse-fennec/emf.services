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

package org.eclipse.fennec.services.broker.rest.internal;

import org.eclipse.fennec.services.common.CallOrigin;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsExtension;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

/**
 * Takes the {@code X-DDSR-Origin} header off an incoming request and
 * binds it for as long as the request is served (#125).
 *
 * <p>One filter rather than a parameter on every operation, for the
 * reason {@link CallOrigin} spells out: the origin belongs to the call,
 * not to any contract, and making it a contract parameter would move
 * the fingerprint of every broker contract for something no contract is
 * about.
 *
 * <p>This one covers the broker's HAND-WRITTEN resources — lookup,
 * events, consumers, registry — which live in the default JAX-RS
 * application. The catalog and publish calls do NOT come through here:
 * they are answered by the generic distribution, which registers one
 * named application per contract with its own providers, so a
 * whiteboard extension is never asked. That half binds the origin in
 * {@code RestDispatcher}, and the two meet in {@link CallOrigin}.
 */
@Component(service = { ContainerRequestFilter.class, ContainerResponseFilter.class })
@JakartarsExtension
@JakartarsName("ddsr-origin")
// Every application, not just the default one. The generic distribution
// registers ONE named JAX-RS application per served contract with its
// own singletons (#84), and an extension that does not select them is
// simply never asked — which is how the first version of this filter
// managed to be green in its unit tests and do nothing over HTTP.
@JakartarsApplicationSelect("(osgi.jakartars.name=*)")
public class OriginFilter implements ContainerRequestFilter, ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext request) {
		// Unconditionally, including the null case: a request without an
		// origin must clear what the previous request left on this
		// thread, or a pooled thread would attribute an anonymous call to
		// whoever used it last.
		CallOrigin.set(ClientOrigin.parse(request.getHeaderString(ClientOrigin.HEADER)));
	}

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) {
		CallOrigin.clear();
	}
}
