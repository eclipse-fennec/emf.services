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
import org.eclipse.fennec.services.MessagingQos;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.Test;

/**
 * The placement rules of a messaging flavor. They are a cross-language
 * contract: the names pinned here are the ones the TypeScript track
 * computes, and the ones already on the wire before the two flavors
 * became one.
 */
class MessagingFlavorsTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static MessagingFlavor mqtt(String channel) {
		MessagingFlavor flavor = F.createMessagingFlavor();
		flavor.setKind(FlavorKind.MQTT);
		flavor.getBrokers().add("tcp://mosquitto:1883");
		flavor.setChannel(channel);
		return flavor;
	}

	private static MessagingFlavor amqp(String channel) {
		MessagingFlavor flavor = F.createMessagingFlavor();
		flavor.setKind(FlavorKind.AMQP);
		flavor.getBrokers().add("amqp://rabbit:5672");
		flavor.setChannel(channel);
		return flavor;
	}

	private static MessagingOperationFlavor operation(MessagingFlavor flavor, String name) {
		ServiceOperation operation = F.createServiceOperation();
		operation.setName(name);
		MessagingOperationFlavor operationFlavor = F.createMessagingOperationFlavor();
		operationFlavor.setName(name);
		operationFlavor.setOperation(operation);
		flavor.getOperationFlavors().add(operationFlavor);
		return operationFlavor;
	}

	@Test
	void anOperationIsReachedOnTheBaseChannelAndItsNameInTheProtocolsSpelling() {
		MessagingFlavor topics = mqtt("ddsr/rpc/payments");
		MessagingFlavor keys = amqp("ddsr.rpc.payments");

		assertThat(MessagingFlavors.channel(topics, operation(topics, "charge")))
				.isEqualTo("ddsr/rpc/payments/charge");
		assertThat(MessagingFlavors.channel(keys, operation(keys, "charge")))
				.isEqualTo("ddsr.rpc.payments.charge");
	}

	@Test
	void aTrailingSeparatorOnTheBaseIsNotALevel() {
		MessagingFlavor topics = mqtt("ddsr/rpc/payments/");
		MessagingFlavor keys = amqp("ddsr.rpc.payments.");

		assertThat(MessagingFlavors.channel(topics, operation(topics, "charge")))
				.isEqualTo("ddsr/rpc/payments/charge");
		assertThat(MessagingFlavors.channel(keys, operation(keys, "charge")))
				.isEqualTo("ddsr.rpc.payments.charge");
	}

	@Test
	void anOperationsOwnChannelWinsAsItIs() {
		MessagingFlavor flavor = mqtt("ddsr/rpc/payments");
		MessagingOperationFlavor charge = operation(flavor, "charge");
		charge.setChannel("billing/debit");

		assertThat(MessagingFlavors.channel(flavor, charge)).isEqualTo("billing/debit");
	}

	@Test
	void aFlavorWithoutAnyChannelIsRefused() {
		MessagingFlavor flavor = mqtt(null);

		assertThatThrownBy(() -> MessagingFlavors.channel(flavor, operation(flavor, "charge")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void aFlavorThatNamesNoPlaceForAnswersLeavesItToTheConnection() {
		MessagingFlavor flavor = amqp("ddsr.rpc.payments");

		assertThat(MessagingFlavors.replyBase(flavor, operation(flavor, "charge")))
				.as("null is a statement: the consumer's connection provides the reply channel")
				.isNull();
	}

	@Test
	void aNamedPlaceForAnswersIsTheOperationsElseTheFlavors() {
		MessagingFlavor flavor = mqtt("ddsr/rpc/req/payments");
		flavor.setReplyChannel("ddsr/rpc/res/payments/");
		MessagingOperationFlavor charge = operation(flavor, "charge");
		MessagingOperationFlavor refund = operation(flavor, "refund");
		refund.setReplyChannel("billing/answers");

		assertThat(MessagingFlavors.replyBase(flavor, charge)).isEqualTo("ddsr/rpc/res/payments");
		assertThat(MessagingFlavors.replyBase(flavor, refund)).isEqualTo("billing/answers");
	}

	@Test
	void aConsumerListensOnASubtreeOfItsOwnNameAndEachCallHasItsChannelThere() {
		MessagingFlavor flavor = mqtt("ddsr/rpc/req/payments");

		// The name is what a broker's ACL is written against, so it must
		// be one level whatever characters it carries.
		assertThat(MessagingFlavors.replySubtree(flavor, "ddsr/rpc/res/payments", "node a/1"))
				.isEqualTo("ddsr/rpc/res/payments/node_a_1");
		assertThat(MessagingFlavors.subtreeFilter(flavor, "ddsr/rpc/res/payments/node_a_1"))
				.isEqualTo("ddsr/rpc/res/payments/node_a_1/#");
		assertThat(MessagingFlavors.replyChannel(flavor, "ddsr/rpc/res/payments", "node a/1", "42"))
				.isEqualTo("ddsr/rpc/res/payments/node_a_1/42");
	}

	@Test
	void theSameOnRoutingKeysWithTheirSeparatorAndWildcard() {
		MessagingFlavor flavor = amqp("ddsr.rpc.payments");

		assertThat(MessagingFlavors.replySubtree(flavor, "ddsr.rpc.answers", "node.a"))
				.isEqualTo("ddsr.rpc.answers.node_a");
		assertThat(MessagingFlavors.subtreeFilter(flavor, "ddsr.rpc.answers.node_a"))
				.isEqualTo("ddsr.rpc.answers.node_a.#");
	}

	@Test
	void aNameCannotChangeWhatAChannelMeans() {
		assertThat(MessagingFlavors.segment(mqtt("x"), "a/b+c#d e")).isEqualTo("a_b_c_d_e");
		assertThat(MessagingFlavors.segment(amqp("x"), "a.b*c#d e")).isEqualTo("a_b_c_d_e");
		assertThat(MessagingFlavors.segment(mqtt("x"), " ")).isEqualTo("anonymous");
		// A dot is an ordinary character in a topic, a slash in a routing key.
		assertThat(MessagingFlavors.segment(mqtt("x"), "a.b")).isEqualTo("a.b");
		assertThat(MessagingFlavors.segment(amqp("x"), "a/b")).isEqualTo("a/b");
	}

	@Test
	void theQosIsTheOperationsElseTheFlavorsAndNeverMoreThanTheProtocolHas() {
		MessagingFlavor topics = mqtt("t");
		MessagingOperationFlavor charge = operation(topics, "charge");
		assertThat(MessagingFlavors.qos(topics, charge)).as("the flavor's default").isEqualTo(1);

		topics.setDefaultQos(MessagingQos.AT_MOST_ONCE);
		assertThat(MessagingFlavors.qos(topics, charge)).isEqualTo(0);

		charge.setQos(MessagingQos.EXACTLY_ONCE);
		assertThat(MessagingFlavors.qos(topics, charge)).isEqualTo(2);

		MessagingFlavor keys = amqp("k");
		MessagingOperationFlavor debit = operation(keys, "debit");
		debit.setQos(MessagingQos.EXACTLY_ONCE);
		assertThat(MessagingFlavors.qos(keys, debit))
				.as("AMQP 0-9-1 has no exactly-once; asking for it gets what there is")
				.isEqualTo(1);
	}

	@Test
	void anOperationThatSaysNothingAboutQosIsNotMistakenForTheFirstLiteral() {
		// Issue #81: without `unsettable` an unset enum reads as its first
		// literal, and the flavor default could never apply.
		MessagingFlavor flavor = mqtt("t");
		flavor.setDefaultQos(MessagingQos.EXACTLY_ONCE);

		assertThat(MessagingFlavors.qos(flavor, operation(flavor, "charge"))).isEqualTo(2);
	}

	@Test
	void theExchangeIsTheBrokersOwnUnlessTheFlavorNamesOneAndMqttHasNone() {
		MessagingFlavor keys = amqp("k");
		assertThat(MessagingFlavors.exchange(keys)).isEqualTo("amq.topic");
		keys.setExchange("calls");
		assertThat(MessagingFlavors.exchange(keys)).isEqualTo("calls");
		keys.setExchange(" ");
		assertThat(MessagingFlavors.exchange(keys)).isEqualTo("amq.topic");

		assertThat(MessagingFlavors.exchange(mqtt("t"))).isNull();
	}

	@Test
	void theProtocolIsTheKindOrForAFlavorWithoutOneTheSchemeOfItsBroker() {
		assertThat(MessagingFlavors.isAmqp(amqp("k"))).isTrue();
		assertThat(MessagingFlavors.isAmqp(mqtt("t"))).isFalse();

		// The kind's model default is REST, which is neither: a flavor
		// decoded without a kind is read by where it says its broker is.
		MessagingFlavor kindless = F.createMessagingFlavor();
		kindless.getBrokers().add("amqps://rabbit:5671/vhost");
		assertThat(MessagingFlavors.isAmqp(kindless)).isTrue();
		kindless.getBrokers().set(0, "ssl://mosquitto:8883");
		assertThat(MessagingFlavors.isAmqp(kindless)).isFalse();
	}

	@Test
	void theEncodingIsTheContractsElseXmi() {
		MessagingFlavor flavor = mqtt("t");
		MessagingOperationFlavor charge = operation(flavor, "charge");
		assertThat(MessagingFlavors.consumes(charge)).isEqualTo("application/xml");

		charge.getConsumes().add("application/x-protobuf");
		charge.getProduces().add("application/json");
		assertThat(MessagingFlavors.consumes(charge)).isEqualTo("application/x-protobuf");
		assertThat(MessagingFlavors.produces(charge)).isEqualTo("application/json");
	}
}
