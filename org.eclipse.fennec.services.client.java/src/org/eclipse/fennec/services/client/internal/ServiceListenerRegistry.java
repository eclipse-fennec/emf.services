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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.eclipse.fennec.services.client.DdsrServiceListener;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;

/**
 * Keeps the registered {@link DdsrServiceListener}s and routes incoming
 * events to the ones that asked for the affected interface.
 * <p>
 * The stream is opened lazily on the first listener and closed with the
 * last, so a consumer that never listens pays for no connection.
 * <p>
 * <b>How an event is routed.</b> A {@code ServiceEvent} carries only
 * type, reference and timestamp — the reference is the identity, exactly
 * as in {@code org.osgi.framework.ServiceEvent}. For a REGISTERED event
 * the wire document also carries the implementation, so the interfaces
 * can be read off it; that mapping is remembered per reference id. An
 * UNREGISTERING event cannot carry it: by then the implementation is
 * detached from its provider and no longer resolvable, which is
 * deliberate (see the withdraw path in {@code DdsrBrokerImpl}). The
 * remembered mapping covers the normal case of a consumer that saw the
 * registration.
 * <p>
 * If an event's interfaces cannot be determined, it goes to <em>all</em>
 * listeners rather than being dropped. Over-delivery is recoverable — a
 * listener can ignore a reference it does not hold — whereas a silently
 * dropped UNREGISTERING leaves a consumer bound to a service that is
 * gone.
 */
final class ServiceListenerRegistry implements EventSource.Handler {

	private static final Logger LOG = Logger.getLogger(ServiceListenerRegistry.class.getName());

	private record Entry(String interfaceName, String filter, DdsrServiceListener listener) {
	}

	private final List<Entry> entries = new CopyOnWriteArrayList<>();

	/** Reference id → interface names, learned from registrations and lookups. */
	private final Map<String, Set<String>> interfacesByReference = new ConcurrentHashMap<>();

	/**
	 * The reference ids this consumer currently knows from lookups —
	 * exactly the acquisition list of the session protocol
	 * (ACQUISITION.md §4: "der Client hat alles schon").
	 */
	public Set<String> knownReferenceIds() {
		return Set.copyOf(interfacesByReference.keySet());
	}

	private final EventSource eventSource;

	private final Runnable onStreamEstablished;

	private AutoCloseable subscription;

	/**
	 * @param eventSource          may be {@code null} — then listener
	 *                             registration is accepted but nothing is
	 *                             ever delivered, which is what a client
	 *                             without an event transport should do
	 *                             rather than fail
	 * @param onStreamEstablished  invoked when the stream comes up, so the
	 *                             caller can re-snapshot (FR-Sync-Reconnect)
	 */
	ServiceListenerRegistry(EventSource eventSource, Runnable onStreamEstablished) {
		this.eventSource = eventSource;
		this.onStreamEstablished = onStreamEstablished;
	}

	synchronized AutoCloseable add(String interfaceName, String filter, DdsrServiceListener listener) {
		Entry entry = new Entry(interfaceName, filter, listener);
		entries.add(entry);
		openIfNeeded();
		return () -> remove(listener);
	}

	/**
	 * An event transport became available — or a BETTER one took over
	 * (DS greedy rebind on service.ranking). Listeners may already be
	 * waiting, and a stream may already be open on the transport that
	 * just lost the binding — so this closes any open stream and reopens
	 * through the indirection, which reads the currently bound source.
	 * The reopen announces itself via onStreamEstablished, so the
	 * consumer re-snapshots and no event is lost in the switch
	 * (FR-Sync-Reconnect covers transport handover for free).
	 */
	synchronized void transportAvailable() {
		if (subscription != null) {
			try {
				subscription.close();
			} catch (Exception closeFailure) {
				LOG.warning("[DDSR-Client] closing the event stream for transport switch failed: "
						+ closeFailure);
			}
			subscription = null;
		}
		openIfNeeded();
	}

	synchronized void remove(DdsrServiceListener listener) {
		entries.removeIf(e -> e.listener() == listener);
		closeIfUnused();
	}

	/** Remembers what interface a reference belongs to, e.g. from a lookup. */
	void noteReference(String referenceId, Set<String> interfaceNames) {
		if (referenceId != null && interfaceNames != null && !interfaceNames.isEmpty()) {
			interfacesByReference.put(referenceId, Set.copyOf(interfaceNames));
		}
	}

	int listenerCount() {
		return entries.size();
	}

	boolean isStreamOpen() {
		return subscription != null;
	}

	private void openIfNeeded() {
		if (subscription == null && eventSource != null && !entries.isEmpty()) {
			// A source may return null when its transport is not up yet.
			// Leaving subscription null means the next registration tries
			// again, rather than us holding a dead handle forever.
			subscription = eventSource.open(this);
		}
	}

	private void closeIfUnused() {
		if (subscription != null && entries.isEmpty()) {
			try {
				subscription.close();
			} catch (Exception closeFailure) {
				LOG.warning("[DDSR-Client] closing the event stream failed: " + closeFailure);
			}
			subscription = null;
		}
	}

	@Override
	public void onStreamEstablished() {
		if (onStreamEstablished != null) {
			onStreamEstablished.run();
		}
	}

	@Override
	public void onEvent(ServiceEvent event) {
		if (event == null) {
			return;
		}
		Set<String> affected = interfacesOf(event);
		for (Entry entry : entries) {
			if (!affected.isEmpty() && !affected.contains(entry.interfaceName())) {
				continue;
			}
			try {
				entry.listener().serviceChanged(event);
			} catch (RuntimeException listenerFailure) {
				// One bad listener must not stop delivery to the others.
				LOG.warning("[DDSR-Client] service listener threw, continuing: " + listenerFailure);
			}
		}
	}

	/**
	 * Interfaces the event is about — read off the document when it is
	 * there, otherwise from what we remembered for this reference.
	 * Empty means "unknown", which the caller treats as "deliver to all".
	 */
	private Set<String> interfacesOf(ServiceEvent event) {
		ServiceReference reference = event.getReference();
		if (reference == null) {
			return Set.of();
		}
		Set<String> fromDocument = fromDocument(reference);
		if (!fromDocument.isEmpty()) {
			if (event.getType() == ServiceEventType.REGISTERED) {
				noteReference(reference.getId(), fromDocument);
			}
			return fromDocument;
		}
		Set<String> remembered = interfacesByReference.get(reference.getId());
		if (event.getType() == ServiceEventType.UNREGISTERING && reference.getId() != null) {
			// The service is gone; stop remembering it.
			interfacesByReference.remove(reference.getId());
		}
		return remembered != null ? remembered : Set.of();
	}

	private static Set<String> fromDocument(ServiceReference reference) {
		ServiceProvider provider = reference.getProvider();
		if (provider == null) {
			return Set.of();
		}
		// The broker builds one document per event containing exactly the
		// implementation the reference belongs to, so every implementation
		// present here is the right one to read interfaces from.
		Set<String> names = new LinkedHashSet<>();
		for (ServiceImplementation impl : provider.getImplementations()) {
			for (ServiceInterface si : impl.getServiceInterfaces()) {
				if (si.getName() != null) {
					names.add(si.getName());
				}
			}
		}
		return names;
	}

	/** Snapshot of the current listeners, for diagnostics. */
	List<String> subscribedInterfaces() {
		List<String> names = new ArrayList<>();
		for (Entry e : entries) {
			names.add(e.interfaceName());
		}
		return names;
	}
}
