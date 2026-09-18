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

package org.eclipse.fennec.services.rsa.topology;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Exports every service that asked to be exported.
 *
 * <p>The simplest policy there is, and the one the specification
 * describes: a service says {@code service.exported.interfaces}, and it
 * is exported. Remote Service Admin itself has no opinion about which
 * services those are — that is deliberate, and this is the thing that
 * has the opinion.
 *
 * <p>A deployment that wants another one writes another component:
 * export only what a filter matches, export nothing, export into a
 * particular scope. That is why this is separate and why it is small.
 */
@Designate(ocd = TopologyPolicy.class)
@Component(immediate = true, configurationPid = TopologyPolicy.PID,
		configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class ExportEverythingAsked implements ServiceTrackerCustomizer<Object, Collection<ExportRegistration>> {

	private static final Logger LOG = Logger.getLogger(ExportEverythingAsked.class.getName());

	@Reference
	private RemoteServiceAdmin rsa;

	private final Map<ServiceReference<?>, Collection<ExportRegistration>> exported = new ConcurrentHashMap<>();

	private ServiceTracker<Object, Collection<ExportRegistration>> tracker;

	@Activate
	void activate(BundleContext context, TopologyPolicy policy) throws InvalidSyntaxException {
		LOG.info("[DDSR] topology policy is " + policy.policy());
		if (TopologyPolicy.MANUAL.equals(policy.policy())) {
			return;
		}
		tracker = new ServiceTracker<>(context,
				context.createFilter("(" + RemoteConstants.SERVICE_EXPORTED_INTERFACES + "=*)"), this);
		tracker.open();
	}

	@Deactivate
	void deactivate() {
		if (tracker != null) {
			tracker.close();
			tracker = null;
		}
		exported.values().forEach(ExportEverythingAsked::close);
		exported.clear();
	}

	@Override
	public Collection<ExportRegistration> addingService(ServiceReference<Object> reference) {
		try {
			Collection<ExportRegistration> registrations = rsa.exportService(reference, null);
			if (registrations.isEmpty()) {
				// Not ours: no distribution answers the configuration type
				// it asked for. Another topology manager or another admin
				// may well take it.
				return null;
			}
			exported.put(reference, registrations);
			LOG.info("[DDSR] exported " + reference);
			return registrations;
		} catch (RuntimeException failure) {
			// One service that cannot be exported must not stop the
			// others, and the reason has to be visible.
			LOG.log(Level.WARNING, "[DDSR] exporting " + reference + " failed", failure);
			return null;
		}
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, Collection<ExportRegistration> registrations) {
		// Properties changed. Re-exporting would mean closing and opening
		// the endpoint, which a consumer would see as the service going
		// away and coming back. Updating in place is what
		// ExportRegistration.update is for, and it is not implemented yet
		// (#24) — so this deliberately does nothing rather than churn.
	}

	@Override
	public void removedService(ServiceReference<Object> reference, Collection<ExportRegistration> registrations) {
		exported.remove(reference);
		close(registrations);
		LOG.info("[DDSR] withdrew " + reference);
	}

	private static void close(Collection<ExportRegistration> registrations) {
		registrations.forEach(ExportRegistration::close);
	}
}
