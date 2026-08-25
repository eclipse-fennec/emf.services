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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.fennec.services.broker.core.ContractAddressing;
import org.eclipse.fennec.services.broker.core.DdsrBroker;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.broker.core.LookupBackend;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;
import org.eclipse.fennec.services.fingerprint.ServiceImplementationFingerprint;
import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.RegistryKind;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.StringProperty;

/**
 * In-memory broker implementation with synchronous XMI snapshot
 * persistence after each acknowledged mutation, guarded by a
 * read-write lock. Pure Java — no OSGi imports — so it can be reused
 * by a plain-Java host module later.
 */
public final class DdsrBrokerImpl implements DdsrBroker {

	private static final java.util.logging.Logger LOG =
			java.util.logging.Logger.getLogger(DdsrBrokerImpl.class.getName());

	/** XMI URI scheme for the in-memory resource (file-backed when persisted). */
	private static final String DEFAULT_SNAPSHOT_PATH = "./broker-state.xmi";

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final ResourceSet resourceSet;
	private final Resource resource;
	private final Path snapshotPath;
	private final LookupBackend lookup;
	private final EventSink events;

	/**
	 * The runtime home of the provider-side handles. Registrations have
	 * no containment place in the persisted RemoteServiceRegistry — they
	 * carry their links as model references instead
	 * ({@code registration.provider/.implementation/.reference}, paired
	 * resp. transient), which replaced the former
	 * {@code implByRegistration} side-map (ACQUISITION.md §8). Insertion
	 * order (deterministic), guarded by the broker lock.
	 */
	private final List<ServiceRegistration> registrations = new ArrayList<>();

	/**
	 * Consumer sessions by consumerId (ACQUISITION.md §3/§4). Runtime
	 * state by design: deliberately NOT part of the persisted registry —
	 * after a restart, consumers rebuild their sessions via their
	 * regular PUTs. Guarded by the broker lock.
	 */
	private final Map<String, ConsumerSession> sessions = new LinkedHashMap<>();

	/**
	 * Cold cache (ACQUISITION.md §10, optional policy — default off):
	 * registrations that held no lease and saw no lookup on their
	 * interfaces for the configured duration are moved out of the hot
	 * registry into one self-contained XMI file each (wire convention:
	 * provider root + full contract siblings), leaving this in-memory
	 * stub behind. Cold is NOT undiscoverable: a lookup on one of the
	 * stub's interface names lazily rehydrates the entry through the
	 * regular publish path. Keyed by provider/impl identity, guarded by
	 * the broker lock; rebuilt from the cold directory on restart.
	 */
	private final Map<String, ColdEntry> coldEntries = new LinkedHashMap<>();

	/** Last lookup instant per interface name — feeds the cold-idle rule. */
	private final Map<String, Instant> lastLookupByInterface = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * When a registration was last known active (published, rehydrated,
	 * or holding a lease at sweep time) — the other half of the
	 * cold-idle rule. Identity-keyed runtime state, pruned each sweep.
	 */
	private final Map<ServiceRegistration, Instant> registrationSince = new IdentityHashMap<>();

	/** In-memory remainder of a coldified registration. */
	private record ColdEntry(String key, List<String> interfaceNames,
			List<String> addressingFingerprints, String implementationId,
			String providerName, Path file) {
	}

	/**
	 * Serializes {@link #persist()} against itself: {@code snapshot()}
	 * runs under the <em>read</em> lock, so two concurrent snapshots (or
	 * a snapshot racing the deactivate path) would otherwise write the
	 * same {@link Resource} to the same file at the same time.
	 */
	private final Object persistMonitor = new Object();

	private RemoteServiceRegistry registry;

	public DdsrBrokerImpl(Path snapshotPath, LookupBackend lookup) {
		this(snapshotPath, lookup, EventSink.NOOP);
	}

	public DdsrBrokerImpl(Path snapshotPath, LookupBackend lookup, EventSink events) {
		this.snapshotPath = snapshotPath != null ? snapshotPath : Paths.get(DEFAULT_SNAPSHOT_PATH);
		this.lookup = lookup;
		this.events = events != null ? events : EventSink.NOOP;

		// EMF setup: register the DDSR package and a XMI resource factory
		// for the .xmi extension. This needs to work both in OSGi (where
		// emf.osgi may have done the registration already) and in plain
		// Java unit tests.
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
				// Corrupt snapshot — treat as empty start. Caller decides
				// how to react via the snapshot() diagnostic.
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
			// Re-index existing references with the lookup backend.
			reindex();
		} else {
			this.resource = resourceSet.createResource(uri);
			this.registry = freshRegistry();
			this.resource.getContents().add(this.registry);
		}
		loadColdStubs();
	}

	private RemoteServiceRegistry freshRegistry() {
		RemoteServiceRegistry r = ServicesFactory.eINSTANCE.createRemoteServiceRegistry();
		r.setName("ddsr-broker");
		r.setKind(RegistryKind.REMOTE);
		return r;
	}

	private void reindex() {
		// After loading a snapshot the registry has providers and
		// implementations, but the ServiceReference / ServiceRegistration
		// pairs (and the LookupBackend index) are transient and have to
		// be rebuilt. New UUIDs are assigned on each rehydration —
		// references aren't stable across restarts; consumers must
		// re-lookup after a broker reconnect.
		for (ServiceImplementation impl : registry.getImplementations()) {
			EObject container = impl.eContainer();
			if (!(container instanceof ServiceProvider)) {
				continue;
			}
			ServiceProvider provider = (ServiceProvider) container;

			ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
			ref.setId(UUID.randomUUID().toString());
			ref.setProvider(provider);
			ServiceRegistration reg = ServicesFactory.eINSTANCE.createServiceRegistration();
			reg.setReference(ref);
			reg.setUnregistered(false);
			reg.setProvider(provider);
			reg.setImplementation(impl);
			ref.setRegistration(reg);
			registrations.add(reg);
			decorateReference(ref, impl);

			lookup.serviceAdded(impl, ref);
		}
	}

	// ============================================================
	// Provider operations
	// ============================================================

	@Override
	public Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		if (provider == null || implementation == null) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION,
					"provider and implementation must not be null");
		}
		if (implementation.eContainer() != provider) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION,
					"implementation must be contained in provider.implementations");
		}

		lock.writeLock().lock();
		try {
			// Catalog validation + by-name resolve: incoming
			// impl.serviceInterfaces may be stub SIs (just name+version
			// to keep the publish body self-contained). Match them
			// against the live catalog and rewire the impl ref to the
			// real catalog entry, so subsequent lookups by interface
			// name can match.
			boolean anyDeprecated = false;
			StringBuilder deprecationNote = new StringBuilder();
			java.util.List<ServiceInterface> sis = implementation.getServiceInterfaces();
			for (int i = 0; i < sis.size(); i++) {
				ServiceInterface si = sis.get(i);
				String name = catalogNameOf(si);
				if (name == null || name.isBlank()) {
					return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG,
							"impl.serviceInterfaces[" + i + "] has no identifiable name — "
							+ "send it as <serviceInterfaces href=\"<broker>/catalog/{name}\"/> "
							+ "or as a sibling root with name+version set");
				}
				CatalogResolution resolution = resolveCatalogEntry(si, name,
						DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG);
				if (resolution.refusal() != null) {
					return resolution.refusal();
				}
				ServiceInterface inCatalog = resolution.entry();
				if (inCatalog.getStatus() == CatalogStatus.DEPRECATED) {
					anyDeprecated = true;
					if (deprecationNote.length() > 0) {
						deprecationNote.append("; ");
					}
					deprecationNote.append(name);
					if (inCatalog.getDeprecationReason() != null) {
						deprecationNote.append(" (").append(inCatalog.getDeprecationReason()).append(")");
					}
				}
				if (inCatalog != si) {
					sis.set(i, inCatalog);
				}
			}

			// Rewire operation cross-refs on the impl's flavors to point
			// at the LIVE catalog operations. The publisher set them on
			// stub-SI copies sent in the wire bundle; without this
			// rewire, the broker would hold dangling refs to the now-
			// orphaned stub-SI tree and XMI serialise would fail.
			rewireOperationRefs(implementation);

			// Provider dedup: if a provider with the same (name, version)
			// is already in the registry, reuse it instead of adding a
			// duplicate.
			ServiceProvider existing = findProviderByNameVersion(provider.getName(), provider.getVersion());
			if (existing != null && existing != provider) {
				provider = existing;
			}

			// Implementation dedup: if the provider already contains
			// an impl with the same (name, version), retire the old
			// one before adding the new wire-published copy. Without
			// this, repeat publishes accumulate duplicate impl
			// entries → duplicate ServiceReferences after reindex →
			// non-deterministic lookup hits.
			ServiceReference retired = null;
			ServiceImplementation old = findImplementationByNameVersion(
					provider.getImplementations(), implementation.getName(), implementation.getVersion());
			if (old != null && old != implementation) {
				retired = retireImplementation(provider, old);
			}

			// EMF containment auto-moves the new impl into the (possibly
			// pre-existing) provider; remove from any previous container.
			if (implementation.eContainer() != provider) {
				provider.getImplementations().add(implementation);
			}

			if (!registry.getProviders().contains(provider)) {
				registry.getProviders().add(provider);
			}
			if (!registry.getImplementations().contains(implementation)) {
				registry.getImplementations().add(implementation);
			}
			// Non-containment refs (registry.providers / .implementations)
			// need their targets to be URI-resolvable for XMI save to work.
			// Park the provider as an additional resource root if it has no
			// container yet — the implementation rides along via containment.
			if (provider.eResource() == null) {
				resource.getContents().add(provider);
			}

			// Synthesize a ServiceReference + ServiceRegistration pair so
			// consumers have something to lookup against.
			ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
			ref.setId(UUID.randomUUID().toString());
			ref.setProvider(provider);
			ServiceRegistration reg = ServicesFactory.eINSTANCE.createServiceRegistration();
			reg.setReference(ref);
			reg.setUnregistered(false);
			reg.setProvider(provider);
			reg.setImplementation(implementation);
			ref.setRegistration(reg);
			registrations.add(reg);
			registrationSince.put(reg, Instant.now());
			// A (re-)publish supersedes a cold twin of the same identity —
			// e.g. a provider restarting while its old registration is
			// parked cold. The stub goes, the file goes.
			ColdEntry coldTwin = coldEntries.remove(
					coldKey(provider.getName(), implementation.getName(), implementation.getVersion()));
			if (coldTwin != null) {
				try {
					Files.deleteIfExists(coldTwin.file());
				} catch (IOException cleanupFailure) {
					LOG.warning("[DDSR] could not delete superseded cold file " + coldTwin.file());
				}
			}
			decorateReference(ref, implementation);

			lookup.serviceAdded(implementation, ref);

			Diagnostic d = persist();
			if (isError(d)) {
				// Rollback in-memory state on persist failure.
				registry.getImplementations().remove(implementation);
				lookup.serviceRemoved(implementation, ref);
				registrations.remove(reg);
				return d;
			}
			// Order matters for a consumer holding the old reference: the
			// replaced service goes away, then the new one appears.
			emit(ServiceEventType.UNREGISTERING, retired);
			emit(ServiceEventType.REGISTERED, ref);

			return anyDeprecated
					? DdsrDiagnostics.warning(DdsrDiagnostics.CODE_INTERFACE_DEPRECATED,
							"interface(s) marked deprecated: " + deprecationNote)
					: DdsrDiagnostics.ok();
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		if (provider == null || implementation == null) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION,
					"provider and implementation must not be null");
		}
		// Ownership is only checkable while the impl is contained
		// somewhere. A DETACHED impl (eContainer() == null) is what a
		// double withdraw on the same live objects looks like — that
		// falls through to the live resolution below and ends as
		// IMPL_NOT_PUBLISHED, not as an ownership violation.
		if (implementation.eContainer() != null && implementation.eContainer() != provider) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION,
					"implementation must belong to the given provider");
		}

		lock.writeLock().lock();
		try {
			// Over REST the provider/implementation pair is freshly parsed
			// from the wire body and never identical to the live objects —
			// resolve by (name, version) before concluding "not published".
			ServiceProvider liveProvider = provider;
			ServiceImplementation liveImpl = implementation;
			if (!registry.getImplementations().contains(liveImpl)) {
				liveProvider = findProviderByNameVersion(provider.getName(), provider.getVersion());
				liveImpl = liveProvider == null ? null
						: findImplementationByNameVersion(liveProvider.getImplementations(),
								implementation.getName(), implementation.getVersion());
				if (liveImpl == null || !registry.getImplementations().contains(liveImpl)) {
					return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED,
							"implementation is not currently published");
				}
			}

			// Find and drop the matching registration / reference pair
			// (deterministic: insertion order of the registrations list).
			ServiceRegistration toRemove = findRegistrationByImplementation(liveImpl);
			ServiceReference withdrawn = toRemove != null ? toRemove.getReference() : null;

			// Build the event material BEFORE anything is detached, so the
			// UNREGISTERING document is self-contained (implementation and
			// interfaces included) — after the removal below, the lookup
			// can no longer resolve the implementation and every transport
			// would have to fall back to "_unknown" / deliver-to-all.
			// Published only after a successful persist, so the EventSink
			// contract ("only acknowledged and persisted mutations") holds.
			ServiceReference eventReference = selfContainedEventReference(liveProvider, liveImpl, withdrawn);

			// Remember positions for an exact rollback: provider
			// containment order matters for positional cross-refs.
			int indexInProvider = liveProvider.getImplementations().indexOf(liveImpl);
			int indexInRegistry = registry.getImplementations().indexOf(liveImpl);

			// A withdrawn registration must never stay acquired: release
			// the leases (holders re-acquire live refs on their next PUT).
			// Remembered for the persist-failure rollback below.
			List<ConsumerSession> leaseHolders = toRemove != null
					? List.copyOf(toRemove.getUsingSessions())
					: List.of();
			if (toRemove != null) {
				lookup.serviceRemoved(liveImpl, toRemove.getReference());
				registrations.remove(toRemove);
				toRemove.getUsingSessions().clear();
				// The transient links (implementation/provider/reference)
				// stay on the dead pair for event building and rollback,
				// but the flag makes it non-resolvable: a withdrawn
				// reference must stop answering getImplementationForReference
				// (parity with the former side-map removal).
				toRemove.setUnregistered(true);
			}

			registry.getImplementations().remove(liveImpl);
			// Detach from the provider as well, exactly like the republish
			// path does in retireImplementation. Dropping it only from
			// registry.implementations leaves it in the provider's
			// containment, which has two consequences: the withdrawn
			// implementation keeps being written to every snapshot, and it
			// keeps a non-containment reference to its catalog interface —
			// so a later removeCatalogEntry pulls that interface out of its
			// containment while something in the resource still points at
			// it, and every subsequent save fails with "not contained in a
			// resource".
			liveProvider.getImplementations().remove(liveImpl);

			Diagnostic d = persist();
			if (isError(d)) {
				// Roll back — a withdrawal that could not be persisted must
				// leave the in-memory state exactly as it was, and no event
				// may be announced for it.
				liveProvider.getImplementations().add(indexInProvider, liveImpl);
				registry.getImplementations().add(indexInRegistry, liveImpl);
				if (toRemove != null) {
					registrations.add(toRemove);
					toRemove.setUnregistered(false);
					toRemove.getUsingSessions().addAll(leaseHolders);
					lookup.serviceAdded(liveImpl, toRemove.getReference());
				}
				return d;
			}
			// After the save, never before: a withdrawal that could not
			// be persisted must not be announced.
			emit(ServiceEventType.UNREGISTERING, eventReference);
			return d;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * A detached copy of the withdrawn reference whose provider subtree
	 * contains exactly the withdrawn implementation (plus copies of its
	 * interfaces), so that {@code EventDocument} can build a
	 * self-contained UNREGISTERING document although the live lookup no
	 * longer resolves the implementation by the time the event goes out.
	 * <p>
	 * Copied with {@code useOriginalReferences = false}: references to
	 * anything outside the copy set (notably the registration and its
	 * eOpposite) are dropped instead of pointing back into — and via
	 * eOpposite mutating — live broker state.
	 */
	private static ServiceReference selfContainedEventReference(ServiceProvider provider,
			ServiceImplementation implementation, ServiceReference reference) {
		if (reference == null) {
			return null;
		}
		EcoreUtil.Copier copier = new EcoreUtil.Copier(true, false);
		Collection<EObject> originals = new ArrayList<>();
		originals.add(reference);
		originals.add(provider);
		originals.addAll(implementation.getServiceInterfaces());
		copier.copyAll(originals);
		copier.copyReferences();

		ServiceProvider providerCopy = (ServiceProvider) copier.get(provider);
		ServiceImplementation implCopy = (ServiceImplementation) copier.get(implementation);
		providerCopy.getImplementations().removeIf(other -> other != implCopy);

		ServiceReference referenceCopy = (ServiceReference) copier.get(reference);
		referenceCopy.setProvider(providerCopy);
		return referenceCopy;
	}

	@Override
	public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
		Diagnostic d = publishImplementation(provider, implementation);
		if (isError(d)) {
			return null;
		}
		lock.readLock().lock();
		try {
			return findRegistrationByImplementation(implementation);
		} finally {
			lock.readLock().unlock();
		}
	}

	// ============================================================
	// Consumer operations
	// ============================================================

	@Override
	public ServiceReference getServiceReference(String interfaceName) {
		touchAndRehydrate(interfaceName);
		lock.readLock().lock();
		try {
			return lookup.getServiceReference(interfaceName, null, null);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		touchAndRehydrate(interfaceName);
		lock.readLock().lock();
		try {
			return filterByRequestedFingerprint(
					lookup.getServiceReferences(interfaceName, filter, capability), capability);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		touchAndRehydrate(interfaceName);
		lock.readLock().lock();
		try {
			return filterByRequestedFingerprint(
					lookup.getAllServiceReferences(interfaceName, filter, capability), capability);
		} finally {
			lock.readLock().unlock();
		}
	}

	// ============================================================
	// Catalog operations
	// ============================================================

	@Override
	public Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		if (serviceInterface == null || serviceInterface.getName() == null) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND,
					"serviceInterface and serviceInterface.name must not be null");
		}
		lock.writeLock().lock();
		try {
			// (name, sd1) key (§11.2): identical content is an idempotent
			// no-op; a different contract under the same name COEXISTS as
			// its own entry — consumers address the contract they speak
			// via the fingerprint, never by name alone.
			String sd1 = addressingFingerprint(serviceInterface);
			List<ServiceInterface> sameName = findCatalogEntriesByName(serviceInterface.getName());
			for (ServiceInterface entry : sameName) {
				if (sd1.equals(addressingFingerprint(entry))) {
					return DdsrDiagnostics.ok("catalog entry already present (identical content), fingerprint="
							+ sd1);
				}
			}
			registry.getCatalog().add(serviceInterface);
			Diagnostic d = persist();
			if (isError(d)) {
				// Roll back so a failed save does not leave the in-memory
				// catalog ahead of the snapshot — see the note on
				// rollbackOnPersistFailure.
				registry.getCatalog().remove(serviceInterface);
				return d;
			}
			// Hand the broker's fingerprint of the accepted entry back to
			// the publisher, so producer and broker can compare views
			// without another round trip (DECISIONS_PARITY D6).
			return DdsrDiagnostics.ok("catalog entry added, fingerprint=" + sd1
					+ (sameName.isEmpty() ? ""
							: " (coexists with " + sameName.size()
							+ " other contract(s) named '" + serviceInterface.getName() + "')"));
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		if (serviceInterface == null || serviceInterface.getName() == null) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND,
					"serviceInterface and serviceInterface.name must not be null");
		}
		lock.writeLock().lock();
		try {
			CatalogResolution resolution = resolveCatalogEntry(serviceInterface,
					serviceInterface.getName(), DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND);
			if (resolution.refusal() != null) {
				return resolution.refusal();
			}
			ServiceInterface inCatalog = resolution.entry();
			CatalogStatus previousStatus = inCatalog.getStatus();
			String previousReason = inCatalog.getDeprecationReason();
			ServiceInterface previousReplacedBy = inCatalog.getReplacedBy();

			inCatalog.setStatus(CatalogStatus.DEPRECATED);
			if (serviceInterface.getDeprecationReason() != null) {
				inCatalog.setDeprecationReason(serviceInterface.getDeprecationReason());
			}
			if (serviceInterface.getReplacedBy() != null) {
				inCatalog.setReplacedBy(serviceInterface.getReplacedBy());
			}

			Diagnostic d = persist();
			if (isError(d)) {
				inCatalog.setStatus(previousStatus);
				inCatalog.setDeprecationReason(previousReason);
				inCatalog.setReplacedBy(previousReplacedBy);
			}
			return d;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		if (serviceInterface == null || serviceInterface.getName() == null) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND,
					"serviceInterface and serviceInterface.name must not be null");
		}
		lock.writeLock().lock();
		try {
			CatalogResolution resolution = resolveCatalogEntry(serviceInterface,
					serviceInterface.getName(), DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND);
			if (resolution.refusal() != null) {
				return resolution.refusal();
			}
			ServiceInterface inCatalog = resolution.entry();
			// Strict-Reject: any live implementation that references this
			// interface blocks the removal (REQUIREMENTS FR-Catalog-Removal-StrictReject).
			// Identity, not name: the publish path rewires every live impl
			// onto its catalog entry, and with (name, sd1) coexistence a
			// same-named SIBLING contract must not block this removal.
			for (ServiceImplementation impl : registry.getImplementations()) {
				for (ServiceInterface si : impl.getServiceInterfaces()) {
					if (si == inCatalog) {
						return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_HAS_LIVE_IMPLS,
								"cannot remove catalog entry '" + inCatalog.getName()
										+ "': implementation '"
										+ impl.getName() + "' still publishes it");
					}
				}
			}
			// Cold entries count as live for strict-reject: they are still
			// discoverable and would fail to rehydrate without their contract.
			String address = addressingFingerprint(inCatalog);
			for (ColdEntry cold : coldEntries.values()) {
				if (cold.addressingFingerprints().contains(address)) {
					return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_HAS_LIVE_IMPLS,
							"cannot remove catalog entry '" + inCatalog.getName()
									+ "': cold implementation '" + cold.implementationId()
									+ "' still references it");
				}
			}
			// Keep the position so a rollback restores the catalog exactly,
			// not just its contents — cross-refs into the catalog use
			// positional URI fragments (//@catalog.N), so order matters.
			int previousIndex = registry.getCatalog().indexOf(inCatalog);
			registry.getCatalog().remove(inCatalog);
			Diagnostic d = persist();
			if (isError(d)) {
				registry.getCatalog().add(previousIndex, inCatalog);
			}
			return d;
		} finally {
			lock.writeLock().unlock();
		}
	}

	// ============================================================
	// State access
	// ============================================================

	/**
	 * Returns a <b>detached copy</b> of the registry, created under the
	 * read lock. Callers (the REST layer serializing it as an entity, the
	 * self-publisher scanning names) work on a consistent snapshot while
	 * concurrent mutations proceed — serializing the <em>live</em> tree
	 * outside the lock was a torn-document race, and it leaked
	 * {@code file:} hrefs of the snapshot path into responses (W1).
	 * <p>
	 * The copy (registry plus provider roots, cross-references rewired)
	 * is parked in a throwaway in-memory resource with the opaque URI
	 * {@code services:registry}, so downstream XMI serialization has
	 * resolvable targets for the registry's non-containment references.
	 */
	@Override
	public RemoteServiceRegistry getRegistry() {
		lock.readLock().lock();
		try {
			EcoreUtil.Copier copier = new EcoreUtil.Copier();
			RemoteServiceRegistry registryCopy = (RemoteServiceRegistry) copier.copy(registry);
			copier.copyAll(new ArrayList<>(registry.getProviders()));
			copier.copyReferences();

			Resource holder = new XMIResourceImpl(URI.createURI("services:registry"));
			holder.getContents().add(registryCopy);
			for (ServiceProvider provider : registry.getProviders()) {
				EObject providerCopy = copier.get(provider);
				if (providerCopy != null && providerCopy.eContainer() == null) {
					holder.getContents().add(providerCopy);
				}
			}
			return registryCopy;
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * The live registry object — package-private for tests and internal
	 * callers that need identity rather than a snapshot. Any read of it
	 * outside the broker's own locking is the caller's race to lose.
	 */
	RemoteServiceRegistry liveRegistry() {
		return registry;
	}

	@Override
	public Diagnostic snapshot() {
		lock.readLock().lock();
		try {
			return persist();
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public ServiceImplementation getImplementationForReference(ServiceReference reference) {
		if (reference == null) {
			return null;
		}
		ServiceRegistration reg = reference.getRegistration();
		if (reg == null) {
			return null;
		}
		lock.readLock().lock();
		try {
			// The transient link survives on a withdrawn pair (needed for
			// event building and rollback) — the flag is what says "dead".
			return reg.isUnregistered() ? null : reg.getImplementation();
		} finally {
			lock.readLock().unlock();
		}
	}

	// ============================================================
	// Session operations (ACQUISITION.md §3/§4)
	// ============================================================

	@Override
	public Diagnostic putSession(ConsumerSession session, Collection<String> acquiredReferenceIds) {
		if (session == null || session.getConsumerId() == null || session.getConsumerId().isBlank()) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_SESSION_INVALID,
					"session and session.consumerId must not be null or blank");
		}
		lock.writeLock().lock();
		try {
			String consumerId = session.getConsumerId();
			// Full replace: the previous session's leases are released
			// first — the incoming list is the complete, current truth.
			ConsumerSession previous = sessions.remove(consumerId);
			if (previous != null) {
				releaseAcquisitions(previous);
			}
			session.setLastRenewal(new Date());
			int accepted = 0;
			List<String> unknown = new ArrayList<>();
			if (acquiredReferenceIds != null) {
				for (String referenceId : acquiredReferenceIds) {
					ServiceRegistration registration = findRegistrationByReferenceId(referenceId);
					if (registration == null) {
						// Over-claiming is harmless: stale or foreign ids
						// (e.g. from before a broker restart) are skipped
						// and reported, never rejected (ACQUISITION.md §5).
						unknown.add(referenceId);
						continue;
					}
					if (!session.getAcquisitions().contains(registration)) {
						session.getAcquisitions().add(registration);
						accepted++;
					}
				}
			}
			sessions.put(consumerId, session);
			// Deliberately NO persist and NO event: sessions are runtime
			// state (ACQUISITION.md §6).
			return DdsrDiagnostics.ok("session accepted, " + accepted + " acquisition(s)"
					+ (unknown.isEmpty() ? "" : ", skipped unknown reference id(s): " + unknown));
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Diagnostic deleteSession(String consumerId) {
		if (consumerId == null || consumerId.isBlank()) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_SESSION_INVALID,
					"consumerId must not be null or blank");
		}
		lock.writeLock().lock();
		try {
			ConsumerSession removed = sessions.remove(consumerId);
			if (removed == null) {
				// Idempotent: a shutdown-notify may race the TTL expiry.
				return DdsrDiagnostics.ok("no session for '" + consumerId + "' — nothing to release");
			}
			releaseAcquisitions(removed);
			return DdsrDiagnostics.ok("session removed, all acquisitions released");
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Optional<SessionSnapshot> getSession(String consumerId) {
		if (consumerId == null || consumerId.isBlank()) {
			return Optional.empty();
		}
		lock.readLock().lock();
		try {
			ConsumerSession stored = sessions.get(consumerId);
			if (stored == null) {
				return Optional.empty();
			}
			// Manual detached copy — NOT EcoreUtil.copy: copying the
			// bidirectional (transient) acquisitions would touch the live
			// registrations' usingSessions via the eOpposite.
			ConsumerSession view = ServicesFactory.eINSTANCE.createConsumerSession();
			view.setConsumerId(stored.getConsumerId());
			view.setLastRenewal(stored.getLastRenewal());
			if (stored.getCapabilities() != null) {
				view.setCapabilities(EcoreUtil.copy(stored.getCapabilities()));
			}
			List<String> ids = new ArrayList<>(stored.getAcquisitions().size());
			for (ServiceRegistration registration : stored.getAcquisitions()) {
				ServiceReference reference = registration.getReference();
				if (reference != null && reference.getId() != null) {
					ids.add(reference.getId());
				}
			}
			return Optional.of(new SessionSnapshot(view, ids));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public int expireSessions(Instant cutoff) {
		if (cutoff == null) {
			return 0;
		}
		lock.writeLock().lock();
		try {
			int expired = 0;
			var iterator = sessions.entrySet().iterator();
			while (iterator.hasNext()) {
				ConsumerSession session = iterator.next().getValue();
				Date lastRenewal = session.getLastRenewal();
				if (lastRenewal == null || lastRenewal.toInstant().isBefore(cutoff)) {
					releaseAcquisitions(session);
					iterator.remove();
					expired++;
				}
			}
			return expired;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public int sessionCount() {
		lock.readLock().lock();
		try {
			return sessions.size();
		} finally {
			lock.readLock().unlock();
		}
	}

	/** Clears the acquisitions; the eOpposite removes the session from every registration's usingSessions. */
	private static void releaseAcquisitions(ConsumerSession session) {
		session.getAcquisitions().clear();
	}

	// ============================================================
	// Cold cache (ACQUISITION.md §10)
	// ============================================================

	/**
	 * Moves every registration cold that has been idle since before the
	 * cutoff: no lease at sweep time (a lease at sweep time restarts its
	 * idle clock), published/rehydrated before the cutoff, and no lookup
	 * on any of its interface names since the cutoff. A coldified
	 * registration announces {@code UNREGISTERING} (its reference id
	 * becomes invalid — the OSGi lifecycle promise holds), but stays
	 * discoverable through its stub: the next lookup rehydrates it via
	 * the regular publish path, announcing {@code REGISTERED} with a
	 * fresh reference. Returns the number of registrations moved.
	 */
	public int coldifyIdle(Instant cutoff) {
		if (cutoff == null) {
			return 0;
		}
		lock.writeLock().lock();
		try {
			Instant now = Instant.now();
			registrationSince.keySet().retainAll(new java.util.HashSet<>(registrations));
			int moved = 0;
			for (ServiceRegistration reg : new ArrayList<>(registrations)) {
				if (reg.isUnregistered() || reg.getImplementation() == null || reg.getProvider() == null) {
					continue;
				}
				Instant since = registrationSince.computeIfAbsent(reg, r -> now);
				if (!reg.getUsingSessions().isEmpty()) {
					registrationSince.put(reg, now);
					continue;
				}
				if (!since.isBefore(cutoff) || anyInterfaceLookedUpSince(reg.getImplementation(), cutoff)) {
					continue;
				}
				if (moveCold(reg)) {
					moved++;
				}
			}
			return moved;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/** Number of cold entries currently parked on disk. */
	public int coldCount() {
		lock.readLock().lock();
		try {
			return coldEntries.size();
		} finally {
			lock.readLock().unlock();
		}
	}

	private boolean anyInterfaceLookedUpSince(ServiceImplementation impl, Instant cutoff) {
		for (ServiceInterface si : impl.getServiceInterfaces()) {
			Instant last = lastLookupByInterface.get(si.getName());
			if (last != null && !last.isBefore(cutoff)) {
				return true;
			}
		}
		return false;
	}

	/** Serialize provider stub + impl + full contracts, retire the hot entry, keep the stub. */
	private boolean moveCold(ServiceRegistration reg) {
		ServiceImplementation impl = reg.getImplementation();
		ServiceProvider provider = reg.getProvider();
		String key = coldKey(provider.getName(), impl.getName(), impl.getVersion());

		List<String> names = new ArrayList<>();
		List<String> fingerprints = new ArrayList<>();
		EcoreUtil.Copier copier = new EcoreUtil.Copier();
		ServiceImplementation implCopy = (ServiceImplementation) copier.copy(impl);
		List<ServiceInterface> contractCopies = new ArrayList<>();
		for (ServiceInterface si : impl.getServiceInterfaces()) {
			contractCopies.add((ServiceInterface) copier.copy(si));
			names.add(si.getName());
			fingerprints.add(addressingFingerprint(si));
		}
		copier.copyReferences();

		ServiceProvider providerStub = ServicesFactory.eINSTANCE.createServiceProvider();
		providerStub.setName(provider.getName());
		providerStub.setVersion(provider.getVersion());
		providerStub.getImplementations().add(implCopy);

		Path file = coldDir().resolve(coldFileName(key));
		try {
			Files.createDirectories(coldDir());
			Resource coldResource = new XMIResourceImpl(URI.createFileURI(file.toAbsolutePath().toString()));
			coldResource.getContents().add(providerStub);
			coldResource.getContents().addAll(contractCopies);
			Map<Object, Object> opts = new HashMap<>();
			opts.put(org.eclipse.emf.ecore.xmi.XMIResource.OPTION_ENCODING, "UTF-8");
			coldResource.save(opts);
		} catch (IOException writeFailure) {
			LOG.warning("[DDSR] cold-cache write failed for " + key + ", entry stays hot: " + writeFailure);
			return false;
		}

		ServiceReference retiredRef = retireImplementation(provider, impl);
		registrationSince.remove(reg);
		coldEntries.put(key, new ColdEntry(key, names, fingerprints,
				impl.getImplementationId(), provider.getName(), file));
		Diagnostic d = persist();
		if (isError(d)) {
			// Degraded but recoverable: the entry is discoverable through
			// its stub, and rehydration republishes it.
			LOG.warning("[DDSR] persist after coldify failed for " + key + ": " + d.getMessage());
		}
		emit(ServiceEventType.UNREGISTERING, retiredRef);
		LOG.fine(() -> "[DDSR] coldified " + key + " -> " + file);
		return true;
	}

	/**
	 * Rehydrate every cold entry serving the given interface name —
	 * called on the lookup path BEFORE the index query, so "cold" is
	 * never "undiscoverable". Runs the regular publish path (catalog
	 * validation, decoration, REGISTERED event); an entry that no longer
	 * publishes cleanly (e.g. its contract left the catalog) is dropped
	 * from the stub index with a warning — the file stays for forensics.
	 */
	/** Lookup-path hook: record interface activity, then wake matching cold entries. */
	private void touchAndRehydrate(String interfaceName) {
		if (interfaceName != null) {
			lastLookupByInterface.put(interfaceName, Instant.now());
		}
		rehydrateColdFor(interfaceName);
	}

	private void rehydrateColdFor(String interfaceName) {
		if (interfaceName == null || coldEntries.isEmpty()) {
			return;
		}
		lock.writeLock().lock();
		try {
			for (ColdEntry entry : new ArrayList<>(coldEntries.values())) {
				if (!entry.interfaceNames().contains(interfaceName)) {
					continue;
				}
				coldEntries.remove(entry.key());
				Diagnostic d = rehydrate(entry);
				if (isError(d)) {
					LOG.warning("[DDSR] cold entry " + entry.key() + " failed to rehydrate and was dropped: "
							+ d.getMessage());
				} else {
					try {
						Files.deleteIfExists(entry.file());
					} catch (IOException cleanupFailure) {
						LOG.warning("[DDSR] rehydrated but could not delete cold file " + entry.file());
					}
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	private Diagnostic rehydrate(ColdEntry entry) {
		ResourceSet coldSet = new ResourceSetImpl();
		coldSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("xmi", new XMIResourceFactoryImpl());
		try {
			Resource coldResource = coldSet.getResource(
					URI.createFileURI(entry.file().toAbsolutePath().toString()), true);
			for (EObject root : coldResource.getContents()) {
				if (root instanceof ServiceProvider provider && !provider.getImplementations().isEmpty()) {
					return publishImplementation(provider, provider.getImplementations().get(0));
				}
			}
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_PERSISTENCE_FAILED,
					"cold file carries no provider root: " + entry.file());
		} catch (RuntimeException loadFailure) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_PERSISTENCE_FAILED,
					"cold file unreadable: " + entry.file() + " — " + loadFailure);
		}
	}

	/** Rebuild the stub index from the cold directory (broker restart). */
	private void loadColdStubs() {
		Path dir = coldDir();
		if (!Files.isDirectory(dir)) {
			return;
		}
		try (var files = Files.list(dir)) {
			files.filter(f -> f.getFileName().toString().endsWith(".xmi")).forEach(this::loadColdStub);
		} catch (IOException scanFailure) {
			LOG.warning("[DDSR] cold directory unreadable, cold entries stay parked: " + scanFailure);
		}
	}

	private void loadColdStub(Path file) {
		ResourceSet coldSet = new ResourceSetImpl();
		coldSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("xmi", new XMIResourceFactoryImpl());
		try {
			Resource coldResource = coldSet.getResource(
					URI.createFileURI(file.toAbsolutePath().toString()), true);
			for (EObject root : coldResource.getContents()) {
				if (!(root instanceof ServiceProvider provider) || provider.getImplementations().isEmpty()) {
					continue;
				}
				ServiceImplementation impl = provider.getImplementations().get(0);
				List<String> names = new ArrayList<>();
				List<String> fingerprints = new ArrayList<>();
				for (ServiceInterface si : impl.getServiceInterfaces()) {
					names.add(si.getName());
					fingerprints.add(addressingFingerprint(si));
				}
				String key = coldKey(provider.getName(), impl.getName(), impl.getVersion());
				coldEntries.put(key, new ColdEntry(key, names, fingerprints,
						impl.getImplementationId(), provider.getName(), file));
				return;
			}
			LOG.warning("[DDSR] cold file without provider root ignored: " + file);
		} catch (RuntimeException parseFailure) {
			LOG.warning("[DDSR] unreadable cold file ignored: " + file + " — " + parseFailure);
		}
	}

	private Path coldDir() {
		Path parent = snapshotPath.toAbsolutePath().getParent();
		return parent.resolve(snapshotPath.getFileName() + ".cold");
	}

	private static String coldKey(String providerName, String implName, String implVersion) {
		return providerName + "|" + implName + "|" + (implVersion == null ? "" : implVersion);
	}

	private static String coldFileName(String key) {
		try {
			byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
					.digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (int i = 0; i < 12; i++) {
				hex.append(String.format("%02x", digest[i]));
			}
			return hex + ".xmi";
		} catch (java.security.NoSuchAlgorithmException impossible) {
			throw new IllegalStateException("JVM without SHA-256", impossible);
		}
	}

	private ServiceRegistration findRegistrationByImplementation(ServiceImplementation implementation) {
		for (ServiceRegistration registration : registrations) {
			if (registration.getImplementation() == implementation) {
				return registration;
			}
		}
		return null;
	}

	private ServiceRegistration findRegistrationByReferenceId(String referenceId) {
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

	/**
	 * Contract addressing (ACQUISITION.md §11.2): when the consumer's
	 * capability carries a {@code ddsr.fingerprint} property, only
	 * references whose broker-computed contract fingerprint matches
	 * exactly are returned. Compatibility = identical sd1 — the
	 * comparison runs against the CATALOG truth (decorateReference hashes
	 * the catalog entries the impl was rewired to at publish), so a
	 * provider whose local contract drifted falls out of compatible
	 * lookups. No range semantics: fingerprints are identity, versions
	 * communicate intent (§11.3).
	 */
	private static List<ServiceReference> filterByRequestedFingerprint(List<ServiceReference> references,
			ConsumerCapability capability) {
		String requested = requestedFingerprint(capability);
		if (requested == null) {
			return references;
		}
		List<ServiceReference> matching = new ArrayList<>(references.size());
		for (ServiceReference reference : references) {
			if (carriesFingerprint(reference, requested)) {
				matching.add(reference);
			}
		}
		return matching;
	}

	private static String requestedFingerprint(ConsumerCapability capability) {
		if (capability == null) {
			return null;
		}
		for (Property property : capability.getProperties()) {
			if ("ddsr.fingerprint".equals(property.getName()) && property instanceof StringProperty sp) {
				String value = sp.getValue();
				return value == null || value.isBlank() ? null : value;
			}
		}
		return null;
	}

	private static boolean carriesFingerprint(ServiceReference reference, String requested) {
		for (Property property : reference.getProperties()) {
			String name = property.getName();
			if (name != null && name.startsWith("ddsr.fingerprint")
					&& property instanceof StringProperty sp
					&& requested.equals(sp.getValue())) {
				return true;
			}
		}
		return false;
	}

	// ============================================================
	// Helpers
	// ============================================================

	private List<ServiceInterface> findCatalogEntriesByName(String name) {
		List<ServiceInterface> entries = new ArrayList<>();
		if (name == null) {
			return entries;
		}
		for (ServiceInterface si : registry.getCatalog()) {
			if (name.equals(si.getName())) {
				entries.add(si);
			}
		}
		return entries;
	}

	/** Resolution outcome: exactly one of entry / refusal is set. */
	private record CatalogResolution(ServiceInterface entry, Diagnostic refusal) {
		static CatalogResolution of(ServiceInterface entry) {
			return new CatalogResolution(entry, null);
		}
		static CatalogResolution refuse(Diagnostic refusal) {
			return new CatalogResolution(null, refusal);
		}
	}

	/**
	 * Contract addressing (ACQUISITION.md §11.2): the catalog key is
	 * {@code (name, sd1)} — same-named entries with different contracts
	 * coexist. An incoming ServiceInterface that carries content
	 * (operations or exceptions) addresses its entry EXACTLY by that
	 * content: its sd1 must match one of the same-named entries — a miss
	 * is contract drift, not a lookup fallback. A stub (name-only
	 * sibling, catalog-URL proxy, or bodyless REST call) resolves by
	 * name alone and requires the name to be unambiguous.
	 *
	 * @param notFoundCode the code for "no entry under this name" —
	 *                     differs between the publish path
	 *                     ({@code CODE_IMPL_INTERFACE_NOT_IN_CATALOG})
	 *                     and catalog governance
	 *                     ({@code CODE_CATALOG_ENTRY_NOT_FOUND})
	 */
	private CatalogResolution resolveCatalogEntry(ServiceInterface incoming, String name, int notFoundCode) {
		List<ServiceInterface> entries = findCatalogEntriesByName(name);
		if (entries.isEmpty()) {
			return CatalogResolution.refuse(DdsrDiagnostics.error(notFoundCode,
					"service interface '" + name + "' is not in the catalog"));
		}
		boolean carriesContent = !((org.eclipse.emf.ecore.InternalEObject) incoming).eIsProxy()
				&& (!incoming.getOperations().isEmpty() || !incoming.getExceptions().isEmpty());
		if (carriesContent) {
			String sd1 = addressingFingerprint(incoming);
			for (ServiceInterface entry : entries) {
				if (sd1.equals(addressingFingerprint(entry))) {
					return CatalogResolution.of(entry);
				}
			}
			return CatalogResolution.refuse(DdsrDiagnostics.error(notFoundCode,
					"contract drift: '" + name + "' is in the catalog ("
					+ entries.size() + " contract(s)), but none matches the submitted content ("
					+ sd1 + ")"));
		}
		if (entries.size() > 1) {
			return CatalogResolution.refuse(DdsrDiagnostics.error(
					DdsrDiagnostics.CODE_CATALOG_ENTRY_AMBIGUOUS,
					"interface name '" + name + "' names " + entries.size()
					+ " coexisting catalog contracts — address the contract by content "
					+ "(full ServiceInterface) or by its sd1 fingerprint"));
		}
		return CatalogResolution.of(entries.get(0));
	}

	/** See {@link ContractAddressing} — status-neutral sd1 for catalog addressing. */
	private static String addressingFingerprint(ServiceInterface si) {
		return ContractAddressing.fingerprint(si);
	}

	/**
	 * Walk every flavor on the implementation and rewire each
	 * {@code operationFlavor.operation} to the matching
	 * {@link ServiceOperation} in {@code impl.serviceInterfaces}
	 * (which by this point has been pointed at the live catalog
	 * entries). Handles wire-side EMF proxies by reading the proxy
	 * URI fragment ({@code //@operations.N}) so we never trigger an
	 * HTTP fetch on the broker side.
	 */
	private void rewireOperationRefs(ServiceImplementation implementation) {
		for (org.eclipse.fennec.services.ServiceFlavor flavor : implementation.getFlavors()) {
			for (org.eclipse.fennec.services.ServiceOperationFlavor of : flavor.getOperationFlavors()) {
				org.eclipse.fennec.services.ServiceOperation live = resolveLiveOperation(implementation, of);
				if (live != null && live != of.getOperation()) {
					of.setOperation(live);
				}
			}
		}
	}

	private org.eclipse.fennec.services.ServiceOperation resolveLiveOperation(
			ServiceImplementation impl,
			org.eclipse.fennec.services.ServiceOperationFlavor of) {
		org.eclipse.fennec.services.ServiceOperation refOp = of.getOperation();
		// (1) Operation cross-ref present and not a proxy: use its name.
		if (refOp != null && !((org.eclipse.emf.ecore.InternalEObject) refOp).eIsProxy()) {
			return findOperationByName(impl, refOp.getName());
		}
		// (2) Operation cross-ref is a proxy from a wire href like
		// http://broker/.../catalog/Payment#//@operations.0 — pull the
		// positional index from the fragment without resolving.
		if (refOp != null) {
			org.eclipse.emf.common.util.URI proxyUri =
					((org.eclipse.emf.ecore.InternalEObject) refOp).eProxyURI();
			Integer idx = positionalIndex(proxyUri);
			if (idx != null) {
				for (ServiceInterface si : impl.getServiceInterfaces()) {
					if (idx >= 0 && idx < si.getOperations().size()) {
						return si.getOperations().get(idx);
					}
				}
			}
		}
		// (3) Fallback: the flavor's own name conventionally matches the
		// operation name (BrokerSelfPublisher and PaymentPublisher both
		// follow this).
		return findOperationByName(impl, of.getName());
	}

	private org.eclipse.fennec.services.ServiceOperation findOperationByName(ServiceImplementation impl, String opName) {
		if (opName == null) {
			return null;
		}
		for (ServiceInterface si : impl.getServiceInterfaces()) {
			for (org.eclipse.fennec.services.ServiceOperation op : si.getOperations()) {
				if (opName.equals(op.getName())) {
					return op;
				}
			}
		}
		return null;
	}

	private static Integer positionalIndex(org.eclipse.emf.common.util.URI uri) {
		if (uri == null) {
			return null;
		}
		String fragment = uri.fragment();
		if (fragment == null) {
			return null;
		}
		// Expect "//@operations.N"
		int dot = fragment.lastIndexOf('.');
		if (dot < 0 || dot == fragment.length() - 1) {
			return null;
		}
		try {
			return Integer.parseInt(fragment.substring(dot + 1));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Extract the catalog-entry name from a ServiceInterface ref.
	 * Handles both:
	 * <ul>
	 *   <li>resolved SI (sibling root in the wire) → {@code si.getName()}</li>
	 *   <li>EMF proxy with URI like {@code .../catalog/Payment} →
	 *       last path segment</li>
	 * </ul>
	 */
	private static String catalogNameOf(ServiceInterface si) {
		if (si == null) {
			return null;
		}
		if (!((org.eclipse.emf.ecore.InternalEObject) si).eIsProxy()) {
			return si.getName();
		}
		org.eclipse.emf.common.util.URI proxyUri =
				((org.eclipse.emf.ecore.InternalEObject) si).eProxyURI();
		if (proxyUri == null) {
			return null;
		}
		// Last path segment of e.g. http://broker/ddsr/rest/catalog/Payment
		int segments = proxyUri.segmentCount();
		if (segments == 0) {
			return null;
		}
		return proxyUri.segment(segments - 1);
	}

	private static ServiceImplementation findImplementationByNameVersion(
			java.util.List<ServiceImplementation> impls, String name, String version) {
		if (name == null) {
			return null;
		}
		for (ServiceImplementation impl : impls) {
			if (name.equals(impl.getName())
					&& java.util.Objects.equals(version, impl.getVersion())) {
				return impl;
			}
		}
		return null;
	}

	/**
	 * Drop a stale {@link ServiceImplementation} that is about to be
	 * replaced by a fresher copy with the same (name, version):
	 * detach from its provider, remove from {@code registry.implementations},
	 * tell the lookup backend its reference is gone, and clean up
	 * the registrations list.
	 */
	private ServiceReference retireImplementation(ServiceProvider provider, ServiceImplementation oldImpl) {
		provider.getImplementations().remove(oldImpl);
		registry.getImplementations().remove(oldImpl);
		ServiceRegistration deadReg = findRegistrationByImplementation(oldImpl);
		if (deadReg != null) {
			registrations.remove(deadReg);
			registrationSince.remove(deadReg);
			// A replaced registration releases its leases; holders
			// re-acquire the successor on their next session PUT.
			deadReg.getUsingSessions().clear();
			deadReg.setUnregistered(true);
		}
		if (deadReg != null && deadReg.getReference() != null) {
			lookup.serviceRemoved(oldImpl, deadReg.getReference());
			// Returned rather than announced here: the replacement is not
			// persisted yet, and the caller emits once it is.
			return deadReg.getReference();
		}
		return null;
	}

	private ServiceProvider findProviderByNameVersion(String name, String version) {
		if (name == null) {
			return null;
		}
		for (ServiceProvider p : registry.getProviders()) {
			if (name.equals(p.getName())
					&& java.util.Objects.equals(version, p.getVersion())) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Hands one lifecycle event to the sink. Called only for mutations
	 * that were acknowledged AND persisted, and while the write lock is
	 * still held, so per-service ordering matches the order in which the
	 * mutations were applied.
	 * <p>
	 * A sink must not throw, but we do not trust it to keep that promise:
	 * a misbehaving subscriber may not undo a change the broker has
	 * already committed and persisted.
	 */
	private void emit(ServiceEventType type, ServiceReference reference) {
		if (reference == null) {
			return;
		}
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(type);
		event.setReference(reference);
		event.setTimestamp(new Date());
		try {
			events.publish(event);
		} catch (RuntimeException sinkFailure) {
			// Swallow deliberately — see above.
		}
	}

	/**
	 * Whether a diagnostic denies the operation. Mutations that already
	 * touched the model roll their change back when this is true, so the
	 * in-memory state never runs ahead of the persisted snapshot.
	 */
	private static boolean isError(Diagnostic d) {
		return d.getSeverity().getValue() >= DiagnosticSeverity.ERROR_VALUE;
	}

	/**
	 * Persists the registry resource to disk. Caller MUST hold the
	 * write lock (or read lock during {@link #snapshot()}). Returns a
	 * Diagnostic — OK on success, ERROR on I/O failure.
	 */
	private Diagnostic persist() {
		synchronized (persistMonitor) {
			try {
				Map<Object, Object> opts = new HashMap<>();
				opts.put(org.eclipse.emf.ecore.xmi.XMIResource.OPTION_ENCODING, "UTF-8");
				resource.save(opts);
				return DdsrDiagnostics.ok();
			} catch (IOException ex) {
				return DdsrDiagnostics.error(DdsrDiagnostics.CODE_PERSISTENCE_FAILED,
						"failed to persist registry snapshot: " + ex.getMessage());
			}
		}
	}

	/**
	 * Framework decoration of a freshly minted reference: the
	 * implementation's provider-supplied properties are copied onto the
	 * reference (the OSGi rule — consumers read properties from the
	 * reference, not the implementation), plus the {@code sd1}
	 * fingerprint of each serviced description as
	 * {@code ddsr.fingerprint} (single interface) or
	 * {@code ddsr.fingerprint.<interfaceName>} (several), so any party
	 * can compare its view of the contract against the broker's.
	 */
	private static void decorateReference(ServiceReference reference, ServiceImplementation implementation) {
		for (Property property : implementation.getProperties()) {
			reference.getProperties().add(EcoreUtil.copy(property));
		}
		List<ServiceInterface> interfaces = implementation.getServiceInterfaces();
		if (interfaces.size() == 1) {
			addFingerprint(reference, "ddsr.fingerprint", interfaces.get(0));
		} else {
			for (ServiceInterface si : interfaces) {
				addFingerprint(reference, "ddsr.fingerprint." + si.getName(), si);
			}
		}
		// im1 beside the sd1s (ACQUISITION.md §11.1): computed AFTER the
		// publish path rewired the impl onto the live catalog entries, so
		// the contract components are the catalog truth. Providers compare
		// it on reconnect to decide lease-renew vs re-publish.
		String implFingerprint = ServiceImplementationFingerprint.fingerprint(implementation);
		if (implFingerprint != null) {
			StringProperty property = ServicesFactory.eINSTANCE.createStringProperty();
			property.setName("ddsr.impl.fingerprint");
			property.setValue(implFingerprint);
			reference.getProperties().add(property);
		}
	}

	private static void addFingerprint(ServiceReference reference, String propertyName, ServiceInterface si) {
		String fingerprint = ServiceDescriptionFingerprint.fingerprint(si);
		if (fingerprint == null) {
			return;
		}
		StringProperty property = ServicesFactory.eINSTANCE.createStringProperty();
		property.setName(propertyName);
		property.setValue(fingerprint);
		reference.getProperties().add(property);
	}
}
