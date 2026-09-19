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

import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;

/**
 * Taking a live registration away, as the concerns that decide to do so
 * need it.
 *
 * <p>Four of them do, for four different reasons: a republish of the
 * same identity, a cutover, a drain that finished, and a provider that
 * went silent. Only one place knows how — detach the implementation,
 * drop the registration, release the leases, unindex it — and this is
 * the name that place answers to. Without it, every one of those four
 * would either repeat the removal or reach into the registration side
 * directly.
 *
 * <p>Called with the broker's write lock held, and it neither saves nor
 * announces: the caller does both, because only the caller knows what
 * to say and whether the change survived being saved.
 */
interface Retirement {

	/**
	 * Removes the implementation from the live registry.
	 *
	 * @return the reference that stops being valid, or {@code null} when
	 *         there was no registration for it
	 */
	ServiceReference retire(ServiceProvider provider, ServiceImplementation implementation);
}
