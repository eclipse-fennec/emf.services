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

/** {@code Registration.update()} (#55): the live objects travel, the broker resolves by (name, version). */
class ProviderImplUpdateTest {

	private static final class CapturingImplementations implements BrokerImplementations {
		ServiceProvider modifiedProvider;
		ServiceImplementation modifiedImplementation;

		@Override
		public Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			throw new UnsupportedOperationException("not exercised");
		}

		@Override
		public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			throw new UnsupportedOperationException("not exercised");
		}

		@Override
		public Diagnostic modifyImplementation(ServiceProvider provider, ServiceImplementation implementation) {
			modifiedProvider = provider;
			modifiedImplementation = implementation;
			Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
			d.setSeverity(DiagnosticSeverity.OK);
			return d;
		}

		@Override
		public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
			throw new UnsupportedOperationException("not exercised");
		}
	}

	@Test
	void updateSendsTheLiveImplementationAsAModification() {
		CapturingImplementations broker = new CapturingImplementations();
		ProviderImpl owner = new ProviderImpl(broker, null);
		ServiceInterface payment = ServicesFactory.eINSTANCE.createServiceInterface();
		payment.setName("Payment");
		payment.setVersion("1.0.0");
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("payments-java");
		provider.setVersion("1.0.0");
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
		provider.getImplementations().add(impl);
		RegistrationImpl registration = new RegistrationImpl(owner, provider, impl, null, owner.synthOk());

		rest.setHost("http://elsewhere:9999");
		Diagnostic d = registration.update();

		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		assertThat(broker.modifiedProvider).isSameAs(provider);
		assertThat(broker.modifiedImplementation).isSameAs(impl);
		assertThat(((RestFlavor) broker.modifiedImplementation.getFlavors().get(0)).getHost())
				.as("what the provider changed on its model is what travels")
				.isEqualTo("http://elsewhere:9999");
	}
}
