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

package org.eclipse.fennec.services.examples.rsa;

import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.eclipse.fennec.services.examples.rsa.api.Ticker;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;

/**
 * A service that answers with a stream, exported like the others.
 *
 * <p>Nothing here knows how the stream travels. Over a messaging broker
 * each number is an answer of its own, and the next one is asked of
 * this stream when the broker has the one before. A transport that
 * answers a request once cannot carry it — asked to export this service
 * over REST, the remote service admin refuses, and says which slot it
 * is about.
 */
@Component(
		service = Ticker.class,
		configurationPid = "org.eclipse.fennec.services.examples.rsa.ticker",
		property = {
				"service.exported.interfaces=*",
				"service.exported.configs=fennec.rest",
				"ddsr.provider.name=rsa-example" })
public class TickerImpl implements Ticker {

	private static final Logger LOG = Logger.getLogger(TickerImpl.class.getName());

	private final PushStreamProvider streams = new PushStreamProvider();

	@Activate
	void activate(Map<String, Object> properties) {
		LOG.info("[RSA-Example] Ticker asks to be exported as " + properties.get("service.exported.configs"));
	}

	@Override
	public PushStream<Integer> count(int upTo) {
		LOG.info("[RSA-Example] count(" + upTo + ") — a stream of " + upTo);
		return streams.streamOf(IntStream.rangeClosed(1, upTo).boxed());
	}
}
