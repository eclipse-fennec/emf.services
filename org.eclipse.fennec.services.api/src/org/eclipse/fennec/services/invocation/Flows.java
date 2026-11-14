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
	 * The same stream, running {@code done} once when it is over — it
	 * completed, failed, or its subscriber cancelled it: what returns a
	 * service that was held for as long as its stream ran.
	 */
	public static <T> Flow.Publisher<T> whenOver(Flow.Publisher<T> source, Runnable done) {
		AtomicBoolean ran = new AtomicBoolean();
		Runnable once = () -> {
			if (ran.compareAndSet(false, true)) {
				done.run();
			}
		};
		return subscriber -> source.subscribe(new Flow.Subscriber<T>() {
			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscriber.onSubscribe(new Flow.Subscription() {
					@Override
					public void request(long n) {
						subscription.request(n);
					}

					@Override
					public void cancel() {
						try {
							subscription.cancel();
						} finally {
							once.run();
						}
					}
				});
			}

			@Override
			public void onNext(T element) {
				subscriber.onNext(element);
			}

			@Override
			public void onError(Throwable failure) {
				try {
					subscriber.onError(failure);
				} finally {
					once.run();
				}
			}

			@Override
			public void onComplete() {
				try {
					subscriber.onComplete();
				} finally {
					once.run();
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
