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

package org.eclipse.fennec.services.broker.mqtt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

/**
 * The MQTT event sink: topic layout and payload. Plain JUnit — the sink
 * takes its publisher as a constructor argument, so nothing here needs an
 * MQTT broker or an OSGi runtime.
 */
class MqttEventSinkTest {

	private static final String NS = "http://eclipse.org/fennec/services/1.0";

	private record Published(String topic, String payload) {
	}

	private final List<Published> sent = new ArrayList<>();

	private final MqttEventSink.Publisher publisher =
			(topic, payload) -> sent.add(new Published(topic, new String(payload, StandardCharsets.UTF_8)));

	/** Serves implementations for references, like the real broker does. */
	private static final class FakeLookup implements BrokerLookup {

		private final Map<org.eclipse.fennec.services.ServiceReference, ServiceImplementation> impls =
				new LinkedHashMap<>();

		@Override
		public org.eclipse.fennec.services.ServiceReference getServiceReference(String interfaceName) {
			return null;
		}

		@Override
		public List<org.eclipse.fennec.services.ServiceReference> getServiceReferences(String interfaceName,
				String filter, ConsumerCapability capability) {
			return List.of();
		}

		@Override
		public List<org.eclipse.fennec.services.ServiceReference> getAllServiceReferences(String interfaceName,
				String filter, ConsumerCapability capability) {
			return List.of();
		}

		@Override
		public ServiceImplementation getImplementationForReference(
				org.eclipse.fennec.services.ServiceReference reference) {
			return impls.get(reference);
		}
	}

	private final FakeLookup lookup = new FakeLookup();

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

	private MqttEventSink sink() {
		return new MqttEventSink(publisher, lookup, new ResourceSetObjects(), "ddsr/events");
	}

	/** A registration event with a resolvable implementation. */
	private ServiceEvent registered(String refId, String interfaceName) {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName(interfaceName);
		si.setVersion("1.0.0");

		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(interfaceName + "-impl");
		impl.getServiceInterfaces().add(si);

		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("payments-java");
		provider.getImplementations().add(impl);

		org.eclipse.fennec.services.ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId(refId);
		ref.setProvider(provider);
		lookup.impls.put(ref, impl);

		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(ServiceEventType.REGISTERED);
		event.setReference(ref);
		return event;
	}

	/** A withdrawal: the implementation is no longer resolvable. */
	private static ServiceEvent unregistering(String refId) {
		org.eclipse.fennec.services.ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId(refId);
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(ServiceEventType.UNREGISTERING);
		event.setReference(ref);
		return event;
	}

	// ------------------------------------------------------------------

	@Test
	void anEventGoesToATopicPerInterface() {
		sink().publish(registered("ref-1", "Payment"));

		assertThat(sent).extracting(Published::topic).containsExactly("ddsr/events/Payment");
	}

	@Test
	void aWithdrawalWithoutAResolvableInterfaceGoesToTheUnknownTopic() {
		sink().publish(unregistering("ref-gone"));

		assertThat(sent).extracting(Published::topic)
				.as("dropping it would leave consumers bound to a dead service")
				.containsExactly("ddsr/events/" + MqttEventSink.UNKNOWN_INTERFACE);
	}

	/**
	 * A withdrawal as the broker emits it since D1 (DECISIONS_PARITY): the
	 * reference is a detached copy whose provider subtree carries exactly
	 * the withdrawn implementation. The lookup no longer resolves it — the
	 * self-contained subtree must still route to the real topic.
	 */
	@Test
	void aSelfContainedWithdrawalGoesToTheInterfaceTopicNotToUnknown() {
		ServiceEvent event = registered("ref-42", "Payment");
		event.setType(ServiceEventType.UNREGISTERING);
		lookup.impls.clear();

		sink().publish(event);

		assertThat(sent).extracting(Published::topic).containsExactly("ddsr/events/Payment");
		assertThat(sent.get(0).payload())
				.contains("type=\"UNREGISTERING\"")
				.contains("Payment");
	}

	@Test
	void thePayloadIsTheSameSelfContainedDocumentAsOnSse() {
		sink().publish(registered("ref-1", "Payment"));

		String payload = sent.get(0).payload();
		assertThat(payload)
				.as("the required type must be explicit on the wire (W4)")
				.contains("type=\"REGISTERED\"")
				.as("and the interface must travel with the event")
				.contains("Payment");
		assertThat(payload)
				.as("no cross-document href into the broker's snapshot (W1)")
				.doesNotContain("file:/");
	}

	@Test
	void aFailingPublisherDoesNotEscape() {
		MqttEventSink hostile = new MqttEventSink(
				(topic, payload) -> {
					throw new IllegalStateException("broker unreachable");
				},
				lookup, new ResourceSetObjects(), "ddsr/events");

		assertThatCode(() -> hostile.publish(registered("ref-1", "Payment")))
				.as("an EventSink must not throw — the mutation is already committed")
				.doesNotThrowAnyException();
	}

	@Test
	void theTopicPrefixIsConfigurable() {
		MqttEventSink custom = new MqttEventSink(publisher, lookup, new ResourceSetObjects(), "acme/ddsr");

		custom.publish(registered("ref-1", "Payment"));

		assertThat(sent).extracting(Published::topic).containsExactly("acme/ddsr/Payment");
	}

	@Test
	void aBlankPrefixFallsBackToTheDefault() {
		MqttEventSink fallback = new MqttEventSink(publisher, lookup, new ResourceSetObjects(), "  ");

		fallback.publish(registered("ref-1", "Payment"));

		assertThat(sent).extracting(Published::topic).containsExactly("ddsr/events/Payment");
	}
}
