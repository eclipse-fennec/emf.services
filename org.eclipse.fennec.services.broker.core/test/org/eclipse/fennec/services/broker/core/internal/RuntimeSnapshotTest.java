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
import java.time.Instant;
import java.util.List;

import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.common.CallOrigin;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.runtime.BrokerRuntimeDTO;
import org.eclipse.fennec.services.runtime.RegistrationDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the broker says about itself (#126).
 *
 * <p>Two things are worth testing and the rest is plumbing: that the
 * snapshot reports the things nobody else can see — who holds a lease,
 * what the event delivery owes — and that the changeCount moves when the
 * state does, because that number is the whole of the "ask again"
 * protocol.
 */
class RuntimeSnapshotTest {

	@TempDir
	Path tmp;

	private DdsrBrokerImpl broker;

	@BeforeEach
	void setUp() {
		broker = new DdsrBrokerImpl(tmp.resolve("broker-state.xmi"), new InMemoryLookupBackend());
	}

	@Test
	@DisplayName("an empty broker says so, rather than saying nothing")
	void emptyBroker() {
		BrokerRuntimeDTO snapshot = broker.runtimeSnapshot();

		assertThat(snapshot.registrations).isEmpty();
		assertThat(snapshot.catalog).isEmpty();
		assertThat(snapshot.sessions).isEmpty();
		assertThat(snapshot.coldEntries).isZero();
		assertThat(snapshot.delivery.dropped).isZero();
		assertThat(snapshot.delivery.owesResync).isFalse();
		assertThat(snapshot.takenAt).isPositive();
	}

	@Test
	@DisplayName("a registration is reported with its contract, flavor and fingerprints")
	void registrationIsReported() {
		broker.addCatalogEntry(contract(), "test");
		ServiceProvider provider = provider("payments", "payments-rest");
		broker.publishImplementation(provider, provider.getImplementations().get(0));

		BrokerRuntimeDTO snapshot = broker.runtimeSnapshot();

		assertThat(snapshot.registrations).hasSize(1);
		RegistrationDTO registration = snapshot.registrations.get(0);
		assertThat(registration.providerName).isEqualTo("payments");
		assertThat(registration.contracts).containsExactly("Payment");
		assertThat(registration.flavors).containsExactly(FlavorKind.REST.getName());
		assertThat(registration.referenceId).isNotBlank();
		assertThat(registration.contractFingerprint)
			.as("sd1, as the broker decorated it — the same string a consumer addresses by")
			.startsWith("sd1:");
		assertThat(registration.implementationFingerprint).startsWith("im1:");
		assertThat(registration.heldBy).isEmpty();
	}

	@Test
	@DisplayName("the lease is reported from the registration's side, which is the side that is asked")
	void leasesAreReported() {
		broker.addCatalogEntry(contract(), "test");
		ServiceProvider provider = provider("payments", "payments-rest");
		broker.publishImplementation(provider, provider.getImplementations().get(0));
		String referenceId = broker.getServiceReferences("Payment", null, null).stream()
				.map(ServiceReference::getId)
				.findFirst()
				.orElseThrow();

		ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
		session.setConsumerId("consumer-7");
		broker.putSession(session, List.of(referenceId));

		BrokerRuntimeDTO snapshot = broker.runtimeSnapshot();

		assertThat(snapshot.registrations.get(0).heldBy).containsExactly("consumer-7");
		assertThat(snapshot.sessions).hasSize(1);
		assertThat(snapshot.sessions.get(0).consumerId).isEqualTo("consumer-7");
		assertThat(snapshot.sessions.get(0).acquisitions).containsExactly(referenceId);
		assertThat(snapshot.sessions.get(0).lastRenewal).isPositive();
	}

	@Test
	@DisplayName("a session says where it reached the broker from, so a lease can be matched to a span (#166)")
	void sessionCarriesItsOrigin() {
		ClientOrigin origin = ClientOrigin.of("ops-console", "11112222-3333-4444-5555-666677778888");
		CallOrigin.set(origin);
		ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
		session.setConsumerId("consumer-8");
		broker.putSession(session, List.of());
		CallOrigin.clear();

		BrokerRuntimeDTO snapshot = broker.runtimeSnapshot();

		assertThat(snapshot.sessions).singleElement()
				.satisfies(dto -> assertThat(dto.origin).isEqualTo(origin.token()));
	}

	@AfterEach
	void forgetOrigin() {
		CallOrigin.clear();
	}

	@Test
	@DisplayName("a deprecated contract says it, and says how many still serve it")
	void catalogIsReported() {
		ServiceInterface contract = contract();
		broker.addCatalogEntry(contract, "test");
		ServiceProvider provider = provider("payments", "payments-rest");
		broker.publishImplementation(provider, provider.getImplementations().get(0));
		broker.deprecateCatalogEntry(contract, "test");

		BrokerRuntimeDTO snapshot = broker.runtimeSnapshot();

		assertThat(snapshot.catalog).hasSize(1);
		assertThat(snapshot.catalog.get(0).name).isEqualTo("Payment");
		assertThat(snapshot.catalog.get(0).status).isEqualTo(CatalogStatus.DEPRECATED.getName());
		assertThat(snapshot.catalog.get(0).fingerprint).startsWith("sd1:");
		assertThat(snapshot.catalog.get(0).implementations)
			.as("a deprecation hides nothing that is already running")
			.isEqualTo(1);
	}

	@Test
	@DisplayName("the changeCount moves when the state does, and only then")
	void changeCountMovesWithTheState() {
		long start = broker.runtimeChangeCount();

		broker.runtimeSnapshot();
		assertThat(broker.runtimeChangeCount())
			.as("reading is not a change")
			.isEqualTo(start);

		broker.addCatalogEntry(contract(), "test");
		long afterCatalog = broker.runtimeChangeCount();
		assertThat(afterCatalog).isGreaterThan(start);

		ServiceProvider provider = provider("payments", "payments-rest");
		broker.publishImplementation(provider, provider.getImplementations().get(0));
		assertThat(broker.runtimeChangeCount()).isGreaterThan(afterCatalog);

		long beforeSweep = broker.runtimeChangeCount();
		broker.expireSessions(Instant.now());
		assertThat(broker.runtimeChangeCount())
			.as("a sweep that found nothing still passed through the state, and says so")
			.isGreaterThan(beforeSweep);
	}

	@Test
	@DisplayName("a withdrawn registration is gone from the snapshot, not merely marked")
	void withdrawnRegistrationsAreGone() {
		broker.addCatalogEntry(contract(), "test");
		ServiceProvider provider = provider("payments", "payments-rest");
		ServiceImplementation implementation = provider.getImplementations().get(0);
		broker.publishImplementation(provider, implementation);

		broker.withdrawImplementation(provider, implementation);

		assertThat(broker.runtimeSnapshot().registrations).isEmpty();
	}

	private static ServiceInterface contract() {
		ServiceInterface contract = ServicesFactory.eINSTANCE.createServiceInterface();
		contract.setName("Payment");
		contract.setVersion("1.0.0");
		ServiceOperation charge = ServicesFactory.eINSTANCE.createServiceOperation();
		charge.setName("charge");
		contract.getOperations().add(charge);
		return contract;
	}

	private static ServiceProvider provider(String providerName, String implementationName) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(providerName);
		provider.setVersion("1.0.0");
		ServiceImplementation implementation = ServicesFactory.eINSTANCE.createServiceImplementation();
		implementation.setName(implementationName);
		implementation.setVersion("1.0.0");
		implementation.setImplementationId(providerName + ":Payment:1.0.0");
		implementation.getServiceInterfaces().add(contract());
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName(implementationName);
		flavor.setKind(FlavorKind.REST);
		flavor.setBasePath("/payments");
		implementation.getFlavors().add(flavor);
		provider.getImplementations().add(implementation);
		return provider;
	}
}
