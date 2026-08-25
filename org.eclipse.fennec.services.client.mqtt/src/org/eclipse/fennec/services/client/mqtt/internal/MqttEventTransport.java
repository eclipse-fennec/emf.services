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

import java.util.logging.Logger;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.fennec.services.client.EventSource;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Owns the consumer-side MQTT connection and registers
 * {@link MqttEventSource} as an {@link EventSource}, which the SDK then
 * binds like any other transport.
 * <p>
 * {@code configurationPolicy = REQUIRE}: a broker URL is
 * deployment-specific, so this transport only starts where someone
 * configured it. A client with both this and the SSE source configured
 * would have two event sources — the SDK binds one; choosing
 * deliberately between them is the job of a service ranking or a target
 * filter, not of this class.
 */
@Component(
		service = EventSource.class,
		configurationPid = "org.eclipse.fennec.services.client.mqtt",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = MqttEventTransport.Config.class)
public final class MqttEventTransport implements EventSource {

	private static final Logger LOG = Logger.getLogger(MqttEventTransport.class.getName());

	@ObjectClassDefinition(name = "DDSR Client MQTT Event Transport",
			description = "Receives lifecycle events over MQTT. Requires configuration to start.")
	public @interface Config {

		@AttributeDefinition(name = "MQTT broker URL", description = "e.g. tcp://localhost:1883")
		String broker_url() default "tcp://localhost:1883";

		@AttributeDefinition(name = "Topic prefix",
				description = "Must match the broker-side prefix; the source subscribes to <prefix>/#.")
		String topic_prefix() default "ddsr/events";

		@AttributeDefinition(name = "Client id",
				description = "MQTT client identifier of this consumer.")
		String client_id() default "ddsr-consumer";

		@AttributeDefinition(name = "QoS", description = "Subscription quality of service.")
		int qos() default 0;
	}

	@Reference
	private ComponentServiceObjects<ResourceSet> rsObjects;

	private MqttAsyncClient client;

	private MqttEventSource source;

	private int qos;

	@Activate
	void activate(Config config) throws Exception {
		this.qos = config.qos();
		this.client = new MqttAsyncClient(config.broker_url(), config.client_id(), new MemoryPersistence());
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setAutomaticReconnect(true);
		client.connect(options).waitForCompletion();
		this.source = new MqttEventSource(this::subscribe, rsObjects, config.topic_prefix());
		LOG.info("[DDSR-MQTT] client transport connected to " + config.broker_url());
	}

	private AutoCloseable subscribe(String topicFilter, MqttEventSource.MessageListener onMessage) throws Exception {
		IMqttMessageListener paho = (topic, message) -> onMessage.onMessage(topic, message.getPayload());
		client.subscribe(topicFilter, qos, paho).waitForCompletion();
		return () -> {
			try {
				client.unsubscribe(topicFilter);
			} catch (Exception ignored) {
				// Disconnecting anyway, or already gone.
			}
		};
	}

	@Override
	public AutoCloseable open(Handler handler) {
		MqttEventSource current = source;
		return current != null ? current.open(handler) : null;
	}

	@Deactivate
	void deactivate() {
		source = null;
		if (client != null) {
			try {
				client.disconnectForcibly(500L, 500L);
			} catch (Exception ignored) {
				// Shutting down.
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
