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

package org.eclipse.fennec.services.client.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.ServiceEventReasons;
import org.eclipse.fennec.services.client.DdsrConsumer;
import org.eclipse.fennec.services.client.DdsrServiceListener;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.client.ServiceLocator;

final class ConsumerImpl implements DdsrConsumer {

	private static final Logger LOG = Logger.getLogger(ConsumerImpl.class.getName());

	private final BrokerLookup lookup;
	private final List<FlavorKind> supportedFlavors;
	private final String consumerId;
	private final ServiceListenerRegistry listeners;
	/** UPDATE_POLICY.md §3: rebind to the successor on UPGRADE_AVAILABLE instead of waiting for the retire. */
	private final boolean greedyRebind;
	/** Tracked locators by the reference id they are bound to (#57). */
	private final Map<String, List<ServiceLocatorImpl>> trackedByReference = new ConcurrentHashMap<>();
	/** Interfaces for which the internal tracking listener is registered. */
	private final Set<String> trackedInterfaces = ConcurrentHashMap.newKeySet();
	private final DdsrServiceListener trackingListener = this::onTrackedEvent;

	/** Everything currently tracked, for the runtime service (#126). */
	List<ServiceLocatorImpl> tracked() {
		List<ServiceLocatorImpl> all = new ArrayList<>();
		for (List<ServiceLocatorImpl> locators : trackedByReference.values()) {
			all.addAll(locators);
		}
		return all;
	}

	/** Whether the event stream is currently open (#126). */
	boolean streamConnected() {
		return listeners.isStreamOpen();
	}

	/** How this runtime names itself to the broker. */
	String consumerId() {
		return consumerId;
	}

	/** The transports it said it speaks. */
	List<FlavorKind> supportedFlavors() {
		return List.copyOf(supportedFlavors);
	}

	ConsumerImpl(BrokerLookup lookup, List<FlavorKind> supportedFlavors, String consumerId) {
		this(lookup, supportedFlavors, consumerId, null);
	}

	ConsumerImpl(BrokerLookup lookup, List<FlavorKind> supportedFlavors, String consumerId,
			EventSource eventSource) {
		this(lookup, supportedFlavors, consumerId, eventSource, false);
	}

	ConsumerImpl(BrokerLookup lookup, List<FlavorKind> supportedFlavors, String consumerId,
			EventSource eventSource, boolean greedyRebind) {
		this.lookup = lookup;
		this.supportedFlavors = supportedFlavors;
		this.consumerId = consumerId;
		this.greedyRebind = greedyRebind;
		// On (re)connect the local view may be stale, so we re-snapshot
		// rather than expect a replay (FR-Sync-Reconnect) and deliver what
		// the listeners missed.
		this.listeners = new ServiceListenerRegistry(eventSource, this::refreshFromSnapshot, consumerId);
	}

	/** Reference ids known from lookups — the session acquisition list (ACQUISITION.md §4). */
	public Set<String> knownReferenceIds() {
		return listeners.knownReferenceIds();
	}

	@Override
	public void release(String referenceId) {
		listeners.forgetReference(referenceId);
	}

	@Override
	public AutoCloseable addServiceListener(String interfaceName, String filter,
			DdsrServiceListener listener) {
		return listeners.add(interfaceName, filter, listener);
	}

	@Override
	public void removeServiceListener(DdsrServiceListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Reads every interface we listen on again and tells the listeners what
	 * they missed.
	 * <p>
	 * FR-Sync-Reconnect: after a drop the broker does not replay missed
	 * events — it keeps no per-client history and the stream carries no
	 * sequence numbers — so the consumer re-reads instead. Reading alone is
	 * not enough, though: a listener that waits for an implementation (an
	 * RSA import, a proxy registrar) only acts on an event. So the snapshot
	 * is compared with what this consumer knew, and the difference is
	 * delivered as events: {@code REGISTERED} for a reference that is new,
	 * {@code UNREGISTERING} with reason {@code RESYNCED} for one that is no
	 * longer listed (#271).
	 * <p>
	 * Called on the initial connect too: whatever was published between a
	 * lookup and the stream coming up is heard the same way. The snapshot
	 * resolves without creating locators — a locator is something a caller
	 * holds, and nobody holds these.
	 * <p>
	 * Runs on the stream's own thread before events start flowing, so the
	 * snapshot is in place by the time the first event arrives.
	 */
	private void refreshFromSnapshot() {
		Set<String> interfaces = new LinkedHashSet<>(listeners.subscribedInterfaces());
		if (interfaces.isEmpty()) {
			return;
		}
		int seen = 0;
		int missed = 0;
		for (String interfaceName : interfaces) {
			Set<String> known = listeners.referencesFor(interfaceName);
			List<ServiceLocatorImpl.Resolved> current;
			try {
				current = resolve(interfaceName, null);
			} catch (RuntimeException lookupFailure) {
				// A stream that just came up may still race the broker.
				// Losing the snapshot is recoverable — the next reconnect
				// tries again — losing the stream is not.
				LOG.warning("[DDSR-Client] snapshot for " + interfaceName
						+ " failed, continuing: " + lookupFailure);
				continue;
			}
			seen += current.size();
			Set<String> listed = new LinkedHashSet<>();
			for (ServiceLocatorImpl.Resolved resolved : current) {
				String id = resolved.reference().getId();
				listed.add(id);
				if (!known.contains(id)) {
					listeners.onEvent(event(ServiceEventType.REGISTERED, resolved.reference(), null));
					missed++;
				}
			}
			for (String id : known) {
				if (!listed.contains(id)) {
					ServiceReference gone = ServicesFactory.eINSTANCE.createServiceReference();
					gone.setId(id);
					listeners.onEvent(event(ServiceEventType.UNREGISTERING, gone, ServiceEventReasons.RESYNCED));
					missed++;
				}
			}
		}
		LOG.info("[DDSR-Client] snapshot after connect: " + seen
				+ " reference(s) across " + interfaces + ", " + missed + " missed change(s) delivered");
	}

	private static ServiceEvent event(ServiceEventType type, ServiceReference reference, String reason) {
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(type);
		event.setReference(reference);
		event.setTimestamp(new Date());
		event.setReasonCode(reason);
		return event;
	}

	private static Set<String> interfaceNamesOf(ServiceImplementation impl) {
		if (impl == null) {
			return Set.of();
		}
		Set<String> names = new LinkedHashSet<>();
		for (ServiceInterface si : impl.getServiceInterfaces()) {
			if (si.getName() != null) {
				names.add(si.getName());
			}
		}
		return names;
	}


	/** Stop listening; the locators handed out stay valid as objects. */
	void close() {
		listeners.close();
	}

	@Override
	public List<ServiceLocator> find(String interfaceName, String filter) {
		ConsumerCapability cap = buildCapability();
		List<ServiceReference> refs = lookup.getServiceReferences(interfaceName, filter, cap);
		if (refs.isEmpty()) {
			return List.of();
		}
		// BrokerLookup contract: returned references carry their provider
		// chain (impl + flavors) populated either by direct EMF traversal
		// (embedded) or by the REST proxy after deserialising the
		// envelope. We ask the lookup for the impl by reference rather
		// than walking ourselves, so flavor selection stays consistent
		// across transports.
		List<ServiceLocator> result = new ArrayList<>(refs.size());
		for (ServiceReference ref : refs) {
			ServiceImplementation impl = lookup.getImplementationForReference(ref);
			// Remember which interfaces this reference serves. An
			// UNREGISTERING event carries no interface information — by then
			// the implementation is detached — so what we learn here is what
			// lets us route the withdrawal of a service we looked up.
			listeners.noteReference(ref.getId(), interfaceNamesOf(impl));
			ServiceLocatorImpl locator = new ServiceLocatorImpl(ref, impl, this::resolve, interfaceName, filter);
			track(locator);
			result.add(locator);
		}
		return result;
	}

	// ------------------------------------------------------------------
	// Locator tracking (#57): locators follow their service
	// ------------------------------------------------------------------

	/** Raw resolution for a rebinding locator — the same lookup as find(), no new locators. */
	private List<ServiceLocatorImpl.Resolved> resolve(String interfaceName, String filter) {
		List<ServiceReference> refs = lookup.getServiceReferences(interfaceName, filter, buildCapability());
		List<ServiceLocatorImpl.Resolved> resolved = new ArrayList<>(refs.size());
		for (ServiceReference ref : refs) {
			ServiceImplementation impl = lookup.getImplementationForReference(ref);
			listeners.noteReference(ref.getId(), interfaceNamesOf(impl));
			resolved.add(new ServiceLocatorImpl.Resolved(ref, impl));
		}
		return resolved;
	}

	private void track(ServiceLocatorImpl locator) {
		locator.onRebound(this::rekey);
		rekey(locator, null);
		// Interest in the interface keeps the event stream open and puts the
		// interface into the reconnect snapshot: one internal listener per interface.
		if (locator.interfaceName() != null && trackedInterfaces.add(locator.interfaceName())) {
			listeners.add(locator.interfaceName(), null, trackingListener);
		}
	}

	private void rekey(ServiceLocatorImpl locator, String previousReferenceId) {
		if (previousReferenceId != null) {
			List<ServiceLocatorImpl> old = trackedByReference.get(previousReferenceId);
			if (old != null) {
				old.remove(locator);
			}
		}
		String id = locator.boundReferenceId();
		if (id != null) {
			trackedByReference.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(locator);
		}
	}

	private void onTrackedEvent(ServiceEvent event) {
		ServiceReference reference = event.getReference();
		if (reference == null || reference.getId() == null) {
			return;
		}
		List<ServiceLocatorImpl> bound = trackedByReference.get(reference.getId());
		if (bound == null) {
			return;
		}
		for (ServiceLocatorImpl locator : bound) {
			locator.onEvent(event, greedyRebind);
		}
	}

	/** Test hook: number of locators currently tracked. */
	int trackedLocatorCount() {
		int n = 0;
		for (List<ServiceLocatorImpl> list : trackedByReference.values()) {
			n += list.size();
		}
		return n;
	}

	private ConsumerCapability buildCapability() {
		if ((supportedFlavors == null || supportedFlavors.isEmpty())
				&& (consumerId == null || consumerId.isBlank())) {
			return null;
		}
		ConsumerCapability cap = ServicesFactory.eINSTANCE.createConsumerCapability();
		if (supportedFlavors != null) {
			cap.getSupportedFlavors().addAll(supportedFlavors);
		}
		if (consumerId != null && !consumerId.isBlank()) {
			cap.setConsumerId(consumerId);
		}
		return cap;
	}
}
