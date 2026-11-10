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

package org.eclipse.fennec.services.provider.amqp;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.fennec.services.AmqpFlavor;

/**
 * Serves a contract over AMQP: one binding per operation of the flavor,
 * answering to the reply address each call names.
 *
 * <p>The shape of {@code MqttDistribution} and of {@code
 * RestDistribution} before it: a caller hands over a flavor and the
 * service behind it, and gets something it can close. Where it connects
 * is the flavor's own statement — its first broker, on its exchange.
 */
public interface AmqpDistribution {

	/**
	 * Listen for calls of this contract.
	 *
	 * <p>Does not wait for the connection: it is called on threads that
	 * must not. {@link Served#whenListening} says when the bindings are
	 * in place.
	 *
	 * @param flavor  what the contract's operations are reachable as
	 * @param service asked per call for the object that answers; may
	 *                answer null, which the caller is told as a failed
	 *                invocation
	 * @param name    what to call this in logs
	 * @return closing it stops answering. Idempotent.
	 */
	Served serve(AmqpFlavor flavor, Supplier<Object> service, String name);

	/**
	 * Hold the connection to a broker, and be told once it is there —
	 * for whoever knows its broker before it has anything to serve, and
	 * offers itself only when an export made now would be listening.
	 *
	 * @param brokerUrl     where the broker is
	 * @param exchange      the topic exchange, or empty for the broker's
	 *                      own
	 * @param whenConnected called once, with the first connection
	 * @return closing it lets the connection go when nothing else holds
	 *         it. Idempotent.
	 */
	AutoCloseable hold(String brokerUrl, String exchange, Runnable whenConnected);

	/** A contract being served. */
	interface Served extends AutoCloseable {

		/** The routing keys it listens on, one per operation. */
		List<String> routingKeys();

		/**
		 * Be told once the bindings are in place on the broker — at once
		 * when they already are. Announcing belongs here.
		 */
		void whenListening(Runnable listening);

		/** Stop answering. Idempotent. */
		@Override
		void close();
	}
}
