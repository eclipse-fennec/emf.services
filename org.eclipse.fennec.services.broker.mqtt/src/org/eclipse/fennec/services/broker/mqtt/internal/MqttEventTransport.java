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

import java.util.logging.Logger;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.ServiceEvent;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Owns the MQTT connection and registers {@link MqttEventSink} as an
 * {@link EventSink}, which the broker's whiteboard then picks up.
 * <p>
 * Registered with {@code configurationPolicy = REQUIRE} deliberately:
 * an MQTT broker URL is deployment-specific, so this transport only
 * starts where someone configured it. Nothing else has to know it
 * exists.
 */
@Component(
		service = EventSink.class,
		configurationPid = "org.eclipse.fennec.services.broker.mqtt",
		configurationPolicy = org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE)
@Designate(ocd = MqttEventTransport.Config.class)
public final class MqttEventTransport implements EventSink {

	private static final Logger LOG = Logger.getLogger(MqttEventTransport.class.getName());

	@ObjectClassDefinition(name = "DDSR Broker MQTT Event Transport",
			description = "Publishes lifecycle events to MQTT. Requires configuration to start.")
	public @interface Config {

		@AttributeDefinition(name = "MQTT broker URL", description = "e.g. tcp://localhost:1883")
		String broker_url() default "tcp://localhost:1883";

		@AttributeDefinition(name = "Topic prefix",
				description = "Events are published to <prefix>/<interface>.")
		String topic_prefix() default "ddsr/events";

		@AttributeDefinition(name = "Client id",
				description = "MQTT client identifier of the broker-side publisher.")
		String client_id() default "ddsr-broker";

		@AttributeDefinition(name = "QoS",
				description = "0 fits a stream whose consumers re-snapshot on reconnect anyway.")
		int qos() default 0;
	}

	@Reference
	private BrokerLookup broker;

	@Reference
	private ComponentServiceObjects<ResourceSet> rsObjects;

	private MqttAsyncClient client;

	private MqttEventSink sink;

	private int qos;

	@Activate
	void activate(Config config) throws Exception {
		this.qos = config.qos();
		this.client = new MqttAsyncClient(config.broker_url(), config.client_id(), new MemoryPersistence());
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setAutomaticReconnect(true);
		client.connect(options).waitForCompletion();
		this.sink = new MqttEventSink(this::send, broker, rsObjects, config.topic_prefix());
		LOG.info("[DDSR-MQTT] event transport connected to " + config.broker_url()
				+ ", topic prefix " + config.topic_prefix());
	}

	private void send(String topic, byte[] payload) throws Exception {
		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		// Not retained: an event describes a transition, not a state. A
		// late subscriber must not be told about a registration that has
		// long been withdrawn — it pulls a snapshot instead
		// (FR-Sync-Reconnect).
		message.setRetained(false);
		client.publish(topic, message);
	}

	@Override
	public void publish(ServiceEvent event) {
		MqttEventSink current = sink;
		if (current != null) {
			current.publish(event);
		}
	}

	@Deactivate
	void deactivate() {
		sink = null;
		if (client != null) {
			try {
				client.disconnectForcibly(500L, 500L);
			} catch (Exception ignored) {
				// Shutting down; nothing useful left to do.
			}
			try {
				client.close();
			} catch (Exception ignored) {
				// Same.
			}
			client = null;
		}
	}
}
