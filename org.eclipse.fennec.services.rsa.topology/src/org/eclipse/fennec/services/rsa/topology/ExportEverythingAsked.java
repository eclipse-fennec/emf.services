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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
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

	/**
	 * Every admin in the framework.
	 *
	 * <p>There is one per configuration type, and they come and go with
	 * the transports they are for — so this is a whiteboard, not a
	 * setting: dynamic, multiple, and bound through methods so the
	 * arrival of one is something this component is told about rather
	 * than something it reads out of a field by luck.
	 */
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addAdmin(RemoteServiceAdmin admin) {
		admins.add(admin);
		exportThrough(admin);
	}

	void removeAdmin(RemoteServiceAdmin admin) {
		admins.remove(admin);
	}

	private final List<RemoteServiceAdmin> admins = new CopyOnWriteArrayList<>();

	/**
	 * Everything that asked to be exported, with whatever came of it.
	 *
	 * <p>A service with an empty collection is one no admin has taken
	 * yet, and it stays here on purpose: it is what {@link #addAdmin}
	 * comes back to.
	 */
	private final Map<ServiceReference<Object>, Collection<ExportRegistration>> exported = new ConcurrentHashMap<>();

	private ServiceTracker<Object, Collection<ExportRegistration>> tracker;

	private BundleContext context;

	@Activate
	void activate(BundleContext context, TopologyPolicy policy) throws InvalidSyntaxException {
		this.context = context;
		apply(policy);
	}

	/**
	 * The policy changed — or, far more often, the same configuration was
	 * delivered a second time while the framework started. Taking it
	 * here rather than letting the component be destroyed and rebuilt is
	 * what keeps an export that is running at that moment from finding
	 * its provider dead (#107).
	 */
	@Modified
	void modified(TopologyPolicy policy) throws InvalidSyntaxException {
		stop();
		apply(policy);
	}

	private void apply(TopologyPolicy policy) throws InvalidSyntaxException {
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
		stop();
	}

	/** Stop exporting on our own and take back what we exported. */
	private void stop() {
		if (tracker != null) {
			tracker.close();
			tracker = null;
		}
		exported.values().forEach(ExportEverythingAsked::close);
		exported.clear();
	}

	@Override
	public Collection<ExportRegistration> addingService(ServiceReference<Object> reference) {
		Collection<ExportRegistration> registrations = new CopyOnWriteArrayList<>();
		exported.put(reference, registrations);
		for (RemoteServiceAdmin admin : admins) {
			// Every admin is asked: each serves one configuration type,
			// and a service that names none should be exported by all of
			// them - which is what a promiscuous topology manager means.
			exportThrough(admin, reference, registrations);
		}
		if (registrations.isEmpty()) {
			// No admin speaks the configuration type this service asked
			// for. It stays tracked all the same, because an admin is a
			// component like any other and may simply not be up yet; when
			// one arrives, addAdmin comes back to it.
			LOG.info("[DDSR] nothing exports " + reference + " yet");
		} else {
			LOG.info("[DDSR] exported " + reference);
		}
		return registrations;
	}

	/**
	 * Offers everything that asked to be exported to an admin that just
	 * arrived.
	 *
	 * <p>Without this, an admin is only ever asked about services that
	 * register after it. That is the wrong way round: a service says it
	 * wants to be exported and then waits, while the transports and the
	 * admin above them take their time coming up. The example provider
	 * exported nothing at all for exactly this reason, and the TCK never
	 * showed it because the TCK exports by hand.
	 */
	private void exportThrough(RemoteServiceAdmin admin) {
		exported.forEach((reference, registrations) -> exportThrough(admin, reference, registrations));
	}

	private void exportThrough(RemoteServiceAdmin admin, ServiceReference<Object> reference,
			Collection<ExportRegistration> registrations) {
		try {
			registrations.addAll(admin.exportService(reference, null));
		} catch (RuntimeException failure) {
			// One service that cannot be exported must not stop the
			// others, and the reason has to be visible.
			LOG.log(Level.WARNING, "[DDSR] exporting " + reference + " failed", failure);
		}
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, Collection<ExportRegistration> registrations) {
		// Properties changed. Re-exporting would mean closing and opening
		// the endpoint, which a consumer would see as the service going
		// away and coming back. Saying the endpoint anew is what
		// ExportRegistration.update is for (#99), and the transport stays
		// up while it happens.
		for (ExportRegistration registration : registrations) {
			try {
				registration.update(null);
			} catch (RuntimeException failure) {
				LOG.log(Level.WARNING, "[DDSR] updating the export of " + reference + " failed", failure);
			}
		}
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
