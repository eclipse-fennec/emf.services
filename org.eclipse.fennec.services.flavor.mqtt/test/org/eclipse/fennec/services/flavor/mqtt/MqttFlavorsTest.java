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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttOperationFlavor;
import org.eclipse.fennec.services.MqttQos;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEventException;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.junit.jupiter.api.Test;

import io.cloudevents.model.ce.CloudEvent;

/**
 * The topic and QoS rules are a cross-language contract: the same
 * assertions stand in {@code mqtt-rpc.test.ts}, down to the strings.
 */
class MqttFlavorsTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private MqttFlavor payments() {
		ServiceOperation charge = F.createServiceOperation();
		charge.setName("charge");

		MqttFlavor flavor = F.createMqttFlavor();
		flavor.setName("payments-mqtt");
		flavor.getBrokers().add("tcp://fake:1883");
		flavor.setRequestTopic("ddsr/rpc/payments");

		MqttOperationFlavor operationFlavor = F.createMqttOperationFlavor();
		operationFlavor.setName("charge");
		operationFlavor.setOperation(charge);
		flavor.getOperationFlavors().add(operationFlavor);
		return flavor;
	}

	private static MqttOperationFlavor first(MqttFlavor flavor) {
		return (MqttOperationFlavor) flavor.getOperationFlavors().get(0);
	}

	@Test
	void the_topics_are_derived_the_way_both_languages_derive_them() {
		MqttFlavor flavor = payments();

		assertThat(MqttFlavors.requestTopic(flavor, first(flavor))).isEqualTo("ddsr/rpc/payments/charge");
		assertThat(MqttFlavors.replyBase(flavor, first(flavor)))
			.isEqualTo("ddsr/rpc/payments/charge/reply");
	}

	@Test
	void an_operation_that_says_nothing_lets_the_flavor_default_apply() {
		MqttFlavor flavor = payments();
		flavor.setDefaultQos(MqttQos.EXACTLY_ONCE);

		assertThat(MqttFlavors.qos(flavor, first(flavor)))
			.as("#81: isSetQos tells silence from a deliberate at-most-once")
			.isEqualTo(2);
	}

	@Test
	void an_operation_may_step_down_below_the_flavor_default() {
		MqttFlavor flavor = payments();
		flavor.setDefaultQos(MqttQos.EXACTLY_ONCE);
		first(flavor).setQos(MqttQos.AT_MOST_ONCE);

		assertThat(MqttFlavors.qos(flavor, first(flavor)))
			.as("the thing the TypeScript side cannot express yet")
			.isZero();
	}

	@Test
	void a_flavor_with_no_request_topic_cannot_say_where_a_call_goes() {
		MqttFlavor flavor = payments();
		flavor.setRequestTopic(null);

		assertThatThrownBy(() -> MqttFlavors.requestTopic(flavor, first(flavor)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("requestTopic");
	}

	@Test
	void the_encoding_is_the_contracts_choice_and_xmi_when_it_says_nothing() {
		MqttFlavor flavor = payments();

		assertThat(MqttFlavors.consumes(first(flavor))).isEqualTo("application/xml");

		first(flavor).getConsumes().add("application/x-protobuf");
		assertThat(MqttFlavors.consumes(first(flavor))).isEqualTo("application/x-protobuf");
	}

	@Test
	void a_call_says_where_its_answer_goes_and_the_answer_says_which_call() {
		CloudEvent request = MqttMessages.request("/consumer/probe", "charge",
				"ddsr/rpc/payments/charge/reply/7", "application/xml");
		byte[] message = MqttMessages.write(request, "<call/>".getBytes(StandardCharsets.UTF_8));

		CloudEventCodec.Message read = MqttMessages.read(message, CloudEvents.TYPE_INVOKE);
		assertThat(MqttMessages.replyTopicOf(read.attributes()))
			.isEqualTo("ddsr/rpc/payments/charge/reply/7");
		assertThat(new String(read.data(), StandardCharsets.UTF_8)).isEqualTo("<call/>");

		CloudEvent reply = MqttMessages.reply(read.attributes(), "/provider/Payment", "application/xml");
		assertThat(MqttMessages.correlationOf(reply)).isEqualTo(request.getEventId());
		assertThat(reply.getSubject())
			.as("an answer is about the same thing the call was about")
			.isEqualTo("charge");
	}

	@Test
	void a_message_of_another_kind_is_not_read_as_a_call() {
		CloudEvent foreign = CloudEvents.newEnvelope("com.example.something.happened", "/elsewhere",
				"application/xml");
		byte[] message = CloudEventCodec.writeStructured(foreign, "<x/>".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> MqttMessages.read(message, CloudEvents.TYPE_INVOKE))
			.isInstanceOf(CloudEventException.class)
			.hasMessageContaining("com.example.something.happened");
	}

	@Test
	void a_call_that_names_no_reply_address_is_refused_at_both_ends() {
		assertThatThrownBy(() -> MqttMessages.request("/consumer/probe", "charge", null, "application/xml"))
			.isInstanceOf(IllegalArgumentException.class);

		CloudEvent withoutReply = CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE, "/consumer/probe",
				"application/xml");
		assertThatThrownBy(() -> MqttMessages.replyTopicOf(withoutReply))
			.isInstanceOf(CloudEventException.class);
	}

	@Test
	void one_consumers_answers_live_under_its_own_name() {
		MqttFlavor flavor = payments();

		assertThat(MqttFlavors.replySubtree(flavor, first(flavor), "node-a"))
			.as("a broker ACL is written against a name, not against an unguessable id")
			.isEqualTo("ddsr/rpc/payments/charge/reply/node-a");
		assertThat(MqttFlavors.replyTopic(flavor, first(flavor), "node-a", "call-1"))
			.isEqualTo("ddsr/rpc/payments/charge/reply/node-a/call-1");
	}

	@Test
	void a_separate_response_tree_is_used_when_the_flavor_names_one() {
		MqttFlavor flavor = payments();
		flavor.setResponseTopic("ddsr/rpc/res/payments/charge");

		assertThat(MqttFlavors.replyTopic(flavor, first(flavor), "node-a", "call-1"))
			.as("requests and answers are two permissions, so a deployment may keep them two trees")
			.isEqualTo("ddsr/rpc/res/payments/charge/node-a/call-1");
		assertThat(MqttFlavors.requestTopic(flavor, first(flavor)))
			.isEqualTo("ddsr/rpc/payments/charge");
	}

	@Test
	void a_name_with_a_wildcard_in_it_cannot_claim_a_subtree_it_was_not_given() {
		assertThat(MqttFlavors.topicSegment("a/#")).isEqualTo("a__");
		assertThat(MqttFlavors.topicSegment("+")).isEqualTo("_");
		assertThat(MqttFlavors.topicSegment(null)).isEqualTo("anonymous");
		assertThat(MqttFlavors.replySubtree(payments(), first(payments()), "a/#"))
			.isEqualTo("ddsr/rpc/payments/charge/reply/a__");
	}
}
