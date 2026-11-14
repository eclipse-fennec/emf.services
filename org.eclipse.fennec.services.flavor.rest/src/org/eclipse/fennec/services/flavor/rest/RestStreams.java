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

package org.eclipse.fennec.services.flavor.rest;

import org.eclipse.fennec.services.Delivery;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceOperation;

/**
 * How a stream travels over REST (#252): as Server-Sent Events, one event
 * per value, shared by the provider that writes them and the consumer that
 * reads them.
 *
 * <p>The data of an event is a CloudEvents message in structured mode —
 * the same message a value is over a messaging broker: for a stream of its
 * own an {@code invoke.reply}, for a stream every receiver shares a
 * {@code broadcast}, each holding a {@code ServiceInvocationResult} in the
 * encoding the flavor says the operation produces. The event names say
 * what the message is:
 * <ul>
 * <li>{@value #VALUE} — the next value;</li>
 * <li>{@value #ERROR} — the last event of a stream that failed: the
 * result holds the Diagnostic, a declared error under its code;</li>
 * <li>{@value #END} — the stream is over, with no data. A connection that
 * closes without it broke off.</li>
 * </ul>
 *
 * <p>The caller ends a stream by closing the connection; TCP is the
 * backpressure. The TypeScript twin is {@code rest-streams.ts}.
 */
public final class RestStreams {

	/** The media type of a streamed answer. */
	public static final String MEDIA_TYPE = "text/event-stream";

	/** The event carrying the next value. */
	public static final String VALUE = "value";

	/** The event ending a stream that failed. */
	public static final String ERROR = "error";

	/** The event ending a stream that is over. */
	public static final String END = "end";

	/** What a value is written in where the flavor says nothing. */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";

	private RestStreams() {
	}

	/** Whether an operation answers with a stream — of its own or a shared one. */
	public static boolean streams(ServiceOperation operation) {
		Parameter returned = operation == null ? null : operation.getReturnValue();
		return returned != null
				&& (returned.getDelivery() == Delivery.STREAM || returned.getDelivery() == Delivery.BROADCAST);
	}

	/** The encoding of each value: the first type the flavor produces, XMI otherwise. */
	public static String contentTypeOf(RestOperationFlavor operationFlavor) {
		return operationFlavor == null || operationFlavor.getProduces().isEmpty() ? DEFAULT_CONTENT_TYPE
				: operationFlavor.getProduces().get(0);
	}

	/** One event: its name and its data, one {@code data:} line per line, ready to be written. */
	public static String frame(String event, String data) {
		StringBuilder frame = new StringBuilder("event: ").append(event).append('\n');
		if (data == null || data.isEmpty()) {
			frame.append("data:\n");
		} else {
			for (String line : data.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
				frame.append("data: ").append(line).append('\n');
			}
		}
		return frame.append('\n').toString();
	}
}
