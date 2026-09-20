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
package org.eclipse.fennec.services.broker.core.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A broker outside OSGi does what a broker inside OSGi does.
 *
 * <p>It did not. The periodic work — advancing a handover, retiring a
 * silent provider, expiring a session, parking an idle registration —
 * lived in the Declarative Services component, so a plain Java caller
 * got a registry that answered lookups and never moved on its own.
 * These tests hold the two together: the same settings on the same
 * object, and the sweeps run.
 */
class BrokerWithoutOsgiTest {

	@TempDir
	Path tmp;

	@Test
	@DisplayName("a broker built in plain Java expires a session on its own")
	void theSweepsRunWithoutAFramework() throws InterruptedException {
		BrokerSettings settings = new BrokerSettings(tmp.resolve("broker-state.xmi"),
				1, 0, 0, 0, 0, UpdatePolicies.DEFAULT_CUTOVER_GRACE_MILLIS);
		DdsrBrokerImpl broker = new DdsrBrokerImpl(settings, new InMemoryLookupBackend(), null);
		try {
			broker.activate();
			ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
			session.setConsumerId("consumer-a");
			assertThat(broker.putSession(session, List.of()).getSeverity()).isEqualTo(DiagnosticSeverity.OK);
			assertThat(broker.sessionCount()).isEqualTo(1);

			// One second of expiry, swept at a quarter of it: nothing here
			// calls expireSessions, the broker does. Polled rather than
			// slept through, so the test ends as soon as it is true.
			long deadline = System.currentTimeMillis() + 15_000;
			while (broker.sessionCount() > 0 && System.currentTimeMillis() < deadline) {
				Thread.sleep(100);
			}
			assertThat(broker.sessionCount())
					.as("the broker expired it without anybody asking").isZero();
		} finally {
			broker.close();
		}
	}

	@Test
	@DisplayName("settings with every sweep off give a broker that only answers")
	void nothingSweepsWhenNothingIsEnabled() {
		BrokerSettings settings = new BrokerSettings(tmp.resolve("broker-state.xmi"),
				0, 0, 0, 0, 0, UpdatePolicies.DEFAULT_CUTOVER_GRACE_MILLIS);
		DdsrBrokerImpl broker = new DdsrBrokerImpl(settings, new InMemoryLookupBackend(), null);
		try {
			broker.activate();
			ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
			session.setConsumerId("consumer-a");
			broker.putSession(session, List.of());

			// Nothing is scheduled, so nothing can take it away.
			assertThat(broker.sessionCount()).isEqualTo(1);
		} finally {
			broker.close();
		}
	}

	@Test
	@DisplayName("closing saves the registry, so a fresh broker finds what the last one held")
	void closingSavesWhatWasPublished() {
		Path snapshot = tmp.resolve("broker-state.xmi");
		BrokerSettings settings = new BrokerSettings(snapshot, 0, 0, 0, 0, 0,
				UpdatePolicies.DEFAULT_CUTOVER_GRACE_MILLIS);

		DdsrBrokerImpl first = new DdsrBrokerImpl(settings, new InMemoryLookupBackend(), null);
		first.activate();
		ServiceInterface payment = serviceInterface();
		first.addCatalogEntry(payment, "test");
		ServiceProvider provider = provider(payment);
		Diagnostic published = first.publishImplementation(provider, provider.getImplementations().get(0));
		assertThat(published.getSeverity().getValue()).isLessThan(DiagnosticSeverity.ERROR_VALUE);
		first.close();

		DdsrBrokerImpl second = new DdsrBrokerImpl(settings, new InMemoryLookupBackend(), null);
		try {
			second.activate();
			assertThat(second.getServiceReferences("Payment", null, null)).hasSize(1);
		} finally {
			second.close();
		}
	}

	private static ServiceInterface serviceInterface() {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName("Payment");
		si.setVersion("1.0.0");
		ServiceOperation operation = ServicesFactory.eINSTANCE.createServiceOperation();
		operation.setName("charge");
		si.getOperations().add(operation);
		return si;
	}

	private static ServiceProvider provider(ServiceInterface payment) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("payments");
		provider.setVersion("1.0.0");
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName("payment-impl");
		impl.setVersion("1.0.0");
		impl.setImplementationId("org.example.PaymentImpl");
		impl.getServiceInterfaces().add(payment);
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName("rest");
		flavor.setHost("http://localhost:9090");
		flavor.setBasePath("/payments");
		for (ServiceOperation op : payment.getOperations()) {
			RestOperationFlavor of = ServicesFactory.eINSTANCE.createRestOperationFlavor();
			of.setName(op.getName());
			of.setPath("/" + op.getName());
			of.setOperation(op);
			flavor.getOperationFlavors().add(of);
		}
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		return provider;
	}
}
