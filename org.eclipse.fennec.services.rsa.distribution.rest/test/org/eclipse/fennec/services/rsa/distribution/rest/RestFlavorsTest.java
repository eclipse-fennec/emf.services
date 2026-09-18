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

package org.eclipse.fennec.services.rsa.distribution.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.derive.JavaContracts;
import org.junit.jupiter.api.Test;

/**
 * The wire an exported service gets when nobody wrote one for it.
 *
 * <p>A derived flavor cannot be as good as a hand-written one — a Java
 * interface does not say which method is safe to repeat or which
 * argument is an identity. What it can be is correct, and these tests
 * pin the choices that make it so.
 */
class RestFlavorsTest {

	interface Payment {
		double charge(double amount, String currency);

		void record(ServiceInterface document, String note);
	}

	interface TwoDocuments {
		void merge(ServiceInterface left, ServiceInterface right);
	}

	private static RestFlavor flavorOf(Class<?> contract) {
		ServiceInterface derived = JavaContracts.contractOf(contract, "1.0.0",
				type -> ServiceInterface.class.isAssignableFrom(type)
						? ServicesPackage.eINSTANCE.getServiceInterface()
						: null);
		return RestFlavors.flavorFor(derived, RestFlavors.basePathFor(derived));
	}

	private static RestOperationFlavor operation(RestFlavor flavor, String name) {
		return flavor.getOperationFlavors().stream()
				.filter(of -> name.equals(of.getName()))
				.map(RestOperationFlavor.class::cast)
				.findFirst()
				.orElseThrow(() -> new AssertionError("no operation flavor " + name));
	}

	@Test
	void everyOperationIsAPost() {
		// Nothing in a Java interface says which methods are safe to
		// repeat. Announcing a GET for one that charges an account is a
		// lie a cache acts on, so nothing is announced as one.
		RestFlavor flavor = flavorOf(Payment.class);

		assertThat(flavor.getOperationFlavors())
				.allSatisfy(of -> assertThat(((RestOperationFlavor) of).getMethod()).isEqualTo(HttpMethod.POST));
	}

	@Test
	void eachOperationHasItsOwnPathUnderTheContractsBase() {
		RestFlavor flavor = flavorOf(Payment.class);

		assertThat(flavor.getBasePath()).isEqualTo("/payment");
		assertThat(operation(flavor, "charge").getPath()).isEqualTo("/charge");
		assertThat(operation(flavor, "record").getPath()).isEqualTo("/record");
	}

	@Test
	void scalarsTravelInTheQuery() {
		assertThat(operation(flavorOf(Payment.class), "charge").getParameterBindings())
				.extracting(binding -> binding.getParameter().getName(), binding -> binding.getBinding())
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("arg0", ParameterBinding.QUERY),
						org.assertj.core.groups.Tuple.tuple("arg1", ParameterBinding.QUERY));
	}

	@Test
	void aModelledArgumentTravelsInTheBody() {
		assertThat(operation(flavorOf(Payment.class), "record").getParameterBindings())
				.extracting(binding -> binding.getBinding())
				.containsExactly(ParameterBinding.BODY, ParameterBinding.QUERY);
	}

	@Test
	void whatComesBackDecidesTheMediaType() {
		// Found by running it: an int answered under application/xml has
		// no writer, and the call fails with a 500 that says nothing.
		RestFlavor flavor = flavorOf(Payment.class);

		assertThat(operation(flavor, "charge").getProduces()).containsExactly("text/plain");
		assertThat(operation(flavor, "record").getProduces()).as("void produces nothing").isEmpty();
		assertThat(operation(flavor, "record").getConsumes())
				.as("a modelled body is XMI, and the flavor says so")
				.containsExactly("application/xml");
		assertThat(operation(flavor, "charge").getConsumes()).as("scalars need no body type").isEmpty();
	}

	@Test
	void twoModelledArgumentsAreRefusedRatherThanSqueezedIntoAQueryString() {
		assertThatThrownBy(() -> flavorOf(TwoDocuments.class))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("one body");
	}
}
