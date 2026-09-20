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

package org.eclipse.fennec.services.cloudevents;

import java.util.Date;
import java.util.UUID;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;

import io.cloudevents.model.ce.CloudEvent;
import io.cloudevents.model.ce.CloudEventsFactory;

/**
 * The names this registry puts into a CloudEvents envelope, and the two
 * builders that fill one.
 *
 * <p>Everything here is wire surface and therefore worth stating once
 * rather than per transport. The {@code type} values are new with #101
 * and deliberately carry the final package name: unlike the frozen
 * {@code ddsr.*} names of #4 they were never published, so there is
 * nothing to rename later.
 *
 * <p>Two attributes that a reader may expect and will not find:
 * CloudEvents defines no reply address and no correlation identifier,
 * because in most bindings the transport has them. MQTT 3.1.1 does not
 * (they arrive with MQTT 5, which the Paho v3 client does not speak),
 * so both travel as extension attributes — {@link #EXTENSION_REPLY_TO}
 * and {@link #EXTENSION_CORRELATION_ID}. The names are lower case
 * without separators because the specification requires that of every
 * extension name.
 */
public final class CloudEvents {

	private CloudEvents() {
	}

	/** The only spec version this registry writes or accepts. */
	public static final String SPEC_VERSION = "1.0";

	/** Media type of a structured-mode message, JSON event format. */
	public static final String STRUCTURED_MEDIA_TYPE = "application/cloudevents+json";

	/** Prefix of the binary-mode attribute headers over HTTP. */
	public static final String HEADER_PREFIX = "ce-";

	private static final String TYPE_PREFIX = "org.eclipse.fennec.services.";

	/** A call, as an event of its own (ACQUISITION.md §1, stage 3). */
	public static final String TYPE_INVOKE = TYPE_PREFIX + "invoke";

	/** The answer to a call, correlated by {@link #EXTENSION_CORRELATION_ID}. */
	public static final String TYPE_INVOKE_REPLY = TYPE_PREFIX + "invoke.reply";

	/** Everything a subscriber knows may be stale; take a fresh snapshot. */
	public static final String TYPE_RESYNC = TYPE_PREFIX + "resync";

	/** Extension carrying the id of the request an answer belongs to. */
	public static final String EXTENSION_CORRELATION_ID = "correlationid";

	/** Extension carrying the topic an answer is expected on. */
	public static final String EXTENSION_REPLY_TO = "replyto";

	/** The event type for one lifecycle transition. */
	public static String typeOf(ServiceEventType eventType) {
		return TYPE_PREFIX + switch (eventType) {
			case UNSPECIFIED -> "unspecified";
			case REGISTERED -> "registered";
			case MODIFIED -> "modified";
			case UNREGISTERING -> "unregistering";
			case MODIFIED_ENDMATCH -> "modified.endmatch";
			case UPGRADE_AVAILABLE -> "upgrade.available";
			case RETIRED -> "retired";
		};
	}

	/**
	 * The lifecycle transition a type names, or {@code null} when the
	 * type is not one of ours. A reader that gets {@code null} has an
	 * envelope it understands carrying something it does not — which is
	 * the normal case on a shared transport and not an error.
	 */
	public static ServiceEventType eventTypeOf(String type) {
		if (type == null || !type.startsWith(TYPE_PREFIX)) {
			return null;
		}
		return switch (type.substring(TYPE_PREFIX.length())) {
			case "unspecified" -> ServiceEventType.UNSPECIFIED;
			case "registered" -> ServiceEventType.REGISTERED;
			case "modified" -> ServiceEventType.MODIFIED;
			case "unregistering" -> ServiceEventType.UNREGISTERING;
			case "modified.endmatch" -> ServiceEventType.MODIFIED_ENDMATCH;
			case "upgrade.available" -> ServiceEventType.UPGRADE_AVAILABLE;
			case "retired" -> ServiceEventType.RETIRED;
			default -> null;
		};
	}

	/**
	 * The envelope for one lifecycle event.
	 *
	 * <p>The subject is the reference id, which is what a consumer
	 * matches on when the topic could not name the interface. The time
	 * is the event's own, not the moment of sending: the broker builds
	 * the event when the mutation happened and a transport may be
	 * behind.
	 *
	 * @param event           the lifecycle event being announced
	 * @param source          the broker, as a URI reference
	 * @param datacontenttype the encoding of the payload that travels
	 *                        with this envelope
	 */
	public static CloudEvent envelopeFor(ServiceEvent event, String source, String datacontenttype) {
		CloudEvent envelope = newEnvelope(typeOf(event.getType()), source, datacontenttype);
		if (event.getReference() != null) {
			envelope.setSubject(event.getReference().getId());
		}
		if (event.getTimestamp() != null) {
			envelope.setTime(event.getTimestamp());
		}
		return envelope;
	}

	/**
	 * An envelope with the four required attributes filled, a fresh id
	 * and the current time.
	 *
	 * @param type            the event type
	 * @param source          the context the event happened in, a URI reference
	 * @param datacontenttype the payload encoding, or {@code null} when
	 *                        there is no payload
	 */
	public static CloudEvent newEnvelope(String type, String source, String datacontenttype) {
		CloudEvent envelope = CloudEventsFactory.eINSTANCE.createCloudEvent();
		envelope.setSpecVersion(SPEC_VERSION);
		envelope.setEventId(UUID.randomUUID().toString());
		envelope.setType(type);
		envelope.setSource(source);
		envelope.setTime(new Date());
		if (datacontenttype != null && !datacontenttype.isBlank()) {
			envelope.setDatacontenttype(datacontenttype);
		}
		return envelope;
	}

	/**
	 * The envelope of an answer to {@code request}: a new event, of the
	 * reply type, pointing back at the request through the correlation
	 * extension.
	 *
	 * <p>It is a second event and not a mutation of the first, which is
	 * what position 3 of #101 decided to say out loud: a call is
	 * modelled as two events, and the pair is held together by an
	 * attribute rather than by a connection.
	 */
	public static CloudEvent replyTo(CloudEvent request, String source, String datacontenttype) {
		CloudEvent envelope = newEnvelope(TYPE_INVOKE_REPLY, source, datacontenttype);
		envelope.getExtensions().put(EXTENSION_CORRELATION_ID, request.getEventId());
		return envelope;
	}

	/**
	 * Convenience for the in-memory case: the payload as the envelope's
	 * own {@code data}. Only usable where the payload is a single
	 * EObject — an event document is multi-root, so the wire path hands
	 * the codec encoded bytes instead (see
	 * {@link CloudEventCodec#writeStructured}).
	 */
	public static CloudEvent withData(CloudEvent envelope, EObject data) {
		envelope.setData(data);
		return envelope;
	}
}
