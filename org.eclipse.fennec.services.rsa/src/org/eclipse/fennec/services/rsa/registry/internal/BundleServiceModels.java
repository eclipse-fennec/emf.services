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

package org.eclipse.fennec.services.rsa.registry.internal;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectProvider;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.rsa.registry.ServiceModelCapability;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * The models that bundles bring with them.
 *
 * <p>A provider that wrote its own contract ships the document and says
 * so with a capability. Nothing else is asked of it: no configuration
 * naming the file, no component to register it, no code at all. The
 * bundle is the only place that knows where its model is, so it is the
 * one that says.
 *
 * <p>Bundles come and go, so this keeps watching after the initial load
 * — a provider installed later is no different from one installed at
 * start, and pretending otherwise would make deployment order matter.
 */
@Component(service = EObjectProvider.class, property = "emf.eobject.provider.name=ddsr.bundle.models")
public class BundleServiceModels implements EObjectProvider, BundleTrackerCustomizer<Bundle> {

	private static final Logger LOG = Logger.getLogger(BundleServiceModels.class.getName());

	/** What this provider calls itself when it writes into the registry. */
	static final String SOURCE = "bundle";

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	private BundleContext context;
	private volatile EObjectRegistryWriter writer;
	private BundleTracker<Bundle> tracker;

	@Activate
	void activate(BundleContext context) {
		this.context = context;
	}

	@Deactivate
	void deactivate() {
		if (tracker != null) {
			tracker.close();
			tracker = null;
		}
	}

	@Override
	public CompletableFuture<Void> load(EObjectRegistryWriter writer) {
		this.writer = writer;
		// The tracker reports every bundle that is already there before
		// it returns, so the load is complete when it opens.
		tracker = new BundleTracker<>(context, Bundle.ACTIVE | Bundle.RESOLVED | Bundle.STARTING, this);
		tracker.open();
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public Bundle addingBundle(Bundle bundle, BundleEvent event) {
		boolean carriedSomething = false;
		for (BundleCapability capability : capabilities(bundle)) {
			carriedSomething |= read(bundle, capability);
		}
		return carriedSomething ? bundle : null;
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle tracked) {
		// Nothing to do: a model is what the bundle contains, and that
		// does not change while it is installed.
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Bundle tracked) {
		for (BundleCapability capability : capabilities(bundle)) {
			String name = name(capability);
			if (name != null && writer != null) {
				writer.remove(SOURCE, name);
				LOG.fine(() -> "[DDSR] " + name + " left with " + bundle.getSymbolicName());
			}
		}
	}

	private Iterable<BundleCapability> capabilities(Bundle bundle) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		return wiring == null ? List.of() : wiring.getCapabilities(ServiceModelCapability.NAMESPACE);
	}

	private static String name(BundleCapability capability) {
		Object stated = capability.getAttributes().get(ServiceModelCapability.NAMESPACE);
		return stated == null ? null : stated.toString();
	}

	/** @return whether the bundle actually yielded a contract */
	private boolean read(Bundle bundle, BundleCapability capability) {
		String name = name(capability);
		Object path = capability.getAttributes().get(ServiceModelCapability.PATH);
		if (name == null || path == null) {
			LOG.warning("[DDSR] " + bundle.getSymbolicName() + " declares a service model without "
					+ (name == null ? "a name" : "a path") + " — ignored");
			return false;
		}
		URL entry = bundle.getEntry(path.toString());
		if (entry == null) {
			LOG.warning("[DDSR] " + bundle.getSymbolicName() + " points at " + path + ", which it does not contain");
			return false;
		}

		ResourceSet resourceSet = resourceSets.getService();
		try {
			Resource document = resourceSet.getResource(
					URI.createURI(entry.toString()), true);
			for (EObject root : document.getContents()) {
				if (root instanceof ServiceInterface contract && name.equals(contract.getName())) {
					writer.put(SOURCE, name, contract,
							Map.of("bundle", bundle.getSymbolicName(), "path", path.toString()));
					LOG.info("[DDSR] " + name + " read from " + bundle.getSymbolicName() + "/" + path);
					return true;
				}
			}
			LOG.warning("[DDSR] " + bundle.getSymbolicName() + "/" + path
					+ " holds no ServiceInterface named " + name);
			return false;
		} catch (RuntimeException unreadable) {
			LOG.log(Level.WARNING, "[DDSR] could not read " + bundle.getSymbolicName() + "/" + path, unreadable);
			return false;
		} finally {
			resourceSets.ungetService(resourceSet);
		}
	}
}
