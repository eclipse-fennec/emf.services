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
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.junit.jupiter.api.DisplayName;
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

	// ------------------------------------------------------------------
	// Loss, and saying so (#124)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a publish that failed is not just logged — the subscribers get told to re-read")
	void aFailedPublishOwesAResync() {
		AtomicBoolean broken = new AtomicBoolean(true);
		MqttEventSink sink = new MqttEventSink((topic, payload) -> {
			if (broken.get()) {
				throw new IllegalStateException("broker unreachable");
			}
			sent.add(new Published(topic, new String(payload, StandardCharsets.UTF_8)));
		}, lookup, new ResourceSetObjects(), "ddsr/events");

		sink.publish(registered("ref-1", "Payment"));
		assertThat(sink.owesResync())
				.as("the loss is remembered, because saying it now would fail too")
				.isTrue();

		// The connection comes back. The debt is paid BEFORE the next
		// event, so a consumer re-reads and then applies what follows.
		broken.set(false);
		sink.publish(registered("ref-2", "Payment"));

		assertThat(sent).extracting(Published::topic)
				.containsExactly("ddsr/events/_resync", "ddsr/events/Payment");
		assertThat(sink.owesResync()).isFalse();
	}

	@Test
	@DisplayName("an event that cannot even be rendered is a loss like any other")
	void anUnrenderableEventOwesAResync() {
		// Not a malformed event — one with no reference still renders and
		// goes out on the _unknown topic, which is deliberate. What
		// cannot be rendered is an event whose codec is unavailable, and
		// from a consumer's side that is indistinguishable from an event
		// it simply never received.
		MqttEventSink sink = new MqttEventSink(publisher, lookup,
				new ComponentServiceObjects<ResourceSet>() {

					@Override
					public ResourceSet getService() {
						throw new IllegalStateException("no ResourceSet to render with");
					}

					@Override
					public void ungetService(ResourceSet service) {
					}

					@Override
					public ServiceReference<ResourceSet> getServiceReference() {
						throw new UnsupportedOperationException("not needed");
					}
				}, "ddsr/events");

		sink.publish(registered("ref-1", "Payment"));

		assertThat(sent).as("nothing reached the wire").isEmpty();
		assertThat(sink.owesResync()).isTrue();
	}

	@Test
	@DisplayName("being told to resync sends the marker straight away when the transport is fine")
	void aResyncIsSentImmediatelyWhenItCan() {
		MqttEventSink sink = sink();

		sink.resyncRequired();

		assertThat(sent).extracting(Published::topic).containsExactly("ddsr/events/_resync");
		assertThat(sink.owesResync()).isFalse();
	}

	@Test
	@DisplayName("a resync that cannot be sent is kept, not lost")
	void aResyncThatFailsIsRemembered() {
		MqttEventSink sink = new MqttEventSink((topic, payload) -> {
			throw new IllegalStateException("broker unreachable");
		}, lookup, new ResourceSetObjects(), "ddsr/events");

		sink.resyncRequired();

		assertThat(sink.owesResync())
				.as("a transport that cannot publish an event cannot publish the warning either")
				.isTrue();
	}

	@Test
	@DisplayName("one resync per loss, not one per event afterwards")
	void theResyncIsSentOnce() {
		MqttEventSink sink = sink();

		sink.resyncRequired();
		sink.publish(registered("ref-1", "Payment"));
		sink.publish(registered("ref-2", "Payment"));

		assertThat(sent).extracting(Published::topic)
				.containsExactly("ddsr/events/_resync", "ddsr/events/Payment", "ddsr/events/Payment");
	}

	@Test
	@DisplayName("the resync topic follows the configured prefix, like everything else")
	void theResyncTopicFollowsThePrefix() {
		MqttEventSink custom = new MqttEventSink(publisher, lookup, new ResourceSetObjects(), "acme/ddsr");

		custom.resyncRequired();

		assertThat(sent).extracting(Published::topic).containsExactly("acme/ddsr/_resync");
	}

	@Test
	@DisplayName("being told to resync must not throw either — it is still a sink call")
	void resyncRequiredDoesNotEscape() {
		MqttEventSink hostile = new MqttEventSink((topic, payload) -> {
			throw new IllegalStateException("broker unreachable");
		}, lookup, new ResourceSetObjects(), "ddsr/events");

		assertThatCode(hostile::resyncRequired).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("a healthy sink owes nothing")
	void nothingIsOwedOnTheNormalPath() {
		MqttEventSink sink = sink();

		sink.publish(registered("ref-1", "Payment"));

		assertThat(sink.owesResync()).isFalse();
		assertThat(sent).extracting(Published::topic).containsExactly("ddsr/events/Payment");
	}
}
