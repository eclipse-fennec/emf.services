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

package org.eclipse.fennec.services.invocation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/** The little a stream needs between a transport and a caller. */
class FlowsTest {

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
	void everyValueIsTurnedIntoAnotherAndTheEndIsTheEnd() throws Exception {
		Taking<String> taking = new Taking<>();
		try (SubmissionPublisher<Integer> source = new SubmissionPublisher<>()) {
			Flows.map(source, number -> "n" + number).subscribe(taking);
			taking.subscribed().request(Long.MAX_VALUE);
			source.submit(1);
			source.submit(2);
		}

		assertThat(taking.ended.get(5, TimeUnit.SECONDS)).isNull();
		assertThat(taking.taken).containsExactly("n1", "n2");
	}

	@Test
	void aFunctionThatFailsEndsTheStreamWithItsFailure() throws Exception {
		Taking<String> taking = new Taking<>();
		IllegalStateException unreadable = new IllegalStateException("unreadable");
		try (SubmissionPublisher<Integer> source = new SubmissionPublisher<>()) {
			Flows.<Integer, String>map(source, number -> {
				throw unreadable;
			}).subscribe(taking);
			taking.subscribed().request(Long.MAX_VALUE);
			source.submit(1);

			assertThat(taking.ended.get(5, TimeUnit.SECONDS)).isSameAs(unreadable);
			assertThat(taking.taken).isEmpty();
		}
	}

	@Test
	void aFailureIsToldInTheWordsOfWhoeverTranslatesIt() throws Exception {
		Taking<Object> taking = new Taking<>();
		IllegalStateException told = new IllegalStateException("told");

		Flows.mapFailure(Flows.failed(new RuntimeException("raw")), raw -> told).subscribe(taking);

		assertThat(taking.ended.get(5, TimeUnit.SECONDS)).isSameAs(told);
	}

	/**
	 * A transport that can end a stream only normally carries a failure
	 * as one last element: the values before it, then that element,
	 * then the end.
	 */
	@Test
	void aFailureBecomesTheLastElementAndTheStreamEndsNormally() throws Exception {
		SubmissionPublisher<String> source = new SubmissionPublisher<>();
		Taking<String> taking = new Taking<>();
		Flows.endingWith(source, failure -> "failed: " + failure.getMessage()).subscribe(taking);
		taking.subscribed().request(10);

		source.submit("one");
		taking.awaitTaken(1);
		source.closeExceptionally(new IllegalStateException("the sensor went away"));

		assertThat(taking.ended.get(5, TimeUnit.SECONDS)).isNull();
		assertThat(taking.taken).containsExactly("one", "failed: the sensor went away");
	}

	/** The last element waits for a subscriber that has asked for nothing. */
	@Test
	void theLastElementWaitsUntilItIsAskedFor() throws Exception {
		Taking<String> taking = new Taking<>();
		Flows.endingWith(Flows.<String>failed(new IllegalStateException("gone")), failure -> "said: " + failure.getMessage())
				.subscribe(taking);
		Flow.Subscription subscription = taking.subscribed();

		assertThat(taking.ended).isNotDone();
		assertThat(taking.taken).isEmpty();
		subscription.request(1);

		assertThat(taking.ended.get(5, TimeUnit.SECONDS)).isNull();
		assertThat(taking.taken).containsExactly("said: gone");
	}

	@Test
	void aStreamThatIsSimplyOverEndsWithoutTheLastElement() throws Exception {
		SubmissionPublisher<String> source = new SubmissionPublisher<>();
		Taking<String> taking = new Taking<>();
		Flows.endingWith(source, failure -> "never").subscribe(taking);
		taking.subscribed().request(10);

		source.submit("one");
		taking.awaitTaken(1);
		source.close();

		assertThat(taking.ended.get(5, TimeUnit.SECONDS)).isNull();
		assertThat(taking.taken).containsExactly("one");
	}
}
