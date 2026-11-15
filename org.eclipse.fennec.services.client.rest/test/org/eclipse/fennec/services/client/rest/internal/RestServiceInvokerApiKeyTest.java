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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;

import org.eclipse.fennec.services.ApiKeyLocation;
import org.eclipse.fennec.services.ApiKeyScheme;
import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.SecurityRequirement;
import org.eclipse.fennec.services.SecuritySchemeKind;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.flavor.rest.RestPlacement;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

/** An endpoint that asks for an API key gets the one configured for it (#299). */
class RestServiceInvokerApiKeyTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;
	private static final String PETSTORE = "http://petstore:8080/api/v3/pet/1";

	@Test
	void theEndpointGetsTheKeyConfiguredForItsHostAndScheme() throws Exception {
		RestOperationFlavor op = operation(true);

		RestServiceInvoker.ApiKey key = invoker(new ConfiguredApiKey("http://petstore:8080/", "api_key", "special-key"))
				.apiKeyFor(flavor(op, ApiKeyLocation.HEADER, false), op, "getPetById", PETSTORE);

		assertThat(key).isEqualTo(new RestServiceInvoker.ApiKey(ApiKeyLocation.HEADER, "X-API-Key", "special-key"));
	}

	@Test
	void anotherEndpointGetsNoKey() throws Exception {
		RestServiceInvoker invoker = invoker(new ConfiguredApiKey("http://elsewhere:8080", "api_key", "special-key"));

		assertThat(invoker.apiKeyFor(flavor(operation(false), ApiKeyLocation.HEADER, false), operation(false), "open",
				PETSTORE)).isNull();
		RestOperationFlavor guarded = operation(true);
		assertThatThrownBy(() -> invoker.apiKeyFor(flavor(guarded, ApiKeyLocation.HEADER, false), guarded, "getPetById",
				PETSTORE)).isInstanceOf(DdsrException.class).hasMessageContaining(ConfiguredApiKey.PID)
				.hasMessageContaining("http://petstore:8080");
	}

	@Test
	void aWildcardMatchesEveryEndpoint() {
		ConfiguredApiKey any = new ConfiguredApiKey("*", null, "k");

		assertThat(any.key("http://a:1", "x")).isEqualTo("k");
		assertThat(new ConfiguredApiKey(null, "api_key", "k").key("http://a:1", "other")).isNull();
	}

	@Test
	void withoutATokenAKeyAlternativeIsNoFailureOfTheBearerScheme() throws Exception {
		RestOperationFlavor op = operation(true);
		RestServiceInvoker invoker = invoker(new ConfiguredApiKey("*", "*", "k"));

		RestFlavor both = flavor(op, ApiKeyLocation.HEADER, true);

		assertThat(invoker.bearerFor(both, op, "getPetById")).as("the key is the other way in").isNull();
		assertThat(invoker.apiKeyFor(both, op, "getPetById", PETSTORE).value()).isEqualTo("k");
	}

	@Test
	void theKeyTravelsWhereTheSchemeSays() {
		RestPlacement nothing = RestPlacement.of(F.createRestOperationFlavor(), Map.of());

		Calls header = calls();
		RestServiceInvoker.send(header.target, HttpMethod.GET, nothing, "application/json", "application/json", Map.of(),
				new RestServiceInvoker.ApiKey(ApiKeyLocation.HEADER, "X-API-Key", "k"));
		verify(header.request).header("X-API-Key", "k");
		verify(header.target, never()).queryParam(anyString(), any());

		Calls query = calls();
		RestServiceInvoker.send(query.target, HttpMethod.GET, nothing, "application/json", "application/json", Map.of(),
				new RestServiceInvoker.ApiKey(ApiKeyLocation.QUERY, "api_key", "k"));
		verify(query.target).queryParam("api_key", "k");

		Calls cookie = calls();
		RestServiceInvoker.send(cookie.target, HttpMethod.GET, nothing, "application/json", "application/json", Map.of(),
				new RestServiceInvoker.ApiKey(ApiKeyLocation.COOKIE, "session_key", "k"));
		verify(cookie.request).cookie("session_key", "k");
	}

	private record Calls(WebTarget target, Invocation.Builder request) {
	}

	private static Calls calls() {
		WebTarget target = mock(WebTarget.class);
		Invocation.Builder request = mock(Invocation.Builder.class);
		when(target.queryParam(anyString(), any())).thenReturn(target);
		when(target.request(anyString())).thenReturn(request);
		when(request.header(anyString(), any())).thenReturn(request);
		when(request.cookie(anyString(), anyString())).thenReturn(request);
		when(request.get()).thenReturn(mock(Response.class));
		return new Calls(target, request);
	}

	private static RestServiceInvoker invoker(ConfiguredApiKey key) throws Exception {
		RestTransport transport = new RestTransport();
		set(transport, RestTransport.class, "providerTokens", new NoTokens());
		transport.addApiKey(key);
		RestServiceInvoker invoker = new RestServiceInvoker();
		set(invoker, RestServiceInvoker.class, "tx", transport);
		return invoker;
	}

	private static void set(Object target, Class<?> type, String name, Object value) throws Exception {
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static RestOperationFlavor operation(boolean guarded) {
		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Petstore");
		ServiceOperation operation = F.createServiceOperation();
		operation.setName(guarded ? "getPetById" : "open");
		if (guarded) {
			SecurityRequirement requirement = F.createSecurityRequirement();
			requirement.setAuthenticated(true);
			operation.setSecurity(requirement);
		}
		contract.getOperations().add(operation);
		RestOperationFlavor op = F.createRestOperationFlavor();
		op.setName(operation.getName());
		op.setOperation(operation);
		return op;
	}

	private static RestFlavor flavor(RestOperationFlavor op, ApiKeyLocation in, boolean alsoBearer) {
		RestFlavor flavor = F.createRestFlavor();
		flavor.getOperationFlavors().add(op);
		if (alsoBearer) {
			BearerTokenScheme bearer = F.createBearerTokenScheme();
			bearer.setName("petstore_auth");
			bearer.setIssuer("https://petstore/oauth");
			flavor.getSecuritySchemes().add(bearer);
		}
		ApiKeyScheme key = F.createApiKeyScheme();
		key.setName("api_key");
		key.setKind(SecuritySchemeKind.API_KEY);
		key.setIn(in);
		key.setParameterName("X-API-Key");
		flavor.getSecuritySchemes().add(key);
		return flavor;
	}
}
