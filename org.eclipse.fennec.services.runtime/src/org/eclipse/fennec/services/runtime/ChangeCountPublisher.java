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

package org.eclipse.fennec.services.runtime;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * Registers a runtime service and keeps its change count current
 * (#126).
 *
 * <p>{@link Constants#SERVICE_CHANGECOUNT} is how a watcher is told:
 * a consumer binds the service dynamically and the component runtime
 * calls its {@code modified} when the value changes. No listener to register, no
 * lifecycle to know — which is the whole point of answering with a
 * service rather than with an event.
 *
 * <p>Shared by both runtimes, because the protocol is the same on both
 * sides: a number that moves when the answer would, and a service that
 * hands out the answer.
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
 *
 * <p><strong>The publisher owns the dictionary.</strong>
 * {@link ServiceRegistration#setProperties} replaces rather than
 * merges, so a property set on the registration from outside would be
 * gone after the next change. Whatever describes the service — and
 * should be filterable, the way {@code osgi.jakartars.endpoint} is —
 * is handed in up front and carried into every update (#188).
 */
public final class ChangeCountPublisher<T> implements AutoCloseable {

	private static final Logger LOG = Logger.getLogger(ChangeCountPublisher.class.getName());

	/**
	 * How long a burst is given to finish before the property is
	 * updated. Long enough that publishing twenty services wakes the
	 * watchers once, short enough that nobody waits for news.
	 */
	private static final long COALESCE_MILLIS = 200;

	private final BundleContext context;

	private final Class<T> type;

	private final T service;

	private final LongSupplier changeCount;

	private final String what;

	private final Map<String, Object> extraProperties;

	private final AtomicLong published = new AtomicLong(-1);

	private final AtomicBoolean running = new AtomicBoolean(true);

	private volatile ServiceRegistration<T> registration;

	private volatile Thread publisher;

	/**
	 * @param type     the service type to register under
	 * @param service  what answers
	 * @param changeCount the number that moves when the answer would
	 * @param what     how to name this in the log
	 */
	public ChangeCountPublisher(BundleContext context, Class<T> type, T service, LongSupplier changeCount,
			String what) {
		this(context, type, service, changeCount, what, new Hashtable<>());
	}

	/**
	 * @param type     the service type to register under
	 * @param service  what answers
	 * @param changeCount the number that moves when the answer would
	 * @param what     how to name this in the log
	 * @param extraProperties registered with the service and kept on
	 *                 every update; copied, so later changes to the
	 *                 dictionary do not reach the registration. A
	 *                 {@value Constants#SERVICE_CHANGECOUNT} among them
	 *                 is ignored — that one is this class's to set.
	 */
	public ChangeCountPublisher(BundleContext context, Class<T> type, T service, LongSupplier changeCount,
			String what, Dictionary<String, ?> extraProperties) {
		this.context = context;
		this.type = type;
		this.service = service;
		this.changeCount = changeCount;
		this.what = what;
		Hashtable<String, Object> copy = new Hashtable<>();
		for (String key : Collections.list(extraProperties.keys())) {
			copy.put(key, extraProperties.get(key));
		}
		this.extraProperties = Collections.unmodifiableMap(copy);
	}

	/** Register the service and start keeping it current. */
	public void open() {
		registration = context.registerService(type, service, propertiesFor(changeCount.getAsLong()));
		published.set(changeCount.getAsLong());

		publisher = new Thread(this::publishChanges, "ddsr-runtime-publisher");
		publisher.setDaemon(true);
		publisher.start();
		LOG.info("[DDSR] " + what + " registered — watchers are told through "
				+ Constants.SERVICE_CHANGECOUNT);
	}

	/**
	 * Publishes the change count whenever it has moved.
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
			long current = changeCount.getAsLong();
			if (current == published.get()) {
				continue;
			}
			ServiceRegistration<T> live = registration;
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
				LOG.warning("[DDSR] a runtime watcher failed on change count " + current + ": " + watcherFailed);
				published.set(current);
			}
		}
	}

	private Dictionary<String, Object> propertiesFor(long changeCount) {
		// The caller's properties first, so they cannot override the
		// change count.
		Dictionary<String, Object> properties = new Hashtable<>(extraProperties);
		properties.put(Constants.SERVICE_CHANGECOUNT, changeCount);
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
		ServiceRegistration<T> live = registration;
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
