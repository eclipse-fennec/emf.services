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

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.common.SecurityRequirements;
import org.eclipse.fennec.services.messaging.LinkSettings;
import org.eclipse.fennec.services.messaging.MessagingLink;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.osgi.framework.BundleContext;
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
 * The connection behind {@link MqttDistribution}: one messaging link per
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

	/**
	 * How long an answer may take to reach the broker. Bounded, because
	 * the thread that waits is one of the few that deliver calls.
	 */
	private static final long PUBLISH_TIMEOUT_MILLIS = 30_000;

	private volatile BundleContext context;

	/** Carries the certificate this node shows on ssl:// connections, if it has one. */
	private volatile Config config;

	@Activate
	void activate(BundleContext context, Config config) {
		this.context = context;
		this.config = config;
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
		MessagingLink link = connection.link;
		MqttOperationDispatcher dispatcher = new MqttOperationDispatcher(flavor, service,
				"/provider/" + (name == null ? flavor.getName() : name), resourceSets,
				(topic, payload, qos) -> link.publish(topic, payload, qos, PUBLISH_TIMEOUT_MILLIS),
				CallTracer.deferred(() -> tracer));

		List<String> topics = new ArrayList<>();
		List<AutoCloseable> subscriptions = new ArrayList<>();
		try {
			// On a connected link each of these is in place when it
			// returns. On one still coming up it is a wish the link makes
			// true with its first connection — and restores after every
			// later one, which is what keeps an export answering across a
			// broker restart.
			for (String topic : dispatcher.requestTopics()) {
				subscriptions.add(link.subscribe(topic, dispatcher.qosFor(topic), dispatcher::onMessage));
				topics.add(topic);
			}
		} catch (RuntimeException failed) {
			closeQuietly(subscriptions);
			release(url);
			throw new IllegalStateException("could not listen for " + name + " on " + url, failed);
		}
		LOG.info("[DDSR-MQTT] serving " + name + " on " + url + " — " + topics);
		return new MqttServed(url, topics, subscriptions, link);
	}

	@Override
	public AutoCloseable hold(String brokerUrl, Runnable whenConnected) {
		if (brokerUrl == null || brokerUrl.isBlank()) {
			throw new IllegalArgumentException("a connection needs a broker to go to");
		}
		MessagingLink link = connect(brokerUrl).link;
		AtomicBoolean told = new AtomicBoolean();
		AutoCloseable connects = link.onConnected(again -> {
			if (told.compareAndSet(false, true)) {
				whenConnected.run();
			}
		});
		AtomicBoolean held = new AtomicBoolean(true);
		return () -> {
			if (held.compareAndSet(true, false)) {
				closeQuietly(List.of(connects));
				release(brokerUrl);
			}
		};
	}

	private static void closeQuietly(List<AutoCloseable> handles) {
		for (AutoCloseable handle : handles) {
			try {
				handle.close();
			} catch (Exception alreadyGone) {
				// Going down anyway, or never there.
			}
		}
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
				String id = UUID.randomUUID().toString().substring(0, 8);
				Config tls = config;
				try {
					connection = new Connection(MessagingLink.open(context,
							new LinkSettings("fennec.services.provider." + id, url, "ddsr-provider-" + id,
									tls.tls_keystore(), tls.tls_keystore_password(), tls.tls_truststore(),
									tls.tls_truststore_password())));
				} catch (RuntimeException unreachable) {
					throw new IllegalStateException("cannot connect to the MQTT broker at " + url,
							unreachable);
				}
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

	/** One link, and the exports that hold it open. */
	private static final class Connection {

		private final MessagingLink link;

		private int holders;

		Connection(MessagingLink link) {
			this.link = link;
		}

		void close() {
			link.close();
		}
	}

	private final class MqttServed implements Served {

		private final String url;

		private final List<String> topics;

		private final List<AutoCloseable> subscriptions;

		private final MessagingLink link;

		private final List<AutoCloseable> listeners = new ArrayList<>();

		private final AtomicBoolean open = new AtomicBoolean(true);

		MqttServed(String url, List<String> topics, List<AutoCloseable> subscriptions, MessagingLink link) {
			this.url = url;
			this.topics = List.copyOf(topics);
			this.subscriptions = List.copyOf(subscriptions);
			this.link = link;
		}

		@Override
		public List<String> topics() {
			return topics;
		}

		@Override
		public void whenListening(Runnable listening) {
			// The link restores its subscriptions before it says it is
			// connected, and makes them at once when it already is: told
			// here means subscribed there.
			AtomicBoolean told = new AtomicBoolean();
			AutoCloseable connects = link.onConnected(again -> {
				if (open.get() && told.compareAndSet(false, true)) {
					listening.run();
				}
			});
			synchronized (listeners) {
				listeners.add(connects);
			}
		}

		@Override
		public void close() {
			if (open.compareAndSet(true, false)) {
				synchronized (listeners) {
					closeQuietly(listeners);
					listeners.clear();
				}
				closeQuietly(subscriptions);
				release(url);
			}
		}
	}
}
