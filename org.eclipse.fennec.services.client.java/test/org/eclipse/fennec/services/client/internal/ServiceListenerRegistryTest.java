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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.fennec.services.client.DdsrServiceListener;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.junit.jupiter.api.Test;

/**
 * Routing and lifecycle of the consumer-side listener registry. Plain
 * JUnit: the registry takes its {@link EventSource} as a constructor
 * argument, so no transport and no OSGi runtime are involved.
 */
class ServiceListenerRegistryTest {

	/** Event source that hands out a handle and lets the test drive it. */
	private static final class FakeSource implements EventSource {

		private Handler handler;
		private int opened;
		private int closed;

		@Override
		public AutoCloseable open(Handler handler) {
			this.handler = handler;
			opened++;
			return () -> closed++;
		}
	}

	private final FakeSource source = new FakeSource();

	private final List<String> reconnects = new ArrayList<>();

	private ServiceListenerRegistry registry() {
		return new ServiceListenerRegistry(source, () -> reconnects.add("established"));
	}

	// --- fixtures -----------------------------------------------------

	/**
	 * A REGISTERED event as it arrives off the wire: the reference points
	 * at a provider whose implementation names the interface.
	 */
	private static ServiceEvent registeredEvent(String refId, String interfaceName) {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName(interfaceName);

		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(interfaceName + "-impl");
		impl.getServiceInterfaces().add(si);

		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("provider-of-" + interfaceName);
		provider.getImplementations().add(impl);

		ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId(refId);
		ref.setProvider(provider);

		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(ServiceEventType.REGISTERED);
		event.setReference(ref);
		return event;
	}

	/**
	 * An UNREGISTERING event as it actually arrives: the implementation is
	 * already detached, so the document carries no interface at all.
	 */
	private static ServiceEvent unregisteringEvent(String refId) {
		ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId(refId);

		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(ServiceEventType.UNREGISTERING);
		event.setReference(ref);
		return event;
	}

	// --- stream lifecycle ---------------------------------------------

	@Test
	void theStreamOpensWithTheFirstListenerAndClosesWithTheLast() {
		ServiceListenerRegistry r = registry();
		assertThat(source.opened).as("nothing to listen to yet").isZero();

		DdsrServiceListener a = event -> {
		};
		DdsrServiceListener b = event -> {
		};
		r.add("Payment", null, a);
		r.add("Payment", null, b);

		assertThat(source.opened).as("one connection, not one per listener").isEqualTo(1);
		assertThat(r.isStreamOpen()).isTrue();

		r.remove(a);
		assertThat(source.closed).as("still one listener left").isZero();

		r.remove(b);
		assertThat(source.closed).isEqualTo(1);
		assertThat(r.isStreamOpen()).isFalse();
	}

	@Test
	void theReturnedHandleRemovesTheListener() throws Exception {
		ServiceListenerRegistry r = registry();
		AtomicInteger hits = new AtomicInteger();

		AutoCloseable handle = r.add("Payment", null, event -> hits.incrementAndGet());
		source.handler.onEvent(registeredEvent("ref-1", "Payment"));
		assertThat(hits.get()).isEqualTo(1);

		handle.close();
		source.handler.onEvent(registeredEvent("ref-2", "Payment"));

		assertThat(hits.get()).as("no delivery after the handle was closed").isEqualTo(1);
		assertThat(r.listenerCount()).isZero();
	}

	@Test
	void aClientWithoutAnEventTransportStillAcceptsListeners() {
		ServiceListenerRegistry r = new ServiceListenerRegistry(null, null);

		r.add("Payment", null, event -> {
		});

		assertThat(r.listenerCount()).isEqualTo(1);
		assertThat(r.isStreamOpen()).as("no transport, so no stream — but no failure either").isFalse();
	}

	@Test
	void aReconnectIsSignalledSoTheConsumerCanReSnapshot() {
		ServiceListenerRegistry r = registry();
		r.add("Payment", null, event -> {
		});

		source.handler.onStreamEstablished();
		source.handler.onStreamEstablished();

		assertThat(reconnects)
				.as("FR-Sync-Reconnect: every (re)connect must be visible to the consumer")
				.hasSize(2);
	}

	@Test
	void aListenerRegisteredBeforeTheTransportGetsConnectedWhenItArrives() {
		// The order that actually happens in OSGi: the component holding
		// the listener came up before the event transport did. Without the
		// retry the listener stayed silently unconnected — that was a real
		// bug, found in a launch, not in a test.
		AtomicInteger delivered = new AtomicInteger();
		FakeSource late = new FakeSource();
		boolean[] transportUp = { false };
		ServiceListenerRegistry r = new ServiceListenerRegistry(
				handler -> transportUp[0] ? late.open(handler) : null, null);

		r.add("Payment", null, event -> delivered.incrementAndGet());
		assertThat(r.isStreamOpen()).as("nothing could carry events yet").isFalse();

		transportUp[0] = true;
		r.transportAvailable();

		assertThat(r.isStreamOpen()).as("the waiting listener must be connected now").isTrue();
		late.handler.onEvent(registeredEvent("ref-1", "Payment"));
		assertThat(delivered.get()).isEqualTo(1);
	}

	@Test
	void transportAvailableIsHarmlessWithoutListeners() {
		ServiceListenerRegistry r = registry();

		r.transportAvailable();

		assertThat(r.isStreamOpen()).as("no listener, so no reason to hold a connection").isFalse();
	}

	// --- routing ------------------------------------------------------

	@Test
	void anEventGoesOnlyToListenersForItsInterface() {
		ServiceListenerRegistry r = registry();
		List<String> payment = new ArrayList<>();
		List<String> orders = new ArrayList<>();
		r.add("Payment", null, event -> payment.add(event.getReference().getId()));
		r.add("OrderQuery", null, event -> orders.add(event.getReference().getId()));

		source.handler.onEvent(registeredEvent("ref-1", "Payment"));

		assertThat(payment).containsExactly("ref-1");
		assertThat(orders).as("must not see an event for another interface").isEmpty();
	}

	@Test
	void anUnregisteringEventIsRoutedFromTheRememberedRegistration() {
		ServiceListenerRegistry r = registry();
		List<ServiceEventType> payment = new ArrayList<>();
		List<ServiceEventType> orders = new ArrayList<>();
		r.add("Payment", null, event -> payment.add(event.getType()));
		r.add("OrderQuery", null, event -> orders.add(event.getType()));

		// The registration teaches the registry which interface ref-1 is.
		source.handler.onEvent(registeredEvent("ref-1", "Payment"));
		// The withdrawal carries no interface information at all.
		source.handler.onEvent(unregisteringEvent("ref-1"));

		assertThat(payment)
				.as("the withdrawal must reach the listener that saw the registration")
				.containsExactly(ServiceEventType.REGISTERED, ServiceEventType.UNREGISTERING);
		assertThat(orders).isEmpty();
	}

	@Test
	void anUnknownReferenceGoesToEveryListenerRatherThanNowhere() {
		ServiceListenerRegistry r = registry();
		List<ServiceEventType> payment = new ArrayList<>();
		List<ServiceEventType> orders = new ArrayList<>();
		r.add("Payment", null, event -> payment.add(event.getType()));
		r.add("OrderQuery", null, event -> orders.add(event.getType()));

		// Never saw a registration for this reference — e.g. the consumer
		// started listening after the service was already published.
		source.handler.onEvent(unregisteringEvent("never-seen"));

		assertThat(payment).as("over-delivery is recoverable, a dropped withdrawal is not")
				.containsExactly(ServiceEventType.UNREGISTERING);
		assertThat(orders).containsExactly(ServiceEventType.UNREGISTERING);
	}

	@Test
	void aReferenceLearnedFromALookupIsAlsoRouted() {
		ServiceListenerRegistry r = registry();
		List<ServiceEventType> payment = new ArrayList<>();
		r.add("Payment", null, event -> payment.add(event.getType()));

		// What a find() would record for the locators it returned.
		r.noteReference("ref-from-lookup", Set.of("Payment"));
		source.handler.onEvent(unregisteringEvent("ref-from-lookup"));

		assertThat(payment).containsExactly(ServiceEventType.UNREGISTERING);
	}

	@Test
	void aThrowingListenerDoesNotStopTheOthers() {
		ServiceListenerRegistry r = registry();
		List<String> survivor = new ArrayList<>();
		r.add("Payment", null, event -> {
			throw new IllegalStateException("listener is broken");
		});
		r.add("Payment", null, event -> survivor.add(event.getReference().getId()));

		source.handler.onEvent(registeredEvent("ref-1", "Payment"));

		assertThat(survivor).containsExactly("ref-1");
	}
}
