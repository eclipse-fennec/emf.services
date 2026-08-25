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

package org.eclipse.fennec.services.broker.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;

/**
 * Turns a {@link ServiceEvent} into the set of roots that make up a
 * self-contained wire document, and answers which flavors the affected
 * service speaks.
 * <p>
 * Lives here rather than in a transport because every transport needs
 * exactly the same thing: SSE, MQTT and whatever comes next all have to
 * ship an event that a receiver can read without dereferencing anything.
 * Keeping one copy also keeps the two subtleties in one place instead of
 * re-learning them per transport.
 */
public final class EventDocument {

	private EventDocument() {
	}

	/**
	 * Roots of a self-contained document for {@code event}: the event
	 * itself plus the reference, its implementation, provider and
	 * interfaces — all copied, so cross-references point inside the
	 * document.
	 * <p>
	 * Two things this gets right, both learned the hard way:
	 * <ul>
	 *   <li>Without the copy step EMF emits cross-document hrefs into the
	 *       broker's own snapshot file, i.e. the {@code file:}-URI leak of
	 *       W1 — in a payload handed to every subscriber.</li>
	 *   <li>The provider is included even when the implementation can no
	 *       longer be resolved, which is the normal state for an
	 *       UNREGISTERING event: the implementation is detached by then.
	 *       Leaving it out puts a dangling cross-reference on the wire.</li>
	 * </ul>
	 */
	public static List<EObject> roots(ServiceEvent event, BrokerLookup lookup) {
		Set<EObject> live = new LinkedHashSet<>();
		live.add(event);
		ServiceReference reference = event.getReference();
		if (reference != null) {
			live.add(reference);
			if (reference.getProvider() != null) {
				live.add(reference.getProvider());
			}
			ServiceImplementation impl = implementationOf(reference, lookup);
			if (impl != null) {
				live.add(impl);
				if (impl.eContainer() instanceof ServiceProvider) {
					live.add(impl.eContainer());
				}
				live.addAll(impl.getServiceInterfaces());
			}
		}

		EcoreUtil.Copier copier = new EcoreUtil.Copier();
		copier.copyAll(live);
		copier.copyReferences();

		// Only the copies that nothing else contains become roots; the
		// containment children ride along with their container.
		List<EObject> roots = new ArrayList<>();
		for (EObject original : live) {
			EObject copy = copier.get(original);
			if (copy != null && copy.eContainer() == null) {
				roots.add(copy);
			}
		}
		return roots;
	}

	/**
	 * Flavors the event's service speaks, for per-subscriber filtering
	 * (FR-Sync-Filtering). Empty means unknown — a caller should then
	 * deliver rather than withhold, the same way the lookup backend
	 * accepts an implementation that declares no flavor.
	 */
	public static Set<FlavorKind> flavorsOf(ServiceEvent event, BrokerLookup lookup) {
		Set<FlavorKind> kinds = EnumSet.noneOf(FlavorKind.class);
		ServiceImplementation impl = implementationOf(event.getReference(), lookup);
		if (impl != null) {
			for (ServiceFlavor flavor : impl.getFlavors()) {
				if (flavor.getKind() != null) {
					kinds.add(flavor.getKind());
				}
			}
		}
		return kinds;
	}

	/** Interface names the event's service serves, or empty if unknown. */
	public static Set<String> interfaceNamesOf(ServiceEvent event, BrokerLookup lookup) {
		Set<String> names = new LinkedHashSet<>();
		ServiceImplementation impl = implementationOf(event.getReference(), lookup);
		if (impl != null) {
			for (ServiceInterface si : impl.getServiceInterfaces()) {
				if (si.getName() != null) {
					names.add(si.getName());
				}
			}
		}
		return names;
	}

	private static ServiceImplementation implementationOf(ServiceReference reference, BrokerLookup lookup) {
		if (reference == null) {
			return null;
		}
		ServiceImplementation impl = lookup != null ? lookup.getImplementationForReference(reference) : null;
		if (impl != null) {
			return impl;
		}
		// Fallback for self-contained references: the withdraw path hands
		// the sinks a detached reference whose provider subtree carries
		// exactly the affected implementation, because by emit time the
		// live lookup no longer resolves it. Only an unambiguous subtree
		// counts — guessing among several implementations would route an
		// event to the wrong interface, which is worse than the
		// deliver-to-all fallback downstream.
		ServiceProvider provider = reference.getProvider();
		if (provider != null && provider.getImplementations().size() == 1) {
			return provider.getImplementations().get(0);
		}
		return null;
	}
}
