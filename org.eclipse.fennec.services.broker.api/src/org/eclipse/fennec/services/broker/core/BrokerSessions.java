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

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;

/**
 * The acquisition stage of the broker (ACQUISITION.md §3/§4): consumer
 * sessions holding leases on service registrations.
 * <p>
 * Sessions are <b>runtime state by design</b> — they are never
 * persisted to the broker snapshot; after a broker restart consumers
 * rebuild them through their regular full-replace PUTs
 * (FR-Sync-Reconnect philosophy). Consequently, everything here is a
 * <b>cooperative protocol, not enforcement</b>: invocation runs
 * peer-to-peer past the broker; a lease buys drain semantics and
 * telemetry, not access control.
 * <p>
 * The one operation model is the idempotent full replace: acquire =
 * add a reference id to the list and PUT, release = remove and PUT,
 * heartbeat = an unchanged PUT. Over-claiming is harmless (unknown or
 * stale reference ids are skipped, not rejected — they are reported in
 * the diagnostic message); under-claiming only hurts the consumer
 * itself (it loses drain protection).
 */
public interface BrokerSessions {

	/** Detached view of a stored session, safe to serialize. */
	record SessionSnapshot(ConsumerSession session, List<String> acquiredReferenceIds) {
	}

	/**
	 * Creates or fully replaces the session for
	 * {@code session.getConsumerId()} and renews its lease. The
	 * acquisition list is given as <em>reference ids</em> (the wire
	 * carries sibling {@code ServiceReference} id-stubs, same convention
	 * as publish); the broker resolves them onto the live registrations.
	 * Unknown ids are skipped and listed in the diagnostic message.
	 *
	 * @param session              the wire session (consumerId required;
	 *                             capabilities optional). The broker takes
	 *                             ownership of the object.
	 * @param acquiredReferenceIds reference ids the consumer claims to use
	 * @return OK on success (message lists accepted/skipped counts);
	 *         ERROR with {@link DdsrDiagnostics#CODE_SESSION_INVALID} for
	 *         a null/blank consumerId
	 */
	Diagnostic putSession(ConsumerSession session, Collection<String> acquiredReferenceIds);

	/**
	 * Releases all acquisitions of the session and forgets it.
	 * Idempotent: deleting an unknown session is OK (message says so).
	 */
	Diagnostic deleteSession(String consumerId);

	/** Detached snapshot of the stored session, if present. */
	Optional<SessionSnapshot> getSession(String consumerId);

	/**
	 * Expires every session whose last renewal is before the cutoff,
	 * releasing its acquisitions. Called by the hosting component's
	 * scheduler; exposed for deterministic tests.
	 *
	 * @return the number of expired sessions
	 */
	int expireSessions(Instant cutoff);

	/** Number of live sessions (diagnostics/tests). */
	int sessionCount();
}
