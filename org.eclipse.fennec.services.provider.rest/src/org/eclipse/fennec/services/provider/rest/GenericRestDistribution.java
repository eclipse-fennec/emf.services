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

import java.net.URL;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
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
import org.osgi.service.component.annotations.Reference;
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
				description = "Path of the document inside that bundle, e.g. model/binding-probe-impl.xmi. It "
						+ "holds the ServiceProvider whose implementation is served; the contract it references "
						+ "is read along with it.")
		String model_entry();

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

	private RestDispatcher dispatcher;

	@Activate
	void activate(BundleContext context, Config config) {
		ResourceSet resourceSet = resourceSets.getService();
		try {
			ServiceImplementation implementation = load(context, config, resourceSet);
			RestFlavor flavor = restFlavorOf(implementation);
			this.dispatcher = new RestDispatcher(flavor,
					() -> implementation(context, config.service_filter()),
					implementation.getServiceInterfaces().get(0).getName(),
					resourceSets);
			LOG.info("[DDSR] serving " + implementation.getServiceInterfaces().get(0).getName()
					+ " generically at " + flavor.getBasePath());
		} finally {
			resourceSets.ungetService(resourceSet);
		}
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
		Resource document = resourceSet.getResource(URI.createURI(entry.toString()), true);
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
