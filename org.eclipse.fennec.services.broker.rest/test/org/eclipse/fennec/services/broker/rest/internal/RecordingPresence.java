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

package org.eclipse.fennec.services.broker.rest.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.broker.core.BrokerSessions;

/**
 * Records which consumers the SSE bridge says are connected.
 *
 * <p>Only the two presence methods matter here; the rest of the session
 * protocol is exercised where it lives, in the broker's own tests.
 */
final class RecordingPresence implements BrokerSessions {

	final List<String> connected = new ArrayList<>();

	final List<String> disconnected = new ArrayList<>();

	/**
	 * Who currently carries a disconnect deadline, the way the broker's
	 * session registry holds it.
	 *
	 * <p>This is the state that matters. A disconnect followed by a
	 * reconnect is harmless — the deadline is cleared again. What must
	 * never happen is a consumer left marked while it holds a stream,
	 * because that expires its session and releases its leases
	 * underneath it (#127).
	 */
	final Set<String> marked = new LinkedHashSet<>();

	@Override
	public void consumerConnected(String consumerId) {
		connected.add(consumerId);
		marked.remove(consumerId);
	}

	@Override
	public void consumerDisconnected(String consumerId) {
		disconnected.add(consumerId);
		marked.add(consumerId);
	}

	@Override
	public Diagnostic putSession(ConsumerSession session, Collection<String> acquiredReferenceIds) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Diagnostic deleteSession(String consumerId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<SessionSnapshot> getSession(String consumerId) {
		return Optional.empty();
	}

	@Override
	public int expireSessions(Instant cutoff) {
		return 0;
	}

	@Override
	public int sessionCount() {
		return 0;
	}
}
