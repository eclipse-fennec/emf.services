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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.EventDocument;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.osgi.service.component.ComponentServiceObjects;

/**
 * Publishes broker lifecycle events to MQTT topics — the second
 * implementation of {@link EventSink}, and therefore the test of whether
 * that abstraction actually holds.
 * <p>
 * Nothing in {@code broker.core} and nothing in the SSE transport
 * changed to make this possible. The payload is the same self-contained
 * XMI document, built by the shared {@link EventDocument}, so a consumer
 * reads the identical wire format regardless of how it arrived.
 * <p>
 * The MQTT client is a constructor argument rather than something this
 * class creates, which keeps it testable without a broker and lets the
 * component own connection lifecycle and configuration.
 */
public final class MqttEventSink implements EventSink {

	private static final Logger LOG = Logger.getLogger(MqttEventSink.class.getName());

	/**
	 * The bit of Paho this sink needs, named separately so tests can
	 * stand in for it. Publishing is all we do — subscribing is the
	 * consumer's side, in {@code client.mqtt}.
	 */
	public interface Publisher {

		/**
		 * @param topic   where to publish
		 * @param payload the message body
		 * @throws Exception any transport failure; the sink swallows it,
		 *                   because a distribution problem may not affect
		 *                   the broker's own consistency
		 */
		void publish(String topic, byte[] payload) throws Exception;
	}

	private final Publisher publisher;

	private final BrokerLookup lookup;

	private final ComponentServiceObjects<ResourceSet> rsObjects;

	private final String topicPrefix;

	public MqttEventSink(Publisher publisher, BrokerLookup lookup,
			ComponentServiceObjects<ResourceSet> rsObjects, String topicPrefix) {
		this.publisher = publisher;
		this.lookup = lookup;
		this.rsObjects = rsObjects;
		this.topicPrefix = topicPrefix != null && !topicPrefix.isBlank() ? topicPrefix : "ddsr/events";
	}

	/**
	 * Topic for one event: {@code <prefix>/<interface>}, one publish per
	 * interface the service serves.
	 * <p>
	 * The interface in the topic is what makes MQTT-side filtering
	 * possible at all: a consumer subscribes to {@code ddsr/events/Payment}
	 * instead of receiving everything and discarding most of it. That is
	 * the MQTT-native equivalent of the SSE transport's {@code ?flavors=}
	 * query parameter — same requirement (FR-Sync-Filtering), expressed
	 * the way each transport expresses it.
	 * <p>
	 * When the interface cannot be determined — the normal case for
	 * UNREGISTERING, where the implementation is already detached — the
	 * event goes to {@code <prefix>/#unknown}. Subscribers are expected to
	 * take that topic as well and match on the reference id, which is the
	 * same "over-deliver rather than drop" rule the consumer-side registry
	 * follows.
	 */
	static List<String> topicsFor(String prefix, java.util.Set<String> interfaceNames) {
		if (interfaceNames.isEmpty()) {
			return List.of(prefix + "/" + UNKNOWN_INTERFACE);
		}
		return interfaceNames.stream().map(name -> prefix + "/" + name).toList();
	}

	/** Topic segment for events whose interface is not knowable. */
	static final String UNKNOWN_INTERFACE = "_unknown";

	@Override
	public void publish(ServiceEvent event) {
		byte[] payload;
		List<String> topics;
		try {
			payload = toXmi(event);
			topics = topicsFor(topicPrefix, EventDocument.interfaceNamesOf(event, lookup));
		} catch (Exception renderFailure) {
			LOG.warning("[DDSR-MQTT] could not render ServiceEvent: " + renderFailure);
			return;
		}
		for (String topic : topics) {
			try {
				publisher.publish(topic, payload);
			} catch (Exception publishFailure) {
				// Contract: a sink must not throw. The broker has already
				// committed and persisted this change; a transport that
				// cannot deliver may not turn that into a failure.
				LOG.warning("[DDSR-MQTT] publishing to " + topic + " failed: " + publishFailure);
			}
		}
	}

	private byte[] toXmi(ServiceEvent event) throws java.io.IOException {
		List<EObject> roots = EventDocument.roots(event, lookup);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, rsObjects, roots);
		return out.toString(StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
	}
}
