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

package org.eclipse.fennec.services.broker.core.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.RegistryKind;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;

/**
 * What every part of the broker shares: the registry model, the lock
 * that guards it, the file it is saved to, and the handles that only
 * exist at runtime.
 *
 * <p>This is deliberately not one of the seven concerns. The concerns
 * are behaviours over one registry, not seven separate stores, and
 * pretending otherwise would mean seven copies of the same locking and
 * saving. Naming the shared part instead makes each behaviour's use of
 * it visible: a component that only reads takes the read lock and never
 * calls {@link #persist()}.
 */
final class BrokerState {

	private static final Logger LOG = Logger.getLogger(BrokerState.class.getName());

	private static final String DEFAULT_SNAPSHOT_PATH = "./broker-state.xmi";

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private final ResourceSet resourceSet;

	private final Resource resource;

	private final Path snapshotPath;

	/**
	 * Serializes {@link #persist()} against itself: a snapshot runs under
	 * the <em>read</em> lock, so two concurrent snapshots (or a snapshot
	 * racing the deactivate path) would otherwise write the same resource
	 * to the same file at the same time.
	 */
	private final Object persistMonitor = new Object();

	private final RemoteServiceRegistry registry;

	/**
	 * The runtime home of the provider-side handles. Registrations have
	 * no containment place in the persisted registry — they carry their
	 * links as model references instead (ACQUISITION.md §8). Insertion
	 * order, deterministic, guarded by the broker lock.
	 */
	private final List<ServiceRegistration> registrations = new ArrayList<>();

	/** Whether a snapshot was read, so the caller knows to rebuild the index. */
	private final boolean rehydrated;

	BrokerState(Path snapshotPath) {
		this.snapshotPath = snapshotPath != null ? snapshotPath : Paths.get(DEFAULT_SNAPSHOT_PATH);

		// EMF setup: register the DDSR package and an XMI resource factory
		// for the .xmi extension. This has to work both in OSGi (where
		// emf.osgi may have registered already) and in plain Java tests.
		ServicesPackage.eINSTANCE.eClass();
		this.resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("xmi", new XMIResourceFactoryImpl());

		URI uri = URI.createFileURI(this.snapshotPath.toAbsolutePath().toString());
		Resource loaded = null;
		if (Files.isRegularFile(this.snapshotPath)) {
			try {
				loaded = resourceSet.getResource(uri, true);
			} catch (Exception ex) {
				// Corrupt snapshot: treat as an empty start, and say so
				// loudly. A snapshot written by an older model version
				// fails here too, and silently starting empty would look
				// like the registry simply lost everything.
				LOG.warning("[DDSR] snapshot " + this.snapshotPath
						+ " could not be read, starting with an empty registry: " + ex);
				loaded = null;
			}
		}
		if (loaded != null && !loaded.getContents().isEmpty()) {
			this.resource = loaded;
			EObject root = loaded.getContents().get(0);
			this.registry = (root instanceof RemoteServiceRegistry) ? (RemoteServiceRegistry) root : freshRegistry();
			if (root != this.registry) {
				resource.getContents().clear();
				resource.getContents().add(this.registry);
			}
			this.rehydrated = true;
		} else {
			this.resource = resourceSet.createResource(uri);
			this.registry = freshRegistry();
			this.resource.getContents().add(this.registry);
			this.rehydrated = false;
		}
	}

	private static RemoteServiceRegistry freshRegistry() {
		RemoteServiceRegistry fresh = ServicesFactory.eINSTANCE.createRemoteServiceRegistry();
		fresh.setName("ddsr-broker");
		fresh.setKind(RegistryKind.REMOTE);
		return fresh;
	}

	/** True when a snapshot was read and the transient handles need rebuilding. */
	boolean rehydrated() {
		return rehydrated;
	}

	RemoteServiceRegistry registry() {
		return registry;
	}

	List<ServiceRegistration> registrations() {
		return registrations;
	}

	Resource resource() {
		return resource;
	}

	Path snapshotPath() {
		return snapshotPath;
	}

	Lock readLock() {
		return lock.readLock();
	}

	Lock writeLock() {
		return lock.writeLock();
	}

	/** Runs a read under the read lock. */
	<T> T read(Supplier<T> action) {
		lock.readLock().lock();
		try {
			return action.get();
		} finally {
			lock.readLock().unlock();
		}
	}

	/** Runs a mutation under the write lock. */
	<T> T write(Supplier<T> action) {
		lock.writeLock().lock();
		try {
			return action.get();
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Writes the registry to its file.
	 *
	 * <p>Every acknowledged mutation is saved before it is announced, so
	 * a subscriber never hears about a change that a restart would undo.
	 */
	Diagnostic persist() {
		synchronized (persistMonitor) {
			try {
				Map<Object, Object> options = new HashMap<>();
				options.put(org.eclipse.emf.ecore.xmi.XMIResource.OPTION_ENCODING, "UTF-8");
				resource.save(options);
				return DdsrDiagnostics.ok();
			} catch (IOException failure) {
				return DdsrDiagnostics.error(DdsrDiagnostics.CODE_PERSISTENCE_FAILED,
						"failed to persist registry snapshot: " + failure.getMessage());
			}
		}
	}

	ServiceProvider providerNamed(String name, String version) {
		if (name == null) {
			return null;
		}
		for (ServiceProvider provider : registry.getProviders()) {
			if (name.equals(provider.getName()) && Objects.equals(version, provider.getVersion())) {
				return provider;
			}
		}
		return null;
	}

	ServiceRegistration registrationOf(ServiceImplementation implementation) {
		for (ServiceRegistration registration : registrations) {
			if (registration.getImplementation() == implementation) {
				return registration;
			}
		}
		return null;
	}

	ServiceRegistration registrationWithReferenceId(String referenceId) {
		if (referenceId == null || referenceId.isBlank()) {
			return null;
		}
		for (ServiceRegistration registration : registrations) {
			ServiceReference reference = registration.getReference();
			if (reference != null && referenceId.equals(reference.getId())) {
				return registration;
			}
		}
		return null;
	}

	/** The implementation with this (name, version) among the given ones. */
	static ServiceImplementation implementationNamed(List<ServiceImplementation> candidates,
			String name, String version) {
		if (name == null) {
			return null;
		}
		for (ServiceImplementation candidate : candidates) {
			if (name.equals(candidate.getName()) && Objects.equals(version, candidate.getVersion())) {
				return candidate;
			}
		}
		return null;
	}
}
