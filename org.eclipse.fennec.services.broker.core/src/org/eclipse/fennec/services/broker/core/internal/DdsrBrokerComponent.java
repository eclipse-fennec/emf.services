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

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.broker.core.DdsrBroker;
import org.eclipse.fennec.services.common.FrameworkShutdown;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.broker.core.LookupBackend;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi DS adapter that exposes a {@link DdsrBrokerImpl} as a
 * {@link DdsrBroker} service. Thin wrapper: configuration reading
 * and an optional pluggable {@link LookupBackend} binding live here;
 * the actual logic lives in {@link DdsrBrokerImpl}, which has no
 * OSGi imports.
 *
 * <p>If no external {@code LookupBackend} is registered (e.g. the
 * Lucene module is not installed), an {@link InMemoryLookupBackend} is
 * used as the default.
 */
@Component(
		service = { DdsrBroker.class, BrokerCatalog.class, BrokerImplementations.class, BrokerLookup.class,
				BrokerSessions.class },
		// The broker is the provider of its own three contracts, so it
		// says so the way every provider does. This is what the generic
		// REST distribution looks it up by (#76) — one service, three
		// contracts, because that is what it is.
		property = {
				"ddsr.contract=BrokerCatalog",
				"ddsr.contract=BrokerImplementations",
				"ddsr.contract=BrokerLookup" },
		configurationPid = "org.eclipse.fennec.services.broker.core",
		immediate = true)
@Designate(ocd = DdsrBrokerComponent.Config.class)
public final class DdsrBrokerComponent implements DdsrBroker {

	private static final Logger LOG = Logger.getLogger(DdsrBrokerComponent.class.getName());

	@ObjectClassDefinition(name = "DDSR Broker Core", description = "Configuration for the in-memory DDSR broker")
	public @interface Config {

		@AttributeDefinition(
				name = "Snapshot path",
				description = "File system path of the XMI snapshot. Persisted synchronously after each mutation.",
				required = false)
		String snapshot_path() default "./broker-state.xmi";

		@AttributeDefinition(
				name = "Session expiry (seconds)",
				description = "A consumer session expires this long after its last PUT (renewal interval "
						+ "should be half of it, per UPDATE_POLICY/ACQUISITION: 600s renew, 1200s expire). "
						+ "The expiry sweep runs at a quarter of this value. 0 disables expiry.",
				required = false)
		long session_expiry_seconds() default 1200;

		@AttributeDefinition(
				name = "Session expiry after a lost event connection (seconds)",
				description = "A consumer whose event connection went away loses its session this long "
						+ "afterwards, instead of waiting out the full expiry. An open connection is a "
						+ "free presence signal, so losing it is worth noticing sooner; a reconnect or a "
						+ "fresh PUT clears it, and the client reconnects within seconds, so this has to "
						+ "stay comfortably above that. Only transports the broker sees a connection for "
						+ "report this at all. 0 disables the shortcut.",
				required = false)
		long session_disconnect_grace_seconds() default 60;

		@AttributeDefinition(
				name = "Cold cache after (seconds)",
				description = "Optional storage policy (ACQUISITION.md \u00a710): a registration that held "
						+ "no lease and saw no lookup on its interfaces for this long is parked on disk "
						+ "but stays discoverable (the next lookup rehydrates it). The sweep runs at a "
						+ "quarter of this value. 0 (default) disables the policy.",
				required = false)
		long cold_after_seconds() default 0;

		@AttributeDefinition(
				name = "Cutover grace (seconds)",
				description = "UPDATE_POLICY.md \u00a72.3: failover window of a HARD_CUTOVER when the successor "
						+ "leaves cutoverGraceMillis at 0. Both versions stay visible during the window.",
				required = false)
		long cutover_grace_seconds() default 30;

		@AttributeDefinition(
				name = "Update-policy sweep (seconds)",
				description = "How often pending DEPRECATE_AND_DRAIN drains and HARD_CUTOVER windows are "
						+ "checked (UPDATE_POLICY.md \u00a72). 0 disables the sweep; policies are then never advanced.",
				required = false)
		long policy_sweep_seconds() default 5;

		@AttributeDefinition(
				name = "Provider-liveness sweep (seconds)",
				description = "How often registrations whose provider promised heartbeats are checked "
						+ "for silence (#52, UPDATE_POLICY.md \u00a74). A registration is retired with "
						+ "PROVIDER_LOST after two missed heartbeats. 0 disables the sweep; heartbeats are "
						+ "then accepted but never enforced.",
				required = false)
		long liveness_sweep_seconds() default 5;
	}

	@Reference(name="broker.backend")
	private volatile LookupBackend externalLookup;

	/**
	 * Event sinks as a whiteboard: every registered {@link EventSink}
	 * service gets the broker's lifecycle events. That is what keeps the
	 * transport open-ended — the SSE endpoint registers one, an MQTT
	 * bridge would register another, and neither requires a change here
	 * or in {@link DdsrBrokerImpl}.
	 */
	private final List<EventSink> eventSinks = new CopyOnWriteArrayList<>();

	/**
	 * Written by the activation thread, read by request threads —
	 * volatile provides the happens-before edge. {@link #required()}
	 * turns the activation/deactivation window into a defined failure
	 * instead of an NPE.
	 */
	private volatile DdsrBrokerImpl delegate;

	/** Removes the JVM shutdown hook again when this component goes. */
	private AutoCloseable cleanShutdown;

	private static void closeQuietly(AutoCloseable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception removalFailure) {
			LOG.log(Level.FINE, "[DDSR] removing the shutdown hook failed", removalFailure);
		}
	}

	private DdsrBrokerImpl required() {
		DdsrBrokerImpl current = delegate;
		if (current == null) {
			throw new IllegalStateException("DDSR broker is not active (component starting or stopping)");
		}
		return current;
	}

	@Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC)
	void addEventSink(EventSink sink) {
		eventSinks.add(sink);
	}

	void removeEventSink(EventSink sink) {
		eventSinks.remove(sink);
	}

	/**
	 * Fans one event out to whatever is currently registered. Passed to
	 * the broker as a stable sink so subscribers can come and go without
	 * the broker knowing.
	 * <p>
	 * Each sink is isolated: one that throws — against its contract —
	 * must not stop the others from being served.
	 */
	private void fanOut(ServiceEvent event) {
		for (EventSink sink : eventSinks) {
			try {
				sink.publish(event);
			} catch (RuntimeException sinkFailure) {
				LOG.warning("[DDSR] event sink failed, continuing: " + sinkFailure);
			}
		}
	}

	@Activate
	void activate(BundleContext context, Config config) {
		try {
			// Every launch that must shut down cleanly needs this, and a
			// component that already owns a lifecycle is the natural place
			// to install it: a plain SIGTERM otherwise kills the JVM
			// without running a single @Deactivate, so the final snapshot
			// and every subscriber close are skipped.
			this.cleanShutdown = FrameworkShutdown.installFor(context);
			// Everything below this line is behaviour, so none of it is
			// decided here: the component turns a configuration into
			// settings and hands them over. A broker embedded in a plain
			// Java program gets the same object with the same settings and
			// behaves the same way, which it did not while the periodic
			// work lived in this class.
			BrokerSettings settings = new BrokerSettings(
					Paths.get(config.snapshot_path()),
					config.session_expiry_seconds(),
					config.session_disconnect_grace_seconds(),
					config.cold_after_seconds(),
					config.policy_sweep_seconds(),
					config.liveness_sweep_seconds(),
					config.cutover_grace_seconds() * 1000L);
			DdsrBrokerImpl broker = new DdsrBrokerImpl(settings, externalLookup, this::fanOut);
			this.delegate = broker;
			broker.activate();
			LOG.info("[DDSR] BrokerCore activated, snapshot=" + settings.snapshotPath().toAbsolutePath());
		} catch (RuntimeException activationFailure) {
			LOG.log(Level.SEVERE, "[DDSR] BrokerCore could not be activated", activationFailure);
			throw activationFailure;
		}
	}

	void deactivate() {
		closeQuietly(cleanShutdown);
		cleanShutdown = null;
		DdsrBrokerImpl broker = delegate;
		delegate = null;
		if (broker != null) {
			broker.close();
		}
	}

	// --- Delegation -------------------------------------------------------

	@Override
	public Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		return required().publishImplementation(provider, implementation);
	}

	@Override
	public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		return required().withdrawImplementation(provider, implementation);
	}

	@Override
	public Diagnostic modifyImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		return required().modifyImplementation(provider, implementation);
	}

	@Override
	public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
		return required().registerService(provider, implementation);
	}

	@Override
	public Diagnostic heartbeat(String referenceId, long intervalSeconds) {
		return required().heartbeat(referenceId, intervalSeconds);
	}

	@Override
	public ServiceReference getServiceReference(String interfaceName) {
		return required().getServiceReference(interfaceName);
	}

	@Override
	public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		return required().getServiceReferences(interfaceName, filter, capability);
	}

	@Override
	public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		return required().getAllServiceReferences(interfaceName, filter, capability);
	}

	@Override
	public Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return required().addCatalogEntry(serviceInterface, requestor);
	}

	@Override
	public Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return required().deprecateCatalogEntry(serviceInterface, requestor);
	}

	@Override
	public Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		return required().removeCatalogEntry(serviceInterface, requestor);
	}

	@Override
	public RemoteServiceRegistry getRegistry() {
		return required().getRegistry();
	}

	// ------------------------------------------------------------
	// The operations the broker's own contracts name (#76). Not on the
	// role interfaces: those are the in-process API and a REST proxy
	// implements them too, while these are this deployment's answer to
	// what its published contracts describe.
	// ------------------------------------------------------------

	public RemoteServiceRegistry listCatalog() {
		return required().listCatalog();
	}

	public ServiceInterface getCatalogEntry(String name, String fingerprint) {
		return required().getCatalogEntry(name, fingerprint);
	}

	public Diagnostic deprecateCatalogEntry(String name, String fingerprint, ServiceInterface governance,
			String requestor) {
		return required().deprecateCatalogEntry(name, fingerprint, governance, requestor);
	}

	public Diagnostic removeCatalogEntry(String name, String fingerprint, String requestor) {
		return required().removeCatalogEntry(name, fingerprint, requestor);
	}

	/**
	 * The publish operations take the provider document, because that is
	 * what travels: the broker accepts exactly one implementation per
	 * call, so which one is not a second argument but a rule about the
	 * document.
	 */
	public Diagnostic publishImplementation(ServiceProvider provider) {
		ServiceImplementation sole = soleImplementation(provider);
		return sole == null ? notExactlyOne(provider) : publishImplementation(provider, sole);
	}

	public Diagnostic modifyImplementation(ServiceProvider provider) {
		ServiceImplementation sole = soleImplementation(provider);
		return sole == null ? notExactlyOne(provider) : modifyImplementation(provider, sole);
	}

	public Diagnostic withdrawImplementation(ServiceProvider provider) {
		ServiceImplementation sole = soleImplementation(provider);
		return sole == null ? notExactlyOne(provider) : withdrawImplementation(provider, sole);
	}

	private static ServiceImplementation soleImplementation(ServiceProvider provider) {
		return provider == null || provider.getImplementations().size() != 1
				? null
				: provider.getImplementations().get(0);
	}

	/**
	 * Refused as a Diagnostic rather than thrown: the caller sent
	 * something this broker will not act on, and that is an answer, not
	 * an accident. No contract declares a code for it, so it travels as
	 * the transport's word for "not a request I can carry out".
	 */
	private static Diagnostic notExactlyOne(ServiceProvider provider) {
		return DdsrDiagnostics.error(DdsrDiagnostics.CODE_MALFORMED_REQUEST,
				"the broker takes exactly one implementation per call, got "
						+ (provider == null ? "no provider" : provider.getImplementations().size()));
	}

	@Override
	public Diagnostic snapshot() {
		return required().snapshot();
	}

	@Override
	public ServiceImplementation getImplementationForReference(ServiceReference reference) {
		return required().getImplementationForReference(reference);
	}

	@Override
	public Diagnostic putSession(ConsumerSession session, Collection<String> acquiredReferenceIds) {
		return required().putSession(session, acquiredReferenceIds);
	}

	@Override
	public Diagnostic deleteSession(String consumerId) {
		return required().deleteSession(consumerId);
	}

	@Override
	public Optional<SessionSnapshot> getSession(String consumerId) {
		return required().getSession(consumerId);
	}

	@Override
	public int expireSessions(Instant cutoff) {
		return required().expireSessions(cutoff);
	}

	@Override
	public void consumerConnected(String consumerId) {
		required().consumerConnected(consumerId);
	}

	@Override
	public void consumerDisconnected(String consumerId) {
		required().consumerDisconnected(consumerId);
	}

	@Override
	public int sessionCount() {
		return required().sessionCount();
	}
}
