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

package org.eclipse.fennec.services.telemetry.rest;

import java.lang.reflect.Method;

import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsExtension;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

/**
 * The server half of a trace for a resource somebody wrote by hand
 * (#126).
 *
 * <p>A contract served by the generic REST distribution is traced by
 * its dispatcher — one place every dispatched call passes. A
 * hand-written {@code @JakartarsResource} has no such place, and
 * without one the provider's end of a call starts a trace of its own
 * while the caller's trace stops at the wire.
 *
 * <p>This is that place: a whiteboard extension in the default
 * application, where a plain resource lands. It deliberately does not
 * reach the generic distribution's applications — those are named and
 * bring their own providers, so a call is traced once wherever it is
 * served.
 *
 * <p><strong>Post-match, on purpose.</strong> A pre-matching filter
 * knows the path and not the operation, and a span named after a path
 * with an id in it is a name nobody can group by. Here the matched
 * method is known and the headers are still untouched.
 */
@Component(service = { ContainerRequestFilter.class, ContainerResponseFilter.class })
@JakartarsExtension
@JakartarsName("fennec-request-tracing")
public class TracedRequests implements ContainerRequestFilter, ContainerResponseFilter {

	/** Where the span of a request waits for its response. */
	private static final String SPAN = "org.eclipse.fennec.services.telemetry.rest.span";

	@Context
	private ResourceInfo resource;

	/**
	 * Whoever is watching calls, if anyone is. Optional and dynamic: a
	 * deployment without telemetry installs nothing, and this extension
	 * then costs a property lookup per request.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile CallTracer tracer;

	@Override
	public void filter(ContainerRequestContext request) {
		CallTracer bound = tracer;
		if (bound == null || isEventStream(request)) {
			return;
		}
		CallSpan span = bound.serving(operationOf(request),
				TraceCarrier.reading(request.getHeaders()::getFirst));
		span.attribute("rpc.system", "fennec.services")
				.attribute("http.request.method", request.getMethod())
				.attribute("url.path", request.getUriInfo().getPath())
				.attribute("fennec.flavor", "REST")
				// Who called (#125), from the header every call of this
				// registry carries. A caller that named nobody is
				// anonymous, which is a statement and not a gap.
				.attribute(ClientOrigin.ATTRIBUTE, originOf(request));
		request.setProperty(SPAN, span);
	}

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) {
		if (!(request.getProperty(SPAN) instanceof CallSpan span)) {
			return;
		}
		request.removeProperty(SPAN);
		span.attribute("http.response.status_code", String.valueOf(response.getStatus()));
		span.close();
	}

	/**
	 * A stream is not a call.
	 *
	 * <p>An SSE subscription is one request that lasts as long as the
	 * consumer does. A span around it would be open for hours and would
	 * say nothing that {@code fennec.services.client.stream.connected}
	 * does not say better.
	 */
	private static boolean isEventStream(ContainerRequestContext request) {
		String accept = request.getHeaderString(HttpHeaders.ACCEPT);
		return accept != null && accept.contains(MediaType.SERVER_SENT_EVENTS);
	}

	/**
	 * What to call this call in a trace: the resource and the method
	 * that matched, which is the closest a hand-written resource comes
	 * to naming a contract and an operation.
	 */
	private String operationOf(ContainerRequestContext request) {
		ResourceInfo matched = resource;
		Method method = matched == null ? null : matched.getResourceMethod();
		if (method == null || matched.getResourceClass() == null) {
			return request.getMethod();
		}
		return matched.getResourceClass().getSimpleName() + "/" + method.getName();
	}

	private static String originOf(ContainerRequestContext request) {
		String token = request.getHeaderString(ClientOrigin.HEADER);
		return token == null || token.isBlank() ? ClientOrigin.ANONYMOUS : token;
	}
}
