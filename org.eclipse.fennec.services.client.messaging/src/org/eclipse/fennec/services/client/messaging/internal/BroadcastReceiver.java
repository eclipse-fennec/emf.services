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

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;

/**
 * One receiver of a stream every receiver shares (#252): a subscription to
 * the operation's channel through a buffer of its own. A value is handed
 * on only when the subscriber has asked for one; until then delivery
 * waits, the buffer fills, and its policy decides — which is the
 * contract's overflow policy, what this receiver may lose. The source
 * never learns of it.
 *
 * <p>A buffer that fails ends the stream with an error that says so. The
 * end of the broadcast completes it, a value holding a Diagnostic fails
 * it, cancelling closes the subscription.
 */
final class BroadcastReceiver implements Flow.Subscription {

	private static final Logger LOG = Logger.getLogger(BroadcastReceiver.class.getName());

	/** What a broadcast message is to the subscriber. */
	interface Decoder {

		/** The value; throws what a value that holds a Diagnostic is. */
		Object value(CloudEventCodec.Message message) throws Exception;
	}

	/** Opens the subscription that hands messages to {@link #received}. */
	interface Subscribe {

		AutoCloseable open(BroadcastReceiver receiver) throws Exception;
	}

	private final Flow.Subscriber<? super Object> subscriber;

	private final Decoder decoder;

	private final String describe;

	private final Object lock = new Object();

	private final AtomicBoolean running = new AtomicBoolean(true);

	private final AtomicBoolean ended = new AtomicBoolean();

	private long demand;

	private volatile AutoCloseable subscription;

	BroadcastReceiver(Flow.Subscriber<? super Object> subscriber, Decoder decoder, String describe) {
		this.subscriber = subscriber;
		this.decoder = decoder;
		this.describe = describe;
	}

	/** Hands the subscriber this subscription and opens the subscription to the channel. */
	void start(Subscribe subscribe) {
		subscriber.onSubscribe(this);
		try {
			AutoCloseable opened = subscribe.open(this);
			subscription = opened;
			if (!running.get()) {
				closeQuietly(opened);
			}
		} catch (Exception refused) {
			fail(refused instanceof DdsrException failure ? failure
					: DdsrException.transport("cannot receive " + describe + ": " + refused.getMessage(), refused));
		}
	}

	@Override
	public void request(long n) {
		if (n <= 0) {
			fail(new IllegalArgumentException("a subscriber asks for at least one value, not " + n));
			cancel();
			return;
		}
		synchronized (lock) {
			demand = demand + n < 0 ? Long.MAX_VALUE : demand + n;
			lock.notifyAll();
		}
	}

	@Override
	public void cancel() {
		if (running.compareAndSet(true, false)) {
			synchronized (lock) {
				lock.notifyAll();
			}
			closeQuietly(subscription);
		}
	}

	/** One message of the channel, on the subscription's delivery thread; waits for demand. */
	void received(CloudEventCodec.Message message) {
		if (!running.get() || ended.get()) {
			return;
		}
		String type = message.attributes().getType();
		if (CloudEvents.TYPE_BROADCAST_END.equals(type)) {
			if (ended.compareAndSet(false, true)) {
				subscriber.onComplete();
			}
			cancel();
			return;
		}
		if (!CloudEvents.TYPE_BROADCAST.equals(type)) {
			LOG.fine(() -> "[DDSR-MSG] " + describe + " carried a '" + type + "', which is no value of it");
			return;
		}
		Object value;
		try {
			value = decoder.value(message);
		} catch (Exception failed) {
			fail(failed instanceof DdsrException failure ? failure
					: new DdsrException("a value of " + describe + " could not be read: " + failed.getMessage(), failed));
			cancel();
			return;
		}
		if (awaitDemand()) {
			subscriber.onNext(value);
		}
	}

	/** The subscription ended on its own: the buffer filled and its policy fails. */
	void overflowed(Throwable why) {
		fail(DdsrException.transport(describe + " fell behind: its buffer was full and the contract says a"
				+ " receiver that falls behind fails — " + why.getMessage(), why));
		cancel();
	}

	private boolean awaitDemand() {
		synchronized (lock) {
			while (demand == 0 && running.get()) {
				try {
					lock.wait();
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			if (!running.get()) {
				return false;
			}
			demand--;
			return true;
		}
	}

	private void fail(Throwable failure) {
		if (ended.compareAndSet(false, true) && running.get()) {
			subscriber.onError(failure);
		}
	}

	private static void closeQuietly(AutoCloseable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (Exception ignored) {
				// closing is all that was wanted
			}
		}
	}
}
