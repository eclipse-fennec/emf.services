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

import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Stops the OSGi framework cleanly when the JVM is terminated from the
 * outside (SIGTERM/SIGINT — e.g. a container runtime or an operator).
 * <p>
 * The bnd launcher used for the exported jars does not install such a
 * hook itself: a plain SIGTERM kills the JVM without ever stopping the
 * framework, so no {@code @Deactivate} runs — the provider's blocking
 * withdraw (FR-P3/D2), the broker's final snapshot and the SSE
 * subscriber close would all be skipped. Found by the FR-P4 harness.
 * <p>
 * The hook stops the system bundle and waits (bounded) for the stop to
 * complete, which runs the whole DS deactivation chain synchronously
 * before the JVM exits.
 */
@Component(immediate = true)
public class FrameworkShutdownHook {

	/** Upper bound for the framework stop on JVM shutdown. */
	private static final long STOP_TIMEOUT_MILLIS = 15_000L;

	private Thread hook;

	@Activate
	void activate(BundleContext context) {
		Framework framework = (Framework) context.getBundle(0);
		hook = new Thread(() -> stopFramework(framework), "ddsr-clean-shutdown");
		Runtime.getRuntime().addShutdownHook(hook);
	}

	@Deactivate
	void deactivate() {
		if (hook != null) {
			try {
				Runtime.getRuntime().removeShutdownHook(hook);
			} catch (IllegalStateException alreadyShuttingDown) {
				// JVM shutdown in progress — the hook is running, fine.
			}
			hook = null;
		}
	}

	private static void stopFramework(Framework framework) {
		// Deliberately stdout/stderr, not JUL: this runs on the JVM-shutdown
		// path, where JUL's own cleanup hook may already have reset the
		// LogManager and logger output would vanish without a trace
		// (DECISIONS_PARITY D14).
		try {
			System.out.println("[DDSR] JVM shutdown — stopping the framework for a clean exit");
			framework.stop();
			framework.waitForStop(STOP_TIMEOUT_MILLIS);
			System.out.println("[DDSR] framework stopped");
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		} catch (Exception stopFailure) {
			System.err.println("[DDSR] clean framework stop on JVM shutdown failed: " + stopFailure);
			stopFailure.printStackTrace();
		}
	}
}
