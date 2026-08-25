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

package org.eclipse.fennec.services.client;

import java.util.List;
import java.util.Optional;

/**
 * Consumer-side facade. The consumer enumerates available services
 * and (post-Increment-3) subscribes to lifecycle events.
 *
 * <p>{@link ServiceLocator} bundles a reference with its owning
 * implementation and transport flavor, so the consumer has the full
 * URL / topic mapping in hand and can invoke without an additional
 * roundtrip.
 */
public interface DdsrConsumer {

	/**
	 * Look up all references for {@code interfaceName}, optionally
	 * narrowed by an LDAP {@code filter} (currently passed through to
	 * the broker; broker support is no-op as of writing).
	 *
	 * <p>The consumer's installed flavor plugins determine what gets
	 * returned: a REST-only client gets only REST-flavored implementations.
	 *
	 * <p>Returns an empty list (never null) if nothing matches.
	 */
	List<ServiceLocator> find(String interfaceName, String filter);

	/**
	 * Convenience for "give me one if any" — returns the first locator
	 * from {@link #find} or empty.
	 */
	/**
	 * Subscribes to lifecycle events for {@code interfaceName}.
	 * <p>
	 * The stream is opened on the first listener and closed with the
	 * last. Delivery needs an event transport to be installed
	 * ({@link EventSource}); without one, registration is accepted but
	 * nothing arrives — a client that cannot stream should not fail, it
	 * should just not notify.
	 *
	 * @param interfaceName the {@code ServiceInterface.name} to watch
	 * @param filter        reserved for an LDAP filter; not evaluated yet
	 * @param listener      receives the events
	 * @return a handle; closing it removes the listener
	 */
	AutoCloseable addServiceListener(String interfaceName, String filter, DdsrServiceListener listener);

	/**
	 * Removes a listener registered with
	 * {@link #addServiceListener}. Equivalent to closing the handle that
	 * call returned; both exist because the model describes
	 * add/remove and callers sometimes have only the listener at hand.
	 */
	void removeServiceListener(DdsrServiceListener listener);

	default Optional<ServiceLocator> findFirst(String interfaceName, String filter) {
		List<ServiceLocator> hits = find(interfaceName, filter);
		return hits.isEmpty() ? Optional.empty() : Optional.of(hits.get(0));
	}
}
