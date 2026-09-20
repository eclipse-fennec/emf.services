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
	private final Announcements announcements;

	/** Who has acquired what. */
	private final Sessions sessions;

	/** Whether a provider is still there. */
	private final Liveness liveness;

	/** What happens to the service a new one replaces. */
	private final UpdatePolicies policies;

	/** Registrations nobody has looked at for a while. */
	private final ColdCache cold;

	/** The contracts the broker knows. */
	private final CatalogStore catalog;

	/** Answering who serves an interface. */
	private final Lookups lookups;

	/** Publishing, changing and withdrawing what a provider offers. */
	private final Registrations registrations;


	/** The registry, the lock that guards it and the file it lives in. */
	private final BrokerState state;

	public DdsrBrokerImpl(Path snapshotPath, LookupBackend lookup) {
		this(snapshotPath, lookup, EventSink.NOOP);
	}

	public DdsrBrokerImpl(Path snapshotPath, LookupBackend lookup, EventSink events) {
		this.state = new BrokerState(snapshotPath);
		this.lookup = lookup;
		this.announcements = new Announcements(events);
		this.sessions = new Sessions(state);
		// The four below need a way to take a registration away, and the
		// cold cache needs a way to put one back. Both live in
		// Registrations, which in turn needs all four — so they are wired
		// with method references that resolve when they are called, not
		// when they are handed over.
		this.liveness = new Liveness(state, announcements, this::retire);
		this.policies = new UpdatePolicies(state, announcements, this::retire);
		this.cold = new ColdCache(state, announcements, this::retire, this::republish, policies);
		this.catalog = new CatalogStore(state, cold);
		this.lookups = new Lookups(state, lookup, cold, policies);
		this.registrations = new Registrations(state, lookup, announcements, catalog, cold, policies, liveness);
		if (state.rehydrated()) {
			// The providers and implementations came back from the
			// snapshot, but the reference and registration pairs and the
			// lookup index are runtime state and have to be rebuilt.
			registrations.reindex();
		}
		cold.loadColdStubs();
	}

	private ServiceReference retire(ServiceProvider provider, ServiceImplementation implementation) {
		return registrations.retire(provider, implementation);
	}

	private Diagnostic republish(ServiceProvider provider, ServiceImplementation implementation) {
		return registrations.republish(provider, implementation);
	}

	// ============================================================
	// Provider operations — see Registrations
	// ============================================================

	@Override
	public Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		return registrations.publishImplementation(provider, implementation);
	}

	@Override
	public Diagnostic modifyImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		return registrations.modifyImplementation(provider, implementation);
	}

	@Override
	public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		return registrations.withdrawImplementation(provider, implementation);
	}

	@Override
	public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
		return registrations.registerService(provider, implementation);
	}

	// ============================================================
	// Provider operations
	// ============================================================







	// ============================================================
	// Consumer operations
	// ============================================================




	// ============================================================
	// The registry itself — see BrokerState
	// ============================================================

	@Override
	public RemoteServiceRegistry getRegistry() {
		return state.getRegistry();
	}

	@Override
	public Diagnostic snapshot() {
		return state.snapshot();
	}

	// ============================================================
	// Consumer lookups — see Lookups
	// ============================================================

	@Override
	public ServiceReference getServiceReference(String interfaceName) {
		return lookups.getServiceReference(interfaceName);
	}

	@Override
	public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		return lookups.getServiceReferences(interfaceName, filter, capability);
	}

	@Override
	public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		return lookups.getAllServiceReferences(interfaceName, filter, capability);
	}

	@Override
	public ServiceImplementation getImplementationForReference(ServiceReference reference) {
		return lookups.getImplementationForReference(reference);
	}

	// ============================================================
	// Catalog — see CatalogStore
	// ============================================================

	@Override
	public Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return catalog.addCatalogEntry(serviceInterface, requestor);
	}

	@Override
	public Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return catalog.deprecateCatalogEntry(serviceInterface, requestor);
	}

	@Override
	public Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return catalog.removeCatalogEntry(serviceInterface, requestor);
	}

	public Diagnostic deprecateCatalogEntry(String name, String fingerprint, ServiceInterface governance,
			String requestor) {
		return catalog.deprecateCatalogEntry(name, fingerprint, governance, requestor);
	}

	public Diagnostic removeCatalogEntry(String name, String fingerprint, String requestor) {
		return catalog.removeCatalogEntry(name, fingerprint, requestor);
	}

	public ServiceInterface getCatalogEntry(String name, String fingerprint) {
		return catalog.getCatalogEntry(name, fingerprint);
	}

	/** The registry as it stands, catalog included. */
	public RemoteServiceRegistry listCatalog() {
		return getRegistry();
	}

	// ============================================================
	// Cold cache — see ColdCache
	// ============================================================

	/** Parks registrations nobody has looked at since the cutoff. */
	public int coldifyIdle(Instant cutoff) {
		return cold.coldifyIdle(cutoff);
	}

	/** How many registrations are parked; for tests and diagnostics. */
	public int coldCount() {
		return cold.coldCount();
	}

	// ============================================================
	// Update policies — see UpdatePolicies
	// ============================================================

	/** UPDATE_POLICY.md §4: silence of this many intervals means the provider is gone. */
	public static final int MISSED_HEARTBEATS_TO_LOSE = Liveness.MISSED_HEARTBEATS_TO_LOSE;

	/** UPDATE_POLICY.md §2.3: default failover window of a HARD_CUTOVER. */
	public static final long DEFAULT_CUTOVER_GRACE_MILLIS = UpdatePolicies.DEFAULT_CUTOVER_GRACE_MILLIS;

	public void setDefaultCutoverGraceMillis(long millis) {
		policies.setDefaultCutoverGraceMillis(millis);
	}

	/** Advances every armed handover; driven by the component's scheduler. */
	public int advanceUpdatePolicies(Instant now) {
		return policies.advanceUpdatePolicies(now);
	}

	// ============================================================
	// Provider liveness — see Liveness
	// ============================================================

	@Override
	public Diagnostic heartbeat(String referenceId, long intervalSeconds) {
		return liveness.heartbeat(referenceId, intervalSeconds);
	}

	/** Sweeps the leases; driven by the component's scheduler, not by the wire. */
	public int retireLostProviders(Instant now) {
		return liveness.retireLostProviders(now);
	}

	/** How many registrations are being watched; for tests and diagnostics. */
	public int providerLeaseCount() {
		return liveness.providerLeaseCount();
	}

	// ============================================================
	// Consumer sessions — see Sessions
	// ============================================================

	@Override
	public Diagnostic putSession(ConsumerSession session, Collection<String> acquiredReferenceIds) {
		return sessions.putSession(session, acquiredReferenceIds);
	}

	@Override
	public Diagnostic deleteSession(String consumerId) {
		return sessions.deleteSession(consumerId);
	}

	@Override
	public Optional<SessionSnapshot> getSession(String consumerId) {
		return sessions.getSession(consumerId);
	}

	@Override
	public int expireSessions(Instant cutoff) {
		return sessions.expireSessions(cutoff);
	}

	@Override
	public void consumerConnected(String consumerId) {
		sessions.consumerConnected(consumerId);
	}

	@Override
	public void consumerDisconnected(String consumerId) {
		sessions.consumerDisconnected(consumerId);
	}

	/** How long a disconnected consumer keeps its session; 0 disables the shortcut. */
	public void setDisconnectGraceSeconds(long seconds) {
		sessions.disconnectGraceSeconds(seconds);
	}

	@Override
	public int sessionCount() {
		return sessions.sessionCount();
	}

	// ============================================================
	// Catalog operations
	// ============================================================




	// ============================================================
	// State access
	// ============================================================


	/**
	 * The live registry object — package-private for tests and internal
	 * callers that need identity rather than a snapshot. Any read of it
	 * outside the broker's own locking is the caller's race to lose.
	 */
	RemoteServiceRegistry liveRegistry() {
		return state.registry();
	}



	// ============================================================
	// Session operations (ACQUISITION.md §3/§4)
	// ============================================================







	// ============================================================
	// Cold cache (ACQUISITION.md §10)
	// ============================================================


















	// ============================================================
	// Helpers
	// ============================================================


















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







	// ============================================================
	// Update policies (UPDATE_POLICY.md §2)
	// ============================================================





	// ============================================================
	// Provider liveness (#52, UPDATE_POLICY.md §4)
	// ============================================================














}
