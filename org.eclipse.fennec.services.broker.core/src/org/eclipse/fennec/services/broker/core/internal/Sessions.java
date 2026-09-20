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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerSessions.SessionSnapshot;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;

/**
 * Who has acquired what, and for how long.
 *
 * <p>Runtime state on purpose (ACQUISITION.md §6): sessions are never
 * persisted and never announced. A broker restart forgets them, and
 * consumers rebuild theirs with their next regular PUT. Nothing here
 * saves the registry, which is what makes this the one concern that
 * cannot corrupt a snapshot.
 *
 * <p>The acquisitions themselves do live in the model, as the
 * eOpposite of a registration's {@code usingSessions}. That is why
 * withdrawing or retiring a registration releases leases without going
 * through this class: the model does it.
 */
final class Sessions {

	private final BrokerState state;

	Sessions(BrokerState state) {
		this.state = state;
	}

	/**
	 * Consumer sessions by consumerId (ACQUISITION.md §3/§4). Runtime
	 * state by design: deliberately NOT part of the persisted registry —
	 * after a restart, consumers rebuild their sessions via their
	 * regular PUTs. Guarded by the broker lock.
	 */
	private final Map<String, ConsumerSession> sessions = new LinkedHashMap<>();

	Diagnostic putSession(ConsumerSession session, Collection<String> acquiredReferenceIds) {
		if (session == null || session.getConsumerId() == null || session.getConsumerId().isBlank()) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_SESSION_INVALID,
					"session and session.consumerId must not be null or blank");
		}
		state.writeLock().lock();
		try {
			String consumerId = session.getConsumerId();
			// Full replace: the previous session's leases are released
			// first — the incoming list is the complete, current truth.
			ConsumerSession previous = sessions.remove(consumerId);
			if (previous != null) {
				releaseAcquisitions(previous);
			}
			session.setLastRenewal(new Date());
			int accepted = 0;
			List<String> unknown = new ArrayList<>();
			if (acquiredReferenceIds != null) {
				for (String referenceId : acquiredReferenceIds) {
					ServiceRegistration registration = state.registrationWithReferenceId(referenceId);
					if (registration == null) {
						// Over-claiming is harmless: stale or foreign ids
						// (e.g. from before a broker restart) are skipped
						// and reported, never rejected (ACQUISITION.md §5).
						unknown.add(referenceId);
						continue;
					}
					if (!session.getAcquisitions().contains(registration)) {
						session.getAcquisitions().add(registration);
						accepted++;
					}
				}
			}
			sessions.put(consumerId, session);
			// A fresh session is a live consumer, whatever its connection
			// did a moment ago.
			disconnectedSince.remove(consumerId);
			// Deliberately NO persist and NO event: sessions are runtime
			// state (ACQUISITION.md §6).
			return DdsrDiagnostics.ok("session accepted, " + accepted + " acquisition(s)"
					+ (unknown.isEmpty() ? "" : ", skipped unknown reference id(s): " + unknown));
		} finally {
			state.writeLock().unlock();
		}
	}
	Diagnostic deleteSession(String consumerId) {
		if (consumerId == null || consumerId.isBlank()) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_SESSION_INVALID,
					"consumerId must not be null or blank");
		}
		state.writeLock().lock();
		try {
			ConsumerSession removed = sessions.remove(consumerId);
			if (removed == null) {
				// Idempotent: a shutdown-notify may race the TTL expiry.
				return DdsrDiagnostics.ok("no session for '" + consumerId + "' — nothing to release");
			}
			releaseAcquisitions(removed);
			return DdsrDiagnostics.ok("session removed, all acquisitions released");
		} finally {
			state.writeLock().unlock();
		}
	}
	Optional<SessionSnapshot> getSession(String consumerId) {
		if (consumerId == null || consumerId.isBlank()) {
			return Optional.empty();
		}
		state.readLock().lock();
		try {
			ConsumerSession stored = sessions.get(consumerId);
			if (stored == null) {
				return Optional.empty();
			}
			// Manual detached copy — NOT EcoreUtil.copy: copying the
			// bidirectional (transient) acquisitions would touch the live
			// registrations' usingSessions via the eOpposite.
			ConsumerSession view = ServicesFactory.eINSTANCE.createConsumerSession();
			view.setConsumerId(stored.getConsumerId());
			view.setLastRenewal(stored.getLastRenewal());
			if (stored.getCapabilities() != null) {
				view.setCapabilities(EcoreUtil.copy(stored.getCapabilities()));
			}
			List<String> ids = new ArrayList<>(stored.getAcquisitions().size());
			for (ServiceRegistration registration : stored.getAcquisitions()) {
				ServiceReference reference = registration.getReference();
				if (reference != null && reference.getId() != null) {
					ids.add(reference.getId());
				}
			}
			return Optional.of(new SessionSnapshot(view, ids));
		} finally {
			state.readLock().unlock();
		}
	}
	int expireSessions(Instant cutoff) {
		if (cutoff == null) {
			return 0;
		}
		Instant now = Instant.now();
		state.writeLock().lock();
		try {
			int expired = 0;
			var iterator = sessions.entrySet().iterator();
			while (iterator.hasNext()) {
				var entry = iterator.next();
				ConsumerSession session = entry.getValue();
				Date lastRenewal = session.getLastRenewal();
				boolean silentTooLong = lastRenewal == null || lastRenewal.toInstant().isBefore(cutoff);
				if (silentTooLong || goneTooLong(entry.getKey(), now)) {
					releaseAcquisitions(session);
					iterator.remove();
					disconnectedSince.remove(entry.getKey());
					expired++;
				}
			}
			return expired;
		} finally {
			state.writeLock().unlock();
		}
	}

	/**
	 * Whether this consumer's event connection has been gone long enough
	 * to stop waiting for the renewal interval.
	 *
	 * <p>Zero switches the shortcut off, and a consumer that never
	 * reported a connection is never affected — which is every consumer
	 * on a transport the broker sees no connection for.
	 */
	private boolean goneTooLong(String consumerId, Instant now) {
		if (disconnectGraceSeconds <= 0) {
			return false;
		}
		Instant since = disconnectedSince.get(consumerId);
		return since != null && !now.isBefore(since.plusSeconds(disconnectGraceSeconds));
	}

	void consumerConnected(String consumerId) {
		if (consumerId != null && !consumerId.isBlank()) {
			disconnectedSince.remove(consumerId);
		}
	}

	void consumerDisconnected(String consumerId) {
		if (consumerId == null || consumerId.isBlank()) {
			return;
		}
		// putIfAbsent: several subscriptions of one consumer may end one
		// after another, and the deadline should run from the first of
		// them, not be pushed back by each.
		disconnectedSince.putIfAbsent(consumerId, Instant.now());
	}

	/** How long a consumer may be disconnected before its session goes; 0 disables. */
	void disconnectGraceSeconds(long seconds) {
		this.disconnectGraceSeconds = seconds;
	}

	/**
	 * When a consumer's event connection went away, by consumer id.
	 *
	 * <p>Runtime state like the sessions themselves. A reconnect or a
	 * fresh session clears the entry, so the normal reconnect of a
	 * client that briefly lost its stream costs nothing.
	 */
	private final Map<String, Instant> disconnectedSince = new java.util.concurrent.ConcurrentHashMap<>();

	private volatile long disconnectGraceSeconds;
	int sessionCount() {
		state.readLock().lock();
		try {
			return sessions.size();
		} finally {
			state.readLock().unlock();
		}
	}
	/** Clears the acquisitions; the eOpposite removes the session from every registration's usingSessions. */
	private static void releaseAcquisitions(ConsumerSession session) {
		session.getAcquisitions().clear();
	}

}
