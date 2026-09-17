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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.RestParameterBinding;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.rest.internal.RestServiceInvoker.Placement;
import org.junit.jupiter.api.Test;

/**
 * Where an argument travels is the provider's statement, not a guess
 * about its Java type (#74). These tests pin the sorting; the HTTP
 * mechanics behind it belong to JAX-RS.
 */
class RestServiceInvokerPlacementTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static Parameter parameter(ServiceOperation op, String name) {
		Parameter p = F.createParameter();
		p.setName(name);
		p.setType("string");
		p.setIndex(op.getParameters().size());
		op.getParameters().add(p);
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

	private static Map<String, Object> args(Object... nameValue) {
		Map<String, Object> args = new LinkedHashMap<>();
		for (int i = 0; i < nameValue.length; i += 2) {
			args.put((String) nameValue[i], nameValue[i + 1]);
		}
		return args;
	}

	@Test
	void eachBindingSendsItsArgumentToItsOwnPlace() {
		ServiceOperation operation = F.createServiceOperation();
		operation.setName("get");
		Parameter id = parameter(operation, "id");
		Parameter currency = parameter(operation, "currency");
		Parameter tenant = parameter(operation, "tenant");

		RestOperationFlavor flavor = F.createRestOperationFlavor();
		flavor.setOperation(operation);
		bind(flavor, id, ParameterBinding.PATH, null);
		bind(flavor, currency, ParameterBinding.QUERY, null);
		bind(flavor, tenant, ParameterBinding.HEADER, "X-Tenant");

		Placement placement = RestServiceInvoker.place(flavor,
				args("id", "42", "currency", "EUR", "tenant", "acme"));

		// GET /payments/{id}?currency=EUR with a tenant header — the case
		// the binding layer exists for.
		assertThat(placement.path).containsExactly(Map.entry("id", "42"));
		assertThat(placement.queryParameters()).containsExactly(Map.entry("currency", "EUR"));
		assertThat(placement.header).containsExactly(Map.entry("X-Tenant", "acme"));
	}

	@Test
	void aWireNameRenamesTheArgumentWithoutTouchingTheContract() {
		ServiceOperation operation = F.createServiceOperation();
		operation.setName("list");
		Parameter accountId = parameter(operation, "accountId");

		RestOperationFlavor flavor = F.createRestOperationFlavor();
		flavor.setOperation(operation);
		bind(flavor, accountId, ParameterBinding.QUERY, "account_id");

		Placement placement = RestServiceInvoker.place(flavor, args("accountId", "7"));

		assertThat(placement.queryParameters())
				.as("the contract keeps calling it accountId")
				.containsExactly(Map.entry("account_id", "7"));
	}

	@Test
	void anUndeclaredArgumentKeepsTravellingAsAQueryParameter() {
		ServiceOperation operation = F.createServiceOperation();
		operation.setName("charge");
		parameter(operation, "amount");

		RestOperationFlavor flavor = F.createRestOperationFlavor();
		flavor.setOperation(operation);

		Placement placement = RestServiceInvoker.place(flavor, args("amount", 12.5));

		assertThat(placement.body()).isNull();
		assertThat(placement.queryParameters()).containsExactly(Map.entry("amount", 12.5));
	}

	@Test
	void anUndeclaredEObjectIsStillTheBody() {
		ServiceOperation operation = F.createServiceOperation();
		operation.setName("addCatalogEntry");
		parameter(operation, "serviceInterface");

		RestOperationFlavor flavor = F.createRestOperationFlavor();
		flavor.setOperation(operation);

		Placement placement = RestServiceInvoker.place(flavor,
				args("serviceInterface", F.createServiceInterface()));

		assertThat(placement.body()).isNotNull();
		assertThat(placement.queryParameters())
				.as("an argument that became the body must not travel twice")
				.isEmpty();
	}

	@Test
	void severalBodyParametersHaveNoWireEncodingYet() {
		ServiceOperation operation = F.createServiceOperation();
		operation.setName("charge");
		Parameter amount = parameter(operation, "amount");
		Parameter currency = parameter(operation, "currency");

		RestOperationFlavor flavor = F.createRestOperationFlavor();
		flavor.setOperation(operation);
		bind(flavor, amount, ParameterBinding.BODY, null);
		bind(flavor, currency, ParameterBinding.BODY, null);

		Placement placement = RestServiceInvoker.place(flavor, args("amount", 12.5, "currency", "EUR"));

		assertThatThrownBy(placement::body)
				.isInstanceOf(DdsrException.class)
				.hasMessageContaining("no wire encoding");
	}
}
