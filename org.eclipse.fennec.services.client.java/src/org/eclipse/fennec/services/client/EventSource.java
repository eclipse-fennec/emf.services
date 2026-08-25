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

package org.eclipse.fennec.services.client;

import org.eclipse.fennec.services.ServiceEvent;

/**
 * The consumer's end of the broker's event stream, kept
 * transport-agnostic on purpose.
 * <p>
 * Mirror image of {@code EventSink} on the broker side: SSE is the
 * standard transport (REQUIREMENTS FR-Sync-Transport) and
 * {@code client.rest} implements this over it, but an MQTT or WebSocket
 * source is an additional implementation and needs no change in
 * {@code client.java}. The SDK stays transport-agnostic the same way it
 * already is for invocation.
 */
public interface EventSource {

	/**
	 * What an event source reports back to the SDK.
	 */
	interface Handler {

		/** One lifecycle event arrived. */
		void onEvent(ServiceEvent event);

		/**
		 * The stream was (re-)established after a drop.
		 * <p>
		 * Per FR-Sync-Reconnect the SDK must then pull a fresh snapshot
		 * instead of expecting missed events to be replayed — the broker
		 * keeps no per-client history and the stream carries no sequence
		 * numbers. Also called for the initial connect, so a consumer has
		 * exactly one code path for "my view may be stale".
		 */
		void onStreamEstablished();
	}

	/**
	 * Opens the stream. The returned handle stops delivery and releases
	 * the connection when closed.
	 *
	 * @param handler receives events and reconnect signals
	 * @return a handle that closes the subscription
	 */
	AutoCloseable open(Handler handler);
}
