/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.fennec.services.m2t.example;

import java.net.URI;
import java.net.URL;
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
import org.eclipse.fennec.services.UpdatePolicy;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.Registration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Announces the {@code directory-java-rest} implementation of the
 * {@code PersonDirectory} contract.
 *
 * <p>Generated from the provider document — do not edit. What it owns is
 * the order: the announcement happens on activation, and on deactivation
 * the withdrawal is awaited before anything else stops, so a consumer is
 * told while this endpoint still answers (FR-P3).
 *
 * <p>The contract is not in this file. It is read from the document that
 * ships with this bundle, which is the same one a generic distribution
 * serves from — one statement of the contract, one fingerprint.
 */
@Component(configurationPid = "org.eclipse.fennec.services.m2t.example.DirectoryJavaRestPublisher",
		configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
@Designate(ocd = DirectoryJavaRestPublisher.Config.class)
public class DirectoryJavaRestPublisher {

	private static final Logger LOG = Logger.getLogger(DirectoryJavaRestPublisher.class.getName());

	@ObjectClassDefinition(name = "directory-java-rest announcement",
			description = "Where this instance of PersonDirectory runs, and how it supersedes an earlier one.")
	public @interface Config {

		@AttributeDefinition(name = "Model bundle",
				description = "Symbolic name of the bundle carrying the provider document. Required: the "
						+ "document travels with the provider that serves it, and only the deployment "
						+ "knows which bundle that is here.")
		String model_bundle() default "";

		@AttributeDefinition(name = "Model entry",
				description = "Path of that document inside the bundle. The default follows the convention "
						+ "of naming a document after the implementation it holds.")
		String model_entry() default "model/directory-java-rest.xmi";

		@AttributeDefinition(name = "Public URL",
				description = "Where consumers reach this deployment. Where an instance runs is deployment and not contract.")
		String public_url() default "http://localhost:9095";

		@AttributeDefinition(name = "Broker URL",
				description = "Only used to name the contract in the publish body: the announcement references <broker>/catalog/<name>.")
		String broker_url() default "http://localhost:8887/ddsr/rest";

		@AttributeDefinition(name = "Update policy",
				description = "How a successor takes over: EVERGREEN, DEPRECATE_AND_DRAIN or HARD_CUTOVER. Only meaningful with a superseded version.")
		String update_policy() default "UNSPECIFIED";

		@AttributeDefinition(name = "Supersedes version",
				description = "Version of the earlier implementation this one replaces. Empty = a plain publish.")
		String replaces_version() default "";
	}

	@Reference
	private DdsrClient client;

	/**
	 * The REST proxy explicitly: a deployment that carries the broker
	 * bundle for its interfaces also has an in-process catalog, and
	 * announcing into an empty local broker looks like success and
	 * reaches nobody.
	 */
	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerCatalog catalog;

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	private ResourceSet publishedFrom;

	private Registration registration;

	@Activate
	void activate(BundleContext context, Config config) {
		if (config.model_bundle().isBlank()) {
			// Said plainly here rather than as a missing-bundle message
			// three frames down: what is missing is a configuration, not
			// a bundle.
			LOG.warning("[DDSR] directory-java-rest is not configured with a model bundle — announcing nothing");
			return;
		}
		try {
			this.publishedFrom = resourceSets.getService();
			ServiceImplementation implementation = load(context, config, publishedFrom);
			ServiceInterface contract = implementation.getServiceInterfaces().get(0);

			Diagnostic added = catalog.addCatalogEntry(contract, "directory-java-rest");
			if (added.getSeverity() == DiagnosticSeverity.ERROR && added.getCode() != 202) {
				throw new IllegalStateException("the broker refused the contract: " + added.getMessage());
			}

			// The contract moves into a resource named by its catalog URL,
			// so the publish body references it instead of carrying a
			// second copy. The broker recognises the URL and rewires to
			// its own entry.
			String entryUrl = config.broker_url().replaceFirst("/+$", "") + "/catalog/" + contract.getName();
			publishedFrom.createResource(org.eclipse.emf.common.util.URI.createURI(entryUrl))
					.getContents().add(contract);

			whereItRuns(implementation, config.public_url());
			supersede(implementation, config);

			this.registration = client.provider().publish(
					(ServiceProvider) implementation.eContainer(), implementation);
			LOG.info("[DDSR] announced PersonDirectory as directory-java-rest"
					+ " (contract → " + entryUrl + ")");
		} catch (RuntimeException failure) {
			// The endpoint keeps serving: an unreachable broker is a
			// reason not to be discovered, not a reason to stop
			// answering the consumers that already found us.
			LOG.log(Level.WARNING, "[DDSR] announcing directory-java-rest failed", failure);
		}
	}

	/**
	 * Withdraw, and wait for it.
	 *
	 * <p>Deliberately synchronous: deactivation returns only once the
	 * broker has acknowledged, so consumers are informed while this
	 * provider still answers. Stopping first and telling afterwards is
	 * the one order that produces connection refused at a consumer that
	 * did everything right.
	 */
	@Deactivate
	void deactivate() {
		if (registration != null) {
			try {
				Diagnostic withdrawn = registration.withdraw();
				if (withdrawn != null && withdrawn.getSeverity() == DiagnosticSeverity.ERROR) {
					// stdout, not JUL: on a shutdown path the LogManager
					// may already have been reset.
					System.out.println("[DDSR] withdraw rejected: " + withdrawn.getMessage());
				}
			} catch (RuntimeException failure) {
				System.out.println("[DDSR] withdraw failed: " + failure);
			}
			registration = null;
		}
		if (publishedFrom != null) {
			resourceSets.ungetService(publishedFrom);
			publishedFrom = null;
		}
	}

	/** Where this instance answers, written into the announced flavor. */
	private static void whereItRuns(ServiceImplementation implementation, String publicUrl) {
		if (publicUrl == null || publicUrl.isBlank()) {
			return;
		}
		for (ServiceFlavor flavor : implementation.getFlavors()) {
			if (flavor instanceof RestFlavor rest) {
				URI url = URI.create(publicUrl);
				String modelled = rest.getBasePath() != null ? rest.getBasePath() : "";
				rest.setHost(url.getScheme() + "://" + url.getAuthority());
				rest.setBasePath((url.getPath() != null ? url.getPath() : "") + modelled);
			}
		}
	}

	/** What this one replaces, when a deployment says it replaces something. */
	private static void supersede(ServiceImplementation implementation, Config config) {
		if (config.replaces_version() == null || config.replaces_version().isBlank()) {
			return;
		}
		ServiceImplementation previous = org.eclipse.fennec.services.ServicesFactory.eINSTANCE
				.createServiceImplementation();
		previous.setName(implementation.getName());
		previous.setVersion(config.replaces_version());
		implementation.setReplaces(previous);
		implementation.setUpdatePolicy(UpdatePolicy.get(config.update_policy()) == null
				? UpdatePolicy.UNSPECIFIED
				: UpdatePolicy.get(config.update_policy()));
	}

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
		Resource document = resourceSet.getResource(
				org.eclipse.emf.common.util.URI.createURI(entry.toString()), true);
		ServiceProvider provider = (ServiceProvider) document.getContents().get(0);
		for (ServiceImplementation candidate : provider.getImplementations()) {
			if ("directory-java-rest".equals(candidate.getName())) {
				return candidate;
			}
		}
		throw new IllegalStateException(config.model_entry() + " holds no implementation named directory-java-rest");
	}
}