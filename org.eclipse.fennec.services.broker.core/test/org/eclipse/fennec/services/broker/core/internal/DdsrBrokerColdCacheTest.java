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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The cold cache (ACQUISITION.md §10): idle registrations move to disk
 * but stay discoverable — the one rule that carries the design is
 * "kalt ≠ unauffindbar". Pins the idle rule (leases and recent lookups
 * block the move), the lifecycle announcements at the boundary
 * (UNREGISTERING on coldify, REGISTERED on rehydrate), lazy rehydration
 * through the lookup path, restart survival via the cold directory,
 * strict-reject coverage for cold contracts, and the supersede rule
 * (a re-publish replaces the cold twin).
 */
class DdsrBrokerColdCacheTest {

	@TempDir
	Path tmp;

	private Path snapshot;
	private DdsrBrokerImpl broker;
	private final List<ServiceEvent> events = new ArrayList<>();

	@BeforeEach
	void setUp() {
		snapshot = tmp.resolve("broker-state.xmi");
		broker = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend(), events::add);
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
	}

	// ------------------------------------------------------------------
	// Fixtures
	// ------------------------------------------------------------------

	private static ServiceInterface serviceInterface(String name, String... operations) {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName(name);
		si.setVersion("1.0.0");
		for (String op : operations) {
			ServiceOperation operation = ServicesFactory.eINSTANCE.createServiceOperation();
			operation.setName(op);
			si.getOperations().add(operation);
		}
		return si;
	}

	private static ServiceProvider provider(String providerName, String implName) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(providerName);
		provider.setVersion("1.0.0");
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(implName);
		impl.setVersion("1.0.0");
		impl.setImplementationId("test:" + implName + ":1.0.0");
		impl.getServiceInterfaces().add(serviceInterface("Payment", "charge", "getBalance"));
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName(implName);
		flavor.setBasePath("/payments");
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		return provider;
	}

	private static ServiceImplementation soleImpl(ServiceProvider p) {
		return p.getImplementations().get(0);
	}

	private void publish(String providerName, String implName) {
		ServiceProvider p = provider(providerName, implName);
		Diagnostic d = broker.publishImplementation(p, soleImpl(p));
		assertThat(d.getSeverity()).as("publish: %s", d.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
	}

	/** A cutoff slightly in the future: everything present counts as idle. */
	private static Instant everythingIdle() {
		return Instant.now().plusSeconds(2);
	}

	private List<ServiceEventType> eventTypes() {
		return events.stream().map(ServiceEvent::getType).toList();
	}

	// ------------------------------------------------------------------

	@Test
	void anIdleRegistrationMovesColdAndAnnouncesUnregistering() {
		publish("prov-a", "impl-a");
		events.clear();

		int moved = broker.coldifyIdle(everythingIdle());

		assertThat(moved).isEqualTo(1);
		assertThat(broker.coldCount()).isEqualTo(1);
		assertThat(broker.getRegistry().getImplementations())
				.as("the hot registry no longer carries the impl")
				.isEmpty();
		assertThat(eventTypes()).containsExactly(ServiceEventType.UNREGISTERING);
	}

	@Test
	void coldStaysDiscoverableAndRehydratesOnLookup() {
		publish("prov-a", "impl-a");
		broker.coldifyIdle(everythingIdle());
		events.clear();

		List<ServiceReference> refs = broker.getServiceReferences("Payment", null, null);

		assertThat(refs).as("kalt ≠ unauffindbar — the lookup wakes the entry").hasSize(1);
		assertThat(broker.coldCount()).isZero();
		assertThat(broker.getRegistry().getImplementations()).hasSize(1);
		assertThat(eventTypes()).contains(ServiceEventType.REGISTERED);
	}

	@Test
	void aLeaseBlocksTheMove() {
		publish("prov-a", "impl-a");
		ServiceReference ref = broker.getServiceReferences("Payment", null, null).get(0);
		ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
		session.setConsumerId("consumer-1");
		broker.putSession(session, List.of(ref.getId()));

		assertThat(broker.coldifyIdle(everythingIdle()))
				.as("a held lease is activity — no cold move")
				.isZero();
		assertThat(broker.coldCount()).isZero();
	}

	@Test
	void aRecentLookupBlocksTheMove() {
		publish("prov-a", "impl-a");
		broker.getServiceReferences("Payment", null, null);

		assertThat(broker.coldifyIdle(Instant.now().minusSeconds(60)))
				.as("interface was looked up after the cutoff")
				.isZero();
	}

	@Test
	void coldEntriesSurviveABrokerRestart() {
		publish("prov-a", "impl-a");
		broker.coldifyIdle(everythingIdle());

		DdsrBrokerImpl restarted = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend());
		assertThat(restarted.coldCount())
				.as("stubs are rebuilt from the cold directory")
				.isEqualTo(1);
		assertThat(restarted.getServiceReferences("Payment", null, null))
				.as("still discoverable after the restart")
				.hasSize(1);
		assertThat(restarted.coldCount()).isZero();
	}

	@Test
	void aColdImplementationStillBlocksCatalogRemoval() {
		publish("prov-a", "impl-a");
		broker.coldifyIdle(everythingIdle());

		Diagnostic d = broker.removeCatalogEntry(
				serviceInterface("Payment", "charge", "getBalance"), "test");

		assertThat(d.getCode())
				.as("cold is live for strict-reject — it could never rehydrate otherwise")
				.isEqualTo(DdsrDiagnostics.CODE_CATALOG_HAS_LIVE_IMPLS);
	}

	@Test
	void aRepublishSupersedesTheColdTwin() {
		publish("prov-a", "impl-a");
		broker.coldifyIdle(everythingIdle());

		// the provider restarts and publishes the same implementation
		publish("prov-a", "impl-a");

		assertThat(broker.coldCount()).as("the cold twin is superseded").isZero();
		assertThat(broker.getServiceReferences("Payment", null, null)).hasSize(1);
	}

	@Test
	void rehydrationServesTheSameContractFingerprint() {
		publish("prov-a", "impl-a");
		ServiceReference before = broker.getServiceReferences("Payment", null, null).get(0);
		String fingerprintBefore = stringProperty(before, "ddsr.fingerprint");
		broker.coldifyIdle(everythingIdle());

		ServiceReference after = broker.getServiceReferences("Payment", null, null).get(0);

		assertThat(stringProperty(after, "ddsr.fingerprint"))
				.as("the rehydrated registration carries the identical contract")
				.isEqualTo(fingerprintBefore);
		assertThat(after.getId())
				.as("the reference identity is NOT stable across the cold boundary")
				.isNotEqualTo(before.getId());
	}

	private static String stringProperty(ServiceReference reference, String name) {
		return reference.getProperties().stream()
				.filter(p -> name.equals(p.getName()))
				.filter(org.eclipse.fennec.services.StringProperty.class::isInstance)
				.map(p -> ((org.eclipse.fennec.services.StringProperty) p).getValue())
				.findFirst().orElse(null);
	}
}
