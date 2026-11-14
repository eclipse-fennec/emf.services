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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.util.pushstream.PushEvent;
import org.osgi.util.pushstream.PushEventConsumer;
import org.osgi.util.pushstream.PushEventSource;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;

/**
 * A {@link PushStream} as a {@link Flow.Publisher}.
 *
 * <p>The messaging library speaks PushStreams; between an invoker and a
 * proxy, and between a service and its dispatcher, a stream travels as
 * the JDK's {@code Flow.Publisher}, which needs no library and which
 * every reactive one adapts to. This is where the two meet.
 *
 * <p>The two disagree about back-pressure. A subscriber of a publisher
 * asks for what it can take; a PushStream pushes, and is slowed by the
 * consumer not returning. So the stream is consumed on its own thread
 * and that thread waits until the subscriber has asked — which is what
 * holds back a messaging subscription that acknowledges a message when
 * it is taken: the broker's window fills, and the sender is slowed by
 * the broker instead of by a buffer here.
 */
public final class PushStreams {

	private PushStreams() {
	}

	/**
	 * The stream as a publisher for <b>one</b> subscriber — a PushStream
	 * is consumed once. Nothing is taken from it before somebody
	 * subscribes and asks.
	 */
	public static <T> Flow.Publisher<T> publisherOf(PushStream<T> stream) {
		AtomicBoolean subscribed = new AtomicBoolean();
		return subscriber -> {
			if (!subscribed.compareAndSet(false, true)) {
				subscriber.onSubscribe(new Flow.Subscription() {
					@Override
					public void request(long n) {
						// refused below; nothing will come
					}

					@Override
					public void cancel() {
						// nothing to stop
					}
				});
				subscriber.onError(new IllegalStateException("this stream has its subscriber already"));
				return;
			}
			Demand demand = new Demand();
			subscriber.onSubscribe(new Flow.Subscription() {
				@Override
				public void request(long n) {
					if (n <= 0) {
						demand.stop();
						stream.close();
						subscriber.onError(new IllegalArgumentException("a subscriber asks for a positive number"));
						return;
					}
					demand.more(n);
				}

				@Override
				public void cancel() {
					demand.stop();
					stream.close();
				}
			});
			stream.forEachEvent(event -> {
				switch (event.getType()) {
				case DATA:
					// Waits here, on the stream's thread, until the
					// subscriber asked: that is the back-pressure.
					if (!demand.take()) {
						return -1;
					}
					subscriber.onNext(event.getData());
					return 0;
				case ERROR:
					if (demand.stop()) {
						subscriber.onError(event.getFailure());
					}
					return -1;
				default:
					if (demand.stop()) {
						subscriber.onComplete();
					}
					return -1;
				}
			});
		};
	}

	/**
	 * A publisher as a stream, for the messaging library that sends a
	 * {@link PushStream}. Unbuffered: the next element is asked for when
	 * the stream's consumer has taken the one before, and a consumer that
	 * asks to wait (a positive back-pressure) is given that time first —
	 * so a sender slowed by its broker slows the publisher down. Closing
	 * the stream cancels the subscription.
	 */
	public static <T> PushStream<T> streamOf(Flow.Publisher<T> publisher) {
		PushEventSource<T> source = consumer -> {
			AtomicReference<Flow.Subscription> held = new AtomicReference<>();
			AtomicBoolean over = new AtomicBoolean();
			publisher.subscribe(new Flow.Subscriber<T>() {
				@Override
				public void onSubscribe(Flow.Subscription subscription) {
					held.set(subscription);
					if (over.get()) {
						subscription.cancel();
					} else {
						subscription.request(1);
					}
				}

				@Override
				public void onNext(T element) {
					long backPressure;
					try {
						backPressure = consumer.accept(PushEvent.data(element));
					} catch (Exception refused) {
						backPressure = -1;
					}
					Flow.Subscription subscription = held.get();
					if (backPressure < 0) {
						over.set(true);
						subscription.cancel();
					} else if (backPressure == 0) {
						subscription.request(1);
					} else {
						CompletableFuture.delayedExecutor(backPressure, TimeUnit.MILLISECONDS)
								.execute(() -> subscription.request(1));
					}
				}

				@Override
				public void onError(Throwable failure) {
					if (over.compareAndSet(false, true)) {
						tell(consumer, PushEvent.error(failure));
					}
				}

				@Override
				public void onComplete() {
					if (over.compareAndSet(false, true)) {
						tell(consumer, PushEvent.close());
					}
				}
			});
			return () -> {
				over.set(true);
				Flow.Subscription subscription = held.get();
				if (subscription != null) {
					subscription.cancel();
				}
			};
		};
		return new PushStreamProvider().buildStream(source).unbuffered().build();
	}

	private static <T> void tell(PushEventConsumer<? super T> consumer, PushEvent<T> event) {
		try {
			consumer.accept(event);
		} catch (Exception ignored) {
			// the stream is over either way
		}
	}

	/**
	 * What a service returned, as a publisher — or {@code null} when it
	 * is not a stream. A {@link Flow.Publisher} is one already; a
	 * {@link PushStream} becomes one ({@link #publisherOf}).
	 */
	@SuppressWarnings("unchecked")
	public static Flow.Publisher<Object> asPublisher(Object returned) {
		if (returned instanceof Flow.Publisher<?> publisher) {
			return (Flow.Publisher<Object>) publisher;
		}
		if (returned instanceof PushStream<?> stream) {
			return publisherOf((PushStream<Object>) stream);
		}
		return null;
	}

	/** What a subscriber has asked for and not been given yet. */
	private static final class Demand {

		private final ReentrantLock lock = new ReentrantLock();

		private final Condition asked = lock.newCondition();

		private long open;

		private boolean stopped;

		void more(long n) {
			lock.lock();
			try {
				open = open + n < 0 ? Long.MAX_VALUE : open + n;
				asked.signalAll();
			} finally {
				lock.unlock();
			}
		}

		/** Waits for one to be asked for; false when the stream is over for this subscriber. */
		boolean take() {
			lock.lock();
			try {
				while (open == 0 && !stopped) {
					asked.awaitUninterruptibly();
				}
				if (stopped) {
					return false;
				}
				open--;
				return true;
			} finally {
				lock.unlock();
			}
		}

		/** Ends it; true the first time, so that an end is told once. */
		boolean stop() {
			lock.lock();
			try {
				boolean first = !stopped;
				stopped = true;
				asked.signalAll();
				return first;
			} finally {
				lock.unlock();
			}
		}
	}
}
