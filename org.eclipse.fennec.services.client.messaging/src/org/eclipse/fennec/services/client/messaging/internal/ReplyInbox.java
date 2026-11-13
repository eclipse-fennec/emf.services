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
