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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.UpdatePolicy;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.ServiceEventReasons;

/**
 * What happens to the service a new one replaces.
 *
 * <p>A publish that names {@code replaces} arms a policy on its
 * predecessor (UPDATE_POLICY.md §2), and from then on the predecessor
 * is on a clock this class keeps: gone at once, gone after a grace
 * window, or gone once the last consumer let go of it.
 *
 * <p>All of it is runtime state, and forgetting it on a restart fails
 * safe: nothing is retired by accident, and the successor re-arms the
 * policy by publishing again with {@code replaces}. That is why nothing
 * here is part of the snapshot.
 *
 * <p>Two of its questions are asked from outside: a lookup asks which
 * references are draining and should no longer be handed out, and the
 * cold cache asks whether a registration is party to a handover and
 * therefore must not be parked.
 */
final class UpdatePolicies {

	private static final Logger LOG = Logger.getLogger(UpdatePolicies.class.getName());

	private final BrokerState state;

	private final Announcements announcements;

	private final Retirement retirement;

	UpdatePolicies(BrokerState state, Announcements announcements, Retirement retirement) {
		this.state = state;
		this.announcements = announcements;
		this.retirement = retirement;
	}

	/**
	 * Predecessors superseded by a publish that declared {@code replaces}
	 * (UPDATE_POLICY.md §2), keyed by the predecessor's registration.
	 * Runtime state like {@link #registrationSince}: a broker restart
	 * forgets pending drains and cutovers, which fails safe — nothing is
	 * retired by accident; the successor re-arms the policy by publishing
	 * again with {@code replaces}.
	 */
	private final Map<ServiceRegistration, Supersession> superseded = new IdentityHashMap<>();

	private record Supersession(ServiceRegistration successor, UpdatePolicy policy, Instant cutoverAt) {
	}

	/** UPDATE_POLICY.md §2.3: default failover window of a HARD_CUTOVER. */
	public static final long DEFAULT_CUTOVER_GRACE_MILLIS = 30_000L;
	private volatile long defaultCutoverGraceMillis = DEFAULT_CUTOVER_GRACE_MILLIS;

	/** Broker default for {@code cutoverGraceMillis} when the successor leaves it at 0. */
	void setDefaultCutoverGraceMillis(long millis) {
		this.defaultCutoverGraceMillis = Math.max(0L, millis);
	}
	/**
	 * Successor override, else the strictest default among its interfaces
	 * (HARD_CUTOVER &gt; DEPRECATE_AND_DRAIN &gt; EVERGREEN), else the broker
	 * default DEPRECATE_AND_DRAIN. {@code UNSPECIFIED} means "inherit" on
	 * both levels.
	 */
	static UpdatePolicy effectiveUpdatePolicy(ServiceImplementation implementation) {
		UpdatePolicy own = implementation.getUpdatePolicy();
		if (own != null && own != UpdatePolicy.UNSPECIFIED) {
			return own;
		}
		UpdatePolicy strictest = UpdatePolicy.UNSPECIFIED;
		for (ServiceInterface si : implementation.getServiceInterfaces()) {
			UpdatePolicy candidate = si.getUpdatePolicy();
			if (candidate != null && candidate.getValue() > strictest.getValue()) {
				strictest = candidate;
			}
		}
		return strictest == UpdatePolicy.UNSPECIFIED ? UpdatePolicy.DEPRECATE_AND_DRAIN : strictest;
	}
	/**
	 * Arms the update policy for a successor that named a live
	 * predecessor. Runs after the successor's REGISTERED went out, so a
	 * consumer reacting to UPGRADE_AVAILABLE already finds the successor.
	 * The predecessor itself is only retired by {@link #advanceUpdatePolicies}.
	 */
	void armUpdatePolicy(ServiceRegistration successor, ServiceImplementation predecessorImpl) {
		ServiceRegistration predecessor = state.registrationOf(predecessorImpl);
		if (predecessor == null || predecessor.isUnregistered() || predecessor == successor) {
			return; // parked cold or already gone — nothing to drain
		}
		UpdatePolicy policy = effectiveUpdatePolicy(successor.getImplementation());
		switch (policy) {
		case EVERGREEN -> LOG.fine(() -> "[DDSR] " + BrokerState.identityOf(successor) + " replaces "
				+ BrokerState.identityOf(predecessor) + " under EVERGREEN — both stay registered");
		case HARD_CUTOVER -> {
			long own = successor.getImplementation().getCutoverGraceMillis();
			long grace = own > 0 ? own : defaultCutoverGraceMillis;
			superseded.put(predecessor, new Supersession(successor, policy, Instant.now().plusMillis(grace)));
			LOG.info("[DDSR] " + BrokerState.identityOf(successor) + " replaces " + BrokerState.identityOf(predecessor)
					+ " under HARD_CUTOVER — retiring the predecessor in " + grace + " ms");
		}
		default -> {
			// DEPRECATE_AND_DRAIN: hidden from new lookups, kept alive by
			// its leases, UPGRADE_AVAILABLE as the hint to migrate.
			superseded.put(predecessor, new Supersession(successor, UpdatePolicy.DEPRECATE_AND_DRAIN, null));
			LOG.info("[DDSR] " + BrokerState.identityOf(successor) + " replaces " + BrokerState.identityOf(predecessor)
					+ " under DEPRECATE_AND_DRAIN — draining " + predecessor.getUsingSessions().size() + " lease(s)");
			announcements.emit(ServiceEventType.UPGRADE_AVAILABLE, predecessor.getReference());
		}
		}
	}
	/**
	 * Advances the update-policy state machine (UPDATE_POLICY.md §2):
	 * retires every DEPRECATE_AND_DRAIN predecessor whose last lease is
	 * gone and every HARD_CUTOVER predecessor whose grace window has
	 * elapsed at {@code now}, announcing each as {@code UNREGISTERING}
	 * followed by {@code RETIRED} (reason REPLACED resp. CUTOVER).
	 * Idempotent. Like {@link #coldifyIdle} a maintenance entry point of
	 * the implementation, deliberately not part of the client-facing
	 * {@code BrokerImplementations} contract.
	 *
	 * @return the number of predecessors retired in this pass
	 */
	int advanceUpdatePolicies(Instant now) {
		if (now == null) {
			return 0;
		}
		state.writeLock().lock();
		try {
			int retired = 0;
			for (Map.Entry<ServiceRegistration, Supersession> entry : new ArrayList<>(superseded.entrySet())) {
				ServiceRegistration predecessor = entry.getKey();
				Supersession supersession = entry.getValue();
				if (predecessor.isUnregistered() || !state.registrations().contains(predecessor)) {
					superseded.remove(predecessor); // withdrawn meanwhile
					continue;
				}
				ServiceRegistration successor = supersession.successor();
				if (successor.isUnregistered() || !state.registrations().contains(successor)) {
					superseded.remove(predecessor);
					LOG.info("[DDSR] successor of " + BrokerState.identityOf(predecessor)
							+ " is gone — cancelling its " + supersession.policy().getLiteral());
					continue;
				}
				// A hard cutover is a clock the publisher set and needs no
				// consent; a drain waits for the last consumer to let go.
				//
				// ACQUISITION.md §6 asks for a grace period here, so that a
				// broker whose session map is still empty right after a
				// restart does not read that as "nobody is using this" and
				// drain everything. That cannot happen: this map is runtime
				// state, a restart forgets every armed handover, and there
				// is nothing left to drain until a successor publishes with
				// `replaces` again. Adding the wait anyway would delay every
				// drain in a deployment that does not use sessions at all,
				// for a hazard that is already ruled out (#2).
				boolean due = supersession.policy() == UpdatePolicy.HARD_CUTOVER
						? !now.isBefore(supersession.cutoverAt())
						: predecessor.getUsingSessions().isEmpty();
				if (!due) {
					continue;
				}
				ServiceProvider provider = predecessor.getProvider();
				ServiceImplementation impl = predecessor.getImplementation();
				// Event material before the detach, as in the withdraw path.
				ServiceReference eventReference = Announcements.selfContained(provider, impl, predecessor.getReference());
				ServiceReference retiredRef = retirement.retire(provider, impl);
				ServiceImplementation successorImpl = successor.getImplementation();
				if (successorImpl != null && successorImpl.getReplaces() == impl) {
					successorImpl.setReplaces(null); // would dangle in the snapshot
				}
				superseded.remove(predecessor);
				Diagnostic d = state.persist();
				if (DdsrDiagnostics.isError(d)) {
					LOG.warning("[DDSR] persist after policy retire of " + BrokerState.identityOf(predecessor)
							+ " failed: " + d.getMessage());
				}
				String reason = supersession.policy() == UpdatePolicy.HARD_CUTOVER
						? ServiceEventReasons.CUTOVER
						: ServiceEventReasons.REPLACED;
				ServiceReference announced = eventReference != null ? eventReference : retiredRef;
				announcements.emit(ServiceEventType.UNREGISTERING, announced, reason);
				announcements.emit(ServiceEventType.RETIRED, announced, reason);
				LOG.info("[DDSR] retired " + BrokerState.identityOf(predecessor) + " (" + reason + ")");
				retired++;
			}
			return retired;
		} finally {
			state.writeLock().unlock();
		}
	}
	/**
	 * UPDATE_POLICY.md §2.2: a predecessor in DEPRECATE_AND_DRAIN is
	 * invisible to new lookups (its holders keep it through their leases);
	 * during a HARD_CUTOVER grace window both stay visible (§2.3 phase 2).
	 * {@code getAllServiceReferences} waives this like it waives flavor
	 * matching.
	 */
	List<ServiceReference> withoutDraining(List<ServiceReference> references) {
		if (superseded.isEmpty() || references.isEmpty()) {
			return references;
		}
		List<ServiceReference> visible = new ArrayList<>(references.size());
		for (ServiceReference reference : references) {
			Supersession supersession = supersessionOf(reference);
			if (supersession == null || supersession.policy() != UpdatePolicy.DEPRECATE_AND_DRAIN) {
				visible.add(reference);
			}
		}
		return visible;
	}
	Supersession supersessionOf(ServiceReference reference) {
		for (Map.Entry<ServiceRegistration, Supersession> entry : superseded.entrySet()) {
			ServiceReference candidate = entry.getKey().getReference();
			if (candidate == reference
					|| (candidate != null && reference.getId() != null && reference.getId().equals(candidate.getId()))) {
				return entry.getValue();
			}
		}
		return null;
	}
	boolean isPartyOfSupersession(ServiceRegistration registration) {
		if (superseded.containsKey(registration)) {
			return true;
		}
		for (Supersession supersession : superseded.values()) {
			if (supersession.successor() == registration) {
				return true;
			}
		}
		return false;
	}
	void forgetSupersession(ServiceRegistration registration) {
		superseded.remove(registration);
		superseded.values().removeIf(supersession -> supersession.successor() == registration);
	}
}
