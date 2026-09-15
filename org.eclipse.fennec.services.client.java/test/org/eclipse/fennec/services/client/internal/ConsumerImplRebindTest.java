/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.ServiceEventReasons;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.client.TrackedServiceLocator;
import org.eclipse.fennec.services.client.TrackedServiceLocator.State;
import org.junit.jupiter.api.Test;

/**
 * #57: locators follow their service. The broker's lifecycle events for
 * the bound reference drive the locator's state; the next use rebinds
 * lazily; {@code reference()} never contacts the broker.
 */
class ConsumerImplRebindTest {

	private static final String PAYMENT = "Payment";

	/** Broker stand-in: references per interface, mutable between calls. */
	private static final class FakeLookup implements BrokerLookup {
		final Map<String, List<ServiceReference>> byInterface = new LinkedHashMap<>();
		final Map<ServiceReference, ServiceImplementation> implByRef = new LinkedHashMap<>();
		int lookupCalls;

		ServiceReference publish(String refId, String host) {
			ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
			si.setName(PAYMENT);
			ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
			provider.setName("provider-of-" + refId);
			ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
			impl.setName("impl-" + refId);
			impl.getServiceInterfaces().add(si);
			RestFlavor rest = ServicesFactory.eINSTANCE.createRestFlavor();
			rest.setName("rest");
			rest.setHost(host);
			rest.setBasePath("/payments");
			impl.getFlavors().add(rest);
			provider.getImplementations().add(impl);
			ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
			ref.setId(refId);
			ref.setProvider(provider);
			byInterface.computeIfAbsent(PAYMENT, k -> new ArrayList<>()).add(ref);
			implByRef.put(ref, impl);
			return ref;
		}

		void retire(String refId) {
			byInterface.getOrDefault(PAYMENT, List.of()).removeIf(r -> refId.equals(r.getId()));
		}

		@Override
		public List<ServiceReference> getServiceReferences(String interfaceName, String filter, ConsumerCapability cap) {
			lookupCalls++;
			return new ArrayList<>(byInterface.getOrDefault(interfaceName, List.of()));
		}

		@Override
		public ServiceReference getServiceReference(String interfaceName) {
			List<ServiceReference> all = byInterface.getOrDefault(interfaceName, List.of());
			return all.isEmpty() ? null : all.get(0);
		}

		@Override
		public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter, ConsumerCapability cap) {
			return getServiceReferences(interfaceName, filter, cap);
		}

		@Override
		public ServiceImplementation getImplementationForReference(ServiceReference reference) {
			return implByRef.get(reference);
		}
	}

	private static final class FakeSource implements EventSource {
		Handler handler;
		int opened;

		@Override
		public AutoCloseable open(Handler handler) {
			this.handler = handler;
			opened++;
			return () -> {
			};
		}

		void emit(ServiceEvent event) {
			handler.onEvent(event);
		}
	}

	private static ServiceEvent bare(ServiceEventType type, String refId, String reason) {
		ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId(refId);
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(type);
		event.setReference(ref);
		event.setReasonCode(reason);
		return event;
	}

	/** A self-contained MODIFIED document: the reference carries its provider subtree with the changed endpoint. */
	private static ServiceEvent modified(String refId, String newHost) {
		FakeLookup scratch = new FakeLookup();
		ServiceReference ref = scratch.publish(refId, newHost);
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(ServiceEventType.MODIFIED);
		event.setReference(ref);
		return event;
	}

	private static String hostOf(ServiceLocator locator) {
		return locator.restFlavor().map(RestFlavor::getHost).orElse(null);
	}

	private final FakeLookup broker = new FakeLookup();
	private final FakeSource source = new FakeSource();

	private ConsumerImpl consumer(boolean greedy) {
		return new ConsumerImpl(broker, List.of(FlavorKind.REST), "c1", source, greedy);
	}

	@Test
	void aLocatorFromFindIsTrackedAndKeepsTheStreamOpen() {
		broker.publish("ref-1", "http://a:1");
		ConsumerImpl consumer = consumer(false);

		TrackedServiceLocator locator = (TrackedServiceLocator) consumer.find(PAYMENT, null).get(0);

		assertThat(locator.state()).isEqualTo(State.LIVE);
		assertThat(locator.interfaceName()).isEqualTo(PAYMENT);
		assertThat(source.opened).as("tracking is interest — the event stream opens").isEqualTo(1);
		assertThat(consumer.trackedLocatorCount()).isEqualTo(1);
	}

	@Test
	void modifiedRefreshesTheLocatorInPlaceWithoutALookup() {
		broker.publish("ref-1", "http://a:1");
		ConsumerImpl consumer = consumer(false);
		TrackedServiceLocator locator = (TrackedServiceLocator) consumer.find(PAYMENT, null).get(0);
		int lookups = broker.lookupCalls;

		source.emit(modified("ref-1", "http://a:2"));

		assertThat(locator.state()).isEqualTo(State.LIVE);
		assertThat(locator.reference().getId()).as("same registration").isEqualTo("ref-1");
		assertThat(hostOf(locator)).as("the endpoint moved with it").isEqualTo("http://a:2");
		assertThat(broker.lookupCalls).isEqualTo(lookups);
	}

	@Test
	void coldifiedIsStaleNotGoneAndTheNextUseRehydratesUnderAFreshId() {
		broker.publish("ref-1", "http://a:1");
		ConsumerImpl consumer = consumer(false);
		TrackedServiceLocator locator = (TrackedServiceLocator) consumer.find(PAYMENT, null).get(0);

		source.emit(bare(ServiceEventType.UNREGISTERING, "ref-1", ServiceEventReasons.COLDIFIED));
		assertThat(locator.state()).isEqualTo(State.STALE);
		assertThat(locator.reference().getId()).as("reference() never contacts the broker").isEqualTo("ref-1");

		// the broker rehydrates on lookup: the same service, a new id
		broker.retire("ref-1");
		broker.publish("ref-1b", "http://a:1");
		assertThat(hostOf(locator)).isEqualTo("http://a:1");
		assertThat(locator.state()).isEqualTo(State.LIVE);
		assertThat(locator.reference().getId()).isEqualTo("ref-1b");
	}

	@Test
	void withdrawnRebindsToAnotherRegistrationOrFailsLoudly() {
		broker.publish("ref-1", "http://a:1");
		broker.publish("ref-2", "http://b:1");
		ConsumerImpl consumer = consumer(false);
		TrackedServiceLocator locator = (TrackedServiceLocator) consumer.find(PAYMENT, null).get(0);
		assertThat(locator.reference().getId()).isEqualTo("ref-1");

		broker.retire("ref-1");
		source.emit(bare(ServiceEventType.UNREGISTERING, "ref-1", ServiceEventReasons.WITHDRAWN));
		assertThat(locator.state()).isEqualTo(State.REBIND);

		assertThat(hostOf(locator)).as("bound to the neighbour").isEqualTo("http://b:1");
		assertThat(locator.reference().getId()).isEqualTo("ref-2");

		broker.retire("ref-2");
		source.emit(bare(ServiceEventType.RETIRED, "ref-2", ServiceEventReasons.CUTOVER));
		assertThatThrownBy(locator::restFlavor)
				.isInstanceOf(DdsrException.class)
				.hasMessageContaining("not available");
	}

	@Test
	void replacedRebindsToTheSuccessorAnnouncedRightAfter() {
		broker.publish("ref-old", "http://a:1");
		ConsumerImpl consumer = consumer(false);
		TrackedServiceLocator locator = (TrackedServiceLocator) consumer.find(PAYMENT, null).get(0);

		broker.retire("ref-old");
		broker.publish("ref-new", "http://a:9");
		source.emit(bare(ServiceEventType.UNREGISTERING, "ref-old", ServiceEventReasons.REPLACED));
		source.emit(bare(ServiceEventType.REGISTERED, "ref-new", null));

		assertThat(hostOf(locator)).isEqualTo("http://a:9");
		assertThat(locator.reference().getId()).isEqualTo("ref-new");
	}

	@Test
	void upgradeAvailableIsAHintForNonGreedyAndARebindForGreedyConsumers() {
		broker.publish("ref-v1", "http://v1:1");
		broker.publish("ref-v2", "http://v2:1");
		ConsumerImpl lazy = consumer(false);
		TrackedServiceLocator kept = (TrackedServiceLocator) lazy.find(PAYMENT, null).get(0);
		source.emit(bare(ServiceEventType.UPGRADE_AVAILABLE, "ref-v1", null));
		assertThat(kept.state()).isEqualTo(State.LIVE);
		assertThat(hostOf(kept)).isEqualTo("http://v1:1");

		FakeSource greedySource = new FakeSource();
		ConsumerImpl greedy = new ConsumerImpl(broker, List.of(FlavorKind.REST), "c2", greedySource, true);
		TrackedServiceLocator switching = (TrackedServiceLocator) greedy.find(PAYMENT, null).get(0);
		greedySource.emit(bare(ServiceEventType.UPGRADE_AVAILABLE, "ref-v1", null));
		assertThat(switching.state()).isEqualTo(State.REBIND);
		assertThat(hostOf(switching)).as("greedy: away from the predecessor even though it is still listed")
				.isEqualTo("http://v2:1");
	}

	@Test
	void rebindExcludingTheCurrentRegistrationIsWhatTheProxyUsesAfterATransportFailure() {
		broker.publish("ref-1", "http://a:1");
		broker.publish("ref-2", "http://b:1");
		ConsumerImpl consumer = consumer(false);
		TrackedServiceLocator locator = (TrackedServiceLocator) consumer.find(PAYMENT, null).get(0);

		assertThat(locator.rebind(true)).isTrue();
		assertThat(locator.reference().getId()).isEqualTo("ref-2");

		broker.retire("ref-1");
		assertThat(locator.rebind(true)).as("nothing but the failed one left").isFalse();
		assertThat(locator.reference().getId()).as("unchanged on failure").isEqualTo("ref-2");
	}

	@Test
	void afterARebindEventsForTheNewReferenceReachTheLocator() {
		broker.publish("ref-1", "http://a:1");
		broker.publish("ref-2", "http://b:1");
		ConsumerImpl consumer = consumer(false);
		TrackedServiceLocator locator = (TrackedServiceLocator) consumer.find(PAYMENT, null).get(0);
		locator.rebind(true);
		assertThat(locator.reference().getId()).isEqualTo("ref-2");

		source.emit(bare(ServiceEventType.UNREGISTERING, "ref-2", ServiceEventReasons.WITHDRAWN));

		assertThat(locator.state()).as("the tracker re-keyed the locator").isEqualTo(State.REBIND);
		assertThat(consumer.trackedLocatorCount()).as("find() handed out two locators; both stay tracked").isEqualTo(2);
	}

	@Test
	void retiredForgetsTheReferenceInTheRoutingMemo() {
		broker.publish("ref-1", "http://a:1");
		ConsumerImpl consumer = consumer(false);
		consumer.find(PAYMENT, null);
		assertThat(consumer.knownReferenceIds()).contains("ref-1");

		source.emit(bare(ServiceEventType.RETIRED, "ref-1", ServiceEventReasons.REPLACED));

		assertThat(consumer.knownReferenceIds()).doesNotContain("ref-1");
	}
}
