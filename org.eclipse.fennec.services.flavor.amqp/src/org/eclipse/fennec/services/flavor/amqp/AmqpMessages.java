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

package org.eclipse.fennec.services.flavor.amqp;

import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEventException;
import org.eclipse.fennec.services.cloudevents.CloudEvents;

import io.cloudevents.model.ce.CloudEvent;

/**
 * What a call and its answer look like on an AMQP wire.
 *
 * <p>The same pair of CloudEvents the MQTT flavor sends, in structured
 * mode, carrying the same documents — a consumer that can read one can
 * read the other. What is missing is the reply address: the envelope
 * has an extension for it because MQTT 3 has nowhere else to put it,
 * and AMQP has. Saying it twice would be two places to disagree.
 */
public final class AmqpMessages {

	private AmqpMessages() {
	}

	/** The envelope of a call. */
	public static CloudEvent request(String source, String subject, String contentType) {
		CloudEvent envelope = CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE, source, contentType);
		envelope.setSubject(subject);
		return envelope;
	}

	/** The envelope of the answer to a call. */
	public static CloudEvent reply(CloudEvent request, String source, String contentType) {
		CloudEvent envelope = CloudEvents.replyTo(request, source, contentType);
		if (request.getSubject() != null) {
			envelope.setSubject(request.getSubject());
		}
		return envelope;
	}

	/** One message: the envelope and the document it carries. */
	public static byte[] write(CloudEvent envelope, byte[] document) {
		return CloudEventCodec.writeStructured(envelope, document);
	}

	/**
	 * Read a message of the expected kind.
	 *
	 * @throws CloudEventException when it is something else, or says
	 *                             nothing
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
}
