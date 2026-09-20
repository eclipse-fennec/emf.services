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
 * Discovery over the broker, with the events heard over MQTT.
 *
 * <p>The same registry as {@code fennec.rest} discovery and the same
 * announcement — a publish is what makes a service findable, and the
 * broker has one API for it. What differs is how this node learns that
 * something changed: not an SSE stream it holds open, but a
 * subscription on the broker's event topics.
 *
 * <p>Which is why this component is thin and has one mandatory
 * reference that looks redundant and is not. The SDK picks its event
 * source by the client's own configuration, so a deployment could name
 * {@code fennec.mqtt} here while the client still listens over SSE, and
 * nothing would be wrong except the deployment's belief about itself.
 * Requiring the MQTT event source to be present makes this component
 * dormant in a runtime that has not got one, so a configuration that
 * says MQTT and means SSE fails to start instead of quietly working.
 *
 * <p>It is still the client's {@code eventSource.target} that decides
 * which source is actually used. The role configuration in
 * {@code rsa.config} writes both, which is where the two stop being
 * two facts.
 */
@Designate(ocd = MqttDiscoveryProvider.Config.class)
// ddsr.rsa.flavor is how an admin's configuration points at this one:
// discovery.target.
@Component(service = ServiceDiscovery.class,
		configurationPid = MqttDiscoveryProvider.PID,
		property = "ddsr.rsa.flavor=fennec.mqtt",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MqttDiscoveryProvider implements ServiceDiscovery {

	/** The configuration this provider reads. */
	public static final String PID = "org.eclipse.fennec.services.rsa.discovery.mqtt";

	/** The RSA configuration type a deployment names to discover this way. */
	public static final String CONFIG_TYPE = "fennec.mqtt";

	private static final Logger LOG = Logger.getLogger(MqttDiscoveryProvider.class.getName());

	@ObjectClassDefinition(name = "Fennec Services RSA MQTT Discovery",
			description = "Which broker hears about exported services, with the events arriving over MQTT.")
	public @interface Config {

		@AttributeDefinition(name = "Broker URL",
				description = "Only used to name a contract in the publish body: the announcement references "
						+ "<broker>/catalog/<name> instead of carrying a second copy of the contract. Where "
						+ "the events arrive from is the MQTT event transport's own configuration.")
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
	 * is saying it hears over MQTT; without the source it would be
	 * saying something untrue, and a component that does not start is a
	 * better way to learn that than a stream that quietly arrives on the
	 * other transport.
	 */
	@Reference(target = "(ddsr.event.transport=mqtt)")
	private EventSource events;

	private Config config;

	@Activate
	void activate(Config config) {
		this.config = config;
		LOG.info("[DDSR] MQTT discovery ready — announcing to " + config.broker_url()
				+ ", listening over the MQTT event transport");
	}

	/** A changed configuration is taken, not died of (#107). */
	@Modified
	void modified(Config config) {
		if (!config.broker_url().equals(this.config.broker_url())) {
			LOG.info("[DDSR] MQTT discovery now announces to " + config.broker_url());
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
