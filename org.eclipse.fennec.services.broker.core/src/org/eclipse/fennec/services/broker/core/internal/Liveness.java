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
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.ServiceEventReasons;

/**
 * Whether a provider is still there.
 *
 * <p>Opt-in: only a provider that sends heartbeats is watched, and the
 * lease it gets is runtime state like a session. A restart forgets
 * every lease, and the providers' next heartbeat rebuilds them by way
 * of a 404 that tells them to publish again.
 *
 * <p>Silence is the only signal there is. Two missed intervals and the
 * registration is retired as PROVIDER_LOST, which is the one path where
 * the broker takes a service away without anyone asking it to.
 */
final class Liveness {

	private static final Logger LOG = Logger.getLogger(Liveness.class.getName());

	private final BrokerState state;

	private final Announcements announcements;

	private final Retirement retirement;

	Liveness(BrokerState state, Announcements announcements, Retirement retirement) {
		this.state = state;
		this.announcements = announcements;
		this.retirement = retirement;
	}

	/**
	 * Provider liveness (#52): the last heartbeat per registration and
	 * the interval the provider promised. Opt-in — only registrations
	 * that heartbeat are in here. Runtime state like the sessions: a
	 * broker restart forgets the leases, and the providers' next
	 * heartbeat (404 → republish) rebuilds them.
	 */
	private final Map<ServiceRegistration, ProviderLease> providerLeases = new IdentityHashMap<>();

	record ProviderLease(Instant lastHeartbeat, long intervalSeconds) {
		Instant lostAt() {
			return lastHeartbeat.plusSeconds(intervalSeconds * MISSED_HEARTBEATS_TO_LOSE);
		}
	}

	/** UPDATE_POLICY.md §4: silence of this many intervals means the provider is gone. */
	public static final int MISSED_HEARTBEATS_TO_LOSE = 2;


	Diagnostic heartbeat(String referenceId, long intervalSeconds) {
		if (referenceId == null || referenceId.isBlank()) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED,
					"referenceId must not be null or blank");
		}
		if (intervalSeconds <= 0) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_HEARTBEAT_INVALID,
					"intervalSeconds must be positive, got " + intervalSeconds);
		}
		state.writeLock().lock();
		try {
			ServiceRegistration registration = state.registrationWithReferenceId(referenceId);
			if (registration == null) {
				// Unknown here means: restart, coldified, retired for silence
				// or replaced — in every case the provider has to publish
				// again to be listed. 404 on the wire, and that is the cue.
				return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED,
						"no live registration for reference '" + referenceId + "' — publish again");
			}
			boolean armed = providerLeases.containsKey(registration);
			providerLeases.put(registration, new ProviderLease(Instant.now(), intervalSeconds));
			if (!armed) {
				LOG.info("[DDSR] provider liveness armed for " + BrokerState.identityOf(registration)
						+ " — lost after " + (intervalSeconds * MISSED_HEARTBEATS_TO_LOSE) + " s of silence");
			}
			// Runtime state, like the sessions: no persist, no event.
			return DdsrDiagnostics.ok("heartbeat accepted, lost after "
					+ (intervalSeconds * MISSED_HEARTBEATS_TO_LOSE) + " s of silence");
		} finally {
			state.writeLock().unlock();
		}
	}
	/**
	 * Retires every registration whose provider promised heartbeats and
	 * has been silent for {@link #MISSED_HEARTBEATS_TO_LOSE} intervals at
	 * {@code now}, announcing {@code UNREGISTERING} followed by
	 * {@code RETIRED} with reason {@code PROVIDER_LOST}. A lost predecessor
	 * of a supersession counts as retired (the drain is over), a lost
	 * successor cancels the drain and the predecessor becomes visible to
	 * lookups again. Maintenance entry point like {@link #coldifyIdle}
	 * and {@link #advanceUpdatePolicies}, not part of the client-facing
	 * contract.
	 *
	 * @return the number of registrations retired in this pass
	 */
	int retireLostProviders(Instant now) {
		if (now == null) {
			return 0;
		}
		state.writeLock().lock();
		try {
			providerLeases.keySet().retainAll(new HashSet<>(state.registrations()));
			int retired = 0;
			for (Map.Entry<ServiceRegistration, ProviderLease> entry : new ArrayList<>(providerLeases.entrySet())) {
				ServiceRegistration registration = entry.getKey();
				if (registration.isUnregistered() || now.isBefore(entry.getValue().lostAt())) {
					continue;
				}
				ServiceProvider provider = registration.getProvider();
				ServiceImplementation impl = registration.getImplementation();
				if (provider == null || impl == null) {
					providerLeases.remove(registration);
					continue;
				}
				// Event material before the detach, as in the withdraw path.
				ServiceReference eventReference = Announcements.selfContained(provider, impl, registration.getReference());
				ServiceReference retiredRef = retirement.retire(provider, impl);
				// A successor's `replaces` would dangle in the snapshot.
				for (ServiceRegistration other : state.registrations()) {
					ServiceImplementation otherImpl = other.getImplementation();
					if (otherImpl != null && otherImpl.getReplaces() == impl) {
						otherImpl.setReplaces(null);
					}
				}
				Diagnostic d = state.persist();
				if (DdsrDiagnostics.isError(d)) {
					LOG.warning("[DDSR] persist after retiring lost provider " + BrokerState.identityOf(registration)
							+ " failed: " + d.getMessage());
				}
				ServiceReference announced = eventReference != null ? eventReference : retiredRef;
				announcements.emit(ServiceEventType.UNREGISTERING, announced, ServiceEventReasons.PROVIDER_LOST);
				announcements.emit(ServiceEventType.RETIRED, announced, ServiceEventReasons.PROVIDER_LOST);
				LOG.warning("[DDSR] retired " + BrokerState.identityOf(registration) + " — provider silent since "
						+ entry.getValue().lastHeartbeat() + " (PROVIDER_LOST)");
				retired++;
			}
			return retired;
		} finally {
			state.writeLock().unlock();
		}
	}
	/**
	 * The lease on one registration, or {@code null} for a provider that
	 * does not heartbeat.
	 *
	 * <p>Absence is an answer here, not a gap: heartbeating is opt-in,
	 * so a registration without a lease is one nobody promised to
	 * supervise.
	 */
	ProviderLease leaseOf(ServiceRegistration registration) {
		state.readLock().lock();
		try {
			return providerLeases.get(registration);
		} finally {
			state.readLock().unlock();
		}
	}

	/** Number of registrations currently under liveness supervision. */
	int providerLeaseCount() {
		state.readLock().lock();
		try {
			return providerLeases.size();
		} finally {
			state.readLock().unlock();
		}
	}

	/**
	 * Drops the lease of a registration that is going away, handing it
	 * back so a failed save can put it there again.
	 *
	 * <p>A withdrawn registration must never keep a lease: the sweeper
	 * would otherwise retire something that is already gone.
	 */
	ProviderLease forget(ServiceRegistration registration) {
		return registration == null ? null : providerLeases.remove(registration);
	}

	/** Puts back what {@link #forget} took, after a mutation did not survive being saved. */
	void restore(ServiceRegistration registration, ProviderLease lease) {
		if (registration != null && lease != null) {
			providerLeases.put(registration, lease);
		}
	}
}
