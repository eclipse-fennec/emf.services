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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.junit.jupiter.api.Test;

/**
 * Which transport a proxy calls over (#98).
 *
 * <p>Before this, one invoker was bound and whatever it spoke was what
 * every call used. With a second flavor in the world that is no longer
 * a decision anybody made — so it is made here, from what the service
 * announces and what this runtime can actually speak.
 */
class ProxyFactoryFlavorSelectionTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	/** A service interface with the one operation the proxy calls. */
	public interface Payment {

		double getBalance(String accountId);
	}

	/** Records that it was the one asked. */
	private static final class NamedInvoker implements ServiceInvoker {

		private final String name;

		private final List<String> calls = new ArrayList<>();

		NamedInvoker(String name) {
			this.name = name;
		}

		@Override
		public Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args) {
			calls.add(operationName);
			return 42.0;
		}
	}

	private static final class FixedLocator implements ServiceLocator {

		private final ServiceImplementation implementation;

		FixedLocator(ServiceImplementation implementation) {
			this.implementation = implementation;
		}

		@Override
		public ServiceReference reference() {
			ServiceReference reference = F.createServiceReference();
			reference.setId("ref-1");
			return reference;
		}

		@Override
		public ServiceImplementation implementation() {
			return implementation;
		}

		@Override
		public Optional<RestFlavor> restFlavor() {
			for (var flavor : implementation.getFlavors()) {
				if (flavor instanceof RestFlavor rest) {
					return Optional.of(rest);
				}
			}
			return Optional.empty();
		}

		@Override
		public Optional<URI> urlFor(String operationName) {
			return Optional.of(URI.create("http://localhost:9091/payments"));
		}
	}

	/** An implementation announcing the given flavors, in that order. */
	private static ServiceImplementation announcing(FlavorKind... kinds) {
		ServiceOperation getBalance = F.createServiceOperation();
		getBalance.setName("getBalance");
		var accountId = F.createParameter();
		accountId.setName("accountId");
		accountId.setType("string");
		getBalance.getParameters().add(accountId);
		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Payment");
		contract.getOperations().add(getBalance);

		ServiceImplementation implementation = F.createServiceImplementation();
		implementation.setName("payments");
		implementation.getServiceInterfaces().add(contract);
		for (FlavorKind kind : kinds) {
			if (kind == FlavorKind.REST) {
				RestFlavor rest = F.createRestFlavor();
				rest.setKind(FlavorKind.REST);
				rest.setName("payments-rest");
				rest.setBasePath("/payments");
				implementation.getFlavors().add(rest);
			} else {
				MqttFlavor mqtt = F.createMqttFlavor();
				mqtt.setKind(FlavorKind.MQTT);
				mqtt.setName("payments-mqtt");
				mqtt.getBrokers().add("tcp://localhost:1883");
				mqtt.setRequestTopic("ddsr/rpc/payments");
				implementation.getFlavors().add(mqtt);
			}
		}
		return implementation;
	}

	private ReflectiveServiceProxyFactory factoryWith(NamedInvoker... invokers) {
		ReflectiveServiceProxyFactory factory = new ReflectiveServiceProxyFactory();
		for (NamedInvoker invoker : invokers) {
			factory.addInvoker(invoker, Map.of(ServiceInvoker.FLAVOR_PROPERTY, invoker.name));
		}
		return factory;
	}

	@Test
	void the_first_announced_flavor_this_runtime_speaks_is_the_one_used() {
		NamedInvoker rest = new NamedInvoker("REST");
		NamedInvoker mqtt = new NamedInvoker("MQTT");
		ServiceLocator locator = new FixedLocator(announcing(FlavorKind.REST, FlavorKind.MQTT));

		factoryWith(rest, mqtt).newProxy(Payment.class, locator).getBalance("acc-1");

		assertThat(rest.calls).containsExactly("getBalance");
		assertThat(mqtt.calls).isEmpty();
	}

	@Test
	void a_provider_that_lists_mqtt_first_is_called_over_mqtt() {
		NamedInvoker rest = new NamedInvoker("REST");
		NamedInvoker mqtt = new NamedInvoker("MQTT");
		ServiceLocator locator = new FixedLocator(announcing(FlavorKind.MQTT, FlavorKind.REST));

		factoryWith(rest, mqtt).newProxy(Payment.class, locator).getBalance("acc-1");

		assertThat(mqtt.calls)
			.as("the order a provider announces its flavors in is its own statement")
			.containsExactly("getBalance");
		assertThat(rest.calls).isEmpty();
	}

	@Test
	void a_flavor_this_runtime_cannot_speak_is_a_failure_that_says_so() {
		NamedInvoker rest = new NamedInvoker("REST");
		ServiceLocator locator = new FixedLocator(announcing(FlavorKind.MQTT));

		assertThatThrownBy(() -> factoryWith(rest).newProxy(Payment.class, locator).getBalance("acc-1"))
			.isInstanceOf(DdsrException.class)
			.hasMessageContaining("MQTT")
			.hasMessageContaining("REST");
	}

	@Test
	void an_invoker_that_names_no_flavor_is_never_picked() {
		NamedInvoker nameless = new NamedInvoker("REST");
		ReflectiveServiceProxyFactory factory = new ReflectiveServiceProxyFactory();
		factory.addInvoker(nameless, Map.of());
		ServiceLocator locator = new FixedLocator(announcing(FlavorKind.REST));

		assertThatThrownBy(() -> factory.newProxy(Payment.class, locator).getBalance("acc-1"))
			.as("calling the wrong way is worse than not calling")
			.isInstanceOf(DdsrException.class);
		assertThat(nameless.calls).isEmpty();
	}

	@Test
	void an_implementation_that_announces_no_flavor_at_all_still_works_with_one_invoker() {
		NamedInvoker rest = new NamedInvoker("REST");
		ServiceLocator locator = new FixedLocator(announcing());

		factoryWith(rest).newProxy(Payment.class, locator).getBalance("acc-1");

		assertThat(rest.calls)
			.as("the older shape of a locator, and one invoker is the only reading of it")
			.containsExactly("getBalance");
	}

	@Test
	void an_invoker_that_goes_away_is_no_longer_picked() {
		NamedInvoker rest = new NamedInvoker("REST");
		NamedInvoker mqtt = new NamedInvoker("MQTT");
		ReflectiveServiceProxyFactory factory = factoryWith(rest, mqtt);
		ServiceLocator locator = new FixedLocator(announcing(FlavorKind.REST, FlavorKind.MQTT));

		factory.removeInvoker(rest, Map.of(ServiceInvoker.FLAVOR_PROPERTY, "REST"));
		factory.newProxy(Payment.class, locator).getBalance("acc-1");

		assertThat(rest.calls).isEmpty();
		assertThat(mqtt.calls)
			.as("a transport that left takes its flavor with it")
			.containsExactly("getBalance");
	}
}
