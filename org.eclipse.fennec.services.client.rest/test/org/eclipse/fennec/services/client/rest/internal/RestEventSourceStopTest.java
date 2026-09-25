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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.client.EventSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * What the reader does with the answer to its stream request (#171).
 *
 * <p>204 No Content is how a server tells an event stream client to stop
 * (WHATWG EventSource). The reader used to take it for an empty stream,
 * announce an established connection, and reconnect every three seconds
 * for as long as the client lived — the bug Jersey's own SseEventSource
 * had until eclipse-ee4j/jersey#6119. Every other failure is still
 * reconnected to: that is FR-Sync-Reconnect, not an accident.
 */
class RestEventSourceStopTest {

	/** Counts what the reader tells its subscriber. */
	private static final class Recording implements EventSource.Handler {

		final AtomicInteger established = new AtomicInteger();
		final CountDownLatch ended = new CountDownLatch(1);

		@Override
		public void onEvent(ServiceEvent event) {
		}

		@Override
		public void onStreamEstablished() {
			established.incrementAndGet();
		}

		@Override
		public void onStreamEnded() {
			ended.countDown();
		}
	}

	private static Response answer(int status) {
		Response response = mock(Response.class);
		when(response.getStatus()).thenReturn(status);
		when(response.readEntity(InputStream.class)).thenReturn(new ByteArrayInputStream(new byte[0]));
		return response;
	}

	/** A target whose stream request is answered with these statuses, one per connect. */
	private static WebTarget answering(AtomicInteger connects, int... statuses) {
		Invocation.Builder builder = mock(Invocation.Builder.class);
		when(builder.get()).thenAnswer(invocation -> {
			int attempt = connects.getAndIncrement();
			return answer(statuses[Math.min(attempt, statuses.length - 1)]);
		});
		WebTarget target = mock(WebTarget.class);
		when(target.request(MediaType.SERVER_SENT_EVENTS)).thenReturn(builder);
		when(target.getUri()).thenReturn(URI.create("http://broker.test/ddsr/rest/events"));
		return target;
	}

	@Test
	@DisplayName("204 on the first connect ends the stream: no establish, no reconnect")
	void noContentStops() throws Exception {
		AtomicInteger connects = new AtomicInteger();
		Recording handler = new Recording();

		try (AutoCloseable stream = new RestEventSource().open(answering(connects, 204), handler)) {
			assertThat(handler.ended.await(5, TimeUnit.SECONDS)).as("the subscriber is told").isTrue();
			Thread.sleep(200);
		}

		assertThat(connects.get()).as("asked once, and not again").isEqualTo(1);
		assertThat(handler.established.get()).as("an empty answer is not a stream").isZero();
	}

	@Test
	@DisplayName("204 on a reconnect ends a stream that was established before")
	void noContentOnReconnectStops() throws Exception {
		AtomicInteger connects = new AtomicInteger();
		Recording handler = new Recording();

		try (AutoCloseable stream = new RestEventSource().open(answering(connects, 200, 204), handler)) {
			assertThat(handler.ended.await(5, TimeUnit.SECONDS)).isTrue();
			Thread.sleep(200);
		}

		assertThat(connects.get()).isEqualTo(2);
		assertThat(handler.established.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("any other failure is reconnected to, as FR-Sync-Reconnect wants")
	void otherFailuresReconnect() throws Exception {
		AtomicInteger connects = new AtomicInteger();
		Recording handler = new Recording();

		try (AutoCloseable stream = new RestEventSource().open(answering(connects, 503, 404, 204), handler)) {
			assertThat(handler.ended.await(5, TimeUnit.SECONDS)).isTrue();
		}

		assertThat(connects.get()).as("503 and 404 were retried, 204 was not").isEqualTo(3);
		assertThat(handler.established.get()).isZero();
	}
}
