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
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.payment;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.provider;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.reference;

import java.io.IOException;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.invocation.ResultDocument;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * The lookup contract's REST face (#56, #88): the query parameters it
 * takes, and the envelope it answers with.
 *
 * <p>What is NOT here any more: statuses. A missing required parameter,
 * a failing Diagnostic and the codes that map to 400 and 404 are the
 * generic distribution's business now, driven by the contract document
 * — which is the whole point of #88.
 */
class BrokerLookupRestTest {

	private final RestTestSupport.FakeLookup lookup = new RestTestSupport.FakeLookup();

	private final RestTestSupport.ResourceSets resourceSets = new RestTestSupport.ResourceSets();

	private BrokerLookupRest service;

	@BeforeEach
	void setUp() {
		service = new BrokerLookupRest();
		service.broker = lookup;
	}

	/** The roots as the transport would send them (#88). */
	private List<EObject> answerRoots(LocalServiceRegistry envelope) {
		return ResultDocument.roots(envelope);
	}

	@Test
	void theInterfaceParameterIsRequired() {
		// The dispatcher refuses this before it ever gets here, because the
		// contract says the parameter is not optional. Kept as a guard so
		// that calling the service directly cannot silently ask for
		// everything.
		assertThatThrownBy(() -> service.getServiceReferences(null, null, null, null, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.getServiceReferences("  ", null, null, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void withoutFlavorsConsumerIdAndFingerprintNoCapabilityIsBuilt() {
		service.getServiceReferences("Payment", "", "", " ", null);

		assertThat(lookup.lastInterface).isEqualTo("Payment");
		assertThat(lookup.lastFilter).as("blank filter means no filter").isNull();
		assertThat(lookup.lastCapability).isNull();
	}

	@Test
	void flavorsAreParsedFromCsvIgnoringWhitespaceAndUnknownTokens() {
		ConsumerCapability capability = BrokerLookupRest.capabilityOf(" REST , MQTT,, GRPC ", null, null);

		assertThat(capability.getSupportedFlavors()).containsExactly(FlavorKind.REST, FlavorKind.MQTT);
	}

	@Test
	void consumerIdAndFingerprintLandOnTheCapability() {
		ConsumerCapability capability = BrokerLookupRest.capabilityOf(null, "consumer-7", " sd1:abc ");

		assertThat(capability.getConsumerId()).isEqualTo("consumer-7");
		assertThat(capability.getSupportedFlavors()).isEmpty();
		assertThat(capability.getProperties()).hasSize(1);
		StringProperty requested = (StringProperty) capability.getProperties().get(0);
		assertThat(requested.getName()).isEqualTo("ddsr.fingerprint");
		assertThat(requested.getValue()).as("trimmed").isEqualTo("sd1:abc");
	}

	@Test
	void theFilterIsPassedThroughVerbatim() {
		service.getServiceReferences("Payment", "(region=eu)", "REST", null, null);

		assertThat(lookup.lastFilter).isEqualTo("(region=eu)");
		assertThat(lookup.lastCapability.getSupportedFlavors()).containsExactly(FlavorKind.REST);
	}

	@Test
	void theEnvelopeCarriesCopiesOfReferencesProvidersAndInterfacesNotTheLiveObjects() {
		ServiceInterface payment = payment();
		ServiceProvider provider = provider("payments", payment, FlavorKind.REST);
		ServiceReference ref = reference("ref-1", provider);
		lookup.results.add(ref);
		lookup.resolves(ref, provider.getImplementations().get(0));

		LocalServiceRegistry envelope = service.getServiceReferences("Payment", null, null, null, null);

		assertThat(envelope.getName()).isEqualTo("lookup-result");
		assertThat(envelope.getReferences()).hasSize(1);
		ServiceReference copy = envelope.getReferences().get(0);
		assertThat(copy.getId()).isEqualTo("ref-1");
		assertThat(copy).as("the wire gets a copy, the registry keeps its objects").isNotSameAs(ref);
		assertThat(envelope.getProviders()).hasSize(1);
		assertThat(envelope.getProviders().get(0).getImplementations()).hasSize(1);

		List<EObject> roots = answerRoots(envelope);
		assertThat(roots.get(0)).isSameAs(envelope);
		assertThat(roots.stream().filter(ServiceInterface.class::isInstance).map(ServiceInterface.class::cast))
				.as("the contract rides along as a sibling root so the consumer can fingerprint it")
				.extracting(ServiceInterface::getName).containsExactly("Payment");
	}

	@Test
	void aProviderCopyCarriesOnlyTheImplementationsThatAreHits() throws IOException {
		// Two versions under one provider: a drain hides the predecessor
		// from lookups — it must not ride along inside the provider copy,
		// and the consumer must be able to pair the one reference with the
		// one implementation.
		ServiceInterface payment = payment();
		ServiceProvider provider = provider("payments", payment, FlavorKind.REST);
		ServiceImplementation v2 = ServicesFactory.eINSTANCE.createServiceImplementation();
		v2.setName(provider.getImplementations().get(0).getName());
		v2.setVersion("2.0.0");
		v2.setImplementationId("org.example.Impl");
		v2.getServiceInterfaces().add(payment);
		provider.getImplementations().add(v2);
		ServiceReference refV2 = reference("ref-v2", provider);
		refV2.getRegistration().setImplementation(v2);
		lookup.results.add(refV2);
		lookup.resolves(refV2, v2);
		v2.setReplaces(provider.getImplementations().get(0));

		LocalServiceRegistry envelope = service.getServiceReferences("Payment", null, null, null, null);

		assertThat(envelope.getReferences()).hasSize(1);
		assertThat(envelope.getProviders()).hasSize(1);
		assertThat(envelope.getProviders().get(0).getImplementations())
				.extracting(ServiceImplementation::getVersion)
				.containsExactly("2.0.0");
		assertThat(envelope.getProviders().get(0).getImplementations().get(0).getReplaces())
				.as("a link to the pruned predecessor would be a dangling href")
				.isNull();
		assertThat(RestTestSupport.xml(resourceSets, new XmiBundle(answerRoots(envelope))))
				.as("serializes").contains("version=\"2.0.0\"");
	}

	@Test
	void anEmptyHitListYieldsAnEmptyEnvelopeAndNoSiblings() {
		LocalServiceRegistry envelope = service.getServiceReferences("Payment", null, null, null, null);

		assertThat(envelope.getReferences()).isEmpty();
		assertThat(envelope.getProviders()).isEmpty();
		assertThat(answerRoots(envelope))
				.as("nothing to carry, so nothing is carried")
				.containsExactly(envelope);
	}

	@Test
	void heartbeatPassesReferenceIdAndIntervalToTheBroker() {
		RecordingImplementations implementations = new RecordingImplementations();
		service.implementations = implementations;

		Diagnostic answer = service.heartbeat("ref-42", 7);

		assertThat(answer).isSameAs(implementations.answer);
		assertThat(implementations.referenceId).isEqualTo("ref-42");
		assertThat(implementations.intervalSeconds).isEqualTo(7);
	}

	private static final class RecordingImplementations implements BrokerImplementations {
		String referenceId;
		long intervalSeconds;
		Diagnostic answer = RestTestSupport.diagnostic(DiagnosticSeverity.OK, DdsrDiagnostics.CODE_OK, "heartbeat accepted");

		@Override
		public Diagnostic heartbeat(String referenceId, long intervalSeconds) {
			this.referenceId = referenceId;
			this.intervalSeconds = intervalSeconds;
			return answer;
		}

		@Override
		public Diagnostic publishImplementation(ServiceProvider p, ServiceImplementation i) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Diagnostic withdrawImplementation(ServiceProvider p, ServiceImplementation i) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Diagnostic modifyImplementation(ServiceProvider p, ServiceImplementation i) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ServiceRegistration registerService(ServiceProvider p, ServiceImplementation i) {
			throw new UnsupportedOperationException();
		}
	}
}
