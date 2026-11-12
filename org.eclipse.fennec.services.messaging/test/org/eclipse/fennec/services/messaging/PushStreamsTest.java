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

package org.eclipse.fennec.services.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;

/**
 * A PushStream as the Flow.Publisher a stream travels as: the stream
 * pushes, the subscriber asks, and where the two disagree the stream
 * waits.
 */
class PushStreamsTest {

	/** Takes a stream one value at a time, on demand of the test. */
	private static final class Taking<T> implements Flow.Subscriber<T> {
		final List<T> taken = new CopyOnWriteArrayList<>();
		final CompletableFuture<Throwable> ended = new CompletableFuture<>();
		volatile Flow.Subscription subscription;

		@Override
		public void onSubscribe(Flow.Subscription asked) {
			this.subscription = asked;
		}

		@Override
		public void onNext(T value) {
			taken.add(value);
		}

		@Override
		public void onError(Throwable failure) {
			ended.complete(failure);
		}

		@Override
		public void onComplete() {
			ended.complete(null);
		}

		/** A publisher may hand over the subscription on a thread of its own. */
		Flow.Subscription subscribed() throws InterruptedException {
			for (int i = 0; i < 250 && subscription == null; i++) {
				Thread.sleep(20);
			}
			return subscription;
		}

		void awaitTaken(int count) throws InterruptedException {
			for (int i = 0; i < 250 && taken.size() < count; i++) {
				Thread.sleep(20);
			}
		}
	}

	@Test
	void theStreamIsHandedOnAsTheSubscriberAsksAndNoFaster() throws Exception {
		PushStreamProvider provider = new PushStreamProvider();
		try (SimplePushEventSource<Integer> source = provider.createSimpleEventSource(Integer.class)) {
			PushStream<Integer> stream = provider.buildStream(source).unbuffered().build();
			Taking<Integer> taking = new Taking<>();
			PushStreams.publisherOf(stream).subscribe(taking);
			Thread feeder = new Thread(() -> {
				for (int i = 1; i <= 3; i++) {
					source.publish(i);
				}
				source.endOfStream();
			});
			feeder.setDaemon(true);
			feeder.start();

			Thread.sleep(200);
			assertThat(taking.taken).as("nothing was asked for, so nothing was handed on").isEmpty();

			taking.subscribed().request(1);
			taking.awaitTaken(1);
			Thread.sleep(100);
			assertThat(taking.taken).as("one was asked for").containsExactly(1);

			taking.subscribed().request(Long.MAX_VALUE);
			assertThat(taking.ended.get(5, TimeUnit.SECONDS)).isNull();
			assertThat(taking.taken).containsExactly(1, 2, 3);
		}
	}

	@Test
	void aSubscriberThatCancelsClosesTheStream() throws Exception {
		PushStreamProvider provider = new PushStreamProvider();
		try (SimplePushEventSource<Integer> source = provider.createSimpleEventSource(Integer.class)) {
			PushStream<Integer> stream = provider.buildStream(source).unbuffered().build();
			Taking<Integer> taking = new Taking<>();
			PushStreams.publisherOf(stream).subscribe(taking);
			for (int i = 0; i < 100 && !source.isConnected(); i++) {
				Thread.sleep(20);
			}
			assertThat(source.isConnected()).isTrue();

			taking.subscribed().cancel();

			for (int i = 0; i < 100 && source.isConnected(); i++) {
				Thread.sleep(20);
			}
			assertThat(source.isConnected()).as("nobody is listening any more").isFalse();
		}
	}

	@Test
	void whatAServiceReturnedIsAStreamOrNot() {
		PushStreamProvider provider = new PushStreamProvider();
		PushStream<String> stream = provider.streamOf(Stream.of("a"));
		Flow.Publisher<Object> publisher = subscriber -> { };

		assertThat(PushStreams.asPublisher(stream)).isNotNull();
		assertThat(PushStreams.asPublisher(publisher)).isSameAs(publisher);
		assertThat(PushStreams.asPublisher("a value")).isNull();
		assertThat(PushStreams.asPublisher(null)).isNull();
		stream.close();
	}
}
