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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

import org.eclipse.fennec.services.runtime.BrokerRuntime;
import org.eclipse.fennec.services.runtime.BrokerRuntimeDTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Registers the broker's runtime service and keeps its revision
 * property current (#126).
 *
 * <p>The property is how a watcher is told: a consumer binds the
 * service dynamically and the component runtime calls its
 * {@code modified} when the value changes. No listener to register, no
 * lifecycle to know — which is the whole point of answering with a
 * service rather than with an event.
 *
 * <p><strong>On its own thread, and coalesced.</strong> Updating a
 * service property makes the component runtime call every bound
 * consumer synchronously; doing that on the broker's mutating thread
 * would put a consumer's work inside the broker's write lock, and a
 * burst of registrations would do it once per registration. So the
 * broker only sets a flag, and this thread publishes what the flag
 * means — at most once per pass, whatever happened in between.
 *
 * <p>The number itself is deliberately dumb: it counts changes rather
 * than describing them. "Has anything happened" is a comparison, and
 * "what" is another snapshot.
 */
final class RuntimePublisher implements AutoCloseable {

	private static final Logger LOG = Logger.getLogger(RuntimePublisher.class.getName());

	/**
	 * How long a burst is given to finish before the property is
	 * updated. Long enough that publishing twenty services wakes the
	 * watchers once, short enough that nobody waits for news.
	 */
	private static final long COALESCE_MILLIS = 200;

	private final BundleContext context;

	private final DdsrBrokerImpl broker;

	private final AtomicLong published = new AtomicLong(-1);

	private final AtomicBoolean running = new AtomicBoolean(true);

	private volatile ServiceRegistration<BrokerRuntime> registration;

	private volatile Thread publisher;

	RuntimePublisher(BundleContext context, DdsrBrokerImpl broker) {
		this.context = context;
		this.broker = broker;
	}

	/** Register the service and start keeping it current. */
	void open() {
		BrokerRuntime service = new BrokerRuntime() {

			@Override
			public BrokerRuntimeDTO snapshot() {
				return broker.runtimeSnapshot();
			}
		};
		registration = context.registerService(BrokerRuntime.class, service, propertiesFor(broker.runtimeRevision()));
		published.set(broker.runtimeRevision());

		publisher = new Thread(this::publishChanges, "ddsr-runtime-publisher");
		publisher.setDaemon(true);
		publisher.start();
		LOG.info("[DDSR] runtime service registered — watchers are told through "
				+ BrokerRuntime.REVISION_PROPERTY);
	}

	/**
	 * Publishes the revision whenever it has moved.
	 *
	 * <p>A poll rather than a wake-up, and on purpose: the broker's
	 * mutating path stays a single atomic increment with nothing to
	 * notify, and the cost here is one comparison every
	 * {@value #COALESCE_MILLIS} milliseconds on a daemon thread.
	 */
	private void publishChanges() {
		while (running.get()) {
			LockSupport.parkNanos(COALESCE_MILLIS * 1_000_000L);
			if (!running.get()) {
				return;
			}
			long current = broker.runtimeRevision();
			if (current == published.get()) {
				continue;
			}
			ServiceRegistration<BrokerRuntime> live = registration;
			if (live == null) {
				return;
			}
			try {
				live.setProperties(propertiesFor(current));
				published.set(current);
			} catch (IllegalStateException unregistered) {
				// The service went away under us, which happens exactly
				// once, on the way down.
				return;
			} catch (RuntimeException watcherFailed) {
				// A consumer's `modified` threw. That is its problem, not
				// the broker's, and it must not stop the next update.
				LOG.warning("[DDSR] a runtime watcher failed on revision " + current + ": " + watcherFailed);
				published.set(current);
			}
		}
	}

	private static Dictionary<String, Object> propertiesFor(long revision) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(BrokerRuntime.REVISION_PROPERTY, revision);
		return properties;
	}

	@Override
	public void close() {
		running.set(false);
		Thread current = publisher;
		if (current != null) {
			LockSupport.unpark(current);
			publisher = null;
		}
		ServiceRegistration<BrokerRuntime> live = registration;
		registration = null;
		if (live != null) {
			try {
				live.unregister();
			} catch (IllegalStateException alreadyGone) {
				// The framework got there first.
			}
		}
	}
}
