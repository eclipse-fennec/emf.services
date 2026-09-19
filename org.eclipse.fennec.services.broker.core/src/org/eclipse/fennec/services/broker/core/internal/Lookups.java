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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.broker.core.LookupBackend;

/**
 * Answering "who serves this interface".
 *
 * <p>Three things happen on the way out, and the order of them is the
 * whole behaviour. A lookup first tells the cold cache that somebody
 * asked, which both feeds the idle rule and brings a parked entry back.
 * Then the backend answers. Then two filters run over that answer: the
 * references that are draining towards a successor are dropped, and,
 * when the consumer named a contract fingerprint, everything whose
 * contract is not exactly that one is dropped too.
 *
 * <p>The cold step deliberately happens before the read lock is taken.
 * Bringing an entry back needs the write lock, and a read lock cannot
 * be upgraded — doing it inside would deadlock.
 */
final class Lookups {

	private final BrokerState state;

	private final LookupBackend lookup;

	private final ColdCache cold;

	private final UpdatePolicies policies;

	Lookups(BrokerState state, LookupBackend lookup, ColdCache cold, UpdatePolicies policies) {
		this.state = state;
		this.lookup = lookup;
		this.cold = cold;
		this.policies = policies;
	}

	ServiceReference getServiceReference(String interfaceName) {
		cold.touchAndRehydrate(interfaceName);
		state.readLock().lock();
		try {
			return lookup.getServiceReference(interfaceName, null, null);
		} finally {
			state.readLock().unlock();
		}
	}
	List<ServiceReference> getServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		cold.touchAndRehydrate(interfaceName);
		state.readLock().lock();
		try {
			return policies.withoutDraining(filterByRequestedFingerprint(
					lookup.getServiceReferences(interfaceName, filter, capability), capability));
		} finally {
			state.readLock().unlock();
		}
	}
	List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		cold.touchAndRehydrate(interfaceName);
		state.readLock().lock();
		try {
			return filterByRequestedFingerprint(
					lookup.getAllServiceReferences(interfaceName, filter, capability), capability);
		} finally {
			state.readLock().unlock();
		}
	}
	ServiceImplementation getImplementationForReference(ServiceReference reference) {
		if (reference == null) {
			return null;
		}
		ServiceRegistration reg = reference.getRegistration();
		if (reg == null) {
			return null;
		}
		state.readLock().lock();
		try {
			// The transient link survives on a withdrawn pair (needed for
			// event building and rollback) — the flag is what says "dead".
			return reg.isUnregistered() ? null : reg.getImplementation();
		} finally {
			state.readLock().unlock();
		}
	}
	/**
	 * Contract addressing (ACQUISITION.md §11.2): when the consumer's
	 * capability carries a {@code ddsr.fingerprint} property, only
	 * references whose broker-computed contract fingerprint matches
	 * exactly are returned. Compatibility = identical sd1 — the
	 * comparison runs against the CATALOG truth (decorateReference hashes
	 * the catalog entries the impl was rewired to at publish), so a
	 * provider whose local contract drifted falls out of compatible
	 * lookups. No range semantics: fingerprints are identity, versions
	 * communicate intent (§11.3).
	 */
	static List<ServiceReference> filterByRequestedFingerprint(List<ServiceReference> references,
			ConsumerCapability capability) {
		String requested = requestedFingerprint(capability);
		if (requested == null) {
			return references;
		}
		List<ServiceReference> matching = new ArrayList<>(references.size());
		for (ServiceReference reference : references) {
			if (carriesFingerprint(reference, requested)) {
				matching.add(reference);
			}
		}
		return matching;
	}
	static String requestedFingerprint(ConsumerCapability capability) {
		if (capability == null) {
			return null;
		}
		for (Property property : capability.getProperties()) {
			if ("ddsr.fingerprint".equals(property.getName()) && property instanceof StringProperty sp) {
				String value = sp.getValue();
				return value == null || value.isBlank() ? null : value;
			}
		}
		return null;
	}
	static boolean carriesFingerprint(ServiceReference reference, String requested) {
		for (Property property : reference.getProperties()) {
			String name = property.getName();
			if (name != null && name.startsWith("ddsr.fingerprint")
					&& property instanceof StringProperty sp
					&& requested.equals(sp.getValue())) {
				return true;
			}
		}
		return false;
	}
}
