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

import org.eclipse.fennec.services.MessagingFlavor;
import org.eclipse.fennec.services.MessagingOperationFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.cloudevents.CloudEventException;
import org.eclipse.fennec.services.cloudevents.CloudEvents;

import io.cloudevents.model.ce.CloudEvent;

/**
 * Where a call travels over a messaging broker — a
 * {@link MessagingFlavor}, read once.
 *
 * <p>Every operation placed: the channel it is reached on, its QoS,
 * what it is encoded in, and where its answer goes. Everything behind
 * this class — the invoker, the dispatcher, the distribution — works on
 * these and never looks at the protocol again. The rules that produce
 * them are {@link MessagingFlavors}.
 *
 * <p>Where an answer goes is the flavor's statement, per operation:
 * <ul>
 * <li>it names a place ({@code replyChannel}): the consumer listens on
 * a subtree of its own name below it and names the exact channel in
 * each call — a place a broker's ACL can be written against;</li>
 * <li>it names none: the consumer's connection provides a reply channel
 * only it reads.</li>
 * </ul>
 * The provider side does not care which: every call says where it wants
 * its answer.
 */
public final class MessagingRoute {

	/**
	 * One operation of the flavor, placed.
	 *
	 * @param replyBase where answers to this operation go, or null when
	 *                  the flavor names no place for them
	 */
	public record Operation(MessagingOperationFlavor flavor, ServiceOperation operation, String channel,
			int qos, String consumes, String produces, String replyBase) {

		/** The operation's name, or the flavor's when it binds none. */
		public String name() {
			return operation != null && operation.getName() != null ? operation.getName() : flavor.getName();
		}

		/** Whether the flavor says where answers to this operation go. */
		public boolean namesReplyChannel() {
			return replyBase != null;
		}
	}

	private final MessagingFlavor flavor;

	private final List<Operation> operations = new ArrayList<>();

	private MessagingRoute(MessagingFlavor flavor) {
		this.flavor = flavor;
		for (ServiceOperationFlavor candidate : flavor.getOperationFlavors()) {
			if (candidate instanceof MessagingOperationFlavor operation) {
				operations.add(new Operation(operation, operation.getOperation(),
						MessagingFlavors.channel(flavor, operation), MessagingFlavors.qos(flavor, operation),
						MessagingFlavors.consumes(operation), MessagingFlavors.produces(operation),
						MessagingFlavors.replyBase(flavor, operation)));
			}
		}
	}

	/** Whether a flavor travels over a messaging broker at all. */
	public static boolean speaks(ServiceFlavor flavor) {
		return flavor instanceof MessagingFlavor;
	}

	/**
	 * The route a flavor describes.
	 *
	 * @throws IllegalArgumentException for a flavor that is no messaging
	 *                                  flavor, or whose operations cannot
	 *                                  be placed
	 */
	public static MessagingRoute of(ServiceFlavor flavor) {
		if (!(flavor instanceof MessagingFlavor messaging)) {
			throw new IllegalArgumentException((flavor == null ? "no flavor" : flavor.eClass().getName())
					+ " does not travel over a messaging broker");
		}
		return new MessagingRoute(messaging);
	}

	/**
	 * The route to an implementation: its first messaging flavor, in the
	 * order it announces them, or null when it has none.
	 */
	public static MessagingRoute of(ServiceImplementation implementation) {
		if (implementation != null) {
			for (ServiceFlavor candidate : implementation.getFlavors()) {
				if (candidate instanceof MessagingFlavor messaging) {
					return new MessagingRoute(messaging);
				}
			}
		}
		return null;
	}

	/** The flavor this route was read from. */
	public MessagingFlavor flavor() {
		return flavor;
	}

	/** What the flavor's protocol is called where people read it: {@code MQTT}, {@code AMQP}. */
	public String protocol() {
		return MessagingFlavors.isAmqp(flavor) ? "AMQP" : "MQTT";
	}

	/** The broker a call is sent to — the first the flavor announces — or null. */
	public String broker() {
		return flavor.getBrokers().isEmpty() ? null : flavor.getBrokers().get(0);
	}

	/** The exchange calls are published to, where the protocol has exchanges; else null. */
	public String exchange() {
		return MessagingFlavors.exchange(flavor);
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

	/**
	 * The subtree a consumer listens on for the answers to one
	 * operation.
	 *
	 * @throws IllegalStateException for an operation whose flavor names
	 *                               no place for answers
	 */
	public String replySubtree(Operation operation, String consumer) {
		return MessagingFlavors.replySubtree(flavor, replyBaseOf(operation), consumer);
	}

	/** Where one session's messages from the provider arrive, for a flavor that names a reply tree. */
	public String sessionReplyChannel(Operation operation, String consumer, String sessionId) {
		return MessagingFlavors.sessionReplyChannel(flavor, replyBaseOf(operation), consumer, sessionId);
	}

	/** Where the provider listens for the messages of the sessions opened on an operation. */
	public String sessionChannelPrefix(Operation operation) {
		return MessagingFlavors.sessionChannelPrefix(flavor, operation.channel());
	}

	/** The filter a consumer subscribes to cover that subtree. */
	public String replyFilter(Operation operation, String consumer) {
		return MessagingFlavors.subtreeFilter(flavor, replySubtree(operation, consumer));
	}

	/** The channel one call's answer goes to. */
	public String replyChannel(Operation operation, String consumer, String requestId) {
		return MessagingFlavors.replyChannel(flavor, replyBaseOf(operation), consumer, requestId);
	}

	private static String replyBaseOf(Operation operation) {
		if (operation.replyBase() == null) {
			throw new IllegalStateException("the flavor names no place for answers to " + operation.name()
					+ " — the connection provides one");
		}
		return operation.replyBase();
	}

	// ---------------------------------------------------------- messages

	/** What a call is written in when the contract does not say. */
	public static final String DEFAULT_CONTENT_TYPE = MessagingFlavors.DEFAULT_CONTENT_TYPE;

	/**
	 * The envelope of a call: a CloudEvent, the same on every protocol.
	 *
	 * @param replyTo the channel the answer goes to, when the flavor
	 *                names a place for answers; null where the
	 *                connection provides one
	 */
	public static CloudEvent request(String source, String subject, String contentType, String replyTo) {
		CloudEvent envelope = CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE, source, contentType);
		envelope.setSubject(subject);
		if (replyTo != null) {
			envelope.getExtensions().put(CloudEvents.EXTENSION_REPLY_TO, replyTo);
		}
		return envelope;
	}

	/**
	 * The envelope of the answer to a call. Its subject is the call's:
	 * an answer says what was asked.
	 */
	public static CloudEvent reply(CloudEvent request, String source, String contentType) {
		CloudEvent envelope = CloudEvents.replyTo(request, source, contentType);
		if (request.getSubject() != null) {
			envelope.setSubject(request.getSubject());
		}
		return envelope;
	}

	/** The reply address a call's envelope names; refused when it names none. */
	public static String replyAddressOf(CloudEvent request) {
		String replyTo = request.getExtensions().get(CloudEvents.EXTENSION_REPLY_TO);
		if (replyTo == null || replyTo.isBlank()) {
			throw new CloudEventException("a call without a reply address cannot be answered");
		}
		return replyTo;
	}

	/** The call an answer's envelope names, or null. */
	public static String correlationOf(CloudEvent reply) {
		return reply.getExtensions().get(CloudEvents.EXTENSION_CORRELATION_ID);
	}
}
