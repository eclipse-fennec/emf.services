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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.ConnectionScheme;
import org.eclipse.fennec.services.MutualTlsScheme;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.SecuritySchemeKind;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.ServicesFactory;
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

	static final String MTLS = "mtls";

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
		if (BEARER.equals(mode) || BEARER_OPTIONAL.equals(mode) || MTLS.equals(mode)) {
			return true;
		}
		if (demands && mode == null) {
			throw new IllegalStateException(contract + " demands authentication, and its configuration states no "
					+ AUTH + " — say " + BEARER + " or " + MTLS + ", or " + NONE + " for an open node");
		}
		if (demands) {
			LOG.warning("[DDSR] " + contract + " demands authentication but is served in mode " + mode
					+ " — its requirements are passed over on this node");
		}
		return false;
	}

	/**
	 * What the announced flavor tells a caller about proving itself, when
	 * the mode came from configuration: a document is written once and
	 * served in many deployments, so the issuer a bearer node trusts, or
	 * the authorities an mtls node accepts, are deployment facts the
	 * document cannot carry. A scheme the document does name is left as
	 * the author wrote it.
	 *
	 * <p>Without this a caller cannot tell from the catalog which token to
	 * bring — the client asks for one per audience of the flavor's bearer
	 * scheme — and would call without one.
	 */
	static void announceScheme(String contract, RestFlavor flavor, Object mode, String issuer, String[] audiences,
			String jwksUri, String scopeClaim, String[] trustedIssuers) {
		if (BEARER.equals(mode) || BEARER_OPTIONAL.equals(mode)) {
			if (SecurityRequirements.bearerScheme(flavor) != null) {
				return;
			}
			if (issuer == null || issuer.isBlank()) {
				LOG.warning("[DDSR] " + contract + " is served in mode " + mode + " and announced without a"
						+ " bearer scheme — configure its issuer, or callers cannot tell which token to bring");
				return;
			}
			BearerTokenScheme bearer = ServicesFactory.eINSTANCE.createBearerTokenScheme();
			bearer.setName(BEARER);
			bearer.setKind(SecuritySchemeKind.BEARER_TOKEN);
			bearer.setIssuer(issuer.strip());
			bearer.getAudiences().addAll(nonBlank(audiences));
			bearer.setJwksUri(jwksUri == null || jwksUri.isBlank() ? null : jwksUri.strip());
			bearer.setScopeClaim(scopeClaim == null || scopeClaim.isBlank() ? null : scopeClaim.strip());
			flavor.getSecuritySchemes().add(bearer);
		} else if (MTLS.equals(mode) && SecurityRequirements.mutualTlsScheme(flavor) == null) {
			MutualTlsScheme certificates = ServicesFactory.eINSTANCE.createMutualTlsScheme();
			certificates.setName(MTLS);
			certificates.setKind(SecuritySchemeKind.MUTUAL_TLS);
			certificates.getTrustedIssuers().addAll(nonBlank(trustedIssuers));
			flavor.getSecuritySchemes().add(certificates);
		}
	}

	private static List<String> nonBlank(String[] values) {
		List<String> kept = new ArrayList<>();
		for (String value : values == null ? new String[0] : values) {
			if (value != null && !value.isBlank()) {
				kept.add(value.strip());
			}
		}
		return kept;
	}

	/**
	 * The mode a contract served from a flavor runs in (the RSA path):
	 * bearer when the flavor offers a bearer scheme, which also puts the
	 * mode on the application — {@value #BEARER} when every operation
	 * needs a principal, {@value #BEARER_OPTIONAL} when some are open
	 * ({@code @PermitAll}), so their anonymous callers reach the dispatcher.
	 * A flavor that offers client certificates instead runs in
	 * {@value #MTLS}; a certificate belongs to the connection, so there
	 * every caller has one and no operation is reached without it. A
	 * contract that demands authentication on a flavor with no scheme is
	 * refused; a connection scheme serves it open, because something in
	 * front of the node checks.
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
		if (SecurityRequirements.mutualTlsScheme(flavor) != null) {
			properties.put(AUTH, MTLS);
			properties.put(EXTENSION_SELECT, selectorFor(MTLS));
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
