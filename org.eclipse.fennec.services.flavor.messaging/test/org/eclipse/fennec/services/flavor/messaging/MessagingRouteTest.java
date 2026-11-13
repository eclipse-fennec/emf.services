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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.MessagingFlavor;
import org.eclipse.fennec.services.MessagingOperationFlavor;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.junit.jupiter.api.Test;

import io.cloudevents.model.ce.CloudEvent;

/**
 * A flavor read once into a route: what everything behind it — the
 * invoker, the dispatcher, the distribution — works on.
 */
class MessagingRouteTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static MessagingFlavor flavor(FlavorKind kind, String broker, String channel) {
		ServiceOperation charge = F.createServiceOperation();
		charge.setName("charge");
		MessagingFlavor flavor = F.createMessagingFlavor();
		flavor.setKind(kind);
		flavor.getBrokers().add(broker);
		flavor.setChannel(channel);
		MessagingOperationFlavor operationFlavor = F.createMessagingOperationFlavor();
		operationFlavor.setName("charge");
		operationFlavor.setOperation(charge);
		flavor.getOperationFlavors().add(operationFlavor);
		return flavor;
	}

	private static MessagingFlavor mqtt() {
		MessagingFlavor flavor = flavor(FlavorKind.MQTT, "tcp://mosquitto:1883", "ddsr/rpc/req/payments");
		flavor.setReplyChannel("ddsr/rpc/res/payments");
		return flavor;
	}

	private static MessagingFlavor amqp() {
		return flavor(FlavorKind.AMQP, "amqp://rabbit:5672", "ddsr.rpc.payments");
	}

	@Test
	void aFlavorThatNamesAPlaceForAnswersGivesTheConsumerItsSubtreeThere() {
		MessagingRoute route = MessagingRoute.of(mqtt());

		assertThat(route.protocol()).isEqualTo("MQTT");
		assertThat(route.broker()).isEqualTo("tcp://mosquitto:1883");
		assertThat(route.exchange()).isNull();
		MessagingRoute.Operation charge = route.operation("charge");
		assertThat(charge.channel()).isEqualTo("ddsr/rpc/req/payments/charge");
		assertThat(charge.qos()).isEqualTo(1);
		assertThat(charge.namesReplyChannel()).isTrue();
		assertThat(route.replySubtree(charge, "node a")).isEqualTo("ddsr/rpc/res/payments/node_a");
		assertThat(route.replyFilter(charge, "node a")).isEqualTo("ddsr/rpc/res/payments/node_a/+");
		assertThat(route.replyChannel(charge, "node a", "42")).isEqualTo("ddsr/rpc/res/payments/node_a/42");
	}

	@Test
	void aFlavorThatNamesNoneLeavesTheAnswerToTheConnection() {
		MessagingRoute route = MessagingRoute.of(amqp());

		assertThat(route.protocol()).isEqualTo("AMQP");
		assertThat(route.exchange()).isEqualTo("amq.topic");
		MessagingRoute.Operation charge = route.operation("charge");
		assertThat(charge.channel()).isEqualTo("ddsr.rpc.payments.charge");
		assertThat(charge.namesReplyChannel()).isFalse();
		assertThatThrownBy(() -> route.replySubtree(charge, "node-a"))
				.as("there is no subtree to listen on where the connection provides the reply channel")
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void whichOfTheTwoIsTheFlavorsStatementNotTheProtocols() {
		// An MQTT contract may leave the reply channel to the connection,
		// and an AMQP one may name a tree: the model decides, per flavor.
		MessagingFlavor mqttWithout = mqtt();
		mqttWithout.setReplyChannel(null);
		MessagingFlavor amqpWith = amqp();
		amqpWith.setReplyChannel("ddsr.rpc.answers");

		assertThat(MessagingRoute.of(mqttWithout).operation("charge").namesReplyChannel()).isFalse();
		MessagingRoute route = MessagingRoute.of(amqpWith);
		assertThat(route.replyFilter(route.operation("charge"), "node.a"))
				.isEqualTo("ddsr.rpc.answers.node_a.*");
	}

	@Test
	void anOperationIsFoundByItsNameAndByTheChannelACallArrivesOn() {
		MessagingFlavor flavor = amqp();
		flavor.getOperationFlavors().get(0).setName("debit");
		MessagingRoute route = MessagingRoute.of(flavor);

		assertThat(route.operation("debit").name()).isEqualTo("charge");
		assertThat(route.operation("charge").flavor().getName()).isEqualTo("debit");
		assertThat(route.operation("refund")).isNull();
		assertThat(route.onChannel("ddsr.rpc.payments.charge").name()).isEqualTo("charge");
		assertThat(route.onChannel("ddsr.rpc.other.charge")).isNull();
	}

	@Test
	void theRouteToAnImplementationIsItsFirstMessagingFlavor() {
		ServiceImplementation implementation = F.createServiceImplementation();
		RestFlavor rest = F.createRestFlavor();
		implementation.getFlavors().add(rest);
		assertThat(MessagingRoute.of(implementation)).isNull();

		implementation.getFlavors().add(amqp());
		implementation.getFlavors().add(mqtt());
		assertThat(MessagingRoute.of(implementation).protocol())
				.as("the order is the provider's own statement about what it prefers")
				.isEqualTo("AMQP");
		assertThatThrownBy(() -> MessagingRoute.of(rest)).isInstanceOf(IllegalArgumentException.class);
		assertThat(MessagingRoute.speaks(rest)).isFalse();
	}

	@Test
	void aBrokerlessFlavorHasNoBrokerRatherThanAnError() {
		MessagingFlavor flavor = mqtt();
		flavor.getBrokers().clear();

		assertThat(MessagingRoute.of(flavor).broker()).isNull();
	}

	@Test
	void aCallNamesAReplyChannelOnlyWhereTheFlavorNamesAPlaceForAnswers() {
		CloudEvent withAddress = MessagingRoute.request("/consumer/a", "charge", "application/xml",
				"ddsr/rpc/res/payments/a/1");
		CloudEvent without = MessagingRoute.request("/consumer/a", "charge", "application/xml", null);

		assertThat(MessagingRoute.replyAddressOf(withAddress)).isEqualTo("ddsr/rpc/res/payments/a/1");
		assertThat(without.getExtensions().get(CloudEvents.EXTENSION_REPLY_TO)).isNull();
		assertThatThrownBy(() -> MessagingRoute.replyAddressOf(without)).hasMessageContaining("reply address");
		assertThat(without.getType()).isEqualTo(CloudEvents.TYPE_INVOKE);
		assertThat(without.getSubject()).isEqualTo("charge");
	}

	@Test
	void anAnswerNamesItsCallAndSaysWhatWasAsked() {
		CloudEvent request = MessagingRoute.request("/consumer/a", "charge", "application/xml", null);
		CloudEvent reply = MessagingRoute.reply(request, "/provider/p", "application/xml");

		assertThat(reply.getType()).isEqualTo(CloudEvents.TYPE_INVOKE_REPLY);
		assertThat(MessagingRoute.correlationOf(reply)).isEqualTo(request.getEventId());
		assertThat(reply.getSubject()).isEqualTo("charge");
		assertThat(reply.getEventId()).isNotEqualTo(request.getEventId());
	}
}
