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
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.EventSink;

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
 * under the broker lock, deliberately.
 */
final class Announcements {

	private final EventSink sink;

	Announcements(EventSink sink) {
		this.sink = sink != null ? sink : EventSink.NOOP;
	}

	void emit(ServiceEventType type, ServiceReference reference) {
		emit(type, reference, null);
	}

	/**
	 * Hands one lifecycle event to the sink.
	 *
	 * <p>A sink must not throw, but we do not trust it to keep that
	 * promise: a misbehaving subscriber may not undo a change the broker
	 * has already committed and saved.
	 */
	void emit(ServiceEventType type, ServiceReference reference, String reason) {
		if (reference == null) {
			return;
		}
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(type);
		event.setReference(reference);
		event.setTimestamp(new Date());
		event.setReasonCode(reason);
		try {
			sink.publish(event);
		} catch (RuntimeException sinkFailure) {
			// Swallowed deliberately: see the class comment.
		}
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
