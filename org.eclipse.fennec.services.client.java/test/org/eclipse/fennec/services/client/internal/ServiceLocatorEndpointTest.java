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

package org.eclipse.fennec.services.client.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.Test;

/**
 * An operation whose flavor binds an argument into the path has no
 * address until a call closes the template. The two views of its
 * endpoint say so differently — and a transport must get the one it can
 * make a request from (#82).
 */
class ServiceLocatorEndpointTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static ServiceLocatorImpl locator(String path) {
		RestOperationFlavor opFlavor = F.createRestOperationFlavor();
		opFlavor.setName("charge");
		opFlavor.setPath(path);

		RestFlavor flavor = F.createRestFlavor();
		flavor.setHost("http://localhost:9090");
		flavor.setBasePath("/payments");
		flavor.getOperationFlavors().add(opFlavor);

		ServiceImplementation implementation = F.createServiceImplementation();
		implementation.setName("payments-ts-rest");
		implementation.getFlavors().add(flavor);

		ServiceReference reference = F.createServiceReference();
		reference.setId("ref-1");
		return new ServiceLocatorImpl(reference, implementation);
	}

	@Test
	void a_plain_path_is_both_an_endpoint_and_an_address() {
		ServiceLocatorImpl locator = locator("/charge");

		assertThat(locator.endpointFor("charge")).hasValue("http://localhost:9090/payments/charge");
		assertThat(locator.urlFor("charge")).hasValueSatisfying(
				url -> assertThat(url.toString()).isEqualTo("http://localhost:9090/payments/charge"));
	}

	@Test
	void a_templated_path_stays_a_template_instead_of_failing_to_parse() {
		ServiceLocatorImpl locator = locator("/charge/{amount}");

		assertThat(locator.endpointFor("charge")).hasValue("http://localhost:9090/payments/charge/{amount}");
	}

	@Test
	void and_is_honestly_not_an_address() {
		// java.net.URI rejects the braces outright, so the choice is
		// between an exception and an honest empty. Anything that needs
		// to call the operation asks endpointFor.
		assertThat(locator("/charge/{amount}").urlFor("charge")).isEmpty();
	}

	@Test
	void an_unknown_operation_has_neither() {
		ServiceLocatorImpl locator = locator("/charge");

		assertThat(locator.endpointFor("refund")).isEmpty();
		assertThat(locator.urlFor("refund")).isEmpty();
	}
}
