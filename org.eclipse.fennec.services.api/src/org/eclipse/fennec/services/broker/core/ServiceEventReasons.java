/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.broker.core;

import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;

/**
 * The broker's vocabulary for {@link ServiceEvent#getReasonCode()}.
 * <p>
 * Every {@link ServiceEventType#UNREGISTERING} the broker emits carries
 * one of these tokens so a consumer can tell "gone for good" from
 * "replaced" from "parked". {@code REGISTERED} and {@code MODIFIED}
 * carry no reason. The tokens are wire values (see docs/WIRE_FORMAT.md)
 * and the model documentation of {@code ServiceEvent.reasonCode}; the
 * attribute is deliberately a free-form string so transports and future
 * policies can add tokens without a model change.
 */
public final class ServiceEventReasons {

	private ServiceEventReasons() {
	}

	/** The provider withdrew the implementation (explicit withdraw or its shutdown hook). */
	public static final String WITHDRAWN = "WITHDRAWN";

	/**
	 * The implementation was superseded: a republish under the same
	 * (name, version) retired the old copy, or a DEPRECATE_AND_DRAIN
	 * drain finished (the latter once update policies are enforced).
	 */
	public static final String REPLACED = "REPLACED";

	/** A HARD_CUTOVER grace window elapsed. */
	public static final String CUTOVER = "CUTOVER";

	/**
	 * The idle sweep moved the entry to the cold cache. It stays
	 * discoverable through its stub and rehydrates on the next lookup
	 * with a fresh {@code REGISTERED}.
	 */
	public static final String COLDIFIED = "COLDIFIED";

	/** A consumer session lease expired (reserved; sessions do not emit service events today). */
	public static final String SESSION_EXPIRED = "SESSION_EXPIRED";

	/** The provider stopped answering: two missed heartbeats, swept by Liveness. */
	public static final String PROVIDER_LOST = "PROVIDER_LOST";
}
