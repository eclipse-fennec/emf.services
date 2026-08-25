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

package org.eclipse.fennec.services.broker.core;

import org.eclipse.fennec.services.ServiceEvent;

/**
 * Where the broker hands its lifecycle events for distribution.
 * <p>
 * Deliberately transport-agnostic. SSE is the standard transport per
 * {@code REQUIREMENTS.md} FR-Sync-Transport, but it is one
 * implementation of this interface, not the mechanism itself — a
 * WebSocket or MQTT sink is an additional implementation and needs no
 * change in {@code broker.core}. That is also the direction
 * {@code UPDATE_POLICY.md §8} points: the event stream becomes a
 * concrete event channel once the channel model from
 * {@code WIRE_CHANNELS.md} lands.
 * <p>
 * Contract:
 * <ul>
 *   <li>The broker calls {@link #publish} only for mutations it has
 *       <em>acknowledged and persisted</em>. A rejected or unsaved
 *       change produces no event.</li>
 *   <li>The call happens while the broker still holds its write lock,
 *       so events for one service arrive in the order the mutations
 *       were applied. An implementation must therefore not block: hand
 *       the event to a queue or a non-blocking write and return.</li>
 *   <li>An implementation must not throw. The broker treats a sink as
 *       untrusted for its own consistency — a failing subscriber may
 *       not roll back a committed mutation.</li>
 *   <li>The {@link ServiceEvent} and the {@code ServiceReference} it
 *       points at are live broker objects. Read them, and copy before
 *       retaining anything beyond the call.</li>
 * </ul>
 */
public interface EventSink {

	/** A sink that discards everything — the default when none is wired. */
	EventSink NOOP = event -> {
	};

	/**
	 * Distribute one lifecycle event. Must not block and must not throw.
	 *
	 * @param event the event to distribute; never {@code null}
	 */
	void publish(ServiceEvent event);
}
