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
 * The names by which the filter is found and the principal is handed
 * on — in one place, so that a resource selecting the filter and the
 * filter itself cannot spell them differently.
 */
public final class SecurityNames {

	/** The whiteboard name of the bearer filter ({@code osgi.jakartars.name}). */
	public static final String BEARER_FILTER = "fennec-bearer";

	/**
	 * The application property that asks for the filter. An
	 * application without it is not touched: the broker's own
	 * application says it, the generic distributions say it per served
	 * contract, a demo may not.
	 */
	public static final String SECURED = "fennec.secured";

	/** The request property the filter sets: the {@link VerifiedPrincipal}. */
	public static final String REQUEST_PRINCIPAL = "fennec.principal";

	/** The extension selector a resource uses to stay unregistered without the filter — fail closed. */
	public static final String SELECT_BEARER_FILTER = "(osgi.jakartars.name=" + BEARER_FILTER + ")";

	private SecurityNames() {
		// no instances
	}
}
