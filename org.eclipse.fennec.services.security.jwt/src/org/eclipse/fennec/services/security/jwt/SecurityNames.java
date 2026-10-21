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
package org.eclipse.fennec.services.security.jwt;

/**
 * The names by which an application states its authentication and a
 * filter finds the applications it guards — in one place, so that a
 * configuration, a resource and the filters cannot spell them
 * differently.
 *
 * <p>An application that wants authentication names its mode twice,
 * and both are needed:
 * <ul>
 * <li>{@link #AUTH}{@code =<mode>} is what a filter selects, so the
 * filter for that mode runs on the application;
 * <li>{@code osgi.jakartars.extension.select=(osgi.jakartars.name=fennec-auth-<mode>)}
 * ({@link #selectFilterFor}) makes the whiteboard keep
 * the application down until that filter is there. Without it, an
 * application asking for bearer on a node without a verifier would be
 * deployed unguarded — fail open.
 * </ul>
 *
 * <p>{@code none} is a mode like any other, with a filter of its own,
 * rather than the absence of one: a configuration that turns
 * authentication off says so in the same words that turn it on, and
 * the value can come from one placeholder ({@code $[env:...]}) for
 * both properties (SECURITY.md §7a).
 */
public final class SecurityNames {

	/**
	 * The application property naming its authentication mode:
	 * {@value #BEARER}, {@value #BEARER_OPTIONAL}, {@value #MTLS} or
	 * {@value #NONE}.
	 */
	public static final String AUTH = "fennec.auth";

	/** Mode: every request carries a bearer token the verifier accepts. */
	public static final String BEARER = "bearer";

	/**
	 * Mode: a token, when there is one, must verify; without one the call
	 * goes on with no principal, and the contract's requirements decide
	 * per operation. For applications in which every request is answered
	 * by the generic dispatcher, which holds each operation to its
	 * requirement — the mode that lets a {@code @PermitAll} operation sit
	 * next to guarded ones.
	 */
	public static final String BEARER_OPTIONAL = "bearer-optional";

	/**
	 * Mode: every request comes with a client certificate (SECURITY.md
	 * §7d) — from the TLS session, or from a trusted gateway that ended
	 * it. The principal is the certificate's subject, its roles the
	 * node's own mapping. A certificate is a credential of the
	 * connection, so there is no optional variant: a TLS listener that
	 * demands one admits nobody without it.
	 */
	public static final String MTLS = "mtls";

	/** Mode: no authentication, chosen explicitly by the deployment. */
	public static final String NONE = "none";

	/** The prefix of the filters' whiteboard names ({@code osgi.jakartars.name}). */
	public static final String FILTER_PREFIX = "fennec-auth-";

	/** The whiteboard name of the bearer filter. */
	public static final String BEARER_FILTER = FILTER_PREFIX + BEARER;

	/** The whiteboard name of the filter for {@value #BEARER_OPTIONAL}. */
	public static final String OPTIONAL_BEARER_FILTER = FILTER_PREFIX + BEARER_OPTIONAL;

	/** The whiteboard name of the filter for {@value #MTLS}. */
	public static final String MTLS_FILTER = FILTER_PREFIX + MTLS;

	/** The whiteboard name of the filter for {@value #NONE}. */
	public static final String NO_AUTH_FILTER = FILTER_PREFIX + NONE;

	/**
	 * The whiteboard name of the feature that holds annotated resource
	 * methods to their {@code @RolesAllowed} / {@code @PermitAll}, on
	 * every application that verifies its callers.
	 */
	public static final String ROLES_FILTER = "fennec-roles";

	/** The applications that verify a caller: a bearer mode or mutual TLS. */
	public static final String SELECT_VERIFYING = "(|(" + AUTH + "=" + BEARER + "*)(" + AUTH + "=" + MTLS + "))";

	/** The request property the bearer filter sets: the {@link VerifiedPrincipal}. */
	public static final String REQUEST_PRINCIPAL = "fennec.principal";

	/**
	 * The extension selector of a resource that must not be served by
	 * an application without a stated mode. The whiteboard evaluates it
	 * per application, and a filter only counts for an application it
	 * selects — so on a {@code bearer} application only the bearer
	 * filter satisfies it, and on an application that states nothing
	 * neither does.
	 */
	public static final String SELECT_ANY_AUTH = "(osgi.jakartars.name=" + FILTER_PREFIX + "*)";

	private SecurityNames() {
		// no instances
	}

	/** The extension selector an application of the given mode carries. */
	public static String selectFilterFor(String mode) {
		return "(osgi.jakartars.name=" + FILTER_PREFIX + mode + ")";
	}
}
