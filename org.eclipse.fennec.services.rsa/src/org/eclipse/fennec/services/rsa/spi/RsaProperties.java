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

	private RsaProperties() {
	}
}
