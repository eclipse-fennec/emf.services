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


package org.eclipse.fennec.tags.ddsr;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import org.eclipse.fennec.tags.TagChange;

/**
 * The changes of the tag registry as a stream every receiver shares — the
 * source behind {@code Tags.changes()}, served by the generic distribution
 * as it serves any broadcast (#278).
 *
 * <p>One run per subscriber the distribution makes: a change goes out as
 * soon as it is asked for, the others wait; the distribution asks for the
 * next as soon as it has handed one on, so the queue stays short. Whoever
 * falls behind is the distribution's to handle, by the overflow policy
 * the contract states. The same shape as the broker's events.
 */
final class ChangesBroadcast implements Consumer<TagChange> {

	private final Set<Run> runs = ConcurrentHashMap.newKeySet();

	Flow.Publisher<TagChange> publisher() {
		return subscriber -> {
			Run run = new Run(subscriber);
			runs.add(run);
			subscriber.onSubscribe(run);
		};
	}

	@Override
	public void accept(TagChange change) {
		for (Run run : runs) {
			run.offer(change);
		}
	}

	/** Every receiver is told the stream is over. */
	void close() {
		for (Run run : List.copyOf(runs)) {
			run.complete();
		}
	}

	private final class Run implements Flow.Subscription {

		private final Flow.Subscriber<? super TagChange> subscriber;

		private final ArrayDeque<TagChange> waiting = new ArrayDeque<>();

		private long demand;

		private boolean over;

		Run(Flow.Subscriber<? super TagChange> subscriber) {
			this.subscriber = subscriber;
		}

		void offer(TagChange change) {
			synchronized (this) {
				if (over) {
					return;
				}
				waiting.add(change);
			}
			drain();
		}

		void complete() {
			synchronized (this) {
				if (over) {
					return;
				}
				over = true;
			}
			runs.remove(this);
			subscriber.onComplete();
		}

		@Override
		public void request(long n) {
			synchronized (this) {
				demand = demand + n < 0 ? Long.MAX_VALUE : demand + n;
			}
			drain();
		}

		@Override
		public void cancel() {
			synchronized (this) {
				over = true;
				waiting.clear();
			}
			runs.remove(this);
		}

		private void drain() {
			while (true) {
				TagChange next;
				synchronized (this) {
					if (over || demand == 0 || waiting.isEmpty()) {
						return;
					}
					demand--;
					next = waiting.poll();
				}
				subscriber.onNext(next);
			}
		}
	}
}
