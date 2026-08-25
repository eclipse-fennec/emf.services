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

package org.eclipse.fennec.services.client.mqtt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

/**
 * The MQTT event source: what it subscribes to, and that it turns a
 * received payload back into a {@link ServiceEvent}. Plain JUnit — the
 * source takes its subscriber as a constructor argument, so no broker
 * and no OSGi runtime are involved.
 */
class MqttEventSourceTest {

	private static final String NS = "http://eclipse.org/fennec/services/1.0";

	/** Captures the subscription and lets the test push messages. */
	private static final class FakeSubscriber implements MqttEventSource.Subscriber {

		private String filter;
		private MqttEventSource.MessageListener listener;
		private int closed;

		@Override
		public AutoCloseable subscribe(String topicFilter, MqttEventSource.MessageListener onMessage) {
			this.filter = topicFilter;
			this.listener = onMessage;
			return () -> closed++;
		}
	}

	private static final class ResourceSetObjects implements ComponentServiceObjects<ResourceSet> {

		@Override
		public ResourceSet getService() {
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
			rs.getPackageRegistry().put(NS, ServicesPackage.eINSTANCE);
			return rs;
		}

		@Override
		public void ungetService(ResourceSet service) {
		}

		@Override
		public ServiceReference<ResourceSet> getServiceReference() {
			throw new UnsupportedOperationException("not needed");
		}
	}

	/** The wire format the broker-side sink produces. */
	private static final String EVENT_XMI = """
			<?xml version="1.0" encoding="UTF-8"?>
			<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:services="%s">
			  <services:ServiceEvent type="UNREGISTERING" reference="/1"/>
			  <services:ServiceReference id="ref-42"/>
			</xmi:XMI>
			""".formatted(NS);

	private final FakeSubscriber subscriber = new FakeSubscriber();

	private final List<ServiceEvent> received = new ArrayList<>();

	private final List<String> established = new ArrayList<>();

	private final EventSource.Handler handler = new EventSource.Handler() {

		@Override
		public void onEvent(ServiceEvent event) {
			received.add(event);
		}

		@Override
		public void onStreamEstablished() {
			established.add("up");
		}
	};

	private MqttEventSource source(String prefix) {
		return new MqttEventSource(subscriber, new ResourceSetObjects(), prefix);
	}

	@Test
	void itSubscribesToTheWholeSubtree() {
		source("ddsr/events").open(handler);

		assertThat(subscriber.filter)
				.as("the interest set is not available at open time — see the class comment")
				.isEqualTo("ddsr/events/#");
	}

	@Test
	void aConfiguredPrefixIsUsed() {
		source("acme/ddsr").open(handler);

		assertThat(subscriber.filter).isEqualTo("acme/ddsr/#");
	}

	@Test
	void openingSignalsThatTheStreamIsUp() {
		source("ddsr/events").open(handler);

		assertThat(established)
				.as("the consumer re-snapshots on this signal (FR-Sync-Reconnect)")
				.containsExactly("up");
	}

	@Test
	void aReceivedPayloadBecomesAServiceEvent() {
		source("ddsr/events").open(handler);

		subscriber.listener.onMessage("ddsr/events/Payment", EVENT_XMI.getBytes(StandardCharsets.UTF_8));

		assertThat(received).hasSize(1);
		ServiceEvent event = received.get(0);
		assertThat(event.getType()).isEqualTo(ServiceEventType.UNREGISTERING);
		assertThat(event.getReference()).isNotNull();
		assertThat(event.getReference().getId()).isEqualTo("ref-42");
	}

	@Test
	void anUndecodablePayloadIsSkippedWithoutEndingTheSubscription() {
		source("ddsr/events").open(handler);

		subscriber.listener.onMessage("ddsr/events/Payment", "this is not XMI".getBytes(StandardCharsets.UTF_8));
		subscriber.listener.onMessage("ddsr/events/Payment", EVENT_XMI.getBytes(StandardCharsets.UTF_8));

		assertThat(received)
				.as("one bad message must not cost us the next good one")
				.hasSize(1);
	}

	@Test
	void anEmptyPayloadIsIgnored() {
		source("ddsr/events").open(handler);

		subscriber.listener.onMessage("ddsr/events/Payment", new byte[0]);

		assertThat(received).isEmpty();
	}

	@Test
	void closingTheHandleCancelsTheSubscription() throws Exception {
		AutoCloseable handle = source("ddsr/events").open(handler);

		handle.close();

		assertThat(subscriber.closed).isEqualTo(1);
	}

	@Test
	void aFailingSubscribeYieldsNoHandleInsteadOfThrowing() {
		MqttEventSource hostile = new MqttEventSource(
				(filter, onMessage) -> {
					throw new IllegalStateException("broker unreachable");
				},
				new ResourceSetObjects(), "ddsr/events");

		AutoCloseable[] handle = new AutoCloseable[1];
		assertThatCode(() -> handle[0] = hostile.open(handler)).doesNotThrowAnyException();
		assertThat(handle[0])
				.as("no subscription, so the SDK can retry — throwing would be swallowed by the binder")
				.isNull();
	}
}
