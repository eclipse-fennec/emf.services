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

package org.eclipse.fennec.services.rsa.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.client.ServiceProxyFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * A remote service, registered here as if it were local.
 *
 * <p>Registered as a {@link ServiceFactory} rather than as one proxy
 * object, and that is the whole trick: the interface has to be loaded
 * by the bundle that wants the service, because only that bundle is
 * wired to the package the interface lives in. So each consumer gets a
 * proxy built with its own class loader — the pattern Aries RSA uses
 * for the same reason.
 *
 * <p>Both the registration the importer holds and the reference anyone
 * else reads: two views of one thing, kept as one object so there is
 * nothing to keep in step.
 */
final class ImportedService implements ImportRegistration, ImportReference, ServiceFactory<Object> {

	private static final Logger LOG = Logger.getLogger(ImportedService.class.getName());

	private final ServiceLocator locator;
	private final ServiceProxyFactory proxies;
	private final Consumer<ImportedService> onUpdate;
	private final Consumer<ImportedService> forget;
	private final AtomicBoolean open = new AtomicBoolean(true);

	private volatile EndpointDescription endpoint;
	private volatile ServiceRegistration<?> registration;

	/**
	 * How this import lets go of the reference it uses.
	 *
	 * <p>An import is the Remote Service Admin way of saying "this
	 * service is in use here", and the registry's way of saying the same
	 * is a session acquisition. The two have to end together: a
	 * predecessor draining towards its successor waits for the last
	 * consumer to let go, and an import that closed without letting go
	 * keeps it waiting for somebody who has already gone (#24).
	 */
	private final Consumer<String> release;

	ImportedService(EndpointDescription endpoint, ServiceLocator locator, ServiceProxyFactory proxies,
			Consumer<ImportedService> onUpdate, Consumer<ImportedService> forget, Consumer<String> release) {
		this.endpoint = endpoint;
		this.locator = locator;
		this.proxies = proxies;
		this.onUpdate = onUpdate;
		this.forget = forget;
		this.release = release;
	}

	/** The reference this import uses, or {@code null} if it has none. */
	private String referenceId() {
		return locator == null || locator.reference() == null ? null : locator.reference().getId();
	}

	/**
	 * Register in the local framework under the endpoint's interfaces,
	 * with its properties — minus the export instructions, which would
	 * ask the importer to export it right back, and plus
	 * {@code service.imported}, which is how a consumer tells a remote
	 * service from a local one when it cares.
	 */
	void register(BundleContext context) {
		List<String> interfaces = endpoint.getInterfaces();
		registration = context.registerService(interfaces.toArray(String[]::new), this, propertiesOf(endpoint));
	}

	private static Dictionary<String, Object> propertiesOf(EndpointDescription endpoint) {
		Dictionary<String, Object> properties = new Hashtable<>();
		for (Map.Entry<String, Object> property : endpoint.getProperties().entrySet()) {
			if (!property.getKey().startsWith("service.exported.")
					&& !property.getKey().equals("service.id")
					&& !property.getKey().equals("objectClass")) {
				properties.put(property.getKey(), property.getValue());
			}
		}
		properties.put(RemoteConstants.SERVICE_IMPORTED, Boolean.TRUE);
		return properties;
	}

	/** The description, whether or not the registration is still open. */
	EndpointDescription description() {
		return endpoint;
	}

	@Override
	public Object getService(Bundle requesting, ServiceRegistration<Object> ignored) {
		List<String> interfaces = endpoint.getInterfaces();
		if (interfaces.size() != 1) {
			// One contract, one interface — that is what the export side
			// produces (#102), and a proxy over several would need a
			// contract for each. Refuse rather than pick.
			LOG.warning("[DDSR] " + endpoint.getId() + " names " + interfaces.size()
					+ " interfaces; an import covers exactly one");
			return null;
		}
		try {
			ClassLoader loader = requesting.adapt(BundleWiring.class).getClassLoader();
			Class<?> contract = loader.loadClass(interfaces.get(0));
			Object proxy = proxies.newProxy(contract, locator);
			LOG.fine(() -> "[DDSR] " + requesting.getSymbolicName() + " got a proxy for " + endpoint.getId());
			return proxy;
		} catch (ClassNotFoundException notVisible) {
			// The bundle asked for a service whose interface it cannot
			// see — a wiring problem in that bundle, not here. Say so
			// with the name, and let the framework treat it as absent.
			LOG.warning("[DDSR] " + requesting.getSymbolicName() + " cannot load " + interfaces.get(0)
					+ ", which it asked for as an imported service");
			return null;
		} catch (RuntimeException | LinkageError failure) {
			// A factory that throws leaves the component runtime to
			// explain, and it explains through a log this deployment may
			// not show. Say it here, where it is seen.
			LOG.log(Level.WARNING, "[DDSR] building a proxy for " + endpoint.getId() + " on behalf of "
					+ requesting.getSymbolicName() + " failed", failure);
			return null;
		}
	}

	@Override
	public void ungetService(Bundle requesting, ServiceRegistration<Object> ignored, Object proxy) {
		// A proxy holds nothing that has to be released.
	}

	@Override
	public ImportReference getImportReference() {
		return open.get() ? this : null;
	}

	@Override
	public ServiceReference<?> getImportedService() {
		ServiceRegistration<?> live = registration;
		return open.get() && live != null ? live.getReference() : null;
	}

	@Override
	public EndpointDescription getImportedEndpoint() {
		return open.get() ? endpoint : null;
	}

	/**
	 * The endpoint said something new about itself: the proxy's
	 * properties follow. Only for the same endpoint — a description of a
	 * different one is not an update but a mistake, and {@code false} is
	 * the specification's word for "did not happen".
	 */
	@Override
	public boolean update(EndpointDescription changed) {
		ServiceRegistration<?> live = registration;
		if (!open.get() || live == null) {
			return false;
		}
		if (!endpoint.isSameService(changed)) {
			LOG.warning("[DDSR] " + changed.getId() + " is not an update of " + endpoint.getId() + " — ignored");
			return false;
		}
		endpoint = changed;
		try {
			live.setProperties(propertiesOf(changed));
		} catch (IllegalStateException alreadyGone) {
			LOG.log(Level.FINE, "[DDSR] " + endpoint.getId() + " was unregistered while being updated", alreadyGone);
			return false;
		}
		onUpdate.accept(this);
		return true;
	}

	@Override
	public void close() {
		if (!open.compareAndSet(true, false)) {
			return;
		}
		try {
			ServiceRegistration<?> live = registration;
			if (live != null) {
				live.unregister();
			}
		} catch (IllegalStateException alreadyGone) {
			LOG.log(Level.FINE, "[DDSR] " + endpoint.getId() + " was already unregistered", alreadyGone);
		} finally {
			String reference = referenceId();
			if (reference != null && release != null) {
				release.accept(reference);
				LOG.fine(() -> "[DDSR] released " + reference + " with the import of " + endpoint.getId());
			}
			forget.accept(this);
		}
	}

	@Override
	public Throwable getException() {
		// An import that failed never becomes a registration here; the
		// admin answers null instead. So there is never one to report.
		return null;
	}
}
