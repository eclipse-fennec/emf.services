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

import java.util.List;

import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttOperationFlavor;
import org.eclipse.fennec.services.MqttQos;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceOperationFlavor;

/**
 * Where a call travels on an MQTT wire: the topic and QoS rules of the
 * frozen convention, read off the flavor pair.
 *
 * <p>Shared by both ends on purpose. The consumer publishes where this
 * says and the provider listens where this says; two copies of the rule
 * would be two readings of one contract, which is the same reason
 * {@code flavor.rest} exists. The TypeScript mirror is
 * {@code mqtt-rpc.ts} and the two agree topic for topic.
 */
public final class MqttFlavors {

	private MqttFlavors() {
	}

	/** What the payload of a call is encoded in unless the contract says otherwise. */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";

	/**
	 * Where a request goes: what the operation flavor says, otherwise
	 * the flavor's request topic with the operation's name below it.
	 */
	public static String requestTopic(MqttFlavor flavor, MqttOperationFlavor operationFlavor) {
		if (operationFlavor.getRequestTopic() != null && !operationFlavor.getRequestTopic().isBlank()) {
			return operationFlavor.getRequestTopic();
		}
		String base = trimSlashes(flavor.getRequestTopic());
		if (base.isEmpty()) {
			throw new IllegalArgumentException(
					"MqttFlavor carries no requestTopic and the operation flavor none either");
		}
		return base + "/" + operationName(operationFlavor);
	}

	/**
	 * The base a consumer builds its reply topic under. One topic per
	 * request goes below it, so a subscription never sees a foreign
	 * answer.
	 */
	public static String replyBase(MqttFlavor flavor, MqttOperationFlavor operationFlavor) {
		if (operationFlavor.getResponseTopic() != null && !operationFlavor.getResponseTopic().isBlank()) {
			return trimSlashes(operationFlavor.getResponseTopic());
		}
		if (flavor.getResponseTopic() != null && !flavor.getResponseTopic().isBlank()) {
			return trimSlashes(flavor.getResponseTopic());
		}
		return requestTopic(flavor, operationFlavor) + "/reply";
	}

	/**
	 * The QoS of one operation: its own when it states one, else the
	 * flavor's default, else at-least-once.
	 *
	 * <p>{@code isSetQos()} rather than a comparison with the first
	 * literal — #81 made the attribute unsettable for exactly this, so
	 * an operation that says nothing can be told from one that says
	 * at-most-once. The TypeScript side still has to guess, and says so
	 * where it does.
	 */
	public static int qos(MqttFlavor flavor, MqttOperationFlavor operationFlavor) {
		if (operationFlavor.isSetQos() && operationFlavor.getQos() != null) {
			return level(operationFlavor.getQos());
		}
		return flavor.getDefaultQos() != null ? level(flavor.getDefaultQos()) : 1;
	}

	/** Whether the answer is to be retained. Never — a call is a transition. */
	public static boolean retained() {
		return false;
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
	 * The operation flavor of one operation, by the flavor's own name
	 * first and then by the operation it binds — a provider may name a
	 * flavor differently from the operation it serves, the same rule the
	 * REST invoker follows.
	 */
	public static MqttOperationFlavor operationFlavor(MqttFlavor flavor, String operationName) {
		for (ServiceOperationFlavor candidate : flavor.getOperationFlavors()) {
			if (candidate instanceof MqttOperationFlavor mqtt && operationName.equals(mqtt.getName())) {
				return mqtt;
			}
		}
		for (ServiceOperationFlavor candidate : flavor.getOperationFlavors()) {
			if (candidate instanceof MqttOperationFlavor mqtt && mqtt.getOperation() != null
					&& operationName.equals(mqtt.getOperation().getName())) {
				return mqtt;
			}
		}
		return null;
	}

	private static String operationName(MqttOperationFlavor operationFlavor) {
		ServiceOperation operation = operationFlavor.getOperation();
		if (operation != null && operation.getName() != null) {
			return operation.getName();
		}
		return operationFlavor.getName() == null ? "" : operationFlavor.getName();
	}

	private static String first(List<String> declared) {
		return declared == null || declared.isEmpty() ? DEFAULT_CONTENT_TYPE : declared.get(0);
	}

	private static int level(MqttQos qos) {
		return switch (qos) {
			case AT_MOST_ONCE -> 0;
			case AT_LEAST_ONCE -> 1;
			case EXACTLY_ONCE -> 2;
		};
	}

	private static String trimSlashes(String topic) {
		if (topic == null) {
			return "";
		}
		String trimmed = topic;
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}
}
