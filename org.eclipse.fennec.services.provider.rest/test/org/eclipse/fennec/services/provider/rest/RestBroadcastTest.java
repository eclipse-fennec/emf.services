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

package org.eclipse.fennec.services.provider.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Future;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.fennec.services.OverflowPolicy;
import org.eclipse.fennec.services.flavor.rest.RestStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.StreamingOutput;

/** One source, many receivers over REST, each protected by its own buffer (#252). */
class RestBroadcastTest {

	private static final SseStream.Encoder TEXT = new SseStream.Encoder() {

		@Override
		public byte[] value(Object value) {
			return String.valueOf(value).getBytes(UTF_8);
		}

		@Override
		public byte[] failure(Throwable failure) {
			return String.valueOf(failure.getMessage()).getBytes(UTF_8);
		}
	};

	private final ExecutorService threads = Executors.newCachedThreadPool();

	@AfterEach
	void stop() {
		threads.shutdownNow();
	}

	/** A source that sends 0..count-1 once both receivers are there, then ends; counts its runs. */
	private static final class Source {

		final AtomicInteger runs = new AtomicInteger();

		final CountDownLatch bothThere = new CountDownLatch(1);

		final int count;

		Source(int count) {
			this.count = count;
		}

		Flow.Publisher<Object> run() {
			runs.incrementAndGet();
			SubmissionPublisher<Object> values = new SubmissionPublisher<>(Runnable::run, 1);
			new Thread(() -> {
				try {
					bothThere.await(5, TimeUnit.SECONDS);
				} catch (InterruptedException interrupted) {
					return;
				}
				for (int i = 0; i < count; i++) {
					values.submit(i);
				}
				values.close();
			}).start();
			return values;
		}
	}

	/** An output that takes nothing until it is let go, then everything. */
	private static final class Stuck extends OutputStream {

		final CountDownLatch letGo = new CountDownLatch(1);

		final ByteArrayOutputStream taken = new ByteArrayOutputStream();

		@Override
		public void write(int b) throws IOException {
			await();
			taken.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			await();
			taken.write(b, off, len);
		}

		private void await() throws IOException {
			try {
				letGo.await();
			} catch (InterruptedException interrupted) {
				throw new IOException(interrupted);
			}
		}
	}

	private Future<?> receive(StreamingOutput receiver, OutputStream out) {
		return threads.submit(() -> {
			receiver.write(out);
			return null;
		});
	}

	private static int values(String events) {
		return events.split("event: " + RestStreams.VALUE, -1).length - 1;
	}

	@Test
	void latestWinsTheFastReceiverGetsEverythingTheSlowOneWhatItsBufferHeld() throws Exception {
		Source source = new Source(50);
		RestBroadcast broadcast = new RestBroadcast(source::run, TEXT, "text/plain", OverflowPolicy.DISCARD_OLDEST,
				4, "readings");
		ByteArrayOutputStream fast = new ByteArrayOutputStream();
		Stuck slow = new Stuck();
		Future<?> fastDone = receive(broadcast.receiver(), fast);
		Future<?> slowDone = receive(broadcast.receiver(), slow);
		Thread.sleep(200);
		source.bothThere.countDown();

		fastDone.get(5, TimeUnit.SECONDS);
		slow.letGo.countDown();
		slowDone.get(5, TimeUnit.SECONDS);

		String fastEvents = fast.toString(UTF_8);
		String slowEvents = slow.taken.toString(UTF_8);
		assertThat(values(fastEvents)).isEqualTo(50);
		assertThat(fastEvents).endsWith("event: end\ndata:\n\n");
		assertThat(values(slowEvents)).as("what it held, at most one in writing beside the buffer").isLessThanOrEqualTo(5);
		assertThat(slowEvents).contains("data: 49").endsWith("event: end\ndata:\n\n");
		assertThat(source.runs).as("one source, however many receive it").hasValue(1);
	}

	@Test
	void mustNotLoseTheSlowReceiverFailsAndTheFastOneGoesOn() throws Exception {
		Source source = new Source(50);
		RestBroadcast broadcast = new RestBroadcast(source::run, TEXT, "text/plain", OverflowPolicy.FAIL, 4,
				"readings");
		ByteArrayOutputStream fast = new ByteArrayOutputStream();
		Stuck slow = new Stuck();
		Future<?> fastDone = receive(broadcast.receiver(), fast);
		Future<?> slowDone = receive(broadcast.receiver(), slow);
		Thread.sleep(200);
		source.bothThere.countDown();

		fastDone.get(5, TimeUnit.SECONDS);
		slow.letGo.countDown();
		slowDone.get(5, TimeUnit.SECONDS);

		assertThat(values(fast.toString(UTF_8))).isEqualTo(50);
		String slowEvents = slow.taken.toString(UTF_8);
		assertThat(slowEvents).contains("event: error").contains("fell behind").doesNotContain("event: end");
		assertThat(slowEvents).as("nothing it was given is lost: 0 to what its buffer held, in order")
				.contains("data: 0\n");
	}
}
