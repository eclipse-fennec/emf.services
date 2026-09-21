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

package org.eclipse.fennec.services.provider.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.List;

import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.Registration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * The decisions of the generic MQTT distribution — the twin of #84 on
 * this transport.
 *
 * <p>What is worth pinning is not that it subscribes (the dispatcher's
 * own test covers what a call does) but the two things that would be
 * wrong quietly: which broker an instance ends up listening on, and
 * the order it goes down in.
 */
class GenericMqttDistributionTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static MqttFlavor flavorOn(String broker) {
		MqttFlavor flavor = F.createMqttFlavor();
		flavor.setName("payments-mqtt");
		if (broker != null) {
			flavor.getBrokers().add(broker);
		}
		return flavor;
	}

	@Test
	@DisplayName("a document that names its broker runs as it is")
	void theDocumentIsEnough() {
		MqttFlavor flavor = flavorOn("tcp://modelled:1883");

		assertThat(GenericMqttDistribution.listenOn(flavor, "").getBrokers())
			.as("an empty setting is not a configuration")
			.containsExactly("tcp://modelled:1883");
	}

	@Test
	@DisplayName("a configured broker replaces the modelled one rather than joining it")
	void configurationWins() {
		MqttFlavor flavor = flavorOn("tcp://modelled:1883");

		assertThat(GenericMqttDistribution.listenOn(flavor, "tcp://deployed:1883").getBrokers())
			.as("a deployment that has moved is not also still at the old address")
			.containsExactly("tcp://deployed:1883");
	}

	@Test
	@DisplayName("a document that leaves the broker to the deployment, and a deployment that says nothing")
	void nobodyNamedABroker() {
		MqttFlavor flavor = flavorOn(null);

		assertThat(GenericMqttDistribution.listenOn(flavor, "").getBrokers())
			.as("there is nothing to connect to — which is a component with nothing to do, not a failure")
			.isEmpty();
	}

	@Test
	@DisplayName("an implementation without an MQTT flavor says so, rather than serving nothing")
	void noMqttFlavor() {
		ServiceImplementation implementation = F.createServiceImplementation();
		implementation.setName("payments-rest-only");
		RestFlavor rest = F.createRestFlavor();
		rest.setName("payments-rest");
		implementation.getFlavors().add(rest);

		assertThatThrownBy(() -> GenericMqttDistribution.mqttFlavorOf(implementation))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("announces no MQTT flavor");
	}

	@Test
	@DisplayName("on the way down it withdraws first and stops listening second")
	void withdrawBeforeSilence() throws Exception {
		GenericMqttDistribution distribution = new GenericMqttDistribution();
		Registration registration = mock(Registration.class);
		MqttDistribution.Served served = mock(MqttDistribution.Served.class);
		set(distribution, "registration", registration);
		set(distribution, "served", served);

		distribution.deactivate();

		InOrder order = inOrder(registration, served);
		// FR-P3 on this transport: a consumer is told before the topics
		// go quiet, not after.
		order.verify(registration).withdraw();
		order.verify(served).close();
	}

	@Test
	@DisplayName("a deployment that only serves has nothing to withdraw")
	void servingOnly() {
		GenericMqttDistribution distribution = new GenericMqttDistribution();

		assertThat(List.of(distribution)).allSatisfy(d -> d.deactivate());
	}

	private static void set(GenericMqttDistribution distribution, String field, Object value)
			throws Exception {
		Field declared = GenericMqttDistribution.class.getDeclaredField(field);
		declared.setAccessible(true);
		declared.set(distribution, value);
	}
}
