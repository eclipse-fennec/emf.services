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
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.client.InvokerProvider;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link InvokerProvider} of a framework: a whiteboard over every
 * {@link ServiceInvoker}, registered by hand because its one property
 * changes while it is registered.
 *
 * <p>The order is what the property is for. An invoker is put in before
 * its flavor is published and taken out after the flavor is withdrawn,
 * so whoever was let in by the property finds the invoker, and whoever
 * is about to be turned away still has it while DS turns them away.
 */
@Component(service = {})
public final class InvokerRegistry implements InvokerProvider {

	/** The invokers this runtime has, by the flavor each speaks. */
	private final Map<FlavorKind, ServiceInvoker> invokers = new ConcurrentHashMap<>();

	/** Flavors whose invoker is on its way out: still callable, no longer published. */
	private final Set<FlavorKind> leaving = ConcurrentHashMap.newKeySet();

	private volatile ServiceRegistration<InvokerProvider> registration;

	/**
	 * Every invoker, as they come and go — dynamic and multiple, bound
	 * by method. This is the one place in the client that is a
	 * whiteboard; everything that depends on invokers depends on this
	 * component instead.
	 */
	@Reference(name = "invoker", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addInvoker(ServiceInvoker invoker, Map<String, Object> properties) {
		for (FlavorKind kind : flavorsOf(properties)) {
			invokers.put(kind, invoker);
		}
		publish();
	}

	void removeInvoker(ServiceInvoker invoker, Map<String, Object> properties) {
		// By flavor, not by invoker: one object may be registered twice,
		// and only the flavors of this registration are leaving.
		List<FlavorKind> going = new ArrayList<>();
		for (FlavorKind kind : flavorsOf(properties)) {
			if (invokers.get(kind) == invoker) {
				going.add(kind);
			}
		}
		leaving.addAll(going);
		try {
			publish();
			for (FlavorKind kind : going) {
				invokers.remove(kind, invoker);
			}
		} finally {
			leaving.removeAll(going);
		}
	}

	@Activate
	void activate(BundleContext context) {
		this.registration = context.registerService(InvokerProvider.class, this, properties(flavors()));
		// An invoker bound while the registration was being made saw no
		// registration to update.
		publish();
	}

	@Deactivate
	void deactivate() {
		ServiceRegistration<InvokerProvider> current = registration;
		registration = null;
		if (current != null) {
			try {
				current.unregister();
			} catch (IllegalStateException alreadyGone) {
				// the bundle is stopping and the framework was faster
			}
		}
	}

	@Override
	public ServiceInvoker invokerFor(FlavorKind kind) {
		return kind == null ? null : invokers.get(kind);
	}

	@Override
	public Set<FlavorKind> flavors() {
		Set<FlavorKind> published = new TreeSet<>();
		published.addAll(invokers.keySet());
		published.removeAll(leaving);
		return published;
	}

	/**
	 * Brings the service property in line with the invokers.
	 *
	 * <p>Without a lock: setting properties delivers service events on
	 * this thread, and DS activates and deactivates other components
	 * inside them. Two threads may each publish what they saw, in either
	 * order, so each looks again afterwards and publishes until what it
	 * published is what is there.
	 */
	private void publish() {
		Set<FlavorKind> published;
		do {
			ServiceRegistration<InvokerProvider> current = registration;
			if (current == null) {
				return;
			}
			published = flavors();
			try {
				current.setProperties(properties(published));
			} catch (IllegalStateException unregistered) {
				return;
			}
		} while (!published.equals(flavors()));
	}

	private static Dictionary<String, Object> properties(Set<FlavorKind> flavors) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(FLAVORS, flavors.stream().map(FlavorKind::getName).toArray(String[]::new));
		return properties;
	}

	/**
	 * The flavors an invoker speaks: one, or several — the messaging
	 * invoker speaks every flavor that travels over a messaging broker,
	 * because there the API is one and the flavor says the rest.
	 */
	static List<FlavorKind> flavorsOf(Map<String, Object> properties) {
		Object declared = properties == null ? null : properties.get(ServiceInvoker.FLAVOR_PROPERTY);
		List<Object> names = new ArrayList<>();
		if (declared instanceof Object[] several) {
			names.addAll(Arrays.asList(several));
		} else if (declared instanceof Collection<?> several) {
			names.addAll(several);
		} else if (declared != null) {
			names.add(declared);
		}
		List<FlavorKind> kinds = new ArrayList<>();
		for (Object name : names) {
			FlavorKind kind = FlavorKind.getByName(String.valueOf(name));
			if (kind != null) {
				kinds.add(kind);
			}
		}
		return kinds;
	}
}
