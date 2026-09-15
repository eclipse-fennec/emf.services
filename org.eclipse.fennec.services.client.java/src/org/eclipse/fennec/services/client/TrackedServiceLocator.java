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
package org.eclipse.fennec.services.client;

/**
 * A {@link ServiceLocator} that follows its service (#57). Locators
 * returned by {@link DdsrConsumer#find} are bound to one registration
 * and watch the event stream for it:
 * <ul>
 *   <li>{@code MODIFIED} — the registration changed in place (endpoint,
 *       properties); the locator refreshes reference and implementation,
 *       the reference id is unchanged.</li>
 *   <li>{@code UNREGISTERING/COLDIFIED} — parked in the broker's cold
 *       cache, not gone; the next use re-looks-up, which rehydrates it
 *       under a fresh reference id.</li>
 *   <li>{@code UNREGISTERING} with any other reason, {@code RETIRED} —
 *       gone; the next use rebinds to another registration of the same
 *       interface and filter (after {@code REPLACED} that is typically
 *       the successor that was announced right after).</li>
 *   <li>{@code UPGRADE_AVAILABLE} — a successor exists; a <em>greedy</em>
 *       consumer rebinds on next use, a non-greedy one keeps the binding
 *       until the broker retires it.</li>
 *   <li>A transport failure on invocation — the proxy asks the locator
 *       to rebind away from the failed registration and retries once (#59).</li>
 * </ul>
 * Rebinding is lazy: nothing talks to the broker until the locator is
 * used again. {@link #reference()} returns the last known reference
 * without contacting the broker; the transport accessors rebind first.
 */
public interface TrackedServiceLocator extends ServiceLocator {

	/** Why the locator is not (or no longer) bound to a live registration. */
	enum State {
		/** Bound to a live registration. */
		LIVE,
		/** The registration changed in place; refresh from the broker on next use, same id preferred. */
		MODIFIED,
		/** Parked in the cold cache; a lookup rehydrates it under a fresh id. */
		STALE,
		/** Gone or superseded; bind to another registration of the same interface and filter. */
		REBIND
	}

	State state();

	/** The interface this locator was found for. */
	String interfaceName();

	/** The LDAP filter this locator was found with, or {@code null}. */
	String filter();

	/**
	 * Re-resolves against the broker now. With {@code excludeCurrent} the
	 * registration this locator is bound to is skipped even if the broker
	 * still lists it (transport failure, greedy upgrade).
	 *
	 * @return whether the locator is bound to a live registration afterwards
	 */
	boolean rebind(boolean excludeCurrent);
}
