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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

/**
 * The provider's half of a trace, for a resource nobody generated.
 *
 * <p>What is worth pinning: that the caller's context is read out of
 * the request's own headers, that the span is named after what
 * matched rather than after a path, that a stream is left alone, and
 * that a framework without telemetry is not charged for any of it.
 */
class TracedRequestsTest {

	/** Remembers what it was asked, and hands out a span that records. */
	private static final class Watching implements CallTracer {

		private final List<String> served = new ArrayList<>();

		private final Map<String, String> attributes = new HashMap<>();

		private String sawTraceparent;

		private boolean closed;

		@Override
		public CallSpan calling(String operation, TraceCarrier outbound) {
			throw new AssertionError("a filter serves, it does not call");
		}

		@Override
		public CallSpan serving(String operation, TraceCarrier inbound) {
			served.add(operation);
			sawTraceparent = inbound.get("traceparent");
			return new CallSpan() {

				@Override
				public CallSpan attribute(String name, String value) {
					attributes.put(name, value);
					return this;
				}

				@Override
				public void failed(Throwable error) {
					attributes.put("error", String.valueOf(error.getMessage()));
				}

				@Override
				public void close() {
					closed = true;
				}
			};
		}
	}

	/** The resource this request matched. */
	public static final class PaymentResource {

		public String getBalance() {
			return "1234";
		}
	}

	private static TracedRequests filterWith(CallTracer tracer) throws Exception {
		TracedRequests filter = new TracedRequests();
		set(filter, "tracer", tracer);
		ResourceInfo matched = mock(ResourceInfo.class);
		when(matched.getResourceClass()).thenAnswer(invocation -> PaymentResource.class);
		when(matched.getResourceMethod())
				.thenReturn(PaymentResource.class.getMethod("getBalance"));
		set(filter, "resource", matched);
		return filter;
	}

	private static void set(TracedRequests filter, String field, Object value) throws Exception {
		Field declared = TracedRequests.class.getDeclaredField(field);
		declared.setAccessible(true);
		declared.set(filter, value);
	}

	private static ContainerRequestContext request(Map<String, String> headers) {
		MultivaluedMap<String, String> all = new MultivaluedHashMap<>();
		headers.forEach(all::putSingle);
		Map<String, Object> properties = new HashMap<>();

		ContainerRequestContext request = mock(ContainerRequestContext.class);
		when(request.getHeaders()).thenReturn(all);
		when(request.getMethod()).thenReturn("GET");
		when(request.getHeaderString(anyString())).thenAnswer(call -> all.getFirst(call.getArgument(0)));
		UriInfo uriInfo = mock(UriInfo.class);
		when(uriInfo.getPath()).thenReturn("balance");
		when(request.getUriInfo()).thenReturn(uriInfo);
		when(request.getProperty(anyString())).thenAnswer(call -> properties.get(call.getArgument(0)));
		doAnswer(call -> properties.put(call.getArgument(0), call.getArgument(1)))
				.when(request).setProperty(anyString(), any());
		doAnswer(call -> properties.remove(call.getArgument(0)))
				.when(request).removeProperty(anyString());
		return request;
	}

	private static ContainerResponseContext response(int status) {
		ContainerResponseContext response = mock(ContainerResponseContext.class);
		when(response.getStatus()).thenReturn(status);
		return response;
	}

	@Test
	@DisplayName("the caller's context is read from the request, and the span names what matched")
	void theCallerIsContinued() throws Exception {
		Watching watching = new Watching();
		TracedRequests filter = filterWith(watching);
		ContainerRequestContext request = request(Map.of(
				"traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
				ClientOrigin.HEADER, "payment-demo/7f3a"));

		filter.filter(request);
		filter.filter(request, response(200));

		assertThat(watching.served)
			.as("named after the resource and the method, never after the path")
			.containsExactly("PaymentResource/getBalance");
		assertThat(watching.sawTraceparent)
			.isEqualTo("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
		assertThat(watching.attributes)
			.containsEntry("http.request.method", "GET")
			.containsEntry("url.path", "balance")
			.containsEntry("fennec.flavor", "REST")
			.containsEntry(ClientOrigin.ATTRIBUTE, "payment-demo/7f3a")
			.containsEntry("http.response.status_code", "200");
		assertThat(watching.closed)
			.as("a span that the response filter did not end would never be exported")
			.isTrue();
	}

	@Test
	@DisplayName("a caller that named nobody is anonymous")
	void anUnnamedCallerIsAnonymous() throws Exception {
		Watching watching = new Watching();
		TracedRequests filter = filterWith(watching);
		ContainerRequestContext request = request(Map.of());

		filter.filter(request);
		filter.filter(request, response(404));

		assertThat(watching.attributes)
			.containsEntry(ClientOrigin.ATTRIBUTE, ClientOrigin.ANONYMOUS)
			.containsEntry("http.response.status_code", "404");
	}

	@Test
	@DisplayName("a stream is not a call, and gets no span")
	void eventStreamsAreLeftAlone() throws Exception {
		Watching watching = new Watching();
		TracedRequests filter = filterWith(watching);

		filter.filter(request(Map.of(HttpHeaders.ACCEPT, MediaType.SERVER_SENT_EVENTS)));

		assertThat(watching.served)
			.as("a subscription lasts as long as the consumer does; a span around it says nothing")
			.isEmpty();
	}

	@Test
	@DisplayName("with nothing watching, a request costs a null check")
	void noTracer() throws Exception {
		TracedRequests filter = filterWith(null);
		ContainerRequestContext request = request(Map.of());

		filter.filter(request);
		filter.filter(request, response(200));

		assertThat(request.getProperty("org.eclipse.fennec.services.telemetry.rest.span")).isNull();
	}
}
