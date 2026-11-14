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

package org.eclipse.fennec.services.client.rest.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.flavor.rest.RestStreams;
import org.junit.jupiter.api.Test;

/** A stream read from Server-Sent Events, as the subscriber asks for it (#252). */
class SseValuesTest {

	private static final String TEXT = "text/plain";

	private static final SseValues.Decoder STRINGS = new SseValues.Decoder() {

		@Override
		public Object value(byte[] document) {
			return new String(document, UTF_8);
		}

		@Override
		public RuntimeException failure(byte[] document) {
			return new DdsrException(new String(document, UTF_8));
		}
	};

	private static String events(String... frames) {
		return String.join("", frames);
	}

	private static String value(String text) {
		return RestStreams.frame(RestStreams.VALUE, text.getBytes(UTF_8), TEXT);
	}

	/** Records what arrives and asks for one value at a time, as told. */
	private static final class Recorder implements Flow.Subscriber<Object> {

		final List<Object> values = new CopyOnWriteArrayList<>();

		final CompletableFuture<Throwable> ended = new CompletableFuture<>();

		final AtomicReference<Flow.Subscription> subscription = new AtomicReference<>();

		final CountDownLatch first = new CountDownLatch(1);

		@Override
		public void onSubscribe(Flow.Subscription given) {
			subscription.set(given);
		}

		@Override
		public void onNext(Object item) {
			values.add(item);
			first.countDown();
		}

		@Override
		public void onError(Throwable throwable) {
			ended.complete(throwable);
		}

		@Override
		public void onComplete() {
			ended.complete(null);
		}
	}

	private static SseValues over(InputStream body, AtomicBoolean closed) {
		return new SseValues("readings", () -> new SseValues.Opened(body, () -> closed.set(true)), STRINGS, TEXT);
	}

	@Test
	void valuesArriveInOrderAndEndCompletes() throws Exception {
		AtomicBoolean closed = new AtomicBoolean();
		String body = events(value("1"), value("two\nlines"), RestStreams.frame(RestStreams.END, null, TEXT));
		Recorder recorder = new Recorder();
		over(new ByteArrayInputStream(body.getBytes(UTF_8)), closed).subscribe(recorder);
		recorder.subscription.get().request(Long.MAX_VALUE);

		assertThat(recorder.ended.get(5, TimeUnit.SECONDS)).isNull();
		assertThat(recorder.values).containsExactly("1", "two\nlines");
		assertThat(closed).isTrue();
	}

	@Test
	void aValueIsReadOnlyWhenOneIsAskedFor() throws Exception {
		String body = events(value("1"), value("2"), RestStreams.frame(RestStreams.END, null, TEXT));
		Recorder recorder = new Recorder();
		over(new ByteArrayInputStream(body.getBytes(UTF_8)), new AtomicBoolean()).subscribe(recorder);
		recorder.subscription.get().request(1);

		assertThat(recorder.first.await(5, TimeUnit.SECONDS)).isTrue();
		Thread.sleep(200);
		assertThat(recorder.values).containsExactly("1");
		assertThat(recorder.ended).isNotDone();

		recorder.subscription.get().request(1);
		assertThat(recorder.ended.get(5, TimeUnit.SECONDS)).isNull();
		assertThat(recorder.values).containsExactly("1", "2");
	}

	@Test
	void anErrorEventFailsTheStreamWithWhatItSays() throws Exception {
		String body = events(value("1"),
				RestStreams.frame(RestStreams.ERROR, "the sensor is gone".getBytes(UTF_8), TEXT));
		Recorder recorder = new Recorder();
		over(new ByteArrayInputStream(body.getBytes(UTF_8)), new AtomicBoolean()).subscribe(recorder);
		recorder.subscription.get().request(Long.MAX_VALUE);

		assertThat(recorder.ended.get(5, TimeUnit.SECONDS)).isInstanceOf(DdsrException.class)
				.hasMessage("the sensor is gone");
	}

	@Test
	void aConnectionThatClosesWithoutEndBrokeOff() throws Exception {
		Recorder recorder = new Recorder();
		over(new ByteArrayInputStream(value("1").getBytes(UTF_8)), new AtomicBoolean()).subscribe(recorder);
		recorder.subscription.get().request(Long.MAX_VALUE);

		assertThat(recorder.ended.get(5, TimeUnit.SECONDS)).isInstanceOf(DdsrException.class)
				.hasMessageContaining("broke off");
	}

	@Test
	void cancellingClosesTheConnection() throws Exception {
		PipedOutputStream provider = new PipedOutputStream();
		PipedInputStream body = new PipedInputStream(provider);
		AtomicBoolean closed = new AtomicBoolean();
		Recorder recorder = new Recorder();
		over(body, closed).subscribe(recorder);
		recorder.subscription.get().request(Long.MAX_VALUE);
		provider.write(value("1").getBytes(UTF_8));
		provider.flush();
		assertThat(recorder.first.await(5, TimeUnit.SECONDS)).isTrue();

		recorder.subscription.get().cancel();

		assertThat(closed).isTrue();
		assertThat(recorder.ended).as("a cancelled stream tells its subscriber nothing more").isNotDone();
	}

	@Test
	void aRefusedCallFailsTheStreamWithTheRefusal() throws Exception {
		DdsrException refused = new DdsrException("HTTP 403: not yours");
		Recorder recorder = new Recorder();
		new SseValues("readings", () -> {
			throw refused;
		}, STRINGS, TEXT).subscribe(recorder);
		recorder.subscription.get().request(1);

		assertThat(recorder.ended.get(5, TimeUnit.SECONDS)).isSameAs(refused);
	}

	@Test
	void aBinaryDocumentTravelsAsBase64() throws IOException {
		byte[] document = { 0, 1, 2, (byte) 0xff, '\n' };
		String frame = RestStreams.frame(RestStreams.VALUE, document, "application/x-protobuf");
		List<String> data = new CopyOnWriteArrayList<>();
		SseFrames.pumpEvents(new BufferedReader(new StringReader(frame)), () -> true,
				(event, text) -> data.add(text));
		assertThat(RestStreams.document(data.get(0), "application/x-protobuf")).containsExactly(document);
	}
}
