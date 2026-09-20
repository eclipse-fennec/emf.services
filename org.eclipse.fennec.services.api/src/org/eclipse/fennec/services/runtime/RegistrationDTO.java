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

package org.eclipse.fennec.services.runtime;

import java.util.List;

import org.osgi.dto.DTO;

/**
 * One live registration, and who is holding on to it.
 *
 * <p>The identifiers are the ones that appear everywhere else: the
 * reference id a consumer binds by, the implementation id a provider
 * republishes under, and the two fingerprints — sd1 addresses the
 * contract, im1 the implementation.
 */
public class RegistrationDTO extends DTO {

	/** What a consumer binds by. Not stable across a broker restart. */
	public String referenceId;

	/** Who published it. */
	public String providerName;

	/** Provider, contract and version, as the provider named itself. */
	public String implementationId;

	public String version;

	/** The contracts this registration serves. */
	public List<String> contracts;

	/** The transports it is reachable over, as the model names them. */
	public List<String> flavors;

	/** sd1 of the contract, as the broker computed it. */
	public String contractFingerprint;

	/** im1 of the implementation. */
	public String implementationFingerprint;

	/** What happens to consumers when a successor arrives. */
	public String updatePolicy;

	/** The consumers holding a lease on it. */
	public List<String> heldBy;

	/**
	 * When the provider last said it was alive, in epoch milliseconds,
	 * or 0 for a registration that does not heartbeat.
	 *
	 * <p>Opt-in by design: a provider that never heartbeats is never
	 * declared lost, so an absent lease here is a statement about the
	 * provider rather than about its health.
	 */
	public long lastHeartbeat;

	/** How often the provider said it would report in, in seconds. */
	public long heartbeatIntervalSeconds;
}
