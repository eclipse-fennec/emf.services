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

package org.eclipse.fennec.services.examples.rsa.api;

import org.eclipse.fennec.services.derive.Broadcast;
import org.osgi.util.pushstream.PushStream;

/**
 * A service that answers with a stream.
 *
 * <p>Like {@link Greeter} it is an ordinary Java interface, and the
 * return type says all there is to say: the values come one after the
 * other, for as long as there are any. In the contract derived from
 * this interface the return value is a stream of {@code int}
 * ({@code delivery = STREAM}); how a stream travels is the transport's
 * business — over a messaging broker as one answer per value, over REST
 * as Server-Sent Events.
 *
 * <p>{@link #beats} is the one stream every receiver shares: one source,
 * however many listen (#252).
 */
public interface Ticker {

	/**
	 * @param upTo the last number of the stream
	 * @return the numbers from 1 to {@code upTo}, one after the other
	 */
	PushStream<Integer> count(int upTo);

	/**
	 * A stream that does not end by itself: what a caller leaves when it
	 * has seen enough, and what has to stop at its source when it does.
	 *
	 * @param everyMillis the time between two numbers
	 * @return the numbers from 1 on, until the caller closes the stream
	 */
	PushStream<Integer> forever(long everyMillis);

	/**
	 * The beat of the ticker, the same for everyone who listens: a number
	 * every 20 ms from 1 on, sent at the ticker's pace whoever listens and
	 * however fast. A listener that falls behind skips to the latest —
	 * what it may lose, the contract says.
	 *
	 * @return the beats from whenever this listener came
	 */
	@Broadcast(overflow = "DISCARD_OLDEST")
	PushStream<Integer> beats();
}
