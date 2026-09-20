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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.LookupBackend;
import org.eclipse.fennec.services.broker.core.ServiceEventReasons;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;
import org.eclipse.fennec.services.fingerprint.ServiceImplementationFingerprint;

/**
 * Publishing, changing and withdrawing what a provider offers.
 *
 * <p>The widest of the concerns, and honestly so: a publish has to ask
 * the catalog to correct the contracts it was sent, mint the
 * reference and registration pair, tell the lookup index, drop a
 * parked twin of the same identity, arm a handover policy if the
 * publish named a predecessor, save, and only then announce. That is
 * one act, not seven, and splitting it further would only spread the
 * order it has to happen in.
 *
 * <p>What it does own alone is the shape of a registration: the pair
 * behind every reference, the decoration that puts the provider's
 * properties and the contract fingerprints onto it, and the removal
 * that four other concerns ask for by the name {@link Retirement}.
 *
 * <p>Two rules run through all of it. A mutation is saved before it is
 * announced, and a save that fails is rolled back exactly, down to the
 * position an implementation had in its provider — positional
 * cross-references make order part of the state.
 */
final class Registrations implements Retirement, Republication {

	private static final java.util.logging.Logger LOG =
			java.util.logging.Logger.getLogger(Registrations.class.getName());

	private final BrokerState state;

	private final LookupBackend lookup;

	private final Announcements announcements;

	private final CatalogStore catalog;

	private final ColdCache cold;

	private final UpdatePolicies policies;

	private final Liveness liveness;

	Registrations(BrokerState state, LookupBackend lookup, Announcements announcements,
			CatalogStore catalog, ColdCache cold, UpdatePolicies policies, Liveness liveness) {
		this.state = state;
		this.lookup = lookup;
		this.announcements = announcements;
		this.catalog = catalog;
		this.cold = cold;
		this.policies = policies;
		this.liveness = liveness;
	}

	@Override
	public Diagnostic republish(ServiceProvider provider, ServiceImplementation implementation) {
		return publishImplementation(provider, implementation);
	}

	void reindex() {
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
			CatalogStore.ContractResolution contracts = catalog.resolveContracts(implementation);
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
				retired = retire(provider, old);
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
			cold.touch(reg);
			// A (re-)publish supersedes a cold twin of the same identity —
			// e.g. a provider restarting while its old registration is
			// parked cold. The stub goes, the file goes.
			cold.dropTwin(provider.getName(), implementation.getName(), implementation.getVersion());
			decorateReference(ref, implementation);

			lookup.serviceAdded(implementation, ref);

			Diagnostic d = state.persist();
			if (DdsrDiagnostics.isError(d)) {
				// Rollback in-memory state on persist failure.
				state.registry().getImplementations().remove(implementation);
				lookup.serviceRemoved(implementation, ref);
				state.registrations().remove(reg);
				return d;
			}
			// Order matters for a consumer holding the old reference: the
			// replaced service goes away, then the new one appears.
			announcements.emit(ServiceEventType.UNREGISTERING, retired, ServiceEventReasons.REPLACED);
			announcements.emit(ServiceEventType.REGISTERED, ref);
			if (predecessor != null) {
				policies.armUpdatePolicy(reg, predecessor);
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
	Diagnostic modifyImplementation(ServiceProvider provider, ServiceImplementation implementation) {
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
			CatalogStore.ContractResolution contracts = catalog.resolveContracts(implementation);
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
			if (DdsrDiagnostics.isError(d)) {
				applyModification(liveImpl, before);
				reference.getProperties().clear();
				reference.getProperties().addAll(decorationBefore);
				lookup.serviceModified(liveImpl, reference);
				return d;
			}
			// Same reference id, same leases: consumers refresh, they do
			// not rebind (UPDATE_POLICY/#55 — MODIFIED is OSGi's MODIFIED).
			announcements.emit(ServiceEventType.MODIFIED, reference);
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
	static void applyModification(ServiceImplementation target, ServiceImplementation source) {
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
	Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
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
			ServiceReference eventReference = Announcements.selfContained(liveProvider, liveImpl, withdrawn);

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
			Liveness.ProviderLease providerLease = toRemove != null ? liveness.forget(toRemove) : null;
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
				policies.forgetSupersession(toRemove);
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
			if (DdsrDiagnostics.isError(d)) {
				// Roll back — a withdrawal that could not be persisted must
				// leave the in-memory state exactly as it was, and no event
				// may be announced for it.
				liveProvider.getImplementations().add(indexInProvider, liveImpl);
				state.registry().getImplementations().add(indexInRegistry, liveImpl);
				if (toRemove != null) {
					state.registrations().add(toRemove);
					toRemove.setUnregistered(false);
					toRemove.getUsingSessions().addAll(leaseHolders);
					liveness.restore(toRemove, providerLease);
					lookup.serviceAdded(liveImpl, toRemove.getReference());
				}
				return d;
			}
			// After the save, never before: a withdrawal that could not
			// be persisted must not be announced.
			announcements.emit(ServiceEventType.UNREGISTERING, eventReference, ServiceEventReasons.WITHDRAWN);
			return d;
		} finally {
			state.writeLock().unlock();
		}
	}
	ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
		Diagnostic d = publishImplementation(provider, implementation);
		if (DdsrDiagnostics.isError(d)) {
			return null;
		}
		state.readLock().lock();
		try {
			return state.registrationOf(implementation);
		} finally {
			state.readLock().unlock();
		}
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
	void rewireOperationRefs(ServiceImplementation implementation) {
		for (org.eclipse.fennec.services.ServiceFlavor flavor : implementation.getFlavors()) {
			for (org.eclipse.fennec.services.ServiceOperationFlavor of : flavor.getOperationFlavors()) {
				org.eclipse.fennec.services.ServiceOperation live = resolveLiveOperation(implementation, of);
				if (live != null && live != of.getOperation()) {
					of.setOperation(live);
				}
			}
		}
	}
	org.eclipse.fennec.services.ServiceOperation resolveLiveOperation(
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
	org.eclipse.fennec.services.ServiceOperation findOperationByName(ServiceImplementation impl, String opName) {
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
	static Integer positionalIndex(org.eclipse.emf.common.util.URI uri) {
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
	 * Drop a stale {@link ServiceImplementation} that is about to be
	 * replaced by a fresher copy with the same (name, version):
	 * detach from its provider, remove from {@code state.registry().implementations},
	 * tell the lookup backend its reference is gone, and clean up
	 * the registrations list.
	 */
	public ServiceReference retire(ServiceProvider provider, ServiceImplementation oldImpl) {
		provider.getImplementations().remove(oldImpl);
		state.registry().getImplementations().remove(oldImpl);
		ServiceRegistration deadReg = state.registrationOf(oldImpl);
		if (deadReg != null) {
			state.registrations().remove(deadReg);
			cold.forget(deadReg);
			liveness.forget(deadReg);
			policies.forgetSupersession(deadReg);
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
	static void decorateReference(ServiceReference reference, ServiceImplementation implementation) {
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
	static void addFingerprint(ServiceReference reference, String propertyName, ServiceInterface si) {
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
