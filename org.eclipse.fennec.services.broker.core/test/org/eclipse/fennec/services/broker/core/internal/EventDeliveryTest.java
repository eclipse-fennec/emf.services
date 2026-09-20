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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * The hand-off between the broker and its subscribers (#124).
 *
 * <p>Three properties carry the change and each is worth a test of its
 * own: what was enqueued in mutation order is delivered in that order,
 * a subscriber can no longer make the broker wait, and nothing a
 * subscriber does — throwing, blocking, never reading again — may stop
 * the events that come after it.
 *
 * <p>Every wait here is on a latch or on a counted barrier, never on a
 * sleep. A concurrency test that waits "long enough" passes for the
 * wrong reason on a loaded machine and proves nothing on a fast one.
 * The {@code @Timeout} is the safety net, not the mechanism: if one of
 * these hangs, that IS the failure.
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class EventDeliveryTest {

	private EventDelivery delivery;

	@AfterEach
	void tearDown() {
		if (delivery != null) {
			delivery.close();
		}
	}

	// ------------------------------------------------------------------
	// What it is for
	// ------------------------------------------------------------------

	@Test
	@DisplayName("delivers what was submitted")
	void deliversWhatWasSubmitted() {
		CollectingSink sink = new CollectingSink();
		delivery = new EventDelivery(sink);

		delivery.submit(event(ServiceEventType.REGISTERED));
		delivery.awaitIdle();

		assertThat(sink.types()).containsExactly(ServiceEventType.REGISTERED);
	}

	@Test
	@DisplayName("keeps the order it was given, which is the order the mutations happened in")
	void keepsSubmissionOrder() {
		CollectingSink sink = new CollectingSink();
		delivery = new EventDelivery(sink);

		// The order that matters in practice: a republish retires the old
		// copy before the new one appears, repeated often enough that a
		// single reordering anywhere would show.
		for (int i = 0; i < 200; i++) {
			delivery.submit(event(ServiceEventType.UNREGISTERING));
			delivery.submit(event(ServiceEventType.REGISTERED));
		}
		delivery.awaitIdle();

		assertThat(sink.types()).hasSize(400);
		for (int i = 0; i < 400; i += 2) {
			assertThat(sink.types().get(i)).isEqualTo(ServiceEventType.UNREGISTERING);
			assertThat(sink.types().get(i + 1)).isEqualTo(ServiceEventType.REGISTERED);
		}
	}

	@Test
	@DisplayName("a slow sink does not reorder anything, it only arrives later")
	void orderSurvivesASlowSink() throws InterruptedException {
		CountDownLatch firstArrived = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CollectingSink sink = new CollectingSink() {
			@Override
			public void publish(ServiceEvent event) {
				if (types().isEmpty()) {
					firstArrived.countDown();
					await(release);
				}
				super.publish(event);
			}
		};
		delivery = new EventDelivery(sink);

		delivery.submit(named(ServiceEventType.REGISTERED, "first"));
		firstArrived.await();
		delivery.submit(named(ServiceEventType.MODIFIED, "second"));
		delivery.submit(named(ServiceEventType.UNREGISTERING, "third"));
		release.countDown();
		delivery.awaitIdle();

		assertThat(sink.reasons()).containsExactly("first", "second", "third");
	}

	@Test
	@DisplayName("the broker does not wait for a subscriber — that is the whole point")
	void submitDoesNotWaitForTheSink() throws InterruptedException {
		CountDownLatch inSink = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		delivery = new EventDelivery(e -> {
			inSink.countDown();
			await(release);
		});

		delivery.submit(event(ServiceEventType.REGISTERED));
		assertThat(inSink.await(10, TimeUnit.SECONDS))
				.as("the delivery thread should have reached the sink")
				.isTrue();

		// The sink is now stuck. Submitting must still return, and it must
		// return without the sink having been released.
		for (int i = 0; i < 50; i++) {
			delivery.submit(event(ServiceEventType.MODIFIED));
		}
		assertThat(release.getCount())
				.as("the submits must not have waited for the blocked sink")
				.isEqualTo(1);

		release.countDown();
	}

	// ------------------------------------------------------------------
	// A subscriber misbehaving
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a sink that throws does not stop the events after it")
	void aThrowingSinkDoesNotStopDelivery() {
		List<ServiceEventType> seen = Collections.synchronizedList(new ArrayList<>());
		delivery = new EventDelivery(e -> {
			if (e.getType() == ServiceEventType.MODIFIED) {
				throw new IllegalStateException("a subscriber that breaks its contract");
			}
			seen.add(e.getType());
		});

		delivery.submit(event(ServiceEventType.REGISTERED));
		delivery.submit(event(ServiceEventType.MODIFIED));
		delivery.submit(event(ServiceEventType.UNREGISTERING));
		delivery.awaitIdle();

		assertThat(seen).containsExactly(ServiceEventType.REGISTERED, ServiceEventType.UNREGISTERING);
	}

	@Test
	@DisplayName("a sink that throws still counts as done, so nothing waits on it forever")
	void aThrowingSinkDoesNotHangTheBarrier() {
		delivery = new EventDelivery(e -> {
			throw new IllegalStateException("every single time");
		});

		delivery.submit(event(ServiceEventType.REGISTERED));
		delivery.submit(event(ServiceEventType.REGISTERED));

		assertThatCode(() -> delivery.awaitIdle()).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("the barrier gives up rather than spinning forever when the delivery thread is gone")
	void theBarrierGivesUpWhenTheDispatcherDied() {
		// An Error is not a RuntimeException and is deliberately not
		// caught — an OutOfMemoryError must not be swallowed and retried.
		// What must not happen is that everything else then hangs on a
		// barrier waiting for a thread that no longer exists.
		delivery = new EventDelivery(e -> {
			throw new StackOverflowError("the dispatcher does not survive this");
		});

		delivery.submit(event(ServiceEventType.REGISTERED));

		assertThatCode(() -> delivery.awaitIdle()).doesNotThrowAnyException();
	}

	// ------------------------------------------------------------------
	// A subscriber that stopped reading: the bounded queue
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a subscriber that stopped reading costs events, not the broker")
	void aFullQueueDropsInsteadOfGrowing() throws InterruptedException {
		CountDownLatch inSink = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		delivery = new EventDelivery(e -> {
			inSink.countDown();
			await(release);
		});

		delivery.submit(event(ServiceEventType.REGISTERED));
		inSink.await();

		// One more than fits: the first is being held inside the sink, so
		// CAPACITY more fill the queue and everything beyond is dropped.
		int excess = 25;
		for (int i = 0; i < EventDelivery.CAPACITY + excess; i++) {
			delivery.submit(event(ServiceEventType.MODIFIED));
		}

		assertThat(delivery.droppedCount())
				.as("everything past the bound is dropped, not queued")
				.isEqualTo(excess);
		release.countDown();
	}

	@Test
	@DisplayName("after a drop the queue keeps working — a loss is not a shutdown")
	void deliveryContinuesAfterADrop() throws InterruptedException {
		CountDownLatch inSink = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CollectingSink sink = new CollectingSink() {
			@Override
			public void publish(ServiceEvent event) {
				if ("blocker".equals(event.getReasonCode())) {
					inSink.countDown();
					await(release);
				}
				super.publish(event);
			}
		};
		delivery = new EventDelivery(sink);

		delivery.submit(named(ServiceEventType.REGISTERED, "blocker"));
		inSink.await();
		for (int i = 0; i < EventDelivery.CAPACITY + 10; i++) {
			delivery.submit(event(ServiceEventType.MODIFIED));
		}
		release.countDown();
		delivery.awaitIdle();

		long dropped = delivery.droppedCount();
		delivery.submit(named(ServiceEventType.RETIRED, "after-the-drop"));
		delivery.awaitIdle();

		assertThat(sink.reasons()).contains("after-the-drop");
		assertThat(delivery.droppedCount())
				.as("the later event was not dropped too")
				.isEqualTo(dropped);
	}

	@Test
	@DisplayName("a burst that fits the bound is never dropped, even if the sink has not started")
	void aBurstWithinTheBoundIsNeverDropped() {
		CollectingSink sink = new CollectingSink();
		delivery = new EventDelivery(sink);

		// The worst case for a burst of this size: assume the sink has
		// taken nothing at all yet. Exactly CAPACITY still fits.
		for (int i = 0; i < EventDelivery.CAPACITY; i++) {
			delivery.submit(event(ServiceEventType.REGISTERED));
		}
		delivery.awaitIdle();

		assertThat(delivery.droppedCount()).isZero();
		assertThat(sink.types()).hasSize(EventDelivery.CAPACITY);
	}

	@Test
	@DisplayName("a producer that outruns the sink for longer than the bound does lose events")
	void outrunningTheSinkForLongerThanTheBoundDropsEvents() {
		// Not a defect, a documented limit, and it is here so that nobody
		// reads the bound as "the queue is effectively infinite". In the
		// broker this cannot be reached by submitting alone: emits are
		// separated by a mutation and a snapshot write, which is orders
		// of magnitude slower than a sink taking an event off a queue.
		// A tight loop has no such spacing.
		CollectingSink sink = new CollectingSink();
		delivery = new EventDelivery(sink);

		for (int i = 0; i < EventDelivery.CAPACITY * 10; i++) {
			delivery.submit(event(ServiceEventType.REGISTERED));
		}
		delivery.awaitIdle();

		assertThat(delivery.droppedCount() + sink.types().size())
				.as("every event is either delivered or counted as dropped — none vanishes quietly")
				.isEqualTo(EventDelivery.CAPACITY * 10);
	}

	// ------------------------------------------------------------------
	// Nothing, null, twice, and after the end
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a null sink is the no-op sink, not a null pointer")
	void aNullSinkIsTheNoOpSink() {
		delivery = new EventDelivery(null);

		assertThatCode(() -> {
			delivery.submit(event(ServiceEventType.REGISTERED));
			delivery.awaitIdle();
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("a null event is ignored, and does not start anything either")
	void aNullEventIsIgnored() {
		CollectingSink sink = new CollectingSink();
		delivery = new EventDelivery(sink);

		delivery.submit(null);
		delivery.awaitIdle();

		assertThat(sink.types()).isEmpty();
		assertThat(delivery.droppedCount()).isZero();
	}

	@Test
	@DisplayName("the barrier returns at once when nothing was ever submitted")
	void anIdleDeliveryIsAlreadyIdle() {
		delivery = new EventDelivery(new CollectingSink());

		assertThatCode(() -> delivery.awaitIdle()).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("closing without ever having delivered anything is not a hang")
	void closingAnUnusedDeliveryIsHarmless() {
		delivery = new EventDelivery(new CollectingSink());

		assertThatCode(() -> delivery.close()).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("closing twice is closing once")
	void closeIsIdempotent() {
		delivery = new EventDelivery(new CollectingSink());
		delivery.submit(event(ServiceEventType.REGISTERED));

		delivery.close();

		assertThatCode(() -> delivery.close()).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("what is queued at close is still delivered — those changes are already saved")
	void closeDrainsWhatIsQueued() {
		CollectingSink sink = new CollectingSink();
		delivery = new EventDelivery(sink);

		for (int i = 0; i < 100; i++) {
			delivery.submit(event(ServiceEventType.REGISTERED));
		}
		delivery.close();

		assertThat(sink.types())
				.as("a subscriber still connected during a shutdown hears what happened")
				.hasSize(100);
	}

	@Test
	@DisplayName("after close nothing is accepted any more")
	void submitAfterCloseIsIgnored() {
		CollectingSink sink = new CollectingSink();
		delivery = new EventDelivery(sink);
		delivery.submit(event(ServiceEventType.REGISTERED));
		delivery.close();
		int delivered = sink.types().size();

		delivery.submit(event(ServiceEventType.MODIFIED));

		assertThat(sink.types()).hasSize(delivered);
	}

	@Test
	@DisplayName("the delivery thread is a daemon — an event may not keep a JVM alive")
	void theDispatcherIsADaemon() throws InterruptedException {
		AtomicReference<Thread> onSink = new AtomicReference<>();
		CountDownLatch arrived = new CountDownLatch(1);
		delivery = new EventDelivery(e -> {
			onSink.set(Thread.currentThread());
			arrived.countDown();
		});

		delivery.submit(event(ServiceEventType.REGISTERED));
		arrived.await();

		assertThat(onSink.get().isDaemon()).isTrue();
		assertThat(onSink.get().getName()).isEqualTo("ddsr-event-delivery");
		assertThat(onSink.get())
				.as("delivery happens off the submitting thread")
				.isNotEqualTo(Thread.currentThread());
	}

	// ------------------------------------------------------------------
	// Fixtures
	// ------------------------------------------------------------------

	private static ServiceEvent event(ServiceEventType type) {
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(type);
		return event;
	}

	private static ServiceEvent named(ServiceEventType type, String reason) {
		ServiceEvent event = event(type);
		event.setReasonCode(reason);
		return event;
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	/** Records on the delivery thread, read from the test thread. */
	private static class CollectingSink implements EventSink {

		private final List<ServiceEvent> events = Collections.synchronizedList(new ArrayList<>());

		@Override
		public void publish(ServiceEvent event) {
			events.add(event);
		}

		List<ServiceEventType> types() {
			synchronized (events) {
				return events.stream().map(ServiceEvent::getType).toList();
			}
		}

		List<String> reasons() {
			synchronized (events) {
				return events.stream().map(ServiceEvent::getReasonCode).toList();
			}
		}
	}
}
