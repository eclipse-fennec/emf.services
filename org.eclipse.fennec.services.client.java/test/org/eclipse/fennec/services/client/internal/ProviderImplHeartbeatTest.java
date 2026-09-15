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
package org.eclipse.fennec.services.client.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.client.Registration;
import org.junit.jupiter.api.Test;

/**
 * Provider liveness on the SDK side (#52): every live registration
 * heartbeats, a registration the broker lost is published again and the
 * application's handle follows the fresh reference.
 */
class ProviderImplHeartbeatTest {

	private static final class FakeImplementations implements BrokerImplementations {

		final List<String> heartbeats = new ArrayList<>();
		final List<Long> intervals = new ArrayList<>();
		final Deque<Diagnostic> heartbeatAnswers = new ArrayDeque<>();
		int publishCalls;
		/** What the broker lists after a publish: the lookup fake mirrors it. */
		FakeLookup lookup;
		String assignOnPublish = "ref-1";

		@Override
		public Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			publishCalls++;
			lookup.held = assignOnPublish;
			return ok("published");
		}

		@Override
		public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			return ok("withdrawn");
		}

		@Override
		public Diagnostic modifyImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			throw new UnsupportedOperationException("not exercised");
		}

		@Override
		public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
			throw new UnsupportedOperationException("not exercised");
		}

		@Override
		public Diagnostic heartbeat(String referenceId, long intervalSeconds) {
			heartbeats.add(referenceId);
			intervals.add(intervalSeconds);
			return heartbeatAnswers.isEmpty() ? ok("heartbeat accepted") : heartbeatAnswers.pop();
		}
	}

	/** Lists the one reference the broker currently holds for the provider — none when {@code held} is null. */
	private static final class FakeLookup implements BrokerLookup {

		String held;
		private final ServiceProvider provider;

		FakeLookup(ServiceProvider provider) {
			this.provider = provider;
		}

		@Override
		public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
				ConsumerCapability capability) {
			if (held == null) {
				return List.of();
			}
			ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
			ref.setId(held);
			ref.setProvider(provider);
			return List.of(ref);
		}

		@Override
		public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
				ConsumerCapability capability) {
			return getServiceReferences(interfaceName, filter, capability);
		}

		@Override
		public ServiceImplementation getImplementationForReference(ServiceReference reference) {
			return null;
		}

		@Override
		public ServiceReference getServiceReference(String referenceId) {
			return null;
		}
	}

	@Test
	void everyLiveRegistrationHeartbeatsWithItsReferenceId() {
		ServiceProvider provider = provider("payments-java");
		ServiceImplementation impl = implementation(provider);
		FakeImplementations broker = new FakeImplementations();
		FakeLookup lookup = new FakeLookup(provider);
		broker.lookup = lookup;
		ProviderImpl owner = new ProviderImpl(broker, lookup);

		Registration registration = owner.publish(provider, impl);
		assertThat(registration.reference().getId()).isEqualTo("ref-1");

		assertThat(owner.heartbeatAll(30)).isEqualTo(1);
		assertThat(broker.heartbeats).containsExactly("ref-1");
		assertThat(broker.intervals).containsExactly(30L);
	}

	@Test
	void aRegistrationTheBrokerLostIsPublishedAgainAndTheHandleFollows() {
		ServiceProvider provider = provider("payments-java");
		ServiceImplementation impl = implementation(provider);
		FakeImplementations broker = new FakeImplementations();
		FakeLookup lookup = new FakeLookup(provider);
		broker.lookup = lookup;
		broker.assignOnPublish = "ref-before-restart";
		ProviderImpl owner = new ProviderImpl(broker, lookup);
		Registration registration = owner.publish(provider, impl);
		assertThat(broker.publishCalls).isEqualTo(1);
		assertThat(registration.reference().getId()).isEqualTo("ref-before-restart");

		// The broker restarted: it neither holds our reference (404 on the
		// heartbeat) nor anything under our name (the reconnect lookup is
		// empty), then assigns a new reference on the publish.
		broker.heartbeatAnswers.add(error(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED, "no live registration"));
		lookup.held = null;
		broker.assignOnPublish = "ref-after-restart";

		assertThat(owner.heartbeatAll(30)).as("the fresh registration is acknowledged").isEqualTo(1);

		assertThat(broker.publishCalls).as("published again").isEqualTo(2);
		assertThat(broker.heartbeats).containsExactly("ref-before-restart", "ref-after-restart");
		assertThat(registration.reference().getId()).as("same handle, new reference").isEqualTo("ref-after-restart");
		assertThat(owner.liveRegistrationCount()).isEqualTo(1);
	}

	@Test
	void otherRefusalsAreLoggedNotRepublished() {
		ServiceProvider provider = provider("payments-java");
		ServiceImplementation impl = implementation(provider);
		FakeImplementations broker = new FakeImplementations();
		FakeLookup lookup = new FakeLookup(provider);
		broker.lookup = lookup;
		ProviderImpl owner = new ProviderImpl(broker, lookup);
		owner.publish(provider, impl);
		broker.heartbeatAnswers.add(error(DdsrDiagnostics.CODE_HEARTBEAT_INVALID, "bad interval"));

		assertThat(owner.heartbeatAll(30)).isZero();
		assertThat(broker.publishCalls).isEqualTo(1);
		assertThat(broker.heartbeats).containsExactly("ref-1");
	}

	@Test
	void aWithdrawnRegistrationStopsHeartbeating() {
		ServiceProvider provider = provider("payments-java");
		ServiceImplementation impl = implementation(provider);
		FakeImplementations broker = new FakeImplementations();
		FakeLookup lookup = new FakeLookup(provider);
		broker.lookup = lookup;
		ProviderImpl owner = new ProviderImpl(broker, lookup);
		Registration registration = owner.publish(provider, impl);

		assertThat(registration.withdraw().getSeverity()).isEqualTo(DiagnosticSeverity.OK);

		assertThat(owner.heartbeatAll(30)).isZero();
		assertThat(broker.heartbeats).isEmpty();
		assertThat(owner.liveRegistrationCount()).isZero();
	}

	// ------------------------------------------------------------------

	private static ServiceProvider provider(String name) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(name);
		provider.setVersion("1.0.0");
		return provider;
	}

	private static ServiceImplementation implementation(ServiceProvider owner) {
		ServiceInterface payment = ServicesFactory.eINSTANCE.createServiceInterface();
		payment.setName("Payment");
		payment.setVersion("1.0.0");
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName("payment-impl");
		impl.setVersion("1.0.0");
		impl.setImplementationId("org.example.PaymentImpl");
		impl.getServiceInterfaces().add(payment);
		RestFlavor rest = ServicesFactory.eINSTANCE.createRestFlavor();
		rest.setName("rest");
		rest.setBasePath("/payments");
		rest.setHost("http://localhost:9091");
		impl.getFlavors().add(rest);
		owner.getImplementations().add(impl);
		return impl;
	}

	private static Diagnostic ok(String message) {
		Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
		d.setSeverity(DiagnosticSeverity.OK);
		d.setMessage(message);
		return d;
	}

	private static Diagnostic error(int code, String message) {
		Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
		d.setSeverity(DiagnosticSeverity.ERROR);
		d.setCode(code);
		d.setMessage(message);
		return d;
	}
}
