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

package org.eclipse.fennec.services.flavor.mqtt;

import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEventException;
import org.eclipse.fennec.services.cloudevents.CloudEvents;

import io.cloudevents.model.ce.CloudEvent;

/**
 * What a call looks like as a message: a CloudEvent in structured mode,
 * with the invocation document as its payload.
 *
 * <p>MQTT 3.1.1 has neither a response topic nor correlation data —
 * they arrive with MQTT 5, which the Paho v3 client does not speak — so
 * the reply address and the correlation travel as extension attributes.
 * That is the specification's own answer to a transport that cannot
 * carry them, and it is why the model says a call carries neither
 * itself.
 *
 * <p>The payload is bytes here. Which encoding it is in is the
 * contract's choice (#100) and the caller's to apply; this class only
 * says which encoding was used, in {@code datacontenttype}.
 */
public final class MqttMessages {

	private MqttMessages() {
	}

	/** The envelope of a call: where the answer goes, and what is called. */
	public static CloudEvent request(String source, String subject, String replyTo, String contentType) {
		if (replyTo == null || replyTo.isBlank()) {
			throw new IllegalArgumentException("a call needs the topic its answer goes to");
		}
		CloudEvent envelope = CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE, source, contentType);
		envelope.setSubject(subject);
		envelope.getExtensions().put(CloudEvents.EXTENSION_REPLY_TO, replyTo);
		return envelope;
	}

	/** The envelope of an answer: a second event, naming the call it answers. */
	public static CloudEvent reply(CloudEvent request, String source, String contentType) {
		CloudEvent envelope = CloudEvents.replyTo(request, source, contentType);
		if (request.getSubject() != null) {
			envelope.setSubject(request.getSubject());
		}
		return envelope;
	}

	/** One message: envelope and payload in a single JSON document. */
	public static byte[] write(CloudEvent envelope, byte[] document) {
		return CloudEventCodec.writeStructured(envelope, document);
	}

	/**
	 * The message a payload carries, refusing anything that is not a
	 * call or an answer of ours — a shared broker carries other
	 * people's messages, and reading one as a call would be worse than
	 * ignoring it.
	 *
	 * @param expectedType {@link CloudEvents#TYPE_INVOKE} or
	 *                     {@link CloudEvents#TYPE_INVOKE_REPLY}
	 */
	public static CloudEventCodec.Message read(byte[] payload, String expectedType) {
		CloudEventCodec.Message message = CloudEventCodec.readStructured(payload);
		if (!expectedType.equals(message.attributes().getType())) {
			throw new CloudEventException("expected " + expectedType + ", got "
					+ message.attributes().getType());
		}
		if (message.data() == null) {
			throw new CloudEventException("a " + expectedType + " without its document says nothing");
		}
		return message;
	}

	/** Where the answer to this call is expected. */
	public static String replyTopicOf(CloudEvent request) {
		String replyTo = request.getExtensions().get(CloudEvents.EXTENSION_REPLY_TO);
		if (replyTo == null || replyTo.isBlank()) {
			throw new CloudEventException("a call without a reply address cannot be answered");
		}
		return replyTo;
	}

	/** Which call an answer answers. */
	public static String correlationOf(CloudEvent reply) {
		return reply.getExtensions().get(CloudEvents.EXTENSION_CORRELATION_ID);
	}
}
