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
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.payment;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.provider;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.reference;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** GET /references (#56): query parsing into a ConsumerCapability and the lookup-result envelope. */
class LookupResourceTest {

	private final RestTestSupport.FakeLookup lookup = new RestTestSupport.FakeLookup();
	private final RestTestSupport.ResourceSets resourceSets = new RestTestSupport.ResourceSets();
	private LookupResource resource;

	@BeforeEach
	void setUp() {
		resource = new LookupResource();
		resource.broker = lookup;
	}

	@Test
	void theInterfaceParameterIsRequired() {
		assertThat(resource.lookup(null, null, null, null, null).getStatus()).isEqualTo(400);
		assertThat(resource.lookup("  ", null, null, null, null).getStatus()).isEqualTo(400);
	}

	@Test
	void withoutFlavorsConsumerIdAndFingerprintNoCapabilityIsBuilt() {
		resource.lookup("Payment", "", "", " ", null);
		assertThat(lookup.lastInterface).isEqualTo("Payment");
		assertThat(lookup.lastFilter).as("blank filter means no filter").isNull();
		assertThat(lookup.lastCapability).isNull();
	}

	@Test
	void flavorsAreParsedFromCsvIgnoringWhitespaceAndUnknownTokens() {
		ConsumerCapability cap = LookupResource.parseCapability(" REST , MQTT,, GRPC ", null, null);
		assertThat(cap.getSupportedFlavors()).containsExactly(FlavorKind.REST, FlavorKind.MQTT);
	}

	@Test
	void consumerIdAndFingerprintLandOnTheCapability() {
		ConsumerCapability cap = LookupResource.parseCapability(null, "consumer-7", " sd1:abc ");
		assertThat(cap.getConsumerId()).isEqualTo("consumer-7");
		assertThat(cap.getSupportedFlavors()).isEmpty();
		assertThat(cap.getProperties()).hasSize(1);
		StringProperty requested = (StringProperty) cap.getProperties().get(0);
		assertThat(requested.getName()).isEqualTo("ddsr.fingerprint");
		assertThat(requested.getValue()).as("trimmed").isEqualTo("sd1:abc");
	}

	@Test
	void theFilterIsPassedThroughVerbatim() {
		resource.lookup("Payment", "(region=eu)", "REST", null, null);
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

		Response r = resource.lookup("Payment", null, null, null, null);

		assertThat(r.getStatus()).isEqualTo(200);
		XmiBundle bundle = (XmiBundle) r.getEntity();
		List<EObject> roots = bundle.roots();
		LocalServiceRegistry envelope = (LocalServiceRegistry) roots.get(0);
		assertThat(envelope.getName()).isEqualTo("lookup-result");
		assertThat(envelope.getReferences()).hasSize(1);
		ServiceReference copy = envelope.getReferences().get(0);
		assertThat(copy.getId()).isEqualTo("ref-1");
		assertThat(copy).as("the wire gets a copy, the registry keeps its objects").isNotSameAs(ref);
		assertThat(envelope.getProviders()).hasSize(1);
		assertThat(envelope.getProviders().get(0).getImplementations()).hasSize(1);
		assertThat(roots.stream().filter(ServiceInterface.class::isInstance).map(ServiceInterface.class::cast))
				.as("the contract rides along as a sibling root so the consumer can fingerprint it")
				.extracting(ServiceInterface::getName).containsExactly("Payment");
	}

	@Test
	void aProviderCopyCarriesOnlyTheImplementationsThatAreHits() throws java.io.IOException {
		// Two versions under one provider (name, version): a drain hides the
		// predecessor from lookups — it must not ride along inside the
		// provider copy, and the consumer must be able to pair the one
		// reference with the one implementation.
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

		v2.setReplaces(provider.getImplementations().get(0)); // the successor names the hidden predecessor

		Response r = resource.lookup("Payment", null, null, null, null);

		XmiBundle bundle = (XmiBundle) r.getEntity();
		LocalServiceRegistry envelope = (LocalServiceRegistry) bundle.roots().get(0);
		assertThat(envelope.getReferences()).hasSize(1);
		assertThat(envelope.getProviders()).hasSize(1);
		assertThat(envelope.getProviders().get(0).getImplementations())
				.extracting(ServiceImplementation::getVersion)
				.containsExactly("2.0.0");
		assertThat(envelope.getProviders().get(0).getImplementations().get(0).getReplaces())
				.as("a link to the pruned predecessor would be a dangling href")
				.isNull();
		assertThat(RestTestSupport.xml(resourceSets, bundle)).as("serializes").contains("version=\"2.0.0\"");
	}

	@Test
	void anEmptyHitListYieldsAnEmptyEnvelope() {
		Response r = resource.lookup("Payment", null, null, null, null);
		LocalServiceRegistry envelope = (LocalServiceRegistry) ((XmiBundle) r.getEntity()).roots().get(0);
		assertThat(envelope.getReferences()).isEmpty();
		assertThat(envelope.getProviders()).isEmpty();
	}
	// ------------------------------------------------------------------
	// PUT /references/{id}/heartbeat (#52)
	// ------------------------------------------------------------------

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

	@Test
	void heartbeatPassesReferenceIdAndIntervalToTheBroker() {
		RecordingImplementations implementations = new RecordingImplementations();
		resource.implementations = implementations;

		assertThat(resource.heartbeat("ref-42", 7).getStatus()).isEqualTo(200);
		assertThat(implementations.referenceId).isEqualTo("ref-42");
		assertThat(implementations.intervalSeconds).isEqualTo(7);
	}

	@Test
	void anUnknownReferenceAnswers404AndABadIntervalAnswers400() {
		RecordingImplementations implementations = new RecordingImplementations();
		resource.implementations = implementations;

		implementations.answer = RestTestSupport.diagnostic(DiagnosticSeverity.ERROR,
				DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED, "no live registration");
		assertThat(resource.heartbeat("gone", 30).getStatus()).as("the provider's cue to publish again").isEqualTo(404);

		implementations.answer = RestTestSupport.diagnostic(DiagnosticSeverity.ERROR,
				DdsrDiagnostics.CODE_HEARTBEAT_INVALID, "intervalSeconds must be positive");
		assertThat(resource.heartbeat("ref-42", 0).getStatus()).isEqualTo(400);
	}
}
