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
package org.eclipse.fennec.services.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.ConnectionScheme;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.SecurityRequirement;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.Test;

/** The operation's own requirement where it states one, else the interface's (SECURITY.md §2). */
class SecurityRequirementsTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	@Test
	void anOperationWithoutItsOwnTakesTheInterfaces() {
		ServiceInterface contract = contract(requirement(true, "payment-reader"));
		ServiceOperation getBalance = operation(contract, "getBalance", null);

		assertThat(SecurityRequirements.effective(getBalance)).isSameAs(contract.getSecurity());
		assertThat(SecurityRequirements.demandsAuthentication(getBalance)).isTrue();
		assertThat(SecurityRequirements.missingScopes(getBalance, List.of())).containsExactly("payment-reader");
	}

	@Test
	void anOperationsOwnOverridesTheInterface() {
		ServiceInterface contract = contract(requirement(true, "payment-reader"));
		ServiceOperation charge = operation(contract, "charge", requirement(true, "payment-writer"));
		ServiceOperation ping = operation(contract, "ping", requirement(false));

		assertThat(SecurityRequirements.missingScopes(charge, List.of("payment-reader")))
				.as("the interface's scope does not carry over").containsExactly("payment-writer");
		assertThat(SecurityRequirements.demandsAuthentication(ping))
				.as("@PermitAll on one method of a guarded interface").isFalse();
	}

	@Test
	void nothingStatedNothingDemanded() {
		ServiceOperation op = operation(contract(null), "getBalance", null);

		assertThat(SecurityRequirements.effective(op)).isNull();
		assertThat(SecurityRequirements.demandsAuthentication(op)).isFalse();
		assertThat(SecurityRequirements.missingScopes(op, null)).isEmpty();
	}

	@Test
	void scopesPresupposeAPrincipal() {
		ServiceOperation op = operation(contract(null), "charge", requirement(false, "payment-writer"));

		assertThat(SecurityRequirements.demandsAuthentication(op))
				.as("scopesImplyAuthenticated, read the safe way round").isTrue();
	}

	@Test
	void allScopesAreNeeded() {
		ServiceOperation op = operation(contract(null), "refund", requirement(true, "payment-writer", "auditor"));

		assertThat(SecurityRequirements.missingScopes(op, List.of("payment-writer"))).containsExactly("auditor");
		assertThat(SecurityRequirements.missingScopes(op, List.of("auditor", "payment-writer"))).isEmpty();
	}

	@Test
	void theBearerSchemeOfAFlavor() {
		RestFlavor flavor = F.createRestFlavor();
		ConnectionScheme gateway = F.createConnectionScheme();
		BearerTokenScheme bearer = F.createBearerTokenScheme();
		flavor.getSecuritySchemes().add(gateway);
		flavor.getSecuritySchemes().add(bearer);

		assertThat(SecurityRequirements.bearerScheme(flavor)).isSameAs(bearer);
		assertThat(SecurityRequirements.bearerScheme(F.createRestFlavor())).isNull();
	}

	private static ServiceInterface contract(SecurityRequirement security) {
		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Payment");
		contract.setSecurity(security);
		return contract;
	}

	private static ServiceOperation operation(ServiceInterface contract, String name, SecurityRequirement security) {
		ServiceOperation operation = F.createServiceOperation();
		operation.setName(name);
		operation.setSecurity(security);
		contract.getOperations().add(operation);
		return operation;
	}

	private static SecurityRequirement requirement(boolean authenticated, String... scopes) {
		SecurityRequirement requirement = F.createSecurityRequirement();
		requirement.setAuthenticated(authenticated);
		requirement.getScopes().addAll(List.of(scopes));
		return requirement;
	}
}
