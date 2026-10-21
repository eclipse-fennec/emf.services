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
package org.eclipse.fennec.services.derive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.common.SecurityRequirements;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;
import org.junit.jupiter.api.Test;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

/**
 * The Jakarta security annotations of a service interface become the
 * contract's requirements (SECURITY.md §6): the interface's as the
 * default, a method's as its override.
 */
class JavaContractsSecurityTest {

	@RolesAllowed("payment-reader")
	interface Payment {

		double getBalance(String account);

		@RolesAllowed({ "payment-writer", "auditor" })
		double charge(double amount);

		@PermitAll
		String ping();
	}

	interface Open {

		String hello();
	}

	@RolesAllowed("**")
	interface AnyCaller {

		String whoAmI();
	}

	interface Forbidding {

		@DenyAll
		void nobody();
	}

	@Test
	void theInterfacesRolesAreTheDefault() {
		ServiceInterface contract = JavaContracts.contractOf(Payment.class, "1.0.0");

		assertThat(contract.getSecurity().isAuthenticated()).isTrue();
		assertThat(contract.getSecurity().getScopes()).containsExactly("payment-reader");
		ServiceOperation getBalance = operation(contract, "getBalance");
		assertThat(getBalance.getSecurity()).as("no own annotation, nothing of its own").isNull();
		assertThat(SecurityRequirements.missingScopes(getBalance, List.of()))
				.containsExactly("payment-reader");
	}

	@Test
	void aMethodsRolesOverrideTheInterface() {
		ServiceOperation charge = operation(JavaContracts.contractOf(Payment.class, "1.0.0"), "charge");

		assertThat(charge.getSecurity().getScopes()).containsExactly("payment-writer", "auditor");
	}

	@Test
	void permitAllOnAMethodLiftsTheDefault() {
		ServiceOperation ping = operation(JavaContracts.contractOf(Payment.class, "1.0.0"), "ping");

		assertThat(ping.getSecurity().isAuthenticated()).isFalse();
		assertThat(SecurityRequirements.demandsAuthentication(ping)).isFalse();
	}

	@Test
	void anUnannotatedInterfaceStatesNothing() {
		ServiceInterface contract = JavaContracts.contractOf(Open.class, "1.0.0");

		assertThat(contract.getSecurity()).isNull();
		assertThat(operation(contract, "hello").getSecurity()).isNull();
	}

	@Test
	void anyAuthenticatedIsARequirementWithoutScopes() {
		// "**" is Jakarta's "any verified caller", not a role called "**" —
		// and what the m2t template writes back for such a requirement.
		ServiceInterface contract = JavaContracts.contractOf(AnyCaller.class, "1.0.0");

		assertThat(contract.getSecurity().isAuthenticated()).isTrue();
		assertThat(contract.getSecurity().getScopes()).isEmpty();
	}

	@Test
	void denyAllIsRefusedNotIgnored() {
		assertThatThrownBy(() -> JavaContracts.contractOf(Forbidding.class, "1.0.0"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("DenyAll");
	}

	@Test
	void theRolesArePartOfTheFingerprint() {
		// Who may call is part of what the contract says, so it moves sd1:
		// a consumer built against the open contract must not bind the
		// guarded one by accident.
		String canonical = ServiceDescriptionFingerprint.canonicalForm(JavaContracts.contractOf(Payment.class, "1.0.0"));

		assertThat(canonical).contains("payment-reader").contains("payment-writer");
	}

	private static ServiceOperation operation(ServiceInterface contract, String name) {
		return contract.getOperations().stream().filter(op -> name.equals(op.getName())).findFirst().orElseThrow();
	}
}
