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

package org.eclipse.fennec.services.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.client.InvokerProvider;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * What the registry says about itself, and when: the property is what
 * a client waits for, so its order against the invokers is the contract.
 */
class InvokerRegistryTest {

	private final InvokerRegistry registry = new InvokerRegistry();

	/** What was published, in order, and whether the invoker was callable at that moment. */
	private final List<String> published = new ArrayList<>();

	@SuppressWarnings("unchecked")
	private void activate(FlavorKind watched) {
		BundleContext context = mock(BundleContext.class);
		ServiceRegistration<InvokerProvider> registration = mock(ServiceRegistration.class);
		when(context.registerService(eq(InvokerProvider.class), any(InvokerProvider.class), any(Dictionary.class)))
				.thenAnswer(call -> {
					record(call.getArgument(2), watched);
					return registration;
				});
		doAnswer(call -> {
			record(call.getArgument(0), watched);
			return null;
		}).when(registration).setProperties(any());
		registry.activate(context);
	}

	private void record(Dictionary<String, Object> properties, FlavorKind watched) {
		String flavors = String.join(",", (String[]) properties.get(InvokerProvider.FLAVORS));
		published.add("[" + flavors + "] " + (registry.invokerFor(watched) != null ? "callable" : "gone"));
	}

	private static ServiceInvoker invoker() {
		return (locator, operation, args) -> null;
	}

	@Test
	@DisplayName("invokers bound before activation are in the registration from the start")
	void whatIsThereIsRegistered() {
		registry.addInvoker(invoker(), Map.of(ServiceInvoker.FLAVOR_PROPERTY, "REST"));

		activate(FlavorKind.REST);

		assertThat(published).first().isEqualTo("[REST] callable");
	}

	@Test
	@DisplayName("an invoker is in before its flavor is published, and out after it is withdrawn")
	void firstTheInvokerThenTheProperty() {
		ServiceInvoker rest = invoker();
		activate(FlavorKind.REST);
		published.clear();

		registry.addInvoker(rest, Map.of(ServiceInvoker.FLAVOR_PROPERTY, "REST"));
		registry.removeInvoker(rest, Map.of(ServiceInvoker.FLAVOR_PROPERTY, "REST"));

		assertThat(published)
			.as("whoever is let in finds the invoker; whoever is turned away still has it meanwhile")
			.containsExactly("[REST] callable", "[] callable");
		assertThat(registry.invokerFor(FlavorKind.REST)).isNull();
	}

	@Test
	@DisplayName("one invoker that speaks two flavors is published under both")
	void theMessagingInvokerIsTwoFlavors() {
		activate(FlavorKind.MQTT);
		published.clear();

		registry.addInvoker(invoker(), Map.of(ServiceInvoker.FLAVOR_PROPERTY, new String[] { "MQTT", "AMQP" }));

		assertThat(registry.flavors()).containsExactlyInAnyOrder(FlavorKind.MQTT, FlavorKind.AMQP);
		assertThat(published).last().asString().contains("MQTT").contains("AMQP");
	}
}
