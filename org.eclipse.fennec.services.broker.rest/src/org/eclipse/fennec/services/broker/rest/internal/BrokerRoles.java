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
package org.eclipse.fennec.services.broker.rest.internal;

import org.eclipse.fennec.services.Principal;
import org.eclipse.fennec.services.common.CallPrincipal;

import jakarta.ws.rs.core.Response;

/**
 * The broker's three roles (SECURITY.md §3), as the realm names them and
 * as its own contract documents ask for them.
 *
 * <p>The three contracts the generic distribution serves say who may call
 * in their documents, and the dispatcher holds them to it. The
 * hand-written resources — sessions, the registry snapshot — are not
 * served from a document, so they ask here, by the same rule: no
 * principal is mode none (the bearer filter turns a request without a
 * token away before it gets here), a principal without the role is 403.
 */
final class BrokerRoles {

	/** May announce, change, withdraw and keep alive implementations, and add contracts. */
	static final String PROVIDER = "service-provider";

	/** May look up, hold a session, and hear the event stream. */
	static final String CONSUMER = "service-consumer";

	/** May curate the catalog and read the whole registry. */
	static final String ADMIN = "service-admin";

	private BrokerRoles() {
		// no instances
	}

	/** {@code null} when the current caller may go on, else the 403 that says why not. */
	static Response refusal(String role) {
		Principal caller = CallPrincipal.current();
		if (caller == null || caller.getScopes().contains(role)) {
			return null;
		}
		return Response.status(Response.Status.FORBIDDEN).entity("this needs " + role).build();
	}
}
