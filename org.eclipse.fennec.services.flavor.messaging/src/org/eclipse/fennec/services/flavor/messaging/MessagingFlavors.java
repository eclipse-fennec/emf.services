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

import java.util.List;
import java.util.Locale;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.MessagingFlavor;
import org.eclipse.fennec.services.MessagingOperationFlavor;
import org.eclipse.fennec.services.MessagingQos;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceOperationFlavor;

/**
 * Where a call travels over a messaging broker: the rules both ends
 * read a {@link MessagingFlavor} by.
 *
 * <p>Shared by the consumer, which publishes a request, and by the
 * provider side, which answers it — the two must not develop their own
 * reading of the same flavor. The names these rules produce are the
 * frozen convention; the TypeScript track computes the same ones.
 *
 * <p>The flavor is one for every protocol. What the protocol decides is
 * how a channel is spelled, and that is all this class asks it:
 *
 * <table>
 * <caption>What a protocol changes</caption>
 * <tr><th></th><th>MQTT</th><th>AMQP 0-9-1</th></tr>
 * <tr><td>a channel is</td><td>a topic</td><td>a routing key on an
 * exchange</td></tr>
 * <tr><td>its levels are separated by</td><td>{@code /}</td>
 * <td>{@code .}</td></tr>
 * <tr><td>a subtree is subscribed as</td><td>{@code <base>/#}</td>
 * <td>{@code <base>.#}</td></tr>
 * <tr><td>the highest QoS is</td><td>2</td><td>1</td></tr>
 * </table>
 */
public final class MessagingFlavors {

	private MessagingFlavors() {
	}

	/** What a call is written in when the contract does not say. */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";

	/** The topic exchange every AMQP 0-9-1 broker declares. */
	public static final String DEFAULT_EXCHANGE = "amq.topic";

	/**
	 * Whether a flavor speaks AMQP: its kind says so, or — for a flavor
	 * that was decoded without one — the scheme of its first broker.
	 * Everything that is not AMQP is MQTT, which is what the flavor's
	 * kind defaults to among the two.
	 */
	public static boolean isAmqp(MessagingFlavor flavor) {
		if (flavor.getKind() == FlavorKind.AMQP) {
			return true;
		}
		if (flavor.getKind() == FlavorKind.MQTT || flavor.getBrokers().isEmpty()) {
			return false;
		}
		return flavor.getBrokers().get(0).toLowerCase(Locale.ROOT).startsWith("amqp");
	}

	/** What separates the levels of a channel on this flavor's protocol. */
	public static String separator(MessagingFlavor flavor) {
		return isAmqp(flavor) ? "." : "/";
	}

	/**
	 * The channel one operation is reached on: its own, or
	 * {@code <channel><separator><operation>}.
	 *
	 * @throws IllegalArgumentException when neither the operation flavor
	 *                                  nor the flavor names a channel
	 */
	public static String channel(MessagingFlavor flavor, MessagingOperationFlavor operationFlavor) {
		if (operationFlavor.getChannel() != null && !operationFlavor.getChannel().isBlank()) {
			return operationFlavor.getChannel();
		}
		String base = trim(flavor, flavor.getChannel());
		if (base.isEmpty()) {
			throw new IllegalArgumentException(
					"the MessagingFlavor carries no channel and the operation flavor none either");
		}
		String name = operationName(operationFlavor);
		// A routing key's segment must not carry the separator or a
		// wildcard; a topic level was always taken as it is.
		return base + separator(flavor) + (isAmqp(flavor) ? segment(flavor, name) : name);
	}

	/**
	 * Where answers to one operation go, or null when the flavor names
	 * no place for them.
	 *
	 * <p>Null is a statement and not a gap: the consumer's connection
	 * then provides a reply channel only it reads and names it in the
	 * call. A flavor that does name one asks the consumer to listen on a
	 * subtree of its own name below it — a place a broker's ACL can be
	 * written against.
	 */
	public static String replyBase(MessagingFlavor flavor, MessagingOperationFlavor operationFlavor) {
		if (operationFlavor.getReplyChannel() != null && !operationFlavor.getReplyChannel().isBlank()) {
			return trim(flavor, operationFlavor.getReplyChannel());
		}
		if (flavor.getReplyChannel() != null && !flavor.getReplyChannel().isBlank()) {
			return trim(flavor, flavor.getReplyChannel());
		}
		return null;
	}

	/**
	 * The subtree one consumer listens on for its answers:
	 * {@code <reply base><separator><consumer>}.
	 *
	 * <p>Under the consumer's name, not merely under an unguessable id:
	 * a broker can be told who may read what only if there is a name to
	 * write the rule against.
	 */
	public static String replySubtree(MessagingFlavor flavor, String replyBase, String consumer) {
		return replyBase + separator(flavor) + segment(flavor, consumer);
	}

	/**
	 * Where the provider listens for the messages of a session opened on
	 * an operation's channel: {@code <channel><separator>session}, each
	 * session one level below. Under the request channel, so the rule a
	 * broker's ACL has for the calls of a contract covers its sessions
	 * with one more level; the requester's side of a session is the
	 * reply channel the call names, in its subtree as for any answer.
	 */
	public static String sessionChannelPrefix(MessagingFlavor flavor, String channel) {
		return channel + separator(flavor) + "session";
	}

	/**
	 * The filter a consumer listens on for the answers to its calls: the
	 * one level below its subtree where a call answered once is answered.
	 * One level and not the whole subtree, so it does not overlap the
	 * channels of the consumer's sessions ({@link #sessionReplyChannel}),
	 * which the messaging library subscribes itself — a broker may deliver
	 * a message once for every subscription it matches.
	 */
	public static String subtreeFilter(MessagingFlavor flavor, String subtree) {
		return subtree + separator(flavor) + (isAmqp(flavor) ? "*" : "+");
	}

	/**
	 * Where the provider's messages in one session arrive, for a flavor
	 * that names a reply tree: {@code <consumer's subtree><separator>session<separator><session>},
	 * in the consumer's subtree as any answer and two levels below it, out
	 * of the reach of {@link #subtreeFilter}.
	 */
	public static String sessionReplyChannel(MessagingFlavor flavor, String replyBase, String consumer,
			String sessionId) {
		return replySubtree(flavor, replyBase, consumer) + separator(flavor) + "session" + separator(flavor)
				+ segment(flavor, sessionId);
	}

	/** The channel one call's answer goes to: the consumer's subtree, then the call. */
	public static String replyChannel(MessagingFlavor flavor, String replyBase, String consumer,
			String requestId) {
		return replySubtree(flavor, replyBase, consumer) + separator(flavor) + segment(flavor, requestId);
	}

	/**
	 * A name as one level of a channel. The separator and the wildcards
	 * of the protocol would change what the channel means, and white
	 * space what it looks like.
	 */
	public static String segment(MessagingFlavor flavor, String name) {
		if (name == null || name.isBlank()) {
			return "anonymous";
		}
		return name.replaceAll(isAmqp(flavor) ? "[.*#\\s]" : "[/+#\\s]", "_");
	}

	/**
	 * The QoS of one operation: its own, else the flavor's default, and
	 * never more than the protocol has.
	 */
	public static int qos(MessagingFlavor flavor, MessagingOperationFlavor operationFlavor) {
		int level;
		if (operationFlavor.isSetQos() && operationFlavor.getQos() != null) {
			level = level(operationFlavor.getQos());
		} else {
			level = flavor.getDefaultQos() != null ? level(flavor.getDefaultQos()) : 1;
		}
		return isAmqp(flavor) ? Math.min(level, 1) : level;
	}

	/** The exchange calls are published to, where the protocol has exchanges; else null. */
	public static String exchange(MessagingFlavor flavor) {
		if (!isAmqp(flavor)) {
			return null;
		}
		String exchange = flavor.getExchange();
		return exchange == null || exchange.isBlank() ? DEFAULT_EXCHANGE : exchange;
	}

	/** What a call is encoded in: the contract's `consumes`, else XMI. */
	public static String consumes(ServiceOperationFlavor operationFlavor) {
		return first(operationFlavor == null ? null : operationFlavor.getConsumes());
	}

	/** What an answer is encoded in: the contract's `produces`, else XMI. */
	public static String produces(ServiceOperationFlavor operationFlavor) {
		return first(operationFlavor == null ? null : operationFlavor.getProduces());
	}

	private static String operationName(MessagingOperationFlavor operationFlavor) {
		ServiceOperation operation = operationFlavor.getOperation();
		if (operation != null && operation.getName() != null) {
			return operation.getName();
		}
		return operationFlavor.getName() == null ? "" : operationFlavor.getName();
	}

	private static String first(List<String> declared) {
		return declared == null || declared.isEmpty() ? DEFAULT_CONTENT_TYPE : declared.get(0);
	}

	private static int level(MessagingQos qos) {
		return switch (qos) {
			case AT_MOST_ONCE -> 0;
			case AT_LEAST_ONCE -> 1;
			case EXACTLY_ONCE -> 2;
		};
	}

	/** A channel without a trailing separator, which is not a level. */
	private static String trim(MessagingFlavor flavor, String channel) {
		if (channel == null) {
			return "";
		}
		String separator = separator(flavor);
		String trimmed = channel.strip();
		while (trimmed.endsWith(separator)) {
			trimmed = trimmed.substring(0, trimmed.length() - separator.length());
		}
		return trimmed;
	}
}
