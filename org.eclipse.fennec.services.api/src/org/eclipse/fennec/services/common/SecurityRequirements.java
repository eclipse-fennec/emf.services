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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.MutualTlsScheme;
import org.eclipse.fennec.services.SecurityRequirement;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;

/**
 * What a contract asks of a caller, operation by operation (SECURITY.md
 * §2): the operation's own requirement where it states one, else the
 * interface's default.
 *
 * <p>The rule is written into two OCL invariants of the model as well
 * ({@code authenticatedContractsHaveAScheme}, {@code scopesNeedAPerCallScheme}),
 * but those check a document; this answers a call. One place in Java,
 * so the serving side, the exporting side and a consumer reading the
 * contract cannot read it differently.
 *
 * <p>The names in {@code scopes} are whatever the verifier reads from the
 * token's scope claim — OAuth scopes, or roles when the claim is
 * {@code realm_access.roles}. Java's {@code @RolesAllowed} lands here.
 */
public final class SecurityRequirements {

	private SecurityRequirements() {
		// no instances
	}

	/** The requirement that applies to the operation, or {@code null} when neither it nor its interface states one. */
	public static SecurityRequirement effective(ServiceOperation operation) {
		if (operation == null) {
			return null;
		}
		if (operation.getSecurity() != null) {
			return operation.getSecurity();
		}
		return operation.eContainer() instanceof ServiceInterface contract ? contract.getSecurity() : null;
	}

	/**
	 * Whether calling the operation needs a verified principal. Scopes
	 * presuppose one, whatever {@code authenticated} says
	 * ({@code scopesImplyAuthenticated}).
	 */
	public static boolean demandsAuthentication(ServiceOperation operation) {
		SecurityRequirement requirement = effective(operation);
		return requirement != null && (requirement.isAuthenticated() || !requirement.getScopes().isEmpty());
	}

	/** Whether any operation of the contract needs a verified principal. */
	public static boolean demandsAuthentication(ServiceInterface contract) {
		return contract != null && contract.getOperations().stream().anyMatch(SecurityRequirements::demandsAuthentication);
	}

	/** The scopes the operation asks for that {@code held} does not contain; empty means satisfied. */
	public static List<String> missingScopes(ServiceOperation operation, Collection<String> held) {
		SecurityRequirement requirement = effective(operation);
		List<String> missing = new ArrayList<>();
		if (requirement != null) {
			for (String scope : requirement.getScopes()) {
				if (held == null || !held.contains(scope)) {
					missing.add(scope);
				}
			}
		}
		return missing;
	}

	/** The first bearer scheme the flavor offers, or {@code null}. */
	public static BearerTokenScheme bearerScheme(ServiceFlavor flavor) {
		if (flavor == null) {
			return null;
		}
		return flavor.getSecuritySchemes().stream()
				.filter(BearerTokenScheme.class::isInstance)
				.map(BearerTokenScheme.class::cast)
				.findFirst().orElse(null);
	}

	/**
	 * The client-certificate scheme a flavor offers, or {@code null}: a
	 * caller of it proves itself on the TLS connection, with nothing in
	 * the request (SECURITY.md §7d).
	 */
	public static MutualTlsScheme mutualTlsScheme(ServiceFlavor flavor) {
		if (flavor == null) {
			return null;
		}
		return flavor.getSecuritySchemes().stream()
				.filter(MutualTlsScheme.class::isInstance)
				.map(MutualTlsScheme.class::cast)
				.findFirst().orElse(null);
	}
}
