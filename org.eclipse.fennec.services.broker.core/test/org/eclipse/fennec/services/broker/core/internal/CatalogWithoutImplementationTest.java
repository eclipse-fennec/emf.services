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

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A contract stands on its own (#271): it is put into the catalog
 * without an implementation, and it stays there whether anybody
 * implements it or not. An implementation may come later and may go
 * again; neither touches the catalog entry. Only the catalog's own
 * governance removes it, and that is refused while something still
 * implements it.
 *
 * <p>Every way the broker forgets something is walked here once with
 * a contract that nobody implements: the lookup, the last withdrawal,
 * the liveness sweep, the cold cache and the restart.
 */
class CatalogWithoutImplementationTest {

	private static final String INTERFACE = "Payment";

	@TempDir
	Path tmp;

	private Path snapshot;
	private DdsrBrokerImpl broker;

	@BeforeEach
	void setUp() {
		snapshot = tmp.resolve("broker-state.xmi");
		broker = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend());
		Diagnostic added = broker.addCatalogEntry(serviceInterface(), "test");
		assertThat(added.getSeverity()).as("add: %s", added.getMessage()).isEqualTo(DiagnosticSeverity.OK);
	}

	@Test
	void aLookupOfAContractNobodyImplementsAnswersNone() {
		assertThat(broker.getServiceReferences(INTERFACE, null, null)).isEmpty();
		assertThat(broker.getAllServiceReferences(INTERFACE, null, null)).isEmpty();
		assertThat(broker.getServiceReference(INTERFACE)).isNull();
		assertThat(catalogNames()).containsExactly(INTERFACE);
	}

	@Test
	void aLookupOfAContractThatIsNotInTheCatalogAnswersNoneAsWell() {
		assertThat(broker.getServiceReferences("Unknown", null, null)).isEmpty();
		assertThat(broker.getServiceReference("Unknown")).isNull();
	}

	@Test
	void theContractStaysWhenTheLastImplementationIsWithdrawn() {
		ServiceProvider p = publish();
		assertThat(broker.getServiceReferences(INTERFACE, null, null)).hasSize(1);

		Diagnostic withdrawn = broker.withdrawImplementation(p, soleImpl(p));

		assertThat(withdrawn.getSeverity()).isNotEqualTo(DiagnosticSeverity.ERROR);
		assertThat(broker.getServiceReferences(INTERFACE, null, null)).isEmpty();
		assertThat(catalogNames()).containsExactly(INTERFACE);
	}

	@Test
	void theContractStaysWhenItsProviderIsLost() {
		publish();
		String referenceId = broker.getServiceReferences(INTERFACE, null, null).get(0).getId();
		Instant armed = Instant.now();
		broker.heartbeat(referenceId, 1);

		assertThat(broker.retireLostProviders(armed.plusSeconds(5))).isEqualTo(1);

		assertThat(broker.getServiceReferences(INTERFACE, null, null)).isEmpty();
		assertThat(catalogNames()).containsExactly(INTERFACE);
	}

	@Test
	void theColdSweepLeavesAContractNobodyImplementsAlone() {
		assertThat(broker.coldifyIdle(Instant.now().plusSeconds(2))).isZero();

		assertThat(catalogNames()).containsExactly(INTERFACE);
		assertThat(broker.getServiceReferences(INTERFACE, null, null)).isEmpty();
	}

	@Test
	void theContractStaysWhenItsOnlyImplementationGoesCold() {
		publish();

		assertThat(broker.coldifyIdle(Instant.now().plusSeconds(2))).isEqualTo(1);

		assertThat(catalogNames()).containsExactly(INTERFACE);
	}

	@Test
	void aContractNobodyImplementsSurvivesARestart() {
		DdsrBrokerImpl restarted = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend());

		assertThat(restarted.getRegistry().getCatalog())
				.extracting(ServiceInterface::getName)
				.containsExactly(INTERFACE);
		assertThat(restarted.getServiceReferences(INTERFACE, null, null)).isEmpty();
	}

	@Test
	void anImplementationArrivingAfterARestartFindsItsContract() {
		DdsrBrokerImpl restarted = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend());
		broker = restarted;

		publish();

		assertThat(restarted.getServiceReferences(INTERFACE, null, null)).hasSize(1);
	}

	@Test
	void aContractNobodyImplementsCanBeRemoved() {
		Diagnostic removed = broker.removeCatalogEntry(serviceInterface(), "test");

		assertThat(removed.getSeverity()).as("remove: %s", removed.getMessage()).isEqualTo(DiagnosticSeverity.OK);
		assertThat(broker.getRegistry().getCatalog()).isEmpty();
		assertThat(broker.getServiceReferences(INTERFACE, null, null)).isEmpty();
	}

	@Test
	void removalIsRefusedWhileSomethingImplementsTheContract() {
		publish();

		Diagnostic refused = broker.removeCatalogEntry(serviceInterface(), "test");

		assertThat(refused.getCode()).isEqualTo(DdsrDiagnostics.CODE_CATALOG_HAS_LIVE_IMPLS);
		assertThat(catalogNames()).containsExactly(INTERFACE);
	}

	@Test
	void anImplementationCannotArriveOnceTheContractIsRemoved() {
		broker.removeCatalogEntry(serviceInterface(), "test");
		ServiceProvider p = provider();

		Diagnostic d = broker.publishImplementation(p, soleImpl(p));

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG);
	}

	// ------------------------------------------------------------------

	private List<String> catalogNames() {
		return broker.getRegistry().getCatalog().stream().map(ServiceInterface::getName).toList();
	}

	private ServiceProvider publish() {
		ServiceProvider p = provider();
		Diagnostic d = broker.publishImplementation(p, soleImpl(p));
		assertThat(d.getSeverity()).as("publish: %s", d.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		return p;
	}

	private static ServiceInterface serviceInterface() {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName(INTERFACE);
		si.setVersion("1.0.0");
		ServiceOperation operation = ServicesFactory.eINSTANCE.createServiceOperation();
		operation.setName("charge");
		si.getOperations().add(operation);
		return si;
	}

	private static ServiceProvider provider() {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("payments");
		provider.setVersion("1.0.0");
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName("payments-rest");
		impl.setVersion("1.0.0");
		impl.setImplementationId("test:payments-rest:1.0.0");
		impl.getServiceInterfaces().add(serviceInterface());
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName("payments-rest");
		flavor.setBasePath("/payments");
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		return provider;
	}

	private static ServiceImplementation soleImpl(ServiceProvider p) {
		return p.getImplementations().get(0);
	}
}
