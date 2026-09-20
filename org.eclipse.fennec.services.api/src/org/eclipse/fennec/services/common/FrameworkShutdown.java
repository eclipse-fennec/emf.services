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

package org.eclipse.fennec.services.common;

import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;

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
 * <p>
 * <p>
 * Not a component, and that is the point. It used to be one, first in
 * broker.core where it quietly served any launch that happened to drag
 * that bundle in, then in a bundle of its own holding nothing else. A
 * hook is not a contract, so it does not belong in an API bundle as a
 * component — but installing one is something a component that already
 * owns a lifecycle can simply do. The broker's and the client's
 * components each call this; between them they cover every launch we
 * ship.
 */
public final class FrameworkShutdown {

	/** Upper bound for the framework stop on JVM shutdown. */
	private static final long STOP_TIMEOUT_MILLIS = 15_000L;

	private FrameworkShutdown() {
	}

	/**
	 * Installs the hook and hands back the way to remove it again.
	 *
	 * <p>Installing it twice in one framework is harmless: the second
	 * hook finds the framework already stopping and returns. Closing the
	 * handle during JVM shutdown is equally harmless — the hook is
	 * already running by then.
	 *
	 * @param context any bundle's context; only the system bundle is
	 *        taken from it
	 * @return a handle that removes the hook, for the caller's deactivate
	 */
	public static AutoCloseable installFor(BundleContext context) {
		Framework framework = (Framework) context.getBundle(0);
		Thread hook = new Thread(() -> stopFramework(framework), "ddsr-clean-shutdown");
		Runtime.getRuntime().addShutdownHook(hook);
		return () -> {
			try {
				Runtime.getRuntime().removeShutdownHook(hook);
			} catch (IllegalStateException alreadyShuttingDown) {
				// JVM shutdown in progress — the hook is running, fine.
			}
		};
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
