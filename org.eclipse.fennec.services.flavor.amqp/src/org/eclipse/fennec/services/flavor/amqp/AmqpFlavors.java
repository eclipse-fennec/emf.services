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

import java.util.List;

import org.eclipse.fennec.services.AmqpFlavor;
import org.eclipse.fennec.services.AmqpOperationFlavor;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceOperationFlavor;

/**
 * Where a call travels over AMQP: the rules both ends read a flavor by.
 *
 * <p>One rule, really. A call is published to the flavor's topic
 * exchange under a routing key, and the key is the operation flavor's
 * own or {@code <requestRoutingKey>.<operation>}. There is no rule for
 * the way back, and that is the difference to MQTT: the request names
 * its reply address in the protocol's reply-to property, the address is
 * a queue only the caller can read, and the answer is matched by the
 * protocol's correlation id. What MQTT needs a naming convention and an
 * ACL for, AMQP has a field for.
 */
public final class AmqpFlavors {

	private AmqpFlavors() {
	}

	/** What a call is written in when the contract does not say. */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";

	/** The topic exchange every AMQP 0-9-1 broker declares. */
	public static final String DEFAULT_EXCHANGE = "amq.topic";

	/**
	 * Calls are confirmed by the broker and acknowledged by the provider
	 * — the one delivery guarantee AMQP 0-9-1 has beyond fire and forget,
	 * and the one a call wants.
	 */
	public static final int QOS = 1;

	/** The exchange calls of this flavor are published to. */
	public static String exchange(AmqpFlavor flavor) {
		String exchange = flavor.getExchange();
		return exchange == null || exchange.isBlank() ? DEFAULT_EXCHANGE : exchange;
	}

	/**
	 * The routing key one operation is reached under.
	 *
	 * @throws IllegalArgumentException when neither the operation flavor
	 *                                  nor the flavor names one
	 */
	public static String routingKey(AmqpFlavor flavor, AmqpOperationFlavor operationFlavor) {
		if (operationFlavor.getRoutingKey() != null && !operationFlavor.getRoutingKey().isBlank()) {
			return operationFlavor.getRoutingKey();
		}
		String base = trimDots(flavor.getRequestRoutingKey());
		if (base.isEmpty()) {
			throw new IllegalArgumentException(
					"AmqpFlavor carries no requestRoutingKey and the operation flavor no routingKey either");
		}
		return base + "." + segment(operationName(operationFlavor));
	}

	/**
	 * A name as one segment of a routing key. The separator and the two
	 * wildcards of a topic exchange would change what a key means, and
	 * white space what it looks like.
	 */
	public static String segment(String name) {
		if (name == null || name.isBlank()) {
			return "anonymous";
		}
		return name.replaceAll("[.*#\\s]", "_");
	}

	/** What a call is encoded in: the contract's `consumes`, else XMI. */
	public static String consumes(ServiceOperationFlavor operationFlavor) {
		return first(operationFlavor == null ? null : operationFlavor.getConsumes());
	}

	/** What an answer is encoded in: the contract's `produces`, else XMI. */
	public static String produces(ServiceOperationFlavor operationFlavor) {
		return first(operationFlavor == null ? null : operationFlavor.getProduces());
	}

	/**
	 * The operation flavor for an operation, by the flavor's own name
	 * first and by the operation it binds second — the order the MQTT
	 * flavor looks in, so one contract reads the same on both.
	 */
	public static AmqpOperationFlavor operationFlavor(AmqpFlavor flavor, String operationName) {
		for (ServiceOperationFlavor candidate : flavor.getOperationFlavors()) {
			if (candidate instanceof AmqpOperationFlavor amqp && operationName.equals(amqp.getName())) {
				return amqp;
			}
		}
		for (ServiceOperationFlavor candidate : flavor.getOperationFlavors()) {
			if (candidate instanceof AmqpOperationFlavor amqp && amqp.getOperation() != null
					&& operationName.equals(amqp.getOperation().getName())) {
				return amqp;
			}
		}
		return null;
	}

	private static String operationName(AmqpOperationFlavor operationFlavor) {
		ServiceOperation operation = operationFlavor.getOperation();
		if (operation != null && operation.getName() != null) {
			return operation.getName();
		}
		return operationFlavor.getName() == null ? "" : operationFlavor.getName();
	}

	private static String first(List<String> declared) {
		return declared == null || declared.isEmpty() ? DEFAULT_CONTENT_TYPE : declared.get(0);
	}

	private static String trimDots(String key) {
		if (key == null) {
			return "";
		}
		String trimmed = key.strip();
		while (trimmed.endsWith(".")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}
}
