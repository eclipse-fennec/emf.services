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
 * Calls the TypeScript-side {@code Payment} implementation via its
 * registered proxy. Activates only when {@link PaymentProxyRegistrar}
 * has registered a {@link PaymentRemote} carrying
 * {@code ddsr.provider.name=payments-ts} — i.e. the TS colleague has
 * published his impl and the lookup found it. The cross-language
 * proof: the same {@code PaymentRemote} interface that talks to our
 * Java provider now talks to a TS provider, only the locator/proxy
 * differs underneath.
 *
 * <p>Transient connect failures are retried (TS service might come up
 * slightly after the broker catalog has the publish recorded).
 *
 * <p>Delete this class once Payment cross-language integration is
 * real (or replace it with a meaningful application).
 */
@Component(immediate = true)
public final class TsPaymentDebug {

	private static final Logger LOG = Logger.getLogger(TsPaymentDebug.class.getName());

	private static final int MAX_ATTEMPTS = 5;
	private static final long BACKOFF_MILLIS = 400;

	@Reference(target = "(&(service.imported=true)(ddsr.provider.name=payments-ts))")
	private PaymentRemote payment;

	private static final String ACCOUNT_ID = "account-1";

	@Activate
	void activate() {
		LOG.info("[DDSR-Debug-TS] PaymentRemote (TS) proxy is available — trying getBalance(\""
				+ ACCOUNT_ID + "\")");
		Throwable last = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				double balance = payment.getBalance(ACCOUNT_ID);
				LOG.info("[DDSR-Debug-TS] ts.getBalance(\"" + ACCOUNT_ID + "\") = " + balance
						+ (attempt > 1 ? " (after " + attempt + " attempts)" : ""));
				// One charge call to round out the cross-language demo.
				try {
					double remaining = payment.charge(10.0, "EUR");
					LOG.info("[DDSR-Debug-TS] ts.charge(10.0, \"EUR\") = " + remaining);
				} catch (Throwable t) {
					LOG.warning("[DDSR-Debug-TS] ts.charge FAILED: " + t);
				}
				return;
			} catch (Throwable t) {
				last = t;
				if (!isTransient(t)) {
					LOG.log(Level.WARNING, "[DDSR-Debug-TS] TS payment call FAILED with non-transport error", t);
					return;
				}
				LOG.fine("[DDSR-Debug-TS]   attempt " + attempt + "/" + MAX_ATTEMPTS
						+ " failed (" + rootCause(t) + "); waiting " + BACKOFF_MILLIS + "ms");
				try {
					Thread.sleep(BACKOFF_MILLIS);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
		LOG.warning("[DDSR-Debug-TS] TS payment call FAILED after " + MAX_ATTEMPTS
				+ " attempts: " + rootCause(last));
	}

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
