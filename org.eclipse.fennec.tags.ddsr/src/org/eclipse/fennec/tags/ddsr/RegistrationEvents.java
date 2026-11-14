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


package org.eclipse.fennec.tags.ddsr;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.osgi.service.component.annotations.Component;

/**
 * The broker's registrations, heard as an {@link EventSink} — the
 * in-process side of the {@code BrokerEvents} contract — and handed on to
 * whoever listens.
 *
 * <p>A component of its own, with no references, on purpose: the broker
 * takes every sink while it activates, and a sink that needed the broker's
 * catalog would ask for the very component being activated (a circular
 * reference DS refuses — it showed in the harness, not in a slower start).
 * {@link DdsrTagSubjects} needs the catalog and listens here instead.
 */
@Component(service = { EventSink.class, RegistrationEvents.class })
public final class RegistrationEvents implements EventSink {

	private static final Logger LOG = Logger.getLogger(RegistrationEvents.class.getName());

	private final List<Consumer<ServiceProvider>> listeners = new CopyOnWriteArrayList<>();

	/** Tells the listener about every registration: the provider document of the event. */
	public AutoCloseable onRegistered(Consumer<ServiceProvider> listener) {
		listeners.add(listener);
		return () -> listeners.remove(listener);
	}

	@Override
	public void publish(ServiceEvent event) {
		if (event == null || event.getType() != ServiceEventType.REGISTERED || event.getReference() == null
				|| !(event.getReference().getProvider() instanceof ServiceProvider provider)) {
			return;
		}
		for (Consumer<ServiceProvider> listener : listeners) {
			try {
				listener.accept(provider);
			} catch (RuntimeException failure) {
				// A sink must not throw: the broker's mutation is committed.
				LOG.log(Level.WARNING, "[Tags] a registration could not be handed on", failure);
			}
		}
	}
}
