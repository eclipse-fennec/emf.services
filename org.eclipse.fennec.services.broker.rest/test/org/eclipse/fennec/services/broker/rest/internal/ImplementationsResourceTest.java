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
package org.eclipse.fennec.services.broker.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.body;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.bytes;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.diagnostic;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.payment;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.provider;

import java.io.IOException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** POST /implementations, POST /implementations/withdraw, DELETE /implementations (#56). */
class ImplementationsResourceTest {

	private static final class FakeImplementations implements BrokerImplementations {
		String lastCall;
		ServiceProvider provider;
		ServiceImplementation implementation;
		Diagnostic answer = diagnostic(DiagnosticSeverity.OK, 0, "ok");

		@Override
		public Diagnostic publishImplementation(ServiceProvider p, ServiceImplementation i) {
			lastCall = "publish"; provider = p; implementation = i;
			return answer;
		}

		@Override
		public Diagnostic withdrawImplementation(ServiceProvider p, ServiceImplementation i) {
			lastCall = "withdraw"; provider = p; implementation = i;
			return answer;
		}

		@Override
		public Diagnostic modifyImplementation(ServiceProvider p, ServiceImplementation i) {
			lastCall = "modify"; provider = p; implementation = i;
			return answer;
		}

		@Override
		public ServiceRegistration registerService(ServiceProvider p, ServiceImplementation i) {
			throw new UnsupportedOperationException("local-style, not reachable over REST");
		}
	}

	private final FakeImplementations broker = new FakeImplementations();
	private final RestTestSupport.ResourceSets resourceSets = new RestTestSupport.ResourceSets();
	private ImplementationsResource resource;

	@BeforeEach
	void setUp() {
		resource = new ImplementationsResource();
		resource.broker = broker;
		resource.rsObjects = resourceSets;
	}

	@Test
	void publishHandsTheParsedProviderAndItsSoleImplementationToTheBroker() throws IOException {
		ServiceInterface payment = payment();
		ServiceProvider provider = provider("payments", payment, FlavorKind.REST);

		Response r = resource.publish(body(resourceSets, provider, payment));

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(broker.lastCall).isEqualTo("publish");
		assertThat(broker.provider.getName()).isEqualTo("payments");
		assertThat(broker.implementation).isSameAs(broker.provider.getImplementations().get(0));
		assertThat(broker.implementation.getServiceInterfaces().get(0).getName())
				.as("the interface stub sent as sibling root is wired to the implementation")
				.isEqualTo("Payment");
		assertThat(r.getEntity()).isInstanceOf(Diagnostic.class);
	}

	@Test
	void aBodyWithoutAProviderRootIsRejected() throws IOException {
		Response r = resource.publish(body(resourceSets, payment()));
		assertThat(r.getStatus()).isEqualTo(400);
		assertThat(broker.lastCall).isNull();
	}

	@Test
	void aProviderWithTwoImplementationsIsRejected() throws IOException {
		ServiceInterface payment = payment();
		ServiceProvider provider = provider("payments", payment, FlavorKind.REST);
		ServiceImplementation second = ServicesFactory.eINSTANCE.createServiceImplementation();
		second.setName("second");
		second.setVersion("1.0.0");
		second.setImplementationId("x");
		provider.getImplementations().add(second);

		assertThat(resource.publish(body(resourceSets, provider, payment)).getStatus()).isEqualTo(400);
	}

	@Test
	void withdrawViaPostAndViaDeleteBothReachTheBroker() throws IOException {
		ServiceProvider stub = provider("payments", null);

		assertThat(resource.withdrawViaPost(body(resourceSets, stub)).getStatus()).isEqualTo(200);
		assertThat(broker.lastCall).isEqualTo("withdraw");
		assertThat(broker.implementation.getServiceInterfaces()).as("an identity stub is enough (#50)").isEmpty();

		broker.lastCall = null;
		assertThat(resource.withdraw(body(resourceSets, stub)).getStatus()).isEqualTo(200);
		assertThat(broker.lastCall).isEqualTo("withdraw");
	}

	@Test
	void modifyIsAPutWithThePublishBodyShape() throws IOException {
		ServiceInterface payment = payment();
		ServiceProvider provider = provider("payments", payment, FlavorKind.REST);

		Response r = resource.modify(body(resourceSets, provider, payment));

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(broker.lastCall).isEqualTo("modify");
		assertThat(broker.implementation.getServiceInterfaces().get(0).getName()).isEqualTo("Payment");

		broker.answer = diagnostic(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_IMPL_CONTRACT_CHANGED, "contract");
		assertThat(resource.modify(body(resourceSets, provider, payment)).getStatus()).as("a contract change is a conflict").isEqualTo(409);
	}

	@Test
	void brokerDiagnosticsMapToHttpStatuses() throws IOException {
		ServiceProvider stub = provider("payments", null);
		broker.answer = diagnostic(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED, "gone");
		assertThat(resource.withdrawViaPost(body(resourceSets, stub)).getStatus()).isEqualTo(404);

		broker.answer = diagnostic(DiagnosticSeverity.WARNING, DdsrDiagnostics.CODE_INTERFACE_DEPRECATED, "old");
		assertThat(resource.publish(body(resourceSets, stub)).getStatus()).as("a warning is still a success").isEqualTo(200);
	}

	@Test
	void malformedIs400AndOversizedIs413() {
		assertThatThrownBy(() -> resource.publish(bytes("<services:ServiceProvider")))
				.isInstanceOf(WebApplicationException.class)
				.extracting(e -> ((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400);
		byte[] huge = new byte[org.eclipse.fennec.services.xmi.codec.WireBody.MAX_BYTES + 1];
		assertThatThrownBy(() -> resource.publish(new java.io.ByteArrayInputStream(huge)))
				.isInstanceOf(WebApplicationException.class)
				.extracting(e -> ((WebApplicationException) e).getResponse().getStatus()).isEqualTo(413);
		assertThat(resourceSets.outstanding).as("released even on rejection").isZero();
	}
}
