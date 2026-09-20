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

/**
 * What this implementation adds to an {@code EndpointDescription}.
 *
 * <p>The specification's properties say that an endpoint exists and
 * which interfaces it has. These two say where it is in <em>this</em>
 * registry, so that a consumer of ours can look it up in the broker
 * instead of reconstructing it from a flat map — and so that a topology
 * manager can build a description for something discovery found without
 * ever having seen the exporting side.
 */
public final class RsaProperties {

	/** The contract's name in the catalog. */
	public static final String CONTRACT = "ddsr.contract";

	/** The implementation id the broker registered it under. */
	public static final String IMPLEMENTATION = "ddsr.implementation";

	/**
	 * Which framework exported this — on the announced implementation,
	 * so that a node can tell its own exports from everyone else's.
	 *
	 * <p>The name is the specification's own
	 * ({@code endpoint.framework.uuid}, 122.10), because that is what an
	 * `EndpointDescription` carries it as and what a foreign reader
	 * recognises. Without it a node that both exports a contract and
	 * waits for one imports itself: a proxy that leaves the framework,
	 * crosses the network and comes back to the service it started
	 * from.
	 */
	public static final String FRAMEWORK_UUID = "endpoint.framework.uuid";

	private RsaProperties() {
	}
}
