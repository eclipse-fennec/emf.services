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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.DdsrConsumer;
import org.eclipse.fennec.services.client.DdsrProvider;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.common.FrameworkShutdown;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
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

		@AttributeDefinition(
				name = "Provider heartbeat (seconds)",
				description = "Provider liveness (#52, UPDATE_POLICY.md \u00a74): interval of the heartbeat "
						+ "sent for every live registration. The broker retires a registration after two "
						+ "missed heartbeats (PROVIDER_LOST); a registration the broker lost is published "
						+ "again. 0 disables heartbeats — the broker then never retires this provider for silence.",
				required = false)
		long provider_heartbeat_seconds() default 30;

		@AttributeDefinition(
				name = "Greedy rebind",
				description = "UPDATE_POLICY.md \u00a73: rebind locators to the successor as soon as the broker "
						+ "announces UPGRADE_AVAILABLE (true), or keep the current registration until the "
						+ "broker retires it (false, default).",
				required = false)
		boolean greedy_rebind() default false;
	}

	// Target the REST-flavor proxies explicitly. broker.core's embedded
	// broker also registers BrokerImplementations / BrokerLookup with
	// no transport property — without this filter DS would non-
	// deterministically bind either side, defeating the point of the
	// flavor split.
	/**
	 * The transport this client hears events on.
	 *
	 * <p>Mandatory and static, and chosen by configuration: a deployment
	 * that configures a client is describing the setup it expects, not
	 * entering a contest. Which source it is said with
	 * {@code eventSource.target} against the {@code ddsr.event.transport}
	 * property the sources carry; with no target it is SSE, which every
	 * launch that has a client at all also has.
	 *
	 * <p>It used to be optional, dynamic and greedy, and that cost
	 * events: a greedy rebind closes the open stream and opens another,
	 * and whatever was published in between reached nobody — MQTT has no
	 * session to replay it from. A transport is not something to change
	 * mid-flight, and nothing ever asked for it; only our own test setup
	 * ranked one source above the other to make the switch happen.
	 */
	@Reference
	private EventSource eventSource;

	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerImplementations implementations;

	/**
	 * The session proxy that holds this consumer's leases
	 * (ACQUISITION.md §5).
	 *
	 * <p>Mandatory and static for the same reason as the event source:
	 * it comes from the same transport bundle as the lookup and the
	 * implementations proxy, both of which are already required here.
	 * Calling it optional said that a client might silently run without
	 * drain protection — which is not a setup anyone configures, only
	 * one nobody noticed.
	 */
	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerSessions sessions;

	private ScheduledExecutorService sessionRenewal;

	private String consumerId;

	private List<FlavorKind> supportedFlavors;

	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerLookup lookup;

	private DdsrClientImpl delegate;

	/** Removes the JVM shutdown hook again when this component goes. */
	private AutoCloseable cleanShutdown;

	/** Kept so a configuration change can rebuild without a fresh activation. */
	private BundleContext context;

	/** What the running client was built from, to tell a real change from a repeat. */
	private Config config;

	@Activate
	void activate(BundleContext context, Config config) {
		this.context = context;
		// A provider that is killed with SIGTERM must still withdraw, and
		// a consumer must still release its session (FR-P3). Neither
		// happens unless something stops the framework first, and the bnd
		// launcher installs no hook of its own.
		this.cleanShutdown = FrameworkShutdown.installFor(context);
		this.config = config;
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
					eventStreams, config.greedy_rebind());
			long renewalSeconds = config.session_interval_seconds();
			long heartbeatSeconds = config.provider_heartbeat_seconds();
			if (renewalSeconds > 0 || heartbeatSeconds > 0) {
				sessionRenewal = Executors.newSingleThreadScheduledExecutor(task -> {
					Thread thread = new Thread(task, "ddsr-client-maintenance");
					thread.setDaemon(true);
					return thread;
				});
			}
			if (renewalSeconds > 0) {
				// First PUT shortly after activation (once lookups may have
				// happened), then the flat renewal interval.
				sessionRenewal.scheduleAtFixedRate(this::renewSession,
						Math.min(renewalSeconds, 5), renewalSeconds, TimeUnit.SECONDS);
			}
			if (heartbeatSeconds > 0) {
				// First heartbeat shortly after activation so the broker's
				// liveness supervision is armed early, then the flat interval
				// the broker is told about.
				sessionRenewal.scheduleAtFixedRate(() -> heartbeatRegistrations(heartbeatSeconds),
						Math.min(heartbeatSeconds, 5), heartbeatSeconds, TimeUnit.SECONDS);
			}
			LOG.info("[DDSR-Client] activated, flavors=" + flavors + ", consumerId=" + consumerId);
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR-Client] activation FAILED", t);
			throw t;
		}
	}

	/**
	 * A changed configuration is taken, not died of.
	 *
	 * <p>Configuration Admin delivers the same configuration more than
	 * once while a framework starts, and without this method the
	 * component runtime answers each delivery by destroying this client
	 * and building another: a new consumer id, a new session at the
	 * broker, every open event stream closed, and — because discovery
	 * and the RSA admin are wired to this service — that whole column
	 * torn down with it. An export running at that moment failed for a
	 * reason nothing in the log explained (#107).
	 *
	 * <p>A configuration that says the same thing therefore does nothing
	 * at all. One that says something different is rebuilt the way a
	 * restart would have done it — but by us, so the component instance,
	 * and everything bound to it, survives.
	 */
	@Modified
	void modified(Config config) {
		if (saysTheSame(this.config, config)) {
			return;
		}
		LOG.info("[DDSR-Client] configuration changed — rebuilding the client");
		BundleContext current = this.context;
		deactivate();
		activate(current, config);
	}

	/** Whether two configurations would build the same client. */
	private static boolean saysTheSame(Config running, Config fresh) {
		if (running == null) {
			return false;
		}
		return Objects.equals(running.supported_flavors(), fresh.supported_flavors())
				&& Objects.equals(blankToNull(running.consumer_id()), blankToNull(fresh.consumer_id()))
				&& running.session_interval_seconds() == fresh.session_interval_seconds()
				&& running.provider_heartbeat_seconds() == fresh.provider_heartbeat_seconds()
				&& running.greedy_rebind() == fresh.greedy_rebind();
	}

	/**
	 * A blank consumer id means "make one up", and the one made up at
	 * activation is still in use — so blank and blank are the same
	 * configuration, not two different ones.
	 */
	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	/**
	 * Opens the stream on the configured transport.
	 *
	 * <p>The source itself may still answer {@code null} — a transport
	 * that is registered but not yet carrying (its connection is coming
	 * up) says so this way. The registry then holds no subscription and
	 * tries again when the next listener registers, rather than keeping
	 * a handle that delivers nothing.
	 */
	/**
	 * The bound event source, reached through this component rather than
	 * handed over once.
	 *
	 * <p>Written out instead of passed as {@code this::openEventStream}.
	 * A method reference can only implement an interface's single
	 * abstract method, so the moment {@link EventSource} grew a second
	 * {@code open} the reference kept satisfying the first one and the
	 * new argument was silently dropped by the interface default. The
	 * consumer id then never reached the wire, and nothing failed — the
	 * broker simply saw an anonymous subscriber.
	 */
	private final EventSource eventStreams = new EventSource() {

		@Override
		public AutoCloseable open(Handler handler) {
			return eventSource.open(handler);
		}

		@Override
		public AutoCloseable open(Handler handler, String consumerId) {
			return eventSource.open(handler, consumerId);
		}
	};

	/**
	 * The idempotent full replace (ACQUISITION.md §4): the current set of
	 * known reference ids IS the acquisition list; an unchanged PUT is
	 * the heartbeat. No transport or no delegate → silently no lease.
	 */
	private void renewSession() {
		ConsumerSessionKeeper current = sessionKeeper();
		if (current != null) {
			current.renew();
		}
	}

	/**
	 * The session behaviour, built fresh from what is bound right now.
	 *
	 * <p>Built per call rather than held: the reference and the
	 * delegate are both replaceable while this component lives, and a
	 * keeper captured at activation would go on renewing a session
	 * against a transport that is gone.
	 */
	private ConsumerSessionKeeper sessionKeeper() {
		BrokerSessions current = sessions;
		DdsrClientImpl client = delegate;
		if (current == null || client == null) {
			return null;
		}
		return new ConsumerSessionKeeper(current, consumerId, supportedFlavors, client::knownReferenceIds);
	}

	private void heartbeatRegistrations(long intervalSeconds) {
		try {
			DdsrClientImpl client = delegate;
			if (client != null) {
				client.heartbeatRegistrations(intervalSeconds);
			}
		} catch (RuntimeException heartbeatFailure) {
			LOG.warning("[DDSR-Client] provider heartbeat failed, retrying next interval: " + heartbeatFailure);
		}
	}

	@Deactivate
	void deactivate() {
		if (cleanShutdown != null) {
			try {
				cleanShutdown.close();
			} catch (Exception removalFailure) {
				// The JVM may already be shutting down, in which case the
				// hook is running and cannot be removed. Nothing to do.
			}
			cleanShutdown = null;
		}
		if (sessionRenewal != null) {
			sessionRenewal.shutdownNow();
			sessionRenewal = null;
		}
		// Shutdown-notify BEFORE the streams close (FR-P3 order): the
		// broker releases the leases immediately instead of waiting for
		// the TTL. Best-effort — a dead broker must not stall shutdown.
		BrokerSessions current = sessions;
		if (current != null) {
			new ConsumerSessionKeeper(current, consumerId, supportedFlavors, List::of).release();
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
