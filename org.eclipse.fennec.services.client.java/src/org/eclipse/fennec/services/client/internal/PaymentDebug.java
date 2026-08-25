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

package org.eclipse.fennec.services.client.internal;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Activates once a {@link PaymentRemote} proxy has been registered.
 * Calls {@code getBalance("42")} and logs the result.
 *
 * <p>If a Java-side Payment provider activates in the same launch
 * (e.g. via {@code org.eclipse.fennec.services.examples.payment}) the
 * publisher posts the impl into the catalog before this component
 * activates — but the actual HTTP endpoint may not yet be listening
 * on its port (Jetty + Jakartars Whiteboard wire the resource into
 * the application asynchronously). To tolerate the startup race
 * we retry a couple of times with a short backoff. After that, we
 * accept the failure as a real configuration problem and log it.
 *
 * <p>Delete this class once Payment integration is real.
 */
@Component(immediate = true)
public final class PaymentDebug {

	private static final Logger LOG = Logger.getLogger(PaymentDebug.class.getName());

	private static final int MAX_ATTEMPTS = 5;
	private static final long BACKOFF_MILLIS = 400;

	@Reference(target = "(&(service.imported=true)(ddsr.provider.name=payments-java))")
	private PaymentRemote payment;

	@Activate
	void activate() {
		LOG.info("[DDSR-Debug] PaymentRemote proxy is available — trying getBalance(\"42\")");
		Throwable last = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				double balance = payment.getBalance("42");
				LOG.info("[DDSR-Debug] payment.getBalance(\"42\") = " + balance
						+ (attempt > 1 ? " (after " + attempt + " attempts)" : ""));
				return;
			} catch (Throwable t) {
				last = t;
				if (!isTransient(t)) {
					LOG.log(Level.WARNING, "[DDSR-Debug] Payment call FAILED with non-transport error", t);
					return;
				}
				LOG.fine("[DDSR-Debug]   attempt " + attempt + "/" + MAX_ATTEMPTS
						+ " failed (" + rootCause(t) + "); waiting " + BACKOFF_MILLIS + "ms");
				try {
					Thread.sleep(BACKOFF_MILLIS);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
		LOG.warning("[DDSR-Debug] Payment call FAILED after " + MAX_ATTEMPTS
				+ " attempts: " + rootCause(last));
	}

	/**
	 * Treat transport/connect/timeout exceptions as transient. We do
	 * this by class name to avoid coupling client.java to JAX-RS.
	 */
	private static boolean isTransient(Throwable t) {
		for (Throwable c = t; c != null; c = c.getCause()) {
			String n = c.getClass().getName();
			if (n.equals("java.net.ConnectException")
					|| n.equals("java.net.SocketTimeoutException")
					|| n.equals("jakarta.ws.rs.ProcessingException")) {
				return true;
			}
		}
		return false;
	}

	private static Throwable rootCause(Throwable t) {
		Throwable c = t;
		while (c != null && c.getCause() != null && c.getCause() != c) {
			c = c.getCause();
		}
		return c;
	}
}
