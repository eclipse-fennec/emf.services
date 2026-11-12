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

package org.eclipse.fennec.services.rsa.discovery.mqtt;

import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.rsa.discovery.BrokerDiscovery;
import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.eclipse.fennec.services.rsa.spi.ServiceDiscovery;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Discovery for a node that hears the broker's events over AMQP — the
 * twin of {@link MqttDiscoveryProvider} for the configuration type
 * {@code fennec.amqp} (#138).
 *
 * <p>There is nothing AMQP in here, and that is the point: over a
 * messaging broker the events are the same events on a channel spelled
 * differently, and the event transport reads the difference off the
 * scheme of its broker URL. What this component adds is what its twin
 * adds: a configuration type an admin can name, and the insistence that
 * the event source it names is really there — a deployment that says
 * AMQP while its client listens over SSE does not start.
 */
@Designate(ocd = AmqpDiscoveryProvider.Config.class)
// ddsr.rsa.flavor is how an admin's configuration points at this one:
// discovery.target.
@Component(service = ServiceDiscovery.class,
		configurationPid = AmqpDiscoveryProvider.PID,
		property = "ddsr.rsa.flavor=fennec.amqp",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AmqpDiscoveryProvider implements ServiceDiscovery {

	/** The configuration this provider reads. */
	public static final String PID = "org.eclipse.fennec.services.rsa.discovery.amqp";

	/** The RSA configuration type a deployment names to discover this way. */
	public static final String CONFIG_TYPE = "fennec.amqp";

	private static final Logger LOG = Logger.getLogger(AmqpDiscoveryProvider.class.getName());

	@ObjectClassDefinition(name = "Fennec Services RSA AMQP Discovery",
			description = "Which broker hears about exported services, with the events arriving over AMQP.")
	public @interface Config {

		@AttributeDefinition(name = "Broker URL",
				description = "Only used to name a contract in the publish body: the announcement references "
						+ "<broker>/catalog/<name> instead of carrying a second copy of the contract. Where "
						+ "the events arrive from is the event transport's own configuration — the same one, with an amqp:// broker URL.")
		String broker_url();
	}

	@Reference
	private DdsrClient client;

	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerCatalog catalog;

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	/**
	 * Present, not used. A deployment that names this configuration type
	 * is saying it hears over AMQP; without the source it would be
	 * saying something untrue, and a component that does not start is a
	 * better way to learn that than a stream that quietly arrives on the
	 * other transport.
	 */
	@Reference(target = "(ddsr.event.transport=amqp)")
	private EventSource events;

	private Config config;

	@Activate
	void activate(Config config) {
		this.config = config;
		LOG.info("[DDSR] AMQP discovery ready — announcing to " + config.broker_url()
				+ ", listening over the AMQP event transport");
	}

	/** A changed configuration is taken, not died of (#107). */
	@Modified
	void modified(Config config) {
		if (!config.broker_url().equals(this.config.broker_url())) {
			LOG.info("[DDSR] AMQP discovery now announces to " + config.broker_url());
		}
		this.config = config;
	}

	@Override
	public String[] supportedConfigs() {
		return discovery().supportedConfigs();
	}

	@Override
	public AutoCloseable announce(ExportedEndpoint endpoint) {
		return discovery().announce(endpoint);
	}

	@Override
	public AutoCloseable watch(String contractName, DiscoveryListener listener) {
		return discovery().watch(contractName, listener);
	}

	private BrokerDiscovery discovery() {
		return new BrokerDiscovery(CONFIG_TYPE, client, catalog, resourceSets, config.broker_url());
	}
}
