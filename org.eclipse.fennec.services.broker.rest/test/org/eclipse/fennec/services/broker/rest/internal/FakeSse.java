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
package org.eclipse.fennec.services.broker.rest.internal;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

/** Minimal in-memory {@link Sse} plus a recording {@link SseEventSink}. */
final class FakeSse implements Sse {

	@Override
	public OutboundSseEvent.Builder newEventBuilder() {
		return new Builder();
	}

	@Override
	public SseBroadcaster newBroadcaster() {
		throw new UnsupportedOperationException("the bridge does not broadcast");
	}

	/** What a subscriber would see on the wire. */
	record Frame(String name, String comment, MediaType mediaType, Object data) implements OutboundSseEvent {
		@Override public Class<?> getType() { return data == null ? null : data.getClass(); }
		@Override public Type getGenericType() { return getType(); }
		@Override public MediaType getMediaType() { return mediaType; }
		@Override public Object getData() { return data; }
		@Override public String getId() { return null; }
		@Override public String getName() { return name; }
		@Override public String getComment() { return comment; }
		@Override public long getReconnectDelay() { return -1; }
		@Override public boolean isReconnectDelaySet() { return false; }
	}

	static final class Builder implements OutboundSseEvent.Builder {
		private String name;
		private String comment;
		private MediaType mediaType;
		private Object data;

		@Override public OutboundSseEvent.Builder id(String id) { return this; }
		@Override public OutboundSseEvent.Builder name(String n) { this.name = n; return this; }
		@Override public OutboundSseEvent.Builder reconnectDelay(long milliseconds) { return this; }
		@Override public OutboundSseEvent.Builder mediaType(MediaType type) { this.mediaType = type; return this; }
		@Override public OutboundSseEvent.Builder comment(String c) { this.comment = c; return this; }
		@Override public OutboundSseEvent.Builder data(@SuppressWarnings("rawtypes") Class type, Object d) { this.data = d; return this; }
		@Override public OutboundSseEvent.Builder data(@SuppressWarnings("rawtypes") GenericType type, Object d) { this.data = d; return this; }
		@Override public OutboundSseEvent.Builder data(Object d) { this.data = d; return this; }
		@Override public OutboundSseEvent build() { return new Frame(name, comment, mediaType, data); }
	}

	static final class Sink implements SseEventSink {
		final List<OutboundSseEvent> sent = new ArrayList<>();
		boolean closed;
		boolean failing;

		@Override
		public boolean isClosed() {
			return closed;
		}

		@Override
		public CompletionStage<?> send(OutboundSseEvent event) {
			if (failing) {
				throw new IllegalStateException("connection reset by peer");
			}
			sent.add(event);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public void close() {
			closed = true;
		}

		List<String> payloads() {
			return sent.stream().map(e -> String.valueOf(e.getData())).toList();
		}
	}
}
