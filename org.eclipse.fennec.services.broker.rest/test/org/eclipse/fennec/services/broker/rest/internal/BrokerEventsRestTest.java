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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.junit.jupiter.api.Test;

/** The broker's events, as the source of a broadcast (#252). */
class BrokerEventsRestTest {

	private final List<String> sessionCalls = new ArrayList<>();

	private BrokerEventsRest events() {
		BrokerEventsRest events = new BrokerEventsRest();
		events.broker = (BrokerLookup) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { BrokerLookup.class }, (self, method, args) -> null);
		events.sessions = (BrokerSessions) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { BrokerSessions.class }, (self, method, args) -> {
					sessionCalls.add(method.getName() + " " + args[0]);
					return null;
				});
		return events;
	}

	private static ServiceEvent registered(String id) {
		org.eclipse.fennec.services.ServiceReference reference = ServicesFactory.eINSTANCE.createServiceReference();
		reference.setId(id);
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(ServiceEventType.REGISTERED);
		event.setReference(reference);
		return event;
	}

	/** Takes what a run delivers and how it ends. */
	private static final class Run implements Flow.Subscriber<ServiceEvent> {

		final List<ServiceEvent> values = new ArrayList<>();

		Flow.Subscription subscription;

		boolean complete;

		@Override
		public void onSubscribe(Flow.Subscription given) {
			subscription = given;
		}

		@Override
		public void onNext(ServiceEvent item) {
			values.add(item);
		}

		@Override
		public void onError(Throwable throwable) {
			throw new AssertionError(throwable);
		}

		@Override
		public void onComplete() {
			complete = true;
		}
	}

	@Test
	void everyEventIsTheNextValueACopyOfWhatHappened() {
		BrokerEventsRest events = events();
		Run run = new Run();
		events.events().subscribe(run);
		run.subscription.request(1);

		ServiceEvent first = registered("ref-1");
		events.publish(first);
		events.publish(registered("ref-2"));

		assertThat(run.values).as("only what was asked for").hasSize(1);
		assertThat(run.values.get(0)).as("copied out of the broker's state").isNotSameAs(first);
		assertThat(run.values.get(0).getReference().getId()).isEqualTo("ref-1");

		run.subscription.request(1);
		assertThat(run.values).extracting(event -> event.getReference().getId()).containsExactly("ref-1", "ref-2");
	}

	@Test
	void aLostEventEndsTheRunSoThatEveryReceiverRereads() {
		BrokerEventsRest events = events();
		Run run = new Run();
		events.events().subscribe(run);

		events.resyncRequired();

		assertThat(run.complete).isTrue();
		events.publish(registered("ref-1"));
		run.subscription.request(1);
		assertThat(run.values).as("a run that is over hears nothing").isEmpty();
	}

	@Test
	void aConsumerIsConnectedWithItsFirstStreamAndGoneWithItsLast() {
		BrokerEventsRest events = events();
		Map<String, String> consumer = Map.of(BrokerEventsRest.CONSUMER_ID, "orders");

		events.joined("events", consumer);
		events.joined("events", consumer);
		events.left("events", consumer);
		assertThat(sessionCalls).containsExactly("consumerConnected orders");

		events.left("events", consumer);
		assertThat(sessionCalls).containsExactly("consumerConnected orders", "consumerDisconnected orders");

		events.joined("events", Map.of());
		assertThat(sessionCalls).as("a receiver that names no consumer is just a receiver").hasSize(2);
	}
}
