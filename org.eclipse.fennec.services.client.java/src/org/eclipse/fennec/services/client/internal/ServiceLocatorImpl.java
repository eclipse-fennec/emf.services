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

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.broker.core.ServiceEventReasons;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.TrackedServiceLocator;

final class ServiceLocatorImpl implements TrackedServiceLocator {

	/** How a locator gets back to the broker: the consumer that created it. */
	interface Rebinder {
		/** Live references for the interface/filter, each with its implementation; never null. */
		List<Resolved> resolve(String interfaceName, String filter);
	}

	record Resolved(ServiceReference reference, ServiceImplementation implementation) {
	}

	private static final Logger LOG = Logger.getLogger(ServiceLocatorImpl.class.getName());

	private final Rebinder rebinder;
	private final String interfaceName;
	private final String filter;
	/**
	 * What this locator is bound to, as one value (#124).
	 *
	 * <p>These three used to be three volatile fields, written by two
	 * methods that do not exclude each other: {@code rebind} on a caller's
	 * thread and {@link #onEvent} on the event stream's. An interleaving
	 * left the reference of one registration next to the implementation
	 * of another, and a call then went to an endpoint that belonged to
	 * neither — while the session kept claiming a reference that was not
	 * the one in use.
	 *
	 * <p>Kept together they can only be replaced together, so every
	 * state a reader can observe is one that actually existed. Two
	 * writers can still race, and the loser's update is simply lost —
	 * which is harmless in both directions here: a rebind that overwrites
	 * an event's REBIND has just resolved something fresh, and an event
	 * that overwrites a rebind leaves the locator asking to rebind again
	 * the next time it is used.
	 */
	private volatile Binding binding;

	private volatile BiConsumer<ServiceLocatorImpl, String> rebound;

	/** The reference, its implementation and what we believe about them. */
	private record Binding(ServiceReference reference, ServiceImplementation implementation, State state) {

		Binding in(State newState) {
			return new Binding(reference, implementation, newState);
		}
	}

	/** Tracker callback: (locator, previous reference id) after a rebind changed the id. */
	void onRebound(BiConsumer<ServiceLocatorImpl, String> callback) {
		this.rebound = callback;
	}

	/** Untracked: what the locator was found with is what it stays. */
	ServiceLocatorImpl(ServiceReference reference, ServiceImplementation implementation) {
		this(reference, implementation, null, null, null);
	}

	ServiceLocatorImpl(ServiceReference reference, ServiceImplementation implementation,
			Rebinder rebinder, String interfaceName, String filter) {
		this.binding = new Binding(reference, implementation, State.LIVE);
		this.rebinder = rebinder;
		this.interfaceName = interfaceName;
		this.filter = filter;
	}

	@Override
	public ServiceReference reference() {
		return binding.reference();
	}

	@Override
	public ServiceImplementation implementation() {
		ensureBound();
		return binding.implementation();
	}

	@Override
	public State state() {
		return binding.state();
	}

	@Override
	public String interfaceName() {
		return interfaceName;
	}

	@Override
	public String filter() {
		return filter;
	}

	String boundReferenceId() {
		ServiceReference current = binding.reference();
		return current != null ? current.getId() : null;
	}

	/** Called by the consumer's tracker when an event names this locator's reference. */
	void onEvent(ServiceEvent event, boolean greedy) {
		ServiceEventType type = event.getType();
		if (type == null) {
			return;
		}
		switch (type) {
		case MODIFIED -> {
			ServiceImplementation fresh = selfContainedImplementation(event.getReference());
			binding = fresh != null
					? new Binding(event.getReference(), fresh, State.LIVE)
					: binding.in(State.MODIFIED);
		}
		case UNREGISTERING -> binding = binding.in(
				ServiceEventReasons.COLDIFIED.equals(event.getReasonCode()) ? State.STALE : State.REBIND);
		case RETIRED -> binding = binding.in(State.REBIND);
		case UPGRADE_AVAILABLE -> {
			if (greedy) {
				binding = binding.in(State.REBIND);
			}
		}
		default -> {
			// REGISTERED / MODIFIED_ENDMATCH: nothing to do for a bound locator
		}
		}
	}

	private static ServiceImplementation selfContainedImplementation(ServiceReference reference) {
		if (reference == null || reference.getProvider() == null
				|| reference.getProvider().getImplementations().size() != 1) {
			return null;
		}
		return reference.getProvider().getImplementations().get(0);
	}

	private void ensureBound() {
		State was = binding.state();
		if (was == State.LIVE || rebinder == null) {
			return;
		}
		if (!rebind(was == State.REBIND)) {
			throw new DdsrException("service " + interfaceName + " (" + boundReferenceId() + ") is not available: "
					+ (was == State.STALE ? "parked and not rehydrated" : "no other registration matches"));
		}
	}

	@Override
	public synchronized boolean rebind(boolean excludeCurrent) {
		if (rebinder == null) {
			return binding.state() == State.LIVE;
		}
		String currentId = boundReferenceId();
		List<Resolved> candidates = rebinder.resolve(interfaceName, filter);
		Resolved chosen = null;
		if (!excludeCurrent && currentId != null) {
			for (Resolved candidate : candidates) {
				if (currentId.equals(candidate.reference().getId())) {
					chosen = candidate;
					break;
				}
			}
		}
		if (chosen == null) {
			for (Resolved candidate : candidates) {
				if (currentId == null || !currentId.equals(candidate.reference().getId())) {
					chosen = candidate;
					break;
				}
			}
		}
		if (chosen == null) {
			return false;
		}
		String fromId = currentId;
		binding = new Binding(chosen.reference(), chosen.implementation(), State.LIVE);
		String toId = chosen.reference().getId();
		if (fromId != null && !fromId.equals(toId)) {
			LOG.info(() -> "[DDSR-Client] " + interfaceName + ": rebound " + fromId + " -> " + toId);
			if (rebound != null) {
				rebound.accept(this, fromId);
			}
		}
		return true;
	}

	@Override
	public Optional<RestFlavor> restFlavor() {
		ensureBound();
		ServiceImplementation impl = binding.implementation();
		if (impl == null) {
			return Optional.empty();
		}
		for (ServiceFlavor f : impl.getFlavors()) {
			if (f instanceof RestFlavor) {
				return Optional.of((RestFlavor) f);
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<URI> urlFor(String operationName) {
		// A templated endpoint is not an address, and URI cannot even
		// hold one. Saying "no URL" is the truthful answer; a caller that
		// wants to make a request asks endpointFor instead.
		return endpointFor(operationName).filter(url -> url.indexOf('{') < 0).map(URI::create);
	}

	@Override
	public Optional<String> endpointFor(String operationName) {
		RestFlavor rf = restFlavor().orElse(null);
		if (rf == null || operationName == null) {
			return Optional.empty();
		}
		RestOperationFlavor opFlavor = findOpFlavor(rf, operationName);
		if (opFlavor == null) {
			return Optional.empty();
		}
		String host = rf.getHost();
		if (host == null || host.isBlank()) {
			// The broker is expected to self-publish its RestFlavor with
			// a populated host. If we land here the broker config is
			// incomplete — we can't make up a URL, return empty.
			return Optional.empty();
		}
		String basePath = nullToEmpty(rf.getBasePath());
		String opPath = nullToEmpty(opFlavor.getPath());
		return Optional.of(stripTrailingSlash(host) + ensureLeadingSlash(basePath) + ensureLeadingSlash(opPath));
	}

	// ----------------------------------------------------------------

	private static RestOperationFlavor findOpFlavor(RestFlavor rf, String name) {
		for (ServiceOperationFlavor of : rf.getOperationFlavors()) {
			if (of instanceof RestOperationFlavor && name.equals(of.getName())) {
				return (RestOperationFlavor) of;
			}
		}
		// fall back: match by operation name (RestOperationFlavor.name might differ from the
		// underlying ServiceOperation.name).
		for (ServiceOperationFlavor of : rf.getOperationFlavors()) {
			if (of instanceof RestOperationFlavor && of.getOperation() != null
					&& name.equals(of.getOperation().getName())) {
				return (RestOperationFlavor) of;
			}
		}
		return null;
	}

	private static String stripTrailingSlash(String s) {
		return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
	}

	private static String ensureLeadingSlash(String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		return s.startsWith("/") ? s : "/" + s;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
