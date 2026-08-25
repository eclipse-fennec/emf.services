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

package org.eclipse.fennec.services.broker.rest.internal;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.fennec.services.FlavorKind;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.annotations.RequireJakartarsWhiteboard;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

/**
 * The broker's event stream (A1): {@code GET /events} with
 * {@code text/event-stream}.
 * <p>
 * Only the entry point — the subscriber list and the actual distribution
 * live in {@link SseEventBridge}, which is a singleton and registered as
 * the transport-agnostic {@code EventSink} the broker feeds. A JAX-RS
 * resource cannot hold that state itself because it is prototype-scoped.
 * <p>
 * {@code ?flavors=REST,MQTT} narrows the stream to implementations the
 * consumer can actually speak (REQUIREMENTS FR-Sync-Filtering). Omitted
 * means everything.
 * <p>
 * Per FR-Sync-Reconnect a client that lost the connection pulls a fresh
 * snapshot instead of replaying missed events, which is why this stream
 * carries no sequence numbers and the broker keeps no per-client
 * history.
 */
@RequireHttpWhiteboard
@RequireJakartarsWhiteboard
@JakartarsResource
@JakartarsName("ddsr-events")
@Component(service = EventsResource.class, scope = ServiceScope.PROTOTYPE)
@Path("/events")
public class EventsResource {

	private static final Logger LOG = Logger.getLogger(EventsResource.class.getName());

	@Reference
	private SseEventBridge bridge;

	@GET
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void subscribe(@Context SseEventSink sink, @Context Sse sse,
			@QueryParam("flavors") String flavors) {
		bridge.subscribe(sse, sink, parseFlavors(flavors));
	}

	/**
	 * Unknown flavor names are ignored rather than rejected: the stream
	 * is a long-lived connection, and refusing it over one unrecognised
	 * token would be a poor trade. An entirely unparseable value ends up
	 * as "no filter".
	 */
	private static Set<FlavorKind> parseFlavors(String raw) {
		Set<FlavorKind> kinds = EnumSet.noneOf(FlavorKind.class);
		if (raw == null || raw.isBlank()) {
			return kinds;
		}
		for (String token : raw.split(",")) {
			String name = token.trim().toUpperCase();
			if (name.isEmpty()) {
				continue;
			}
			try {
				kinds.add(FlavorKind.valueOf(name));
			} catch (IllegalArgumentException unknown) {
				LOG.warning("[DDSR] ignoring unknown flavor in /events?flavors=: " + token.trim());
			}
		}
		return kinds;
	}
}
