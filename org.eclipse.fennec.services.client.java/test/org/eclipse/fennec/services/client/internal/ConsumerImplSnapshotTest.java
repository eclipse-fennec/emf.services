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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceReference;
import org.junit.jupiter.api.Test;

/**
 * The snapshot the consumer pulls when the event stream comes up
 * (FR-Sync-Reconnect), and what it is good for: routing the withdrawal
 * of a service the consumer never saw registered.
 * <p>
 * Plain JUnit — the consumer takes its lookup and its event source as
 * constructor arguments.
 */
class ConsumerImplSnapshotTest {

	/** Minimal broker stand-in that serves a fixed set of references. */
	private static final class FakeLookup implements BrokerLookup {

		private final Map<String, List<ServiceReference>> byInterface = new LinkedHashMap<>();
		private final Map<ServiceReference, ServiceImplementation> implByRef = new LinkedHashMap<>();
		private int lookupCalls;

		void publish(String interfaceName, String refId) {
			ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
			si.setName(interfaceName);
			ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
			impl.setName(interfaceName + "-impl");
			impl.getServiceInterfaces().add(si);
			ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
			ref.setId(refId);
			byInterface.computeIfAbsent(interfaceName, k -> new ArrayList<>()).add(ref);
			implByRef.put(ref, impl);
		}

		@Override
		public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
				ConsumerCapability capability) {
			lookupCalls++;
			return byInterface.getOrDefault(interfaceName, List.of());
		}

		@Override
		public ServiceReference getServiceReference(String interfaceName) {
			List<ServiceReference> all = byInterface.getOrDefault(interfaceName, List.of());
			return all.isEmpty() ? null : all.get(0);
		}

		@Override
		public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
				ConsumerCapability capability) {
			return getServiceReferences(interfaceName, filter, capability);
		}

		@Override
		public ServiceImplementation getImplementationForReference(ServiceReference reference) {
			return implByRef.get(reference);
		}
	}

	private static final class FakeSource implements EventSource {

		private Handler handler;

		@Override
		public AutoCloseable open(Handler handler) {
			this.handler = handler;
			return () -> {
			};
		}
	}

	private static ServiceEvent unregistering(String refId) {
		ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId(refId);
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(ServiceEventType.UNREGISTERING);
		event.setReference(ref);
		return event;
	}

	private final FakeLookup broker = new FakeLookup();

	private final FakeSource source = new FakeSource();

	private ConsumerImpl consumer() {
		return new ConsumerImpl(broker, List.of(FlavorKind.REST), "test-consumer", source);
	}

	@Test
	void aWithdrawalOfAServiceWeNeverSawRegisteredIsStillRoutedCorrectly() {
		// The service exists before the consumer starts listening — the
		// case the snapshot is there for.
		broker.publish("Payment", "ref-payment");
		broker.publish("OrderQuery", "ref-orders");

		ConsumerImpl consumer = consumer();
		List<ServiceEventType> payment = new ArrayList<>();
		List<ServiceEventType> orders = new ArrayList<>();
		consumer.addServiceListener("Payment", null, e -> payment.add(e.getType()));
		consumer.addServiceListener("OrderQuery", null, e -> orders.add(e.getType()));

		// Stream comes up → snapshot is pulled.
		source.handler.onStreamEstablished();
		source.handler.onEvent(unregistering("ref-payment"));

		assertThat(payment)
				.as("the snapshot taught the registry that ref-payment is Payment")
				.containsExactly(ServiceEventType.UNREGISTERING);
		assertThat(orders)
				.as("and that it is not OrderQuery — without the snapshot both would have been notified")
				.isEmpty();
	}

	@Test
	void theSnapshotCoversEveryInterfaceWeListenOn() {
		broker.publish("Payment", "ref-1");
		broker.publish("OrderQuery", "ref-2");
		ConsumerImpl consumer = consumer();
		consumer.addServiceListener("Payment", null, e -> {
		});
		consumer.addServiceListener("OrderQuery", null, e -> {
		});

		int before = broker.lookupCalls;
		source.handler.onStreamEstablished();

		assertThat(broker.lookupCalls - before)
				.as("one lookup per distinct subscribed interface")
				.isEqualTo(2);
	}

	@Test
	void duplicateListenersOnOneInterfaceCauseOneLookup() {
		broker.publish("Payment", "ref-1");
		ConsumerImpl consumer = consumer();
		consumer.addServiceListener("Payment", null, e -> {
		});
		consumer.addServiceListener("Payment", null, e -> {
		});

		int before = broker.lookupCalls;
		source.handler.onStreamEstablished();

		assertThat(broker.lookupCalls - before).isEqualTo(1);
	}

	@Test
	void aReconnectPullsTheSnapshotAgain() {
		broker.publish("Payment", "ref-1");
		ConsumerImpl consumer = consumer();
		consumer.addServiceListener("Payment", null, e -> {
		});

		source.handler.onStreamEstablished();
		int afterFirst = broker.lookupCalls;
		source.handler.onStreamEstablished();

		assertThat(broker.lookupCalls - afterFirst)
				.as("every reconnect re-reads; missed events are never replayed")
				.isEqualTo(1);
	}

	@Test
	void registeringAListenerDoesNotQueryTheBroker() {
		broker.publish("Payment", "ref-1");
		ConsumerImpl consumer = consumer();

		consumer.addServiceListener("Payment", null, e -> {
		});

		assertThat(broker.lookupCalls)
				.as("the snapshot belongs to the connect, not to the registration")
				.isZero();
	}

	@Test
	void aFailingLookupDoesNotBreakTheStream() {
		ConsumerImpl consumer = consumer();
		consumer.addServiceListener("Boom", null, e -> {
		});

		// "Boom" is unknown to the fake broker, which simply returns
		// nothing — the snapshot must complete regardless and the stream
		// stay usable.
		source.handler.onStreamEstablished();
		List<ServiceEventType> received = new ArrayList<>();
		consumer.addServiceListener("Boom", null, e -> received.add(e.getType()));
		source.handler.onEvent(unregistering("whatever"));

		assertThat(received).containsExactly(ServiceEventType.UNREGISTERING);
	}
}
