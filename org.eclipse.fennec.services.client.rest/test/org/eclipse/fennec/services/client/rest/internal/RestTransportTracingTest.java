/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.client.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;

/**
 * The client half of a trace on the calls the SDK makes itself (#126).
 *
 * <p>A publish, a lookup, a heartbeat: these do not go through the
 * service invoker, and before this they started a trace at the broker
 * rather than continuing one from here. The proxies hand their prepared
 * request to the transport, and the transport is where the span and the
 * header belong — one place every broker call passes, rather than
 * twelve that can each forget.
 */
class RestTransportTracingTest {

	/** A tracer that injects a fixed context and remembers the rest. */
	private static final class Watching implements CallTracer {

		private String called;

		private final Map<String, String> attributes = new HashMap<>();

		private String failure;

		@Override
		public CallSpan calling(String operation, TraceCarrier outbound) {
			called = operation;
			outbound.set("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
			return new CallSpan() {

				@Override
				public CallSpan attribute(String name, String value) {
					attributes.put(name, value);
					return this;
				}

				@Override
				public void failed(Throwable error) {
					failure = String.valueOf(error.getMessage());
				}

				@Override
				public void close() {
					// nothing to close in a fake
				}
			};
		}

		@Override
		public CallSpan serving(String operation, TraceCarrier inbound) {
			throw new AssertionError("the transport calls, it does not serve");
		}
	}

	/** Records the headers a request was given; every builder call returns itself. */
	private static final class Recording {

		private final Map<String, Object> headers = new HashMap<>();

		private final Invocation.Builder builder = (Invocation.Builder) Proxy.newProxyInstance(
				Invocation.Builder.class.getClassLoader(), new Class<?>[] { Invocation.Builder.class },
				(proxy, method, args) -> {
					if ("header".equals(method.getName())) {
						headers.put(String.valueOf(args[0]), args[1]);
						return proxy;
					}
					return null;
				});
	}

	private static RestTransport transportWith(CallTracer tracer) throws Exception {
		RestTransport transport = new RestTransport();
		set(transport, "tracer", tracer);
		set(transport, "baseUrl", URI.create("http://localhost:9090/ddsr/rest"));
		return transport;
	}

	private static void set(RestTransport transport, String field, Object value) throws Exception {
		Field declared = RestTransport.class.getDeclaredField(field);
		declared.setAccessible(true);
		declared.set(transport, value);
	}

	@Test
	@DisplayName("the context the span wrote is on the request that goes out")
	void contextTravelsOnTheRequest() throws Exception {
		Watching watching = new Watching();
		Recording request = new Recording();

		Response answer = transportWith(watching).send("BrokerImplementations/publishImplementation",
				request.builder, builder -> Response.ok().build());

		assertThat(answer.getStatus()).isEqualTo(200);
		assertThat(watching.called)
			.as("named as the broker's own dispatcher names the serving half, so the two read as one call")
			.isEqualTo("BrokerImplementations/publishImplementation");
		assertThat(request.headers)
			.containsEntry("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
		assertThat(watching.attributes)
			.containsEntry("server.address", "http://localhost:9090/ddsr/rest")
			.containsEntry("fennec.flavor", "REST")
			.containsEntry("http.response.status_code", "200");
	}

	@Test
	@DisplayName("a broker that cannot be reached is marked on the span and still thrown")
	void unreachableBrokerIsRecorded() throws Exception {
		Watching watching = new Watching();
		Recording request = new Recording();
		RestTransport transport = transportWith(watching);

		assertThatThrownBy(() -> transport.send("BrokerLookup/getServiceReferences", request.builder,
				builder -> {
					throw new ProcessingException("connection refused");
				}))
			.as("the proxies see what they always saw")
			.isInstanceOf(ProcessingException.class);

		assertThat(watching.failure)
			.as("and the one call worth seeing in a trace is the one that did not arrive")
			.isEqualTo("connection refused");
	}

	@Test
	@DisplayName("with nothing watching, the request goes out exactly as before")
	void noTracerChangesNothing() throws Exception {
		Recording request = new Recording();

		String answer = transportWith(null).send("BrokerCatalog/getRegistry", request.builder,
				builder -> "the answer");

		assertThat(answer).isEqualTo("the answer");
		assertThat(request.headers)
			.as("no span, no header — telemetry that is not installed is not on the wire")
			.isEmpty();
	}
}
