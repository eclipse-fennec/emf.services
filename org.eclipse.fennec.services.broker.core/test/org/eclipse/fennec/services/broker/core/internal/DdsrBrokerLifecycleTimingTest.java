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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.EventDocument;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Timing between registration, unregistration and change, seen through
 * the event stream and the registration handles — the interleavings a
 * consumer actually experiences.
 * <p>
 * Extra edges beyond the agreed list: the withdraw-of-a-neighbour case
 * (withdrawing impl A must not touch impl B's registration or events)
 * and the double-withdraw where the SECOND call races on the same live
 * objects (not just wire copies).
 */
class DdsrBrokerLifecycleTimingTest {

	@TempDir
	Path tmp;

	private RecordingSink sink;

	private DdsrBrokerImpl broker;

	@BeforeEach
	void setUp() {
		sink = new RecordingSink();
		broker = new DdsrBrokerImpl(tmp.resolve("broker-state.xmi"), new InMemoryLookupBackend(), sink);
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
	}

	// ------------------------------------------------------------------
	// Fixtures
	// ------------------------------------------------------------------

	private static final class RecordingSink implements EventSink {

		private final List<ServiceEvent> received = new ArrayList<>();

		@Override
		public void publish(ServiceEvent event) {
			received.add(event);
		}

		List<ServiceEventType> types() {
			return received.stream().map(ServiceEvent::getType).toList();
		}
	}

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

	// ------------------------------------------------------------------
	// Republish ordering and self-containment
	// ------------------------------------------------------------------

	@Test
	void republishEmitsSelfContainedUnregisteringBeforeRegistered() {
		ServiceProvider first = provider("prov-a", "impl-a");
		broker.publishImplementation(first, soleImpl(first));
		ServiceProvider second = provider("prov-a", "impl-a");
		broker.publishImplementation(second, soleImpl(second));

		assertThat(sink.types())
				.as("the consumer must see the old reference go before the replacement arrives")
				.containsExactly(ServiceEventType.REGISTERED,
						ServiceEventType.UNREGISTERING,
						ServiceEventType.REGISTERED);

		// BOTH events of the republish are self-contained: the interface
		// names are determinable from the event document alone — that is
		// what keeps MQTT off the _unknown topic and SSE routing precise.
		ServiceEvent unregistering = sink.received.get(1);
		ServiceEvent registered = sink.received.get(2);
		assertThat(EventDocument.interfaceNamesOf(unregistering, broker))
				.as("UNREGISTERING must name its interfaces")
				.containsExactly("Payment");
		assertThat(EventDocument.interfaceNamesOf(registered, broker))
				.containsExactly("Payment");
		assertThat(unregistering.getReference().getId())
				.as("the UNREGISTERING names the OLD reference, the REGISTERED the new one")
				.isNotEqualTo(registered.getReference().getId());
	}

	@Test
	void withdrawEmitsASelfContainedUnregistering() {
		ServiceProvider p = provider("prov-a", "impl-a");
		broker.publishImplementation(p, soleImpl(p));

		broker.withdrawImplementation(p, soleImpl(p));

		ServiceEvent unregistering = sink.received.get(sink.received.size() - 1);
		assertThat(unregistering.getType()).isEqualTo(ServiceEventType.UNREGISTERING);
		assertThat(EventDocument.interfaceNamesOf(unregistering, broker))
				.as("the withdraw event carries the interfaces although the lookup no longer resolves")
				.containsExactly("Payment");
	}

	// ------------------------------------------------------------------
	// Double withdraw / withdraw of the unknown
	// ------------------------------------------------------------------

	@Test
	void withdrawingSomethingNeverPublishedEmitsNothingAndChangesNothing() {
		ServiceProvider p = provider("prov-a", "impl-a");

		Diagnostic d = broker.withdrawImplementation(p, soleImpl(p));

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED);
		assertThat(sink.received).isEmpty();
		assertThat(broker.liveRegistry().getImplementations()).isEmpty();
	}

	@Test
	void aDoubleWithdrawOnTheSameLiveObjectsIsRefusedWithoutASecondEvent() {
		ServiceProvider p = provider("prov-a", "impl-a");
		broker.publishImplementation(p, soleImpl(p));
		ServiceImplementation liveImpl = soleImpl(p);

		Diagnostic first = broker.withdrawImplementation(p, liveImpl);
		Diagnostic second = broker.withdrawImplementation(p, liveImpl);

		assertThat(first.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		assertThat(second.getCode())
				.as("the second withdraw races on the same live objects and must be refused")
				.isEqualTo(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED);
		assertThat(sink.types())
				.as("exactly one UNREGISTERING, never two")
				.containsExactly(ServiceEventType.REGISTERED, ServiceEventType.UNREGISTERING);
	}

	@Test
	void withdrawingOneImplementationLeavesItsNeighbourUntouched() {
		ServiceProvider a = provider("prov-a", "impl-a");
		broker.publishImplementation(a, soleImpl(a));
		ServiceProvider b = provider("prov-b", "impl-b");
		broker.publishImplementation(b, soleImpl(b));

		broker.withdrawImplementation(a, soleImpl(a));

		assertThat(broker.getServiceReferences("Payment", null, null))
				.as("the neighbour keeps serving")
				.hasSize(1);
		assertThat(broker.getServiceReferences("Payment", null, null).get(0).getProvider().getName())
				.isEqualTo("prov-b");
	}

	// ------------------------------------------------------------------
	// Registration handles carry the model links (side-map replacement)
	// ------------------------------------------------------------------

	@Test
	void registerServiceReturnsAFullyLinkedRegistration() {
		ServiceProvider p = provider("prov-a", "impl-a");

		ServiceRegistration registration = broker.registerService(p, soleImpl(p));

		assertThat(registration).isNotNull();
		assertThat(registration.getImplementation())
				.as("the model ref replaced the implByRegistration side-map")
				.isSameAs(soleImpl(p));
		assertThat(registration.getProvider().getName()).isEqualTo("prov-a");
		assertThat(registration.getReference()).isNotNull();
		assertThat(registration.getReference().getRegistration()).isSameAs(registration);
		assertThat(registration.isUnregistered()).isFalse();
	}

	@Test
	void withdrawMarksTheRegistrationUnregisteredAndARepublishMintsAFreshOne() {
		ServiceProvider p = provider("prov-a", "impl-a");
		ServiceRegistration first = broker.registerService(p, soleImpl(p));
		ServiceImplementation impl = soleImpl(p);

		broker.withdrawImplementation((ServiceProvider) impl.eContainer(), impl);
		assertThat(first.isUnregistered())
				.as("the dead handle says so itself")
				.isTrue();
		assertThat(broker.getImplementationForReference(first.getReference()))
				.as("a withdrawn reference must not resolve")
				.isNull();

		// A republish is a fresh publish: the caller re-establishes the
		// containment (publish requires impl ∈ provider.implementations).
		p.getImplementations().add(impl);
		ServiceRegistration second = broker.registerService(p, impl);
		assertThat(second).isNotSameAs(first);
		assertThat(second.isUnregistered()).isFalse();
		assertThat(second.getReference().getId()).isNotEqualTo(first.getReference().getId());
	}
}
