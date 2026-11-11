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

package org.eclipse.fennec.services.client.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.client.InvokerProvider;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.client.ServiceProxyFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * A client is there exactly while there is an invoker for every flavor
 * its configuration names.
 *
 * <p>This is what Declarative Services does with a target and a service
 * property that changes, so it is tested in a framework. The client SDK
 * runs alone; the test plays the transport — it registers the broker
 * proxies, an event source and the invokers — which is what lets it
 * leave one out and take one away.
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
class ClientWaitsForInvokersTest {

	private static final String CLIENT_PID = "org.eclipse.fennec.services.client";

	private static final String CLIENT_COMPONENT =
			"org.eclipse.fennec.services.client.internal.DdsrClientComponent";

	private static final String REST_AND_MQTT = "(&(fennec.invokers=REST)(fennec.invokers=MQTT))";

	@InjectBundleContext
	BundleContext context;

	@InjectService
	ConfigurationAdmin configurations;

	@InjectService
	ServiceComponentRuntime components;

	private final List<ServiceRegistration<?>> registered = new ArrayList<>();

	private Configuration configuration;

	@BeforeEach
	void theTransportIsThere() {
		// Everything else the client needs, so that the invokers are the
		// only thing a test can be about.
		Dictionary<String, Object> rest = new Hashtable<>();
		rest.put("ddsr.broker.transport", "rest");
		for (Class<?> proxy : List.of(BrokerLookup.class, BrokerImplementations.class, BrokerSessions.class)) {
			registered.add(context.registerService(proxy.getName(), answeringNothing(proxy), rest));
		}
		registered.add(context.registerService(EventSource.class.getName(),
				answeringNothing(EventSource.class), null));
	}

	@AfterEach
	void nothingIsLeftBehind() throws Exception {
		if (configuration != null) {
			configuration.delete();
			configuration = null;
		}
		for (ServiceRegistration<?> registration : registered) {
			try {
				registration.unregister();
			} catch (IllegalStateException alreadyGone) {
				// a test took it away itself
			}
		}
		registered.clear();
		await("the client of the previous test is gone", () -> client() == null);
	}

	@Test
	@DisplayName("without a configuration there is no client, whatever is installed")
	void noConfigurationNoClient() throws Exception {
		invoker("REST");

		await("the invoker is published", () -> publishedFlavors().contains("REST"));
		settle();

		assertThat(client()).as("the client requires its configuration").isNull();
	}

	@Test
	@DisplayName("configured for REST and MQTT with only a REST invoker, the client stays down — and says why")
	void aMissingInvokerKeepsTheClientDown() throws Exception {
		invoker("REST");
		configure("REST,MQTT", REST_AND_MQTT);

		await("the client's configuration has arrived", () -> clientConfiguration() != null);
		await("the invoker is published", () -> publishedFlavors().contains("REST"));
		settle();

		assertThat(client()).as("nothing is said to the broker about a flavor that cannot be called").isNull();
		ComponentConfigurationDTO waiting = clientConfiguration();
		assertThat(waiting.state).isEqualTo(ComponentConfigurationDTO.UNSATISFIED_REFERENCE);
		assertThat(Arrays.stream(waiting.unsatisfiedReferences).map((UnsatisfiedReferenceDTO r) -> r.name))
			.as("the one thing that is missing, by the name the configuration targets")
			.containsExactly("invokers");
	}

	@Test
	@DisplayName("the client comes up with the last invoker it was configured for")
	void theLastInvokerBringsTheClientUp() throws Exception {
		invoker("REST");
		configure("REST,MQTT", REST_AND_MQTT);
		await("the client's configuration has arrived", () -> clientConfiguration() != null);
		settle();
		assertThat(client()).isNull();

		invoker("MQTT", "AMQP");

		await("the client is there", () -> client() != null);
		assertThat(publishedFlavors()).contains("REST", "MQTT", "AMQP");
	}

	@Test
	@DisplayName("the client goes when one of its invokers does, and comes back with it")
	void anInvokerThatGoesTakesTheClientWithIt() throws Exception {
		invoker("REST");
		ServiceRegistration<?> messaging = invoker("MQTT", "AMQP");
		configure("REST,MQTT", REST_AND_MQTT);
		await("the client is there", () -> client() != null);

		messaging.unregister();

		await("the client is gone", () -> client() == null);
		assertThat(publishedFlavors()).containsExactly("REST");

		invoker("MQTT", "AMQP");

		await("the client is back", () -> client() != null);
	}

	@Test
	@DisplayName("a client configured for REST alone does not wait for anything else")
	void oneFlavorOneInvoker() throws Exception {
		invoker("REST");
		configure("REST", InvokerProvider.REST_TARGET);

		await("the client is there", () -> client() != null);
	}

	@Test
	@DisplayName("the proxy factory needs the provider, not a particular invoker")
	void theFactoryDoesNotWait() throws Exception {
		await("the proxy factory is there", () -> context.getServiceReference(ServiceProxyFactory.class) != null);

		assertThat(publishedFlavors()).as("no invoker at all, and the factory is still a service").isEmpty();
	}

	// ------------------------------------------------------------------

	private ServiceRegistration<?> invoker(String... flavors) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(ServiceInvoker.FLAVOR_PROPERTY, flavors.length == 1 ? flavors[0] : flavors);
		// An object of its own each time: a lambda that captures nothing
		// is one instance, and two invokers must be two.
		ServiceInvoker invoker = new ServiceInvoker() {

			@Override
			public Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args) {
				return null;
			}
		};
		ServiceRegistration<?> registration = context.registerService(ServiceInvoker.class, invoker, properties);
		registered.add(registration);
		return registration;
	}

	private void configure(String flavors, String target) throws Exception {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("supported.flavors", flavors);
		properties.put("invokers.target", target);
		// No timers: the broker proxies of this test answer nothing.
		properties.put("session.interval.seconds", 0L);
		properties.put("provider.heartbeat.seconds", 0L);
		configuration = configurations.getConfiguration(CLIENT_PID, "?");
		configuration.update(properties);
	}

	private ServiceReference<DdsrClient> client() {
		return context.getServiceReference(DdsrClient.class);
	}

	/** The flavors the provider says it has an invoker for, right now. */
	private List<String> publishedFlavors() {
		ServiceReference<InvokerProvider> provider = context.getServiceReference(InvokerProvider.class);
		if (provider == null) {
			return List.of();
		}
		Object flavors = provider.getProperty(InvokerProvider.FLAVORS);
		return flavors instanceof String[] names ? List.of(names) : List.of();
	}

	/** The client's one component configuration, or null while it has none. */
	private ComponentConfigurationDTO clientConfiguration() {
		for (ComponentDescriptionDTO description : components.getComponentDescriptionDTOs()) {
			if (CLIENT_COMPONENT.equals(description.name)) {
				return components.getComponentConfigurationDTOs(description).stream().findFirst().orElse(null);
			}
		}
		return null;
	}

	private static void await(String what, BooleanSupplier condition) throws InterruptedException {
		long deadline = System.nanoTime() + 10_000_000_000L;
		while (!condition.getAsBoolean()) {
			assertThat(System.nanoTime()).as("waited ten seconds for: " + what).isLessThan(deadline);
			Thread.sleep(50);
		}
	}

	/** Time for DS to do what it would do if it were going to — before asserting that it did not. */
	private static void settle() throws InterruptedException {
		Thread.sleep(1000);
	}

	/** A service of the given type whose every method answers null, false or zero. */
	private static Object answeringNothing(Class<?> type) {
		return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
			switch (method.getName()) {
				case "hashCode":
					return System.identityHashCode(proxy);
				case "equals":
					return proxy == args[0];
				case "toString":
					return "fake " + type.getSimpleName();
				default:
					Class<?> returns = method.getReturnType();
					if (returns == boolean.class) {
						return false;
					}
					if (returns == long.class) {
						return 0L;
					}
					if (returns == int.class) {
						return 0;
					}
					return null;
			}
		});
	}
}
