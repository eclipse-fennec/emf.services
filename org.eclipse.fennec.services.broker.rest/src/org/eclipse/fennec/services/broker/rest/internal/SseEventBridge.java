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

package org.eclipse.fennec.services.broker.rest.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.broker.core.EventDocument;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Distributes broker lifecycle events to SSE subscribers.
 * <p>
 * This is the REST-flavored implementation of {@link EventSink} — one
 * transport among several. It registers itself as an {@code EventSink}
 * service; the broker's whiteboard picks it up. An MQTT bridge would be
 * a sibling of this class and would need no change in
 * {@code broker.core}.
 * <p>
 * Held as a singleton (unlike the JAX-RS resources, which are
 * prototype-scoped) because the subscriber list has to outlive a single
 * request. {@link EventsResource} is the thin entry point that hands new
 * subscribers over.
 */
@Component(service = { EventSink.class, SseEventBridge.class }, immediate = true,
		configurationPid = "org.eclipse.fennec.services.broker.rest.sse")
@Designate(ocd = SseEventBridge.Config.class)
public class SseEventBridge implements EventSink {

	private static final Logger LOG = Logger.getLogger(SseEventBridge.class.getName());

	@ObjectClassDefinition(name = "DDSR SSE Event Bridge",
			description = "Distribution of broker lifecycle events over SSE")
	public @interface Config {

		@AttributeDefinition(
				name = "Heartbeat (seconds)",
				description = "Interval for SSE comment frames (\": keepalive\"). Keeps consumer readers "
						+ "waking up regularly — a blocked read would otherwise pin the connection "
						+ "beyond its subscription's close — and prunes dead subscribers. 0 disables.")
		int heartbeat_seconds() default 10;
	}

	/** One connected consumer. */
	private static final class Subscription {

		private final SseEventSink sink;

		/** Flavors this consumer can speak; empty means "no filter". */
		private final Set<FlavorKind> flavors;

		/** Who this is, when it said so; {@code null} otherwise. */
		private final String consumerId;

		Subscription(SseEventSink sink, Set<FlavorKind> flavors, String consumerId) {
			this.sink = sink;
			this.flavors = flavors;
			this.consumerId = consumerId;
		}
	}

	private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

	@Reference
	BrokerLookup broker;

	/**
	 * Told when a consumer's connection comes and goes.
	 *
	 * <p>An open event stream is a presence signal the broker gets for
	 * free, and losing one is worth acting on sooner than the renewal
	 * timeout (ACQUISITION.md §4).
	 */
	@Reference
	BrokerSessions sessions;

	@Reference(target = "(emf.name=services)")
	ComponentServiceObjects<ResourceSet> rsObjects;

	private volatile Sse sse;

	private ScheduledExecutorService heartbeat;

	@Activate
	void activate(Config config) {
		int interval = config.heartbeat_seconds();
		if (interval > 0) {
			heartbeat = Executors.newSingleThreadScheduledExecutor(task -> {
				Thread thread = new Thread(task, "ddsr-sse-heartbeat");
				thread.setDaemon(true);
				return thread;
			});
			heartbeat.scheduleAtFixedRate(this::sendHeartbeat, interval, interval, TimeUnit.SECONDS);
		}
	}

	@Deactivate
	void deactivate() {
		if (heartbeat != null) {
			heartbeat.shutdownNow();
			heartbeat = null;
		}
		for (Subscription s : subscriptions) {
			try {
				s.sink.close();
			} catch (RuntimeException ignored) {
				// Shutting down anyway.
			}
		}
		subscriptions.clear();
	}

	/**
	 * Sends an SSE comment frame to every subscriber. Consumers ignore
	 * comments by protocol, but the frame makes a blocked reader wake up
	 * regularly — without it, a consumer's stream close can only take
	 * effect at the NEXT real event, and on the server side dead
	 * connections would linger unnoticed until then.
	 */
	void sendHeartbeat() {
		Sse currentSse = sse;
		if (currentSse == null || subscriptions.isEmpty()) {
			return;
		}
		OutboundSseEvent keepalive = currentSse.newEventBuilder()
				.comment("keepalive")
				.build();
		for (Subscription s : subscriptions) {
			try {
				if (s.sink.isClosed()) {
					drop(s);
					continue;
				}
				s.sink.send(keepalive);
			} catch (RuntimeException sendFailure) {
				drop(s);
			}
		}
	}

	/**
	 * Registers a new subscriber. Called from {@link EventsResource} with
	 * the injected JAX-RS objects, since only a resource method can
	 * obtain them.
	 *
	 * @param sse     the SSE factory, kept for building outbound events
	 * @param sink    the subscriber's sink
	 * @param flavors flavors the consumer can speak; empty = no filter
	 * @param consumerId who is subscribing, or {@code null} when the
	 *        subscriber does not say
	 */
	void subscribe(Sse sse, SseEventSink sink, Set<FlavorKind> flavors, String consumerId) {
		this.sse = sse;
		subscriptions.add(new Subscription(sink, flavors, consumerId));
		arrived(consumerId);
		LOG.info("[DDSR] SSE subscriber added, now " + subscriptions.size()
				+ (flavors.isEmpty() ? " (no flavor filter)" : " (flavors=" + flavors + ")")
				+ (consumerId == null || consumerId.isBlank() ? "" : " for " + consumerId));
	}

	/**
	 * Drops a subscription and, if it was the last one this consumer had,
	 * tells the broker its connection is gone.
	 *
	 * <p>Counting matters: a consumer that reconnects before the old
	 * sink is pruned briefly has two, and reporting the first one's end
	 * would start a deadline for a consumer that is demonstrably there.
	 */
	private void drop(Subscription subscription) {
		subscriptions.remove(subscription);
		left(subscription.consumerId);
	}

	/**
	 * How many streams a consumer currently holds.
	 *
	 * <p>A counter rather than a scan of the subscription list, because
	 * the two ends of a connection are reported by different threads and
	 * the answer has to be decided in one step. Counting the list meant
	 * removing a subscription, looking, and reporting — and a reconnect
	 * landing in that gap left a consumer that is demonstrably here
	 * carrying a disconnect deadline, after which its session expired
	 * and its leases were released underneath it (#127).
	 */
	private final Map<String, Integer> streamsPerConsumer = new ConcurrentHashMap<>();

	/** Reports a consumer as present on its first stream, and only then. */
	private void arrived(String consumerId) {
		if (consumerId == null || consumerId.isBlank()) {
			return;
		}
		if (streamsPerConsumer.merge(consumerId, 1, Integer::sum) == 1) {
			sessions.consumerConnected(consumerId);
		}
	}

	/** Reports a consumer as gone when its last stream ends, and only then. */
	private void left(String consumerId) {
		if (consumerId == null || consumerId.isBlank()) {
			return;
		}
		Integer remaining = streamsPerConsumer.computeIfPresent(consumerId,
				(id, count) -> count <= 1 ? null : count - 1);
		if (remaining == null) {
			sessions.consumerDisconnected(consumerId);
		}
	}

	int subscriberCount() {
		return subscriptions.size();
	}

	@Override
	public void publish(ServiceEvent event) {
		if (subscriptions.isEmpty() || sse == null) {
			return;
		}
		// Serialize once for all subscribers. Since #124 this runs on the
		// broker's delivery thread, not on its mutating thread under the
		// write lock — so taking a moment here no longer stops the
		// registry. It still does no I/O: the send itself is what may
		// block, and blocking it is now this bridge's problem alone.
		String payload;
		Set<FlavorKind> eventFlavors;
		try {
			eventFlavors = EventDocument.flavorsOf(event, broker);
			payload = toXmi(event);
		} catch (IOException | RuntimeException failure) {
			// Never let a distribution problem escape into the broker.
			LOG.log(Level.WARNING, "[DDSR] could not render ServiceEvent for SSE", failure);
			return;
		}

		OutboundSseEvent outbound = sse.newEventBuilder()
				.name("ddsr-service-event")
				.mediaType(MediaType.APPLICATION_XML_TYPE)
				.data(String.class, payload)
				.build();

		for (Subscription s : subscriptions) {
			if (!matches(s, eventFlavors)) {
				continue;
			}
			try {
				if (s.sink.isClosed()) {
					drop(s);
					continue;
				}
				s.sink.send(outbound);
			} catch (RuntimeException sendFailure) {
				// A dead subscriber must not affect the others or the broker.
				drop(s);
			}
		}
	}

	/**
	 * FR-Sync-Filtering: a consumer that only speaks REST must not
	 * receive events about MQTT-only implementations. An event whose
	 * implementation declares no flavor at all is delivered to everyone —
	 * same rule the lookup backend applies.
	 */
	static boolean matches(Subscription s, Set<FlavorKind> eventFlavors) {
		if (s.flavors.isEmpty() || eventFlavors.isEmpty()) {
			return true;
		}
		for (FlavorKind kind : eventFlavors) {
			if (s.flavors.contains(kind)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Renders the event as a self-contained XMI document. The document
	 * shape — and the two traps in it — live in {@link EventDocument},
	 * shared with every other transport.
	 */
	private String toXmi(ServiceEvent event) throws IOException {
		List<EObject> roots = EventDocument.roots(event, broker);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, rsObjects, roots);
		return out.toString(StandardCharsets.UTF_8);
	}
}
