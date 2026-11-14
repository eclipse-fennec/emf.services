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
import java.util.ArrayDeque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.OverflowPolicy;
import org.eclipse.fennec.services.flavor.rest.RestStreams;

import jakarta.ws.rs.core.StreamingOutput;

/**
 * A stream every receiver shares, over REST (#252): the provider is the
 * distribution point. The service is called once, when the first receiver
 * comes, and every value goes to every open connection — at the source's
 * pace. Each connection has a buffer of its own, and the contract's
 * overflow policy says what happens when it is full: {@code DISCARD_OLDEST}
 * drops the oldest value of that connection, {@code FAIL} ends that
 * connection with an {@code error} event, {@code BLOCK} holds the source
 * until there is room — over REST the provider is the receiver's buffer,
 * so waiting there is waiting at the source. When the last receiver goes,
 * the service's stream is cancelled: over REST nobody listens to a
 * broadcast without a connection.
 */
final class RestBroadcast {

	private static final Logger LOG = Logger.getLogger(RestBroadcast.class.getName());

	/** How many values a connection holds that it has not written yet. */
	static final int BUFFER = 256;

	/**
	 * How long a connection may stay silent before it gets an SSE comment:
	 * a proxy keeps it open, a receiver that went away without a word is
	 * noticed by the failed write instead of at the next value, and a
	 * reader with a read timeout — the Java client's is 10 s — never waits
	 * that long for a byte.
	 */
	static final long KEEPALIVE_MILLIS = 5_000;

	private final Supplier<Flow.Publisher<Object>> source;

	private final SseStream.Encoder encoder;

	private final OverflowPolicy policy;

	private final int size;

	private final String describe;

	private final Set<Receiver> receivers = ConcurrentHashMap.newKeySet();

	private Flow.Subscription running;

	/** A run is on, or being started: the next receiver joins it. */
	private boolean active;

	/**
	 * @param source calls the service, once per run of the broadcast
	 */
	RestBroadcast(Supplier<Flow.Publisher<Object>> source, SseStream.Encoder encoder, OverflowPolicy policy, int size,
			String describe) {
		this.source = source;
		this.encoder = encoder;
		this.policy = policy == null ? OverflowPolicy.FAIL : policy;
		this.size = size > 0 ? size : BUFFER;
		this.describe = describe;
	}

	/** One more receiver: the body of its answer. */
	StreamingOutput receiver() {
		return receiver(() -> {
		}, () -> {
		});
	}

	/**
	 * One more receiver, and who to tell when it comes and when it goes —
	 * the service, where it wants to know ({@code BroadcastReceivers}).
	 */
	StreamingOutput receiver(Runnable joined, Runnable left) {
		return out -> {
			Receiver receiver = new Receiver();
			join(receiver);
			joined.run();
			try {
				receiver.write(out);
			} finally {
				leave(receiver);
				left.run();
			}
		};
	}

	private synchronized void join(Receiver receiver) {
		receivers.add(receiver);
		if (!active) {
			active = true;
			start();
		}
	}

	private synchronized void leave(Receiver receiver) {
		receivers.remove(receiver);
		if (receivers.isEmpty() && active) {
			active = false;
			if (running != null) {
				running.cancel();
			}
			running = null;
			LOG.fine(() -> "[DDSR-REST] " + describe + " has no receiver left, its stream is cancelled");
		}
	}

	private void start() {
		Flow.Subscription[] mine = new Flow.Subscription[1];
		source.get().subscribe(new Flow.Subscriber<>() {

			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				mine[0] = subscription;
				synchronized (RestBroadcast.this) {
					if (!active) {
						subscription.cancel();
						return;
					}
					running = subscription;
				}
				subscription.request(1);
			}

			@Override
			public void onNext(Object value) {
				String message;
				try {
					message = encoder.value(value);
				} catch (Exception unwritable) {
					LOG.log(Level.WARNING, "[DDSR-REST] a value of " + describe + " could not be written", unwritable);
					mine[0].request(1);
					return;
				}
				String frame = RestStreams.frame(RestStreams.VALUE, message);
				for (Receiver receiver : receivers) {
					receiver.offer(frame);
				}
				mine[0].request(1);
			}

			@Override
			public void onError(Throwable failure) {
				String frame;
				try {
					frame = RestStreams.frame(RestStreams.ERROR, encoder.failure(failure));
				} catch (Exception unsayable) {
					frame = RestStreams.frame(RestStreams.ERROR, null);
				}
				over(frame, mine[0]);
			}

			@Override
			public void onComplete() {
				over(RestStreams.frame(RestStreams.END, null), mine[0]);
			}
		});
	}

	/** The run is over: every receiver gets the last event, and the next receiver starts a new run. */
	private void over(String frame, Flow.Subscription ended) {
		synchronized (this) {
			if (running == ended || running == null) {
				running = null;
				active = false;
			}
		}
		for (Receiver receiver : receivers) {
			receiver.last(frame);
		}
	}

	/**
	 * One connection: its buffer, written as fast as it takes it.
	 *
	 * <p>Waits on a lock and its condition, never on a monitor: the HTTP
	 * server may serve a connection on a virtual thread, and on Java 21 one
	 * that waits inside {@code synchronized} holds its carrier thread — a
	 * dozen quiet receivers would take every carrier, and the next request
	 * would hang (found by the harness).
	 */
	private final class Receiver {

		private final ReentrantLock lock = new ReentrantLock();

		private final Condition changed = lock.newCondition();

		private final ArrayDeque<String> buffer = new ArrayDeque<>();

		private String last;

		private boolean gone;

		void offer(String frame) {
			lock.lock();
			try {
				offerLocked(frame);
			} finally {
				lock.unlock();
			}
		}

		private void offerLocked(String frame) {
			if (gone || last != null) {
				return;
			}
			while (buffer.size() >= size) {
				switch (policy) {
				case DISCARD_OLDEST -> buffer.pollFirst();
				case BLOCK -> {
					try {
						changed.await();
					} catch (InterruptedException interrupted) {
						Thread.currentThread().interrupt();
						return;
					}
					if (gone) {
						return;
					}
				}
				default -> {
					// FAIL: what is buffered is still written, then the
					// connection learns that it fell behind.
					String why = describe + " fell behind: its buffer of " + size
							+ " values was full and the contract says a receiver that falls behind fails";
					try {
						last = RestStreams.frame(RestStreams.ERROR,
								encoder.failure(new IllegalStateException(why)));
					} catch (Exception unsayable) {
						last = RestStreams.frame(RestStreams.ERROR, null);
					}
					changed.signalAll();
					return;
				}
				}
			}
			buffer.addLast(frame);
			changed.signalAll();
		}

		void last(String frame) {
			lock.lock();
			try {
				if (last == null) {
					last = frame;
				}
				changed.signalAll();
			} finally {
				lock.unlock();
			}
		}

		void write(OutputStream out) throws IOException {
			try {
				// At once, before any value: the answer's headers go out with
				// it, so the receiver knows the stream stands — the moment it
				// takes its snapshot — instead of when the first value comes.
				out.write(": connected\n\n".getBytes(UTF_8));
				out.flush();
				while (true) {
					String frame;
					boolean closing;
					lock.lock();
					try {
						long remaining = TimeUnit.MILLISECONDS.toNanos(KEEPALIVE_MILLIS);
						while (buffer.isEmpty() && last == null && remaining > 0) {
							remaining = changed.awaitNanos(remaining);
						}
						if (buffer.isEmpty() && last == null) {
							closing = false;
							frame = ": keepalive\n\n";
						} else {
							closing = buffer.isEmpty();
							frame = closing ? last : buffer.pollFirst();
						}
						changed.signalAll();
					} finally {
						lock.unlock();
					}
					out.write(frame.getBytes(UTF_8));
					out.flush();
					if (closing) {
						return;
					}
				}
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			} catch (IOException gone) {
				LOG.fine(() -> "[DDSR-REST] a receiver of " + describe + " went away: " + gone.getMessage());
			} finally {
				lock.lock();
				try {
					this.gone = true;
					changed.signalAll();
				} finally {
					lock.unlock();
				}
			}
		}
	}
}
