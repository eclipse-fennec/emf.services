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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.examples.rsa.api.Ticker;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Consumes a stream from a service in another framework.
 *
 * <p>Written as it would be against a local service: the method returns
 * a PushStream and the caller says what to do with each value and with
 * the end. The call does not wait — the stream is consumed as its
 * values arrive, and how fast this consumer takes them is how fast the
 * other side is asked for them.
 */
@Component(immediate = true)
public class TickerCaller {

	private static final Logger LOG = Logger.getLogger(TickerCaller.class.getName());

	private static final int UP_TO = 5;

	@Reference
	private Ticker ticker;

	@Activate
	void activate() {
		// Nothing is sent before the stream is consumed, and consuming it
		// does not wait: this returns at once, and the ticks arrive on a
		// thread of the stream's.
		List<Integer> ticks = new CopyOnWriteArrayList<>();
		ticker.count(UP_TO).forEach(ticks::add)
				.onSuccess(ended -> LOG.info("[RSA-Consumer] ticks " + ticks + " — the stream ended"))
				.onFailure(broken -> LOG.log(Level.WARNING, "[RSA-Consumer] the stream of ticks broke off after "
						+ ticks, broken));
	}
}
