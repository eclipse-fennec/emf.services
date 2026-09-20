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

package org.eclipse.fennec.services.rsa.distribution.mqtt;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttOperationFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.provider.mqtt.MqttDistribution;
import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.eclipse.fennec.services.rsa.spi.FlavorDistribution;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Exports a service over MQTT.
 *
 * <p>The second implementation of {@link FlavorDistribution}, and the
 * reason that interface exists: the RSA core did not change for it, and
 * neither did the REST one. Three steps, the same three: the contract
 * is derived from the exported interface, a flavor is built saying
 * which topics answer for it, and the generic MQTT distribution
 * subscribes them and invokes the service.
 *
 * <p>It does not announce anything. Telling others is discovery's job,
 * and a deployment may serve over MQTT while announcing over REST —
 * which is the pairing that proves the two halves are actually
 * separate.
 */
@Designate(ocd = MqttDistributionProvider.Config.class)
// ddsr.rsa.flavor is how an admin's configuration points at this one:
// distribution.target. A deployment says which transport it exports
// over; it is not decided by whoever happens to be registered.
@Component(service = FlavorDistribution.class,
		configurationPid = "org.eclipse.fennec.services.rsa.distribution.mqtt",
		property = "ddsr.rsa.flavor=fennec.mqtt",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MqttDistributionProvider implements FlavorDistribution {

	/** The RSA configuration type a service asks for to be exported this way. */
	public static final String CONFIG_TYPE = "fennec.mqtt";

	/**
	 * The one configuration-type property on the endpoint description:
	 * the topic base a consumer publishes calls under. The specification
	 * wants at least one property prefixed with the type's name, so that
	 * a consumer without our model still finds the address.
	 */
	public static final String TOPIC_PROPERTY = CONFIG_TYPE + ".topic";

	/** Where the calls arrive: the broker, for a consumer that has no model. */
	public static final String BROKER_PROPERTY = CONFIG_TYPE + ".broker";

	private static final Logger LOG = Logger.getLogger(MqttDistributionProvider.class.getName());

	@ObjectClassDefinition(name = "Fennec Services RSA MQTT Distribution",
			description = "Which broker services exported over MQTT listen on, and under which topics.")
	public @interface Config {

		@AttributeDefinition(name = "Broker URL",
				description = "The MQTT broker this deployment serves on, e.g. tcp://localhost:1883. It is "
						+ "announced with every export, because a consumer dials what the flavor says.")
		String broker_url();

		@AttributeDefinition(name = "Topic prefix",
				description = "Calls arrive under <prefix>/req/<provider>/<contract>/<operation> and answers "
						+ "go to <prefix>/res/<provider>/<contract>/<consumer>/<call>. Two trees, so a broker "
						+ "ACL can say who may ask and who may hear: a provider subscribes req and publishes "
						+ "res, a consumer does the opposite and only under its own name.")
		String topic_prefix() default "ddsr/rpc";
	}

	@Reference
	private MqttDistribution distribution;

	private Config config;

	/** This framework's short name in identities nobody named themselves. */
	private String frameworkTag;

	@Activate
	void activate(BundleContext context, Config config) {
		this.config = config;
		String uuid = context.getProperty(Constants.FRAMEWORK_UUID);
		this.frameworkTag = uuid == null || uuid.length() < 8 ? "local" : uuid.substring(0, 8);
		LOG.info("[DDSR] MQTT distribution ready — exports answer on " + config.broker_url()
				+ " under " + config.topic_prefix());
	}

	/**
	 * A changed configuration is taken, not died of — the same lesson as
	 * on the REST side (#107): destroying this component mid-export
	 * hands out service objects that are already dead.
	 *
	 * <p>Topics already subscribed keep answering where they were
	 * announced; a new prefix or broker applies to what is exported from
	 * now on.
	 */
	@Modified
	void modified(Config config) {
		if (!config.broker_url().equals(this.config.broker_url())
				|| !config.topic_prefix().equals(this.config.topic_prefix())) {
			LOG.info("[DDSR] MQTT distribution now says " + config.broker_url() + " / "
					+ config.topic_prefix() + "; what is already served keeps its topics");
		}
		this.config = config;
	}

	@Override
	public String[] supportedConfigs() {
		return new String[] { CONFIG_TYPE };
	}

	@Override
	public FlavorKind flavor() {
		return FlavorKind.MQTT;
	}

	@Override
	public ExportedEndpoint export(Object service, ServiceInterface contract, Map<String, ?> properties) {
		if (contract == null) {
			throw new IllegalArgumentException("an MQTT export needs the contract to serve");
		}
		String provider = providerNameFor(properties);
		// Under the provider, not the contract alone: two services
		// exporting the same interface in one framework are two
		// endpoints, and two subscriptions on one topic would be both of
		// them answering every call.
		String where = provider + "/" + contract.getName().toLowerCase(Locale.ROOT);
		// Requests and answers in SEPARATE trees, not one under the
		// other. A broker ACL is written per subtree, and the two
		// directions are not the same permission: everyone may ask a
		// provider, nobody else may read what it answered. Nesting the
		// answers under the request topic would make those one rule.
		String requestTopic = topicPrefix() + "/req/" + where;
		String responseTopic = topicPrefix() + "/res/" + where;
		MqttFlavor flavor = flavorFor(contract, requestTopic, responseTopic, brokerFor(properties));

		MqttDistribution.Served served = distribution.serve(flavor, service,
				provider + ":" + contract.getName());

		ServiceImplementation implementation = implementationOf(contract, flavor, provider);
		LOG.info("[DDSR] exported " + contract.getName() + " over MQTT on " + flavor.getBrokers().get(0)
				+ " under " + requestTopic);
		return new MqttEndpoint(contract, implementation, served, requestTopic);
	}

	/** A flavor whose topics say where each operation answers. */
	private static MqttFlavor flavorFor(ServiceInterface contract, String requestTopic,
			String responseTopic, String broker) {
		MqttFlavor flavor = ServicesFactory.eINSTANCE.createMqttFlavor();
		flavor.setName(contract.getName() + "-mqtt");
		flavor.setKind(FlavorKind.MQTT);
		flavor.getBrokers().add(broker);
		flavor.setRequestTopic(requestTopic);
		flavor.setResponseTopic(responseTopic);
		for (ServiceOperation operation : contract.getOperations()) {
			MqttOperationFlavor operationFlavor = ServicesFactory.eINSTANCE.createMqttOperationFlavor();
			operationFlavor.setName(operation.getName());
			operationFlavor.setOperation(operation);
			// Nothing about encodings here on purpose. On this wire the
			// whole call is one document, and what it is written in is the
			// flavor's default (XMI) unless a contract says otherwise —
			// saying it again per operation would be a second place for
			// the same fact to be wrong in.
			flavor.getOperationFlavors().add(operationFlavor);
		}
		return flavor;
	}

	private String topicPrefix() {
		String prefix = config.topic_prefix();
		if (prefix == null || prefix.isBlank()) {
			return "ddsr/rpc";
		}
		return prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
	}

	/**
	 * The configured broker, unless the exporter named one for this
	 * service. An exporter that says something unusable is refused
	 * before anything is subscribed, so a refusal leaves nothing behind.
	 */
	private String brokerFor(Map<String, ?> properties) {
		Object asked = properties == null ? null : properties.get(BROKER_PROPERTY);
		if (asked == null) {
			return config.broker_url();
		}
		if (!(asked instanceof String url) || url.isBlank()) {
			throw new IllegalArgumentException(BROKER_PROPERTY + " has to be a broker URL, got " + asked);
		}
		return url;
	}

	/**
	 * Who the endpoint says it is. An exporter that names itself with
	 * {@code ddsr.provider.name} is taken at its word; one that does not
	 * gets a name from this framework and the service's id, so that two
	 * frameworks exporting the same interface do not look to the broker
	 * like one provider restarting.
	 */
	private String providerNameFor(Map<String, ?> properties) {
		Object named = properties == null ? null : properties.get("ddsr.provider.name");
		if (named != null && !named.toString().isBlank()) {
			return named.toString();
		}
		Object serviceId = properties == null ? null : properties.get(Constants.SERVICE_ID);
		return "rsa-" + frameworkTag + (serviceId == null ? "" : "-" + serviceId);
	}

	private static ServiceImplementation implementationOf(ServiceInterface contract, MqttFlavor flavor,
			String provider) {
		ServiceImplementation implementation = ServicesFactory.eINSTANCE.createServiceImplementation();
		implementation.setName(provider + "-" + contract.getName());
		implementation.setVersion(contract.getVersion());
		implementation.setImplementationId(provider + ":" + contract.getName() + ":" + contract.getVersion());
		implementation.getServiceInterfaces().add(contract);
		implementation.getFlavors().add(flavor);
		return implementation;
	}

	private static final class MqttEndpoint implements ExportedEndpoint {

		private final ServiceInterface contract;

		private final ServiceImplementation implementation;

		private final MqttDistribution.Served served;

		private final String requestTopic;

		private final AtomicBoolean open = new AtomicBoolean(true);

		MqttEndpoint(ServiceInterface contract, ServiceImplementation implementation,
				MqttDistribution.Served served, String requestTopic) {
			this.contract = contract;
			this.implementation = implementation;
			this.served = served;
			this.requestTopic = requestTopic;
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
			MqttFlavor flavor = (MqttFlavor) implementation.getFlavors().get(0);
			return Map.of(TOPIC_PROPERTY, requestTopic, BROKER_PROPERTY, flavor.getBrokers().get(0));
		}

		@Override
		public void close() {
			if (open.compareAndSet(true, false)) {
				served.close();
			}
		}
	}
}
