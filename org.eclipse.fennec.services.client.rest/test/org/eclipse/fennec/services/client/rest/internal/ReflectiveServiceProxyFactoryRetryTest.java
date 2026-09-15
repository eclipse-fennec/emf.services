/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.client.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.client.TrackedServiceLocator;
import org.junit.jupiter.api.Test;

/** #59: a transport failure makes the proxy rebind once and retry once — nothing more. */
class ReflectiveServiceProxyFactoryRetryTest {

	interface Payment {
		double getBalance(String accountId);
	}

	private static final class ScriptedInvoker implements ServiceInvoker {
		final List<RuntimeException> failures = new ArrayList<>();
		int calls;

		@Override
		public Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args) {
			calls++;
			if (!failures.isEmpty()) {
				throw failures.remove(0);
			}
			return 42.0;
		}
	}

	/** A tracked locator whose rebind outcome the test scripts. */
	private static final class ScriptedLocator implements TrackedServiceLocator {
		private final ServiceImplementation implementation;
		private final ServiceReference reference;
		boolean rebindAnswer = true;
		final List<Boolean> rebindCalls = new ArrayList<>();

		ScriptedLocator() {
			ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
			si.setName("Payment");
			ServiceOperation op = ServicesFactory.eINSTANCE.createServiceOperation();
			op.setName("getBalance");
			Parameter p = ServicesFactory.eINSTANCE.createParameter();
			p.setName("accountId");
			p.setType("string");
			op.getParameters().add(p);
			si.getOperations().add(op);
			implementation = ServicesFactory.eINSTANCE.createServiceImplementation();
			implementation.setName("impl");
			implementation.getServiceInterfaces().add(si);
			RestFlavor rest = ServicesFactory.eINSTANCE.createRestFlavor();
			rest.setName("rest");
			rest.setHost("http://provider:9091");
			rest.setBasePath("/payments");
			RestOperationFlavor of = ServicesFactory.eINSTANCE.createRestOperationFlavor();
			of.setName("getBalance");
			of.setPath("/balance");
			of.setOperation(op);
			rest.getOperationFlavors().add(of);
			implementation.getFlavors().add(rest);
			reference = ServicesFactory.eINSTANCE.createServiceReference();
			reference.setId("ref-1");
		}

		@Override public ServiceReference reference() { return reference; }
		@Override public ServiceImplementation implementation() { return implementation; }
		@Override public Optional<RestFlavor> restFlavor() { return Optional.of((RestFlavor) implementation.getFlavors().get(0)); }
		@Override public Optional<URI> urlFor(String operationName) { return Optional.of(URI.create("http://provider:9091/payments/balance")); }
		@Override public State state() { return State.LIVE; }
		@Override public String interfaceName() { return "Payment"; }
		@Override public String filter() { return null; }

		@Override
		public boolean rebind(boolean excludeCurrent) {
			rebindCalls.add(excludeCurrent);
			return rebindAnswer;
		}
	}

	private static Payment proxy(ScriptedInvoker invoker, ServiceLocator locator) {
		ReflectiveServiceProxyFactory factory = new ReflectiveServiceProxyFactory();
		factory.invoker = invoker;
		return factory.newProxy(Payment.class, locator);
	}

	@Test
	void aTransportFailureRebindsAwayFromTheFailedRegistrationAndRetriesOnce() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		invoker.failures.add(DdsrException.transport("connect refused", new RuntimeException("ECONNREFUSED")));
		ScriptedLocator locator = new ScriptedLocator();

		double balance = proxy(invoker, locator).getBalance("acc-1");

		assertThat(balance).isEqualTo(42.0);
		assertThat(invoker.calls).isEqualTo(2);
		assertThat(locator.rebindCalls).as("excludeCurrent — never back to the one that failed").containsExactly(true);
	}

	@Test
	void aSecondTransportFailureIsTheCallers() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		invoker.failures.add(DdsrException.transport("timeout", null));
		invoker.failures.add(DdsrException.transport("timeout again", null));
		ScriptedLocator locator = new ScriptedLocator();

		assertThatThrownBy(() -> proxy(invoker, locator).getBalance("acc-1"))
				.isInstanceOf(DdsrException.class)
				.hasMessageContaining("timeout again");
		assertThat(invoker.calls).isEqualTo(2);
	}

	@Test
	void whenNothingElseIsRegisteredTheFailurePropagatesWithoutARetry() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		invoker.failures.add(DdsrException.transport("connect refused", null));
		ScriptedLocator locator = new ScriptedLocator();
		locator.rebindAnswer = false;

		assertThatThrownBy(() -> proxy(invoker, locator).getBalance("acc-1"))
				.isInstanceOf(DdsrException.class)
				.hasMessageContaining("connect refused");
		assertThat(invoker.calls).isEqualTo(1);
	}

	@Test
	void aBrokerOrApplicationFailureIsNeverRetried() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		invoker.failures.add(new DdsrException("HTTP 500 from the provider"));
		ScriptedLocator locator = new ScriptedLocator();

		assertThatThrownBy(() -> proxy(invoker, locator).getBalance("acc-1")).isInstanceOf(DdsrException.class);
		assertThat(invoker.calls).isEqualTo(1);
		assertThat(locator.rebindCalls).isEmpty();
	}

	@Test
	void anUntrackedLocatorGetsNoRetryEither() {
		ScriptedInvoker invoker = new ScriptedInvoker();
		invoker.failures.add(DdsrException.transport("connect refused", null));
		ScriptedLocator tracked = new ScriptedLocator();
		ServiceLocator plain = new ServiceLocator() {
			@Override public ServiceReference reference() { return tracked.reference(); }
			@Override public ServiceImplementation implementation() { return tracked.implementation(); }
			@Override public Optional<RestFlavor> restFlavor() { return tracked.restFlavor(); }
			@Override public Optional<URI> urlFor(String operationName) { return tracked.urlFor(operationName); }
		};

		assertThatThrownBy(() -> proxy(invoker, plain).getBalance("acc-1")).isInstanceOf(DdsrException.class);
		assertThat(invoker.calls).isEqualTo(1);
	}
}
