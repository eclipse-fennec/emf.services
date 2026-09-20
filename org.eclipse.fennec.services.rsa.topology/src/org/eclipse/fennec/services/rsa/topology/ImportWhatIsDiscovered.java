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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Imports what a discovery reports.
 *
 * <p>The other importer here waits for someone to ask: it watches the
 * filters local bundles listen with and then goes looking. This one
 * works the other way round — a discovery says an endpoint exists, and a
 * promiscuous topology manager imports it whether or not anyone has
 * asked yet. That is what makes an endpoint described in a bundle's
 * header (122.6.2) or announced by another framework turn into a
 * service here.
 *
 * <p>Registered with a scope that matches every endpoint: which of them
 * are worth importing is the admin's judgement — it refuses the ones
 * whose configuration type nobody here speaks — not a filter's.
 */
@Component(service = EndpointEventListener.class, immediate = true, configurationPid = TopologyPolicy.PID,
		configurationPolicy = ConfigurationPolicy.OPTIONAL,
		property = EndpointEventListener.ENDPOINT_LISTENER_SCOPE + "=(endpoint.id=*)")
public class ImportWhatIsDiscovered implements EndpointEventListener {

	private static final Logger LOG = Logger.getLogger(ImportWhatIsDiscovered.class.getName());

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
		// An endpoint that arrived before any admin did is not gone, it is
		// waiting. Discovery reports an endpoint once; if nobody could take
		// it then, nobody ever hears of it again unless we come back to it.
		List.copyOf(waiting.values()).forEach(this::add);
	}

	void removeAdmin(RemoteServiceAdmin admin) {
		admins.remove(admin);
	}

	private final List<RemoteServiceAdmin> admins = new CopyOnWriteArrayList<>();

	/** What we imported, by the endpoint's id. */
	private final Map<String, ImportRegistration> imported = new ConcurrentHashMap<>();

	/** What was discovered while no admin could take it, by the same id. */
	private final Map<String, EndpointDescription> waiting = new ConcurrentHashMap<>();

	private volatile boolean manual;

	@Activate
	void activate(TopologyPolicy policy) {
		apply(policy);
	}

	/** A configuration update is taken, not died of (#107). */
	@Modified
	void modified(TopologyPolicy policy) {
		apply(policy);
	}

	private void apply(TopologyPolicy policy) {
		manual = TopologyPolicy.MANUAL.equals(policy.import_policy());
		LOG.info("[DDSR] discovered endpoints are imported: " + !manual);
	}

	@Override
	public void endpointChanged(EndpointEvent event, String matchedFilter) {
		EndpointDescription endpoint = event.getEndpoint();
		switch (event.getType()) {
		case EndpointEvent.ADDED -> add(endpoint);
		case EndpointEvent.MODIFIED -> {
			// The endpoint says something new about itself. Tell the
			// import, which re-registers the proxy's properties without
			// taking the service away from whoever is using it.
			ImportRegistration known = imported.get(endpoint.getId());
			if (known == null) {
				add(endpoint);
			} else if (!known.update(endpoint)) {
				LOG.fine(() -> "[DDSR] " + endpoint.getId() + " could not be updated in place — re-importing");
				remove(endpoint.getId());
				add(endpoint);
			}
		}
		case EndpointEvent.REMOVED, EndpointEvent.MODIFIED_ENDMATCH -> remove(endpoint.getId());
		default -> LOG.fine(() -> "[DDSR] endpoint event " + event.getType() + " needs no import");
		}
	}

	private void add(EndpointDescription endpoint) {
		String id = endpoint.getId();
		if (manual || imported.containsKey(id)) {
			return;
		}
		// Claim the endpoint before the slow part. Importing registers a
		// service and fires the whole component cascade, and two threads
		// reach here routinely — a discovery event and an admin arriving.
		// Both used to pass the check above, both imported, and the loser's
		// registration was dropped on the floor: a proxy in the framework
		// that nothing can ever close (#124).
		if (!importing.add(id)) {
			return;
		}
		try {
			// Whoever speaks this endpoint's configuration type takes it;
			// the others answer null, which is the specification's way of
			// saying "ask someone else".
			ImportRegistration registration = null;
			for (RemoteServiceAdmin admin : admins) {
				registration = admin.importService(endpoint);
				if (registration != null) {
					break;
				}
			}
			if (registration == null) {
				waiting.put(endpoint.getId(), endpoint);
				LOG.fine(() -> "[DDSR] no admin speaks " + endpoint.getId() + " yet; it waits for one");
				return;
			}
			waiting.remove(id);
			if (withdrawnWhileImporting.remove(id)) {
				// The endpoint went away while we were importing it.
				// Discovery reports a disappearance once, so nothing will
				// come back to clean this up: close it here or it stays.
				close(id, registration);
				LOG.fine(() -> "[DDSR] " + id + " was withdrawn while it was being imported");
				return;
			}
			imported.put(id, registration);
			LOG.info("[DDSR] imported the discovered endpoint " + id + " " + endpoint.getInterfaces());
		} catch (RuntimeException failure) {
			// A discovery is calling us and has nowhere to put an
			// exception; one endpoint that cannot be imported must not
			// stop the next.
			LOG.log(Level.WARNING, "[DDSR] importing the discovered endpoint " + endpoint.getId() + " failed",
					failure);
		} finally {
			importing.remove(id);
		}
	}

	/** Endpoint ids currently being imported, so a second thread does not repeat it. */
	private final Set<String> importing = ConcurrentHashMap.newKeySet();

	/** Ids whose removal arrived while the import was still in flight. */
	private final Set<String> withdrawnWhileImporting = ConcurrentHashMap.newKeySet();

	private static void close(String id, ImportRegistration registration) {
		try {
			registration.close();
		} catch (RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] closing the import of " + id + " failed", failure);
		}
	}

	private void remove(String endpointId) {
		waiting.remove(endpointId);
		if (importing.contains(endpointId)) {
			// An import is in flight for this id. Leaving a note is the
			// only way to reach it: imported.remove would find nothing and
			// the import would install a proxy for something already gone.
			withdrawnWhileImporting.add(endpointId);
		}
		ImportRegistration registration = imported.remove(endpointId);
		if (registration == null) {
			return;
		}
		try {
			registration.close();
			LOG.info("[DDSR] dropped the imported endpoint " + endpointId);
		} catch (RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] closing the import of " + endpointId + " failed", failure);
		}
	}

	@Deactivate
	void deactivate() {
		imported.keySet().forEach(this::remove);
		imported.clear();
		waiting.clear();
	}
}
