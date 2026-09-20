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

package org.eclipse.fennec.services.broker.core.internal;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.broker.core.EventSink;

/**
 * The hand-off between the broker and whoever is listening (#124).
 *
 * <p>The broker used to call the sinks itself, while holding its write
 * lock, and the reason was ordering: per-service order had to match the
 * order the mutations were applied in. The price was that foreign code —
 * an SSE send into a stalled TCP connection, an MQTT publish waiting at
 * the in-flight limit — ran inside the lock that every publish, every
 * withdraw and every lookup needs. One slow subscriber was enough to
 * stop the registry.
 *
 * <p>A queue buys the same ordering for less. The position is decided by
 * {@link #submit}, which the broker calls while it still holds the write
 * lock and which never blocks; exactly one thread drains the queue, so
 * what was enqueued in mutation order is delivered in mutation order.
 * What the broker no longer does is wait.
 *
 * <p><b>The queue is bounded, and that is a decision, not an
 * oversight.</b> An unbounded queue in front of a subscriber that has
 * stopped reading does not remove the failure, it converts it into heap
 * exhaustion and takes the broker down later and less visibly. When the
 * queue is full the event is dropped and counted. A consumer that
 * missed an event is not lost: it re-reads on its next reconnect
 * (FR-Sync-Reconnect), which is the same recovery a dropped MQTT
 * publish needs, and the drop is logged at WARNING so the deployment
 * can see that it happened at all.
 *
 * <p>The bound is against a subscriber that stopped reading, not
 * against a fast producer. A caller that submits faster than the sink
 * consumes, for longer than {@link #CAPACITY} events, will lose some —
 * and in the broker that cannot be reached by submitting alone,
 * because every emit is separated by a mutation and a snapshot write,
 * which is orders of magnitude slower than taking an event off a
 * queue. Whatever happens, an event is either delivered or counted:
 * none goes missing quietly.
 */
final class EventDelivery implements AutoCloseable {

	private static final Logger LOG = Logger.getLogger(EventDelivery.class.getName());

	/**
	 * Room for a burst, not for an outage. A registry emits a handful of
	 * events per lifecycle change; a thousand covers a mass re-publish
	 * after a restart while still being small enough that a subscriber
	 * which stopped reading is noticed in seconds rather than in an
	 * out-of-memory hours later.
	 */
	static final int CAPACITY = 1000;

	/** Ends the dispatcher without being delivered; never null-checked by accident. */
	private static final ServiceEvent POISON = org.eclipse.fennec.services.ServicesFactory.eINSTANCE
			.createServiceEvent();

	private final EventSink sink;

	private final BlockingQueue<ServiceEvent> queue = new ArrayBlockingQueue<>(CAPACITY);

	private final AtomicLong submitted = new AtomicLong();

	private final AtomicLong delivered = new AtomicLong();

	private final AtomicLong dropped = new AtomicLong();

	private volatile Thread dispatcher;

	private volatile boolean closed;

	EventDelivery(EventSink sink) {
		this.sink = sink != null ? sink : EventSink.NOOP;
	}

	/**
	 * Takes an event for delivery and returns at once.
	 *
	 * <p>Called under the broker's write lock, which is what fixes the
	 * order, and which is also why it must not block or do I/O.
	 */
	void submit(ServiceEvent event) {
		if (event == null || closed) {
			return;
		}
		start();
		submitted.incrementAndGet();
		if (!queue.offer(event)) {
			long total = dropped.incrementAndGet();
			LOG.warning("[DDSR] event queue full (" + CAPACITY + ") — dropped "
					+ event.getType().getLiteral() + ", " + total + " dropped so far."
					+ " A subscriber is not reading; consumers recover on their next reconnect.");
		}
	}

	/**
	 * Blocks until everything submitted so far has been handed to the
	 * sink.
	 *
	 * <p>For tests, and for a clean shutdown. Deterministic on purpose:
	 * it counts rather than sleeps, because a barrier that waits "long
	 * enough" is the kind of test that passes for the wrong reason.
	 */
	void awaitIdle() {
		long target = submitted.get() - dropped.get();
		while (delivered.get() < target) {
			Thread current = dispatcher;
			if (current == null || !current.isAlive()) {
				return; // nothing is going to move it along any more
			}
			Thread.onSpinWait();
		}
	}

	long droppedCount() {
		return dropped.get();
	}

	private synchronized void start() {
		if (dispatcher != null || closed) {
			return;
		}
		Thread thread = new Thread(this::run, "ddsr-event-delivery");
		// A daemon: the delivery of a lifecycle event must never be the
		// reason a JVM refuses to exit.
		thread.setDaemon(true);
		dispatcher = thread;
		thread.start();
	}

	private void run() {
		while (true) {
			ServiceEvent event;
			try {
				event = queue.take();
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				return;
			}
			if (event == POISON) {
				return;
			}
			try {
				sink.publish(event);
			} catch (RuntimeException sinkFailure) {
				// A sink must not throw, and we do not trust it to keep
				// that promise: the change behind this event is committed
				// and saved, and one bad subscriber may not stop the
				// others from hearing about it.
				LOG.log(Level.WARNING, "[DDSR] a sink threw while receiving "
						+ event.getType().getLiteral(), sinkFailure);
			} finally {
				delivered.incrementAndGet();
			}
		}
	}

	/**
	 * Delivers what is still queued, then stops.
	 *
	 * <p>Draining rather than dropping: these events are about changes
	 * that are already saved, and a subscriber that is still connected
	 * during a shutdown has every right to hear about them.
	 */
	@Override
	public void close() {
		Thread current;
		synchronized (this) {
			if (closed) {
				return;
			}
			closed = true;
			current = dispatcher;
		}
		if (current == null) {
			return;
		}
		awaitIdle();
		if (!queue.offer(POISON)) {
			current.interrupt();
		}
		try {
			current.join(TimeUnit.SECONDS.toMillis(5));
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
		dispatcher = null;
	}
}
