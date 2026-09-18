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
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

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

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

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
		ServiceRegistration<Application> registration = context.registerService(Application.class,
				new ServedApplication(dispatcher, resourceSets), properties);
		LOG.info("[DDSR] serving " + (name == null ? base : name) + " at " + base);

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
