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

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.broker.core.DdsrBroker;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.broker.core.LookupBackend;
import org.eclipse.fennec.services.runtime.BrokerRuntimeDTO;

/**
 * The broker: an in-memory registry with synchronous XMI snapshot
 * persistence after every acknowledged change, guarded by one
 * read-write lock.
 *
 * <p>A façade over the components behind it, each with one concern.
 * What it owns itself is the lifecycle: {@link #activate()} starts the
 * work the broker does on its own — advancing a handover, retiring a
 * silent provider, expiring a session, parking an idle registration —
 * and {@link #close()} stops it.
 *
 * <p>Plain Java, no OSGi imports, and that is not a promise for later:
 * the Declarative Services component around this is only a translation
 * from Configuration Admin into {@link BrokerSettings}. The same
 * settings give the same behaviour with or without a framework, which
 * was not true while the periodic work lived in the component.
 */
public final class DdsrBrokerImpl implements DdsrBroker {

	/** Where an acknowledged and persisted change is announced. */
	private final EventDelivery delivery;

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

	/**
	 * What this broker looks like from outside (#126).
	 *
	 * <p>A concern like the others, and the only one that changes
	 * nothing. It is told after every mutation rather than deriving the
	 * fact from the event stream, because leases and sessions raise no
	 * service events — a runtime that reported everything except who
	 * holds what would report the easy half.
	 */
	private final Runtime runtime;

	/** The work the broker does on its own, on a clock. */
	private final Maintenance maintenance;

	public DdsrBrokerImpl(Path snapshotPath, LookupBackend lookup) {
		this(snapshotPath, lookup, EventSink.NOOP);
	}

	public DdsrBrokerImpl(Path snapshotPath, LookupBackend lookup, EventSink events) {
		this(BrokerSettings.defaults(snapshotPath), lookup, events);
	}

	/**
	 * A broker with everything it needs to be told.
	 *
	 * <p>Building it does not start it: {@link #activate()} does. The
	 * two are separate because a caller may want to hand the object
	 * around, register it, or hold it briefly before the periodic work
	 * begins.
	 */
	public DdsrBrokerImpl(BrokerSettings settings, LookupBackend lookup, EventSink events) {
		this.maintenance = new Maintenance(settings);
		this.state = new BrokerState(settings.snapshotPath());
		this.delivery = new EventDelivery(events);
		this.announcements = new Announcements(delivery);
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
		this.runtime = new Runtime(state, sessions, liveness, cold, delivery);
		if (state.rehydrated()) {
			// The providers and implementations came back from the
			// snapshot, but the reference and registration pairs and the
			// lookup index are runtime state and have to be rebuilt.
			registrations.reindex();
		}
		cold.loadColdStubs();
		setDisconnectGraceSeconds(settings.sessionDisconnectGraceSeconds());
		setDefaultCutoverGraceMillis(settings.cutoverGraceMillis());
	}

	/**
	 * Starts the work the broker does on its own.
	 *
	 * <p>Separate from the constructor, and idempotent enough to be
	 * called once: the sweeps a broker runs are behaviour, not
	 * deployment, so they belong here rather than in whatever framework
	 * happens to host it.
	 */
	public void activate() {
		maintenance.start(this);
	}

	/**
	 * Stops the periodic work and saves the registry one last time.
	 *
	 * <p>The save is defensive. Every acknowledged change was already
	 * saved before it was announced; this only covers a mutation that
	 * landed while the shutdown was in flight.
	 */
	public void close() {
		maintenance.close();
		snapshot();
		// After the snapshot: what is still queued is about changes that
		// are already saved, and a subscriber still connected during a
		// shutdown has every right to hear about them.
		delivery.close();
	}

	/**
	 * Blocks until every event emitted so far has reached the sink.
	 *
	 * <p>Delivery is asynchronous since #124, so "the call returned"
	 * and "the subscribers were told" are two moments. Tests need the
	 * second one, and they need it without sleeping.
	 */
	void awaitEventsDelivered() {
		delivery.awaitIdle();
	}

	/** How many events were dropped because a subscriber stopped reading. */
	long droppedEventCount() {
		return delivery.droppedCount();
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
		return changed(registrations.publishImplementation(provider, implementation));
	}

	@Override
	public Diagnostic modifyImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		return changed(registrations.modifyImplementation(provider, implementation));
	}

	@Override
	public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		return changed(registrations.withdrawImplementation(provider, implementation));
	}

	@Override
	public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
		return changed(registrations.registerService(provider, implementation));
	}

	/**
	 * Report that the broker's state may look different now, and hand
	 * the caller's value back.
	 *
	 * <p>After the mutation, not before, so a watcher that re-reads
	 * immediately sees what happened rather than what was about to.
	 *
	 * <p>Unconditionally, including for a mutation that was refused: a
	 * watcher woken for nothing re-reads and finds nothing different,
	 * which is correct and briefly wasteful — the same trade the event
	 * stream makes when it tells everyone to re-snapshot.
	 */
	private <T> T changed(T result) {
		runtime.changed();
		return result;
	}

	// ============================================================
	// Introspection — see Runtime
	// ============================================================

	/** What this broker holds, for anything that wants to watch it (#126). */
	public BrokerRuntimeDTO runtimeSnapshot() {
		return runtime.snapshot();
	}

	/** Which snapshot the next answer would be. */
	public long runtimeRevision() {
		return runtime.revision();
	}

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
		return changed(catalog.addCatalogEntry(serviceInterface, requestor));
	}

	@Override
	public Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return changed(catalog.deprecateCatalogEntry(serviceInterface, requestor));
	}

	@Override
	public Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return changed(catalog.removeCatalogEntry(serviceInterface, requestor));
	}

	public Diagnostic deprecateCatalogEntry(String name, String fingerprint, ServiceInterface governance,
			String requestor) {
		return changed(catalog.deprecateCatalogEntry(name, fingerprint, governance, requestor));
	}

	public Diagnostic removeCatalogEntry(String name, String fingerprint, String requestor) {
		return changed(catalog.removeCatalogEntry(name, fingerprint, requestor));
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
		return changed(cold.coldifyIdle(cutoff));
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
		return changed(policies.advanceUpdatePolicies(now));
	}

	// ============================================================
	// Provider liveness — see Liveness
	// ============================================================

	@Override
	public Diagnostic heartbeat(String referenceId, long intervalSeconds) {
		return changed(liveness.heartbeat(referenceId, intervalSeconds));
	}

	/** Sweeps the leases; driven by the component's scheduler, not by the wire. */
	public int retireLostProviders(Instant now) {
		return changed(liveness.retireLostProviders(now));
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
		return changed(sessions.putSession(session, acquiredReferenceIds));
	}

	@Override
	public Diagnostic deleteSession(String consumerId) {
		return changed(sessions.deleteSession(consumerId));
	}

	@Override
	public Optional<SessionSnapshot> getSession(String consumerId) {
		return sessions.getSession(consumerId);
	}

	@Override
	public int expireSessions(Instant cutoff) {
		return changed(sessions.expireSessions(cutoff));
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

}
