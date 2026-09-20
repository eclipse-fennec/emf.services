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
 *   <li>Calls arrive on the broker's delivery thread, one at a time and
 *       in the order the mutations were applied. Since #124 that is no
 *       longer the broker's own thread under its write lock, so an
 *       implementation MAY take its time — waiting for a transport to
 *       confirm is allowed and often the honest thing to do. What it
 *       costs is delivery: everything behind it waits, and a sink that
 *       stops reading altogether eventually loses events to the
 *       delivery queue's bound.</li>
 *   <li>An implementation must not throw. The broker treats a sink as
 *       untrusted for its own consistency — a failing subscriber may
 *       not roll back a committed mutation.</li>
 *   <li>The {@link ServiceEvent} and everything it points at are a
 *       detached copy, made when the event happened. Read it, keep it,
 *       render it later: it describes that moment and nothing will
 *       change under it.</li>
 * </ul>
 */
public interface EventSink {

	/** A sink that discards everything — the default when none is wired. */
	EventSink NOOP = event -> {
	};

	/**
	 * Distribute one lifecycle event. Must not throw.
	 *
	 * @param event the event to distribute; never {@code null}
	 */
	void publish(ServiceEvent event);

	/**
	 * Something was lost and the subscribers have to re-read.
	 *
	 * <p>Called when the broker knows an event did not reach the wire:
	 * the delivery queue dropped it because a subscriber stopped
	 * reading, or a transport could not put it out. A consumer cannot
	 * notice that by itself — the broker keeps no per-client history and
	 * the stream carries no sequence numbers — so the only honest
	 * recovery is the one FR-Sync-Reconnect already defines: tell them
	 * to take a fresh snapshot.
	 *
	 * <p>Optional. A sink that has no way to say this does nothing, and
	 * silence is then the truthful answer rather than a pretended one.
	 * A sink that can say it should also remember that it owes the
	 * signal when saying it fails, and send it at the next opportunity:
	 * a transport that just failed to publish an event will usually
	 * fail to publish the warning about it too.
	 */
	default void resyncRequired() {
		// Nothing to say it with.
	}
}
