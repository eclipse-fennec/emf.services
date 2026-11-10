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

import org.eclipse.fennec.services.AmqpFlavor;
import org.eclipse.fennec.services.AmqpOperationFlavor;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttOperationFlavor;
import org.eclipse.fennec.services.MqttQos;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.junit.jupiter.api.Test;

import io.cloudevents.model.ce.CloudEvent;

/**
 * One route out of two flavors: what everything behind it relies on to
 * be written once.
 */
class MessagingRouteTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static ServiceOperation operation(String name) {
		ServiceOperation operation = F.createServiceOperation();
		operation.setName(name);
		return operation;
	}

	private static MqttFlavor mqtt() {
		MqttFlavor flavor = F.createMqttFlavor();
		flavor.getBrokers().add("tcp://mosquitto:1883");
		flavor.setRequestTopic("ddsr/rpc/req/payments");
		flavor.setResponseTopic("ddsr/rpc/res/payments");
		flavor.setDefaultQos(MqttQos.AT_LEAST_ONCE);
		MqttOperationFlavor charge = F.createMqttOperationFlavor();
		charge.setName("charge");
		charge.setOperation(operation("charge"));
		flavor.getOperationFlavors().add(charge);
		return flavor;
	}

	private static AmqpFlavor amqp() {
		AmqpFlavor flavor = F.createAmqpFlavor();
		flavor.getBrokers().add("amqp://rabbit:5672");
		flavor.setRequestRoutingKey("ddsr.rpc.payments");
		AmqpOperationFlavor charge = F.createAmqpOperationFlavor();
		charge.setName("charge");
		charge.setOperation(operation("charge"));
		flavor.getOperationFlavors().add(charge);
		return flavor;
	}

	@Test
	void anMqttFlavorIsATopicAndAWayBackInTheEnvelope() {
		MessagingRoute route = MessagingRoute.of(mqtt());

		assertThat(route.protocol()).isEqualTo("MQTT");
		assertThat(route.broker()).isEqualTo("tcp://mosquitto:1883");
		assertThat(route.exchange()).isNull();
		assertThat(route.repliesNatively()).isFalse();
		MessagingRoute.Operation charge = route.operation("charge");
		assertThat(charge.channel()).isEqualTo("ddsr/rpc/req/payments/charge");
		assertThat(charge.qos()).isEqualTo(1);
		assertThat(route.replySubtree(charge, "node a")).isEqualTo("ddsr/rpc/res/payments/node_a");
		assertThat(route.replyTopic(charge, "node a", "42")).isEqualTo("ddsr/rpc/res/payments/node_a/42");
	}

	@Test
	void anAmqpFlavorIsARoutingKeyOnAnExchangeAndAWayBackOfItsOwn() {
		MessagingRoute route = MessagingRoute.of(amqp());

		assertThat(route.protocol()).isEqualTo("AMQP");
		assertThat(route.broker()).isEqualTo("amqp://rabbit:5672");
		assertThat(route.exchange()).isEqualTo("amq.topic");
		assertThat(route.repliesNatively()).isTrue();
		MessagingRoute.Operation charge = route.operation("charge");
		assertThat(charge.channel()).isEqualTo("ddsr.rpc.payments.charge");
		assertThatThrownBy(() -> route.replySubtree(charge, "node-a"))
				.as("there is no reply tree to name where the protocol carries the address")
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void anOperationIsFoundByItsChannelWhichIsHowACallArrives() {
		MessagingRoute route = MessagingRoute.of(amqp());

		assertThat(route.onChannel("ddsr.rpc.payments.charge").name()).isEqualTo("charge");
		assertThat(route.onChannel("ddsr.rpc.other.charge")).isNull();
		assertThat(route.operation("refund")).isNull();
	}

	@Test
	void aNameBecomesOneLevelOfTheProtocolsChannels() {
		assertThat(MessagingRoute.of(mqtt()).segment("a/b+c")).isEqualTo("a_b_c");
		assertThat(MessagingRoute.of(amqp()).segment("a.b*c")).isEqualTo("a_b_c");
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
		MqttFlavor flavor = mqtt();
		flavor.getBrokers().clear();

		assertThat(MessagingRoute.of(flavor).broker()).isNull();
	}

	@Test
	void theEnvelopeNamesAReplyAddressOnlyWhereTheProtocolHasNone() {
		CloudEvent withAddress = MessagingRoute.request("/consumer/a", "charge", "application/xml",
				"ddsr/rpc/res/payments/a/1");
		CloudEvent without = MessagingRoute.request("/consumer/a", "charge", "application/xml", null);

		assertThat(MessagingRoute.replyAddressOf(withAddress)).isEqualTo("ddsr/rpc/res/payments/a/1");
		assertThat(without.getExtensions().get(CloudEvents.EXTENSION_REPLY_TO)).isNull();
		assertThat(without.getType()).isEqualTo(CloudEvents.TYPE_INVOKE);
		assertThat(without.getSubject()).isEqualTo("charge");
	}

	@Test
	void anAnswerNamesItsCallOnEitherProtocol() {
		CloudEvent request = MessagingRoute.request("/consumer/a", "charge", "application/xml", null);
		byte[] wire = MessagingRoute.write(MessagingRoute.reply(request, "/provider/p", "application/xml"),
				"<r/>".getBytes());

		var read = MessagingRoute.read(wire, CloudEvents.TYPE_INVOKE_REPLY);
		assertThat(MessagingRoute.correlationOf(read.attributes())).isEqualTo(request.getEventId());
		assertThat(read.attributes().getSubject()).isEqualTo("charge");
	}
}
