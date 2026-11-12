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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.junit.jupiter.api.Test;

import io.cloudevents.model.ce.CloudEvent;

/**
 * Several calls, one subscription: which answer belongs to which call
 * is the envelope's business.
 *
 * <p>The inbox is where a shared reply subtree becomes safe to use.
 * Answers arrive in whatever order the providers finish in, callers
 * come and go, and a message that belongs to nobody has to be dropped
 * rather than handed to the next waiter — which is exactly what these
 * tests pin.
 */
class ReplyInboxTest {

	private static CloudEventCodec.Message answerTo(String requestId, String body) {
		CloudEvent envelope = CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE_REPLY, "/provider/Payment",
				"application/xml");
		envelope.getExtensions().put(CloudEvents.EXTENSION_CORRELATION_ID, requestId);
		return new CloudEventCodec.Message(envelope, body.getBytes(StandardCharsets.UTF_8));
	}

	private static String bodyOf(CloudEventCodec.Message message) {
		return new String(message.data(), StandardCharsets.UTF_8);
	}

	@Test
	void the_filter_covers_this_consumer_and_nothing_wider() {
		assertThat(new ReplyInbox("ddsr/rpc/payments/charge/reply/me", "ddsr/rpc/payments/charge/reply/me/#").filter())
			.isEqualTo("ddsr/rpc/payments/charge/reply/me/#");
	}

	@Test
	void answers_reach_the_call_they_name_whatever_order_they_arrive_in() throws Exception {
		ReplyInbox inbox = new ReplyInbox("reply/me", "reply/me/#");
		inbox.expect("call-1");
		inbox.expect("call-2");

		// The second call is answered first — the usual case when two
		// providers of one contract are not equally fast.
		inbox.deliver(answerTo("call-2", "<two/>"));
		inbox.deliver(answerTo("call-1", "<one/>"));

		assertThat(bodyOf(inbox.await("call-1", 1, TimeUnit.SECONDS))).isEqualTo("<one/>");
		assertThat(bodyOf(inbox.await("call-2", 1, TimeUnit.SECONDS))).isEqualTo("<two/>");
	}

	@Test
	void an_answer_nobody_waits_for_is_dropped_and_not_given_to_the_next_caller() throws Exception {
		ReplyInbox inbox = new ReplyInbox("reply/me", "reply/me/#");
		inbox.expect("call-1");
		inbox.forget("call-1");

		inbox.deliver(answerTo("call-1", "<late/>"));

		inbox.expect("call-2");
		assertThat(inbox.await("call-2", 50, TimeUnit.MILLISECONDS))
			.as("a late answer belongs to the call that is gone, not to the next one")
			.isNull();
	}

	@Test
	void a_message_that_is_not_an_answer_does_not_take_the_subscription_down() throws Exception {
		ReplyInbox inbox = new ReplyInbox("reply/me", "reply/me/#");
		inbox.expect("call-1");

		assertThatCode(() -> {
			// What is no CloudEvent never gets this far: the connection
			// does not deliver it. What does arrive may still be anything.
			inbox.deliver(null);
			inbox.deliver(new CloudEventCodec.Message(CloudEvents.newEnvelope(
					"com.example.something.happened", "/elsewhere", "application/xml"),
					"<x/>".getBytes(StandardCharsets.UTF_8)));
			inbox.deliver(new CloudEventCodec.Message(CloudEvents.newEnvelope(
					CloudEvents.TYPE_INVOKE_REPLY, "/provider/Payment", "application/xml"),
					"<nameless/>".getBytes(StandardCharsets.UTF_8)));
		}).doesNotThrowAnyException();

		inbox.deliver(answerTo("call-1", "<good/>"));
		assertThat(bodyOf(inbox.await("call-1", 1, TimeUnit.SECONDS)))
			.as("the call after the bad messages still gets its answer")
			.isEqualTo("<good/>");
	}

	@Test
	void a_wait_that_runs_out_says_so_rather_than_hanging() throws Exception {
		ReplyInbox inbox = new ReplyInbox("reply/me", "reply/me/#");
		inbox.expect("call-1");

		assertThat(inbox.await("call-1", 30, TimeUnit.MILLISECONDS)).isNull();
	}

	/**
	 * The same inbox for a call that does not wait (osgi.async): the
	 * expectation is a future, and the thread that delivers the answer
	 * completes it.
	 */
	@Test
	void aCallThatDoesNotWaitIsToldWhenItsAnswerArrives() {
		ReplyInbox inbox = new ReplyInbox("reply/me", "reply/me/#");
		CompletableFuture<CloudEventCodec.Message> answer = inbox.expect("call-1");

		assertThat(answer).isNotDone();
		inbox.deliver(answerTo("call-2", "<somebody else's/>"));
		assertThat(answer).as("an answer to another call is not this one's").isNotDone();
		inbox.deliver(answerTo("call-1", "<one/>"));

		assertThat(answer).isCompleted();
		assertThat(bodyOf(answer.join())).isEqualTo("<one/>");
	}

	private static CloudEventCodec.Message lastOf(String requestId, String error) {
		CloudEventCodec.Message last = answerTo(requestId, "");
		last.attributes().getExtensions().put(CloudEvents.EXTENSION_REPLY_COMPLETE, "true");
		if (error != null) {
			last.attributes().getExtensions().put(CloudEvents.EXTENSION_REPLY_ERROR, error);
		}
		return last;
	}

	/** Collects a stream of answers for the test; asks for all of them at once, or when told to. */
	private static final class Collecting implements Flow.Subscriber<CloudEventCodec.Message> {
		final List<String> bodies = new CopyOnWriteArrayList<>();
		final CompletableFuture<Throwable> ended = new CompletableFuture<>();
		final CompletableFuture<Flow.Subscription> subscribed = new CompletableFuture<>();
		private final boolean eager;

		Collecting() {
			this(true);
		}

		Collecting(boolean eager) {
			this.eager = eager;
		}

		@Override
		public void onSubscribe(Flow.Subscription asked) {
			subscribed.complete(asked);
			if (eager) {
				asked.request(Long.MAX_VALUE);
			}
		}

		@Override
		public void onNext(CloudEventCodec.Message answer) {
			bodies.add(bodyOf(answer));
		}

		@Override
		public void onError(Throwable failure) {
			ended.complete(failure);
		}

		@Override
		public void onComplete() {
			ended.complete(null);
		}
	}

	/**
	 * A call answered with a stream (Delivery.STREAM): every answer that
	 * correlates is handed on, and the one marked complete ends it.
	 */
	@Test
	void aStreamOfAnswersIsHandedOnUntilTheOneThatEndsIt() throws Exception {
		ReplyInbox inbox = new ReplyInbox("reply/me", "reply/me/#");
		Collecting collecting = new Collecting();
		inbox.expectMany("call-1", 5_000).subscribe(collecting);

		inbox.deliver(answerTo("call-1", "<one/>"));
		inbox.deliver(answerTo("call-2", "<somebody else's/>"));
		inbox.deliver(answerTo("call-1", "<two/>"));
		inbox.deliver(lastOf("call-1", null));

		assertThat(collecting.ended.get(5, TimeUnit.SECONDS)).isNull();
		assertThat(collecting.bodies).containsExactly("<one/>", "<two/>");
	}

	@Test
	void aStreamTheResponderEndedWithAFailureFailsWithItsReasonAndItsLastMessage() throws Exception {
		ReplyInbox inbox = new ReplyInbox("reply/me", "reply/me/#");
		Collecting collecting = new Collecting();
		inbox.expectMany("call-1", 5_000).subscribe(collecting);

		inbox.deliver(answerTo("call-1", "<one/>"));
		inbox.deliver(lastOf("call-1", "only 3.50 left"));

		Throwable failure = collecting.ended.get(5, TimeUnit.SECONDS);
		assertThat(failure).isInstanceOf(ReplyInbox.StreamFailed.class).hasMessage("only 3.50 left");
		assertThat(((ReplyInbox.StreamFailed) failure).last()).isNotNull();
		assertThat(collecting.bodies).containsExactly("<one/>");
	}

	/**
	 * A subscriber behind on the values gets them before the failure that
	 * ended the stream: the failure must not overtake what is buffered.
	 */
	@Test
	void aFailureDoesNotOvertakeTheValuesASlowSubscriberHasNotTakenYet() throws Exception {
		ReplyInbox inbox = new ReplyInbox("reply/me", "reply/me/#");
		Collecting slow = new Collecting(false);
		inbox.expectMany("call-1", 5_000).subscribe(slow);

		inbox.deliver(answerTo("call-1", "<one/>"));
		inbox.deliver(answerTo("call-1", "<two/>"));
		inbox.deliver(lastOf("call-1", "only 3.50 left"));
		slow.subscribed.get(5, TimeUnit.SECONDS).request(Long.MAX_VALUE);

		assertThat(slow.ended.get(5, TimeUnit.SECONDS)).isInstanceOf(ReplyInbox.StreamFailed.class);
		assertThat(slow.bodies).containsExactly("<one/>", "<two/>");
	}

	/** Nor does the silence: a stream is not quiet while its subscriber has values to take. */
	@Test
	void aSlowSubscriberIsNotToldOfSilenceBeforeItHasTakenWhatArrived() throws Exception {
		ReplyInbox inbox = new ReplyInbox("reply/me", "reply/me/#");
		Collecting slow = new Collecting(false);
		// An idle time a cold JVM does not spend on the first subscribe.
		inbox.expectMany("call-1", 1_000).subscribe(slow);
		inbox.deliver(answerTo("call-1", "<one/>"));
		Flow.Subscription subscription = slow.subscribed.get(5, TimeUnit.SECONDS);

		Thread.sleep(2_500);
		assertThat(slow.ended).isNotDone();
		subscription.request(Long.MAX_VALUE);

		assertThat(slow.ended.get(5, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
		assertThat(slow.bodies).containsExactly("<one/>");
	}

	@Test
	void aStreamThatGoesQuietFails() throws Exception {
		ReplyInbox inbox = new ReplyInbox("reply/me", "reply/me/#");
		Collecting collecting = new Collecting();
		inbox.expectMany("call-1", 150).subscribe(collecting);

		assertThat(collecting.ended.get(5, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
	}
}
