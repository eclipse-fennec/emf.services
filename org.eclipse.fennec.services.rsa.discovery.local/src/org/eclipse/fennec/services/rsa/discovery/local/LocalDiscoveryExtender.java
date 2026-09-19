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

package org.eclipse.fennec.services.rsa.discovery.local;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.rsa.spi.EndpointScopes;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Endpoints a bundle brings with it.
 *
 * <p>122.6.2 lets a bundle carry the descriptions of endpoints it knows
 * about, as XML files named by its {@code Remote-Service} header. That
 * is discovery without a network: the answer was written down before the
 * question was asked, which is exactly what a closed deployment, a test
 * fixture or a fixed gateway wants.
 *
 * <p>This is an extender and says so in its manifest. What it does is
 * the same as any other discovery: tell the endpoint listeners here, and
 * tell them again when the bundle goes.
 */
@SuppressWarnings("deprecation")
@Component(service = {}, immediate = true)
public class LocalDiscoveryExtender implements BundleTrackerCustomizer<List<EndpointDescription>> {

	/** The header that names the description files. */
	static final String REMOTE_SERVICE_HEADER = "Remote-Service";

	private static final Logger LOG = Logger.getLogger(LocalDiscoveryExtender.class.getName());

	private final Map<EndpointEventListener, List<String>> eventListeners = new ConcurrentHashMap<>();

	private final Map<EndpointListener, List<String>> legacyListeners = new ConcurrentHashMap<>();

	/** What each bundle told us, so it can be taken back when it goes. */
	private final Map<Bundle, List<EndpointDescription>> described = new ConcurrentHashMap<>();

	private BundleTracker<List<EndpointDescription>> tracker;

	private Object componentId;

	// Method injection with the properties: the scope a listener asked
	// for is half the news, and it changes as listeners come and go.
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addEventListener(EndpointEventListener listener, Map<String, Object> properties) {
		if (someoneElse(properties)) {
			List<String> scope = EndpointScopes.of(properties);
			eventListeners.put(listener, scope);
			replay(listener, scope);
		}
	}

	void removeEventListener(EndpointEventListener listener) {
		eventListeners.remove(listener);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addLegacyListener(EndpointListener listener, Map<String, Object> properties) {
		if (someoneElse(properties)) {
			List<String> scope = EndpointScopes.of(properties);
			legacyListeners.put(listener, scope);
			replay(listener, scope);
		}
	}

	void removeLegacyListener(EndpointListener listener) {
		legacyListeners.remove(listener);
	}

	private boolean someoneElse(Map<String, Object> properties) {
		Object id = properties.get("component.id");
		return id == null || !id.equals(componentId);
	}

	@Activate
	void activate(ComponentContext context, BundleContext bundleContext) {
		this.componentId = context.getProperties().get("component.id");
		// Active bundles only: a description is a claim that something is
		// reachable, and a bundle that has not started is not making it.
		tracker = new BundleTracker<>(bundleContext, Bundle.ACTIVE, this);
		tracker.open();
		LOG.info("[DDSR] local discovery ready — reading " + REMOTE_SERVICE_HEADER + " headers");
	}

	@Deactivate
	void deactivate() {
		if (tracker != null) {
			tracker.close();
			tracker = null;
		}
		described.clear();
	}

	@Override
	public List<EndpointDescription> addingBundle(Bundle bundle, BundleEvent event) {
		String header = bundle.getHeaders().get(REMOTE_SERVICE_HEADER);
		if (header == null || header.isBlank()) {
			return null;
		}
		List<EndpointDescription> endpoints = new ArrayList<>();
		for (URL document : documentsOf(bundle, header)) {
			try (InputStream in = document.openStream()) {
				endpoints.addAll(EndpointDescriptions.read(in));
			} catch (IOException unreadable) {
				// One unreadable file must not hide the others, and the
				// bundle that carries it has to be named.
				LOG.log(Level.WARNING, "[DDSR] " + bundle.getSymbolicName() + " carries an endpoint description "
						+ "that cannot be read: " + document, unreadable);
			}
		}
		if (endpoints.isEmpty()) {
			return null;
		}
		described.put(bundle, endpoints);
		LOG.info("[DDSR] " + bundle.getSymbolicName() + " describes " + endpoints.size() + " endpoint(s)");
		for (EndpointDescription endpoint : endpoints) {
			deliver(new EndpointEvent(EndpointEvent.ADDED, endpoint));
		}
		return endpoints;
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, List<EndpointDescription> endpoints) {
		// The header cannot change without the bundle being updated, and
		// an update takes it out of ACTIVE first — so there is nothing to
		// do here that removedBundle and addingBundle do not already do.
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, List<EndpointDescription> endpoints) {
		described.remove(bundle);
		for (EndpointDescription endpoint : endpoints) {
			deliver(new EndpointEvent(EndpointEvent.REMOVED, endpoint));
		}
		LOG.info("[DDSR] " + bundle.getSymbolicName() + " went, and with it " + endpoints.size() + " endpoint(s)");
	}

	/**
	 * The files a {@code Remote-Service} header names. An entry is a file
	 * or a directory; a directory means every XML file directly in it,
	 * which is what the specification says and what lets a bundle add one
	 * without touching its manifest.
	 */
	private static List<URL> documentsOf(Bundle bundle, String header) {
		List<URL> documents = new ArrayList<>();
		for (String entry : header.split(",")) {
			String path = entry.trim();
			if (path.isEmpty()) {
				continue;
			}
			if (path.endsWith("/")) {
				Enumeration<URL> found = bundle.findEntries(path, "*.xml", false);
				while (found != null && found.hasMoreElements()) {
					documents.add(found.nextElement());
				}
				continue;
			}
			URL document = bundle.getEntry(path);
			if (document != null) {
				documents.add(document);
			} else {
				LOG.warning("[DDSR] " + bundle.getSymbolicName() + " names " + path + " in its "
						+ REMOTE_SERVICE_HEADER + " header, and there is no such entry");
			}
		}
		return documents;
	}

	private void deliver(EndpointEvent event) {
		EndpointDescription endpoint = event.getEndpoint();
		eventListeners.forEach((listener, scope) -> {
			String matched = EndpointScopes.matching(scope, endpoint);
			if (matched != null) {
				safely(() -> listener.endpointChanged(event, matched), listener);
			}
		});
		legacyListeners.forEach((listener, scope) -> {
			String matched = EndpointScopes.matching(scope, endpoint);
			if (matched == null) {
				return;
			}
			safely(() -> {
				if (event.getType() == EndpointEvent.REMOVED) {
					listener.endpointRemoved(endpoint, matched);
				} else {
					listener.endpointAdded(endpoint, matched);
				}
			}, listener);
		});
	}

	/** Everything already read, for a listener that just showed up. */
	private void replay(Object listener, List<String> scope) {
		for (List<EndpointDescription> endpoints : described.values()) {
			for (EndpointDescription endpoint : endpoints) {
				String matched = EndpointScopes.matching(scope, endpoint);
				if (matched == null) {
					continue;
				}
				EndpointEvent event = new EndpointEvent(EndpointEvent.ADDED, endpoint);
				if (listener instanceof EndpointEventListener events) {
					safely(() -> events.endpointChanged(event, matched), listener);
				} else if (listener instanceof EndpointListener legacy) {
					safely(() -> legacy.endpointAdded(endpoint, matched), listener);
				}
			}
		}
	}

	private static void safely(Runnable delivery, Object listener) {
		try {
			delivery.run();
		} catch (RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] " + listener.getClass().getName() + " threw on an endpoint event",
					failure);
		}
	}
}
