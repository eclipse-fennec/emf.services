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

package org.eclipse.fennec.services.provider.messaging;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.common.SecurityRequirements;
import org.eclipse.fennec.services.flavor.messaging.MessagingRoute;
import org.eclipse.fennec.services.messaging.LinkSettings;
import org.eclipse.fennec.services.messaging.MessagingLink;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.gecko.messaging.api.annotations.RequireMessaging;
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
 * The connection behind {@link MessagingDistribution}: one messaging
 * link per broker (and exchange), shared by everything served there.
 *
 * <p>Pooled rather than per export, because a framework exporting five
 * contracts to one broker has one relationship with that broker, not
 * five. Opened on the first export and closed with the last one — a
 * distribution with nothing to serve holds no connection, which is also
 * what keeps a deployment that never exports over messaging from
 * dialling a broker it was merely configured with.
 *
 * <p>Where to connect is not configured here. The flavor says it, and
 * the scheme of its broker URL says which protocol is spoken there.
 * What is configured here is who this node is to a broker — a
 * certificate, or a name and a password — because that is the one thing
 * a flavor must not say: it is announced to everyone.
 */
@Designate(ocd = MessagingDistributionComponent.Config.class)
@Component(service = MessagingDistribution.class, configurationPid = MessagingDistributionComponent.PID)
@RequireMessaging
public class MessagingDistributionComponent implements MessagingDistribution {

	private static final Logger LOG = Logger.getLogger(MessagingDistributionComponent.class.getName());

	/** Optional: only a node that shows a broker who it is needs it. */
	public static final String PID = "org.eclipse.fennec.services.provider.messaging.connection";

	@ObjectClassDefinition(name = "Fennec Services Messaging Distribution Connection",
			description = "Who this node is to the messaging brokers its flavors announce (MQTT, AMQP).")
	public @interface Config {

		@AttributeDefinition(name = "MQTT version", required = false,
				description = "Which MQTT this connection speaks to an MQTT broker: 3 (MQTT 3.1.1, the "
						+ "default) or 5. A broker carries messages between the two, so nodes need not "
						+ "agree; 5 adds the broker's flow control (receiveMaximum) on this connection. "
						+ "Needs the MQTT 5 binding in the launch. Means nothing with an amqp:// URL.")
		int mqtt_version() default 3;

		@AttributeDefinition(name = "User name", required = false,
				description = "The user this node connects to brokers as. Empty: the broker's default. "
						+ "Stated here and never in a broker URL — the URL is what a flavor announces.")
		String username() default "";

		@AttributeDefinition(name = "Password", type = AttributeType.PASSWORD, required = false,
				description = "The user's password.")
		String password() default "";

		@AttributeDefinition(name = "TLS key store", required = false,
				description = "The certificate and key this node shows brokers announced with a TLS URL "
						+ "(ssl://, amqps://; PKCS12). Over MQTT its CN is the provider's name there — "
						+ "the user an ACL lets read requests and write replies.")
		String tls_keystore() default "";

		@AttributeDefinition(name = "TLS key store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the key store and of its key.")
		String tls_keystore_password() default "";

		@AttributeDefinition(name = "TLS trust store", required = false,
				description = "The authorities whose broker certificates this node believes (PKCS12). "
						+ "Empty: the JVM's.")
		String tls_truststore() default "";

		@AttributeDefinition(name = "TLS trust store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the trust store.")
		String tls_truststore_password() default "";

		@AttributeDefinition(name = "Largest call", required = false,
				description = "How many bytes the document of a call may have. Zero leaves the default of "
						+ "one megabyte per message; a larger value also raises what this node's "
						+ "connection accepts and sends. The messaging broker has a limit of its own.")
		long max_body_bytes() default 0;
	}

	private volatile BundleContext context;

	/** Who this node is to a broker. */
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
	private final AtomicReference<CallTracer> tracer = new AtomicReference<>();

	@Reference(name = "tracer", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	void setTracer(CallTracer tracer) {
		this.tracer.set(tracer);
	}

	void unsetTracer(CallTracer tracer) {
		// Only the one that is bound: a replacement is set before the one
		// it replaces is unset.
		this.tracer.compareAndSet(tracer, null);
	}

	/** One connection per broker and exchange, and how many exports hold it. */
	private final Map<String, Connection> connections = new LinkedHashMap<>();

	@Override
	public Served serve(ServiceFlavor flavor, Object service, String name) {
		return serve(flavor, () -> service, name);
	}

	@Override
	public Served serve(ServiceFlavor flavor, Supplier<Object> service, String name) {
		if (flavor == null || service == null) {
			throw new IllegalArgumentException("a flavor and the service behind it are both needed");
		}
		MessagingRoute route = MessagingRoute.of(flavor);
		if (route.broker() == null) {
			throw new IllegalArgumentException("an export over " + route.protocol()
					+ " needs a broker to listen on, and " + flavor.getName() + " announces none");
		}
		// Over messaging a call carries no credential of its own; identity
		// is the messaging broker's business, on the connection
		// (SECURITY.md §2, ConnectionScheme). A contract that demands a
		// caller is therefore served only under a flavor that says who
		// checks — never open by omission.
		if (flavor.getSecuritySchemes().isEmpty() && flavor.getOperationFlavors().stream()
				.anyMatch(op -> SecurityRequirements.demandsAuthentication(op.getOperation()))) {
			throw new IllegalStateException((name == null ? flavor.getName() : name)
					+ " demands authentication, and a " + route.protocol()
					+ " flavor without a scheme cannot check it — not served");
		}
		Connection connection = connect(route.broker(), route.exchange());
		MessagingLink link = connection.link;
		MessagingOperationDispatcher dispatcher = new MessagingOperationDispatcher(flavor, service,
				"/provider/" + (name == null ? flavor.getName() : name), resourceSets,
				CallTracer.deferred(tracer::get), config.max_body_bytes());

		List<String> channels = new ArrayList<>();
		List<AutoCloseable> subscriptions = new ArrayList<>();
		try {
			// On a connected link each of these is in place when it
			// returns. On one still coming up it is a wish the link makes
			// true with its first connection — and restores after every
			// later one, which is what keeps an export answering across a
			// broker restart.
			for (String channel : dispatcher.channels()) {
				// The same on every protocol: the connection knows where
				// the answer goes, because it read the request.
				// An operation that answers with a stream is served in the
				// messaging library's sessions, every other one per request.
				subscriptions.add(dispatcher.streams(channel)
						? link.serveSessions(channel, dispatcher.qosFor(channel), dispatcher::onSession)
						: link.serve(channel, dispatcher.qosFor(channel),
								(received, call, reply) -> dispatcher.onRequest(received, call, reply::send)));
				channels.add(channel);
			}
		} catch (RuntimeException failed) {
			closeQuietly(subscriptions);
			release(connection);
			throw new IllegalStateException("could not listen for " + name + " on " + link.url(), failed);
		}
		LOG.info("[DDSR-MSG] serving " + name + " over " + route.protocol() + " on " + link.url() + " — "
				+ channels);
		return new MessagingServed(channels, subscriptions, connection);
	}

	@Override
	public AutoCloseable hold(String brokerUrl, String exchange, Runnable whenConnected) {
		if (brokerUrl == null || brokerUrl.isBlank()) {
			throw new IllegalArgumentException("a connection needs a broker to go to");
		}
		Connection connection = connect(brokerUrl, exchange);
		AtomicBoolean told = new AtomicBoolean();
		AutoCloseable connects = connection.link.onConnected(again -> {
			if (told.compareAndSet(false, true)) {
				whenConnected.run();
			}
		});
		AtomicBoolean held = new AtomicBoolean(true);
		return () -> {
			if (held.compareAndSet(true, false)) {
				closeQuietly(List.of(connects));
				release(connection);
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
				connection.link.close();
			}
			connections.clear();
		}
	}

	private Connection connect(String url, String exchange) {
		String key = url + " " + (exchange == null || exchange.isBlank() ? "" : exchange);
		synchronized (connections) {
			Connection connection = connections.get(key);
			if (connection == null) {
				String id = UUID.randomUUID().toString().substring(0, 8);
				Config who = config;
				try {
					connection = new Connection(key, MessagingLink.open(context,
							new LinkSettings("fennec.services.provider." + id, url, "ddsr-provider-" + id,
									who.tls_keystore(), who.tls_keystore_password(), who.tls_truststore(),
									who.tls_truststore_password())
									.onExchange(exchange).as(who.username(), who.password())
									.carrying(who.max_body_bytes()).speaking(who.mqtt_version())));
				} catch (RuntimeException unreachable) {
					throw new IllegalStateException("cannot connect to the broker at " + url, unreachable);
				}
				connections.put(key, connection);
			}
			connection.holders++;
			return connection;
		}
	}

	private void release(Connection connection) {
		synchronized (connections) {
			if (--connection.holders <= 0 && connections.remove(connection.key, connection)) {
				connection.link.close();
			}
		}
	}

	/** One link, and the exports that hold it open. */
	private static final class Connection {

		private final String key;

		private final MessagingLink link;

		private int holders;

		Connection(String key, MessagingLink link) {
			this.key = key;
			this.link = link;
		}
	}

	private final class MessagingServed implements Served {

		private final List<String> channels;

		private final List<AutoCloseable> subscriptions;

		private final Connection connection;

		private final List<AutoCloseable> listeners = new ArrayList<>();

		private final AtomicBoolean open = new AtomicBoolean(true);

		MessagingServed(List<String> channels, List<AutoCloseable> subscriptions, Connection connection) {
			this.channels = List.copyOf(channels);
			this.subscriptions = List.copyOf(subscriptions);
			this.connection = connection;
		}

		@Override
		public List<String> channels() {
			return channels;
		}

		@Override
		public void whenListening(Runnable listening) {
			// The link restores its subscriptions before it says it is
			// connected, and makes them at once when it already is: told
			// here means subscribed there.
			AtomicBoolean told = new AtomicBoolean();
			AutoCloseable connects = connection.link.onConnected(again -> {
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
				release(connection);
			}
		}
	}
}
