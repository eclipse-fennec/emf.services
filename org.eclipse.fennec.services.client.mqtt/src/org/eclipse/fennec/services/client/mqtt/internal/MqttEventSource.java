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

import java.io.ByteArrayInputStream;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.osgi.service.component.ComponentServiceObjects;

/**
 * Receives lifecycle events over MQTT — the second implementation of
 * {@link EventSource}, and with it the proof that the abstraction holds:
 * neither {@code client.java} nor the SSE source changed to make this
 * work, and the payload is byte-for-byte the same self-contained XMI
 * document.
 * <p>
 * <b>A limitation this transport exposed.</b> {@link EventSource#open}
 * carries no notion of <em>which</em> interfaces the consumer cares
 * about. The SSE source hides that behind a configured
 * {@code ?flavors=} parameter, but MQTT's natural filter is the topic,
 * and the broker publishes one topic per interface precisely so a
 * consumer can subscribe narrowly. Since the interest set is not
 * available here, this source subscribes to the whole subtree
 * ({@code <prefix>/#}) and lets the SDK's listener registry do the
 * routing. Correct, but it moves bytes it could have avoided — see
 * OPEN_ISSUES A2 for the API shape that would fix it.
 */
public final class MqttEventSource implements EventSource {

	private static final Logger LOG = Logger.getLogger(MqttEventSource.class.getName());

	/**
	 * The bit of Paho this source needs, named separately so tests can
	 * stand in for it.
	 */
	public interface Subscriber {

		/**
		 * @param topicFilter MQTT topic filter, wildcards allowed
		 * @param onMessage   called for each message on that filter
		 * @return handle that cancels the subscription
		 * @throws Exception if subscribing fails
		 */
		AutoCloseable subscribe(String topicFilter, MessageListener onMessage) throws Exception;
	}

	/** One received MQTT message. */
	public interface MessageListener {
		void onMessage(String topic, byte[] payload);
	}

	private final Subscriber subscriber;

	private final ComponentServiceObjects<ResourceSet> rsObjects;

	private final String topicPrefix;

	public MqttEventSource(Subscriber subscriber, ComponentServiceObjects<ResourceSet> rsObjects,
			String topicPrefix) {
		this.subscriber = subscriber;
		this.rsObjects = rsObjects;
		this.topicPrefix = topicPrefix != null && !topicPrefix.isBlank() ? topicPrefix : "ddsr/events";
	}

	@Override
	public AutoCloseable open(Handler handler) {
		try {
			AutoCloseable subscription = subscriber.subscribe(topicPrefix + "/#",
					(topic, payload) -> deliver(handler, payload));
			LOG.info("[DDSR-MQTT] subscribed to " + topicPrefix + "/#");
			// The subscription being live is what "established" means here.
			// Paho reconnects on its own and re-subscribes, and each time it
			// does the consumer has to re-snapshot (FR-Sync-Reconnect) — the
			// component signals that through the same handler.
			handler.onStreamEstablished();
			return subscription;
		} catch (Exception failure) {
			// Returning null tells the SDK there is no subscription, so a
			// later attempt can try again. Throwing would surface inside
			// whatever bound this source and be swallowed there.
			LOG.warning("[DDSR-MQTT] could not subscribe to " + topicPrefix + "/#: " + failure);
			return null;
		}
	}

	private void deliver(Handler handler, byte[] payload) {
		if (payload == null || payload.length == 0) {
			return;
		}
		try {
			XmiBundle bundle = XmiCodec.readBundle(new ByteArrayInputStream(payload), rsObjects);
			for (EObject root : bundle.roots()) {
				if (root instanceof ServiceEvent event) {
					handler.onEvent(event);
					return;
				}
			}
			LOG.warning("[DDSR-MQTT] message carried no ServiceEvent, ignoring");
		} catch (Exception decodeFailure) {
			// One bad message must not end the subscription.
			LOG.warning("[DDSR-MQTT] could not decode a message, skipping: " + decodeFailure);
		}
	}

	String topicFilter() {
		return topicPrefix + "/#";
	}
}
