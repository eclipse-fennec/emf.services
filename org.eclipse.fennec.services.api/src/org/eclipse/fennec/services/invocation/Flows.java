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

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * The little a stream needs on its way between a transport and a
 * caller. {@link Flow.Publisher} is the type a stream travels as
 * inside this project — the JDK's own, so that nothing here depends on
 * a streaming library — and the JDK ships it without operators.
 */
public final class Flows {

	private Flows() {
	}

	/**
	 * The same stream with every element turned into another. A
	 * function that fails ends the stream with its failure: the
	 * subscription upstream is cancelled and the subscriber is told.
	 */
	public static <T, R> Flow.Publisher<R> map(Flow.Publisher<T> source, Function<? super T, ? extends R> each) {
		return subscriber -> source.subscribe(new Flow.Subscriber<T>() {
			private Flow.Subscription upstream;

			private boolean ended;

			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				this.upstream = subscription;
				subscriber.onSubscribe(subscription);
			}

			@Override
			public void onNext(T element) {
				if (ended) {
					return;
				}
				R mapped;
				try {
					mapped = each.apply(element);
				} catch (RuntimeException failed) {
					ended = true;
					upstream.cancel();
					subscriber.onError(failed);
					return;
				}
				subscriber.onNext(mapped);
			}

			@Override
			public void onError(Throwable failure) {
				if (!ended) {
					ended = true;
					subscriber.onError(failure);
				}
			}

			@Override
			public void onComplete() {
				if (!ended) {
					ended = true;
					subscriber.onComplete();
				}
			}
		});
	}

	/**
	 * The same stream, failing with another failure than it would have:
	 * what turns a transport's reason into the one a caller is told.
	 */
	public static <T> Flow.Publisher<T> mapFailure(Flow.Publisher<T> source,
			Function<? super Throwable, ? extends Throwable> told) {
		return subscriber -> source.subscribe(new Flow.Subscriber<T>() {
			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscriber.onSubscribe(subscription);
			}

			@Override
			public void onNext(T element) {
				subscriber.onNext(element);
			}

			@Override
			public void onError(Throwable failure) {
				Throwable other;
				try {
					other = told.apply(failure);
				} catch (RuntimeException untranslatable) {
					other = failure;
				}
				subscriber.onError(other);
			}

			@Override
			public void onComplete() {
				subscriber.onComplete();
			}
		});
	}

	/**
	 * The same stream, ending with one more element instead of a failure:
	 * a failure becomes the element {@code last} makes of it, handed on
	 * when the subscriber has asked for it, and the stream then completes.
	 * What turns a broken stream into the message that says why, for a
	 * transport that can end a stream only normally.
	 */
	public static <T> Flow.Publisher<T> endingWith(Flow.Publisher<T> source,
			Function<? super Throwable, ? extends T> last) {
		return subscriber -> source.subscribe(new Flow.Subscriber<T>() {
			private final AtomicLong asked = new AtomicLong();

			private final AtomicReference<T> held = new AtomicReference<>();

			private final AtomicBoolean ended = new AtomicBoolean();

			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscriber.onSubscribe(new Flow.Subscription() {
					@Override
					public void request(long n) {
						if (n > 0) {
							asked.getAndAccumulate(n, (had, more) -> had + more < 0 ? Long.MAX_VALUE : had + more);
						}
						if (handOverHeld()) {
							return;
						}
						subscription.request(n);
					}

					@Override
					public void cancel() {
						ended.set(true);
						held.set(null);
						subscription.cancel();
					}
				});
			}

			@Override
			public void onNext(T element) {
				asked.decrementAndGet();
				subscriber.onNext(element);
			}

			@Override
			public void onError(Throwable failure) {
				if (!ended.compareAndSet(false, true)) {
					return;
				}
				T element;
				try {
					element = last.apply(failure);
				} catch (RuntimeException unsayable) {
					subscriber.onError(failure);
					return;
				}
				// Held until asked for; whichever of this and a request sees
				// both hands it on, and only one of them can take it.
				held.set(element);
				handOverHeld();
			}

			private boolean handOverHeld() {
				if (asked.get() <= 0) {
					return false;
				}
				T element = held.getAndSet(null);
				if (element == null) {
					return false;
				}
				subscriber.onNext(element);
				subscriber.onComplete();
				return true;
			}

			@Override
			public void onComplete() {
				if (ended.compareAndSet(false, true)) {
					subscriber.onComplete();
				}
			}
		});
	}

	/** A stream that fails at once: for a call that could not be started. */
	public static <T> Flow.Publisher<T> failed(Throwable failure) {
		return subscriber -> {
			subscriber.onSubscribe(new Flow.Subscription() {
				@Override
				public void request(long n) {
					// nothing will come
				}

				@Override
				public void cancel() {
					// nothing to stop
				}
			});
			subscriber.onError(failure);
		};
	}
}
