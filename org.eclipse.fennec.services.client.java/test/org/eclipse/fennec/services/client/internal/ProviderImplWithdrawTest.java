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

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.junit.jupiter.api.Test;

/**
 * #50: a withdraw must not depend on the live provider tree still being
 * serializable — it sends an identity stub the broker resolves by
 * (name, version).
 */
class ProviderImplWithdrawTest {

	private static final class CapturingImplementations implements BrokerImplementations {
		ServiceProvider withdrawnProvider;
		ServiceImplementation withdrawnImplementation;

		@Override
		public Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			throw new UnsupportedOperationException("not exercised");
		}

		@Override
		public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			withdrawnProvider = provider;
			withdrawnImplementation = implementation;
			Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
			d.setSeverity(DiagnosticSeverity.OK);
			return d;
		}

		@Override
		public Diagnostic modifyImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			throw new UnsupportedOperationException("not exercised");
		}

		@Override
		public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
			throw new UnsupportedOperationException("not exercised");
		}
	}

	private static ServiceProvider liveProvider() {
		ServiceInterface payment = ServicesFactory.eINSTANCE.createServiceInterface();
		payment.setName("Payment");
		payment.setVersion("1.0.0");
		// Parked in a catalog-URL resource like the payment example does …
		Resource catalogEntry = new ResourceImpl(URI.createURI("http://broker.test/ddsr/rest/catalog/Payment"));
		catalogEntry.getContents().add(payment);

		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("payments-java");
		provider.setVersion("1.0.0");
		provider.setSymbolicName("org.example.payments");
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName("payment-impl");
		impl.setVersion("1.0.0");
		impl.setImplementationId("org.example.PaymentImpl");
		impl.getServiceInterfaces().add(payment);
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName("rest");
		flavor.setBasePath("/payments");
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		return provider;
	}

	@Test
	void withdrawSendsADetachedIdentityStubNotTheLiveTree() {
		CapturingImplementations broker = new CapturingImplementations();
		ProviderImpl provider = new ProviderImpl(broker, null);
		ServiceProvider live = liveProvider();
		ServiceImplementation liveImpl = live.getImplementations().get(0);

		Diagnostic d = provider.withdrawInternal(live, liveImpl);

		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		ServiceProvider stub = broker.withdrawnProvider;
		assertThat(stub).isNotSameAs(live);
		assertThat(stub.eResource()).isNull();
		assertThat(stub.getName()).isEqualTo("payments-java");
		assertThat(stub.getVersion()).isEqualTo("1.0.0");
		assertThat(stub.getSymbolicName()).isEqualTo("org.example.payments");
		ServiceImplementation implStub = broker.withdrawnImplementation;
		assertThat(implStub).isSameAs(stub.getImplementations().get(0));
		assertThat(implStub.getName()).isEqualTo("payment-impl");
		assertThat(implStub.getVersion()).isEqualTo("1.0.0");
		assertThat(implStub.getImplementationId()).isEqualTo("org.example.PaymentImpl");
		assertThat(implStub.getServiceInterfaces())
				.as("no cross-reference that could dangle")
				.isEmpty();
		assertThat(implStub.getFlavors()).isEmpty();
	}

	@Test
	void theLiveObjectsAreLeftUntouched() {
		CapturingImplementations broker = new CapturingImplementations();
		ProviderImpl provider = new ProviderImpl(broker, null);
		ServiceProvider live = liveProvider();
		ServiceImplementation liveImpl = live.getImplementations().get(0);

		provider.withdrawInternal(live, liveImpl);

		assertThat(liveImpl.eContainer()).isSameAs(live);
		assertThat(liveImpl.getServiceInterfaces()).hasSize(1);
		assertThat(liveImpl.getFlavors()).hasSize(1);
	}

	@Test
	void aWithdrawStillWorksAfterTheInterfaceLostItsResource() {
		// What emf.osgi's ResourceSetPrototypeFactory.ungetService does to
		// the example's catalog-URL resource: contents cleared.
		CapturingImplementations broker = new CapturingImplementations();
		ProviderImpl provider = new ProviderImpl(broker, null);
		ServiceProvider live = liveProvider();
		ServiceImplementation liveImpl = live.getImplementations().get(0);
		liveImpl.getServiceInterfaces().get(0).eResource().getContents().clear();
		assertThat(liveImpl.getServiceInterfaces().get(0).eResource()).isNull();

		Diagnostic d = provider.withdrawInternal(live, liveImpl);

		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		assertThat(broker.withdrawnImplementation.getServiceInterfaces()).isEmpty();
	}
}
