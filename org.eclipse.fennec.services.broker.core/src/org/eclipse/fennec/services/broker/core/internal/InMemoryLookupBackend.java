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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.broker.core.LookupBackend;

/**
 * Default {@link LookupBackend}: maintains references in a simple
 * concurrent map keyed by {@code ServiceInterface.name}, plus a
 * reference→implementation side-map so flavor matching has the
 * implementation context.
 *
 * <p>Limitations of this prototype implementation:
 * <ul>
 * <li>LDAP {@code filter} is evaluated by {@link LdapFilter} over the
 *     reference's typed properties. A syntactically broken filter
 *     yields an empty result plus a log line — never an exception
 *     (a consumer must not be able to 500 the broker with a typo).</li>
 * <li>Result ordering is registration order; ranking by
 *     {@code service.ranking} / {@code service.id} is not yet
 *     implemented.</li>
 * <li>{@code serviceModified} is a no-op; properties are read live
 *     from the reference object at lookup time, so no index update is
 *     needed unless interfaces change — which the model does not
 *     allow.</li>
 * </ul>
 */
final class InMemoryLookupBackend implements LookupBackend {

	private static final Logger LOG = Logger.getLogger(InMemoryLookupBackend.class.getName());

	/** Interface name → references currently published for that interface. */
	private final Map<String, List<ServiceReference>> byInterface = new ConcurrentHashMap<>();

	/** Reference → owning implementation, for flavor matching. */
	private final Map<ServiceReference, ServiceImplementation> implByRef = Collections
			.synchronizedMap(new IdentityHashMap<>());

	@Override
	public ServiceReference getServiceReference(String interfaceName, String filter, ConsumerCapability capability) {
		List<ServiceReference> all = getServiceReferences(interfaceName, filter, capability);
		return all.isEmpty() ? null : all.get(0);
	}

	@Override
	public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		List<ServiceReference> candidates = byInterface.getOrDefault(interfaceName, Collections.emptyList());
		if (candidates.isEmpty()) {
			return Collections.emptyList();
		}
		LdapFilter parsed = parseOrNull(filter);
		if (filter != null && !filter.isBlank() && parsed == null) {
			return Collections.emptyList();
		}
		List<ServiceReference> result = new ArrayList<>(candidates.size());
		// Snapshot read of the synchronizedList:
		synchronized (candidates) {
			for (ServiceReference ref : candidates) {
				if (matchesFlavor(ref, capability) && (parsed == null || parsed.matches(ref))) {
					result.add(ref);
				}
			}
		}
		return result;
	}

	@Override
	public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		List<ServiceReference> candidates = byInterface.getOrDefault(interfaceName, Collections.emptyList());
		if (candidates.isEmpty()) {
			return Collections.emptyList();
		}
		// "All" waives flavor/compatibility matching (the OSGi analogy is
		// class-space compatibility), not the filter.
		LdapFilter parsed = parseOrNull(filter);
		if (filter != null && !filter.isBlank() && parsed == null) {
			return Collections.emptyList();
		}
		List<ServiceReference> result = new ArrayList<>(candidates.size());
		synchronized (candidates) {
			for (ServiceReference ref : candidates) {
				if (parsed == null || parsed.matches(ref)) {
					result.add(ref);
				}
			}
		}
		return result;
	}

	/** Parsed filter, or {@code null} for no filter / a broken filter. */
	private static LdapFilter parseOrNull(String filter) {
		if (filter == null || filter.isBlank()) {
			return null;
		}
		try {
			return LdapFilter.parse(filter);
		} catch (IllegalArgumentException broken) {
			LOG.log(Level.WARNING, "ignoring broken LDAP filter, returning no matches: {0}",
					broken.getMessage());
			return null;
		}
	}

	@Override
	public void serviceAdded(ServiceImplementation implementation, ServiceReference reference) {
		if (implementation == null || reference == null) {
			return;
		}
		implByRef.put(reference, implementation);
		for (ServiceInterface si : implementation.getServiceInterfaces()) {
			byInterface.computeIfAbsent(si.getName(), k -> Collections.synchronizedList(new ArrayList<>()))
					.add(reference);
		}
	}

	@Override
	public void serviceModified(ServiceImplementation implementation, ServiceReference reference) {
		// Properties are read live; no index update required.
	}

	@Override
	public void serviceRemoved(ServiceImplementation implementation, ServiceReference reference) {
		if (reference == null) {
			return;
		}
		ServiceImplementation impl = implementation != null ? implementation : implByRef.remove(reference);
		if (impl == null) {
			return;
		}
		implByRef.remove(reference);
		for (ServiceInterface si : impl.getServiceInterfaces()) {
			List<ServiceReference> list = byInterface.get(si.getName());
			if (list != null) {
				list.remove(reference);
				if (list.isEmpty()) {
					byInterface.remove(si.getName());
				}
			}
		}
	}

	/** True if the reference has at least one flavor the consumer can speak. */
	private boolean matchesFlavor(ServiceReference ref, ConsumerCapability capability) {
		if (capability == null) {
			return true;
		}
		List<FlavorKind> supported = capability.getSupportedFlavors();
		if (supported == null || supported.isEmpty()) {
			// An empty declaration is the absence of a constraint, not
			// "speaks nothing": capabilities can exist for the consumerId
			// or the contract fingerprint alone (LookupResource builds
			// them that way), and those lookups must behave like ones
			// without a capability — same rule as the empty event-flavor
			// filter ("deliver rather than withhold").
			return true;
		}
		ServiceImplementation impl = implByRef.get(ref);
		if (impl == null || impl.getFlavors().isEmpty()) {
			// Local-only references without explicit flavor: accept any
			// consumer (callable in-process).
			return true;
		}
		for (ServiceFlavor f : impl.getFlavors()) {
			if (supported.contains(f.getKind())) {
				return true;
			}
		}
		return false;
	}
}
