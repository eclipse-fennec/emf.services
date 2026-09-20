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
import java.util.Collection;
import java.util.Date;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;

/**
 * How the broker says what happened.
 *
 * <p>Handed to whoever announces, instead of every method reaching for
 * the sink. That is the point of having it: the concerns that emit are
 * now the ones that hold this, and the concerns that do not, cannot.
 *
 * <p>Two rules travel with it, and both are load-bearing. An event goes
 * out only after the change behind it was saved, so a subscriber never
 * hears about something a restart would undo. And it goes out while the
 * write lock is still held, so per-service order matches the order the
 * mutations were applied in — which means foreign subscriber code runs
 * under the broker lock, deliberately. That second rule is what #124
 * is about and what the next step removes; this one prepares it.
 *
 * <p><b>Every event carries its own moment.</b> Until now only the
 * withdraw and retire paths handed over a detached copy, because by
 * then the live implementation was already gone; every other event
 * carried the LIVE reference and was rendered by the sink, under the
 * lock, against state that could not change while it ran. Take the
 * delivery off the lock and that stops being true — the document would
 * describe whatever the registry looked like when the sink got around
 * to it. So the copy moves here, to the moment the event is about, for
 * all of them. It also makes true for every event what
 * {@code EventDocument} and the withdraw path already wanted: no live
 * registration, and no cross-reference into the broker's snapshot
 * resource, can leak into what goes on the wire.
 */
final class Announcements {

	private final EventDelivery delivery;

	Announcements(EventDelivery delivery) {
		this.delivery = delivery;
	}

	void emit(ServiceEventType type, ServiceReference reference) {
		emit(type, reference, null);
	}

	/**
	 * Hands one lifecycle event to the sink.
	 *
	 * <p>Handed over, not delivered: {@link EventDelivery} decides the
	 * position here, under the write lock, and carries it out afterwards
	 * on its own thread. What a sink then does, including throwing, can
	 * no longer reach the change this event is about.
	 */
	void emit(ServiceEventType type, ServiceReference reference, String reason) {
		if (reference == null) {
			return;
		}
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(type);
		event.setReference(snapshot(reference));
		event.setTimestamp(new Date());
		event.setReasonCode(reason);
		delivery.submit(event);
	}

	/**
	 * The reference as it was at this moment, detached from live state.
	 *
	 * <p>A reference that no longer has a registration has already been
	 * through {@link #selfContained} — the withdraw and retire paths
	 * build theirs before the detach, because afterwards there would be
	 * nothing left to copy. Everything else is still live and is copied
	 * here.
	 */
	private static ServiceReference snapshot(ServiceReference reference) {
		ServiceRegistration registration = reference.getRegistration();
		if (registration == null) {
			return reference;
		}
		ServiceImplementation implementation = registration.getImplementation();
		ServiceProvider provider = reference.getProvider();
		if (implementation == null || provider == null) {
			// Nothing to build a subtree from; the reference alone still
			// carries the id, which is what consumers match on.
			return reference;
		}
		return selfContained(provider, implementation, reference);
	}

	/**
	 * A detached copy of the withdrawn reference whose provider subtree
	 * contains exactly the withdrawn implementation (plus copies of its
	 * interfaces), so that {@code EventDocument} can build a
	 * self-contained UNREGISTERING document although the live lookup no
	 * longer resolves the implementation by the time the event goes out.
	 * <p>
	 * Copied with {@code useOriginalReferences = false}: references to
	 * anything outside the copy set (notably the registration and its
	 * eOpposite) are dropped instead of pointing back into — and via
	 * eOpposite mutating — live broker state.
	 */
	static ServiceReference selfContained(ServiceProvider provider,
			ServiceImplementation implementation, ServiceReference reference) {
		if (reference == null) {
			return null;
		}
		EcoreUtil.Copier copier = new EcoreUtil.Copier(true, false);
		Collection<EObject> originals = new ArrayList<>();
		originals.add(reference);
		originals.add(provider);
		originals.addAll(implementation.getServiceInterfaces());
		copier.copyAll(originals);
		copier.copyReferences();

		ServiceProvider providerCopy = (ServiceProvider) copier.get(provider);
		ServiceImplementation implCopy = (ServiceImplementation) copier.get(implementation);
		providerCopy.getImplementations().removeIf(other -> other != implCopy);

		ServiceReference referenceCopy = (ServiceReference) copier.get(reference);
		referenceCopy.setProvider(providerCopy);
		return referenceCopy;
	}
}
