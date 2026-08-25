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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

/**
 * The REST flavor's {@link EventSource}: subscribes to the broker's
 * {@code /events} stream over HTTP-SSE and hands decoded
 * {@link ServiceEvent}s to the SDK.
 * <p>
 * One implementation of a transport-agnostic interface — an MQTT source
 * would sit beside this class and change nothing in
 * {@code client.java}.
 */
@Component(service = EventSource.class, configurationPid = "org.eclipse.fennec.services.client.rest.events",
		immediate = true)
@Designate(ocd = RestEventSource.Config.class)
public final class RestEventSource implements EventSource {

	private static final Logger LOG = Logger.getLogger(RestEventSource.class.getName());

	@ObjectClassDefinition(name = "DDSR REST Event Source",
			description = "Subscription to the broker's SSE event stream")
	public @interface Config {

		@AttributeDefinition(
				name = "Supported flavors",
				description = "Comma-separated flavor kinds this consumer can speak. Narrows the stream "
						+ "server-side (FR-Sync-Filtering). Empty means no filter.")
		String flavors() default "REST";

		@AttributeDefinition(
				name = "Reconnect delay (seconds)",
				description = "How long the client waits before retrying a dropped stream.")
		int reconnect_seconds() default 3;
	}

	@Reference
	private RestTransport transport;

	@Reference
	private ComponentServiceObjects<ResourceSet> rsObjects;

	private String flavors;

	private int reconnectSeconds;

	@Activate
	void activate(Config config) {
		this.flavors = config.flavors();
		this.reconnectSeconds = config.reconnect_seconds();
		LOG.info("[DDSR-Client] RestEventSource activated, flavors=" + flavors);
	}

	@Override
	public AutoCloseable open(Handler handler) {
		WebTarget target = transport.target().path("events");
		if (flavors != null && !flavors.isBlank()) {
			target = target.queryParam("flavors", flavors);
		}
		StreamReader reader = new StreamReader(target, handler);
		reader.start();
		return reader;
	}

	/**
	 * Reads the stream with a plain JAX-RS client and parses SSE itself.
	 * <p>
	 * The obvious route, {@code jakarta.ws.rs.sse.SseEventSource}, does not
	 * work here: it finds its implementation through a ServiceLoader
	 * lookup, and {@code jakarta.ws.rs-api} declares no
	 * {@code osgi.serviceloader} requirement, so SPI-Fly never weaves it
	 * and the lookup fails with "Provider for
	 * jakarta.ws.rs.sse.SseEventSource.Builder cannot be found" — even
	 * though jersey-media-sse is in the runtime. The server side is
	 * unaffected because the whiteboard wires SSE explicitly.
	 * <p>
	 * Parsing it ourselves costs little — SSE is a line protocol — removes
	 * that fragility, and buys precise reconnect semantics: we know
	 * exactly when a connection was (re-)established and can say so,
	 * which FR-Sync-Reconnect needs in order to trigger a fresh snapshot.
	 */
	private final class StreamReader implements AutoCloseable, Runnable {

		private final WebTarget target;

		private final Handler handler;

		private final AtomicBoolean running = new AtomicBoolean(true);

		private volatile Thread thread;

		private volatile InputStream open;

		StreamReader(WebTarget target, Handler handler) {
			this.target = target;
			this.handler = handler;
		}

		void start() {
			thread = new Thread(this, "ddsr-event-stream");
			thread.setDaemon(true);
			thread.start();
		}

		@Override
		public void run() {
			while (running.get()) {
				try (InputStream in = target.request(MediaType.SERVER_SENT_EVENTS).get(InputStream.class)) {
					this.open = in;
					LOG.info("[DDSR-Client] subscribed to " + target.getUri());
					handler.onStreamEstablished();
					pump(in);
					// Reaching here means end of stream without an error —
					// the server closed it. Worth saying, because otherwise
					// a reconnect looks like nothing happened at all.
					if (running.get()) {
						LOG.info("[DDSR-Client] event stream closed by the broker, reconnecting in "
								+ reconnectSeconds + "s");
					}
				} catch (Exception failure) {
					if (running.get()) {
						LOG.warning("[DDSR-Client] event stream dropped, retrying in "
								+ reconnectSeconds + "s: " + failure);
					}
				}
				if (!running.get()) {
					return;
				}
				try {
					TimeUnit.SECONDS.sleep(reconnectSeconds);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}

		/**
		 * Accumulates {@code data:} lines until a blank line ends the
		 * event, then hands the joined payload on. Other SSE fields
		 * ({@code event:}, {@code id:}, comments) are read and ignored —
		 * the payload carries the type, so nothing depends on them.
		 */
		private void pump(InputStream in) throws IOException {
			BufferedReader lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			StringBuilder data = new StringBuilder();
			String line;
			while (running.get() && (line = lines.readLine()) != null) {
				if (line.isEmpty()) {
					if (data.length() > 0) {
						deliver(handler, data.toString());
						data.setLength(0);
					}
					continue;
				}
				if (line.startsWith("data:")) {
					String value = line.substring("data:".length());
					// One optional leading space belongs to the framing.
					if (value.startsWith(" ")) {
						value = value.substring(1);
					}
					if (data.length() > 0) {
						data.append('\n');
					}
					data.append(value);
				}
			}
		}

		/**
		 * Never closes the stream on the calling thread: the JDK's
		 * ChunkedInputStream.close() drains the remaining body under the
		 * connection's read lock, which the pumping thread holds while
		 * blocked in read — a cross-thread close therefore hangs until
		 * the next byte arrives. During framework shutdown that pinned
		 * the FelixStartLevel thread for the whole waitForStop timeout
		 * and the provider's withdraw never ran (found by the FR-P4
		 * harness). The close is delegated to a bounded daemon thread;
		 * the broker's SSE heartbeat guarantees the reader wakes up and
		 * lets it through within one interval.
		 */
		@Override
		public void close() {
			running.set(false);
			Thread t = thread;
			if (t != null) {
				t.interrupt();
			}
			InputStream current = open;
			if (current != null) {
				Thread closer = new Thread(() -> {
					try {
						current.close();
					} catch (IOException ignored) {
						// Best effort; the reader loop exits on its own.
					}
				}, "ddsr-event-stream-closer");
				closer.setDaemon(true);
				closer.start();
				try {
					closer.join(2_000);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private void deliver(Handler handler, String payload) {
		if (payload == null || payload.isBlank()) {
			return;
		}
		try {
			// Multi-root document: the event plus the reference, provider
			// and interfaces it needs to be self-contained.
			XmiBundle bundle = XmiCodec.readBundle(
					new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)), rsObjects);
			for (EObject root : bundle.roots()) {
				if (root instanceof ServiceEvent serviceEvent) {
					handler.onEvent(serviceEvent);
					return;
				}
			}
			LOG.warning("[DDSR-Client] event payload carried no ServiceEvent, ignoring");
		} catch (Exception decodeFailure) {
			// A single undecodable frame must not tear down the stream.
			LOG.log(Level.WARNING, "[DDSR-Client] could not decode an event, skipping", decodeFailure);
		}
	}
}
