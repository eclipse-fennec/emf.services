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

package org.eclipse.fennec.services.flavor.messaging;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.fennec.services.AmqpFlavor;
import org.eclipse.fennec.services.AmqpOperationFlavor;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttOperationFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.eclipse.fennec.services.flavor.amqp.AmqpFlavors;
import org.eclipse.fennec.services.flavor.mqtt.MqttFlavors;
import org.eclipse.fennec.services.flavor.mqtt.MqttMessages;

import io.cloudevents.model.ce.CloudEvent;

/**
 * Where a call travels over a messaging broker — read from a flavor,
 * whichever messaging flavor it is.
 *
 * <p>Over messaging the API is one: publish to a channel, subscribe to
 * a channel, a request and its answer. What a protocol changes is what
 * a channel is called, where the broker is and how an answer finds its
 * way back — and those are exactly the facts a flavor states. This
 * class reads them into one shape, so that everything behind it is
 * written once. The rules themselves stay in the flavor's own bundle
 * ({@code MqttFlavors}, {@code AmqpFlavors}); nothing here decides a
 * topic or a routing key.
 *
 * <p>Two ways back exist, and the route says which applies:
 * <ul>
 * <li><b>in the envelope</b> (MQTT 3.1.1): the protocol carries no
 * reply address, so the call names a topic in its envelope and the
 * consumer listens on a subtree of its own name;</li>
 * <li><b>the protocol's own</b> (AMQP): reply-to and correlation id are
 * message properties, and the connection handles both.</li>
 * </ul>
 */
public final class MessagingRoute {

	/** One operation of the flavor, placed. */
	public record Operation(ServiceOperationFlavor flavor, ServiceOperation operation, String channel,
			int qos, String consumes, String produces) {

		/** The operation's name, or the flavor's when it binds none. */
		public String name() {
			return operation != null && operation.getName() != null ? operation.getName() : flavor.getName();
		}
	}

	private final ServiceFlavor flavor;

	private final List<Operation> operations = new ArrayList<>();

	private MessagingRoute(ServiceFlavor flavor) {
		this.flavor = flavor;
		for (ServiceOperationFlavor candidate : flavor.getOperationFlavors()) {
			if (flavor instanceof MqttFlavor mqtt && candidate instanceof MqttOperationFlavor operation) {
				operations.add(new Operation(operation, operation.getOperation(),
						MqttFlavors.requestTopic(mqtt, operation), MqttFlavors.qos(mqtt, operation),
						MqttFlavors.consumes(operation), MqttFlavors.produces(operation)));
			} else if (flavor instanceof AmqpFlavor amqp && candidate instanceof AmqpOperationFlavor operation) {
				operations.add(new Operation(operation, operation.getOperation(),
						AmqpFlavors.routingKey(amqp, operation), AmqpFlavors.QOS,
						AmqpFlavors.consumes(operation), AmqpFlavors.produces(operation)));
			}
		}
	}

	/** Whether a flavor travels over a messaging broker at all. */
	public static boolean speaks(ServiceFlavor flavor) {
		return flavor instanceof MqttFlavor || flavor instanceof AmqpFlavor;
	}

	/**
	 * The route a flavor describes.
	 *
	 * @throws IllegalArgumentException for a flavor that is no messaging
	 *                                  flavor, or whose operations cannot
	 *                                  be placed
	 */
	public static MessagingRoute of(ServiceFlavor flavor) {
		if (!speaks(flavor)) {
			throw new IllegalArgumentException((flavor == null ? "no flavor" : flavor.eClass().getName())
					+ " does not travel over a messaging broker");
		}
		return new MessagingRoute(flavor);
	}

	/**
	 * The route to an implementation: its first messaging flavor, in the
	 * order it announces them, or null when it has none.
	 */
	public static MessagingRoute of(ServiceImplementation implementation) {
		if (implementation != null) {
			for (ServiceFlavor candidate : implementation.getFlavors()) {
				if (speaks(candidate)) {
					return new MessagingRoute(candidate);
				}
			}
		}
		return null;
	}

	/** The flavor this route was read from. */
	public ServiceFlavor flavor() {
		return flavor;
	}

	/** What the flavor is called where people read it: {@code MQTT}, {@code AMQP}. */
	public String protocol() {
		return flavor instanceof AmqpFlavor ? "AMQP" : "MQTT";
	}

	/** The broker a call is sent to — the first the flavor announces — or null. */
	public String broker() {
		List<String> brokers = flavor instanceof AmqpFlavor amqp ? amqp.getBrokers()
				: ((MqttFlavor) flavor).getBrokers();
		return brokers.isEmpty() ? null : brokers.get(0);
	}

	/** The exchange calls are published to, where the protocol has exchanges; else null. */
	public String exchange() {
		return flavor instanceof AmqpFlavor amqp ? AmqpFlavors.exchange(amqp) : null;
	}

	/**
	 * Whether the protocol carries the reply address and the correlation
	 * itself. Where it does not, both travel in the envelope.
	 */
	public boolean repliesNatively() {
		return flavor instanceof AmqpFlavor;
	}

	/** Every operation of the flavor, placed, in the flavor's order. */
	public List<Operation> operations() {
		return List.copyOf(operations);
	}

	/**
	 * One operation, by the flavor's own name first and by the operation
	 * it binds second; null when the flavor has none of that name.
	 */
	public Operation operation(String name) {
		for (Operation candidate : operations) {
			if (name.equals(candidate.flavor().getName())) {
				return candidate;
			}
		}
		for (Operation candidate : operations) {
			if (candidate.operation() != null && name.equals(candidate.operation().getName())) {
				return candidate;
			}
		}
		return null;
	}

	/** The operation that answers on a channel, or null. */
	public Operation onChannel(String channel) {
		for (Operation candidate : operations) {
			if (candidate.channel().equals(channel)) {
				return candidate;
			}
		}
		return null;
	}

	/** A name as one level of a channel of this protocol. */
	public String segment(String name) {
		return flavor instanceof AmqpFlavor ? AmqpFlavors.segment(name) : MqttFlavors.topicSegment(name);
	}

	/**
	 * Where a consumer listens for its answers, when the way back is in
	 * the envelope: a subtree of its own name, which is what a broker's
	 * ACL can be written against.
	 *
	 * @throws IllegalStateException on a route that replies natively
	 */
	public String replySubtree(Operation operation, String consumer) {
		return MqttFlavors.replySubtree(envelopeFlavor(), (MqttOperationFlavor) operation.flavor(), consumer);
	}

	/** The topic one call's answer goes to, when the way back is in the envelope. */
	public String replyTopic(Operation operation, String consumer, String requestId) {
		return MqttFlavors.replyTopic(envelopeFlavor(), (MqttOperationFlavor) operation.flavor(), consumer,
				requestId);
	}

	private MqttFlavor envelopeFlavor() {
		if (flavor instanceof MqttFlavor mqtt) {
			return mqtt;
		}
		throw new IllegalStateException("over " + protocol() + " the reply address is the protocol's own");
	}

	// ---------------------------------------------------------- messages

	/** What a call is written in when the contract does not say. */
	public static final String DEFAULT_CONTENT_TYPE = MqttFlavors.DEFAULT_CONTENT_TYPE;

	/**
	 * The envelope of a call: a CloudEvent, the same on every protocol.
	 *
	 * @param replyTo the topic the answer goes to, when the way back is
	 *                in the envelope; null where the protocol carries it
	 */
	public static CloudEvent request(String source, String subject, String contentType, String replyTo) {
		CloudEvent envelope = CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE, source, contentType);
		envelope.setSubject(subject);
		if (replyTo != null) {
			envelope.getExtensions().put(CloudEvents.EXTENSION_REPLY_TO, replyTo);
		}
		return envelope;
	}

	/** The envelope of the answer to a call. */
	public static CloudEvent reply(CloudEvent request, String source, String contentType) {
		return MqttMessages.reply(request, source, contentType);
	}

	/** One message: the envelope and the document it carries. */
	public static byte[] write(CloudEvent envelope, byte[] document) {
		return MqttMessages.write(envelope, document);
	}

	/** Read a message of the expected kind; anything else is refused. */
	public static CloudEventCodec.Message read(byte[] payload, String expectedType) {
		return MqttMessages.read(payload, expectedType);
	}

	/** The reply address a call's envelope names; refused when it names none. */
	public static String replyAddressOf(CloudEvent request) {
		return MqttMessages.replyTopicOf(request);
	}

	/** The call an answer's envelope names, or null. */
	public static String correlationOf(CloudEvent reply) {
		return MqttMessages.correlationOf(reply);
	}
}
