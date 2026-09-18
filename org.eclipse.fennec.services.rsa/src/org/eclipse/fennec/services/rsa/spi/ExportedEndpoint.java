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

package org.eclipse.fennec.services.rsa.spi;

import java.util.Map;

import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;

/**
 * A service that is reachable right now, and everything needed to say
 * so to someone else.
 *
 * <p>Deliberately the model, not a property map. Remote Service Admin
 * describes an endpoint as an {@code EndpointDescription} — a flat bag
 * of strings — and that is what leaves the framework. Between the parts
 * of this implementation the description is the contract and the
 * implementation themselves: discovery has to announce a
 * {@code ServiceImplementation} with its flavor, and flattening it into
 * properties only to parse it back would lose exactly what this registry
 * is for.
 */
public interface ExportedEndpoint extends AutoCloseable {

	/** What the service promises — derived from the exported interface. */
	ServiceInterface contract();

	/**
	 * How to reach it: one flavor, with the deployment facts filled in.
	 * This is what a discovery provider announces and what a consumer
	 * reads to build a call.
	 */
	ServiceImplementation implementation();

	/**
	 * What the {@code EndpointDescription} has to carry for this
	 * configuration type — the specification wants at least one property
	 * prefixed with the type's name, so that a consumer without our model
	 * still finds the address. For REST that is {@code fennec.rest.url}.
	 * Keys are expected to start with a supported configuration type.
	 */
	default Map<String, Object> properties() {
		return Map.of();
	}

	/** Take the endpoint down. Idempotent. */
	@Override
	void close();
}
