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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.fennec.services.AmqpFlavor;
import org.eclipse.fennec.services.AmqpOperationFlavor;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
import org.junit.jupiter.api.Test;

import io.cloudevents.model.ce.CloudEvent;

/** The AMQP placement rules, which both ends of a call read alike. */
class AmqpFlavorsTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static AmqpFlavor flavor(String base) {
		AmqpFlavor flavor = F.createAmqpFlavor();
		flavor.setRequestRoutingKey(base);
		return flavor;
	}

	private static AmqpOperationFlavor operation(AmqpFlavor flavor, String name) {
		ServiceOperation operation = F.createServiceOperation();
		operation.setName(name);
		AmqpOperationFlavor operationFlavor = F.createAmqpOperationFlavor();
		operationFlavor.setName(name);
		operationFlavor.setOperation(operation);
		flavor.getOperationFlavors().add(operationFlavor);
		return operationFlavor;
	}

	@Test
	void anOperationIsReachedUnderTheBaseKeyAndItsName() {
		AmqpFlavor flavor = flavor("ddsr.rpc.payments");

		assertThat(AmqpFlavors.routingKey(flavor, operation(flavor, "charge")))
				.isEqualTo("ddsr.rpc.payments.charge");
	}

	@Test
	void aTrailingSeparatorOnTheBaseIsNotASegment() {
		AmqpFlavor flavor = flavor("ddsr.rpc.payments.");

		assertThat(AmqpFlavors.routingKey(flavor, operation(flavor, "charge")))
				.isEqualTo("ddsr.rpc.payments.charge");
	}

	@Test
	void anOperationsOwnKeyWinsAsItIs() {
		AmqpFlavor flavor = flavor("ddsr.rpc.payments");
		AmqpOperationFlavor charge = operation(flavor, "charge");
		charge.setRoutingKey("billing.debit");

		assertThat(AmqpFlavors.routingKey(flavor, charge)).isEqualTo("billing.debit");
	}

	@Test
	void aFlavorWithoutAnyKeyIsRefused() {
		AmqpFlavor flavor = flavor(null);

		assertThatThrownBy(() -> AmqpFlavors.routingKey(flavor, operation(flavor, "charge")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void aNameCannotChangeWhatAKeyMeans() {
		// The separator and the wildcards of a topic exchange: a name
		// carrying one would address something else.
		assertThat(AmqpFlavors.segment("a.b*c#d e")).isEqualTo("a_b_c_d_e");
		assertThat(AmqpFlavors.segment(" ")).isEqualTo("anonymous");
	}

	@Test
	void theExchangeIsTheBrokersOwnUnlessTheFlavorNamesOne() {
		AmqpFlavor flavor = flavor("k");
		assertThat(AmqpFlavors.exchange(flavor)).isEqualTo("amq.topic");

		flavor.setExchange("calls");
		assertThat(AmqpFlavors.exchange(flavor)).isEqualTo("calls");

		flavor.setExchange(" ");
		assertThat(AmqpFlavors.exchange(flavor)).isEqualTo("amq.topic");
	}

	@Test
	void anOperationFlavorIsFoundByItsNameAndThenByItsOperation() {
		AmqpFlavor flavor = flavor("k");
		AmqpOperationFlavor charge = operation(flavor, "charge");
		charge.setName("debit");

		assertThat(AmqpFlavors.operationFlavor(flavor, "debit")).isSameAs(charge);
		assertThat(AmqpFlavors.operationFlavor(flavor, "charge")).isSameAs(charge);
		assertThat(AmqpFlavors.operationFlavor(flavor, "refund")).isNull();
	}

	@Test
	void theEncodingIsTheContractsElseXmi() {
		AmqpFlavor flavor = flavor("k");
		AmqpOperationFlavor charge = operation(flavor, "charge");
		assertThat(AmqpFlavors.consumes(charge)).isEqualTo("application/xml");

		charge.getConsumes().add("application/x-protobuf");
		charge.getProduces().add("application/json");
		assertThat(AmqpFlavors.consumes(charge)).isEqualTo("application/x-protobuf");
		assertThat(AmqpFlavors.produces(charge)).isEqualTo("application/json");
	}

	@Test
	void aCallCarriesNoReplyAddressBecauseTheProtocolDoes() {
		CloudEvent request = AmqpMessages.request("/consumer/a", "charge", "application/xml");

		assertThat(request.getType()).isEqualTo(CloudEvents.TYPE_INVOKE);
		assertThat(request.getSubject()).isEqualTo("charge");
		assertThat(request.getExtensions().get(CloudEvents.EXTENSION_REPLY_TO)).isNull();
	}

	@Test
	void anAnswerNamesTheCallAndComesBackReadable() {
		CloudEvent request = AmqpMessages.request("/consumer/a", "charge", "application/xml");
		CloudEvent reply = AmqpMessages.reply(request, "/provider/p", "application/xml");
		byte[] wire = AmqpMessages.write(reply, "<r/>".getBytes());

		var read = AmqpMessages.read(wire, CloudEvents.TYPE_INVOKE_REPLY);
		assertThat(read.attributes().getSubject()).isEqualTo("charge");
		assertThat(read.attributes().getExtensions().get(CloudEvents.EXTENSION_CORRELATION_ID))
				.isEqualTo(request.getEventId());
		assertThat(new String(read.data())).isEqualTo("<r/>");
		assertThatThrownBy(() -> AmqpMessages.read(wire, CloudEvents.TYPE_INVOKE))
				.hasMessageContaining("expected");
	}
}
