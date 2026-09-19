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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.RegistryKind;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.UpdatePolicy;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.broker.core.ContractAddressing;
import org.eclipse.fennec.services.broker.core.DdsrBroker;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.exception.CatalogEntryAmbiguous;
import org.eclipse.fennec.services.broker.core.exception.CatalogEntryNotFound;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.broker.core.LookupBackend;
import org.eclipse.fennec.services.broker.core.ServiceEventReasons;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;
import org.eclipse.fennec.services.fingerprint.ServiceImplementationFingerprint;

/**
 * In-memory broker implementation with synchronous XMI snapshot
 * persistence after each acknowledged mutation, guarded by a
 * read-write lock. Pure Java — no OSGi imports — so it can be reused
 * by a plain-Java host module later.
 */
public final class DdsrBrokerImpl implements DdsrBroker {

	private static final java.util.logging.Logger LOG =
			java.util.logging.Logger.getLogger(DdsrBrokerImpl.class.getName());

	/** The index a lookup asks; fed by every publish, modify and withdraw. */
	private final LookupBackend lookup;

	/** Where an acknowledged and persisted change is announced. */
	private final EventSink events;

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
	/**
	 * Predecessors superseded by a publish that declared {@code replaces}
	 * (UPDATE_POLICY.md §2), keyed by the predecessor's registration.
	 * Runtime state like {@link #registrationSince}: a broker restart
	 * forgets pending drains and cutovers, which fails safe — nothing is
	 * retired by accident; the successor re-arms the policy by publishing
	 * again with {@code replaces}.
	 */
	private final Map<ServiceRegistration, Supersession> superseded = new IdentityHashMap<>();

	private record Supersession(ServiceRegistration successor, UpdatePolicy policy, Instant cutoverAt) {
	}

	/**
	 * Provider liveness (#52): the last heartbeat per registration and
	 * the interval the provider promised. Opt-in — only registrations
	 * that heartbeat are in here. Runtime state like the sessions: a
	 * broker restart forgets the leases, and the providers' next
	 * heartbeat (404 → republish) rebuilds them.
	 */
	private final Map<ServiceRegistration, ProviderLease> providerLeases = new IdentityHashMap<>();

	private record ProviderLease(Instant lastHeartbeat, long intervalSeconds) {
		Instant lostAt() {
			return lastHeartbeat.plusSeconds(intervalSeconds * MISSED_HEARTBEATS_TO_LOSE);
		}
	}

	/** UPDATE_POLICY.md §4: silence of this many intervals means the provider is gone. */
	public static final int MISSED_HEARTBEATS_TO_LOSE = 2;

	/** UPDATE_POLICY.md §2.3: default failover window of a HARD_CUTOVER. */
	public static final long DEFAULT_CUTOVER_GRACE_MILLIS = 30_000L;
	private volatile long defaultCutoverGraceMillis = DEFAULT_CUTOVER_GRACE_MILLIS;

	/** In-memory remainder of a coldified registration. */
	private record ColdEntry(String key, List<String> interfaceNames,
			List<String> addressingFingerprints, String implementationId,
			String providerName, Path file) {
	}

	/** The registry, the lock that guards it and the file it lives in. */
	private final BrokerState state;

	public DdsrBrokerImpl(Path snapshotPath, LookupBackend lookup) {
		this(snapshotPath, lookup, EventSink.NOOP);
	}

	public DdsrBrokerImpl(Path snapshotPath, LookupBackend lookup, EventSink events) {
		this.state = new BrokerState(snapshotPath);
		this.lookup = lookup;
		this.events = events != null ? events : EventSink.NOOP;
		if (state.rehydrated()) {
			// The providers and implementations came back from the snapshot,
			// but the reference/registration pairs and the lookup index are
			// runtime state and have to be rebuilt.
			reindex();
		}
		loadColdStubs();
	}

	private void reindex() {
		// After loading a snapshot the registry has providers and
		// implementations, but the ServiceReference / ServiceRegistration
		// pairs (and the LookupBackend index) are transient and have to
		// be rebuilt. New UUIDs are assigned on each rehydration —
		// references aren't stable across restarts; consumers must
		// re-lookup after a broker reconnect.
		for (ServiceImplementation impl : state.registry().getImplementations()) {
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
			state.registrations().add(reg);
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

		state.writeLock().lock();
		try {
			// Catalog validation + by-name resolve: incoming
			// impl.serviceInterfaces may be stub SIs (just name+version
			// to keep the publish body self-contained). Match them
			// against the live catalog and rewire the impl ref to the
			// real catalog entry, so subsequent lookups by interface
			// name can match.
			ContractResolution contracts = resolveContracts(implementation);
			if (contracts.refusal() != null) {
				return contracts.refusal();
			}
			boolean anyDeprecated = contracts.deprecationNote() != null;
			String deprecationNote = contracts.deprecationNote();

			// Rewire operation cross-refs on the impl's flavors to point
			// at the LIVE catalog operations. The publisher set them on
			// stub-SI copies sent in the wire bundle; without this
			// rewire, the broker would hold dangling refs to the now-
			// orphaned stub-SI tree and XMI serialise would fail.
			rewireOperationRefs(implementation);

			// Update policy (UPDATE_POLICY.md §2): `replaces` arrives as a
			// stub from the wire. Rewire it to the live predecessor — a
			// dangling non-containment ref would break the XMI save — or
			// clear it with a WARNING when nothing matches: a restarting
			// successor whose predecessor is long gone must still publish.
			ServiceImplementation predecessor = null;
			String replacesNote = null;
			if (implementation.getReplaces() != null) {
				ServiceImplementation wanted = implementation.getReplaces();
				ServiceImplementation live = BrokerState.implementationNamed(
						state.registry().getImplementations(), wanted.getName(), wanted.getVersion());
				boolean ownIdentity = live != null
						&& java.util.Objects.equals(live.getName(), implementation.getName())
						&& java.util.Objects.equals(live.getVersion(), implementation.getVersion());
				if (live == null || ownIdentity) {
					replacesNote = "replaces " + wanted.getName() + "/" + wanted.getVersion()
							+ (live == null
									? " is not published — treated as a plain publish"
									: " names the implementation's own identity — the same (name, version) is deduplicated anyway");
					implementation.setReplaces(null);
				} else {
					implementation.setReplaces(live);
					predecessor = live;
				}
			}

			// Provider dedup: if a provider with the same (name, version)
			// is already in the registry, reuse it instead of adding a
			// duplicate.
			ServiceProvider existing = state.providerNamed(provider.getName(), provider.getVersion());
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
			ServiceImplementation old = BrokerState.implementationNamed(
					provider.getImplementations(), implementation.getName(), implementation.getVersion());
			if (old != null && old != implementation) {
				retired = retireImplementation(provider, old);
			}

			// EMF containment auto-moves the new impl into the (possibly
			// pre-existing) provider; remove from any previous container.
			if (implementation.eContainer() != provider) {
				provider.getImplementations().add(implementation);
			}

			if (!state.registry().getProviders().contains(provider)) {
				state.registry().getProviders().add(provider);
			}
			if (!state.registry().getImplementations().contains(implementation)) {
				state.registry().getImplementations().add(implementation);
			}
			// Non-containment refs (state.registry().providers / .implementations)
			// need their targets to be URI-resolvable for XMI save to work.
			// Park the provider as an additional resource root if it has no
			// container yet — the implementation rides along via containment.
			if (provider.eResource() == null) {
				state.resource().getContents().add(provider);
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
			state.registrations().add(reg);
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

			Diagnostic d = state.persist();
			if (isError(d)) {
				// Rollback in-memory state on persist failure.
				state.registry().getImplementations().remove(implementation);
				lookup.serviceRemoved(implementation, ref);
				state.registrations().remove(reg);
				return d;
			}
			// Order matters for a consumer holding the old reference: the
			// replaced service goes away, then the new one appears.
			emit(ServiceEventType.UNREGISTERING, retired, ServiceEventReasons.REPLACED);
			emit(ServiceEventType.REGISTERED, ref);
			if (predecessor != null) {
				armUpdatePolicy(reg, predecessor);
			}

			if (replacesNote != null) {
				return DdsrDiagnostics.warning(DdsrDiagnostics.CODE_IMPL_REPLACES_NOT_FOUND, replacesNote
						+ (anyDeprecated ? "; interface(s) marked deprecated: " + deprecationNote : ""));
			}
			return anyDeprecated
					? DdsrDiagnostics.warning(DdsrDiagnostics.CODE_INTERFACE_DEPRECATED,
							"interface(s) marked deprecated: " + deprecationNote)
					: DdsrDiagnostics.ok();
		} finally {
			state.writeLock().unlock();
		}
	}

	@Override
	public Diagnostic modifyImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		if (provider == null || implementation == null) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION,
					"provider and implementation must not be null");
		}
		if (implementation.eContainer() != provider) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION,
					"implementation must be contained in provider.implementations");
		}
		state.writeLock().lock();
		try {
			// Over REST the pair is freshly parsed from the wire; resolve the
			// live registration by (name, version) like withdraw does.
			ServiceProvider liveProvider = state.providerNamed(provider.getName(), provider.getVersion());
			ServiceImplementation liveImpl = liveProvider == null ? null
					: BrokerState.implementationNamed(liveProvider.getImplementations(),
							implementation.getName(), implementation.getVersion());
			ServiceRegistration registration = liveImpl == null ? null : state.registrationOf(liveImpl);
			if (liveImpl == null || registration == null || registration.isUnregistered()
					|| !state.registry().getImplementations().contains(liveImpl)) {
				return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED,
						"modify needs a live registration of " + implementation.getName() + "/"
						+ implementation.getVersion() + " — publish instead");
			}
			ContractResolution contracts = resolveContracts(implementation);
			if (contracts.refusal() != null) {
				return contracts.refusal();
			}
			// A modification keeps the contract. resolveContracts rewired the
			// incoming refs onto the live catalog entries, so identity
			// comparison is exactly "same (name, sd1) entries".
			if (!new HashSet<>(implementation.getServiceInterfaces()).equals(new HashSet<>(liveImpl.getServiceInterfaces()))) {
				return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_CONTRACT_CHANGED,
						"the modification changes the implemented contracts (sd1) — a contract change is a new"
						+ " registration: publish it (with replaces for a policy-driven handover)");
			}
			rewireOperationRefs(implementation);

			ServiceImplementation before = EcoreUtil.copy(liveImpl);
			ServiceReference reference = registration.getReference();
			List<Property> decorationBefore = new ArrayList<>(reference.getProperties());
			applyModification(liveImpl, implementation);
			reference.getProperties().clear();
			decorateReference(reference, liveImpl);
			lookup.serviceModified(liveImpl, reference);

			Diagnostic d = state.persist();
			if (isError(d)) {
				applyModification(liveImpl, before);
				reference.getProperties().clear();
				reference.getProperties().addAll(decorationBefore);
				lookup.serviceModified(liveImpl, reference);
				return d;
			}
			// Same reference id, same leases: consumers refresh, they do
			// not rebind (UPDATE_POLICY/#55 — MODIFIED is OSGi's MODIFIED).
			emit(ServiceEventType.MODIFIED, reference);
			return contracts.deprecationNote() != null
					? DdsrDiagnostics.warning(DdsrDiagnostics.CODE_INTERFACE_DEPRECATED,
							"interface(s) marked deprecated: " + contracts.deprecationNote())
					: DdsrDiagnostics.ok();
		} finally {
			state.writeLock().unlock();
		}
	}

	/**
	 * Moves "what is registered" from the wire object onto the live
	 * implementation: flavors, properties, capabilities, description,
	 * implementationId, update-policy knobs. The contract (serviceInterfaces)
	 * is checked equal by the caller and {@code replaces} stays — arming a
	 * policy is a publish-time act.
	 */
	private static void applyModification(ServiceImplementation target, ServiceImplementation source) {
		target.setDescription(source.getDescription());
		target.setImplementationId(source.getImplementationId());
		target.setUpdatePolicy(source.getUpdatePolicy());
		target.setCutoverGraceMillis(source.getCutoverGraceMillis());
		target.getFlavors().clear();
		target.getFlavors().addAll(new ArrayList<>(source.getFlavors()));
		target.getProperties().clear();
		target.getProperties().addAll(new ArrayList<>(source.getProperties()));
		target.getCapabilities().clear();
		target.getCapabilities().addAll(new ArrayList<>(source.getCapabilities()));
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

		state.writeLock().lock();
		try {
			// Over REST the provider/implementation pair is freshly parsed
			// from the wire body and never identical to the live objects —
			// resolve by (name, version) before concluding "not published".
			ServiceProvider liveProvider = provider;
			ServiceImplementation liveImpl = implementation;
			if (!state.registry().getImplementations().contains(liveImpl)) {
				liveProvider = state.providerNamed(provider.getName(), provider.getVersion());
				liveImpl = liveProvider == null ? null
						: BrokerState.implementationNamed(liveProvider.getImplementations(),
								implementation.getName(), implementation.getVersion());
				if (liveImpl == null || !state.registry().getImplementations().contains(liveImpl)) {
					return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED,
							"implementation is not currently published");
				}
			}

			// Find and drop the matching registration / reference pair
			// (deterministic: insertion order of the registrations list).
			ServiceRegistration toRemove = state.registrationOf(liveImpl);
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
			int indexInRegistry = state.registry().getImplementations().indexOf(liveImpl);

			// A withdrawn registration must never stay acquired: release
			// the leases (holders re-acquire live refs on their next PUT).
			// Remembered for the persist-failure rollback below.
			List<ConsumerSession> leaseHolders = toRemove != null
					? List.copyOf(toRemove.getUsingSessions())
					: List.of();
			ProviderLease providerLease = toRemove != null ? providerLeases.remove(toRemove) : null;
			if (toRemove != null) {
				lookup.serviceRemoved(liveImpl, toRemove.getReference());
				state.registrations().remove(toRemove);
				toRemove.getUsingSessions().clear();
				// The transient links (implementation/provider/reference)
				// stay on the dead pair for event building and rollback,
				// but the flag makes it non-resolvable: a withdrawn
				// reference must stop answering getImplementationForReference
				// (parity with the former side-map removal).
				toRemove.setUnregistered(true);
				// A withdrawn predecessor needs no drain; a withdrawn
				// successor cancels the drain — the predecessor stays.
				forgetSupersession(toRemove);
			}

			state.registry().getImplementations().remove(liveImpl);
			// Detach from the provider as well, exactly like the republish
			// path does in retireImplementation. Dropping it only from
			// state.registry().implementations leaves it in the provider's
			// containment, which has two consequences: the withdrawn
			// implementation keeps being written to every snapshot, and it
			// keeps a non-containment reference to its catalog interface —
			// so a later removeCatalogEntry pulls that interface out of its
			// containment while something in the resource still points at
			// it, and every subsequent save fails with "not contained in a
			// resource".
			liveProvider.getImplementations().remove(liveImpl);

			Diagnostic d = state.persist();
			if (isError(d)) {
				// Roll back — a withdrawal that could not be persisted must
				// leave the in-memory state exactly as it was, and no event
				// may be announced for it.
				liveProvider.getImplementations().add(indexInProvider, liveImpl);
				state.registry().getImplementations().add(indexInRegistry, liveImpl);
				if (toRemove != null) {
					state.registrations().add(toRemove);
					toRemove.setUnregistered(false);
					toRemove.getUsingSessions().addAll(leaseHolders);
					if (providerLease != null) {
						providerLeases.put(toRemove, providerLease);
					}
					lookup.serviceAdded(liveImpl, toRemove.getReference());
				}
				return d;
			}
			// After the save, never before: a withdrawal that could not
			// be persisted must not be announced.
			emit(ServiceEventType.UNREGISTERING, eventReference, ServiceEventReasons.WITHDRAWN);
			return d;
		} finally {
			state.writeLock().unlock();
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
		state.readLock().lock();
		try {
			return state.registrationOf(implementation);
		} finally {
			state.readLock().unlock();
		}
	}

	// ============================================================
	// Consumer operations
	// ============================================================

	@Override
	public ServiceReference getServiceReference(String interfaceName) {
		touchAndRehydrate(interfaceName);
		state.readLock().lock();
		try {
			return lookup.getServiceReference(interfaceName, null, null);
		} finally {
			state.readLock().unlock();
		}
	}

	@Override
	public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		touchAndRehydrate(interfaceName);
		state.readLock().lock();
		try {
			return withoutDraining(filterByRequestedFingerprint(
					lookup.getServiceReferences(interfaceName, filter, capability), capability));
		} finally {
			state.readLock().unlock();
		}
	}

	@Override
	public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		touchAndRehydrate(interfaceName);
		state.readLock().lock();
		try {
			return filterByRequestedFingerprint(
					lookup.getAllServiceReferences(interfaceName, filter, capability), capability);
		} finally {
			state.readLock().unlock();
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
		state.writeLock().lock();
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
			state.registry().getCatalog().add(serviceInterface);
			Diagnostic d = state.persist();
			if (isError(d)) {
				// Roll back so a failed save does not leave the in-memory
				// catalog ahead of the snapshot — see the note on
				// rollbackOnPersistFailure.
				state.registry().getCatalog().remove(serviceInterface);
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
			state.writeLock().unlock();
		}
	}

	@Override
	public Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		if (serviceInterface == null || serviceInterface.getName() == null) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND,
					"serviceInterface and serviceInterface.name must not be null");
		}
		state.writeLock().lock();
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

			Diagnostic d = state.persist();
			if (isError(d)) {
				inCatalog.setStatus(previousStatus);
				inCatalog.setDeprecationReason(previousReason);
				inCatalog.setReplacedBy(previousReplacedBy);
			}
			return d;
		} finally {
			state.writeLock().unlock();
		}
	}

	@Override
	public Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		if (serviceInterface == null || serviceInterface.getName() == null) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND,
					"serviceInterface and serviceInterface.name must not be null");
		}
		state.writeLock().lock();
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
			for (ServiceImplementation impl : state.registry().getImplementations()) {
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
			int previousIndex = state.registry().getCatalog().indexOf(inCatalog);
			state.registry().getCatalog().remove(inCatalog);
			Diagnostic d = state.persist();
			if (isError(d)) {
				state.registry().getCatalog().add(previousIndex, inCatalog);
			}
			return d;
		} finally {
			state.writeLock().unlock();
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
		state.readLock().lock();
		try {
			EcoreUtil.Copier copier = new EcoreUtil.Copier();
			RemoteServiceRegistry registryCopy = (RemoteServiceRegistry) copier.copy(state.registry());
			copier.copyAll(new ArrayList<>(state.registry().getProviders()));
			copier.copyReferences();

			Resource holder = new XMIResourceImpl(URI.createURI("services:registry"));
			holder.getContents().add(registryCopy);
			for (ServiceProvider provider : state.registry().getProviders()) {
				EObject providerCopy = copier.get(provider);
				if (providerCopy != null && providerCopy.eContainer() == null) {
					holder.getContents().add(providerCopy);
				}
			}
			return registryCopy;
		} finally {
			state.readLock().unlock();
		}
	}

	/**
	 * The live registry object — package-private for tests and internal
	 * callers that need identity rather than a snapshot. Any read of it
	 * outside the broker's own locking is the caller's race to lose.
	 */
	RemoteServiceRegistry liveRegistry() {
		return state.registry();
	}

	@Override
	public Diagnostic snapshot() {
		state.readLock().lock();
		try {
			return state.persist();
		} finally {
			state.readLock().unlock();
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
		state.readLock().lock();
		try {
			// The transient link survives on a withdrawn pair (needed for
			// event building and rollback) — the flag is what says "dead".
			return reg.isUnregistered() ? null : reg.getImplementation();
		} finally {
			state.readLock().unlock();
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
		state.writeLock().lock();
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
					ServiceRegistration registration = state.registrationWithReferenceId(referenceId);
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
			state.writeLock().unlock();
		}
	}

	@Override
	public Diagnostic deleteSession(String consumerId) {
		if (consumerId == null || consumerId.isBlank()) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_SESSION_INVALID,
					"consumerId must not be null or blank");
		}
		state.writeLock().lock();
		try {
			ConsumerSession removed = sessions.remove(consumerId);
			if (removed == null) {
				// Idempotent: a shutdown-notify may race the TTL expiry.
				return DdsrDiagnostics.ok("no session for '" + consumerId + "' — nothing to release");
			}
			releaseAcquisitions(removed);
			return DdsrDiagnostics.ok("session removed, all acquisitions released");
		} finally {
			state.writeLock().unlock();
		}
	}

	@Override
	public Optional<SessionSnapshot> getSession(String consumerId) {
		if (consumerId == null || consumerId.isBlank()) {
			return Optional.empty();
		}
		state.readLock().lock();
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
			state.readLock().unlock();
		}
	}

	@Override
	public int expireSessions(Instant cutoff) {
		if (cutoff == null) {
			return 0;
		}
		state.writeLock().lock();
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
			state.writeLock().unlock();
		}
	}

	@Override
	public int sessionCount() {
		state.readLock().lock();
		try {
			return sessions.size();
		} finally {
			state.readLock().unlock();
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
		state.writeLock().lock();
		try {
			Instant now = Instant.now();
			registrationSince.keySet().retainAll(new java.util.HashSet<>(state.registrations()));
			int moved = 0;
			for (ServiceRegistration reg : new ArrayList<>(state.registrations())) {
				if (reg.isUnregistered() || reg.getImplementation() == null || reg.getProvider() == null) {
					continue;
				}
				if (isPartyOfSupersession(reg)) {
					// The policy sweep owns the predecessor's fate, and the
					// successor must stay hot for consumers migrating to it.
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
			state.writeLock().unlock();
		}
	}

	/** Number of cold entries currently parked on disk. */
	public int coldCount() {
		state.readLock().lock();
		try {
			return coldEntries.size();
		} finally {
			state.readLock().unlock();
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

		// Event material before the detach (like withdraw and the policy
		// retire): after retireImplementation the lookup no longer resolves
		// the implementation, and a bare reference would send the event to
		// MQTT's _unknown topic / SSE's deliver-to-all fallback (#54 matrix).
		ServiceReference eventReference = selfContainedEventReference(provider, impl, reg.getReference());
		ServiceReference retiredRef = retireImplementation(provider, impl);
		registrationSince.remove(reg);
		coldEntries.put(key, new ColdEntry(key, names, fingerprints,
				impl.getImplementationId(), provider.getName(), file));
		Diagnostic d = state.persist();
		if (isError(d)) {
			// Degraded but recoverable: the entry is discoverable through
			// its stub, and rehydration republishes it.
			LOG.warning("[DDSR] persist after coldify failed for " + key + ": " + d.getMessage());
		}
		emit(ServiceEventType.UNREGISTERING, eventReference != null ? eventReference : retiredRef,
				ServiceEventReasons.COLDIFIED);
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
		state.writeLock().lock();
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
			state.writeLock().unlock();
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
		Path parent = state.snapshotPath().toAbsolutePath().getParent();
		return parent.resolve(state.snapshotPath().getFileName() + ".cold");
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
		for (ServiceInterface si : state.registry().getCatalog()) {
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


	/**
	 * Drop a stale {@link ServiceImplementation} that is about to be
	 * replaced by a fresher copy with the same (name, version):
	 * detach from its provider, remove from {@code state.registry().implementations},
	 * tell the lookup backend its reference is gone, and clean up
	 * the registrations list.
	 */
	private ServiceReference retireImplementation(ServiceProvider provider, ServiceImplementation oldImpl) {
		provider.getImplementations().remove(oldImpl);
		state.registry().getImplementations().remove(oldImpl);
		ServiceRegistration deadReg = state.registrationOf(oldImpl);
		if (deadReg != null) {
			state.registrations().remove(deadReg);
			registrationSince.remove(deadReg);
			providerLeases.remove(deadReg);
			forgetSupersession(deadReg);
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

	private record ContractResolution(Diagnostic refusal, String deprecationNote) {
	}

	/**
	 * Shared by publish and modify: the incoming impl.serviceInterfaces
	 * may be stub SIs (just name+version, or an href to the catalog URL)
	 * to keep the wire body self-contained. Match them against the live
	 * catalog and rewire the impl ref to the real catalog entry, so
	 * subsequent lookups by interface name can match. Refuses unknown or
	 * unidentifiable interfaces; reports deprecated ones as a note.
	 */
	private ContractResolution resolveContracts(ServiceImplementation implementation) {
		StringBuilder deprecationNote = new StringBuilder();
		java.util.List<ServiceInterface> sis = implementation.getServiceInterfaces();
		for (int i = 0; i < sis.size(); i++) {
			ServiceInterface si = sis.get(i);
			String name = catalogNameOf(si);
			if (name == null || name.isBlank()) {
				return new ContractResolution(DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG,
						"impl.serviceInterfaces[" + i + "] has no identifiable name — "
						+ "send it as <serviceInterfaces href=\"<broker>/catalog/{name}\"/> "
						+ "or as a sibling root with name+version set"), null);
			}
			CatalogResolution resolution = resolveCatalogEntry(si, name,
					DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG);
			if (resolution.refusal() != null) {
				return new ContractResolution(resolution.refusal(), null);
			}
			ServiceInterface inCatalog = resolution.entry();
			if (inCatalog.getStatus() == CatalogStatus.DEPRECATED) {
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
				rewireContractReferences(implementation, si, inCatalog);
			}
		}
		return new ContractResolution(null, deprecationNote.length() == 0 ? null : deprecationNote.toString());
	}


	/**
	 * Re-point everything in the implementation that referenced the
	 * published copy of a contract at the catalog entry that replaced it.
	 *
	 * <p>A publish body carries the contract along, so the broker sees two
	 * objects for one contract: the copy in the payload and the entry in
	 * its catalog. {@link #resolveContracts} keeps the catalog entry —
	 * anything else would let a publisher redefine a contract by
	 * announcing an implementation of it. But a flavor does not only
	 * reference the contract, it references single parameters and
	 * exceptions <em>inside</em> it, and those references would still
	 * point into the payload copy, which is now attached to nothing.
	 * Saving the registry then fails with "not contained in a resource".
	 *
	 * <p>The correspondence is the containment path: the two contracts are
	 * the same contract, and a catalog entry is only accepted when its
	 * fingerprint says so, which makes the path from the contract to a
	 * parameter the same on both sides.
	 *
	 * <p>This stayed invisible while the TypeScript client omitted
	 * single-valued cross-references from what it published (#79) — there
	 * simply was no reference into the contract to rewire.
	 */
	private static void rewireContractReferences(ServiceImplementation implementation,
			ServiceInterface published, ServiceInterface inCatalog) {
		List<EObject> elements = new ArrayList<>();
		elements.add(implementation);
		implementation.eAllContents().forEachRemaining(elements::add);
		for (EObject element : elements) {
			for (EReference reference : element.eClass().getEAllReferences()) {
				if (reference.isContainment() || reference.isDerived() || !element.eIsSet(reference)) {
					continue;
				}
				if (reference.isMany()) {
					@SuppressWarnings("unchecked")
					List<EObject> values = (List<EObject>) element.eGet(reference);
					for (int i = 0; i < values.size(); i++) {
						EObject replacement = counterpart(values.get(i), published, inCatalog);
						if (replacement != null) {
							values.set(i, replacement);
						}
					}
				} else {
					EObject replacement = counterpart((EObject) element.eGet(reference), published, inCatalog);
					if (replacement != null) {
						element.eSet(reference, replacement);
					}
				}
			}
		}
	}

	/**
	 * The object at the same place inside the catalog entry, or
	 * {@code null} when the reference does not point into the published
	 * copy at all and is to be left alone.
	 */
	private static EObject counterpart(EObject target, ServiceInterface published, ServiceInterface inCatalog) {
		if (target == null || target.eIsProxy() || !EcoreUtil.isAncestor(published, target)) {
			return null;
		}
		if (target == published) {
			return inCatalog;
		}
		String path = EcoreUtil.getRelativeURIFragmentPath(published, target);
		EObject counterpart = EcoreUtil.getEObject(inCatalog, path);
		if (counterpart == null) {
			LOG.warning("[DDSR] catalog entry " + inCatalog.getName() + " has nothing at " + path
					+ " — a reference of the published implementation is dropped");
		}
		return counterpart;
	}


	// ============================================================
	// The catalog as the wire states it (broker-catalog-api.xmi, #76)
	//
	// The contract describes what goes over HTTP, and these are the
	// operations it names. They were the REST resource's methods until
	// the generic distribution started serving that contract — and what
	// they do is broker behaviour, not transport: which entry a name and
	// a fingerprint address, and what "not found" or "ambiguous" means
	// is the catalog's business.
	// ============================================================

	/** The registry as it stands, catalog included. */
	public RemoteServiceRegistry listCatalog() {
		return getRegistry();
	}

	/**
	 * One contract by name.
	 *
	 * <p>Several contracts may share a name — they are told apart by
	 * content fingerprint (ACQUISITION.md §11.2) — so a bare name is
	 * only answered while it addresses one.
	 */
	public ServiceInterface getCatalogEntry(String name, String fingerprint) {
		state.readLock().lock();
		try {
			List<ServiceInterface> named = entriesNamed(name);
			if (fingerprint != null && !fingerprint.isBlank()) {
				for (ServiceInterface candidate : named) {
					if (ContractAddressing.matches(candidate, fingerprint)) {
						return candidate;
					}
				}
				throw new CatalogEntryNotFound(
						"no catalog entry named '" + name + "' with fingerprint " + fingerprint);
			}
			if (named.isEmpty()) {
				throw new CatalogEntryNotFound("no catalog entry named '" + name + "'");
			}
			if (named.size() > 1) {
				throw new CatalogEntryAmbiguous("interface name '" + name + "' names " + named.size()
						+ " coexisting contracts — address one via its fingerprint; available: "
						+ named.stream().map(ContractAddressing::fingerprint).collect(Collectors.joining(", ")));
			}
			return named.get(0);
		} finally {
			state.readLock().unlock();
		}
	}

	/** Soft-deprecate the contract a name and fingerprint address. */
	public Diagnostic deprecateCatalogEntry(String name, String fingerprint, ServiceInterface governance,
			String requestor) {
		ServiceInterface target = governanceTarget(name, fingerprint);
		if (governance != null) {
			// Carry the caller's governance fields onto the resolved
			// target; nothing else about what they sent is taken.
			if (governance.getDeprecationReason() != null) {
				target.setDeprecationReason(governance.getDeprecationReason());
			}
			if (governance.getReplacedBy() != null) {
				target.setReplacedBy(governance.getReplacedBy());
			}
		}
		return deprecateCatalogEntry(target, requestor);
	}

	/** Remove the contract a name and fingerprint address. */
	public Diagnostic removeCatalogEntry(String name, String fingerprint, String requestor) {
		return removeCatalogEntry(governanceTarget(name, fingerprint), requestor);
	}

	/**
	 * What a governance call acts on: with a fingerprint the entry
	 * itself, so content addressing hits it even where several contracts
	 * share the name; without one a name-only stub, which leaves the
	 * unambiguity rule to the operation that is about to run.
	 */
	private ServiceInterface governanceTarget(String name, String fingerprint) {
		if (fingerprint == null || fingerprint.isBlank()) {
			ServiceInterface stub = ServicesFactory.eINSTANCE.createServiceInterface();
			stub.setName(name);
			return stub;
		}
		return getCatalogEntry(name, fingerprint);
	}

	private List<ServiceInterface> entriesNamed(String name) {
		List<ServiceInterface> named = new ArrayList<>();
		for (ServiceInterface entry : state.registry().getCatalog()) {
			if (entry.getName() != null && entry.getName().equals(name)) {
				named.add(entry);
			}
		}
		return named;
	}

	// ============================================================
	// Update policies (UPDATE_POLICY.md §2)
	// ============================================================

	/** Broker default for {@code cutoverGraceMillis} when the successor leaves it at 0. */
	public void setDefaultCutoverGraceMillis(long millis) {
		this.defaultCutoverGraceMillis = Math.max(0L, millis);
	}

	/**
	 * Successor override, else the strictest default among its interfaces
	 * (HARD_CUTOVER &gt; DEPRECATE_AND_DRAIN &gt; EVERGREEN), else the broker
	 * default DEPRECATE_AND_DRAIN. {@code UNSPECIFIED} means "inherit" on
	 * both levels.
	 */
	static UpdatePolicy effectiveUpdatePolicy(ServiceImplementation implementation) {
		UpdatePolicy own = implementation.getUpdatePolicy();
		if (own != null && own != UpdatePolicy.UNSPECIFIED) {
			return own;
		}
		UpdatePolicy strictest = UpdatePolicy.UNSPECIFIED;
		for (ServiceInterface si : implementation.getServiceInterfaces()) {
			UpdatePolicy candidate = si.getUpdatePolicy();
			if (candidate != null && candidate.getValue() > strictest.getValue()) {
				strictest = candidate;
			}
		}
		return strictest == UpdatePolicy.UNSPECIFIED ? UpdatePolicy.DEPRECATE_AND_DRAIN : strictest;
	}

	/**
	 * Arms the update policy for a successor that named a live
	 * predecessor. Runs after the successor's REGISTERED went out, so a
	 * consumer reacting to UPGRADE_AVAILABLE already finds the successor.
	 * The predecessor itself is only retired by {@link #advanceUpdatePolicies}.
	 */
	private void armUpdatePolicy(ServiceRegistration successor, ServiceImplementation predecessorImpl) {
		ServiceRegistration predecessor = state.registrationOf(predecessorImpl);
		if (predecessor == null || predecessor.isUnregistered() || predecessor == successor) {
			return; // parked cold or already gone — nothing to drain
		}
		UpdatePolicy policy = effectiveUpdatePolicy(successor.getImplementation());
		switch (policy) {
		case EVERGREEN -> LOG.fine(() -> "[DDSR] " + identityOf(successor) + " replaces "
				+ identityOf(predecessor) + " under EVERGREEN — both stay registered");
		case HARD_CUTOVER -> {
			long own = successor.getImplementation().getCutoverGraceMillis();
			long grace = own > 0 ? own : defaultCutoverGraceMillis;
			superseded.put(predecessor, new Supersession(successor, policy, Instant.now().plusMillis(grace)));
			LOG.info("[DDSR] " + identityOf(successor) + " replaces " + identityOf(predecessor)
					+ " under HARD_CUTOVER — retiring the predecessor in " + grace + " ms");
		}
		default -> {
			// DEPRECATE_AND_DRAIN: hidden from new lookups, kept alive by
			// its leases, UPGRADE_AVAILABLE as the hint to migrate.
			superseded.put(predecessor, new Supersession(successor, UpdatePolicy.DEPRECATE_AND_DRAIN, null));
			LOG.info("[DDSR] " + identityOf(successor) + " replaces " + identityOf(predecessor)
					+ " under DEPRECATE_AND_DRAIN — draining " + predecessor.getUsingSessions().size() + " lease(s)");
			emit(ServiceEventType.UPGRADE_AVAILABLE, predecessor.getReference());
		}
		}
	}

	/**
	 * Advances the update-policy state machine (UPDATE_POLICY.md §2):
	 * retires every DEPRECATE_AND_DRAIN predecessor whose last lease is
	 * gone and every HARD_CUTOVER predecessor whose grace window has
	 * elapsed at {@code now}, announcing each as {@code UNREGISTERING}
	 * followed by {@code RETIRED} (reason REPLACED resp. CUTOVER).
	 * Idempotent. Like {@link #coldifyIdle} a maintenance entry point of
	 * the implementation, deliberately not part of the client-facing
	 * {@code BrokerImplementations} contract.
	 *
	 * @return the number of predecessors retired in this pass
	 */
	public int advanceUpdatePolicies(Instant now) {
		if (now == null) {
			return 0;
		}
		state.writeLock().lock();
		try {
			int retired = 0;
			for (Map.Entry<ServiceRegistration, Supersession> entry : new ArrayList<>(superseded.entrySet())) {
				ServiceRegistration predecessor = entry.getKey();
				Supersession supersession = entry.getValue();
				if (predecessor.isUnregistered() || !state.registrations().contains(predecessor)) {
					superseded.remove(predecessor); // withdrawn meanwhile
					continue;
				}
				ServiceRegistration successor = supersession.successor();
				if (successor.isUnregistered() || !state.registrations().contains(successor)) {
					superseded.remove(predecessor);
					LOG.info("[DDSR] successor of " + identityOf(predecessor)
							+ " is gone — cancelling its " + supersession.policy().getLiteral());
					continue;
				}
				boolean due = supersession.policy() == UpdatePolicy.HARD_CUTOVER
						? !now.isBefore(supersession.cutoverAt())
						: predecessor.getUsingSessions().isEmpty();
				if (!due) {
					continue;
				}
				ServiceProvider provider = predecessor.getProvider();
				ServiceImplementation impl = predecessor.getImplementation();
				// Event material before the detach, as in the withdraw path.
				ServiceReference eventReference = selfContainedEventReference(provider, impl, predecessor.getReference());
				ServiceReference retiredRef = retireImplementation(provider, impl);
				ServiceImplementation successorImpl = successor.getImplementation();
				if (successorImpl != null && successorImpl.getReplaces() == impl) {
					successorImpl.setReplaces(null); // would dangle in the snapshot
				}
				superseded.remove(predecessor);
				Diagnostic d = state.persist();
				if (isError(d)) {
					LOG.warning("[DDSR] persist after policy retire of " + identityOf(predecessor)
							+ " failed: " + d.getMessage());
				}
				String reason = supersession.policy() == UpdatePolicy.HARD_CUTOVER
						? ServiceEventReasons.CUTOVER
						: ServiceEventReasons.REPLACED;
				ServiceReference announced = eventReference != null ? eventReference : retiredRef;
				emit(ServiceEventType.UNREGISTERING, announced, reason);
				emit(ServiceEventType.RETIRED, announced, reason);
				LOG.info("[DDSR] retired " + identityOf(predecessor) + " (" + reason + ")");
				retired++;
			}
			return retired;
		} finally {
			state.writeLock().unlock();
		}
	}

	// ============================================================
	// Provider liveness (#52, UPDATE_POLICY.md §4)
	// ============================================================

	@Override
	public Diagnostic heartbeat(String referenceId, long intervalSeconds) {
		if (referenceId == null || referenceId.isBlank()) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED,
					"referenceId must not be null or blank");
		}
		if (intervalSeconds <= 0) {
			return DdsrDiagnostics.error(DdsrDiagnostics.CODE_HEARTBEAT_INVALID,
					"intervalSeconds must be positive, got " + intervalSeconds);
		}
		state.writeLock().lock();
		try {
			ServiceRegistration registration = state.registrationWithReferenceId(referenceId);
			if (registration == null) {
				// Unknown here means: restart, coldified, retired for silence
				// or replaced — in every case the provider has to publish
				// again to be listed. 404 on the wire, and that is the cue.
				return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED,
						"no live registration for reference '" + referenceId + "' — publish again");
			}
			boolean armed = providerLeases.containsKey(registration);
			providerLeases.put(registration, new ProviderLease(Instant.now(), intervalSeconds));
			if (!armed) {
				LOG.info("[DDSR] provider liveness armed for " + identityOf(registration)
						+ " — lost after " + (intervalSeconds * MISSED_HEARTBEATS_TO_LOSE) + " s of silence");
			}
			// Runtime state, like the sessions: no persist, no event.
			return DdsrDiagnostics.ok("heartbeat accepted, lost after "
					+ (intervalSeconds * MISSED_HEARTBEATS_TO_LOSE) + " s of silence");
		} finally {
			state.writeLock().unlock();
		}
	}

	/**
	 * Retires every registration whose provider promised heartbeats and
	 * has been silent for {@link #MISSED_HEARTBEATS_TO_LOSE} intervals at
	 * {@code now}, announcing {@code UNREGISTERING} followed by
	 * {@code RETIRED} with reason {@code PROVIDER_LOST}. A lost predecessor
	 * of a supersession counts as retired (the drain is over), a lost
	 * successor cancels the drain and the predecessor becomes visible to
	 * lookups again. Maintenance entry point like {@link #coldifyIdle}
	 * and {@link #advanceUpdatePolicies}, not part of the client-facing
	 * contract.
	 *
	 * @return the number of registrations retired in this pass
	 */
	public int retireLostProviders(Instant now) {
		if (now == null) {
			return 0;
		}
		state.writeLock().lock();
		try {
			providerLeases.keySet().retainAll(new HashSet<>(state.registrations()));
			int retired = 0;
			for (Map.Entry<ServiceRegistration, ProviderLease> entry : new ArrayList<>(providerLeases.entrySet())) {
				ServiceRegistration registration = entry.getKey();
				if (registration.isUnregistered() || now.isBefore(entry.getValue().lostAt())) {
					continue;
				}
				ServiceProvider provider = registration.getProvider();
				ServiceImplementation impl = registration.getImplementation();
				if (provider == null || impl == null) {
					providerLeases.remove(registration);
					continue;
				}
				// Event material before the detach, as in the withdraw path.
				ServiceReference eventReference = selfContainedEventReference(provider, impl, registration.getReference());
				ServiceReference retiredRef = retireImplementation(provider, impl);
				// A successor's `replaces` would dangle in the snapshot.
				for (ServiceRegistration other : state.registrations()) {
					ServiceImplementation otherImpl = other.getImplementation();
					if (otherImpl != null && otherImpl.getReplaces() == impl) {
						otherImpl.setReplaces(null);
					}
				}
				Diagnostic d = state.persist();
				if (isError(d)) {
					LOG.warning("[DDSR] persist after retiring lost provider " + identityOf(registration)
							+ " failed: " + d.getMessage());
				}
				ServiceReference announced = eventReference != null ? eventReference : retiredRef;
				emit(ServiceEventType.UNREGISTERING, announced, ServiceEventReasons.PROVIDER_LOST);
				emit(ServiceEventType.RETIRED, announced, ServiceEventReasons.PROVIDER_LOST);
				LOG.warning("[DDSR] retired " + identityOf(registration) + " — provider silent since "
						+ entry.getValue().lastHeartbeat() + " (PROVIDER_LOST)");
				retired++;
			}
			return retired;
		} finally {
			state.writeLock().unlock();
		}
	}

	/** Number of registrations currently under liveness supervision. */
	public int providerLeaseCount() {
		state.readLock().lock();
		try {
			return providerLeases.size();
		} finally {
			state.readLock().unlock();
		}
	}

	/**
	 * UPDATE_POLICY.md §2.2: a predecessor in DEPRECATE_AND_DRAIN is
	 * invisible to new lookups (its holders keep it through their leases);
	 * during a HARD_CUTOVER grace window both stay visible (§2.3 phase 2).
	 * {@code getAllServiceReferences} waives this like it waives flavor
	 * matching.
	 */
	private List<ServiceReference> withoutDraining(List<ServiceReference> references) {
		if (superseded.isEmpty() || references.isEmpty()) {
			return references;
		}
		List<ServiceReference> visible = new ArrayList<>(references.size());
		for (ServiceReference reference : references) {
			Supersession supersession = supersessionOf(reference);
			if (supersession == null || supersession.policy() != UpdatePolicy.DEPRECATE_AND_DRAIN) {
				visible.add(reference);
			}
		}
		return visible;
	}

	private Supersession supersessionOf(ServiceReference reference) {
		for (Map.Entry<ServiceRegistration, Supersession> entry : superseded.entrySet()) {
			ServiceReference candidate = entry.getKey().getReference();
			if (candidate == reference
					|| (candidate != null && reference.getId() != null && reference.getId().equals(candidate.getId()))) {
				return entry.getValue();
			}
		}
		return null;
	}

	private boolean isPartyOfSupersession(ServiceRegistration registration) {
		if (superseded.containsKey(registration)) {
			return true;
		}
		for (Supersession supersession : superseded.values()) {
			if (supersession.successor() == registration) {
				return true;
			}
		}
		return false;
	}

	private void forgetSupersession(ServiceRegistration registration) {
		superseded.remove(registration);
		superseded.values().removeIf(supersession -> supersession.successor() == registration);
	}

	private static String identityOf(ServiceRegistration registration) {
		ServiceImplementation impl = registration.getImplementation();
		return impl == null ? "?" : impl.getName() + "/" + impl.getVersion();
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
		emit(type, reference, null);
	}

	/**
	 * Emits a lifecycle event. {@code reason} is one of the
	 * {@link ServiceEventReasons} tokens for an {@code UNREGISTERING},
	 * {@code null} for {@code REGISTERED} — see the model documentation
	 * of {@code ServiceEvent.reasonCode}.
	 */
	private void emit(ServiceEventType type, ServiceReference reference, String reason) {
		if (reference == null) {
			return;
		}
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(type);
		event.setReference(reference);
		event.setTimestamp(new Date());
		event.setReasonCode(reason);
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
