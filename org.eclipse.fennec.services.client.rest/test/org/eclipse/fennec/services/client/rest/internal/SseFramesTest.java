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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

/** Java twin of the TS {@code sse-parser.test.ts} (#56): both clients must cut the stream identically. */
class SseFramesTest {

	private static List<String> parse(String stream) throws IOException {
		List<String> out = new ArrayList<>();
		SseFrames.pump(new BufferedReader(new StringReader(stream)), () -> true, out::add);
		return out;
	}

	@Test
	void flushesADataPayloadOnTheBlankLine() throws IOException {
		assertThat(parse("data: hello\n\n")).containsExactly("hello");
	}

	@Test
	void joinsMultipleDataLinesWithNewline() throws IOException {
		assertThat(parse("data: <a>\ndata: <b>\n\n")).containsExactly("<a>\n<b>");
	}

	@Test
	void stripsExactlyOneLeadingSpaceAfterTheColon() throws IOException {
		assertThat(parse("data:  two\ndata:none\n\n")).containsExactly(" two\nnone");
	}

	@Test
	void ignoresEventIdRetryAndCommentLines() throws IOException {
		assertThat(parse("event: ddsr-service-event\nid: 7\nretry: 3000\n: keepalive\ndata: x\n\n"))
				.containsExactly("x");
	}

	@Test
	void handlesCrlfLineEndings() throws IOException {
		assertThat(parse("data: a\r\ndata: b\r\n\r\n")).containsExactly("a\nb");
	}

	@Test
	void emitsSeveralEventsFromOneStream() throws IOException {
		assertThat(parse("data: 1\n\ndata: 2\n\n: keepalive\n\ndata: 3\n\n")).containsExactly("1", "2", "3");
	}

	@Test
	void aBlankLineWithoutPendingDataEmitsNothing() throws IOException {
		assertThat(parse("\n\n: keepalive\n\n")).isEmpty();
	}

	@Test
	void anUnterminatedPayloadIsNotDelivered() throws IOException {
		// The broker terminates every event with a blank line; a half
		// event at connection loss must not be delivered as a document.
		assertThat(parse("data: partial")).isEmpty();
	}

	@Test
	void stopsWhenTheReaderIsAskedToStop() throws IOException {
		AtomicBoolean running = new AtomicBoolean(true);
		List<String> out = new ArrayList<>();
		BufferedReader lines = new BufferedReader(new StringReader("data: 1\n\ndata: 2\n\n"));
		SseFrames.pump(lines, () -> {
			boolean r = running.get();
			running.set(false); // stop after the first line has been read
			return r;
		}, out::add);
		assertThat(out).isEmpty();
	}
}
