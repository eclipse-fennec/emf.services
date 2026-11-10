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

package org.eclipse.fennec.services.provider.amqp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.AmqpFlavor;
import org.eclipse.fennec.services.common.SecurityRequirements;
import org.eclipse.fennec.services.flavor.amqp.AmqpFlavors;
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
 * The connection behind {@link AmqpDistribution}: one messaging link
 * per broker and exchange, shared by everything served there.
 *
 * <p>Where to connect is not configured here. The flavor says it. What
 * is configured here is who this node is to the broker — a name and a
 * password, or a certificate — because that is the one thing a flavor
 * must not say: it is announced to everyone.
 */
@Designate(ocd = AmqpDistributionComponent.Config.class)
@Component(service = AmqpDistribution.class, configurationPid = AmqpDistributionComponent.PID)
@RequireMessaging(protocol = "amqp")
public class AmqpDistributionComponent implements AmqpDistribution {

	private static final Logger LOG = Logger.getLogger(AmqpDistributionComponent.class.getName());

	/** Optional: only a node the broker asks for credentials needs it. */
	public static final String PID = "org.eclipse.fennec.services.provider.amqp.connection";

	@ObjectClassDefinition(name = "Fennec Services AMQP Distribution Connection",
			description = "Who this node is to the AMQP brokers its flavors announce.")
	public @interface Config {

		@AttributeDefinition(name = "User name", required = false,
				description = "The user this node connects as. Empty: the broker's default.")
		String username() default "";

		@AttributeDefinition(name = "Password", type = AttributeType.PASSWORD, required = false,
				description = "The user's password.")
		String password() default "";

		@AttributeDefinition(name = "TLS key store", required = false,
				description = "The certificate and key this node shows brokers announced with amqps:// "
						+ "(PKCS12).")
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
	}

	private volatile BundleContext context;

	private volatile Config config;

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	/** Whoever is watching calls, if anyone is (#126). */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile CallTracer tracer;

	/** One connection per broker and exchange, and how many exports hold it. */
	private final Map<String, Connection> connections = new LinkedHashMap<>();

	@Activate
	void activate(BundleContext context, Config config) {
		this.context = context;
		this.config = config;
	}

	@Override
	public Served serve(AmqpFlavor flavor, Supplier<Object> service, String name) {
		if (flavor == null || service == null) {
			throw new IllegalArgumentException("a flavor and the service behind it are both needed");
		}
		if (flavor.getBrokers().isEmpty()) {
			throw new IllegalArgumentException("an AMQP export needs a broker to listen on, and "
					+ flavor.getName() + " announces none");
		}
		// A call carries no credential of its own here either; identity is
		// the messaging broker's business, on the connection (SECURITY.md
		// §2). A contract that demands a caller is served only under a
		// flavor that says who checks — never open by omission.
		if (flavor.getSecuritySchemes().isEmpty() && flavor.getOperationFlavors().stream()
				.anyMatch(op -> SecurityRequirements.demandsAuthentication(op.getOperation()))) {
			throw new IllegalStateException((name == null ? flavor.getName() : name)
					+ " demands authentication, and an AMQP flavor without a scheme cannot check it — not served");
		}
		String url = flavor.getBrokers().get(0);
		String exchange = AmqpFlavors.exchange(flavor);
		Connection connection = connect(url, exchange);
		MessagingLink link = connection.link;
		AmqpOperationDispatcher dispatcher = new AmqpOperationDispatcher(flavor, service,
				"/provider/" + (name == null ? flavor.getName() : name), resourceSets,
				CallTracer.deferred(() -> tracer));

		List<String> keys = new ArrayList<>();
		List<AutoCloseable> bindings = new ArrayList<>();
		try {
			for (String key : dispatcher.routingKeys()) {
				bindings.add(link.serve(key, AmqpFlavors.QOS,
						(received, payload, reply) -> dispatcher.onRequest(received, payload, reply::send)));
				keys.add(key);
			}
		} catch (RuntimeException failed) {
			closeQuietly(bindings);
			release(connection);
			throw new IllegalStateException("could not listen for " + name + " on " + link.url(), failed);
		}
		LOG.info("[DDSR-AMQP] serving " + name + " on " + link.url() + " (" + exchange + ") — " + keys);
		return new AmqpServed(keys, bindings, connection);
	}

	@Override
	public AutoCloseable hold(String brokerUrl, String exchange, Runnable whenConnected) {
		if (brokerUrl == null || brokerUrl.isBlank()) {
			throw new IllegalArgumentException("a connection needs a broker to go to");
		}
		Connection connection = connect(brokerUrl,
				exchange == null || exchange.isBlank() ? AmqpFlavors.DEFAULT_EXCHANGE : exchange);
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
		String key = url + " " + exchange;
		synchronized (connections) {
			Connection connection = connections.get(key);
			if (connection == null) {
				String id = UUID.randomUUID().toString().substring(0, 8);
				Config who = config;
				try {
					connection = new Connection(key, MessagingLink.open(context,
							new LinkSettings("fennec.services.amqp.provider." + id, url, "ddsr-provider-" + id,
									who.tls_keystore(), who.tls_keystore_password(), who.tls_truststore(),
									who.tls_truststore_password())
									.onExchange(exchange).as(who.username(), who.password())));
				} catch (RuntimeException unreachable) {
					throw new IllegalStateException("cannot connect to the AMQP broker at " + url,
							unreachable);
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

	private static void closeQuietly(List<AutoCloseable> handles) {
		for (AutoCloseable handle : handles) {
			try {
				handle.close();
			} catch (Exception alreadyGone) {
				// Going down anyway, or never there.
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

	private final class AmqpServed implements Served {

		private final List<String> keys;

		private final List<AutoCloseable> bindings;

		private final Connection connection;

		private final List<AutoCloseable> listeners = new ArrayList<>();

		private final AtomicBoolean open = new AtomicBoolean(true);

		AmqpServed(List<String> keys, List<AutoCloseable> bindings, Connection connection) {
			this.keys = List.copyOf(keys);
			this.bindings = List.copyOf(bindings);
			this.connection = connection;
		}

		@Override
		public List<String> routingKeys() {
			return keys;
		}

		@Override
		public void whenListening(Runnable listening) {
			// The link restores its subscriptions before it says it is
			// connected, and makes them at once when it already is: told
			// here means bound there.
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
				closeQuietly(bindings);
				release(connection);
			}
		}
	}
}
