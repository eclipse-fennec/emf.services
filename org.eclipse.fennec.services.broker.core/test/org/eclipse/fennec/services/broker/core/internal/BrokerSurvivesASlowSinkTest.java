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

package org.eclipse.fennec.services.broker.core.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * The reason #124 exists: one subscriber that stops reading must not
 * stop the registry.
 *
 * <p>Until the delivery moved off the write lock, an SSE send into a
 * stalled TCP connection or an MQTT publish waiting at the in-flight
 * limit ran inside the lock that every publish, every withdraw and
 * every lookup needs. This test holds a sink still and asks the broker
 * to keep working.
 *
 * <p>Deliberately not a race: the sink is held on a latch, so "the
 * subscriber is stuck" is a fact for the whole test rather than a
 * window someone hopes to hit. The check that it was still stuck at the
 * end is what makes the passing calls mean something — without it the
 * test would also pass if the sink had simply been quick.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class BrokerSurvivesASlowSinkTest {

	@TempDir
	Path tmp;

	private final CountDownLatch inSink = new CountDownLatch(1);

	private final CountDownLatch release = new CountDownLatch(1);

	private DdsrBrokerImpl broker;

	@BeforeEach
	void setUp() {
		EventSink stuck = event -> {
			inSink.countDown();
			try {
				release.await();
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			}
		};
		broker = new DdsrBrokerImpl(tmp.resolve("broker-state.xmi"), new InMemoryLookupBackend(), stuck);
		broker.addCatalogEntry(contract(), "test");
	}

	@AfterEach
	void tearDown() {
		release.countDown();
		broker.close();
	}

	@Test
	@DisplayName("a subscriber stuck in the middle of an event does not stop publishing")
	void aStuckSubscriberDoesNotStopPublishing() throws InterruptedException {
		ServiceProvider first = provider("acme", "payments-1");
		broker.publishImplementation(first, first.getImplementations().get(0));
		assertThat(inSink.await(10, TimeUnit.SECONDS))
				.as("the delivery thread should be inside the sink by now")
				.isTrue();

		// The subscriber is stuck. The registry is not.
		ServiceProvider second = provider("other", "payments-2");
		Diagnostic published = broker.publishImplementation(second, second.getImplementations().get(0));

		assertThat(published.getSeverity()).as(published.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		assertThat(release.getCount())
				.as("and it got through WITHOUT the subscriber ever being released")
				.isEqualTo(1);
	}

	@Test
	@DisplayName("a stuck subscriber does not stop lookups, which is the hot path")
	void aStuckSubscriberDoesNotStopLookups() throws InterruptedException {
		ServiceProvider p = provider("acme", "payments-1");
		broker.publishImplementation(p, p.getImplementations().get(0));
		assertThat(inSink.await(10, TimeUnit.SECONDS)).isTrue();

		List<ServiceReference> found = broker.getServiceReferences("Payment", null, null);

		assertThat(found).hasSize(1);
		assertThat(release.getCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("a stuck subscriber does not stop a withdraw, so a provider can still shut down cleanly")
	void aStuckSubscriberDoesNotStopWithdrawing() throws InterruptedException {
		ServiceProvider p = provider("acme", "payments-1");
		broker.publishImplementation(p, p.getImplementations().get(0));
		assertThat(inSink.await(10, TimeUnit.SECONDS)).isTrue();

		Diagnostic withdrawn = broker.withdrawImplementation(p, p.getImplementations().get(0));

		assertThat(withdrawn.getSeverity()).as(withdrawn.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		assertThat(broker.getServiceReferences("Payment", null, null)).isEmpty();
		assertThat(release.getCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("the catalog stays usable too — a stuck subscriber is not a global outage")
	void aStuckSubscriberDoesNotStopTheCatalog() throws InterruptedException {
		ServiceProvider p = provider("acme", "payments-1");
		broker.publishImplementation(p, p.getImplementations().get(0));
		assertThat(inSink.await(10, TimeUnit.SECONDS)).isTrue();

		ServiceInterface another = contract();
		another.setName("Shipping");
		Diagnostic added = broker.addCatalogEntry(another, "test");

		assertThat(added.getSeverity()).as(added.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		assertThat(release.getCount()).isEqualTo(1);
	}

	// ------------------------------------------------------------------
	// Fixtures
	// ------------------------------------------------------------------

	private static ServiceInterface contract() {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName("Payment");
		si.setVersion("1.0.0");
		ServiceOperation charge = ServicesFactory.eINSTANCE.createServiceOperation();
		charge.setName("charge");
		si.getOperations().add(charge);
		return si;
	}

	private static ServiceProvider provider(String providerName, String implName) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(providerName);
		provider.setVersion("1.0.0");
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(implName);
		impl.setVersion("1.0.0");
		impl.getServiceInterfaces().add(contract());
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName(implName);
		flavor.setBasePath("/payments");
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		return provider;
	}
}
