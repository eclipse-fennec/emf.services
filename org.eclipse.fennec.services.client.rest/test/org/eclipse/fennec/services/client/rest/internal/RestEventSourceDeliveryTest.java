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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
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

	private static final String EVENT_XMI = """
			<?xml version="1.0" encoding="UTF-8"?>
			<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:services="%s">
			  <services:ServiceEvent type="UNREGISTERING" reference="/1"/>
			  <services:ServiceReference id="ref-42"/>
			</xmi:XMI>
			""".formatted(NS);

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

	private static String message(String document, String type) {
		CloudEvent envelope = CloudEvents.newEnvelope(type, "/test/broker", "application/xml");
		return new String(CloudEventCodec.writeStructured(envelope,
				document.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
	}

	private static String lifecycleMessage(String document) {
		return message(document, CloudEvents.typeOf(ServiceEventType.UNREGISTERING));
	}

	@Test
	void theDocumentInsideTheEnvelopeBecomesAServiceEvent() {
		source().deliver(handler, lifecycleMessage(EVENT_XMI));

		assertThat(received).hasSize(1);
		assertThat(received.get(0).getType()).isEqualTo(ServiceEventType.UNREGISTERING);
		assertThat(received.get(0).getReference().getId()).isEqualTo("ref-42");
	}

	@Test
	void aFrameThatIsNotACloudEventIsSkippedWithoutEndingTheStream() {
		RestEventSource source = source();

		source.deliver(handler, EVENT_XMI);
		source.deliver(handler, lifecycleMessage(EVENT_XMI));

		assertThat(received)
				.as("a bare document is no longer the wire shape, and one bad frame must not"
						+ " cost the next good one")
				.hasSize(1);
	}

	@Test
	void anEventOfAForeignTypeIsNotDelivered() {
		source().deliver(handler, message(EVENT_XMI, "com.example.something.happened"));

		assertThat(received).isEmpty();
	}

	@Test
	void anEnvelopeWithoutItsDocumentIsNotDelivered() {
		String withoutPayload = new String(CloudEventCodec.writeStructured(
				CloudEvents.newEnvelope(CloudEvents.typeOf(ServiceEventType.REGISTERED),
						"/test/broker", "application/xml"),
				null), StandardCharsets.UTF_8);

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
