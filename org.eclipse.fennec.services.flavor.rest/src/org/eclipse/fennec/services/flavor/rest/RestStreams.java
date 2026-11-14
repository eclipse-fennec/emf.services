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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;
import java.util.Locale;

import org.eclipse.fennec.services.Delivery;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceOperation;

/**
 * How a stream travels over REST (#252): as Server-Sent Events, one event
 * per value, shared by the provider that writes them and the consumer that
 * reads them.
 *
 * <p>Every value is a {@code ServiceInvocationResult} in the encoding the
 * flavor says the operation produces — the same document a value has over
 * messaging, so a stream is one thing on every transport. The event names
 * say what the document is:
 * <ul>
 * <li>{@value #VALUE} — the next value;</li>
 * <li>{@value #ERROR} — the last event of a stream that failed: the
 * document holds the Diagnostic, a declared error under its code;</li>
 * <li>{@value #END} — the stream is over, with no data. A connection that
 * closes without it broke off.</li>
 * </ul>
 * A document in a textual encoding (XML, JSON, text) is the data as it
 * is, one {@code data:} line per line; any other encoding (Protobuf)
 * travels Base64-encoded on one line.
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

	/** Whether a document in this encoding is text, and travels as it is. */
	public static boolean isText(String contentType) {
		if (contentType == null) {
			return true;
		}
		String type = contentType.toLowerCase(Locale.ROOT);
		int parameters = type.indexOf(';');
		if (parameters >= 0) {
			type = type.substring(0, parameters).trim();
		}
		return type.startsWith("text/") || type.equals("application/xml") || type.equals("application/json")
				|| type.endsWith("+xml") || type.endsWith("+json");
	}

	/** One event: its name and the document as data, ready to be written. */
	public static String frame(String event, byte[] document, String contentType) {
		StringBuilder frame = new StringBuilder("event: ").append(event).append('\n');
		if (document == null || document.length == 0) {
			frame.append("data:\n");
		} else if (isText(contentType)) {
			String text = new String(document, UTF_8).replace("\r\n", "\n").replace('\r', '\n');
			for (String line : text.split("\n", -1)) {
				frame.append("data: ").append(line).append('\n');
			}
		} else {
			frame.append("data: ").append(Base64.getEncoder().encodeToString(document)).append('\n');
		}
		return frame.append('\n').toString();
	}

	/** The document an event's data carries, the inverse of {@link #frame}. */
	public static byte[] document(String data, String contentType) {
		if (data == null || data.isEmpty()) {
			return new byte[0];
		}
		return isText(contentType) ? data.getBytes(UTF_8) : Base64.getDecoder().decode(data.trim());
	}
}
