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

package org.eclipse.fennec.services.broker.core.internal;

import java.nio.file.Path;

/**
 * Everything a broker needs to be told, as a value.
 *
 * <p>The point of it being a value is that a broker behaves the same
 * with and without OSGi. The component reads these from Configuration
 * Admin; a plain-Java caller passes them directly. Neither gets a
 * broker that behaves differently from the other's.
 *
 * @param snapshotPath where the registry is saved
 * @param sessionExpirySeconds how long a session survives without a renewal; 0 switches expiry off
 * @param sessionDisconnectGraceSeconds how long a consumer may be disconnected before its session goes; 0 switches it off
 * @param coldAfterSeconds how long a registration may be idle before it is parked; 0 switches the cold cache off
 * @param policySweepSeconds how often armed handovers are advanced; 0 switches the sweep off
 * @param livenessSweepSeconds how often provider leases are checked; 0 switches the sweep off
 * @param cutoverGraceMillis the default failover window of a hard cutover
 */
public record BrokerSettings(
		Path snapshotPath,
		long sessionExpirySeconds,
		long sessionDisconnectGraceSeconds,
		long coldAfterSeconds,
		long policySweepSeconds,
		long livenessSweepSeconds,
		long cutoverGraceMillis) {

	/** What a broker does when nobody says otherwise. */
	public static BrokerSettings defaults(Path snapshotPath) {
		return new BrokerSettings(snapshotPath, 1200, 60, 0, 5, 5,
				UpdatePolicies.DEFAULT_CUTOVER_GRACE_MILLIS);
	}

	/** Whether any of the periodic work is switched on at all. */
	boolean anySweepEnabled() {
		return sessionExpirySeconds > 0 || coldAfterSeconds > 0
				|| policySweepSeconds > 0 || livenessSweepSeconds > 0;
	}
}
