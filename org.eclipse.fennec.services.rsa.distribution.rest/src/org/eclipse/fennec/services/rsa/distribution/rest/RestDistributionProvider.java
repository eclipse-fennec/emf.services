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

package org.eclipse.fennec.services.rsa.distribution.rest;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.derive.JavaContracts;
import org.eclipse.fennec.services.provider.rest.RestDistribution;
import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.eclipse.fennec.services.rsa.spi.FlavorDistribution;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Exports a service over REST.
 *
 * <p>Three steps and nothing else: the contract is derived from the
 * exported interface, a flavor is built that says how to call it, and
 * the generic distribution (#84) mounts an endpoint that answers by
 * invoking the service. Nothing here writes a contract, a resource class
 * or a publisher — those all exist already, and the whole point of this
 * being small is that they do.
 *
 * <p>It does not announce anything. Telling others is discovery's job,
 * and a deployment may do that over a different transport than the one
 * it serves on.
 */
@Designate(ocd = RestDistributionProvider.Config.class)
@Component(service = FlavorDistribution.class,
		configurationPid = "org.eclipse.fennec.services.rsa.distribution.rest",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RestDistributionProvider implements FlavorDistribution {

	/** The RSA configuration type a service asks for to be exported this way. */
	public static final String CONFIG_TYPE = "fennec.rest";

	private static final Logger LOG = Logger.getLogger(RestDistributionProvider.class.getName());

	@ObjectClassDefinition(name = "Fennec Services RSA REST Distribution",
			description = "Where services exported over REST are reachable.")
	public @interface Config {

		@AttributeDefinition(name = "Public URL",
				description = "Where consumers reach this deployment, e.g. http://host:9091/services. Every "
						+ "exported contract is served under its own path below it.")
		String public_url();

		@AttributeDefinition(name = "Default contract version",
				description = "The version a derived contract gets when the exporting service names none. "
						+ "A contract without a version cannot be addressed.")
		String default_version() default "1.0.0";
	}

	@Reference
	private RestDistribution distribution;

	private Config config;

	@Activate
	void activate(Config config) {
		this.config = config;
	}

	@Override
	public String[] supportedConfigs() {
		return new String[] { CONFIG_TYPE };
	}

	@Override
	public FlavorKind flavor() {
		return FlavorKind.REST;
	}

	@Override
	public ExportedEndpoint export(Object service, Class<?>[] contracts, Map<String, ?> properties) {
		if (contracts == null || contracts.length != 1) {
			// One contract, one endpoint. A service exported as two
			// interfaces is two exports — anything else would need one
			// path to mean two contracts.
			throw new IllegalArgumentException("a REST export covers exactly one interface, got "
					+ (contracts == null ? 0 : contracts.length));
		}
		String version = version(properties);
		ServiceInterface contract = JavaContracts.contractOf(contracts[0], version);
		String mountPath = RestFlavors.basePathFor(contract);
		RestFlavor flavor = RestFlavors.flavorFor(contract, mountPath);

		RestDistribution.Served served = distribution.serve(flavor, service, contract.getName());

		// Only now does the flavor say where it is: what was mounted is
		// this deployment's business, what a consumer dials is the
		// deployment's address plus that path.
		URI publicUrl = URI.create(config.public_url());
		flavor.setHost(publicUrl.getScheme() + "://" + publicUrl.getAuthority());
		flavor.setBasePath((publicUrl.getPath() == null ? "" : publicUrl.getPath()) + mountPath);

		ServiceImplementation implementation = implementationOf(contract, flavor, properties);
		LOG.info("[DDSR] exported " + contract.getName() + " over REST at "
				+ flavor.getHost() + flavor.getBasePath());
		return new RestEndpoint(contract, implementation, served);
	}

	private String version(Map<String, ?> properties) {
		Object stated = properties == null ? null : properties.get("ddsr.contract.version");
		return stated == null || stated.toString().isBlank() ? config.default_version() : stated.toString();
	}

	private static ServiceImplementation implementationOf(ServiceInterface contract, RestFlavor flavor,
			Map<String, ?> properties) {
		Object providerName = properties == null ? null : properties.get("ddsr.provider.name");
		String provider = providerName == null || providerName.toString().isBlank()
				? "rsa-export"
				: providerName.toString();

		ServiceImplementation implementation = ServicesFactory.eINSTANCE.createServiceImplementation();
		implementation.setName(provider + "-" + contract.getName());
		implementation.setVersion(contract.getVersion());
		implementation.setImplementationId(provider + ":" + contract.getName() + ":" + contract.getVersion());
		implementation.getServiceInterfaces().add(contract);
		implementation.getFlavors().add(flavor);
		return implementation;
	}

	private static final class RestEndpoint implements ExportedEndpoint {

		private final ServiceInterface contract;
		private final ServiceImplementation implementation;
		private final RestDistribution.Served served;
		private final AtomicBoolean open = new AtomicBoolean(true);

		RestEndpoint(ServiceInterface contract, ServiceImplementation implementation,
				RestDistribution.Served served) {
			this.contract = contract;
			this.implementation = implementation;
			this.served = served;
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
		public void close() {
			if (open.compareAndSet(true, false)) {
				served.close();
			}
		}
	}
}
