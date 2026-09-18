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

import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
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
				description = "Name of the contract this configuration serves. Both references are targeted at "
						+ "it: set model.target and service.target to (ddsr.contract=<name>).")
		String ddsr_contract() default "";

		@AttributeDefinition(
				name = "Application base",
				description = "Where the Jakarta REST whiteboard mounts this application. Leave empty to take "
						+ "the flavor's basePath, which is the usual case.")
		String osgi_jakartars_application_base() default "";
	}

	/**
	 * The service that implements the contract. Through
	 * {@link ComponentServiceObjects} so a PROTOTYPE-scoped implementation
	 * gets an instance per call rather than one shared across consumers.
	 */
	@Reference(name = "service")
	private ComponentServiceObjects<Object> serviceObjects;

	/**
	 * The implementation model, as a service rather than as a URI in the
	 * configuration: it lives in the provider's bundle, and the provider
	 * already has it in hand — it loads the same document to publish it.
	 * Handing it over as a service keeps this component free of any idea
	 * about where a model file might be.
	 */
	@Reference(name = "model")
	private ServiceImplementation implementation;

	private RestDispatcher dispatcher;

	@Activate
	void activate(Config config) {
		RestFlavor flavor = restFlavorOf(implementation);
		this.dispatcher = new RestDispatcher(flavor, serviceObjects);
		LOG.info("[DDSR] serving " + implementation.getServiceInterfaces().get(0).getName()
				+ " generically at " + flavor.getBasePath());
	}

	@Override
	public Set<Object> getSingletons() {
		return dispatcher != null ? Set.of(dispatcher) : Set.of();
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
