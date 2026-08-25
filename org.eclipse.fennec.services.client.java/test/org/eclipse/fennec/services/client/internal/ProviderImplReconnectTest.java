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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.client.Registration;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;
import org.eclipse.fennec.services.fingerprint.ServiceImplementationFingerprint;
import org.junit.jupiter.api.Test;

/**
 * The idempotent provider reconnect (ACQUISITION.md §11.1): whether
 * {@code publish()} goes to the broker or reuses the registration the
 * broker already holds is decided purely by fingerprint comparison
 * against the reference decoration — im1 equal → skip and reuse; any
 * drift → publish (the broker retires the old entry).
 * <p>
 * Plain JUnit — broker stand-ins decorate their references exactly the
 * way {@code DdsrBrokerImpl.decorateReference} does (properties copied,
 * sd1 + im1 as string properties).
 */
class ProviderImplReconnectTest {

	private static final class FakeImplementations implements BrokerImplementations {

		int publishCalls;

		@Override
		public Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			publishCalls++;
			Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
			d.setSeverity(DiagnosticSeverity.OK);
			return d;
		}

		@Override
		public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
			d.setSeverity(DiagnosticSeverity.OK);
			return d;
		}

		@Override
		public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
			throw new UnsupportedOperationException("not exercised by this test");
		}
	}

	private static final class FakeLookup implements BrokerLookup {

		private final List<ServiceReference> references = new ArrayList<>();

		@Override
		public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
				ConsumerCapability capability) {
			return new ArrayList<>(references);
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

	// ------------------------------------------------------------------

	private static ServiceProvider provider(String name) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(name);
		return provider;
	}

	private static ServiceImplementation implementation(ServiceProvider owner, String host) {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName("Payment");
		si.setVersion("1.0.0");
		ServiceOperation charge = ServicesFactory.eINSTANCE.createServiceOperation();
		charge.setName("charge");
		charge.setReturnType("double");
		si.getOperations().add(charge);

		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName("payments-rest");
		impl.setImplementationId("test:payments:1.0.0");
		impl.getServiceInterfaces().add(si);

		RestFlavor rest = ServicesFactory.eINSTANCE.createRestFlavor();
		rest.setName("rest");
		rest.setHost(host);
		rest.setBasePath("/payments");
		RestOperationFlavor opFlavor = ServicesFactory.eINSTANCE.createRestOperationFlavor();
		opFlavor.setName("charge");
		opFlavor.setMethod(HttpMethod.POST);
		opFlavor.setPath("/charge");
		opFlavor.setOperation(charge);
		rest.getOperationFlavors().add(opFlavor);
		impl.getFlavors().add(rest);

		owner.getImplementations().add(impl);
		return impl;
	}

	/** Mirror of the broker's {@code decorateReference} for the fake. */
	private static ServiceReference brokerReference(ServiceProvider owner, ServiceImplementation impl) {
		ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId("held-" + owner.getName());
		ref.setProvider(owner);
		for (Property property : impl.getProperties()) {
			ref.getProperties().add(EcoreUtil.copy(property));
		}
		addString(ref, "ddsr.fingerprint",
				ServiceDescriptionFingerprint.fingerprint(impl.getServiceInterfaces().get(0)));
		addString(ref, "ddsr.impl.fingerprint", ServiceImplementationFingerprint.fingerprint(impl));
		return ref;
	}

	private static void addString(ServiceReference ref, String name, String value) {
		StringProperty property = ServicesFactory.eINSTANCE.createStringProperty();
		property.setName(name);
		property.setValue(value);
		ref.getProperties().add(property);
	}

	// ------------------------------------------------------------------

	@Test
	void firstPublishGoesToTheBroker() {
		FakeImplementations implementations = new FakeImplementations();
		ProviderImpl subject = new ProviderImpl(implementations, new FakeLookup());
		ServiceProvider self = provider("pay-provider");
		ServiceImplementation impl = implementation(self, "http://localhost:9090");

		subject.publish(self, impl);

		assertThat(implementations.publishCalls).isEqualTo(1);
	}

	@Test
	void reconnectWithIdenticalContentSkipsThePublish() {
		// what the broker held from before the provider restart
		ServiceProvider before = provider("pay-provider");
		ServiceImplementation heldImpl = implementation(before, "http://localhost:9090");
		FakeLookup lookup = new FakeLookup();
		lookup.references.add(brokerReference(before, heldImpl));

		FakeImplementations implementations = new FakeImplementations();
		ProviderImpl subject = new ProviderImpl(implementations, lookup);
		// the restarted provider builds the same model from scratch
		ServiceProvider self = provider("pay-provider");
		ServiceImplementation impl = implementation(self, "http://localhost:9090");

		Registration registration = subject.publish(self, impl);

		assertThat(implementations.publishCalls)
				.as("im1 match — the publish must be skipped")
				.isZero();
		assertThat(registration.reference().getId()).isEqualTo("held-pay-provider");
		assertThat(registration.diagnostic().getMessage()).contains("im1 match");
		assertThat(registration.diagnostic().getSeverity()).isEqualTo(DiagnosticSeverity.OK);
	}

	@Test
	void endpointDriftRepublishes() {
		ServiceProvider before = provider("pay-provider");
		ServiceImplementation heldImpl = implementation(before, "http://old-host:9090");
		FakeLookup lookup = new FakeLookup();
		lookup.references.add(brokerReference(before, heldImpl));

		FakeImplementations implementations = new FakeImplementations();
		ProviderImpl subject = new ProviderImpl(implementations, lookup);
		ServiceProvider self = provider("pay-provider");
		ServiceImplementation impl = implementation(self, "http://new-host:9090");

		subject.publish(self, impl);

		assertThat(implementations.publishCalls)
				.as("im1 differs (endpoint moved) — must re-publish")
				.isEqualTo(1);
	}

	@Test
	void contractDriftRepublishes() {
		ServiceProvider before = provider("pay-provider");
		ServiceImplementation heldImpl = implementation(before, "http://localhost:9090");
		// the held contract has an extra operation → different sd1
		ServiceOperation extra = ServicesFactory.eINSTANCE.createServiceOperation();
		extra.setName("refund");
		heldImpl.getServiceInterfaces().get(0).getOperations().add(extra);
		FakeLookup lookup = new FakeLookup();
		lookup.references.add(brokerReference(before, heldImpl));

		FakeImplementations implementations = new FakeImplementations();
		ProviderImpl subject = new ProviderImpl(implementations, lookup);
		ServiceProvider self = provider("pay-provider");
		ServiceImplementation impl = implementation(self, "http://localhost:9090");

		subject.publish(self, impl);

		assertThat(implementations.publishCalls)
				.as("sd1 differs (contract drift) — must re-publish")
				.isEqualTo(1);
	}

	@Test
	void foreignProvidersRegistrationIsNeverReused() {
		// identical content, but registered by someone else
		ServiceProvider other = provider("someone-else");
		ServiceImplementation heldImpl = implementation(other, "http://localhost:9090");
		FakeLookup lookup = new FakeLookup();
		lookup.references.add(brokerReference(other, heldImpl));

		FakeImplementations implementations = new FakeImplementations();
		ProviderImpl subject = new ProviderImpl(implementations, lookup);
		ServiceProvider self = provider("pay-provider");
		ServiceImplementation impl = implementation(self, "http://localhost:9090");

		subject.publish(self, impl);

		assertThat(implementations.publishCalls)
				.as("another provider's identical registration must not be adopted")
				.isEqualTo(1);
	}
}
