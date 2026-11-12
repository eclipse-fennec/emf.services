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

package org.eclipse.fennec.services.client.internal;

import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import org.osgi.util.pushstream.PushEvent;
import org.osgi.util.pushstream.PushEventConsumer;
import org.osgi.util.pushstream.PushEventSource;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;

/**
 * A {@link Flow.Publisher} as the {@link PushStream} a caller's
 * interface declares.
 *
 * <p>Kept in a class of its own because it is the one place in this
 * bundle that touches the pushstream package, which is imported
 * optionally: it is loaded when a proxy is asked for a PushStream, and
 * a caller that asks for one has the package.
 *
 * <p>The two agree on back-pressure here without a buffer in between.
 * A value is asked of the publisher one at a time, and the next one
 * only when the stream's consumer has returned from the one before —
 * after the pause it asked for, where it asked for one.
 */
final class PushStreamOf {

	private PushStreamOf() {
	}

	/** The publisher as a stream. Nothing is asked of it before the stream is consumed. */
	static <T> PushStream<T> of(Flow.Publisher<T> publisher) {
		PushEventSource<T> source = consumer -> {
			Feeding<T> feeding = new Feeding<>(consumer);
			publisher.subscribe(feeding);
			return feeding;
		};
		return new PushStreamProvider().buildStream(source).unbuffered().build();
	}

	/** Feeds one consumer of the stream from one subscription. */
	private static final class Feeding<T> implements Flow.Subscriber<T>, AutoCloseable {

		private final PushEventConsumer<? super T> consumer;

		private volatile Flow.Subscription subscription;

		private volatile boolean ended;

		Feeding(PushEventConsumer<? super T> consumer) {
			this.consumer = consumer;
		}

		@Override
		public void onSubscribe(Flow.Subscription asked) {
			this.subscription = asked;
			if (ended) {
				asked.cancel();
			} else {
				asked.request(1);
			}
		}

		@Override
		public void onNext(T value) {
			if (ended) {
				return;
			}
			try {
				long pause = consumer.accept(PushEvent.data(value));
				if (pause < 0) {
					close();
					return;
				}
				if (pause > 0) {
					TimeUnit.MILLISECONDS.sleep(pause);
				}
				subscription.request(1);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				close();
			} catch (Exception refused) {
				close();
				end(PushEvent.error(refused));
			}
		}

		@Override
		public void onError(Throwable failure) {
			ended = true;
			end(PushEvent.error(failure));
		}

		@Override
		public void onComplete() {
			ended = true;
			end(PushEvent.close());
		}

		private void end(PushEvent<T> last) {
			try {
				consumer.accept(last);
			} catch (Exception ignored) {
				// The stream is over either way.
			}
		}

		/** The stream was closed from its end: nothing more is asked for. */
		@Override
		public void close() {
			ended = true;
			Flow.Subscription asked = subscription;
			if (asked != null) {
				asked.cancel();
			}
		}
	}
}
