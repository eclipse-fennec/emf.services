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

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.flavor.messaging.MessagingRoute;

import io.cloudevents.model.ce.CloudEvent;

/**
 * One consumer's answers, on one subtree.
 *
 * <p>Everything this runtime will be sent arrives under
 * {@code <reply base>/<this consumer>/…}, so the subscription is taken
 * once and every call waits in the same inbox. Which answer belongs to
 * which call is the envelope's business — {@code correlationid} — and
 * not the topic's, which is what lets several calls be in flight at
 * once without a subscription each.
 *
 * <p>That the subtree carries the consumer's name is the point. A
 * reply topic separated only by an unguessable id keeps peers apart by
 * obscurity; a named subtree can be written into a broker ACL, and
 * then a consumer is not merely unlikely to see another's answers but
 * unable to.
 *
 * <p>An answer nobody is waiting for is dropped: a call that already
 * timed out, or one whose caller gave up. Keeping it would be a leak
 * with no reader.
 */
final class ReplyInbox {

	private static final Logger LOG = Logger.getLogger(ReplyInbox.class.getName());

	/**
	 * One waiting call: a future, because there is one answer and a
	 * caller either waits for it or is told — the same inbox serves the
	 * call that blocks and the one that answers later.
	 */
	private final Map<String, CompletableFuture<CloudEventCodec.Message>> waiting = new ConcurrentHashMap<>();

	private final String subtree;

	private final String filter;

	/**
	 * @param subtree where this consumer's answers arrive
	 * @param filter  what covers that subtree, in the spelling of the
	 *                flavor's protocol
	 */
	ReplyInbox(String subtree, String filter) {
		this.subtree = subtree;
		this.filter = filter;
	}

	/** The filter that covers every answer to this consumer. */
	String filter() {
		return filter;
	}

	/**
	 * Expect the answer to this call.
	 *
	 * @return completes with the answer when it arrives — for a caller
	 *         that would rather be told than wait
	 */
	CompletableFuture<CloudEventCodec.Message> expect(String requestId) {
		CompletableFuture<CloudEventCodec.Message> answer = new CompletableFuture<>();
		waiting.put(requestId, answer);
		return answer;
	}

	/** Stop expecting it — answered, timed out or given up on. */
	void forget(String requestId) {
		waiting.remove(requestId);
	}

	/** The calls that are answered with a stream, by the id of their request. */
	private final Map<String, Answers> streams = new ConcurrentHashMap<>();

	/**
	 * Expect a stream of answers to this call ({@code Delivery.STREAM}):
	 * any number of them and the one that ends it.
	 *
	 * <p>The answers are handed on as the subscriber asks. One that is
	 * slower than they arrive fills a small buffer, and then the thread
	 * that delivers this inbox waits — which holds back every call on
	 * this subtree, and through the broker's window the responder. A
	 * stream that brings nothing for {@code idleMillis} fails with a
	 * {@link TimeoutException}.
	 */
	Flow.Publisher<CloudEventCodec.Message> expectMany(String requestId, long idleMillis) {
		Answers answers = new Answers(requestId, idleMillis);
		streams.put(requestId, answers);
		answers.watch();
		return answers.publisher;
	}

	/** End a stream of answers from this side — the request could not be sent. */
	void fail(String requestId, Throwable failure) {
		Answers answers = streams.remove(requestId);
		if (answers != null) {
			answers.publisher.closeExceptionally(failure);
		}
	}

	/** The stream ended with a failure the responder reported; carries its last message. */
	static final class StreamFailed extends RuntimeException {

		private static final long serialVersionUID = 1L;

		private final transient CloudEventCodec.Message last;

		StreamFailed(String reason, CloudEventCodec.Message last) {
			super(reason);
			this.last = last;
		}

		/** The message that ended the stream — it may carry the Diagnostic. */
		CloudEventCodec.Message last() {
			return last;
		}
	}

	/** One stream of answers on its way to its subscriber. */
	private final class Answers {

		private static final int BUFFER = 64;

		private final String requestId;

		private final long idleMillis;

		private final SubmissionPublisher<CloudEventCodec.Message> publisher =
				new SubmissionPublisher<>(ForkJoinPool.commonPool(), BUFFER);

		private volatile long lastSeen = System.nanoTime();

		Answers(String requestId, long idleMillis) {
			this.requestId = requestId;
			this.idleMillis = idleMillis;
		}

		void take(CloudEventCodec.Message message) {
			lastSeen = System.nanoTime();
			Map<String, String> extensions = message.attributes().getExtensions().map();
			if (Boolean.parseBoolean(extensions.get(CloudEvents.EXTENSION_REPLY_COMPLETE))) {
				streams.remove(requestId, this);
				String reason = extensions.get(CloudEvents.EXTENSION_REPLY_ERROR);
				if (reason != null) {
					publisher.closeExceptionally(new StreamFailed(reason, message));
				} else {
					publisher.close();
				}
				return;
			}
			// Blocks while the subscriber is behind by a buffer: the
			// back-pressure of this stream.
			publisher.submit(message);
		}

		/** Ends a stream that has gone quiet, and looks again while it has not. */
		void watch() {
			long quietFor = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastSeen);
			if (streams.get(requestId) != this) {
				return;
			}
			if (quietFor >= idleMillis) {
				streams.remove(requestId, this);
				publisher.closeExceptionally(new TimeoutException("nothing arrived on " + subtree + " for "
						+ idleMillis + " ms"));
				return;
			}
			CompletableFuture.delayedExecutor(idleMillis - quietFor + 1, TimeUnit.MILLISECONDS)
					.execute(this::watch);
		}
	}

	/**
	 * Wait for the answer to one call.
	 *
	 * @return the message, or {@code null} when the wait ran out
	 */
	CloudEventCodec.Message await(String requestId, long timeout, TimeUnit unit) throws InterruptedException {
		CompletableFuture<CloudEventCodec.Message> answer = waiting.get(requestId);
		if (answer == null) {
			throw new IllegalStateException("no call is waiting for " + requestId);
		}
		try {
			return answer.get(timeout, unit);
		} catch (TimeoutException silence) {
			return null;
		} catch (ExecutionException | CancellationException given) {
			// Nothing completes an expectation with a failure but its
			// own caller giving up; either way there is no answer.
			return null;
		}
	}

	/**
	 * One message off the subtree, as the connection read it. Never
	 * throws: this runs on a thread of the connection, and a message
	 * that is not an answer must not take the subscription every other
	 * call shares down with it.
	 */
	void deliver(CloudEventCodec.Message message) {
		if (message == null) {
			return;
		}
		CloudEvent envelope = message.attributes();
		if (!CloudEvents.TYPE_INVOKE_REPLY.equals(envelope.getType())) {
			LOG.fine(() -> "[DDSR-MQTT] ignoring a '" + envelope.getType() + "' message on "
					+ subtree + " — this subtree is for answers");
			return;
		}
		String correlation = MessagingRoute.correlationOf(envelope);
		if (correlation == null) {
			LOG.warning("[DDSR-MQTT] an answer on " + subtree + " named no call, dropped");
			return;
		}
		Answers stream = streams.get(correlation);
		if (stream != null) {
			stream.take(message);
			return;
		}
		CompletableFuture<CloudEventCodec.Message> answer = waiting.get(correlation);
		if (answer == null) {
			// The caller is gone: it timed out, or was interrupted. There
			// is nobody to hand this to and nothing to keep it for.
			LOG.fine(() -> "[DDSR-MQTT] nobody is waiting for " + correlation + " any more");
			return;
		}
		answer.complete(message);
	}
}
