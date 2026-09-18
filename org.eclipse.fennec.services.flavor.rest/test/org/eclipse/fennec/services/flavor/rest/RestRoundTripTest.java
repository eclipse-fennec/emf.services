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

package org.eclipse.fennec.services.flavor.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.RestParameterBinding;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.Test;

/**
 * The two directions of one flavor have to meet: what a consumer writes
 * with {@link RestPlacement} is what a provider reads with
 * {@link RestRoute} and {@link RestArguments}. These tests put a call
 * through both and compare — the check that no side has its own reading.
 */
class RestRoundTripTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static RestFlavor directory() {
		ServiceOperation get = F.createServiceOperation();
		get.setName("get");
		RestFlavor flavor = F.createRestFlavor();
		RestOperationFlavor operationFlavor = F.createRestOperationFlavor();
		operationFlavor.setName("get");
		operationFlavor.setMethod(HttpMethod.GET);
		operationFlavor.setPath("/persons/{id}");
		operationFlavor.setOperation(get);
		flavor.getOperationFlavors().add(operationFlavor);

		bind(operationFlavor, parameter(get, "id", "string"), ParameterBinding.PATH, null);
		bind(operationFlavor, parameter(get, "currency", "string"), ParameterBinding.QUERY, null);
		bind(operationFlavor, parameter(get, "tenant", "string"), ParameterBinding.HEADER, "X-Tenant");
		bind(operationFlavor, parameter(get, "limit", "int"), ParameterBinding.QUERY, "max");
		return flavor;
	}

	private static Parameter parameter(ServiceOperation operation, String name, String type) {
		Parameter p = F.createParameter();
		p.setName(name);
		p.setType(type);
		p.setIndex(operation.getParameters().size());
		operation.getParameters().add(p);
		return p;
	}

	private static void bind(RestOperationFlavor flavor, Parameter parameter,
			ParameterBinding where, String wireName) {
		RestParameterBinding binding = F.createRestParameterBinding();
		binding.setParameter(parameter);
		binding.setBinding(where);
		if (wireName != null) {
			binding.setWireName(wireName);
		}
		flavor.getParameterBindings().add(binding);
	}

	@Test
	void whatTheConsumerWritesIsWhatTheProviderReads() {
		RestFlavor flavor = directory();
		RestOperationFlavor operationFlavor = (RestOperationFlavor) flavor.getOperationFlavors().get(0);
		Map<String, Object> sent = Map.of("id", "acct-42", "currency", "EUR", "tenant", "acme", "limit", 25);

		RestPlacement placement = RestPlacement.of(operationFlavor, sent);

		// what the transport would put on the wire
		String path = "/persons/" + placement.path().get("id");
		Map<String, Object> query = placement.query();
		Map<String, Object> headers = placement.header();

		RestRoute route = RestRoute.match(flavor, "GET", path).orElseThrow();
		Map<String, Object> received = RestArguments.of(route.operationFlavor(), route.pathVariables(),
				name -> query.containsKey(name) ? List.of(String.valueOf(query.get(name))) : List.of(),
				name -> headers.containsKey(name) ? String.valueOf(headers.get(name)) : null,
				null);

		assertThat(received)
				.as("every value arrives under its own parameter name, in its declared type")
				.containsExactlyInAnyOrderEntriesOf(sent);
	}

	@Test
	void theMoreSpecificPathWins() {
		RestFlavor flavor = directory();
		ServiceOperation list = F.createServiceOperation();
		list.setName("list");
		RestOperationFlavor listFlavor = F.createRestOperationFlavor();
		listFlavor.setName("list");
		listFlavor.setMethod(HttpMethod.GET);
		listFlavor.setPath("/persons");
		listFlavor.setOperation(list);
		flavor.getOperationFlavors().add(listFlavor);

		assertThat(RestRoute.match(flavor, "GET", "/persons/42").orElseThrow().operationFlavor().getName())
				.isEqualTo("get");
		assertThat(RestRoute.match(flavor, "GET", "/persons").orElseThrow().operationFlavor().getName())
				.as("declaration order must not decide what an endpoint does")
				.isEqualTo("list");
	}

	@Test
	void anAbsentArgumentFallsBackToTheContractsDefault() {
		RestFlavor flavor = directory();
		RestOperationFlavor operationFlavor = (RestOperationFlavor) flavor.getOperationFlavors().get(0);
		operationFlavor.getOperation().getParameters().stream()
				.filter(p -> "limit".equals(p.getName()))
				.forEach(p -> { p.setOptional(true); p.setDefaultValue("50"); });

		Map<String, Object> received = RestArguments.of(operationFlavor, Map.of("id", "acct-42"),
				name -> List.of(), name -> null, null);

		assertThat(received).containsEntry("limit", 50);
		assertThat(received).containsEntry("currency", null);
	}

	@Test
	void theMethodIsPartOfTheRoute() {
		assertThat(RestRoute.match(directory(), "POST", "/persons/42")).isEmpty();
	}

	@Test
	void anOperationWithoutABodyBindingHasNoBodyParameter() {
		assertThat(RestArguments.bodyParameter((RestOperationFlavor) directory().getOperationFlavors().get(0)))
				.as("every argument of this one travels in the path, query or a header")
				.isNull();
	}

	@Test
	void theBodyParameterIsTheOneBoundToIt() {
		RestFlavor flavor = directory();
		RestOperationFlavor operationFlavor = (RestOperationFlavor) flavor.getOperationFlavors().get(0);
		Parameter note = parameter(operationFlavor.getOperation(), "note", "string");
		bind(operationFlavor, note, ParameterBinding.BODY, null);

		assertThat(RestArguments.bodyParameter(operationFlavor)).isSameAs(note);
	}

	@Test
	void aBodyThatArrivesAsTextIsConvertedLikeAnyOtherValue() {
		// A transport that read the payload as text hands it over as
		// such; what it becomes is the contract's business, the same
		// rule that applies to a query parameter.
		RestFlavor flavor = directory();
		RestOperationFlavor operationFlavor = (RestOperationFlavor) flavor.getOperationFlavors().get(0);
		bind(operationFlavor, parameter(operationFlavor.getOperation(), "count", "int"),
				ParameterBinding.BODY, null);

		Map<String, Object> read = RestArguments.of(operationFlavor, Map.of("id", "acct-42"),
				name -> List.of(), name -> null, "17");

		assertThat(read).containsEntry("count", 17);
	}

	@Test
	void aBodyThatIsAlreadyAnObjectIsPassedThrough() {
		RestFlavor flavor = directory();
		RestOperationFlavor operationFlavor = (RestOperationFlavor) flavor.getOperationFlavors().get(0);
		bind(operationFlavor, parameter(operationFlavor.getOperation(), "contract", "object"),
				ParameterBinding.BODY, null);
		ServiceOperation payload = F.createServiceOperation();

		Map<String, Object> read = RestArguments.of(operationFlavor, Map.of("id", "acct-42"),
				name -> List.of(), name -> null, payload);

		assertThat(read).containsEntry("contract", payload);
	}
}
