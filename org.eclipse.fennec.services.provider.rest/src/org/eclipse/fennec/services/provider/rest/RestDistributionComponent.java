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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyReader;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jakartars.runtime.JakartarsServiceRuntime;
import org.osgi.service.jakartars.runtime.dto.ApplicationDTO;
import org.osgi.service.jakartars.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jakartars.runtime.dto.RuntimeDTO;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;

import jakarta.ws.rs.core.Application;

/**
 * Mounts an endpoint for a flavor handed over at runtime.
 *
 * <p>One Jakarta REST application per served flavor, registered as a
 * service so the whiteboard deploys it — the same mechanism
 * {@link GenericRestDistribution} uses, only the model arrives through
 * the API instead of through a configuration.
 */
@Component(service = RestDistribution.class)
public class RestDistributionComponent implements RestDistribution {

	private static final Logger LOG = Logger.getLogger(RestDistributionComponent.class.getName());

	/** How long a whiteboard may take to deploy one application. */
	private static final long DEPLOY_TIMEOUT_MILLIS = 10_000;

	/**
	 * The ResourceSet for the services model — bound dynamically, and
	 * that is the point, not a style choice.
	 *
	 * <p>The ResourceSet service is registered first and only <em>gains</em>
	 * {@code emf.name=services} afterwards, through a property update as
	 * the model's configurator arrives. A static reference has to be
	 * woken by that modification to notice a service it did not match
	 * before, and this component — alone among the ones targeting this
	 * ResourceSet — has no configuration whose arrival would make the
	 * runtime look at it again. In roughly one child framework in ten of
	 * the RSA TCK it was never looked at again: the distribution simply
	 * never appeared, and nothing said why. A dynamic reference is
	 * tracked as services come, go and change, which is the path that
	 * does not depend on that wake-up. (#106)
	 */
	@Reference(target = "(emf.name=services)", policy = ReferencePolicy.DYNAMIC)
	void setResourceSets(ComponentServiceObjects<ResourceSet> resourceSets) {
		this.resourceSets = resourceSets;
	}

	void unsetResourceSets(ComponentServiceObjects<ResourceSet> resourceSets) {
		if (this.resourceSets == resourceSets) {
			this.resourceSets = null;
		}
	}

	private volatile ComponentServiceObjects<ResourceSet> resourceSets;

	/**
	 * The whiteboard this distribution mounts into.
	 *
	 * <p>Mandatory and static, and that is the readiness signal itself:
	 * this component exists only where something can actually serve, so
	 * the {@code RestDistribution} service exists only then, so the
	 * {@code FlavorDistribution} above it exists only then, and so does
	 * the remote service admin that is configured for it. A caller never
	 * meets an admin that cannot serve, and nothing has to wait on a
	 * timer to find that out (#114).
	 *
	 * <p>It is also the only way to learn whether an application was
	 * really deployed — registering one is a request, not an
	 * accomplishment — which is what {@link #awaitDeployed} asks it.
	 *
	 * <p>An earlier attempt at this turned "the whiteboard is late" into
	 * "the distribution never appears" in three of seven frameworks of
	 * the RSA TCK. That was not this reference's doing: the component
	 * below it held a static reference to a ResourceSet that gains its
	 * target property only later, and the child framework started our
	 * bundles before the one carrying their configuration. Both are
	 * fixed, and required is what this reference should have been.
	 *
	 * <p>A deployment with more than one whiteboard says which, the way
	 * DS lets any reference be pointed: {@code runtime.target}.
	 */
	@Reference(name = "runtime")
	private JakartarsServiceRuntime runtime;

	private BundleContext context;

	@Activate
	void activate(BundleContext context) {
		this.context = context;
	}

	@Override
	public Served serve(RestFlavor flavor, Object service, String name) {
		if (flavor == null || service == null) {
			throw new IllegalArgumentException("a flavor and the service behind it are both needed");
		}
		String base = flavor.getBasePath() == null || flavor.getBasePath().isBlank() ? "/" : flavor.getBasePath();

		RestDispatcher dispatcher = new RestDispatcher(flavor, () -> new SingleService(service),
				name == null ? base : name, resourceSets);

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("osgi.jakartars.application.base", base);
		properties.put("osgi.jakartars.name", name == null ? base : name);
		String applicationName = name == null ? base : name;
		ServiceRegistration<Application> registration = context.registerService(Application.class,
				new ServedApplication(dispatcher, resourceSets), properties);
		try {
			awaitDeployed(applicationName);
		} catch (RuntimeException notServed) {
			// Nothing is serving, so nothing may be announced: the whole
			// order this project keeps — serve, then tell the world —
			// rests on this call having actually happened.
			registration.unregister();
			throw notServed;
		}
		LOG.info("[DDSR] serving " + applicationName + " at " + base);

		return new Served() {

			private final AtomicBoolean open = new AtomicBoolean(true);

			@Override
			public String basePath() {
				return base;
			}

			@Override
			public void close() {
				if (open.compareAndSet(true, false)) {
					registration.unregister();
					LOG.info("[DDSR] stopped serving " + (name == null ? base : name));
				}
			}
		};
	}

	/**
	 * Wait until the whiteboard says the application is deployed.
	 *
	 * <p>Registering an {@code Application} service asks the whiteboard
	 * to deploy it; the deployment happens afterwards, on the
	 * whiteboard's own thread. Returning before it has is what made a
	 * consumer dial an address that answered 404 — the announce-before-
	 * mount race the harness worked around with a retry.
	 *
	 * <p>The runtime's DTO is the only honest answer to "is it up?", and
	 * it also says when the answer is no: a rejected application is in
	 * {@code failedApplicationDTOs} with a reason, and that reason is
	 * worth far more than a timeout.
	 */
	private void awaitDeployed(String applicationName) {
		long deadline = System.currentTimeMillis() + DEPLOY_TIMEOUT_MILLIS;
		while (true) {
			RuntimeDTO dto = runtime.getRuntimeDTO();
			for (ApplicationDTO application : dto.applicationDTOs) {
				if (applicationName.equals(application.name)) {
					return;
				}
			}
			for (FailedApplicationDTO failed : dto.failedApplicationDTOs) {
				if (applicationName.equals(failed.name)) {
					throw new IllegalStateException("the whiteboard refused the application " + applicationName
							+ ": failure reason " + failed.failureReason);
				}
			}
			if (System.currentTimeMillis() >= deadline) {
				throw new IllegalStateException("the whiteboard did not deploy the application "
						+ applicationName + " within " + DEPLOY_TIMEOUT_MILLIS / 1000 + " s");
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("interrupted while waiting for " + applicationName, interrupted);
			}
		}
	}

	/**
	 * The application the whiteboard deploys: the dispatcher plus the
	 * XMI codec, for the same reason {@link GenericRestDistribution}
	 * carries it — the codec's providers attach to the default
	 * application, not to this one.
	 */
	private static final class ServedApplication extends Application {

		private final RestDispatcher dispatcher;
		private final ComponentServiceObjects<ResourceSet> resourceSets;

		ServedApplication(RestDispatcher dispatcher, ComponentServiceObjects<ResourceSet> resourceSets) {
			this.dispatcher = dispatcher;
			this.resourceSets = resourceSets;
		}

		@Override
		public Set<Object> getSingletons() {
			return Set.of(dispatcher,
					new XmiMessageBodyReader(resourceSets),
					new XmiMessageBodyWriter(resourceSets));
		}
	}

	/**
	 * The service is known already, so there is nothing to look up. It
	 * still arrives through {@code ServiceObjects} because that is what
	 * the dispatcher asks for, and because a service that is handed over
	 * directly has no per-call instance to hand out.
	 */
	private static final class SingleService implements org.osgi.framework.ServiceObjects<Object> {

		private final Object service;

		SingleService(Object service) {
			this.service = service;
		}

		@Override
		public Object getService() {
			return service;
		}

		@Override
		public void ungetService(Object service) {
			// nothing was acquired
		}

		@Override
		public org.osgi.framework.ServiceReference<Object> getServiceReference() {
			throw new UnsupportedOperationException("this service was handed over directly, not looked up");
		}
	}
}
