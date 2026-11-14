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

package org.eclipse.fennec.services.client.rest.internal;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.flavor.rest.RestStreams;

/**
 * A stream answered over REST, read as it arrives (#252): the request is
 * sent when a subscriber is there, every {@code value} event becomes the
 * next value, {@code error} fails the stream with what its document says,
 * {@code end} completes it ({@link RestStreams}).
 *
 * <p>An event is read only when the subscriber has asked for one, so a
 * slow subscriber leaves the rest in the connection — TCP tells the
 * provider to wait. Cancelling closes the connection, which is how the
 * provider learns the caller wants no more.
 *
 * <p>One thread per subscription, reading; an HTTP response is read by
 * whoever holds it.
 */
final class SseValues implements Flow.Publisher<Object> {

	private static final Logger LOG = Logger.getLogger(SseValues.class.getName());

	/**
	 * The stream as it arrives: the body to read, and what closes the
	 * connection.
	 */
	record Opened(InputStream body, AutoCloseable connection) {
	}

	/** Sends the call; throws what the caller is to see when the answer is no stream. */
	interface Request {

		Opened open() throws Exception;
	}

	/** What the data of an event — a CloudEvents message ({@link RestStreams}) — is to the caller. */
	interface Decoder {

		Object value(String message) throws Exception;

		RuntimeException failure(String message) throws Exception;
	}

	private final String operationName;

	private final Request request;

	private final Decoder decoder;

	/**
	 * @param request sends the call; asked once per subscriber
	 */
	SseValues(String operationName, Request request, Decoder decoder) {
		this.operationName = operationName;
		this.request = request;
		this.decoder = decoder;
	}

	@Override
	public void subscribe(Flow.Subscriber<? super Object> subscriber) {
		Reader reader = new Reader(subscriber);
		subscriber.onSubscribe(reader);
		Thread thread = new Thread(reader, "ddsr-rest-stream-" + operationName);
		thread.setDaemon(true);
		thread.start();
	}

	private final class Reader implements Flow.Subscription, Runnable {

		private final Flow.Subscriber<? super Object> subscriber;

		private final AtomicBoolean running = new AtomicBoolean(true);

		private final AtomicBoolean ended = new AtomicBoolean();

		private final Object lock = new Object();

		private long demand;

		private volatile Opened opened;

		Reader(Flow.Subscriber<? super Object> subscriber) {
			this.subscriber = subscriber;
		}

		@Override
		public void request(long n) {
			if (n <= 0) {
				cancel();
				fail(new IllegalArgumentException("a subscriber asks for at least one value, not " + n));
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
				close();
			}
		}

		@Override
		public void run() {
			try {
				opened = request.open();
				if (!running.get()) {
					return;
				}
				BufferedReader lines = new BufferedReader(new InputStreamReader(opened.body(), UTF_8));
				SseFrames.pumpEvents(lines, () -> running.get() && !ended.get(), this::onEvent);
				if (running.get() && !ended.get()) {
					fail(new DdsrException("the stream of " + operationName + " broke off: the provider closed it"
							+ " without saying it was over"));
				}
			} catch (Exception broken) {
				if (running.get() && !ended.get()) {
					fail(broken instanceof DdsrException failure ? failure
							: DdsrException.transport("the stream of " + operationName + " broke off: "
									+ broken.getMessage(), broken));
				}
			} finally {
				close();
			}
		}

		private void onEvent(String event, String data) {
			try {
				switch (event) {
				case RestStreams.VALUE -> {
					if (awaitDemand()) {
						subscriber.onNext(decoder.value(data));
					}
				}
				case RestStreams.ERROR -> fail(decoder.failure(data));
				case RestStreams.END -> {
					if (ended.compareAndSet(false, true) && running.get()) {
						// The connection first: a subscriber told the stream
						// is over finds nothing of it still open.
						close();
						subscriber.onComplete();
					}
				}
				default -> LOG.fine(() -> "[DDSR-Client] the stream of " + operationName
						+ " carried an event '" + event + "' that means nothing here");
				}
			} catch (RuntimeException failure) {
				fail(failure);
			} catch (Exception unreadable) {
				fail(new DdsrException("a value of the stream of " + operationName + " could not be read: "
						+ unreadable.getMessage(), unreadable));
			}
		}

		/** Waits until a value is asked for; false when the subscriber wants none any more. */
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

		private void close() {
			Opened open = opened;
			if (open != null) {
				try {
					open.body().close();
				} catch (IOException ignored) {
					// closing is all that was wanted
				}
				try {
					open.connection().close();
				} catch (Exception ignored) {
					// closing is all that was wanted
				}
			}
		}
	}
}
