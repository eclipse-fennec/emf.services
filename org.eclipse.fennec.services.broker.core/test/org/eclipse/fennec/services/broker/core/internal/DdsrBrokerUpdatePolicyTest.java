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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.UpdatePolicy;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.broker.core.ServiceEventReasons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The update-policy state machine of UPDATE_POLICY.md §2 (#45): a
 * successor publishes with {@code replaces}; EVERGREEN keeps both,
 * DEPRECATE_AND_DRAIN hides the predecessor and retires it once its
 * last lease is gone, HARD_CUTOVER retires it after the grace window
 * regardless of leases.
 */
class DdsrBrokerUpdatePolicyTest {

	private static final String INTERFACE = "Payment";

	@TempDir
	Path tmp;

	private final RecordingEventSink sink = new RecordingEventSink();
	private Path snapshot;
	private DdsrBrokerImpl broker;
	private ServiceInterface payment;

	@BeforeEach
	void setUp() {
		snapshot = tmp.resolve("broker-state.xmi");
		broker = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend(), sink);
		payment = serviceInterface(INTERFACE, "charge");
		broker.addCatalogEntry(payment, "test");
		sink.received.clear();
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

	/** Provider "payments" carrying one implementation "payment-impl" at the given version. */
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
		flavor.setHost("http://localhost:909" + implVersion.charAt(0));
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

	private static ServiceImplementation soleImpl(ServiceProvider provider) {
		return provider.getImplementations().get(0);
	}

	/** A wire-style stub naming the predecessor by (name, version) only. */
	private static ServiceImplementation replacesStub(String version) {
		ServiceImplementation stub = ServicesFactory.eINSTANCE.createServiceImplementation();
		stub.setName("payment-impl");
		stub.setVersion(version);
		stub.setImplementationId("stub");
		return stub;
	}

	private ServiceProvider publishSuccessor(String version, UpdatePolicy policy, String replaces) {
		ServiceProvider provider = version(version);
		ServiceImplementation impl = soleImpl(provider);
		impl.setUpdatePolicy(policy);
		impl.setReplaces(replacesStub(replaces));
		Diagnostic d = broker.publishImplementation(provider, impl);
		assertThat(d.getSeverity().getValue()).isLessThan(DiagnosticSeverity.ERROR_VALUE);
		return provider;
	}

	private List<String> visibleVersions() {
		return broker.getServiceReferences(INTERFACE, null, null).stream()
				.map(ref -> ref.getRegistration().getImplementation().getVersion())
				.toList();
	}

	private List<String> allVersions() {
		return broker.getAllServiceReferences(INTERFACE, null, null).stream()
				.map(ref -> ref.getRegistration().getImplementation().getVersion())
				.toList();
	}

	private ServiceReference referenceOf(String version) {
		return broker.getAllServiceReferences(INTERFACE, null, null).stream()
				.filter(ref -> version.equals(ref.getRegistration().getImplementation().getVersion()))
				.findFirst()
				.orElseThrow();
	}

	private void hold(String consumerId, ServiceReference reference) {
		ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
		session.setConsumerId(consumerId);
		Diagnostic d = broker.putSession(session, List.of(reference.getId()));
		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
	}

	// ------------------------------------------------------------------
	// Policy resolution
	// ------------------------------------------------------------------

	@Test
	void unspecifiedEverywhereMeansDeprecateAndDrain() {
		assertThat(DdsrBrokerImpl.effectiveUpdatePolicy(soleImpl(version("2.0.0"))))
				.isEqualTo(UpdatePolicy.DEPRECATE_AND_DRAIN);
	}

	@Test
	void theInterfaceDefaultAppliesAndTheStrictestInterfaceWins() {
		ServiceImplementation impl = soleImpl(version("2.0.0"));
		ServiceInterface lenient = serviceInterface("Audit");
		lenient.setUpdatePolicy(UpdatePolicy.EVERGREEN);
		impl.getServiceInterfaces().add(lenient);
		payment.setUpdatePolicy(UpdatePolicy.HARD_CUTOVER);

		assertThat(DdsrBrokerImpl.effectiveUpdatePolicy(impl)).isEqualTo(UpdatePolicy.HARD_CUTOVER);
	}

	@Test
	void theImplementationOverridesItsInterfaces() {
		ServiceImplementation impl = soleImpl(version("2.0.0"));
		payment.setUpdatePolicy(UpdatePolicy.HARD_CUTOVER);
		impl.setUpdatePolicy(UpdatePolicy.EVERGREEN);

		assertThat(DdsrBrokerImpl.effectiveUpdatePolicy(impl)).isEqualTo(UpdatePolicy.EVERGREEN);
	}

	// ------------------------------------------------------------------
	// replaces resolution
	// ------------------------------------------------------------------

	@Test
	void anUnknownPredecessorIsAWarningAndAPlainPublish() {
		ServiceProvider provider = version("2.0.0");
		ServiceImplementation impl = soleImpl(provider);
		impl.setReplaces(replacesStub("0.0.1"));

		Diagnostic d = broker.publishImplementation(provider, impl);

		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.WARNING);
		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_REPLACES_NOT_FOUND);
		assertThat(impl.getReplaces()).as("the dangling stub must not reach the snapshot").isNull();
		assertThat(visibleVersions()).containsExactly("2.0.0");
		assertThat(sink.types()).containsExactly(ServiceEventType.REGISTERED);
	}

	@Test
	void theStubIsRewiredToTheLivePredecessor() {
		broker.publishImplementation(version("1.0.0"), soleImpl(version("1.0.0")));
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		ServiceProvider v2 = publishSuccessor("2.0.0", UpdatePolicy.EVERGREEN, "1.0.0");

		ServiceImplementation live = referenceOf("1.0.0").getRegistration().getImplementation();
		assertThat(soleImpl(v2).getReplaces()).isSameAs(live);
	}

	// ------------------------------------------------------------------
	// EVERGREEN
	// ------------------------------------------------------------------

	@Test
	void evergreenKeepsBothVersionsRegisteredAndVisible() {
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		publishSuccessor("2.0.0", UpdatePolicy.EVERGREEN, "1.0.0");

		assertThat(visibleVersions()).containsExactlyInAnyOrder("1.0.0", "2.0.0");
		assertThat(broker.advanceUpdatePolicies(Instant.now().plusSeconds(3600))).isZero();
		assertThat(visibleVersions()).containsExactlyInAnyOrder("1.0.0", "2.0.0");
		assertThat(sink.types()).containsExactly(ServiceEventType.REGISTERED, ServiceEventType.REGISTERED);
	}

	// ------------------------------------------------------------------
	// DEPRECATE_AND_DRAIN
	// ------------------------------------------------------------------

	@Test
	void drainHidesThePredecessorFromLookupsAndAnnouncesTheUpgrade() {
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		ServiceReference oldRef = referenceOf("1.0.0");
		sink.received.clear();

		publishSuccessor("2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0");

		assertThat(visibleVersions()).as("new lookups get the successor only").containsExactly("2.0.0");
		assertThat(allVersions()).as("getAll waives the drain filter").containsExactlyInAnyOrder("1.0.0", "2.0.0");
		assertThat(sink.types())
				.as("REGISTERED first, so a consumer reacting to the hint already finds the successor")
				.containsExactly(ServiceEventType.REGISTERED, ServiceEventType.UPGRADE_AVAILABLE);
		assertThat(sink.received.get(1).getReference().getId()).isEqualTo(oldRef.getId());
	}

	@Test
	void drainRetiresThePredecessorOnceItsLastLeaseIsGone() {
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		hold("consumer-1", referenceOf("1.0.0"));
		publishSuccessor("2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0");
		sink.received.clear();

		assertThat(broker.advanceUpdatePolicies(Instant.now())).as("a held lease blocks the retire").isZero();
		assertThat(allVersions()).containsExactlyInAnyOrder("1.0.0", "2.0.0");

		broker.deleteSession("consumer-1");
		assertThat(broker.advanceUpdatePolicies(Instant.now())).isEqualTo(1);

		assertThat(allVersions()).containsExactly("2.0.0");
		assertThat(broker.getRegistry().getImplementations()).hasSize(1);
		assertThat(sink.types()).containsExactly(ServiceEventType.UNREGISTERING, ServiceEventType.RETIRED);
		assertThat(sink.reasons()).containsExactly(ServiceEventReasons.REPLACED, ServiceEventReasons.REPLACED);
		assertThat(broker.advanceUpdatePolicies(Instant.now())).as("idempotent").isZero();
	}

	@Test
	void drainWithoutAnyLeaseRetiresOnTheNextSweep() {
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		publishSuccessor("2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0");

		assertThat(broker.advanceUpdatePolicies(Instant.now())).isEqualTo(1);
		assertThat(allVersions()).containsExactly("2.0.0");
	}

	@Test
	void withdrawingTheSuccessorCancelsTheDrain() {
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		ServiceProvider v2 = publishSuccessor("2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0");
		assertThat(visibleVersions()).containsExactly("2.0.0");

		broker.withdrawImplementation(v2, soleImpl(v2));

		assertThat(visibleVersions()).as("the predecessor is a normal registration again").containsExactly("1.0.0");
		assertThat(broker.advanceUpdatePolicies(Instant.now())).isZero();
		assertThat(allVersions()).containsExactly("1.0.0");
	}

	@Test
	void aDrainingPredecessorIsNotParkedCold() {
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		hold("consumer-1", referenceOf("1.0.0"));
		publishSuccessor("2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0");

		assertThat(broker.coldifyIdle(Instant.now().plusSeconds(3600))).isZero();
		assertThat(broker.coldCount()).isZero();
	}

	// ------------------------------------------------------------------
	// HARD_CUTOVER
	// ------------------------------------------------------------------

	@Test
	void hardCutoverKeepsBothVisibleDuringGraceThenRetiresDespiteLeases() {
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		hold("consumer-1", referenceOf("1.0.0"));
		ServiceProvider v2 = version("2.0.0");
		soleImpl(v2).setUpdatePolicy(UpdatePolicy.HARD_CUTOVER);
		soleImpl(v2).setCutoverGraceMillis(1_000L);
		soleImpl(v2).setReplaces(replacesStub("1.0.0"));
		sink.received.clear();
		broker.publishImplementation(v2, soleImpl(v2));

		assertThat(visibleVersions()).as("phase 2: failover window, both visible")
				.containsExactlyInAnyOrder("1.0.0", "2.0.0");
		assertThat(sink.types()).as("no UPGRADE_AVAILABLE hint for a cutover")
				.containsExactly(ServiceEventType.REGISTERED);
		assertThat(broker.advanceUpdatePolicies(Instant.now())).as("inside the window").isZero();

		assertThat(broker.advanceUpdatePolicies(Instant.now().plusSeconds(2))).isEqualTo(1);

		assertThat(allVersions()).containsExactly("2.0.0");
		assertThat(sink.types()).containsExactly(ServiceEventType.REGISTERED,
				ServiceEventType.UNREGISTERING, ServiceEventType.RETIRED);
		assertThat(sink.reasons().subList(1, 3))
				.containsExactly(ServiceEventReasons.CUTOVER, ServiceEventReasons.CUTOVER);
	}

	@Test
	void hardCutoverFallsBackToTheBrokerDefaultGrace() {
		broker.setDefaultCutoverGraceMillis(60_000L);
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		publishSuccessor("2.0.0", UpdatePolicy.HARD_CUTOVER, "1.0.0");

		assertThat(broker.advanceUpdatePolicies(Instant.now().plusSeconds(30))).isZero();
		assertThat(broker.advanceUpdatePolicies(Instant.now().plusSeconds(61))).isEqualTo(1);
	}

	@Test
	void theInterfacePolicyDrivesAnUnspecifiedSuccessor() {
		payment.setUpdatePolicy(UpdatePolicy.HARD_CUTOVER);
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		publishSuccessor("2.0.0", UpdatePolicy.UNSPECIFIED, "1.0.0");

		assertThat(visibleVersions()).as("cutover, not drain: both stay visible")
				.containsExactlyInAnyOrder("1.0.0", "2.0.0");
		assertThat(broker.advanceUpdatePolicies(Instant.now().plusMillis(DdsrBrokerImpl.DEFAULT_CUTOVER_GRACE_MILLIS + 1)))
				.isEqualTo(1);
	}

	// ------------------------------------------------------------------
	// Snapshot round trips
	// ------------------------------------------------------------------

	@Test
	void aPendingDrainSurvivesTheSnapshotAsPlainRegistrations() {
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		hold("consumer-1", referenceOf("1.0.0"));
		publishSuccessor("2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0");

		DdsrBrokerImpl reloaded = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend());

		// replaces points into the same resource — the snapshot loads; the
		// drain itself is runtime state and is forgotten (fails safe).
		assertThat(reloaded.getRegistry().getImplementations()).hasSize(2);
		assertThat(reloaded.advanceUpdatePolicies(Instant.now().plusSeconds(3600))).isZero();
	}

	@Test
	void aRetiredPredecessorLeavesNoDanglingReplacesInTheSnapshot() {
		ServiceProvider v1 = version("1.0.0");
		broker.publishImplementation(v1, soleImpl(v1));
		publishSuccessor("2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0");
		assertThat(broker.advanceUpdatePolicies(Instant.now())).isEqualTo(1);

		DdsrBrokerImpl reloaded = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend());

		assertThat(reloaded.getRegistry().getImplementations()).hasSize(1);
		assertThat(reloaded.getRegistry().getImplementations().get(0).getReplaces()).isNull();
	}
}
