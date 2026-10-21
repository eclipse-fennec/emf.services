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
package org.eclipse.fennec.services.provider.rest;

import java.util.Dictionary;
import java.util.logging.Logger;

import org.eclipse.fennec.services.ConnectionScheme;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.common.SecurityRequirements;

/**
 * How a served contract is guarded, decided once when it is served
 * (SECURITY.md §4, §7a).
 *
 * <p>The application states its mode with the same two properties the
 * broker's applications carry — {@value #AUTH}, and an extension selector
 * requiring the filter of that mode — so a bearer application on a node
 * without a verifier is not deployed rather than deployed open. The
 * names are spelled out here rather than taken from the security bundle:
 * serving a contract must not need the verifier's bundle on a node that
 * serves only open contracts.
 */
final class ServedSecurity {

	static final String AUTH = "fennec.auth";

	static final String BEARER = "bearer";

	static final String BEARER_OPTIONAL = "bearer-optional";

	static final String NONE = "none";

	static final String EXTENSION_SELECT = "osgi.jakartars.extension.select";

	private static final Logger LOG = Logger.getLogger(ServedSecurity.class.getName());

	private ServedSecurity() {
		// no instances
	}

	/** Whether any operation the flavor serves needs a verified caller. */
	static boolean demandsAuthentication(RestFlavor flavor) {
		for (ServiceOperationFlavor operationFlavor : flavor.getOperationFlavors()) {
			if (SecurityRequirements.demandsAuthentication(operationFlavor.getOperation())) {
				return true;
			}
		}
		return false;
	}

	/** Whether every operation the flavor serves needs one — then the strict filter may turn the anonymous away. */
	static boolean everyOperationDemandsAuthentication(RestFlavor flavor) {
		for (ServiceOperationFlavor operationFlavor : flavor.getOperationFlavors()) {
			if (!SecurityRequirements.demandsAuthentication(operationFlavor.getOperation())) {
				return false;
			}
		}
		return !flavor.getOperationFlavors().isEmpty();
	}

	/** The extension selector an application of this mode carries. */
	static String selectorFor(String mode) {
		return "(osgi.jakartars.name=fennec-auth-" + mode + ")";
	}

	/**
	 * The mode a contract served from a document runs in, as its
	 * configuration states it. A contract that demands authentication is
	 * not served under no stated mode at all: open has to be said
	 * ({@value #NONE}), never assumed.
	 *
	 * @return whether the dispatcher enforces the contract's requirements
	 */
	static boolean enforcedByConfiguration(String contract, RestFlavor flavor, Object mode) {
		boolean demands = demandsAuthentication(flavor);
		if (BEARER.equals(mode) || BEARER_OPTIONAL.equals(mode)) {
			return true;
		}
		if (demands && mode == null) {
			throw new IllegalStateException(contract + " demands authentication, and its configuration states no "
					+ AUTH + " — say " + BEARER + ", or " + NONE + " for an open node");
		}
		if (demands) {
			LOG.warning("[DDSR] " + contract + " demands authentication but is served in mode " + mode
					+ " — its requirements are passed over on this node");
		}
		return false;
	}

	/**
	 * The mode a contract served from a flavor runs in (the RSA path):
	 * bearer when the flavor offers a bearer scheme, which also puts the
	 * mode on the application — {@value #BEARER} when every operation
	 * needs a principal, {@value #BEARER_OPTIONAL} when some are open
	 * ({@code @PermitAll}), so their anonymous callers reach the dispatcher. A contract that demands authentication on a
	 * flavor with no scheme is refused; a connection scheme serves it
	 * open, because something in front of the node checks.
	 *
	 * @return whether the dispatcher enforces the contract's requirements
	 */
	static boolean enforcedByFlavor(String contract, RestFlavor flavor, Dictionary<String, Object> properties) {
		if (SecurityRequirements.bearerScheme(flavor) != null) {
			String mode = everyOperationDemandsAuthentication(flavor) ? BEARER : BEARER_OPTIONAL;
			properties.put(AUTH, mode);
			properties.put(EXTENSION_SELECT, selectorFor(mode));
			return true;
		}
		if (!demandsAuthentication(flavor)) {
			return false;
		}
		boolean gateway = flavor.getSecuritySchemes().stream().anyMatch(ConnectionScheme.class::isInstance);
		if (!gateway) {
			throw new IllegalStateException(contract + " demands authentication, and its flavor offers no scheme"
					+ " to prove it with — not served");
		}
		LOG.info("[DDSR] " + contract + " is served behind a connection scheme — whatever is in front checks");
		return false;
	}
}
