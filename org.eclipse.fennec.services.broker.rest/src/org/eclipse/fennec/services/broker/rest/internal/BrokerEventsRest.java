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

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.broker.core.EventDocument;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.invocation.BroadcastReceivers;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The events contract ({@code BrokerEvents}), as a service the generic
 * distribution can serve (#252).
 *
 * <p>The broker's events used to have a transport of their own — an SSE
 * resource and a bridge that wrote its frames. They are a contract now:
 * {@code events()} is a stream every receiver shares, and the generic REST
 * distribution serves it as it serves any broadcast. This class is the
 * source: the broker's event sink, turning every event into the next
 * value — the event with what it is about, copied out of the broker's
 * state ({@link EventDocument}).
 *
 * <p>Two things the broker needs beyond a plain broadcast. When an event
 * could not be delivered ({@link #resyncRequired}) every run of the stream
 * ends, every receiver is told the stream is over and takes a fresh
 * snapshot when it connects again (#124). And a receiver that says which
 * consumer it is ties its connection to that consumer's session: the
 * broker hears when the consumer's last stream goes (ACQUISITION.md §4).
 */
@Component(service = { BrokerEventsRest.class, EventSink.class }, immediate = true,
		property = "ddsr.contract=BrokerEvents")
public class BrokerEventsRest implements EventSink, BroadcastReceivers {

	private static final Logger LOG = Logger.getLogger(BrokerEventsRest.class.getName());

	/** The request parameter a receiver names its consumer with. */
	static final String CONSUMER_ID = "consumerId";

	/** The runs of the stream open now: one per subscriber the distribution made. */
	private final Set<Run> runs = ConcurrentHashMap.newKeySet();

	/** How many streams each consumer holds — reported when the first comes and the last goes (#127). */
	private final Map<String, Integer> streamsPerConsumer = new ConcurrentHashMap<>();

	/**
	 * The broker this sink is part of. Dynamic and optional on purpose:
	 * the broker binds its event sinks, this one among them, while it is
	 * being built — a static reference back to it is a cycle Declarative
	 * Services refuses. Until it is there, an event is copied without the
	 * lookup ({@link EventDocument} falls back to what the event carries).
	 */
	private final AtomicReference<BrokerLookup> broker = new AtomicReference<>();

	/** The sessions a receiver's connection is tied to; the same broker, for the same reason. */
	private final AtomicReference<BrokerSessions> sessions = new AtomicReference<>();

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	void setBroker(BrokerLookup lookup) {
		broker.set(lookup);
	}

	void unsetBroker(BrokerLookup lookup) {
		broker.compareAndSet(lookup, null);
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	void setSessions(BrokerSessions brokerSessions) {
		sessions.set(brokerSessions);
	}

	void unsetSessions(BrokerSessions brokerSessions) {
		sessions.compareAndSet(brokerSessions, null);
	}

	/** The contract's one operation: every event, from now on, for every receiver. */
	public Flow.Publisher<ServiceEvent> events() {
		return subscriber -> {
			Run run = new Run(subscriber);
			runs.add(run);
			subscriber.onSubscribe(run);
		};
	}

	@Override
	public void publish(ServiceEvent event) {
		if (runs.isEmpty()) {
			return;
		}
		ServiceEvent value;
		try {
			List<EObject> roots = EventDocument.roots(event, broker.get());
			value = (ServiceEvent) roots.get(0);
		} catch (RuntimeException failure) {
			LOG.warning("[DDSR] could not render an event: " + failure);
			return;
		}
		for (Run run : runs) {
			run.offer(value);
		}
	}

	/**
	 * Ends every run, so that every receiver re-reads what it holds
	 * (#124): the cheaper truth than a frame of its own, because a
	 * consumer already re-reads everything when its stream comes up.
	 */
	@Override
	public void resyncRequired() {
		if (runs.isEmpty()) {
			return;
		}
		LOG.warning("[DDSR] an event did not reach the wire — ending the event stream so consumers re-snapshot");
		for (Run run : List.copyOf(runs)) {
			run.complete();
		}
	}

	@Override
	public void joined(String operation, Map<String, String> parameters) {
		String consumerId = parameters.get(CONSUMER_ID);
		if (consumerId != null && !consumerId.isBlank()
				&& streamsPerConsumer.merge(consumerId, 1, Integer::sum) == 1) {
			BrokerSessions bound = sessions.get();
			if (bound != null) {
				bound.consumerConnected(consumerId);
			}
		}
	}

	@Override
	public void left(String operation, Map<String, String> parameters) {
		String consumerId = parameters.get(CONSUMER_ID);
		if (consumerId == null || consumerId.isBlank()) {
			return;
		}
		Integer remaining = streamsPerConsumer.computeIfPresent(consumerId,
				(id, count) -> count <= 1 ? null : count - 1);
		BrokerSessions bound = sessions.get();
		if (remaining == null && bound != null) {
			bound.consumerDisconnected(consumerId);
		}
	}

	@Deactivate
	void deactivate() {
		for (Run run : List.copyOf(runs)) {
			run.complete();
		}
	}

	/**
	 * One subscriber of the stream: the events it asked for go out at
	 * once, the others wait — the distribution asks for the next as soon
	 * as it has handed one on, so the queue stays short.
	 */
	private final class Run implements Flow.Subscription {

		private final Flow.Subscriber<? super ServiceEvent> subscriber;

		private final ArrayDeque<ServiceEvent> waiting = new ArrayDeque<>();

		private long demand;

		private boolean over;

		Run(Flow.Subscriber<? super ServiceEvent> subscriber) {
			this.subscriber = subscriber;
		}

		void offer(ServiceEvent event) {
			synchronized (this) {
				if (over) {
					return;
				}
				waiting.add(event);
			}
			drain();
		}

		void complete() {
			synchronized (this) {
				if (over) {
					return;
				}
				over = true;
			}
			runs.remove(this);
			subscriber.onComplete();
		}

		@Override
		public void request(long n) {
			synchronized (this) {
				demand = demand + n < 0 ? Long.MAX_VALUE : demand + n;
			}
			drain();
		}

		@Override
		public void cancel() {
			synchronized (this) {
				over = true;
				waiting.clear();
			}
			runs.remove(this);
		}

		private void drain() {
			while (true) {
				ServiceEvent next;
				synchronized (this) {
					if (over || demand == 0 || waiting.isEmpty()) {
						return;
					}
					demand--;
					next = waiting.poll();
				}
				subscriber.onNext(next);
			}
		}
	}
}
