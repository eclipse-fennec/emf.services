/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.client.rest.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The subset of the SSE wire format the broker emits, parsed line by
 * line: {@code data:} lines accumulate (joined with a newline, exactly
 * one optional leading space belongs to the framing), a blank line
 * flushes the accumulated payload, every other line — {@code event:},
 * {@code id:}, {@code retry:} and {@code :} comments such as the
 * broker's keepalive — is ignored. The event name is not needed: the
 * broker sends exactly one kind of event on this stream. Mirror of the
 * TypeScript {@code sse-parser.ts}; pinned by {@code SseFramesTest}.
 */
final class SseFrames {

	private SseFrames() {
	}

	/**
	 * Pumps until the reader ends or {@code running} turns false,
	 * handing every complete payload to {@code onData}.
	 */
	static void pump(BufferedReader lines, BooleanSupplier running, Consumer<String> onData) throws IOException {
		pumpEvents(lines, running, (event, data) -> {
			if (!data.isEmpty()) {
				onData.accept(data);
			}
		});
	}

	/**
	 * The same, with the event's name — {@code message} where the frame
	 * names none, as SSE has it — for a stream whose events say what they
	 * are (#252). An event with a name and no data is handed over too: a
	 * stream's {@code end} carries nothing.
	 */
	static void pumpEvents(BufferedReader lines, BooleanSupplier running, BiConsumer<String, String> onEvent)
			throws IOException {
		StringBuilder data = new StringBuilder();
		String event = null;
		boolean dataSeen = false;
		String line;
		while (running.getAsBoolean() && (line = lines.readLine()) != null) {
			if (line.isEmpty()) {
				if (dataSeen || event != null) {
					onEvent.accept(event == null ? "message" : event, data.toString());
				}
				data.setLength(0);
				event = null;
				dataSeen = false;
				continue;
			}
			if (line.startsWith("event:")) {
				event = line.substring("event:".length()).trim();
				continue;
			}
			if (line.startsWith("data:")) {
				String value = line.substring("data:".length());
				if (value.startsWith(" ")) {
					value = value.substring(1);
				}
				if (dataSeen) {
					data.append('\n');
				}
				data.append(value);
				dataSeen = true;
			}
		}
	}
}
