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

package org.eclipse.fennec.services.rsa.discovery.rest;

import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.client.DdsrClient;
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
 * Discovery over the broker, named by the {@code fennec.rest}
 * configuration type.
 *
 * <p>Announcing is a publish: the contract goes into the catalog and the
 * implementation is registered, which is what makes it findable — the
 * registry is the discovery. Hearing is the other direction of the same
 * thing: a lookup for what is there now, and the broker's event stream
 * for what changes.
 *
 * <p>Nothing here is new machinery. The value is that Remote Service
 * Admin gets a discovery it can drive, and that the transport it runs
 * over is a choice: an MQTT discovery is another implementation of this
 * interface, not a change to anything that uses it.
 */
@Designate(ocd = RestDiscoveryProvider.Config.class)
// ddsr.rsa.flavor is how an admin's configuration points at this one:
// discovery.target.
@Component(service = ServiceDiscovery.class,
		configurationPid = RestDiscoveryProvider.PID,
		property = "ddsr.rsa.flavor=fennec.rest",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RestDiscoveryProvider implements ServiceDiscovery {

	/** The configuration both this provider and the endpoint bridge read. */
	public static final String PID = "org.eclipse.fennec.services.rsa.discovery.rest";

	/** The RSA configuration type a deployment names to discover this way. */
	public static final String CONFIG_TYPE = "fennec.rest";

	private static final Logger LOG = Logger.getLogger(RestDiscoveryProvider.class.getName());

	@ObjectClassDefinition(name = "Fennec Services RSA REST Discovery",
			description = "Which broker hears about exported services, and is asked about others'.")
	public @interface Config {

		@AttributeDefinition(name = "Broker URL",
				description = "Only used to name a contract in the publish body: the announcement references "
						+ "<broker>/catalog/<name> instead of carrying a second copy of the contract. The "
						+ "transport to the broker is configured on the client's REST proxy.")
		String broker_url();
	}

	@Reference
	private DdsrClient client;

	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerCatalog catalog;

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	private Config config;

	@Activate
	void activate(Config config) {
		this.config = config;
		LOG.info("[DDSR] discovery ready — announcing to and watching " + config.broker_url());
	}

	/**
	 * A changed configuration is taken, not died of — see the same method
	 * on the REST distribution (#107). A discovery that is destroyed and
	 * rebuilt mid-export hands out service objects that are already dead,
	 * and the export fails for a reason no log explains.
	 */
	@Modified
	void modified(Config config) {
		if (!config.broker_url().equals(this.config.broker_url())) {
			LOG.info("[DDSR] discovery now talks to " + config.broker_url());
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

	/**
	 * Built per call rather than held, because the configuration may
	 * change under it and the broker URL it names is read at publish
	 * time. What discovery over the broker means lives in
	 * {@link BrokerDiscovery} — this component is which configuration
	 * type answers to it.
	 */
	private BrokerDiscovery discovery() {
		return new BrokerDiscovery(CONFIG_TYPE, client, catalog, resourceSets, config.broker_url());
	}
}
