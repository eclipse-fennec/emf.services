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

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The work a broker does on its own, on a clock nobody asked it about.
 *
 * <p>Four sweeps: an armed handover has to be advanced, a silent
 * provider has to be retired, a session that stopped renewing has to
 * expire, and an idle registration may be parked. None of it is a
 * response to a request, and none of it is OSGi.
 *
 * <p>That last part is why this lives here and not in the component.
 * A broker embedded in a plain Java program used to get no sweeps at
 * all: handovers never completed, dead providers stayed listed and
 * sessions never expired. The same object with the same settings now
 * behaves the same either way, and the component's job shrinks to
 * turning a configuration into {@link BrokerSettings}.
 *
 * <p>One daemon thread runs all four. They are short, they take the
 * broker's write lock, and running them in parallel would only mean
 * contending for it.
 */
final class Maintenance implements AutoCloseable {

	private static final Logger LOG = Logger.getLogger(Maintenance.class.getName());

	private final BrokerSettings settings;

	private ScheduledExecutorService sweeps;

	Maintenance(BrokerSettings settings) {
		this.settings = settings;
	}

	/**
	 * Starts whichever sweeps the settings switched on.
	 *
	 * @param broker the broker to sweep; held only for the sweeps' sake
	 */
	void start(DdsrBrokerImpl broker) {
		if (!settings.anySweepEnabled()) {
			LOG.fine("[DDSR] no maintenance sweep is enabled");
			return;
		}
		sweeps = Executors.newSingleThreadScheduledExecutor(task -> {
			Thread thread = new Thread(task, "ddsr-broker-maintenance");
			thread.setDaemon(true);
			return thread;
		});

		schedule(settings.policySweepSeconds(), settings.policySweepSeconds(),
				() -> broker.advanceUpdatePolicies(Instant.now()),
				"update policies retired %d superseded implementation(s)", Level.INFO, "update-policy");

		schedule(settings.livenessSweepSeconds(), settings.livenessSweepSeconds(),
				() -> broker.retireLostProviders(Instant.now()),
				"retired %d registration(s) of silent provider(s)", Level.WARNING, "provider-liveness");

		// A quarter of the deadline: often enough that the overshoot is a
		// fraction of it, rarely enough to stay cheap.
		long expiry = settings.sessionExpirySeconds();
		schedule(expiry, quarterOf(expiry),
				() -> broker.expireSessions(Instant.now().minusSeconds(expiry)),
				"expired %d consumer session(s) without renewal", Level.INFO, "session expiry");

		long cold = settings.coldAfterSeconds();
		schedule(cold, quarterOf(cold),
				() -> broker.coldifyIdle(Instant.now().minusSeconds(cold)),
				"parked %d idle registration(s) in the cold cache", Level.INFO, "cold-cache");
	}

	private static long quarterOf(long seconds) {
		return Math.max(1, seconds / 4);
	}

	/**
	 * Schedules one sweep, unless it is switched off.
	 *
	 * <p>Every sweep is wrapped the same way, and the wrapping is the
	 * point: one that throws must not take the executor down with it and
	 * silently stop the other three.
	 */
	private void schedule(long enabledWhenPositive, long everySeconds, IntSupplier sweep,
			String didSomething, Level level, String what) {
		if (enabledWhenPositive <= 0) {
			return;
		}
		sweeps.scheduleAtFixedRate(() -> {
			try {
				int count = sweep.getAsInt();
				if (count > 0) {
					LOG.log(level, "[DDSR] " + String.format(didSomething, count));
				}
			} catch (RuntimeException sweepFailure) {
				LOG.warning("[DDSR] " + what + " sweep failed, continuing: " + sweepFailure);
			}
		}, everySeconds, everySeconds, TimeUnit.SECONDS);
	}

	@Override
	public void close() {
		if (sweeps != null) {
			sweeps.shutdownNow();
			sweeps = null;
		}
	}
}
