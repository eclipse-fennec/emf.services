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

package org.eclipse.fennec.services.examples.rsa.consumer;

import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * A remote call, tried again a few times when it fails.
 *
 * <p>A service that may be remote may fail for a moment — the connection
 * to its broker is still being made, a provider is restarting — and what
 * a careful consumer does about it is to try again before it gives up.
 * The example says so here rather than relying on the transport, and it
 * knows nothing about which transport that is: any failure is tried again,
 * which is right for a call that changes nothing.
 */
final class Remote {

	private static final Logger LOG = Logger.getLogger(Remote.class.getName());

	private static final int ATTEMPTS = 5;

	private static final long PAUSE_MILLIS = 2000;

	private Remote() {
	}

	/** The value of the call, after at most five attempts two seconds apart. */
	static <T> T call(String what, Supplier<T> call) {
		RuntimeException last = null;
		for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
			try {
				return call.get();
			} catch (RuntimeException failed) {
				last = failed;
				if (attempt < ATTEMPTS) {
					LOG.info("[RSA-Consumer] " + what + " failed (" + failed.getMessage() + "), trying again");
					try {
						Thread.sleep(PAUSE_MILLIS);
					} catch (InterruptedException interrupted) {
						Thread.currentThread().interrupt();
						throw failed;
					}
				}
			}
		}
		throw last;
	}
}
