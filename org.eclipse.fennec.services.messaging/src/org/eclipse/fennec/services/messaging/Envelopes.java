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

package org.eclipse.fennec.services.messaging;

import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.gecko.messaging.api.Message;
import org.gecko.messaging.api.MessageContext;
import org.gecko.messaging.api.MessageContextBuilder;
import org.gecko.messaging.api.MessagingConstants;
import org.gecko.messaging.api.cloudevents.CloudEventMessageContext;
import org.gecko.messaging.api.cloudevents.CloudEventMessageContextBuilder;

import io.cloudevents.model.ce.CloudEvent;
import io.cloudevents.model.ce.CloudEventsFactory;

/**
 * Between the envelope this project hands around — the CloudEvents
 * model and the payload beside it — and a message of the messaging
 * instance, which writes and reads the envelope on the wire itself.
 *
 * <p>Two attributes change sides on the way. {@code replyto} and
 * {@code correlationid} are extensions in the model, because CloudEvents
 * defines neither; to the instance they are the reply channel and the
 * correlation of the message, which it puts where the protocol has a
 * place for them and into the envelope where it has not. Everything
 * else is an attribute on both sides, and every other extension a
 * header.
 */
final class Envelopes {

	private Envelopes() {
	}

	/**
	 * A message of the instance from an envelope and its payload.
	 *
	 * @param builder     a fresh builder of the instance
	 * @param correlation the correlation the message has to carry, which
	 *                    wins over the envelope's; null to take the
	 *                    envelope's
	 */
	static Message toMessage(MessageContextBuilder builder, String channel, CloudEventCodec.Message carried,
			int qos, String correlation) {
		CloudEvent envelope = carried.attributes();
		CloudEventMessageContextBuilder attributes = builder.adapt(CloudEventMessageContextBuilder.class);
		attributes.id(envelope.getEventId()).source(envelope.getSource()).type(envelope.getType());
		if (envelope.getSubject() != null) {
			attributes.subject(envelope.getSubject());
		}
		if (envelope.getDataschema() != null) {
			attributes.dataSchema(envelope.getDataschema());
		}
		if (envelope.getTime() != null) {
			attributes.time(OffsetDateTime.ofInstant(envelope.getTime().toInstant(), ZoneOffset.UTC));
		}
		builder.channel(channel).extensionEntry(MessagingConstants.EXTENSION_QOS, qos);
		if (envelope.getDatacontenttype() != null && !envelope.getDatacontenttype().isBlank()) {
			builder.contentType(envelope.getDatacontenttype());
		}
		String correlates = correlation;
		for (Map.Entry<String, String> extension : envelope.getExtensions()) {
			String name = extension.getKey();
			String value = extension.getValue();
			if (name == null || value == null) {
				continue;
			}
			if (CloudEvents.EXTENSION_REPLY_TO.equals(name)) {
				builder.replyTo(value);
			} else if (CloudEvents.EXTENSION_CORRELATION_ID.equals(name)) {
				if (correlates == null) {
					correlates = value;
				}
			} else {
				builder.header(name, value);
			}
		}
		if (correlates != null) {
			builder.correlationId(correlates);
		}
		byte[] data = carried.data();
		return builder.content(ByteBuffer.wrap(data == null ? new byte[0] : data)).buildMessage();
	}

	/**
	 * The envelope and payload a received message carries, or null for
	 * a message that is no CloudEvent — which an instance in structured
	 * mode does not deliver in the first place.
	 */
	static CloudEventCodec.Message fromMessage(Message message) {
		MessageContext context = message.getContext();
		if (!(context instanceof CloudEventMessageContext carrying) || carrying.getCloudEvent() == null) {
			return null;
		}
		var received = carrying.getCloudEvent();
		CloudEvent envelope = CloudEventsFactory.eINSTANCE.createCloudEvent();
		envelope.setSpecVersion(received.getSpecVersion());
		envelope.setEventId(received.getId());
		envelope.setSource(received.getSource());
		envelope.setType(received.getType());
		envelope.setSubject(received.getSubject());
		envelope.setDataschema(received.getDataSchema());
		if (received.getTime() != null) {
			envelope.setTime(Date.from(received.getTime().toInstant()));
		}
		if (context.getContentType() != null) {
			envelope.setDatacontenttype(context.getContentType());
		}
		for (Map.Entry<String, Object> extension : received.getExtensions().entrySet()) {
			if (extension.getValue() != null) {
				envelope.getExtensions().put(extension.getKey(), String.valueOf(extension.getValue()));
			}
		}
		if (context.getReplyToChannel() != null) {
			envelope.getExtensions().put(CloudEvents.EXTENSION_REPLY_TO, context.getReplyToChannel());
		}
		if (context.getCorrelationId() != null) {
			envelope.getExtensions().put(CloudEvents.EXTENSION_CORRELATION_ID, context.getCorrelationId());
		}
		ByteBuffer content = message.payload();
		byte[] data = null;
		if (content.hasRemaining()) {
			data = new byte[content.remaining()];
			content.get(data);
		}
		return new CloudEventCodec.Message(envelope, data);
	}
}
