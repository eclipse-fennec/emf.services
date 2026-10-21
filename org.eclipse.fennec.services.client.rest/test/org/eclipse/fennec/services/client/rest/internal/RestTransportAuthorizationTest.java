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
package org.eclipse.fennec.services.client.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.URI;

import org.eclipse.fennec.services.common.TokenSupplier;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * The broker's token goes to the broker and nowhere else (SECURITY.md
 * §4): the same client carries service invocations, and a provider that
 * received the token could replay it.
 */
class RestTransportAuthorizationTest {

	private static final URI BROKER = URI.create("http://broker.test:8887/ddsr/rest");

	@Test
	void aCallToTheBrokerCarriesTheToken() throws Exception {
		MultivaluedMap<String, Object> headers = call(transport(() -> "t0k3n"), "http://broker.test:8887/ddsr/rest/catalog/");

		assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer t0k3n");
	}

	@Test
	void theEventStreamIsACallToTheBrokerToo() throws Exception {
		MultivaluedMap<String, Object> headers = call(transport(() -> "t0k3n"), "http://broker.test:8887/ddsr/rest/events");

		assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer t0k3n");
	}

	@Test
	void aCallToAProviderDoesNot() throws Exception {
		RestTransport transport = transport(() -> "t0k3n");

		assertThat(call(transport, "http://payments.test:9091/payments/charge").containsKey(HttpHeaders.AUTHORIZATION))
				.as("another host").isFalse();
		assertThat(call(transport, "http://broker.test:9091/ddsr/rest/catalog").containsKey(HttpHeaders.AUTHORIZATION))
				.as("same host, another port").isFalse();
		assertThat(call(transport, "http://broker.test:8887/ddsr/restless").containsKey(HttpHeaders.AUTHORIZATION))
				.as("a path that only starts with the broker's").isFalse();
		assertThat(call(transport, "https://broker.test:8887/ddsr/rest/catalog").containsKey(HttpHeaders.AUTHORIZATION))
				.as("another scheme").isFalse();
	}

	@Test
	void aNodeThatProvesItselfByCertificateShowsTheBrokerNoToken() throws Exception {
		// Discovery over the node's certificate, calls over tokens: the
		// supplier is there for the providers, and still the broker gets none.
		RestTransport transport = transport(() -> "t0k3n");
		set(transport, "tokenToBroker", false);

		assertThat(call(transport, "http://broker.test:8887/ddsr/rest/catalog/").containsKey(HttpHeaders.AUTHORIZATION))
				.isFalse();
	}

	@Test
	void withoutASupplierNothingIsAdded() throws Exception {
		assertThat(call(transport(null), "http://broker.test:8887/ddsr/rest/catalog/").containsKey(HttpHeaders.AUTHORIZATION))
				.isFalse();
	}

	@Test
	void aSupplierThatCannotDeliverFailsTheCallHere() throws Exception {
		RestTransport transport = transport(() -> {
			throw new IllegalStateException("token endpoint unreachable");
		});

		assertThatThrownBy(() -> call(transport, "http://broker.test:8887/ddsr/rest/catalog/"))
				.hasMessageContaining("token endpoint unreachable");
	}

	private static MultivaluedMap<String, Object> call(RestTransport transport, String uri) {
		MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
		ClientRequestContext request = mock(ClientRequestContext.class);
		when(request.getUri()).thenReturn(URI.create(uri));
		when(request.getHeaders()).thenReturn(headers);
		transport.authorize(request);
		return headers;
	}

	private static RestTransport transport(TokenSupplier tokens) throws Exception {
		RestTransport transport = new RestTransport();
		set(transport, "baseUrl", BROKER);
		set(transport, "tokens", tokens);
		return transport;
	}

	private static void set(RestTransport transport, String field, Object value) throws Exception {
		Field declared = RestTransport.class.getDeclaredField(field);
		declared.setAccessible(true);
		declared.set(transport, value);
	}
}
