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

package org.eclipse.fennec.services.client.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.invocation.Invocations;
import org.eclipse.fennec.services.invocation.ResultDocument;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.cloudevents.model.ce.CloudEvent;

/**
 * What arrives in an SSE frame and what the SDK makes of it.
 *
 * <p>The frame carries no headers, so the envelope is in the data:
 * structured mode, exactly what the MQTT transport carries. These tests
 * build the message the way the broker's bridge does rather than
 * hand-writing the JSON, so a change on the writing side reaches them.
 */
class RestEventSourceDeliveryTest {

	private static final String NS = ServicesPackage.eNS_URI;

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

	private final List<ServiceEvent> received = new ArrayList<>();

	private final List<String> established = new ArrayList<>();

	private final EventSource.Handler handler = new EventSource.Handler() {

		@Override
		public void onEvent(ServiceEvent event) {
			received.add(event);
		}

		@Override
		public void onStreamEstablished() {
			established.add("established");
		}
	};

	private RestEventSource source() {
		RestEventSource source = new RestEventSource();
		source.rsObjects = new ResourceSetObjects();
		return source;
	}

	/** An event about ref-42, of a provider whose one implementation speaks the flavor given. */
	private static ServiceEvent event(FlavorKind flavor) {
		ServicesFactory f = ServicesFactory.eINSTANCE;
		ServiceImplementation implementation = f.createServiceImplementation();
		implementation.setName("payments");
		RestFlavor rest = f.createRestFlavor();
		rest.setName("rest");
		rest.setKind(flavor);
		implementation.getFlavors().add(rest);
		ServiceProvider provider = f.createServiceProvider();
		provider.setName("payments-java");
		provider.getImplementations().add(implementation);
		org.eclipse.fennec.services.ServiceReference reference = f.createServiceReference();
		reference.setId("ref-42");
		reference.setProvider(provider);
		ServiceEvent event = f.createServiceEvent();
		event.setType(ServiceEventType.UNREGISTERING);
		event.setReference(reference);
		return event;
	}

	/** A value of the broker's broadcast, written the way the broker writes it. */
	private static String message(ServiceEvent event, String type) {
		ServiceInvocationResult result = Invocations.result(event);
		ByteArrayOutputStream document = new ByteArrayOutputStream();
		try {
			XmiCodec.write(document, new ResourceSetObjects(), "application/xml", ResultDocument.roots(result));
		} catch (IOException unwritable) {
			throw new UncheckedIOException(unwritable);
		}
		CloudEvent envelope = CloudEvents.newEnvelope(type, "/test/broker", "application/xml");
		return new String(CloudEventCodec.writeStructured(envelope, document.toByteArray()), StandardCharsets.UTF_8);
	}

	private static String broadcast(ServiceEvent event) {
		return message(event, CloudEvents.TYPE_BROADCAST);
	}

	@Test
	void theValueOfTheBroadcastBecomesAServiceEvent() {
		source().deliver(handler, broadcast(event(FlavorKind.REST)));

		assertThat(received).hasSize(1);
		assertThat(received.get(0).getType()).isEqualTo(ServiceEventType.UNREGISTERING);
		assertThat(received.get(0).getReference().getId()).isEqualTo("ref-42");
		assertThat(received.get(0).getReference().getProvider().getName())
				.as("what the event is about travels with it").isEqualTo("payments-java");
	}

	/** The events are one stream every receiver shares; the receiver filters (#252). */
	@Test
	void anEventAboutAServiceThisConsumerCannotCallIsNotDelivered() {
		RestEventSource source = source();
		source.flavors = "REST";

		source.deliver(handler, broadcast(event(FlavorKind.MQTT)));
		source.deliver(handler, broadcast(event(FlavorKind.REST)));

		assertThat(received).hasSize(1);
	}

	@Test
	void aFrameThatIsNotACloudEventIsSkippedWithoutEndingTheStream() {
		RestEventSource source = source();

		source.deliver(handler, "<not-a-cloud-event/>");
		source.deliver(handler, broadcast(event(FlavorKind.REST)));

		assertThat(received).as("one bad frame must not cost the next good one").hasSize(1);
	}

	@Test
	void aMessageOfAnotherTypeIsNotDelivered() {
		source().deliver(handler, message(event(FlavorKind.REST), "com.example.something.happened"));

		assertThat(received).isEmpty();
	}

	@Test
	void anEnvelopeWithoutItsDocumentIsNotDelivered() {
		String withoutPayload = new String(CloudEventCodec.writeStructured(
				CloudEvents.newEnvelope(CloudEvents.TYPE_BROADCAST, "/test/broker", "application/xml"), null),
				StandardCharsets.UTF_8);

		source().deliver(handler, withoutPayload);

		assertThat(received).isEmpty();
	}

	@Test
	void anEmptyFrameIsIgnored() {
		source().deliver(handler, "   ");

		assertThat(received).isEmpty();
		assertThat(established).isEmpty();
	}
}
