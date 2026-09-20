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

package org.eclipse.fennec.services.client.mqtt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;
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

	private static byte[] answerTo(String requestId, String body) {
		CloudEvent envelope = CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE_REPLY, "/provider/Payment",
				"application/xml");
		envelope.getExtensions().put(CloudEvents.EXTENSION_CORRELATION_ID, requestId);
		return CloudEventCodec.writeStructured(envelope, body.getBytes(StandardCharsets.UTF_8));
	}

	private static String bodyOf(byte[] message) {
		return new String(CloudEventCodec.readStructured(message).data(), StandardCharsets.UTF_8);
	}

	@Test
	void the_filter_covers_this_consumer_and_nothing_wider() {
		assertThat(new ReplyInbox("ddsr/rpc/payments/charge/reply/me").filter())
			.isEqualTo("ddsr/rpc/payments/charge/reply/me/#");
	}

	@Test
	void answers_reach_the_call_they_name_whatever_order_they_arrive_in() throws Exception {
		ReplyInbox inbox = new ReplyInbox("reply/me");
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
		ReplyInbox inbox = new ReplyInbox("reply/me");
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
		ReplyInbox inbox = new ReplyInbox("reply/me");
		inbox.expect("call-1");

		assertThatCode(() -> {
			inbox.deliver("not even json".getBytes(StandardCharsets.UTF_8));
			inbox.deliver(CloudEventCodec.writeStructured(CloudEvents.newEnvelope(
					"com.example.something.happened", "/elsewhere", "application/xml"),
					"<x/>".getBytes(StandardCharsets.UTF_8)));
			inbox.deliver(CloudEventCodec.writeStructured(CloudEvents.newEnvelope(
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
		ReplyInbox inbox = new ReplyInbox("reply/me");
		inbox.expect("call-1");

		assertThat(inbox.await("call-1", 30, TimeUnit.MILLISECONDS)).isNull();
	}
}
