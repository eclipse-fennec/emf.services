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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.client.DdsrConsumer;
import org.eclipse.fennec.services.client.DdsrServiceListener;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceReference;

final class ConsumerImpl implements DdsrConsumer {

	private static final Logger LOG = Logger.getLogger(ConsumerImpl.class.getName());

	private final BrokerLookup lookup;
	private final List<FlavorKind> supportedFlavors;
	private final String consumerId;
	private final ServiceListenerRegistry listeners;

	ConsumerImpl(BrokerLookup lookup, List<FlavorKind> supportedFlavors, String consumerId) {
		this(lookup, supportedFlavors, consumerId, null);
	}

	ConsumerImpl(BrokerLookup lookup, List<FlavorKind> supportedFlavors, String consumerId,
			EventSource eventSource) {
		this.lookup = lookup;
		this.supportedFlavors = supportedFlavors;
		this.consumerId = consumerId;
		// On (re)connect the local view may be stale, so we re-snapshot
		// rather than expect a replay (FR-Sync-Reconnect). Nothing is
		// cached here yet, so the hook only logs — the point is that the
		// signal exists and has one owner.
		this.listeners = new ServiceListenerRegistry(eventSource, this::refreshFromSnapshot);
	}

	/** Reference ids known from lookups — the session acquisition list (ACQUISITION.md §4). */
	public java.util.Set<String> knownReferenceIds() {
		return listeners.knownReferenceIds();
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
	 * Pulls a fresh snapshot for every interface we listen on.
	 * <p>
	 * FR-Sync-Reconnect: after a drop the broker does not replay missed
	 * events — it keeps no per-client history and the stream carries no
	 * sequence numbers — so the consumer re-reads instead. Called on the
	 * initial connect too, which is the point: a consumer that starts
	 * listening while services are already published would otherwise know
	 * nothing about them and could not route their withdrawals.
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
		for (String interfaceName : interfaces) {
			try {
				// find() honours the consumer capability and records what it
				// returns, which is exactly the snapshot we need.
				seen += find(interfaceName, null).size();
			} catch (RuntimeException lookupFailure) {
				// A stream that just came up may still race the broker.
				// Losing the snapshot is recoverable — the next reconnect
				// tries again — losing the stream is not.
				LOG.warning("[DDSR-Client] snapshot for " + interfaceName
						+ " failed, continuing: " + lookupFailure);
			}
		}
		LOG.info("[DDSR-Client] snapshot after connect: " + seen
				+ " reference(s) across " + interfaces);
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

	/** @see ServiceListenerRegistry#transportAvailable() */
	void transportAvailable() {
		listeners.transportAvailable();
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
			result.add(new ServiceLocatorImpl(ref, impl));
		}
		return result;
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
