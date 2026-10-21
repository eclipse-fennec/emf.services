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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.SecurityRequirement;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.common.TokenSupplier;
import org.junit.jupiter.api.Test;

/**
 * A provider call carries a token of the provider's audience — never the
 * broker's (SECURITY.md §1, §6).
 */
class RestServiceInvokerBearerTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	/** Hands out "token-for-<audience>" and remembers who asked for what. */
	private static final class Audiences implements TokenSupplier {

		final List<String> asked = new ArrayList<>();

		@Override
		public String token() {
			asked.add("<broker>");
			return "broker-token";
		}

		@Override
		public String token(String audience) {
			asked.add(audience);
			return "token-for-" + audience;
		}
	}

	@Test
	void aGuardedTargetIsCalledWithATokenOfItsAudience() throws Exception {
		Audiences tokens = new Audiences();
		RestOperationFlavor op = operation(true);

		String bearer = invoker(tokens).bearerFor(flavorWithScheme(op), op, "record");

		assertThat(bearer).isEqualTo("token-for-rsa-example");
		assertThat(tokens.asked).as("never the broker's").containsExactly("rsa-example");
	}

	@Test
	void aTargetWithoutASchemeIsCalledWithoutAToken() throws Exception {
		Audiences tokens = new Audiences();
		RestOperationFlavor op = operation(false);
		RestFlavor open = F.createRestFlavor();
		open.getOperationFlavors().add(op);

		assertThat(invoker(tokens).bearerFor(open, op, "version")).isNull();
		assertThat(tokens.asked).isEmpty();
	}

	@Test
	void aGuardedOperationWithoutASupplierFailsSayingWhy() throws Exception {
		RestOperationFlavor op = operation(true);

		assertThatThrownBy(() -> invoker(null).bearerFor(flavorWithScheme(op), op, "record"))
				.isInstanceOf(DdsrException.class)
				.hasMessageContaining("no token supplier");
	}

	@Test
	void anOpenOperationOfAGuardedTargetNeedsNoSupplier() throws Exception {
		RestOperationFlavor op = operation(false);

		assertThat(invoker(null).bearerFor(flavorWithScheme(op), op, "version")).isNull();
	}

	private static RestServiceInvoker invoker(TokenSupplier tokens) throws Exception {
		RestServiceInvoker invoker = new RestServiceInvoker();
		Field field = RestServiceInvoker.class.getDeclaredField("tokens");
		field.setAccessible(true);
		field.set(invoker, tokens);
		return invoker;
	}

	private static RestOperationFlavor operation(boolean guarded) {
		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Ledger");
		ServiceOperation operation = F.createServiceOperation();
		operation.setName(guarded ? "record" : "version");
		if (guarded) {
			SecurityRequirement requirement = F.createSecurityRequirement();
			requirement.setAuthenticated(true);
			requirement.getScopes().add("ledger-writer");
			operation.setSecurity(requirement);
		}
		contract.getOperations().add(operation);
		RestOperationFlavor op = F.createRestOperationFlavor();
		op.setName(operation.getName());
		op.setOperation(operation);
		return op;
	}

	private static RestFlavor flavorWithScheme(RestOperationFlavor op) {
		RestFlavor flavor = F.createRestFlavor();
		flavor.getOperationFlavors().add(op);
		BearerTokenScheme scheme = F.createBearerTokenScheme();
		scheme.setIssuer("http://localhost:18080/realms/fennec");
		scheme.getAudiences().add("rsa-example");
		flavor.getSecuritySchemes().add(scheme);
		return flavor;
	}
}
