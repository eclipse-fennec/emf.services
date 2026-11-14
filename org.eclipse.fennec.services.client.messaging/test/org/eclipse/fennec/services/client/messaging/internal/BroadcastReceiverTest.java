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

package org.eclipse.fennec.services.client.messaging.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.junit.jupiter.api.Test;

/** A receiver of a stream every receiver shares, over messaging (#252). */
class BroadcastReceiverTest {

	private static CloudEventCodec.Message value(String text) {
		return new CloudEventCodec.Message(CloudEvents.newEnvelope(CloudEvents.TYPE_BROADCAST, "/test", "text/plain"),
				text.getBytes(UTF_8));
	}

	private static CloudEventCodec.Message end() {
		return new CloudEventCodec.Message(CloudEvents.newEnvelope(CloudEvents.TYPE_BROADCAST_END, "/test", null), null);
	}

	private static final class Recorder implements Flow.Subscriber<Object> {

		final List<Object> values = new CopyOnWriteArrayList<>();

		final CompletableFuture<Throwable> ended = new CompletableFuture<>();

		final AtomicReference<Flow.Subscription> subscription = new AtomicReference<>();

		@Override
		public void onSubscribe(Flow.Subscription given) {
			subscription.set(given);
		}

		@Override
		public void onNext(Object item) {
			values.add(item);
		}

		@Override
		public void onError(Throwable throwable) {
			ended.complete(throwable);
		}

		@Override
		public void onComplete() {
			ended.complete(null);
		}
	}

	private final AtomicBoolean closed = new AtomicBoolean();

	private final AtomicReference<BroadcastReceiver> opened = new AtomicReference<>();

	private BroadcastReceiver receiver(Recorder recorder) {
		BroadcastReceiver receiver = new BroadcastReceiver(recorder,
				message -> new String(message.data(), UTF_8), "the broadcast of readings");
		receiver.start(self -> {
			opened.set(self);
			return () -> closed.set(true);
		});
		return receiver;
	}

	@Test
	void valuesAreHandedOnAsAskedForAndTheEndCompletes() throws Exception {
		Recorder recorder = new Recorder();
		BroadcastReceiver receiver = receiver(recorder);
		recorder.subscription.get().request(2);

		receiver.received(value("1"));
		receiver.received(value("2"));
		receiver.received(end());

		assertThat(recorder.values).containsExactly("1", "2");
		assertThat(recorder.ended.get(1, TimeUnit.SECONDS)).isNull();
		assertThat(closed).as("the subscription goes with the end").isTrue();
	}

	@Test
	void deliveryWaitsForDemandWhichIsWhatFillsTheBuffer() throws Exception {
		Recorder recorder = new Recorder();
		BroadcastReceiver receiver = receiver(recorder);
		CompletableFuture<Void> delivering = CompletableFuture.runAsync(() -> receiver.received(value("1")));

		Thread.sleep(200);
		assertThat(delivering).as("held up until a value is asked for").isNotDone();
		assertThat(recorder.values).isEmpty();

		recorder.subscription.get().request(1);
		delivering.get(1, TimeUnit.SECONDS);
		assertThat(recorder.values).containsExactly("1");
	}

	@Test
	void aBufferThatFailsEndsTheStreamSayingTheReceiverFellBehind() throws Exception {
		Recorder recorder = new Recorder();
		receiver(recorder);

		opened.get().overflowed(new IllegalStateException("buffer full"));

		assertThat(recorder.ended.get(1, TimeUnit.SECONDS)).isInstanceOf(DdsrException.class)
				.hasMessageContaining("fell behind");
		assertThat(closed).isTrue();
	}

	@Test
	void cancellingClosesTheSubscriptionAndReleasesAWaitingDelivery() throws Exception {
		Recorder recorder = new Recorder();
		BroadcastReceiver receiver = receiver(recorder);
		CompletableFuture<Void> delivering = CompletableFuture.runAsync(() -> receiver.received(value("1")));
		Thread.sleep(100);

		recorder.subscription.get().cancel();

		delivering.get(1, TimeUnit.SECONDS);
		assertThat(closed).isTrue();
		assertThat(recorder.values).isEmpty();
		assertThat(recorder.ended).isNotDone();
	}
}
