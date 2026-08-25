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
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.broker.core.LookupBackend;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
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
				name = "Cold cache after (seconds)",
				description = "Optional storage policy (ACQUISITION.md \u00a710): a registration that held "
						+ "no lease and saw no lookup on its interfaces for this long is parked on disk "
						+ "but stays discoverable (the next lookup rehydrates it). The sweep runs at a "
						+ "quarter of this value. 0 (default) disables the policy.",
				required = false)
		long cold_after_seconds() default 0;
	}

	@Reference(
			cardinality = ReferenceCardinality.OPTIONAL,
			policy = ReferencePolicy.DYNAMIC,
			policyOption = ReferencePolicyOption.GREEDY)
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

	private ScheduledExecutorService sessionExpiry;

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
	void activate(Config config) {
		try {
			Path snapshotPath = Paths.get(config.snapshot_path());
			LookupBackend backend = externalLookup != null ? externalLookup : new InMemoryLookupBackend();
			this.delegate = new DdsrBrokerImpl(snapshotPath, backend, this::fanOut);
			long expirySeconds = config.session_expiry_seconds();
			long coldSeconds = config.cold_after_seconds();
			if (expirySeconds > 0 || coldSeconds > 0) {
				sessionExpiry = Executors.newSingleThreadScheduledExecutor(task -> {
					Thread thread = new Thread(task, "ddsr-broker-maintenance");
					thread.setDaemon(true);
					return thread;
				});
			}
			if (expirySeconds > 0) {
				long sweepSeconds = Math.max(1, expirySeconds / 4);
				sessionExpiry.scheduleAtFixedRate(() -> {
					try {
						DdsrBrokerImpl current = delegate;
						if (current == null) {
							return;
						}
						int expired = current.expireSessions(Instant.now().minusSeconds(expirySeconds));
						if (expired > 0) {
							LOG.info("[DDSR] expired " + expired + " consumer session(s) without renewal");
						}
					} catch (RuntimeException sweepFailure) {
						LOG.warning("[DDSR] session expiry sweep failed, continuing: " + sweepFailure);
					}
				}, sweepSeconds, sweepSeconds, TimeUnit.SECONDS);
			}
			if (coldSeconds > 0) {
				long sweepSeconds = Math.max(1, coldSeconds / 4);
				sessionExpiry.scheduleAtFixedRate(() -> {
					try {
						DdsrBrokerImpl current = delegate;
						if (current == null) {
							return;
						}
						int moved = current.coldifyIdle(Instant.now().minusSeconds(coldSeconds));
						if (moved > 0) {
							LOG.info("[DDSR] parked " + moved + " idle registration(s) in the cold cache");
						}
					} catch (RuntimeException sweepFailure) {
						LOG.warning("[DDSR] cold-cache sweep failed, continuing: " + sweepFailure);
					}
				}, sweepSeconds, sweepSeconds, TimeUnit.SECONDS);
			}
			LOG.info("[DDSR] BrokerCore activated, snapshot=" + snapshotPath.toAbsolutePath());
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR] BrokerCore activation FAILED", t);
			throw t;
		}
	}

	@Deactivate
	void deactivate() {
		if (sessionExpiry != null) {
			sessionExpiry.shutdownNow();
			sessionExpiry = null;
		}
		if (delegate != null) {
			// Best-effort final snapshot — already persisted after every
			// mutation, but defensive in case mutations happened during
			// in-flight shutdown.
			delegate.snapshot();
		}
		delegate = null;
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
	public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
		return required().registerService(provider, implementation);
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
	public int sessionCount() {
		return required().sessionCount();
	}
}
