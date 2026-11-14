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

package org.eclipse.fennec.services.provider.rest;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.flavor.rest.RestStreams;

import jakarta.ws.rs.core.StreamingOutput;

/**
 * A stream written as the body of one answer, in Server-Sent Events
 * ({@link RestStreams}): a value is asked for, written and flushed, and
 * only then is the next asked for — the connection sets the pace, so a
 * caller that reads slowly slows the stream down instead of piling it up
 * here. A caller that goes away shows as a failed write, and the stream is
 * cancelled, and with it the service's.
 *
 * <p>Blocks the thread that writes the answer for as long as the stream
 * lasts, which is what an HTTP answer is.
 */
final class SseStream implements StreamingOutput {

	private static final Logger LOG = Logger.getLogger(SseStream.class.getName());

	/** How a value or a failure becomes the data of one event: a CloudEvents message ({@link RestStreams}). */
	interface Encoder {

		String value(Object value) throws Exception;

		String failure(Throwable failure) throws Exception;
	}

	private static final Object END = new Object();

	private final Flow.Publisher<Object> values;

	private final Encoder encoder;

	private final Runnable done;

	/**
	 * @param done run once the stream is over, however it ended — the
	 *        service is held for the stream's lifetime and returned here
	 */
	SseStream(Flow.Publisher<Object> values, Encoder encoder, Runnable done) {
		this.values = values;
		this.encoder = encoder;
		this.done = done;
	}

	@Override
	public void write(OutputStream out) throws IOException {
		LinkedBlockingQueue<Object> signals = new LinkedBlockingQueue<>();
		AtomicReference<Flow.Subscription> subscription = new AtomicReference<>();
		try {
			values.subscribe(new Flow.Subscriber<>() {

				@Override
				public void onSubscribe(Flow.Subscription given) {
					subscription.set(given);
					given.request(1);
				}

				@Override
				public void onNext(Object value) {
					signals.add(new Value(value));
				}

				@Override
				public void onError(Throwable failure) {
					signals.add(new Failure(failure));
				}

				@Override
				public void onComplete() {
					signals.add(END);
				}
			});
			while (true) {
				Object signal = signals.take();
				if (signal instanceof Value value) {
					send(out, RestStreams.VALUE, encoder.value(value.value()));
					subscription.get().request(1);
				} else if (signal instanceof Failure failure) {
					LOG.log(Level.FINE, "[DDSR-REST] a stream ended with a failure", failure.failure());
					send(out, RestStreams.ERROR, encoder.failure(failure.failure()));
					return;
				} else {
					send(out, RestStreams.END, null);
					return;
				}
			}
		} catch (IOException gone) {
			// The caller closed the connection: the one way it has to say
			// it wants no more.
			cancel(subscription);
			LOG.fine(() -> "[DDSR-REST] the caller of a stream went away: " + gone.getMessage());
		} catch (InterruptedException interrupted) {
			cancel(subscription);
			Thread.currentThread().interrupt();
		} catch (Exception unwritable) {
			cancel(subscription);
			LOG.log(Level.WARNING, "[DDSR-REST] a value of a stream could not be written", unwritable);
			try {
				send(out, RestStreams.ERROR, encoder.failure(unwritable));
			} catch (Exception unsayable) {
				// the connection is the only one there is to tell
			}
		} finally {
			done.run();
		}
	}

	private void send(OutputStream out, String event, String data) throws IOException {
		out.write(RestStreams.frame(event, data).getBytes(UTF_8));
		out.flush();
	}

	private static void cancel(AtomicReference<Flow.Subscription> subscription) {
		Flow.Subscription given = subscription.get();
		if (given != null) {
			given.cancel();
		}
	}

	private record Value(Object value) {
	}

	private record Failure(Throwable failure) {
	}
}
