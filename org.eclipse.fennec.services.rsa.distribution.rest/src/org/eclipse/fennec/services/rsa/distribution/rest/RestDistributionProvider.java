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
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.SecuritySchemeKind;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.common.SecurityRequirements;
import org.eclipse.fennec.services.provider.rest.RestDistribution;
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
// ddsr.rsa.flavor is how an admin's configuration points at this one:
// distribution.target. A deployment says which transport it exports
// over; it is not decided by whoever happens to be registered.
@Component(service = FlavorDistribution.class,
		configurationPid = "org.eclipse.fennec.services.rsa.distribution.rest",
		property = "ddsr.rsa.flavor=fennec.rest",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RestDistributionProvider implements FlavorDistribution {

	/** The RSA configuration type a service asks for to be exported this way. */
	public static final String CONFIG_TYPE = "fennec.rest";

	/**
	 * The one configuration-type property on the endpoint description:
	 * where the exported contract answers. An exporter may set it to say
	 * a different public address for this one service; it has to be a
	 * URL then, and anything else is refused as the garbage it is.
	 */
	public static final String URL_PROPERTY = CONFIG_TYPE + ".url";

	private static final Logger LOG = Logger.getLogger(RestDistributionProvider.class.getName());

	@ObjectClassDefinition(name = "Fennec Services RSA REST Distribution",
			description = "Where services exported over REST are reachable.")
	public @interface Config {

		@AttributeDefinition(name = "Public URL",
				description = "Where consumers reach this deployment, e.g. http://host:9091/services. Every "
						+ "exported contract is served under its own path below it.")
		String public_url();

		@AttributeDefinition(name = "Token issuer",
				description = "The issuer of the bearer tokens this node's verifier accepts. With it, this "
						+ "distribution promises the intent fennec.bearer and serves contracts that demand "
						+ "authentication; without it, such contracts are not exported. Empty means none.")
		String issuer() default "";

		@AttributeDefinition(name = "Token audiences",
				description = "The audiences a token for this node must carry one of — what a consumer asks "
						+ "its identity provider for, per target.")
		String[] audiences() default {};

		@AttributeDefinition(name = "JWKS URI", description = "Where the issuer's signing keys are fetched.")
		String jwks_uri() default "";

		@AttributeDefinition(name = "Scope claim",
				description = "Where the verifier reads scopes from: 'scope', or 'realm_access.roles' for roles.")
		String scope_claim() default "scope";

	}

	@Reference
	private RestDistribution distribution;

	private Config config;

	/** This framework's short name in identities nobody named themselves. */
	private String frameworkTag;

	@Activate
	void activate(BundleContext context, Config config) {
		this.config = config;
		String uuid = context.getProperty(Constants.FRAMEWORK_UUID);
		this.frameworkTag = uuid == null || uuid.length() < 8 ? "local" : uuid.substring(0, 8);
		LOG.info("[DDSR] REST distribution ready — exports are reachable under " + config.public_url());
	}

	/**
	 * A changed configuration is taken, not died of.
	 *
	 * <p>Without this method the component runtime answers every
	 * configuration update by destroying the instance and building a new
	 * one — and Configuration Admin delivers the same configuration more
	 * than once while a framework starts. Anything holding this
	 * provider's service objects at that moment (an export in flight,
	 * for one) finds them dead, which is how #107 showed itself.
	 *
	 * <p>Endpoints already mounted keep the address they were announced
	 * with; a new public URL applies to what is exported from now on.
	 * Re-announcing the live ones belongs to the configuration work in
	 * #109, which is where the address stops being said twice.
	 */
	@Modified
	void modified(Config config) {
		if (!config.public_url().equals(this.config.public_url())) {
			LOG.info("[DDSR] REST distribution now says " + config.public_url()
					+ "; endpoints already announced keep their address");
		}
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

	/** {@code osgi.basic}, and {@code fennec.bearer} once there is an issuer to name. */
	@Override
	public String[] supportedIntents() {
		return config.issuer().isBlank() ? new String[] { "osgi.basic" }
				: new String[] { "osgi.basic", BEARER_INTENT };
	}

	@Override
	public ExportedEndpoint export(Object service, ServiceInterface contract, Map<String, ?> properties) {
		if (contract == null) {
			throw new IllegalArgumentException("a REST export needs the contract to serve");
		}
		URI publicUrl = publicUrlFor(properties);
		String provider = providerNameFor(properties);
		// Mounted under the provider, not the contract alone: two services
		// exporting the same interface in one framework are two endpoints,
		// and two applications on one path would be one of them answering
		// for both.
		String mountPath = "/" + provider + RestFlavors.basePathFor(contract);
		RestFlavor flavor = RestFlavors.flavorFor(contract, mountPath);
		// How a caller proves itself, said in the flavor — and only for a
		// contract that asks: an open contract stays exactly what it was.
		// Without an issuer a guarded contract gets no scheme, and serving
		// it refuses (fail closed).
		if (SecurityRequirements.demandsAuthentication(contract) && !config.issuer().isBlank()) {
			flavor.getSecuritySchemes().add(bearerScheme());
		}

		RestDistribution.Served served = distribution.serve(flavor, service, provider + ":" + contract.getName());

		// Only now does the flavor say where it is: what was mounted is
		// this deployment's business, what a consumer dials is the
		// deployment's address plus that path.
		flavor.setHost(publicUrl.getScheme() + "://" + publicUrl.getAuthority());
		flavor.setBasePath((publicUrl.getPath() == null ? "" : publicUrl.getPath()) + mountPath);

		ServiceImplementation implementation = implementationOf(contract, flavor, provider);
		LOG.info("[DDSR] exported " + contract.getName() + " over REST at "
				+ flavor.getHost() + flavor.getBasePath());
		return new RestEndpoint(contract, implementation, served);
	}

	private BearerTokenScheme bearerScheme() {
		BearerTokenScheme scheme = ServicesFactory.eINSTANCE.createBearerTokenScheme();
		scheme.setName("bearer");
		scheme.setKind(SecuritySchemeKind.BEARER_TOKEN);
		scheme.setIssuer(config.issuer());
		scheme.getAudiences().addAll(List.of(config.audiences()));
		scheme.setJwksUri(config.jwks_uri().isBlank() ? null : config.jwks_uri());
		scheme.setScopeClaim(config.scope_claim());
		return scheme;
	}

	/**
	 * The configured public URL, unless the exporter said one for this
	 * service. Validated before anything is mounted, so that a refusal
	 * leaves nothing behind.
	 */
	private URI publicUrlFor(Map<String, ?> properties) {
		Object asked = properties == null ? null : properties.get(URL_PROPERTY);
		if (asked == null) {
			return URI.create(config.public_url());
		}
		if (!(asked instanceof String url) || url.isBlank()) {
			throw new IllegalArgumentException(URL_PROPERTY + " has to be a URL, got "
					+ asked.getClass().getName());
		}
		try {
			URI parsed = new URI(url);
			if (parsed.getScheme() == null || parsed.getAuthority() == null) {
				throw new IllegalArgumentException(URL_PROPERTY + " has to be an absolute URL, got '" + url + "'");
			}
			return parsed;
		} catch (URISyntaxException malformed) {
			throw new IllegalArgumentException(URL_PROPERTY + " is not a URL: '" + url + "'", malformed);
		}
	}

	/**
	 * Who the endpoint says it is. An exporter that names itself with
	 * {@code ddsr.provider.name} is taken at its word. One that does not
	 * gets a name from this framework and the service's id — the
	 * implementation id is provider, contract and version, and two
	 * frameworks exporting the same interface must not look to the broker
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

	private static ServiceImplementation implementationOf(ServiceInterface contract, RestFlavor flavor,
			String provider) {
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
		public Map<String, Object> properties() {
			RestFlavor flavor = (RestFlavor) implementation.getFlavors().get(0);
			return Map.of(URL_PROPERTY, flavor.getHost() + flavor.getBasePath());
		}

		@Override
		public void close() {
			if (open.compareAndSet(true, false)) {
				served.close();
			}
		}
	}
}
