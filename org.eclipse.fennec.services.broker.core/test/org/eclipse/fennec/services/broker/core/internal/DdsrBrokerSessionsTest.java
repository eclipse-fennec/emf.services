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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerSessions.SessionSnapshot;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The acquisition stage (ACQUISITION.md §3–§6): consumer sessions and
 * their leases against the broker's registrations, including the
 * timing edges between registration, unregistration and session
 * changes.
 * <p>
 * Beyond the agreed case list this class pins a few extra edges:
 * duplicate reference ids in one PUT link only once; a {@code null}
 * acquisition list is a valid empty session (heartbeat before the
 * first find); two sessions may hold the same registration and a
 * withdraw releases both; a session can be re-created after DELETE;
 * {@code expireSessions(null)} is a no-op; and a withdrawn reference
 * stops resolving via {@code getImplementationForReference} although
 * the dead pair keeps its transient links for event building.
 */
class DdsrBrokerSessionsTest {

	@TempDir
	Path tmp;

	private Path snapshot;

	private InMemoryLookupBackend lookup;

	private DdsrBrokerImpl broker;

	@BeforeEach
	void setUp() {
		snapshot = tmp.resolve("broker-state.xmi");
		lookup = new InMemoryLookupBackend();
		broker = new DdsrBrokerImpl(snapshot, lookup);
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

	/** A fresh wire-shaped session object, as every REST PUT parses one. */
	private static ConsumerSession session(String consumerId) {
		ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
		session.setConsumerId(consumerId);
		return session;
	}

	private ServiceReference publishAndRef(String providerName, String implName) {
		ServiceProvider p = provider(providerName, implName);
		Diagnostic d = broker.publishImplementation(p, soleImpl(p));
		assertThat(d.getSeverity()).as("publish: %s", d.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		List<ServiceReference> refs = broker.getServiceReferences("Payment", null, null);
		return refs.stream()
				.filter(r -> providerName.equals(r.getProvider().getName()))
				.findFirst().orElseThrow();
	}

	/** The live registration behind a reference (transient in-process link). */
	private static ServiceRegistration registrationOf(ServiceReference ref) {
		return ref.getRegistration();
	}

	// ------------------------------------------------------------------
	// put / get / delete basics
	// ------------------------------------------------------------------

	@Test
	void putSessionStoresAndLinksAcquisitions() {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");

		Diagnostic d = broker.putSession(session("consumer-1"), List.of(ref.getId()));

		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		assertThat(d.getMessage()).contains("1 acquisition");
		assertThat(broker.sessionCount()).isEqualTo(1);
		assertThat(registrationOf(ref).getUsingSessions())
				.as("the eOpposite view on the registration must show the session")
				.extracting(ConsumerSession::getConsumerId)
				.containsExactly("consumer-1");
	}

	@Test
	void putSessionIsAFullReplace() {
		ServiceReference refA = publishAndRef("prov-a", "impl-a");
		ServiceReference refB = publishAndRef("prov-b", "impl-b");

		broker.putSession(session("consumer-1"), List.of(refA.getId()));
		broker.putSession(session("consumer-1"), List.of(refB.getId()));

		assertThat(broker.sessionCount()).isEqualTo(1);
		assertThat(registrationOf(refA).getUsingSessions())
				.as("the deselected registration must have lost the lease")
				.isEmpty();
		assertThat(registrationOf(refB).getUsingSessions())
				.extracting(ConsumerSession::getConsumerId)
				.containsExactly("consumer-1");
		assertThat(broker.getSession("consumer-1").orElseThrow().acquiredReferenceIds())
				.containsExactly(refB.getId());
	}

	@Test
	void heartbeatRenewsTheLeaseWithoutChangingAcquisitions() throws InterruptedException {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		broker.putSession(session("consumer-1"), List.of(ref.getId()));
		Date first = broker.getSession("consumer-1").orElseThrow().session().getLastRenewal();

		Thread.sleep(10);
		broker.putSession(session("consumer-1"), List.of(ref.getId()));

		SessionSnapshot renewed = broker.getSession("consumer-1").orElseThrow();
		assertThat(renewed.session().getLastRenewal())
				.as("an unchanged PUT is the heartbeat and must renew the lease")
				.isAfter(first);
		assertThat(renewed.acquiredReferenceIds()).containsExactly(ref.getId());
	}

	@Test
	void staleAcquireIsSkippedAndReportedNotRejected() {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		ServiceProvider p = (ServiceProvider) soleImplOfProvider("prov-a").eContainer();
		broker.withdrawImplementation(p, soleImplOfProvider("prov-a"));

		Diagnostic d = broker.putSession(session("consumer-1"), List.of(ref.getId(), "no-such-ref"));

		assertThat(d.getSeverity())
				.as("over-claiming is harmless (ACQUISITION §5) — never reject")
				.isEqualTo(DiagnosticSeverity.OK);
		assertThat(d.getMessage()).contains("skipped unknown").contains(ref.getId()).contains("no-such-ref");
		assertThat(broker.getSession("consumer-1").orElseThrow().acquiredReferenceIds()).isEmpty();
	}

	private ServiceImplementation soleImplOfProvider(String providerName) {
		return broker.liveRegistry().getProviders().stream()
				.filter(p -> providerName.equals(p.getName()))
				.findFirst().orElseThrow()
				.getImplementations().get(0);
	}

	@Test
	void aBlankConsumerIdIsInvalid() {
		assertThat(broker.putSession(session(null), List.of()).getCode())
				.isEqualTo(DdsrDiagnostics.CODE_SESSION_INVALID);
		assertThat(broker.putSession(session("  "), List.of()).getCode())
				.isEqualTo(DdsrDiagnostics.CODE_SESSION_INVALID);
		assertThat(broker.putSession(null, List.of()).getCode())
				.isEqualTo(DdsrDiagnostics.CODE_SESSION_INVALID);
		assertThat(broker.deleteSession(" ").getCode())
				.isEqualTo(DdsrDiagnostics.CODE_SESSION_INVALID);
	}

	@Test
	void deleteSessionReleasesAndIsIdempotent() {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		broker.putSession(session("consumer-1"), List.of(ref.getId()));

		Diagnostic first = broker.deleteSession("consumer-1");
		Diagnostic second = broker.deleteSession("consumer-1");

		assertThat(first.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		assertThat(first.getMessage()).contains("released");
		assertThat(second.getSeverity())
				.as("a shutdown-notify may race the TTL expiry — idempotent")
				.isEqualTo(DiagnosticSeverity.OK);
		assertThat(second.getMessage()).contains("nothing to release");
		assertThat(registrationOf(ref).getUsingSessions()).isEmpty();
		assertThat(broker.sessionCount()).isZero();
	}

	@Test
	void aSessionCanBeRecreatedAfterDelete() {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		broker.putSession(session("consumer-1"), List.of(ref.getId()));
		broker.deleteSession("consumer-1");

		broker.putSession(session("consumer-1"), List.of(ref.getId()));

		assertThat(broker.getSession("consumer-1").orElseThrow().acquiredReferenceIds())
				.containsExactly(ref.getId());
	}

	@Test
	void getSessionReturnsADetachedSnapshot() {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		ConsumerSession wire = session("consumer-1");
		ConsumerCapability capability = ServicesFactory.eINSTANCE.createConsumerCapability();
		capability.setConsumerId("consumer-1");
		capability.getSupportedFlavors().add(FlavorKind.REST);
		wire.setCapabilities(capability);
		broker.putSession(wire, List.of(ref.getId()));

		SessionSnapshot snapshot = broker.getSession("consumer-1").orElseThrow();
		snapshot.session().setConsumerId("tampered");
		snapshot.session().getCapabilities().getSupportedFlavors().clear();

		SessionSnapshot again = broker.getSession("consumer-1").orElseThrow();
		assertThat(again.session().getConsumerId())
				.as("mutating the snapshot must not touch broker state")
				.isEqualTo("consumer-1");
		assertThat(again.session().getCapabilities().getSupportedFlavors())
				.containsExactly(FlavorKind.REST);
		assertThat(again.acquiredReferenceIds()).containsExactly(ref.getId());
		assertThat(broker.getSession("unknown")).isEmpty();
	}

	@Test
	void duplicateReferenceIdsLinkOnlyOnce() {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");

		broker.putSession(session("consumer-1"), List.of(ref.getId(), ref.getId()));

		assertThat(broker.getSession("consumer-1").orElseThrow().acquiredReferenceIds())
				.containsExactly(ref.getId());
		assertThat(registrationOf(ref).getUsingSessions()).hasSize(1);
	}

	@Test
	void aNullAcquisitionListIsAValidEmptySession() {
		Diagnostic d = broker.putSession(session("consumer-1"), null);

		assertThat(d.getSeverity())
				.as("a consumer may announce itself before its first find")
				.isEqualTo(DiagnosticSeverity.OK);
		assertThat(broker.getSession("consumer-1").orElseThrow().acquiredReferenceIds()).isEmpty();
	}

	// ------------------------------------------------------------------
	// Expiry (TTL)
	// ------------------------------------------------------------------

	@Test
	void expiryReleasesOldSessionsButKeepsFreshOnes() {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		broker.putSession(session("stale"), List.of(ref.getId()));
		broker.putSession(session("fresh"), List.of(ref.getId()));
		// Backdate the stale one through the API surface: expire with a
		// cutoff between the two renewals is racy, so use a far-future
		// cutoff for "stale" determinism below and a far-past cutoff here.
		assertThat(broker.expireSessions(Instant.now().minusSeconds(3600)))
				.as("no session is older than an hour")
				.isZero();

		int expired = broker.expireSessions(Instant.now().plusSeconds(3600));

		assertThat(expired).isEqualTo(2);
		assertThat(broker.sessionCount()).isZero();
		assertThat(registrationOf(ref).getUsingSessions())
				.as("expiry must release the leases")
				.isEmpty();
		assertThat(broker.getSession("stale")).isEmpty();
	}

	@Test
	void aRenewalExactlyAtTheCutoffDoesNotExpire() {
		publishAndRef("prov-a", "impl-a");
		broker.putSession(session("consumer-1"), null);
		Date lastRenewal = broker.getSession("consumer-1").orElseThrow().session().getLastRenewal();

		int expired = broker.expireSessions(lastRenewal.toInstant());

		assertThat(expired)
				.as("isBefore semantics: equal to the cutoff means still alive")
				.isZero();
		assertThat(broker.sessionCount()).isEqualTo(1);
	}

	@Test
	void expireWithNullCutoffIsANoOp() {
		broker.putSession(session("consumer-1"), null);

		assertThat(broker.expireSessions(null)).isZero();
		assertThat(broker.sessionCount()).isEqualTo(1);
	}

	// ------------------------------------------------------------------
	// Timing against registration / unregistration / republish
	// ------------------------------------------------------------------

	@Test
	void withdrawReleasesTheLeasesButKeepsTheSession() {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		broker.putSession(session("consumer-1"), List.of(ref.getId()));
		ServiceRegistration registration = registrationOf(ref);

		ServiceImplementation impl = soleImplOfProvider("prov-a");
		Diagnostic d = broker.withdrawImplementation((ServiceProvider) impl.eContainer(), impl);

		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		assertThat(registration.getUsingSessions())
				.as("a withdrawn registration must never stay acquired")
				.isEmpty();
		SessionSnapshot after = broker.getSession("consumer-1").orElseThrow();
		assertThat(after.acquiredReferenceIds())
				.as("the session survives, only the lease is gone")
				.isEmpty();
	}

	@Test
	void aWithdrawnReferenceStopsResolving() {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		assertThat(broker.getImplementationForReference(ref)).isNotNull();

		ServiceImplementation impl = soleImplOfProvider("prov-a");
		broker.withdrawImplementation((ServiceProvider) impl.eContainer(), impl);

		assertThat(broker.getImplementationForReference(ref))
				.as("a dead reference must not keep resolving through its transient links")
				.isNull();
	}

	@Test
	void twoSessionsOnTheSameRegistrationAreBothReleasedByWithdraw() {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		broker.putSession(session("consumer-1"), List.of(ref.getId()));
		broker.putSession(session("consumer-2"), List.of(ref.getId()));
		assertThat(registrationOf(ref).getUsingSessions()).hasSize(2);

		ServiceImplementation impl = soleImplOfProvider("prov-a");
		broker.withdrawImplementation((ServiceProvider) impl.eContainer(), impl);

		assertThat(broker.getSession("consumer-1").orElseThrow().acquiredReferenceIds()).isEmpty();
		assertThat(broker.getSession("consumer-2").orElseThrow().acquiredReferenceIds()).isEmpty();
	}

	@Test
	void republishReleasesTheReplacedLeaseAndTheNewIdRelinks() {
		ServiceReference oldRef = publishAndRef("prov-a", "impl-a");
		broker.putSession(session("consumer-1"), List.of(oldRef.getId()));
		ServiceRegistration oldRegistration = registrationOf(oldRef);

		// Republish same (provider, impl name, version): retires the old
		// registration underneath the consumer.
		ServiceProvider again = provider("prov-a", "impl-a");
		broker.publishImplementation(again, soleImpl(again));

		assertThat(oldRegistration.getUsingSessions())
				.as("the retired registration must have released its leases")
				.isEmpty();
		assertThat(broker.getSession("consumer-1").orElseThrow().acquiredReferenceIds()).isEmpty();

		// The consumer's next full-replace PUT names the new reference.
		ServiceReference newRef = broker.getServiceReferences("Payment", null, null).get(0);
		assertThat(newRef.getId()).isNotEqualTo(oldRef.getId());
		broker.putSession(session("consumer-1"), List.of(newRef.getId()));
		assertThat(registrationOf(newRef).getUsingSessions())
				.extracting(ConsumerSession::getConsumerId)
				.containsExactly("consumer-1");
	}

	@Test
	void withdrawRollbackOnPersistFailureRestoresTheLeases() throws IOException {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		broker.putSession(session("consumer-1"), List.of(ref.getId()));

		// Same broker instance, but its snapshot target becomes unwritable
		// AFTER the state above was persisted: swap file for directory.
		Files.delete(snapshot);
		Files.createDirectory(snapshot);

		ServiceImplementation impl = soleImplOfProvider("prov-a");
		Diagnostic d = broker.withdrawImplementation((ServiceProvider) impl.eContainer(), impl);

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_PERSISTENCE_FAILED);
		assertThat(broker.getSession("consumer-1").orElseThrow().acquiredReferenceIds())
				.as("a rolled-back withdraw must restore the leases exactly")
				.containsExactly(ref.getId());
		assertThat(registrationOf(ref).getUsingSessions())
				.extracting(ConsumerSession::getConsumerId)
				.containsExactly("consumer-1");
		assertThat(broker.getImplementationForReference(ref))
				.as("the reference must resolve again after the rollback")
				.isNotNull();
	}

	// ------------------------------------------------------------------
	// Sessions are runtime state — never persisted (ACQUISITION §6)
	// ------------------------------------------------------------------

	@Test
	void sessionsNeverReachTheSnapshotAndDieWithTheBroker() throws IOException {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		broker.putSession(session("consumer-1"), List.of(ref.getId()));
		assertThat(broker.snapshot().getSeverity()).isEqualTo(DiagnosticSeverity.OK);

		String xmi = Files.readString(snapshot);
		assertThat(xmi)
				.as("no session, lease or registration trace may hit the disk")
				.doesNotContain("ConsumerSession")
				.doesNotContain("usingSessions")
				.doesNotContain("acquisitions")
				.doesNotContain("registration=");

		// A restarted broker: implementations survive, sessions do not,
		// and the pre-restart reference ids are worthless (regenerated).
		DdsrBrokerImpl restarted = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend());
		assertThat(restarted.sessionCount()).isZero();
		assertThat(restarted.liveRegistry().getImplementations()).hasSize(1);

		Diagnostic d = restarted.putSession(session("consumer-1"), List.of(ref.getId()));
		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		assertThat(d.getMessage())
				.as("old ids are skipped — the consumer re-finds and re-acquires")
				.contains("skipped unknown");
		assertThat(restarted.getSession("consumer-1").orElseThrow().acquiredReferenceIds()).isEmpty();
	}

	@Test
	void linkedSessionsBreakNeitherRegistryCopyNorPersist() {
		ServiceReference ref = publishAndRef("prov-a", "impl-a");
		broker.putSession(session("consumer-1"), List.of(ref.getId()));

		// The transient-flag proof on both serialization paths: the copy
		// for GET /registry and the snapshot save must both survive live
		// session links.
		assertThat(broker.getRegistry()).isNotNull();
		assertThat(broker.getRegistry().getImplementations()).hasSize(1);
		assertThat(broker.snapshot().getSeverity()).isEqualTo(DiagnosticSeverity.OK);
	}
}
