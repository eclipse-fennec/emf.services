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

package org.eclipse.fennec.services.provider.rest;

import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.Registration;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyReader;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jakarta.ws.rs.core.Application;

/**
 * Serves one contract over REST, from its model and nothing else.
 *
 * <p>A factory configuration names the implementation document and the
 * service that implements the contract; this component reads the flavor
 * out of the document and registers itself as a Jakarta REST application
 * under the path the flavor states. The dispatcher inside it routes every
 * request through the same bindings a consumer reads — there is no code
 * per contract, and no generation step.
 *
 * <p>It is registered as an {@link Application} rather than as a resource
 * so each distributed implementation gets its own base path: two contracts
 * of one provider would otherwise collide at the root, and the whiteboard
 * offers exactly this with {@code osgi.jakartars.application.base}.
 *
 * <p>The service is taken through {@link ComponentServiceObjects} so a
 * PROTOTYPE-scoped implementation gets an instance per call rather than
 * one shared across every consumer.
 *
 * <p>With {@code publish} it also announces what it serves, which is what
 * makes a headless provider possible: a configuration and a service, no
 * code. Two ordering caveats come with putting both jobs in one
 * component, and neither is hidden here. On activation the announcement
 * goes out before the whiteboard has mounted the application, because
 * Declarative Services registers this component's service only after
 * {@code activate} returns — so there is a brief window in which a
 * consumer can find an endpoint that is not answering yet. On
 * deactivation it is the other way round: the service is unregistered
 * before {@code deactivate} runs, so the endpoint stops before the
 * withdrawal is announced, which is the opposite of the order FR-P3
 * asks for. A deployment that must have that order announces from a
 * separate component and leaves {@code publish} off here.
 */
@Designate(ocd = GenericRestDistribution.Config.class, factory = true)
@Component(
		service = Application.class,
		configurationPid = "org.eclipse.fennec.services.provider.rest",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class GenericRestDistribution extends Application {

	private static final Logger LOG = Logger.getLogger(GenericRestDistribution.class.getName());

	@ObjectClassDefinition(
			name = "Generic REST distribution",
			description = "Serves one contract over REST from its published model.")
	public @interface Config {

		@AttributeDefinition(
				name = "Contract",
				description = "Name of the contract this configuration serves.")
		String ddsr_contract() default "";

		@AttributeDefinition(
				name = "Service filter",
				description = "Which service implements the contract, as an LDAP filter — usually "
						+ "(ddsr.contract=<name>). A filter rather than a typed reference on purpose: an "
						+ "implementation registers under whatever interface it has, and a generic transport "
						+ "must not require it to be one this bundle knows.")
		String service_filter();

		@AttributeDefinition(
				name = "Model bundle",
				description = "Symbolic name of the bundle carrying the implementation document. The model "
						+ "travels with the provider that serves it, not with this component.")
		String model_bundle();

		@AttributeDefinition(
				name = "Model entry",
				description = "Path of the document inside that bundle, e.g. model/binding-probe.xmi. It "
						+ "holds the ServiceProvider whose implementation is served, and the contract it "
						+ "references — one document, so every reference in it resolves wherever it is read.")
		String model_entry();

		@AttributeDefinition(
				name = "Publish",
				description = "Whether this component also announces the implementation to the broker. "
						+ "With it, a provider is a configuration and a service and no code at all; "
						+ "without it, something else does the announcing and this only serves.")
		boolean publish() default false;

		@AttributeDefinition(
				name = "Public URL",
				description = "Where consumers reach this deployment, e.g. http://host:9091/payments. Where "
						+ "an instance runs is deployment, not contract, so it is stated here and written "
						+ "into the published flavor — the modelled basePath is appended to it.")
		String public_url() default "";

		@AttributeDefinition(
				name = "Broker URL",
				description = "Only used to name the contract in the publish body: the announcement "
						+ "references <broker>/catalog/<name> instead of carrying a second copy of the "
						+ "contract. The transport to the broker is configured on the client's REST "
						+ "proxy, not here.")
		String broker_url() default "";

		@AttributeDefinition(
				name = "Application base",
				description = "Where the Jakarta REST whiteboard mounts this application. Leave empty to take "
						+ "the flavor's basePath, which is the usual case.")
		String osgi_jakartars_application_base() default "";
	}

	/**
	 * Read through the ResourceSet that carries the services model, so the
	 * document resolves against the registered package rather than against
	 * whatever happens to be on a classpath.
	 */
	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	private volatile DdsrClient client;

	private volatile BrokerCatalog catalog;

	private RestDispatcher dispatcher;

	private Registration registration;

	private ResourceSet publishedFrom;

	private Config config;

	private ServiceImplementation implementation;

	/**
	 * Optional on purpose: serving and announcing are separate jobs, and
	 * a deployment that only serves must not wait for a broker client
	 * that will never come.
	 *
	 * <p>Bound through a method rather than a field because the arrival
	 * is what triggers the announcement. A broker client that shows up
	 * after this component activated is the normal case, not an edge
	 * one — its own configuration has to be applied first — and an
	 * announcement that only ever happened in {@code activate} would
	 * simply be skipped then, leaving an endpoint nobody can discover.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL,
			policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.RELUCTANT)
	void setClient(DdsrClient client) {
		this.client = client;
		announceIfReady();
	}

	void unsetClient(DdsrClient client) {
		if (this.client == client) {
			withdraw();
			this.client = null;
		}
	}

	/**
	 * The REST proxy explicitly: a deployment that carries broker.core
	 * for the interface alone also has that bundle's own in-process
	 * catalog to offer, and announcing into an empty local broker looks
	 * like success and reaches nobody.
	 */
	@Reference(target = "(ddsr.broker.transport=rest)",
			cardinality = ReferenceCardinality.OPTIONAL,
			policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.RELUCTANT)
	void setCatalog(BrokerCatalog catalog) {
		this.catalog = catalog;
		announceIfReady();
	}

	void unsetCatalog(BrokerCatalog catalog) {
		if (this.catalog == catalog) {
			this.catalog = null;
		}
	}

	@Activate
	void activate(BundleContext context, Config config) {
		// The ResourceSet is held for as long as the model is: releasing
		// it while still reading the documents it loaded would pull the
		// ground out from under the dispatcher.
		this.publishedFrom = resourceSets.getService();
		ServiceImplementation implementation = load(context, config, publishedFrom);
		RestFlavor flavor = restFlavorOf(implementation);
		String contract = implementation.getServiceInterfaces().get(0).getName();

		this.dispatcher = new RestDispatcher(flavor,
				() -> implementation(context, config.service_filter()), contract, resourceSets);
		LOG.info("[DDSR] serving " + contract + " generically at " + flavor.getBasePath());

		this.config = config;
		this.implementation = implementation;
		announceIfReady();
	}

	/**
	 * Announce once everything it takes is there — the model, both
	 * broker-side services, and a configuration that asks for it. Called
	 * from activation and from every binding, because which of them
	 * comes last is not ours to decide.
	 */
	private synchronized void announceIfReady() {
		if (registration != null || config == null || !config.publish() || implementation == null) {
			return;
		}
		if (client == null || catalog == null) {
			return;
		}
		announce(config, implementation);
	}

	/**
	 * Announce the implementation this component serves.
	 *
	 * <p>With this, a provider is a configuration and a service: nothing
	 * states the contract twice, because what is announced is the same
	 * document the dispatcher serves from. Only the deployment facts are
	 * filled in — where this instance is reachable is not contract.
	 *
	 * <p>The contract moves into a resource named by its catalog URL
	 * before publishing, so the body references it instead of carrying a
	 * second copy of it. The broker recognises that URL and rewires to
	 * its own entry.
	 *
	 * <p>A failure here leaves the endpoint serving. That is deliberate:
	 * an unreachable broker is a reason not to be discovered, not a
	 * reason to stop answering the consumers that already found us.
	 */
	private void announce(Config config, ServiceImplementation implementation) {
		DdsrClient broker = this.client;
		BrokerCatalog brokerCatalog = this.catalog;
		try {
			ServiceInterface contract = implementation.getServiceInterfaces().get(0);
			Diagnostic added = brokerCatalog.addCatalogEntry(contract, config.ddsr_contract() + "-distribution");
			if (added.getSeverity() == DiagnosticSeverity.ERROR && added.getCode() != 202) {
				throw new IllegalStateException("the broker refused the contract: " + added.getMessage());
			}

			String entryUrl = config.broker_url().replaceFirst("/+$", "") + "/catalog/" + contract.getName();
			publishedFrom.createResource(org.eclipse.emf.common.util.URI.createURI(entryUrl))
					.getContents().add(contract);

			RestFlavor flavor = restFlavorOf(implementation);
			URI url = URI.create(config.public_url());
			String mountPath = flavor.getBasePath() != null ? flavor.getBasePath() : "";
			flavor.setHost(url.getScheme() + "://" + url.getAuthority());
			flavor.setBasePath((url.getPath() != null ? url.getPath() : "") + mountPath);

			this.registration = broker.provider().publish(
					(ServiceProvider) implementation.eContainer(), implementation);
			LOG.info("[DDSR] published " + contract.getName() + " at " + flavor.getHost()
					+ flavor.getBasePath() + " (contract → " + entryUrl + ")");
		} catch (RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] publishing " + config.ddsr_contract()
					+ " failed — the endpoint keeps serving", failure);
		}
	}

	@Deactivate
	void deactivate() {
		withdraw();
		if (publishedFrom != null) {
			resourceSets.ungetService(publishedFrom);
			publishedFrom = null;
		}
	}

	private synchronized void withdraw() {
		if (registration == null) {
			return;
		}
		try {
			registration.withdraw();
		} catch (RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] withdrawing failed", failure);
		}
		registration = null;
	}


	/**
	 * The service implementing this contract, looked up per call so a
	 * provider that comes and goes is followed without bookkeeping.
	 * Through {@link ServiceObjects} so a PROTOTYPE-scoped implementation
	 * yields an instance per call instead of one shared across consumers.
	 *
	 * <p>By filter rather than by a typed reference on purpose: an
	 * implementation registers under whatever interface it has, and a
	 * generic transport must not require it to be one this bundle knows.
	 */
	@SuppressWarnings("unchecked")
	private static ServiceObjects<Object> implementation(BundleContext context, String serviceFilter) {
		try {
			ServiceReference<?>[] candidates = context.getServiceReferences((String) null, serviceFilter);
			if (candidates == null || candidates.length == 0) {
				return null;
			}
			return (ServiceObjects<Object>) context.getServiceObjects(candidates[0]);
		} catch (InvalidSyntaxException malformed) {
			throw new IllegalStateException("service filter is not a filter: " + serviceFilter, malformed);
		}
	}

	/**
	 * The implementation document, from the bundle that owns it. The model
	 * stays where the provider keeps it — this component only needs to be
	 * told which bundle and which entry.
	 */
	private static ServiceImplementation load(BundleContext context, Config config, ResourceSet resourceSet) {
		Bundle owner = null;
		for (Bundle candidate : context.getBundles()) {
			if (candidate.getSymbolicName().equals(config.model_bundle())) {
				owner = candidate;
				break;
			}
		}
		if (owner == null) {
			throw new IllegalStateException("no bundle " + config.model_bundle() + " to read the model from");
		}
		URL entry = owner.getEntry(config.model_entry());
		if (entry == null) {
			throw new IllegalStateException(config.model_bundle() + " carries no entry " + config.model_entry());
		}
		Resource document = resourceSet.getResource(org.eclipse.emf.common.util.URI
				.createURI(entry.toString()), true);
		ServiceProvider provider = (ServiceProvider) document.getContents().get(0);
		return provider.getImplementations().get(0);
	}

	/**
	 * The dispatcher, and the XMI codec it needs.
	 *
	 * <p>The codec's providers are registered as whiteboard extensions
	 * elsewhere, which attaches them to the default application — not to
	 * this one. An application carries its own, so a contract whose
	 * operations take or return a model is served here the same way the
	 * hand-written broker endpoints serve theirs.
	 */
	@Override
	public Set<Object> getSingletons() {
		if (dispatcher == null) {
			return Set.of();
		}
		return Set.of(dispatcher,
				new XmiMessageBodyReader(resourceSets),
				new XmiMessageBodyWriter(resourceSets));
	}

	private static RestFlavor restFlavorOf(ServiceImplementation implementation) {
		for (ServiceFlavor flavor : implementation.getFlavors()) {
			if (flavor instanceof RestFlavor rest) {
				return rest;
			}
		}
		throw new IllegalStateException("implementation " + implementation.getName()
				+ " has no RestFlavor — nothing to serve over REST");
	}
}
