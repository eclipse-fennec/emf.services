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

package org.eclipse.fennec.services.client.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.client.DdsrConsumer;
import org.eclipse.fennec.services.client.DdsrProvider;
import org.eclipse.fennec.services.FlavorKind;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi DS adapter for {@link DdsrClientImpl}. References the broker
 * role services ({@link BrokerImplementations}, {@link BrokerLookup})
 * — whichever flavor bundle (client.rest, future client.mqtt, or an
 * in-process broker.core) provides them is what the SDK ends up
 * talking to.
 *
 * <p>No broker URL is configured here on purpose. The wire endpoint is
 * owned by the active flavor bundle (e.g. {@code RestTransport} in
 * client.rest); URLs for individual operations come from the
 * {@code RestFlavor.host} + {@code basePath} the broker self-publishes.
 */
@Component(
		service = DdsrClient.class,
		configurationPid = "org.eclipse.fennec.services.client",
		immediate = true)
@Designate(ocd = DdsrClientComponent.Config.class)
public final class DdsrClientComponent implements DdsrClient {

	private static final Logger LOG = Logger.getLogger(DdsrClientComponent.class.getName());

	@ObjectClassDefinition(name = "DDSR Java Client", description = "Transport-agnostic DDSR client SDK")
	public @interface Config {

		@AttributeDefinition(
				name = "Supported flavors",
				description = "Comma-separated list of FlavorKind values the consumer can speak (REST, MQTT)")
		String supported_flavors() default "REST";

		@AttributeDefinition(
				name = "Consumer ID",
				description = "Symbolic identity attached to lookups and to the broker session. "
						+ "Generated (consumer-<uuid>) when empty.",
				required = false)
		String consumer_id() default "";

		@AttributeDefinition(
				name = "Session renewal (seconds)",
				description = "Interval of the idempotent session PUT (acquire+release+heartbeat in one, "
						+ "ACQUISITION.md §4). Should be half the broker's expiry. 0 disables sessions.",
				required = false)
		long session_interval_seconds() default 600;
	}

	// Target the REST-flavor proxies explicitly. broker.core's embedded
	// broker also registers BrokerImplementations / BrokerLookup with
	// no transport property — without this filter DS would non-
	// deterministically bind either side, defeating the point of the
	// flavor split.
	/**
	 * Optional: without an event transport the client works exactly as
	 * before, it just does not notify. Which transport shows up here —
	 * SSE today, MQTT later — is none of the SDK's business.
	 */
	private volatile EventSource eventSource;

	/**
	 * Bind method rather than field injection, because the arrival of a
	 * transport has to be acted on: a listener may already be registered
	 * and waiting. Registration and transport activation are independent
	 * components, so either order happens — and it did, which is how this
	 * was found.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC,
			policyOption = ReferencePolicyOption.GREEDY)
	void bindEventSource(EventSource source) {
		this.eventSource = source;
		DdsrClientImpl current = delegate;
		if (current != null) {
			current.transportAvailable();
		}
	}

	void unbindEventSource(EventSource source) {
		if (this.eventSource == source) {
			this.eventSource = null;
		}
	}

	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerImplementations implementations;

	/**
	 * Optional like the event transport: a client without a session
	 * proxy simply does not lease — it only loses drain protection
	 * (ACQUISITION.md §5), nothing functional.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC,
			policyOption = ReferencePolicyOption.GREEDY,
			target = "(ddsr.broker.transport=rest)")
	private volatile BrokerSessions sessions;

	private ScheduledExecutorService sessionRenewal;

	private String consumerId;

	private List<FlavorKind> supportedFlavors;

	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerLookup lookup;

	private DdsrClientImpl delegate;

	@Activate
	void activate(Config config) {
		try {
			List<FlavorKind> flavors = parseFlavors(config.supported_flavors());
			this.supportedFlavors = flavors;
			String consumerId = config.consumer_id() == null || config.consumer_id().isBlank()
					? "consumer-" + UUID.randomUUID()
					: config.consumer_id();
			this.consumerId = consumerId;
			// Indirection, not the value: the reference is optional and
			// dynamic, so reading it once at activation would freeze
			// whatever happened to be bound at that moment — the same
			// mistake the broker side avoids with its fan-out.
			this.delegate = new DdsrClientImpl(implementations, lookup, flavors, consumerId,
					this::openEventStream);
			long renewalSeconds = config.session_interval_seconds();
			if (renewalSeconds > 0) {
				sessionRenewal = Executors.newSingleThreadScheduledExecutor(task -> {
					Thread thread = new Thread(task, "ddsr-session-renewal");
					thread.setDaemon(true);
					return thread;
				});
				// First PUT shortly after activation (once lookups may have
				// happened), then the flat renewal interval.
				sessionRenewal.scheduleAtFixedRate(this::renewSession,
						Math.min(renewalSeconds, 5), renewalSeconds, TimeUnit.SECONDS);
			}
			LOG.info("[DDSR-Client] activated, flavors=" + flavors + ", consumerId=" + consumerId);
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR-Client] activation FAILED", t);
			throw t;
		}
	}

	/**
	 * Opens the stream on whichever event transport is bound right now.
	 * Returns {@code null} when none is — the caller then knows it has no
	 * subscription and can try again later, instead of holding a handle
	 * that never delivers anything.
	 */
	private AutoCloseable openEventStream(EventSource.Handler handler) {
		EventSource current = eventSource;
		if (current == null) {
			LOG.info("[DDSR-Client] no event transport bound yet — will connect when one appears");
			return null;
		}
		return current.open(handler);
	}

	/**
	 * The idempotent full replace (ACQUISITION.md §4): the current set of
	 * known reference ids IS the acquisition list; an unchanged PUT is
	 * the heartbeat. No transport or no delegate → silently no lease.
	 */
	private void renewSession() {
		try {
			BrokerSessions current = sessions;
			DdsrClientImpl client = delegate;
			if (current == null || client == null) {
				return;
			}
			ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
			session.setConsumerId(consumerId);
			ConsumerCapability capability = ServicesFactory.eINSTANCE.createConsumerCapability();
			capability.setConsumerId(consumerId);
			if (supportedFlavors != null) {
				capability.getSupportedFlavors().addAll(supportedFlavors);
			}
			session.setCapabilities(capability);
			current.putSession(session, client.knownReferenceIds());
		} catch (RuntimeException renewalFailure) {
			// Lease renewal is best-effort: the broker treats a missed
			// renewal as any other silence (TTL), so log and carry on.
			LOG.warning("[DDSR-Client] session renewal failed, retrying next interval: " + renewalFailure);
		}
	}

	@Deactivate
	void deactivate() {
		if (sessionRenewal != null) {
			sessionRenewal.shutdownNow();
			sessionRenewal = null;
		}
		// Shutdown-notify BEFORE the streams close (FR-P3 order): the
		// broker releases the leases immediately instead of waiting for
		// the TTL. Best-effort — a dead broker must not stall shutdown.
		BrokerSessions current = sessions;
		if (current != null && consumerId != null) {
			try {
				current.deleteSession(consumerId);
				// stdout, not JUL: shutdown path — JUL's cleanup hook may
				// already have reset the LogManager (DECISIONS_PARITY D14).
				System.out.println("[DDSR-Client] session released at broker: " + consumerId);
			} catch (RuntimeException deleteFailure) {
				System.err.println("[DDSR-Client] session release failed, broker will expire it: "
						+ deleteFailure);
			}
		}
		if (delegate != null) {
			delegate.close();
			delegate = null;
		}
	}

	private static List<FlavorKind> parseFlavors(String csv) {
		List<FlavorKind> kinds = new ArrayList<>();
		if (csv == null || csv.isBlank()) {
			return kinds;
		}
		for (String s : csv.split(",")) {
			String t = s.trim();
			if (t.isEmpty()) {
				continue;
			}
			FlavorKind k = FlavorKind.getByName(t);
			if (k != null) {
				kinds.add(k);
			}
		}
		return kinds;
	}

	@Override
	public DdsrProvider provider() {
		return delegate.provider();
	}

	@Override
	public DdsrConsumer consumer() {
		return delegate.consumer();
	}

	@Override
	public void close() {
		delegate.close();
	}
}
