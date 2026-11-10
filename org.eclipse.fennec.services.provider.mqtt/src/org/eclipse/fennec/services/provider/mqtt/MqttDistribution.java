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

package org.eclipse.fennec.services.provider.mqtt;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.fennec.services.MqttFlavor;

/**
 * Serves a contract over MQTT: one subscription per operation of the
 * flavor, answering on the topic each call names.
 *
 * <p>The mirror of {@code RestDistribution}, and deliberately the same
 * shape: a caller hands over a flavor and the service behind it, and
 * gets something it can close. Remote Service Admin uses this to export
 * over MQTT without knowing anything about the connection, topics or
 * the envelope.
 *
 * <p>Where it connects is the flavor's own statement — the brokers it
 * announces are the brokers it listens on, the same way
 * {@code RestFlavor.host} says where a REST export answers.
 */
public interface MqttDistribution {

	/**
	 * Listen for calls of this contract.
	 *
	 * @param flavor  what the contract's operations are reachable as; its
	 *                first broker entry is where this listens
	 * @param service the object that answers
	 * @param name    what to call this in logs
	 * @return closing it stops answering. Idempotent.
	 */
	Served serve(MqttFlavor flavor, Object service, String name);

	/**
	 * The same, for a caller that resolves the service per call.
	 *
	 * <p>What a generic distribution needs: the implementation behind a
	 * contract comes and goes, and a transport that captured it once
	 * would answer on behalf of a service that is no longer there. The
	 * supplier is asked per call and may answer null, which the caller
	 * is told about as a failed invocation rather than as silence.
	 */
	Served serve(MqttFlavor flavor, Supplier<Object> service, String name);

	/**
	 * Hold the connection to a broker, and be told once it is there.
	 *
	 * <p>For whoever knows its broker before it has anything to serve.
	 * A connection comes up on its own time — {@link #serve} does not
	 * wait for it, because it is called on threads that must not wait —
	 * so a caller that wants its first export to be listening the moment
	 * it is announced opens the connection early and offers itself only
	 * when it is told.
	 *
	 * @param brokerUrl     where the broker is
	 * @param whenConnected called once, with the first connection
	 * @return closing it lets the connection go when nothing else
	 *         holds it. Idempotent.
	 */
	AutoCloseable hold(String brokerUrl, Runnable whenConnected);

	/** A contract being served. */
	interface Served extends AutoCloseable {

		/** The topics it listens on, one per operation. */
		List<String> topics();

		/**
		 * Be told once the topics are subscribed on the broker — at
		 * once when they already are. Announcing belongs here: a
		 * consumer that is told about a service calls it, and a call to
		 * a topic nobody listens on yet is answered by nobody.
		 */
		void whenListening(Runnable listening);

		/** Stop answering. Idempotent. */
		@Override
		void close();
	}
}
