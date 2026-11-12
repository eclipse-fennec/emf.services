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

package org.eclipse.fennec.services.rsa.distribution.messaging;

import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.fennec.services.MessagingFlavor;
import org.eclipse.fennec.services.MessagingOperationFlavor;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.provider.messaging.MessagingDistribution;
import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.eclipse.fennec.services.rsa.spi.FlavorDistribution;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Distribution over the AMQP flavor: what makes an exported service
 * reachable on routing keys.
 *
 * <p>The third implementation of {@link FlavorDistribution}, and the
 * same three steps as the other two: the contract is derived from the
 * exported interface, a flavor is built saying which routing keys
 * answer for it, and the messaging distribution binds them and invokes
 * the service. The RSA core did not change for it — and neither did
 * the distribution: it is the one the MQTT provider hands its flavor
 * to. What this class adds is what an AMQP flavor says.
 *
 * <p>It does not announce anything. Telling others is discovery's job,
 * and a node that serves over AMQP announces over REST like any other.
 */
@Designate(ocd = AmqpDistributionProvider.Config.class)
// Registers itself rather than letting DS do it, and only once the
// broker it was configured with is connected — for the reason the MQTT
// provider gives: an admin whose configuration points here exports as
// soon as this service is there, so being there has to mean that an
// export made now is listening when it returns.
@Component(service = {},
		configurationPid = "org.eclipse.fennec.services.rsa.distribution.amqp",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AmqpDistributionProvider implements FlavorDistribution {

	/** The RSA configuration type a service asks for to be exported this way. */
	public static final String CONFIG_TYPE = "fennec.amqp";

	/**
	 * The configuration-type properties on the endpoint description:
	 * where a consumer without our model publishes calls.
	 */
	public static final String ROUTING_KEY_PROPERTY = CONFIG_TYPE + ".routingKey";

	/** The broker the calls arrive on. */
	public static final String BROKER_PROPERTY = CONFIG_TYPE + ".broker";

	/** The exchange they are published to. */
	public static final String EXCHANGE_PROPERTY = CONFIG_TYPE + ".exchange";

	/** What an admin's {@code distribution.target} selects this provider by. */
	static final String FLAVOR_PROPERTY = "ddsr.rsa.flavor";

	private static final Logger LOG = Logger.getLogger(AmqpDistributionProvider.class.getName());

	@ObjectClassDefinition(name = "Fennec Services RSA AMQP Distribution",
			description = "Which broker services exported over AMQP listen on, and under which routing keys.")
	public @interface Config {

		@AttributeDefinition(name = "Broker URL",
				description = "The AMQP broker this deployment serves on, e.g. amqp://localhost:5672 or "
						+ "amqps://…, with the virtual host as its path. It is announced with every export, "
						+ "because a consumer dials what the flavor says — so it carries no user and no "
						+ "password; those belong to the connection (…provider.messaging.connection).")
		String broker_url();

		@AttributeDefinition(name = "Exchange",
				description = "The topic exchange calls are published to. The default exists on every "
						+ "AMQP 0-9-1 broker.")
		String exchange() default "amq.topic";

		@AttributeDefinition(name = "Routing key prefix",
				description = "Calls arrive under <prefix>.<provider>.<contract>.<operation>.")
		String routing_prefix() default "ddsr.rpc";

		@AttributeDefinition(name = "Content type", required = false,
				description = "What calls and answers of exported services are encoded in, e.g. "
						+ "application/json or application/x-protobuf. An exported interface has no contract "
						+ "document to say it, so the deployment may; empty is XMI. The launch needs a resource "
						+ "factory registered for that content type — emf.codec for JSON — and so does every "
						+ "consumer, which reads the encoding off the announced flavor.")
		String content_type() default "";
	}

	@Reference
	private MessagingDistribution distribution;

	private volatile Config config;

	/** This framework's short name in identities nobody named themselves. */
	private String frameworkTag;

	/** The connection to the configured broker, held from activation on. */
	private AutoCloseable connection;

	private ServiceRegistration<FlavorDistribution> registration;

	private boolean active;

	@Activate
	void activate(BundleContext context, Config config) {
		this.config = config;
		String uuid = context.getProperty(Constants.FRAMEWORK_UUID);
		this.frameworkTag = uuid == null || uuid.length() < 8 ? "local" : uuid.substring(0, 8);
		synchronized (this) {
			active = true;
		}
		String broker = config.broker_url();
		this.connection = distribution.hold(broker, config.exchange(), () -> offer(context, broker));
	}

	private synchronized void offer(BundleContext context, String broker) {
		if (!active || registration != null) {
			return;
		}
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(FLAVOR_PROPERTY, CONFIG_TYPE);
		registration = context.registerService(FlavorDistribution.class, this, properties);
		LOG.info("[DDSR] AMQP distribution ready — exports answer on " + broker + " (" + exchange()
				+ ") under " + routingPrefix());
	}

	/**
	 * A changed configuration is taken, not died of (#107). What is
	 * already served keeps its routing keys; a new prefix applies to
	 * what is exported from now on.
	 */
	@Modified
	void modified(Config config) {
		this.config = config;
	}

	@Deactivate
	void deactivate() {
		ServiceRegistration<FlavorDistribution> offered;
		synchronized (this) {
			active = false;
			offered = registration;
			registration = null;
		}
		if (offered != null) {
			try {
				offered.unregister();
			} catch (IllegalStateException alreadyGone) {
				// The framework got there first.
			}
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (Exception alreadyGone) {
				// Going down anyway.
			}
			connection = null;
		}
	}

	/** A messaging broker delivers many answers to one call. */
	@Override
	public boolean carriesStreams() {
		return true;
	}

	@Override
	public String[] supportedConfigs() {
		return new String[] { CONFIG_TYPE };
	}

	@Override
	public FlavorKind flavor() {
		return FlavorKind.AMQP;
	}

	@Override
	public ExportedEndpoint export(Object service, ServiceInterface contract, Map<String, ?> properties) {
		if (contract == null) {
			throw new IllegalArgumentException("an AMQP export needs the contract to serve");
		}
		String provider = providerNameFor(properties);
		// Under the provider, not the contract alone: two services
		// exporting the same interface in one framework are two
		// endpoints, and two bindings of one key would be both of them
		// answering every call.
		String routingKey = routingPrefix() + "." + segment(provider) + "."
				+ segment(contract.getName().toLowerCase(Locale.ROOT));
		MessagingFlavor flavor = flavorFor(contract, routingKey, config.broker_url(), exchange(),
				config.content_type());

		MessagingDistribution.Served served = distribution.serve(flavor, service,
				provider + ":" + contract.getName());

		ServiceImplementation implementation = implementationOf(contract, flavor, provider);
		LOG.info("[DDSR] exported " + contract.getName() + " over AMQP on " + config.broker_url()
				+ " under " + routingKey
				+ (config.content_type().isBlank() ? "" : ", encoded as " + config.content_type().strip()));
		return new AmqpEndpoint(contract, implementation, served, routingKey);
	}

	/** A flavor whose routing keys say where each operation answers. */
	private static MessagingFlavor flavorFor(ServiceInterface contract, String routingKey, String broker,
			String exchange, String contentType) {
		MessagingFlavor flavor = ServicesFactory.eINSTANCE.createMessagingFlavor();
		flavor.setName(contract.getName() + "-amqp");
		flavor.setKind(FlavorKind.AMQP);
		flavor.getBrokers().add(broker);
		flavor.setExchange(exchange);
		flavor.setChannel(routingKey);
		for (ServiceOperation operation : contract.getOperations()) {
			MessagingOperationFlavor operationFlavor = ServicesFactory.eINSTANCE.createMessagingOperationFlavor();
			operationFlavor.setName(operation.getName());
			operationFlavor.setOperation(operation);
			// The encoding only where the deployment named one; else the
			// flavor's default (XMI).
			if (contentType != null && !contentType.isBlank()) {
				operationFlavor.getConsumes().add(contentType.strip());
				operationFlavor.getProduces().add(contentType.strip());
			}
			flavor.getOperationFlavors().add(operationFlavor);
		}
		return flavor;
	}

	private String exchange() {
		String exchange = config.exchange();
		return exchange == null || exchange.isBlank() ? "amq.topic" : exchange;
	}

	private String routingPrefix() {
		String prefix = config.routing_prefix();
		if (prefix == null || prefix.isBlank()) {
			return "ddsr.rpc";
		}
		return prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
	}

	/** A name as one segment of a routing key: no separator, no wildcard. */
	static String segment(String name) {
		return name.replaceAll("[.*#\\s]", "_");
	}

	/**
	 * Who the endpoint says it is: the exporter's own name, or one from
	 * this framework and the service's id, so that two frameworks
	 * exporting the same interface do not look like one provider
	 * restarting.
	 */
	private String providerNameFor(Map<String, ?> properties) {
		Object named = properties == null ? null : properties.get("ddsr.provider.name");
		if (named != null && !named.toString().isBlank()) {
			return named.toString();
		}
		Object serviceId = properties == null ? null : properties.get(Constants.SERVICE_ID);
		return "rsa-" + frameworkTag + (serviceId == null ? "" : "-" + serviceId);
	}

	private static ServiceImplementation implementationOf(ServiceInterface contract, MessagingFlavor flavor,
			String provider) {
		ServiceImplementation implementation = ServicesFactory.eINSTANCE.createServiceImplementation();
		implementation.setName(provider + "-" + contract.getName());
		implementation.setVersion(contract.getVersion());
		implementation.setImplementationId(provider + ":" + contract.getName() + ":" + contract.getVersion());
		implementation.getServiceInterfaces().add(contract);
		implementation.getFlavors().add(flavor);
		return implementation;
	}

	private static final class AmqpEndpoint implements ExportedEndpoint {

		private final ServiceInterface contract;

		private final ServiceImplementation implementation;

		private final MessagingDistribution.Served served;

		private final String routingKey;

		private final AtomicBoolean open = new AtomicBoolean(true);

		AmqpEndpoint(ServiceInterface contract, ServiceImplementation implementation,
				MessagingDistribution.Served served, String routingKey) {
			this.contract = contract;
			this.implementation = implementation;
			this.served = served;
			this.routingKey = routingKey;
		}

		@Override
		public ServiceInterface contract() {
			return contract;
		}

		@Override
		public ServiceImplementation implementation() {
			return implementation;
		}

		@Override
		public Map<String, Object> properties() {
			MessagingFlavor flavor = (MessagingFlavor) implementation.getFlavors().get(0);
			return Map.of(ROUTING_KEY_PROPERTY, routingKey, BROKER_PROPERTY, flavor.getBrokers().get(0),
					EXCHANGE_PROPERTY, flavor.getExchange());
		}

		@Override
		public void close() {
			if (open.compareAndSet(true, false)) {
				served.close();
			}
		}
	}
}
