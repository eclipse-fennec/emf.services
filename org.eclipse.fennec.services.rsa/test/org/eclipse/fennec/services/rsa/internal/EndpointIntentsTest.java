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
package org.eclipse.fennec.services.rsa.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.fennec.services.SecurityRequirement;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.rsa.spi.FlavorDistribution;
import org.junit.jupiter.api.Test;

/**
 * An endpoint says what this call is, not everything its transport could
 * do: {@code fennec.bearer} only where the contract demands a caller
 * (SECURITY.md §6).
 */
class EndpointIntentsTest {

	private static final List<String> PROMISED = List.of("osgi.basic", FlavorDistribution.BEARER_INTENT);

	@Test
	void aGuardedContractCarriesTheBearerIntent() {
		assertThat(FennecRemoteServiceAdmin.intentsOf(contract(true), PROMISED))
				.containsExactly("osgi.basic", FlavorDistribution.BEARER_INTENT);
	}

	@Test
	void anOpenContractDoesNot() {
		assertThat(FennecRemoteServiceAdmin.intentsOf(contract(false), PROMISED)).containsExactly("osgi.basic");
	}

	@Test
	void whatTheTransportNeverPromisedStaysAway() {
		assertThat(FennecRemoteServiceAdmin.intentsOf(contract(true), List.of("osgi.basic")))
				.as("a node without an issuer does not claim it").containsExactly("osgi.basic");
	}

	private static ServiceInterface contract(boolean guarded) {
		ServiceInterface contract = ServicesFactory.eINSTANCE.createServiceInterface();
		contract.setName("Greeter");
		ServiceOperation greet = ServicesFactory.eINSTANCE.createServiceOperation();
		greet.setName("greet");
		contract.getOperations().add(greet);
		if (guarded) {
			SecurityRequirement requirement = ServicesFactory.eINSTANCE.createSecurityRequirement();
			requirement.setAuthenticated(true);
			requirement.getScopes().add("greeter");
			contract.setSecurity(requirement);
		}
		return contract;
	}
}
