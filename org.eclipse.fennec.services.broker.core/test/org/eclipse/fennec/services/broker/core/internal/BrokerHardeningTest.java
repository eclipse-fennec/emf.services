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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.broker.core.ContractAddressing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the broker does under the conditions that were only reachable
 * through the façade, and therefore never checked.
 *
 * <p>The split (#110) made each of these a question to one component,
 * and writing them down is the other half of the exercise: a
 * refactoring that only moves code proves nothing about the code it
 * moved.
 */
class BrokerHardeningTest {

	private static final String INTERFACE = "Payment";

	@TempDir
	Path tmp;

	private final RecordingEventSink sink = new RecordingEventSink();

	private DdsrBrokerImpl broker;

	private ServiceInterface payment;

	@BeforeEach
	void setUp() {
		broker = new DdsrBrokerImpl(tmp.resolve("broker-state.xmi"), new InMemoryLookupBackend(), sink);
		payment = serviceInterface(INTERFACE);
		broker.addCatalogEntry(payment, "test");
		sink.received.clear();
	}

	// ------------------------------------------------------------------
	// Catalog governance under a lock (the two gaps closed in #110)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("deprecating by name and fingerprint records the reason on the live entry")
	void deprecationByFingerprintCarriesGovernance() {
		String fingerprint = ContractAddressing.fingerprint(liveEntry());
		ServiceInterface governance = ServicesFactory.eINSTANCE.createServiceInterface();
		governance.setName(INTERFACE);
		governance.setDeprecationReason("superseded by Payment 2");

		Diagnostic d = broker.deprecateCatalogEntry(INTERFACE, fingerprint, governance, "test");

		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		assertThat(liveEntry().getStatus()).isEqualTo(CatalogStatus.DEPRECATED);
		assertThat(liveEntry().getDeprecationReason()).isEqualTo("superseded by Payment 2");
	}

	@Test
	@DisplayName("a deprecation that cannot be saved leaves the entry exactly as it was")
	void deprecationRollsBackItsGovernanceToo() throws Exception {
		String fingerprint = ContractAddressing.fingerprint(liveEntry());
		ServiceInterface governance = ServicesFactory.eINSTANCE.createServiceInterface();
		governance.setName(INTERFACE);
		governance.setDeprecationReason("this must not survive");

		// Make the snapshot unwritable: the save fails, and everything the
		// operation touched has to be put back. The governance fields used
		// to be written before the lock was even taken, so they stayed.
		Path snapshot = tmp.resolve("broker-state.xmi");
		Files.deleteIfExists(snapshot);
		Files.createDirectory(snapshot);

		Diagnostic d = broker.deprecateCatalogEntry(INTERFACE, fingerprint, governance, "test");

		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.ERROR);
		assertThat(liveEntry().getStatus()).as("status rolled back").isNotEqualTo(CatalogStatus.DEPRECATED);
		assertThat(liveEntry().getDeprecationReason()).as("and so did the reason").isNull();
	}

	// ------------------------------------------------------------------
	// Lookup
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a capability nobody satisfies returns nothing rather than everything")
	void aCapabilityThatMatchesNothingMatchesNothing() {
		publish("1.0.0");
		ConsumerCapability capability = ServicesFactory.eINSTANCE.createConsumerCapability();
		StringProperty wanted = ServicesFactory.eINSTANCE.createStringProperty();
		wanted.setName("ddsr.fingerprint");
		wanted.setValue("sd1:this-is-not-a-contract-anybody-has");
		capability.getProperties().add(wanted);

		assertThat(broker.getServiceReferences(INTERFACE, null, capability)).isEmpty();
		assertThat(broker.getAllServiceReferences(INTERFACE, null, capability)).isEmpty();
	}

	@Test
	@DisplayName("a deprecated contract is still served: deprecation says do not start, not stop")
	void aDeprecatedEntryStillAnswersItsFingerprint() {
		publish("1.0.0");
		String fingerprint = broker.getServiceReferences(INTERFACE, null, null).get(0)
				.getProperties().stream()
				.filter(property -> "ddsr.fingerprint".equals(property.getName()))
				.map(property -> ((StringProperty) property).getValue())
				.findFirst().orElseThrow();

		ServiceInterface governance = ServicesFactory.eINSTANCE.createServiceInterface();
		governance.setName(INTERFACE);
		governance.setDeprecationReason("please move on");
		broker.deprecateCatalogEntry(INTERFACE, ContractAddressing.fingerprint(liveEntry()), governance, "test");

		ConsumerCapability capability = ServicesFactory.eINSTANCE.createConsumerCapability();
		StringProperty wanted = ServicesFactory.eINSTANCE.createStringProperty();
		wanted.setName("ddsr.fingerprint");
		wanted.setValue(fingerprint);
		capability.getProperties().add(wanted);

		assertThat(broker.getServiceReferences(INTERFACE, null, capability))
				.as("the contract is unchanged, so the fingerprint still matches").hasSize(1);
	}

	// ------------------------------------------------------------------
	// Snapshots written by another version
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a snapshot this model cannot read starts an empty registry instead of failing to start")
	void anUnreadableSnapshotStartsEmpty() throws Exception {
		Path snapshot = tmp.resolve("from-the-future.xmi");
		Files.writeString(snapshot, """
				<?xml version="1.0" encoding="UTF-8"?>
				<services:RemoteServiceRegistry xmlns:services="http://eclipse.org/fennec/services/1.0"\
				 xmlns:xmi="http://www.omg.org/XMI" xmi:version="2.0" name="ddsr-broker"\
				 kind="REMOTE" somethingThisVersionHasNeverHeardOf="42"/>
				""");

		DdsrBrokerImpl restarted = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend(), sink);

		assertThat(restarted.getRegistry()).as("it came up at all").isNotNull();
		assertThat(restarted.getServiceReferences(INTERFACE, null, null)).isEmpty();
	}

	@Test
	@DisplayName("a snapshot written by this broker comes back with its registrations rebuilt")
	void aSnapshotRoundTrips() {
		publish("1.0.0");
		assertThat(broker.snapshot().getSeverity()).isEqualTo(DiagnosticSeverity.OK);

		DdsrBrokerImpl restarted = new DdsrBrokerImpl(tmp.resolve("broker-state.xmi"),
				new InMemoryLookupBackend(), sink);

		List<ServiceReference> references = restarted.getServiceReferences(INTERFACE, null, null);
		assertThat(references).hasSize(1);
		assertThat(references.get(0).getId()).as("a fresh id: references do not survive a restart")
				.isNotBlank();
		assertThat(references.get(0).getRegistration().getImplementation().getVersion()).isEqualTo("1.0.0");
	}

	// ------------------------------------------------------------------
	// Races
	// ------------------------------------------------------------------

	@Test
	@DisplayName("publishing and withdrawing the same identity while the sweeper runs leaves one consistent answer")
	void publishWithdrawAndSweepDoNotCorruptEachOther() throws Exception {
		publish("1.0.0");
		broker.heartbeat(broker.getServiceReferences(INTERFACE, null, null).get(0).getId(), 1);

		ExecutorService pool = Executors.newFixedThreadPool(3);
		CountDownLatch go = new CountDownLatch(1);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		try {
			for (Runnable work : List.of(
					(Runnable) () -> { for (int i = 0; i < 40; i++) { publishQuietly("1.0.0"); } },
					(Runnable) () -> { for (int i = 0; i < 40; i++) { withdrawQuietly("1.0.0"); } },
					(Runnable) () -> { for (int i = 0; i < 40; i++) {
						broker.retireLostProviders(Instant.now().plusSeconds(60));
					} })) {
				pool.submit(() -> {
					try {
						go.await();
						work.run();
					} catch (Throwable thrown) {
						failure.compareAndSet(null, thrown);
					}
				});
			}
			go.countDown();
			pool.shutdown();
			assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).as("no thread is stuck").isTrue();
		} finally {
			pool.shutdownNow();
		}

		assertThat(failure.get()).as("nothing blew up").isNull();
		// Whatever the interleaving settled on, the registry and the lookup
		// index have to agree about it.
		int inRegistry = broker.getRegistry().getImplementations().size();
		int visible = broker.getAllServiceReferences(INTERFACE, null, null).size();
		assertThat(visible).as("index and registry agree").isEqualTo(inRegistry);
		assertThat(broker.providerLeaseCount()).as("no lease outlives its registration")
				.isLessThanOrEqualTo(inRegistry);
	}

	@Test
	@DisplayName("a session renewed while the expiry sweep runs is either kept or dropped, never half of both")
	void sessionExpiryAndRenewalDoNotTear() throws Exception {
		publish("1.0.0");
		String referenceId = broker.getServiceReferences(INTERFACE, null, null).get(0).getId();

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch go = new CountDownLatch(1);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		try {
			pool.submit(() -> {
				try {
					go.await();
					for (int i = 0; i < 200; i++) {
						ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
						session.setConsumerId("consumer");
						broker.putSession(session, List.of(referenceId));
					}
				} catch (Throwable thrown) {
					failure.compareAndSet(null, thrown);
				}
			});
			pool.submit(() -> {
				try {
					go.await();
					for (int i = 0; i < 200; i++) {
						broker.expireSessions(Instant.now().plusSeconds(1));
					}
				} catch (Throwable thrown) {
					failure.compareAndSet(null, thrown);
				}
			});
			go.countDown();
			pool.shutdown();
			assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
		} finally {
			pool.shutdownNow();
		}

		assertThat(failure.get()).isNull();
		assertThat(broker.sessionCount()).as("one consumer, so at most one session")
				.isLessThanOrEqualTo(1);
		// Every acquisition that survived must point at a registration that
		// is still live: a released lease that stayed behind would keep a
		// withdrawn service from ever being retired.
		broker.getSession("consumer").ifPresent(snapshot ->
				assertThat(snapshot.acquiredReferenceIds()).allSatisfy(id ->
						assertThat(broker.getAllServiceReferences(INTERFACE, null, null))
								.anyMatch(reference -> id.equals(reference.getId()))));
	}

	// ------------------------------------------------------------------

	private ServiceInterface liveEntry() {
		return broker.getRegistry().getCatalog().stream()
				.filter(entry -> INTERFACE.equals(entry.getName()))
				.findFirst().orElseThrow();
	}

	private void publishQuietly(String version) {
		ServiceProvider provider = version(version);
		broker.publishImplementation(provider, provider.getImplementations().get(0));
	}

	private void withdrawQuietly(String version) {
		ServiceProvider provider = version(version);
		broker.withdrawImplementation(provider, provider.getImplementations().get(0));
	}

	private void publish(String version) {
		ServiceProvider provider = version(version);
		Diagnostic d = broker.publishImplementation(provider, provider.getImplementations().get(0));
		assertThat(d.getSeverity().getValue()).isLessThan(DiagnosticSeverity.ERROR_VALUE);
	}

	private ServiceInterface serviceInterface(String name) {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName(name);
		si.setVersion("1.0.0");
		ServiceOperation operation = ServicesFactory.eINSTANCE.createServiceOperation();
		operation.setName("charge");
		si.getOperations().add(operation);
		return si;
	}

	private ServiceProvider version(String implVersion) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("payments");
		provider.setVersion(implVersion);
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName("payment-impl");
		impl.setVersion(implVersion);
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
