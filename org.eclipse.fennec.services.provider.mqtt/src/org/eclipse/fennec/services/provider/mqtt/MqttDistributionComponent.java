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

package org.eclipse.fennec.services.provider.mqtt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.common.SecurityRequirements;
import org.eclipse.fennec.services.common.TlsContexts;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * The connection behind {@link MqttDistribution}: one Paho client per
 * broker URL, shared by everything served on it.
 *
 * <p>Pooled by URL rather than per export, because a framework
 * exporting five contracts to one broker has one relationship with that
 * broker, not five. Opened on the first export and closed with the last
 * one — a distribution with nothing to serve holds no connection, which
 * is also what keeps a deployment that never exports over MQTT from
 * dialling a broker it was merely configured with.
 *
 * <p>Where to connect is not configured here. The flavor says it: the
 * brokers a contract announces are the brokers it listens on, the same
 * way a REST export answers where its host says.
 */
@Designate(ocd = MqttDistributionComponent.Config.class)
@Component(service = MqttDistribution.class, configurationPid = MqttDistributionComponent.PID)
public class MqttDistributionComponent implements MqttDistribution {

	private static final Logger LOG = Logger.getLogger(MqttDistributionComponent.class.getName());

	/** Optional: only a node that shows an MQTT broker a certificate needs it. */
	public static final String PID = "org.eclipse.fennec.services.provider.mqtt.connection";

	@ObjectClassDefinition(name = "Fennec Services MQTT Distribution Connection",
			description = "How the MQTT distribution connects to the brokers its flavors announce.")
	public @interface Config {

		@AttributeDefinition(name = "TLS key store", required = false,
				description = "The certificate and key this node shows MQTT brokers announced with ssl:// "
						+ "(PKCS12). Its CN is the provider's name there — the user an ACL lets read requests "
						+ "and write replies.")
		String tls_keystore() default "";

		@AttributeDefinition(name = "TLS key store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the key store and of its key.")
		String tls_keystore_password() default "";

		@AttributeDefinition(name = "TLS trust store", required = false,
				description = "The authorities whose MQTT broker certificates this node believes (PKCS12). "
						+ "Empty: the JVM's.")
		String tls_truststore() default "";

		@AttributeDefinition(name = "TLS trust store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the trust store.")
		String tls_truststore_password() default "";
	}

	/** The certificate this node shows on ssl:// connections, if it has one. */
	private volatile SSLContext tls;

	@Activate
	void activate(Config config) {
		this.tls = TlsContexts.of(config.tls_keystore(), config.tls_keystore_password(), "PKCS12",
				config.tls_truststore(), config.tls_truststore_password(), "PKCS12");
	}

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	/**
	 * Whoever is watching calls, if anyone is (#126). Read per call
	 * through {@link CallTracer#deferred}, because an export outlives
	 * the telemetry bundle in both directions.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile CallTracer tracer;

	/** One connection per broker, and how many exports hold it. */
	private final Map<String, Connection> connections = new LinkedHashMap<>();

	@Override
	public Served serve(MqttFlavor flavor, Object service, String name) {
		return serve(flavor, () -> service, name);
	}

	@Override
	public Served serve(MqttFlavor flavor, Supplier<Object> service, String name) {
		if (flavor == null || service == null) {
			throw new IllegalArgumentException("a flavor and the service behind it are both needed");
		}
		if (flavor.getBrokers().isEmpty()) {
			throw new IllegalArgumentException("an MQTT export needs a broker to listen on, and "
					+ flavor.getName() + " announces none");
		}
		// Over MQTT a call carries no credential of its own (Paho v3 has no
		// user properties); identity is the messaging broker's business, on
		// the connection (SECURITY.md §2, ConnectionScheme). A contract that
		// demands a caller is therefore served only under a flavor that
		// says who checks — never open by omission.
		if (flavor.getSecuritySchemes().isEmpty() && flavor.getOperationFlavors().stream()
				.anyMatch(op -> SecurityRequirements.demandsAuthentication(op.getOperation()))) {
			throw new IllegalStateException((name == null ? flavor.getName() : name)
					+ " demands authentication, and an MQTT flavor without a scheme cannot check it — not served");
		}
		String url = flavor.getBrokers().get(0);
		Connection connection = connect(url);
		MqttOperationDispatcher dispatcher = new MqttOperationDispatcher(flavor, service,
				"/provider/" + (name == null ? flavor.getName() : name), resourceSets,
				connection::publish, CallTracer.deferred(() -> tracer));

		List<String> subscribed = new ArrayList<>();
		try {
			for (String topic : dispatcher.requestTopics()) {
				connection.subscribe(topic, dispatcher.qosFor(topic),
						(received, message) -> dispatcher.onMessage(received, message.getPayload()));
				subscribed.add(topic);
			}
		} catch (Exception failed) {
			for (String topic : subscribed) {
				connection.unsubscribeQuietly(topic);
			}
			release(url);
			throw new IllegalStateException("could not listen for " + name + " on " + url, failed);
		}
		LOG.info("[DDSR-MQTT] serving " + name + " on " + url + " — " + subscribed);
		return new MqttServed(url, subscribed, connection);
	}

	@Deactivate
	void deactivate() {
		synchronized (connections) {
			for (Connection connection : connections.values()) {
				connection.close();
			}
			connections.clear();
		}
	}

	private Connection connect(String url) {
		synchronized (connections) {
			Connection connection = connections.get(url);
			if (connection == null) {
				connection = new Connection(url, tls);
				connections.put(url, connection);
			}
			connection.holders++;
			return connection;
		}
	}

	private void release(String url) {
		synchronized (connections) {
			Connection connection = connections.get(url);
			if (connection != null && --connection.holders <= 0) {
				connections.remove(url);
				connection.close();
			}
		}
	}

	/** One Paho client, and the exports that hold it open. */
	private static final class Connection {

		private final MqttAsyncClient client;

		private int holders;

		Connection(String url, SSLContext tls) {
			try {
				client = new MqttAsyncClient(url, "ddsr-provider-" + UUID.randomUUID().toString().substring(0, 8),
						new MemoryPersistence());
				MqttConnectOptions options = new MqttConnectOptions();
				options.setCleanSession(true);
				options.setAutomaticReconnect(true);
				if (tls != null && url.startsWith("ssl://")) {
					options.setSocketFactory(tls.getSocketFactory());
				}
				client.connect(options).waitForCompletion();
			} catch (Exception unreachable) {
				throw new IllegalStateException("cannot reach the MQTT broker at " + url, unreachable);
			}
		}

		void subscribe(String topic, int qos, IMqttMessageListener listener) throws Exception {
			client.subscribe(topic, qos, listener).waitForCompletion();
		}

		void publish(String topic, byte[] payload, int qos) throws Exception {
			MqttMessage message = new MqttMessage(payload);
			message.setQos(qos);
			message.setRetained(false);
			client.publish(topic, message).waitForCompletion();
		}

		void unsubscribeQuietly(String topic) {
			try {
				client.unsubscribe(topic);
			} catch (Exception alreadyGone) {
				// Going down anyway, or never there.
			}
		}

		void close() {
			try {
				client.disconnectForcibly(500L, 500L);
			} catch (Exception shuttingDown) {
				// Nothing left to do about it.
			}
			try {
				client.close();
			} catch (Exception shuttingDown) {
				// Same.
			}
		}
	}

	private final class MqttServed implements Served {

		private final String url;

		private final List<String> topics;

		private final Connection connection;

		private final AtomicBoolean open = new AtomicBoolean(true);

		MqttServed(String url, List<String> topics, Connection connection) {
			this.url = url;
			this.topics = List.copyOf(topics);
			this.connection = connection;
		}

		@Override
		public List<String> topics() {
			return topics;
		}

		@Override
		public void close() {
			if (open.compareAndSet(true, false)) {
				for (String topic : topics) {
					connection.unsubscribeQuietly(topic);
				}
				release(url);
			}
		}
	}
}
