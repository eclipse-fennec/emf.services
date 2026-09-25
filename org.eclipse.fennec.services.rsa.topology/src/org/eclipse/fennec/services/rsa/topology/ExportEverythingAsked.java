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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 *
 * <p>Every export happens on this component's own thread, never on the
 * thread that told it something changed (#164). That thread belongs to
 * whoever registered the service or the admin, and an export may have to
 * wait for exactly that someone: the REST distribution waits until the
 * Jakarta REST whiteboard has deployed what it registered, and the
 * whiteboard deploys on the thread that also registers its runtime —
 * which is the thread an admin arrives on, through the chain of
 * components that runtime satisfies. Exporting there waited for itself
 * until it timed out. One thread, too, and not a pool: the claims below
 * stay simple when only one thread ever reads and writes them.
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
	void addAdmin(RemoteServiceAdmin admin, Map<String, Object> properties) {
		admins.add(admin);
		Object supported = properties.get("remote.configs.supported");
		spokenByAdmin.put(admin, supported == null ? admin.getClass().getSimpleName()
				: String.valueOf(supported));
		exporter.execute(() -> exportThrough(admin));
	}

	void removeAdmin(RemoteServiceAdmin admin) {
		admins.remove(admin);
		spokenByAdmin.remove(admin);
	}

	/**
	 * What each admin says it speaks, kept for the one message that
	 * needs it: a service nobody exported, and why.
	 */
	private final Map<RemoteServiceAdmin, String> spokenByAdmin = new ConcurrentHashMap<>();

	private final List<RemoteServiceAdmin> admins = new CopyOnWriteArrayList<>();

	/**
	 * Everything that asked to be exported, with whatever came of it.
	 *
	 * <p>A service with an empty collection is one no admin has taken
	 * yet, and it stays here on purpose: it is what {@link #addAdmin}
	 * comes back to. Only exports that worked are in a collection — a
	 * failed one is not something an admin took.
	 */
	private final Map<ServiceReference<Object>, Collection<ExportRegistration>> exported = new ConcurrentHashMap<>();

	/**
	 * Which admin has already been asked about which service.
	 *
	 * <p>Two events reach the export path routinely: a service
	 * registering, and an admin arriving and being offered everything
	 * that was waiting. Each sees what the other left, so asking without
	 * the claim means exporting twice. Read and written on
	 * {@link #exporter} only, which is why it needs no locking.
	 */
	private final Set<Asked> alreadyAsked = new HashSet<>();

	/** One admin's question about one service. */
	private record Asked(RemoteServiceAdmin admin, ServiceReference<Object> reference) {
	}

	/** The one thread every export, update and claim runs on. */
	private final ExecutorService exporter = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "fennec-rsa-topology-export");
		thread.setDaemon(true);
		return thread;
	});

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
		// Not waited for: an export still running finishes on its own,
		// finds its service no longer tracked and closes what it got.
		exporter.shutdown();
	}

	/** Stop exporting on our own and take back what we exported. */
	private void stop() {
		if (tracker != null) {
			tracker.close();
			tracker = null;
		}
		exported.values().forEach(ExportEverythingAsked::close);
		exported.clear();
		exporter.execute(alreadyAsked::clear);
	}

	/**
	 * Waits until everything handed to the export thread so far is done.
	 * For tests, which otherwise could not tell "not exported" from "not
	 * exported yet".
	 */
	void settle() {
		CompletableFuture.runAsync(() -> {
		}, exporter).join();
	}

	@Override
	public Collection<ExportRegistration> addingService(ServiceReference<Object> reference) {
		Collection<ExportRegistration> registrations = new CopyOnWriteArrayList<>();
		exported.put(reference, registrations);
		exporter.execute(() -> exportEverywhere(reference, registrations));
		return registrations;
	}

	private void exportEverywhere(ServiceReference<Object> reference, Collection<ExportRegistration> registrations) {
		boolean failed = false;
		for (RemoteServiceAdmin admin : admins) {
			// Every admin is asked: each serves one configuration type,
			// and a service that names none should be exported by all of
			// them - which is what a promiscuous topology manager means.
			failed |= !exportThrough(admin, reference, registrations);
		}
		if (!stillTracked(reference, registrations)) {
			return;
		}
		if (!registrations.isEmpty()) {
			LOG.info("[DDSR] exported " + reference);
		} else if (failed) {
			// The admin already said why. What is left to say is that
			// this is not the end: the service is asked about again when
			// it changes, and another admin arriving is asked as well.
			LOG.info("[DDSR] nothing exports " + reference + " — the export failed, and it waits for a change");
		} else {
			// No admin speaks the configuration type this service asked
			// for. It stays tracked all the same, because an admin is a
			// component like any other and may simply not be up yet; when
			// one arrives, addAdmin comes back to it.
			//
			// Both sides in the message, because the two ways this ends
			// badly look identical without them: an admin that is not up
			// yet (fine, it will be asked) and a service asking for a
			// type nobody here will ever speak (not fine, and invisible
			// until someone wonders why nothing happened).
			LOG.info("[DDSR] nothing exports " + reference + " yet — it asks for "
					+ asksFor(reference) + ", and " + spoken() + " is here");
		}
	}

	/** Which configuration types a service asks to be exported over. */
	private static String asksFor(ServiceReference<Object> reference) {
		Object configs = reference.getProperty(RemoteConstants.SERVICE_EXPORTED_CONFIGS);
		if (configs == null) {
			return "any type";
		}
		return configs instanceof Object[] several ? Arrays.toString(several) : String.valueOf(configs);
	}

	/** Which types the admins in this runtime actually speak. */
	private String spoken() {
		return spokenByAdmin.isEmpty() ? "no admin" : List.copyOf(spokenByAdmin.values()).toString();
	}

	/**
	 * Whether a service is still the one this component tracks under
	 * that collection. It is not when it went away, or when the policy
	 * was applied anew, while its export was waiting for this thread.
	 */
	private boolean stillTracked(ServiceReference<Object> reference, Collection<ExportRegistration> registrations) {
		return exported.get(reference) == registrations;
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
		if (!admins.contains(admin)) {
			return;
		}
		exported.forEach((reference, registrations) -> {
			boolean wasWaiting = registrations.isEmpty();
			exportThrough(admin, reference, registrations);
			if (wasWaiting && !registrations.isEmpty()) {
				// Said out loud, because the message before it was
				// "nothing exports this yet" and a reader is owed the
				// other half. Without it the only sign that the wait
				// ended is the absence of further complaints.
				LOG.info("[DDSR] exported " + reference + " — the admin it was waiting for is here");
			}
		});
	}

	/**
	 * Asks one admin to export one service, once.
	 *
	 * @return false if the export failed; true if it worked, and also if
	 *         the admin declined or had been asked already
	 */
	private boolean exportThrough(RemoteServiceAdmin admin, ServiceReference<Object> reference,
			Collection<ExportRegistration> registrations) {
		if (!stillTracked(reference, registrations)) {
			return true;
		}
		// One admin exports one service once. Without the claim, a service
		// registering while an admin arrives was exported twice through the
		// same admin — two endpoints and two announcements for one service
		// (#124).
		Asked asked = new Asked(admin, reference);
		if (!alreadyAsked.add(asked)) {
			return true;
		}
		Collection<ExportRegistration> answer;
		try {
			answer = admin.exportService(reference, null);
		} catch (RuntimeException failure) {
			// One service that cannot be exported must not stop the
			// others, and the reason has to be visible.
			LOG.log(Level.WARNING, "[DDSR] exporting " + reference + " failed", failure);
			return false;
		}
		boolean failed = false;
		for (ExportRegistration registration : answer) {
			if (registration.getException() == null) {
				registrations.add(registration);
			} else {
				// The admin reported it already; closing it is what
				// releases whatever it holds. It is not kept, because
				// what an admin failed to do is not something it took.
				registration.close();
				failed = true;
			}
		}
		// A failure keeps its claim, the way a declined configuration type
		// does. Giving it back would have the next event in the queue ask
		// again at once — a service registering while an admin arrives
		// then failed twice. What asks again is a change to the service,
		// which drops the claims of a service nothing exports, and that
		// is now reached: a failure is no longer in the collection, so it
		// no longer looks exported (#164).
		if (!stillTracked(reference, registrations)) {
			// The service left, or the policy was applied anew, while the
			// admin was exporting it. Nothing will close this later.
			close(registrations);
			registrations.clear();
			alreadyAsked.remove(asked);
		}
		return !failed;
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, Collection<ExportRegistration> registrations) {
		exporter.execute(() -> modified(reference, registrations));
	}

	private void modified(ServiceReference<Object> reference, Collection<ExportRegistration> registrations) {
		if (!stillTracked(reference, registrations)) {
			return;
		}
		if (registrations.isEmpty()) {
			// Nothing is exported yet, and the change may be exactly what
			// makes it exportable: a service that named a configuration
			// type nobody here spoke, reconfigured to one that is up.
			// Every admin was already asked once and said no — that
			// answer was about the old properties, so the claims go and
			// the question is put again.
			//
			// Found with #98: the example provider asks to be exported
			// over the transport its deployment names, which arrives as
			// a configuration after the service is registered. Without
			// this, it was never exported at all and nothing said why.
			alreadyAsked.removeIf(asked -> asked.reference().equals(reference));
			for (RemoteServiceAdmin admin : admins) {
				exportThrough(admin, reference, registrations);
			}
			if (!registrations.isEmpty()) {
				LOG.info("[DDSR] exported " + reference + " after it changed what it asks for");
			}
			return;
		}
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
		// Withdrawn here and now, not on the export thread: the service is
		// going away, and its endpoint must not outlive it. An export of
		// it still waiting there finds it untracked and closes its own.
		exported.remove(reference, registrations);
		close(registrations);
		exporter.execute(() -> alreadyAsked.removeIf(asked -> asked.reference().equals(reference)));
		LOG.info("[DDSR] withdrew " + reference);
	}

	private static void close(Collection<ExportRegistration> registrations) {
		registrations.forEach(ExportRegistration::close);
	}
}
