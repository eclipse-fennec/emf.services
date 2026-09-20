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

import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerSessions.SessionSnapshot;
import org.eclipse.fennec.services.common.CallOrigin;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the broker writes down about where a call came from (#125).
 *
 * <p>The transport binds the origin of the request it is serving and
 * the broker reads it from there, so these tests bind it the same way
 * the {@code OriginFilter} does. That is also the point of the last two
 * cases: an in-process call has no origin and must not inherit one, and
 * an identity the protocol itself carries beats the transport's.
 */
class BrokerRecordsOriginTest {

	private static final ClientOrigin PROD =
			ClientOrigin.of("payments-prod-eu", "5f2b8c1e-0f4a-4a6e-9b3d-7c1a2e8f4d60");

	private static final ClientOrigin OTHER =
			ClientOrigin.of("ops-console", "11112222-3333-4444-5555-666677778888");

	@TempDir
	Path tmp;

	private DdsrBrokerImpl broker;

	@BeforeEach
	void setUp() {
		broker = new DdsrBrokerImpl(tmp.resolve("broker-state.xmi"), new InMemoryLookupBackend());
	}

	@AfterEach
	void tearDown() {
		CallOrigin.clear();
	}

	// ------------------------------------------------------------------
	// Catalog
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a catalog entry remembers which system added it")
	void addedByIsRecorded() {
		CallOrigin.set(PROD);

		broker.addCatalogEntry(contract(), null);

		assertThat(entry().getAddedBy()).isEqualTo(PROD.token());
	}

	@Test
	@DisplayName("a deprecation names its own system, not the one that added the entry")
	void deprecatedByIsItsOwnRecord() {
		CallOrigin.set(PROD);
		broker.addCatalogEntry(contract(), null);

		CallOrigin.set(OTHER);
		ServiceInterface governance = contract();
		governance.setDeprecationReason("superseded");
		Diagnostic d = broker.deprecateCatalogEntry(governance, null);

		assertThat(d.getSeverity()).as(d.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		assertThat(entry().getStatus()).isEqualTo(CatalogStatus.DEPRECATED);
		assertThat(entry().getAddedBy()).isEqualTo(PROD.token());
		assertThat(entry().getDeprecatedBy()).isEqualTo(OTHER.token());
	}

	@Test
	@DisplayName("an explicit requestor beats the transport, because it was chosen on purpose")
	void explicitRequestorWins() {
		CallOrigin.set(PROD);

		broker.addCatalogEntry(contract(), "migration-script");

		assertThat(entry().getAddedBy()).isEqualTo("migration-script");
	}

	@Test
	@DisplayName("the contract's default requestor does not beat the origin — nobody chose that word")
	void theDefaultRequestorIsNotAChoice() {
		CallOrigin.set(PROD);

		// What the dispatcher hands over for a catalog call that named
		// nobody: the parameter's modelled defaultValue.
		broker.addCatalogEntry(contract(), ClientOrigin.ANONYMOUS);

		assertThat(entry().getAddedBy()).isEqualTo(PROD.token());
	}

	@Test
	@DisplayName("a call that carried no origin is anonymous, not the previous caller")
	void withoutAnOriginNothingIsInherited() {
		CallOrigin.set(PROD);
		CallOrigin.set(null);

		broker.addCatalogEntry(contract(), null);

		assertThat(entry().getAddedBy()).isEqualTo(ClientOrigin.ANONYMOUS);
	}

	// ------------------------------------------------------------------
	// Registrations
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a registration remembers which system published it")
	void publishedByIsRecorded() {
		broker.addCatalogEntry(contract(), null);
		CallOrigin.set(PROD);

		ServiceProvider p = provider("acme", "payments-1");
		Diagnostic d = broker.publishImplementation(p, p.getImplementations().get(0));

		assertThat(d.getSeverity()).as(d.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		assertThat(registration().getPublishedBy()).isEqualTo(PROD.token());
	}

	@Test
	@DisplayName("a modify in place moves the record on: the field names who is responsible now")
	void modifyTakesOverThePublishedBy() {
		broker.addCatalogEntry(contract(), null);
		CallOrigin.set(PROD);
		ServiceProvider p = provider("acme", "payments-1");
		broker.publishImplementation(p, p.getImplementations().get(0));

		CallOrigin.set(OTHER);
		ServiceProvider changed = provider("acme", "payments-1");
		changed.getImplementations().get(0).setDescription("now with a description");
		Diagnostic d = broker.modifyImplementation(changed, changed.getImplementations().get(0));

		assertThat(d.getSeverity()).as(d.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
		assertThat(registration().getPublishedBy()).isEqualTo(OTHER.token());
	}

	// ------------------------------------------------------------------
	// Sessions
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a session records where it came from, next to the name the consumer chose itself")
	void sessionOriginIsRecorded() {
		CallOrigin.set(PROD);

		ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
		session.setConsumerId("consumer-7");
		broker.putSession(session, List.of());

		SessionSnapshot stored = broker.getSession("consumer-7").orElseThrow();
		assertThat(stored.session().getConsumerId()).isEqualTo("consumer-7");
		assertThat(stored.session().getOrigin()).isEqualTo(PROD.token());
	}

	@Test
	@DisplayName("a renewal from a restarted runtime updates the origin")
	void renewalUpdatesTheOrigin() {
		ConsumerSession first = ServicesFactory.eINSTANCE.createConsumerSession();
		first.setConsumerId("consumer-7");
		CallOrigin.set(PROD);
		broker.putSession(first, List.of());

		ConsumerSession again = ServicesFactory.eINSTANCE.createConsumerSession();
		again.setConsumerId("consumer-7");
		CallOrigin.set(OTHER);
		broker.putSession(again, List.of());

		assertThat(broker.getSession("consumer-7").orElseThrow().session().getOrigin())
				.isEqualTo(OTHER.token());
	}

	// ------------------------------------------------------------------
	// Fixtures
	// ------------------------------------------------------------------

	private ServiceInterface entry() {
		return broker.getRegistry().getCatalog().stream()
				.filter(si -> "Payment".equals(si.getName()))
				.findFirst().orElseThrow();
	}

	private ServiceReference reference() {
		return broker.getServiceReferences("Payment", null, null).get(0);
	}

	private org.eclipse.fennec.services.ServiceRegistration registration() {
		return reference().getRegistration();
	}

	private static ServiceInterface contract() {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName("Payment");
		si.setVersion("1.0.0");
		ServiceOperation charge = ServicesFactory.eINSTANCE.createServiceOperation();
		charge.setName("charge");
		si.getOperations().add(charge);
		return si;
	}

	private static ServiceProvider provider(String providerName, String implName) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(providerName);
		provider.setVersion("1.0.0");
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(implName);
		impl.setVersion("1.0.0");
		impl.getServiceInterfaces().add(contract());
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName(implName);
		flavor.setBasePath("/payments");
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		return provider;
	}
}
