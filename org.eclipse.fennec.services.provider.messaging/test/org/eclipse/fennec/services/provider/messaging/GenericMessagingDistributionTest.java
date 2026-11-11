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

package org.eclipse.fennec.services.provider.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.List;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.MessagingFlavor;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;

/**
 * The decisions of the generic MQTT distribution — the twin of #84 on
 * this transport.
 *
 * <p>What is worth pinning is not that it subscribes (the dispatcher's
 * own test covers what a call does) but the two things that would be
 * wrong quietly: which broker an instance ends up listening on, and
 * the order it goes down in.
 */
class GenericMessagingDistributionTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static MessagingFlavor flavorOn(String broker) {
		MessagingFlavor flavor = F.createMessagingFlavor();
		flavor.setName("payments-mqtt");
		if (broker != null) {
			flavor.getBrokers().add(broker);
		}
		return flavor;
	}

	@Test
	@DisplayName("a document that names its broker runs as it is")
	void theDocumentIsEnough() {
		MessagingFlavor flavor = flavorOn("tcp://modelled:1883");

		assertThat(GenericMessagingDistribution.listenOn(flavor, "").getBrokers())
			.as("an empty setting is not a configuration")
			.containsExactly("tcp://modelled:1883");
	}

	@Test
	@DisplayName("a configured broker replaces the modelled one rather than joining it")
	void configurationWins() {
		MessagingFlavor flavor = flavorOn("tcp://modelled:1883");

		assertThat(GenericMessagingDistribution.listenOn(flavor, "tcp://deployed:1883").getBrokers())
			.as("a deployment that has moved is not also still at the old address")
			.containsExactly("tcp://deployed:1883");
	}

	@Test
	@DisplayName("a document that leaves the broker to the deployment, and a deployment that says nothing")
	void nobodyNamedABroker() {
		MessagingFlavor flavor = flavorOn(null);

		assertThat(GenericMessagingDistribution.listenOn(flavor, "").getBrokers())
			.as("there is nothing to connect to — which is a component with nothing to do, not a failure")
			.isEmpty();
	}

	@Test
	@DisplayName("an AMQP document is deployed the same way: its broker is the deployment's to say")
	void anAmqpFlavorListensWhereTheDeploymentSays() {
		MessagingFlavor flavor = F.createMessagingFlavor();
		flavor.setKind(FlavorKind.AMQP);
		flavor.getBrokers().add("amqp://modelled:5672");

		assertThat(GenericMessagingDistribution.listenOn(flavor, "amqp://deployed:5672").getBrokers())
			.containsExactly("amqp://deployed:5672");

		ServiceImplementation implementation = F.createServiceImplementation();
		implementation.getFlavors().add(F.createRestFlavor());
		implementation.getFlavors().add(flavor);
		assertThat(GenericMessagingDistribution.messagingFlavorOf(implementation)).isSameAs(flavor);
	}

	@Test
	@DisplayName("an implementation without a messaging flavor says so, rather than serving nothing")
	void noMqttFlavor() {
		ServiceImplementation implementation = F.createServiceImplementation();
		implementation.setName("payments-rest-only");
		RestFlavor rest = F.createRestFlavor();
		rest.setName("payments-rest");
		implementation.getFlavors().add(rest);

		assertThatThrownBy(() -> GenericMessagingDistribution.messagingFlavorOf(implementation))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("announces no flavor that travels over messaging");
	}

	@Test
	@DisplayName("on the way down the condition goes first and the topics go quiet second")
	@SuppressWarnings("unchecked")
	void theConditionGoesBeforeSilence() throws Exception {
		// The announcement is bound to the condition: taking it away
		// deactivates the announcement, which withdraws. FR-P3 on this
		// transport — a consumer is told before the topics go quiet.
		GenericMessagingDistribution distribution = new GenericMessagingDistribution();
		ServiceRegistration<Object> condition = mock(ServiceRegistration.class);
		Configuration announcement = mock(Configuration.class);
		MessagingDistribution.Served served = mock(MessagingDistribution.Served.class);
		set(distribution, "condition", condition);
		set(distribution, "announcement", announcement);
		set(distribution, "served", served);

		distribution.deactivate();

		InOrder order = inOrder(condition, announcement, served);
		order.verify(condition).unregister();
		order.verify(announcement).delete();
		order.verify(served).close();
	}



	void servingOnly() {
		GenericMessagingDistribution distribution = new GenericMessagingDistribution();

		assertThat(List.of(distribution)).allSatisfy(d -> d.deactivate());
	}

	private static void set(GenericMessagingDistribution distribution, String field, Object value)
			throws Exception {
		Field declared = GenericMessagingDistribution.class.getDeclaredField(field);
		declared.setAccessible(true);
		declared.set(distribution, value);
	}
}
