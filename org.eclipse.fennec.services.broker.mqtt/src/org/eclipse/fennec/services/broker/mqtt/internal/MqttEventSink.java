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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceInvocationResult;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.EventDocument;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.invocation.Invocations;
import org.eclipse.fennec.services.invocation.ResultDocument;
import org.eclipse.fennec.services.messaging.Channels;
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
	 * The bit of the connection this sink needs, named separately so tests can
	 * stand in for it. Publishing is all we do — subscribing is the
	 * consumer's side, in {@code client.messaging}.
	 */
	public interface Publisher {

		/**
		 * @param topic   where to publish
		 * @param message the event: its envelope, and the document it
		 *                carries or none
		 * @throws Exception any transport failure; the sink swallows it,
		 *                   because a distribution problem may not affect
		 *                   the broker's own consistency
		 */
		void publish(String topic, CloudEventCodec.Message message) throws Exception;
	}

	private final Publisher publisher;

	private final BrokerLookup lookup;

	private final ComponentServiceObjects<ResourceSet> rsObjects;

	private final String topicPrefix;

	private final String eventSource;

	private final Channels channels;

	public MqttEventSink(Publisher publisher, BrokerLookup lookup,
			ComponentServiceObjects<ResourceSet> rsObjects, String topicPrefix, String eventSource) {
		this(publisher, lookup, rsObjects, topicPrefix, eventSource, Channels.MQTT);
	}

	/**
	 * @param channels how the messaging broker spells a channel — the
	 *                 prefix is given the way MQTT writes it and becomes
	 *                 a routing key on AMQP (#138)
	 */
	public MqttEventSink(Publisher publisher, BrokerLookup lookup,
			ComponentServiceObjects<ResourceSet> rsObjects, String topicPrefix, String eventSource,
			Channels channels) {
		this.channels = channels;
		this.publisher = publisher;
		this.lookup = lookup;
		this.rsObjects = rsObjects;
		this.topicPrefix = topicPrefix != null && !topicPrefix.isBlank() ? topicPrefix : "ddsr/events";
		this.eventSource = eventSource != null && !eventSource.isBlank() ? eventSource
				: "/fennec/services/broker";
	}

	/** Set while this sink still owes its subscribers a resync signal. */
	private final AtomicBoolean pendingResync = new AtomicBoolean();

	@Override
	public void publish(ServiceEvent event) {
		CloudEventCodec.Message payload;
		List<String> topics;
		try {
			payload = toMessage(event);
			// One channel, the operation's: the events are a stream every
			// receiver shares (#252), and a receiver filters what it hears.
			topics = List.of(topicPrefix);
		} catch (Exception renderFailure) {
			// Nothing was put on the wire, and a consumer cannot tell the
			// difference between an event it never got and one that never
			// existed. It owes them a re-read either way.
			LOG.warning("[DDSR-MQTT] could not render ServiceEvent: " + renderFailure);
			pendingResync.set(true);
			return;
		}
		// Before the event, so a consumer re-reads and then applies what
		// follows, rather than applying first and undoing it afterwards.
		sendPendingResync();
		for (String topic : topics) {
			try {
				publisher.publish(topic, payload);
			} catch (Exception publishFailure) {
				// Contract: a sink must not throw. The broker has already
				// committed and persisted this change; a transport that
				// cannot deliver may not turn that into a failure. What it
				// must not do either is stay quiet about it.
				LOG.warning("[DDSR-MQTT] publishing to " + topic + " failed, subscribers will be"
						+ " told to re-snapshot: " + publishFailure);
				pendingResync.set(true);
			}
		}
	}

	/**
	 * Remembers that subscribers have to re-read, and tries to say so.
	 *
	 * <p>Trying immediately is worth it — the loss may have been the
	 * broker's delivery queue rather than this transport, and then the
	 * connection is fine. When it is this transport that is failing, the
	 * attempt fails too and the debt survives: it is paid before the
	 * next event that gets through, which is the first moment a
	 * subscriber could have heard it anyway.
	 */
	@Override
	public void resyncRequired() {
		pendingResync.set(true);
		sendPendingResync();
	}

	private void sendPendingResync() {
		if (!pendingResync.get()) {
			return;
		}
		String topic = topicPrefix;
		try {
			// The end of the stream, as for any broadcast: every receiver
			// takes a fresh snapshot and goes on listening (#124, #252).
			publisher.publish(topic, new CloudEventCodec.Message(
					CloudEvents.newEnvelope(CloudEvents.TYPE_BROADCAST_END, eventSource, null), null));
			pendingResync.set(false);
			LOG.info("[DDSR-MQTT] told subscribers on " + topic + " to take a fresh snapshot");
		} catch (Exception stillFailing) {
			LOG.fine(() -> "[DDSR-MQTT] still cannot signal a resync on " + topic
					+ ", will retry before the next event: " + stillFailing);
		}
	}

	/** Test seam: does this sink still owe its subscribers a resync? */
	boolean owesResync() {
		return pendingResync.get();
	}

	/**
	 * One event as a CloudEvent: the envelope that says what happened,
	 * and beside it the self-contained XMI document it happened to. How
	 * the two become one message on the wire is the connection's — the
	 * messaging instance writes them in structured mode, the whole event
	 * in one JSON document.
	 */
	private CloudEventCodec.Message toMessage(ServiceEvent event) throws IOException {
		ServiceEvent value = (ServiceEvent) EventDocument.roots(event, lookup).get(0);
		ServiceInvocationResult result = Invocations.result(value);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, rsObjects, XMI_CONTENT_TYPE, ResultDocument.roots(result));
		return new CloudEventCodec.Message(
				CloudEvents.describing(CloudEvents.newEnvelope(CloudEvents.TYPE_BROADCAST, eventSource,
						XMI_CONTENT_TYPE), result),
				out.toByteArray());
	}

	/** What the payload of a lifecycle event is encoded in today. */
	private static final String XMI_CONTENT_TYPE = "application/xml";
}
