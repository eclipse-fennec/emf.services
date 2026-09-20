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
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.UpdatePolicy;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.ServiceEventReasons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Provider liveness (#52, UPDATE_POLICY.md §4): heartbeats arm the
 * supervision per registration, two missed heartbeats retire the
 * registration with {@code PROVIDER_LOST}, and the update-policy
 * machinery treats a lost party the way the issue asks for.
 */
class DdsrBrokerLivenessTest {

	private static final String INTERFACE = "Payment";

	@TempDir
	Path tmp;

	private final RecordingEventSink sink = new RecordingEventSink();
	private DdsrBrokerImpl broker;
	private ServiceInterface payment;

	@BeforeEach
	void setUp() {
		broker = new DdsrBrokerImpl(tmp.resolve("broker-state.xmi"), new InMemoryLookupBackend(), sink);
		sink.deliveredBy(broker);
		payment = serviceInterface(INTERFACE, "charge");
		broker.addCatalogEntry(payment, "test");
		sink.clear();
	}

	@Test
	void twoMissedHeartbeatsRetireTheRegistrationWithProviderLost() {
		publish("1.0.0");
		String referenceId = soleReferenceId();
		sink.clear();

		Instant armed = Instant.now();
		Diagnostic d = broker.heartbeat(referenceId, 2);
		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		assertThat(broker.providerLeaseCount()).isEqualTo(1);

		assertThat(broker.retireLostProviders(armed.plusSeconds(3))).as("one missed heartbeat is not lost").isZero();
		assertThat(sink.received()).isEmpty();

		assertThat(broker.retireLostProviders(armed.plusSeconds(5))).as("2 × interval of silence").isEqualTo(1);
		assertThat(sink.types()).containsExactly(ServiceEventType.UNREGISTERING, ServiceEventType.RETIRED);
		assertThat(sink.reasons()).containsExactly(ServiceEventReasons.PROVIDER_LOST, ServiceEventReasons.PROVIDER_LOST);
		assertThat(sink.received().get(0).getReference().getId()).isEqualTo(referenceId);
		assertThat(broker.getServiceReferences(INTERFACE, null, null)).isEmpty();
		assertThat(broker.getRegistry().getImplementations()).isEmpty();
		assertThat(broker.providerLeaseCount()).isZero();
		assertThat(broker.retireLostProviders(armed.plus(Duration.ofDays(1)))).as("idempotent").isZero();
	}

	@Test
	void aRegistrationThatNeverHeartbeatsIsNeverRetiredForSilence() {
		publish("1.0.0");
		sink.clear();
		assertThat(broker.retireLostProviders(Instant.now().plus(Duration.ofDays(365)))).isZero();
		assertThat(sink.received()).isEmpty();
		assertThat(broker.getServiceReferences(INTERFACE, null, null)).hasSize(1);
	}

	@Test
	void aHeartbeatWithinTheWindowRestartsTheClock() {
		publish("1.0.0");
		String referenceId = soleReferenceId();
		broker.heartbeat(referenceId, 2);
		Instant renewed = Instant.now();
		broker.heartbeat(referenceId, 2);
		assertThat(broker.providerLeaseCount()).as("one lease per registration, not per heartbeat").isEqualTo(1);
		assertThat(broker.retireLostProviders(renewed.plusSeconds(3))).isZero();
		assertThat(broker.getServiceReferences(INTERFACE, null, null)).hasSize(1);
	}

	@Test
	void unknownReferencesAndBadIntervalsAreRefused() {
		Diagnostic unknown = broker.heartbeat("no-such-reference", 30);
		assertThat(unknown.getSeverity()).isEqualTo(DiagnosticSeverity.ERROR);
		assertThat(unknown.getCode()).as("404 on the wire: publish again").isEqualTo(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED);

		publish("1.0.0");
		Diagnostic bad = broker.heartbeat(soleReferenceId(), 0);
		assertThat(bad.getSeverity()).isEqualTo(DiagnosticSeverity.ERROR);
		assertThat(bad.getCode()).isEqualTo(DdsrDiagnostics.CODE_HEARTBEAT_INVALID);
		assertThat(broker.providerLeaseCount()).isZero();
	}

	@Test
	void aWithdrawnRegistrationIsNoLongerSupervised() {
		ServiceProvider provider = publish("1.0.0");
		broker.heartbeat(soleReferenceId(), 1);
		sink.clear();

		broker.withdrawImplementation(provider, soleImpl(provider));
		assertThat(broker.providerLeaseCount()).isZero();
		assertThat(broker.retireLostProviders(Instant.now().plusSeconds(60))).isZero();
		assertThat(sink.types()).containsExactly(ServiceEventType.UNREGISTERING);
		assertThat(sink.reasons()).containsExactly(ServiceEventReasons.WITHDRAWN);
	}

	@Test
	void aRetiredReferenceRefusesFurtherHeartbeats() {
		publish("1.0.0");
		String referenceId = soleReferenceId();
		broker.heartbeat(referenceId, 1);
		broker.retireLostProviders(Instant.now().plusSeconds(10));

		Diagnostic late = broker.heartbeat(referenceId, 1);
		assertThat(late.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED);
	}

	@Test
	void aLostSuccessorCancelsTheDrainAndThePredecessorIsVisibleAgain() {
		publish("1.0.0");
		String predecessorId = soleReferenceId();
		hold("consumer-1", predecessorId);
		publishSuccessor("2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0");
		assertThat(visibleVersions()).as("draining predecessor is hidden").containsExactly("2.0.0");
		String successorId = referenceOf("2.0.0").getId();
		sink.clear();

		broker.heartbeat(successorId, 1);
		assertThat(broker.retireLostProviders(Instant.now().plusSeconds(10))).isEqualTo(1);

		assertThat(sink.reasons()).containsExactly(ServiceEventReasons.PROVIDER_LOST, ServiceEventReasons.PROVIDER_LOST);
		assertThat(sink.received().get(0).getReference().getId()).isEqualTo(successorId);
		assertThat(visibleVersions()).as("drain cancelled").containsExactly("1.0.0");
		assertThat(broker.advanceUpdatePolicies(Instant.now().plus(Duration.ofDays(1)))).isZero();
	}

	@Test
	void aLostPredecessorEndsTheDrainAsRetired() {
		publish("1.0.0");
		String predecessorId = soleReferenceId();
		hold("consumer-1", predecessorId);
		ServiceProvider successor = publishSuccessor("2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0");
		sink.clear();

		broker.heartbeat(predecessorId, 1);
		assertThat(broker.retireLostProviders(Instant.now().plusSeconds(10))).isEqualTo(1);

		assertThat(sink.types()).containsExactly(ServiceEventType.UNREGISTERING, ServiceEventType.RETIRED);
		assertThat(sink.reasons()).as("lost, not replaced — the consumer learns the true cause")
				.containsExactly(ServiceEventReasons.PROVIDER_LOST, ServiceEventReasons.PROVIDER_LOST);
		assertThat(sink.received().get(0).getReference().getId()).isEqualTo(predecessorId);
		assertThat(soleImpl(successor).getReplaces()).as("nothing dangles in the snapshot").isNull();
		assertThat(allVersions()).containsExactly("2.0.0");
		assertThat(broker.advanceUpdatePolicies(Instant.now().plus(Duration.ofDays(1)))).isZero();
		assertThat(broker.snapshot().getSeverity()).isEqualTo(DiagnosticSeverity.OK);
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

	private ServiceProvider publish(String implVersion) {
		ServiceProvider provider = version(implVersion);
		Diagnostic d = broker.publishImplementation(provider, soleImpl(provider));
		assertThat(d.getSeverity().getValue()).isLessThan(DiagnosticSeverity.ERROR_VALUE);
		return provider;
	}

	private ServiceProvider publishSuccessor(String version, UpdatePolicy policy, String replaces) {
		ServiceProvider provider = version(version);
		ServiceImplementation impl = soleImpl(provider);
		impl.setUpdatePolicy(policy);
		ServiceImplementation stub = ServicesFactory.eINSTANCE.createServiceImplementation();
		stub.setName("payment-impl");
		stub.setVersion(replaces);
		stub.setImplementationId("stub");
		impl.setReplaces(stub);
		Diagnostic d = broker.publishImplementation(provider, impl);
		assertThat(d.getSeverity().getValue()).isLessThan(DiagnosticSeverity.ERROR_VALUE);
		return provider;
	}

	private void hold(String consumerId, String referenceId) {
		ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
		session.setConsumerId(consumerId);
		assertThat(broker.putSession(session, List.of(referenceId)).getSeverity()).isEqualTo(DiagnosticSeverity.OK);
	}

	private static ServiceImplementation soleImpl(ServiceProvider provider) {
		return provider.getImplementations().get(0);
	}

	private String soleReferenceId() {
		List<ServiceReference> refs = broker.getServiceReferences(INTERFACE, null, null);
		assertThat(refs).hasSize(1);
		return refs.get(0).getId();
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
				.findFirst().orElseThrow();
	}
}
