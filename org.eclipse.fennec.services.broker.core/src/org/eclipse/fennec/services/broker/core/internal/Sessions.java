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
import java.util.Iterator;
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
		state.writeLock().lock();
		try {
			int expired = 0;
			var iterator = sessions.entrySet().iterator();
			while (iterator.hasNext()) {
				ConsumerSession session = iterator.next().getValue();
				Date lastRenewal = session.getLastRenewal();
				if (lastRenewal == null || lastRenewal.toInstant().isBefore(cutoff)) {
					releaseAcquisitions(session);
					iterator.remove();
					expired++;
				}
			}
			return expired;
		} finally {
			state.writeLock().unlock();
		}
	}
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
