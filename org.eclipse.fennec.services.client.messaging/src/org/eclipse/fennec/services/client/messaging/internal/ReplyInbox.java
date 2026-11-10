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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

	/** One waiting call. A queue of one, because there is one answer. */
	private final Map<String, BlockingQueue<CloudEventCodec.Message>> waiting = new ConcurrentHashMap<>();

	private final String subtree;

	ReplyInbox(String subtree) {
		this.subtree = subtree;
	}

	/** The filter that covers every answer to this consumer. */
	String filter() {
		return subtree + "/#";
	}

	/** Expect the answer to this call. */
	void expect(String requestId) {
		waiting.put(requestId, new ArrayBlockingQueue<>(1));
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
		BlockingQueue<CloudEventCodec.Message> queue = waiting.get(requestId);
		if (queue == null) {
			throw new IllegalStateException("no call is waiting for " + requestId);
		}
		return queue.poll(timeout, unit);
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
		BlockingQueue<CloudEventCodec.Message> queue = waiting.get(correlation);
		if (queue == null) {
			// The caller is gone: it timed out, or was interrupted. There
			// is nobody to hand this to and nothing to keep it for.
			LOG.fine(() -> "[DDSR-MQTT] nobody is waiting for " + correlation + " any more");
			return;
		}
		queue.offer(message);
	}
}
