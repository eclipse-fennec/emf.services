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
import java.util.function.Supplier;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.Principal;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.common.CallPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What a principal registered, only that principal changes (SECURITY.md
 * §3) — the object-level rule no contract can state.
 *
 * <p>Every call goes through a fresh provider document, the way a REST
 * call parses one, so the broker has to find the live registration by
 * identity before it can ask whose it is.
 */
class BrokerOwnershipTest {

	private static final String ISSUER = "https://idp.example/realms/dim";

	@TempDir
	Path tmp;

	private DdsrBrokerImpl broker;

	@BeforeEach
	void setUp() {
		broker = new DdsrBrokerImpl(tmp.resolve("broker-state.xmi"), new InMemoryLookupBackend());
		broker.addCatalogEntry(contract(), "test");
	}

	@AfterEach
	void clearThread() {
		CallPrincipal.clear();
	}

	// ------------------------------------------------------------------
	// registrations
	// ------------------------------------------------------------------

	@Test
	void thePublisherIsRecordedAsTheOwner() {
		as("provider-a", () -> publish("1.0.0"));

		assertThat(liveReference().getRegistration().getPrincipal().getSubject()).isEqualTo("provider-a");
	}

	@Test
	void anotherPrincipalCannotWithdraw() {
		as("provider-a", () -> publish("1.0.0"));

		Diagnostic refused = as("provider-b", () -> withdraw("1.0.0"));

		assertThat(refused.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION);
		assertThat(broker.getServiceReferences("Payment", null, null)).hasSize(1);
	}

	@Test
	void theOwnerCanWithdraw() {
		as("provider-a", () -> publish("1.0.0"));

		Diagnostic withdrawn = as("provider-a", () -> withdraw("1.0.0"));

		assertThat(withdrawn.getSeverity()).as(withdrawn.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		assertThat(broker.getServiceReferences("Payment", null, null)).isEmpty();
	}

	@Test
	void anotherPrincipalCannotModify() {
		as("provider-a", () -> publish("1.0.0"));

		Diagnostic refused = as("provider-b", () -> {
			ServiceProvider changed = provider("1.0.0");
			((RestFlavor) changed.getImplementations().get(0).getFlavors().get(0)).setBasePath("/elsewhere");
			return broker.modifyImplementation(changed, changed.getImplementations().get(0));
		});

		assertThat(refused.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION);
		assertThat(((RestFlavor) liveReference().getRegistration().getImplementation().getFlavors().get(0))
				.getBasePath()).isEqualTo("/payments");
	}

	@Test
	void anotherPrincipalCannotTakeARegistrationOverByPublishingItAgain() {
		as("provider-a", () -> publish("1.0.0"));
		String before = liveReference().getId();

		Diagnostic refused = as("provider-b", () -> publish("1.0.0"));

		assertThat(refused.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION);
		assertThat(liveReference().getId()).isEqualTo(before);
		assertThat(liveReference().getRegistration().getPrincipal().getSubject()).isEqualTo("provider-a");
	}

	@Test
	void anotherPrincipalCannotReplaceThroughAnUpdatePolicy() {
		as("provider-a", () -> publish("1.0.0"));

		Diagnostic refused = as("provider-b", () -> {
			ServiceProvider successor = provider("2.0.0");
			ServiceImplementation predecessor = ServicesFactory.eINSTANCE.createServiceImplementation();
			predecessor.setName("payment");
			predecessor.setVersion("1.0.0");
			successor.getImplementations().get(0).setReplaces(predecessor);
			return broker.publishImplementation(successor, successor.getImplementations().get(0));
		});

		assertThat(refused.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION);
		assertThat(broker.getServiceReferences("Payment", null, null)).hasSize(1);
	}

	@Test
	void anotherPrincipalCannotKeepARegistrationAlive() {
		as("provider-a", () -> publish("1.0.0"));
		String referenceId = liveReference().getId();

		assertThat(as("provider-b", () -> broker.heartbeat(referenceId, 5)).getCode())
				.isEqualTo(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION);
		assertThat(as("provider-a", () -> broker.heartbeat(referenceId, 5)).getSeverity())
				.isNotEqualTo(DiagnosticSeverity.ERROR);
	}

	@Test
	void theSameSubjectFromAnotherIssuerIsSomebodyElse() {
		as("provider-a", () -> publish("1.0.0"));

		CallPrincipal.set(principal("provider-a", "https://other.example"));
		Diagnostic refused = withdraw("1.0.0");

		assertThat(refused.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION);
	}

	@Test
	void aCallWithoutAPrincipalIsLeftAlone() {
		// In process, or a broker running with auth.mode=none: nothing was
		// verified that a refusal could rest on.
		as("provider-a", () -> publish("1.0.0"));

		Diagnostic withdrawn = withdraw("1.0.0");

		assertThat(withdrawn.getSeverity()).as(withdrawn.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
	}

	@Test
	void aRegistrationWithoutAnOwnerIsClaimedByTheNextPublish() {
		// Published without authentication — or rebuilt from a snapshot,
		// where the principal is not kept.
		publish("1.0.0");
		assertThat(liveReference().getRegistration().getPrincipal()).isNull();

		Diagnostic claimed = as("provider-a", () -> publish("1.0.0"));

		assertThat(claimed.getSeverity()).as(claimed.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		assertThat(liveReference().getRegistration().getPrincipal().getSubject()).isEqualTo("provider-a");
	}

	@Test
	void theRegistrationHoldsACopyNotTheCallersPrincipal() {
		Principal onTheThread = principal("provider-a", ISSUER);
		CallPrincipal.set(onTheThread);
		publish("1.0.0");

		Principal recorded = liveReference().getRegistration().getPrincipal();
		assertThat(recorded).isNotSameAs(onTheThread);
		assertThat(onTheThread.eContainer()).isNull();
	}

	// ------------------------------------------------------------------
	// sessions
	// ------------------------------------------------------------------

	@Test
	void aSessionIsNamedByItsOwnersSubject() {
		Diagnostic accepted = as("consumer-a", () -> broker.putSession(session("consumer-a"), List.of()));
		Diagnostic refused = as("consumer-b", () -> broker.putSession(session("consumer-a"), List.of()));

		assertThat(accepted.getSeverity()).as(accepted.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		assertThat(refused.getCode()).isEqualTo(DdsrDiagnostics.CODE_SESSION_OWNERSHIP_VIOLATION);
	}

	@Test
	void anotherPrincipalCannotEndASession() {
		as("consumer-a", () -> broker.putSession(session("consumer-a"), List.of()));

		Diagnostic refused = as("consumer-b", () -> broker.deleteSession("consumer-a"));

		assertThat(refused.getCode()).isEqualTo(DdsrDiagnostics.CODE_SESSION_OWNERSHIP_VIOLATION);
		assertThat(broker.getSession("consumer-a")).isPresent();
	}

	@Test
	void aSessionWithoutAPrincipalIsLeftAlone() {
		Diagnostic accepted = broker.putSession(session("anyone"), List.of());

		assertThat(accepted.getSeverity()).as(accepted.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		assertThat(broker.deleteSession("anyone").getSeverity()).isNotEqualTo(DiagnosticSeverity.ERROR);
	}

	// ------------------------------------------------------------------
	// domain
	// ------------------------------------------------------------------

	@Test
	void aBrokerOfADomainWritesItOntoWhatACallCreates() {
		// One broker per domain (SECURITY.md §3): the verifier already
		// refused tokens of others, so everything a call creates here is
		// of this broker's domain — and says so, for the day one broker
		// holds several.
		broker.close();
		broker = new DdsrBrokerImpl(new BrokerSettings(tmp.resolve("tenant-state.xmi"), 0, 0, 0, 0, 0,
				UpdatePolicies.DEFAULT_CUTOVER_GRACE_MILLIS, "dim"), new InMemoryLookupBackend(), null);
		broker.addCatalogEntry(contract(), "test");

		as("provider-a", () -> publish("1.0.0"));
		as("consumer-a", () -> broker.putSession(session("consumer-a"), List.of()));

		assertThat(liveReference().getRegistration().getDomain()).isEqualTo("dim");
		assertThat(broker.getSession("consumer-a").orElseThrow().session().getDomain()).isEqualTo("dim");
	}

	@Test
	void aBrokerOfTheDefaultDomainWritesNone() {
		as("provider-a", () -> publish("1.0.0"));
		as("consumer-a", () -> broker.putSession(session("consumer-a"), List.of()));

		assertThat(liveReference().getRegistration().getDomain()).isNull();
		assertThat(broker.getSession("consumer-a").orElseThrow().session().getDomain()).isNull();
	}

	// ------------------------------------------------------------------
	// fixtures
	// ------------------------------------------------------------------

	/** Runs {@code call} as the given subject, the way the bearer filter would. */
	private static <T> T as(String subject, Supplier<T> call) {
		CallPrincipal.set(principal(subject, ISSUER));
		try {
			return call.get();
		} finally {
			CallPrincipal.clear();
		}
	}

	private static Principal principal(String subject, String issuer) {
		Principal principal = ServicesFactory.eINSTANCE.createPrincipal();
		principal.setSubject(subject);
		principal.setIssuer(issuer);
		return principal;
	}

	private Diagnostic publish(String version) {
		ServiceProvider provider = provider(version);
		return broker.publishImplementation(provider, provider.getImplementations().get(0));
	}

	private Diagnostic withdraw(String version) {
		ServiceProvider provider = provider(version);
		return broker.withdrawImplementation(provider, provider.getImplementations().get(0));
	}

	private ServiceReference liveReference() {
		List<ServiceReference> references = broker.getServiceReferences("Payment", null, null);
		assertThat(references).hasSize(1);
		return references.get(0);
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

	private static ServiceProvider provider(String implementationVersion) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("payments");
		provider.setVersion("1.0.0");
		ServiceImplementation implementation = ServicesFactory.eINSTANCE.createServiceImplementation();
		implementation.setName("payment");
		implementation.setVersion(implementationVersion);
		implementation.setImplementationId("org.example.payment");
		implementation.getServiceInterfaces().add(contract());
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName("payment-rest");
		flavor.setBasePath("/payments");
		implementation.getFlavors().add(flavor);
		provider.getImplementations().add(implementation);
		return provider;
	}

	private static ConsumerSession session(String consumerId) {
		ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
		session.setConsumerId(consumerId);
		return session;
	}
}
