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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.fennec.services.BoolProperty;
import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.DoubleProperty;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.FloatProperty;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.LongProperty;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ShortProperty;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.broker.core.EventDocument;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behaviour of the in-memory broker: catalog validation, the dedup
 * rules that C1 was raised for, cross-ref rewiring, and the
 * persist/rehydrate round trip.
 * <p>
 * Plain JUnit — {@code DdsrBrokerImpl} is pure Java and takes its
 * snapshot path and lookup backend as constructor arguments, so no OSGi
 * runtime is involved.
 */
class DdsrBrokerImplTest {

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

	/**
	 * A provider carrying one implementation of {@code si}, shaped like a
	 * real publish body: the implementation references the interface and
	 * exposes a REST flavor with one operation flavor per operation.
	 */
	private static ServiceProvider provider(String providerName, String implName, ServiceInterface si) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(providerName);
		provider.setVersion("1.0.0");

		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(implName);
		impl.setVersion("1.0.0");
		impl.getServiceInterfaces().add(si);

		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName(implName);
		flavor.setHost("http://localhost:9091");
		flavor.setBasePath("/payments");
		for (ServiceOperation op : si.getOperations()) {
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

	private static boolean isError(Diagnostic d) {
		return d.getSeverity().getValue() >= DiagnosticSeverity.ERROR_VALUE;
	}

	/** Publishes a fresh provider for an interface already in the catalog. */
	private ServiceProvider publishFresh(String providerName, String implName, String interfaceName) {
		ServiceInterface stub = serviceInterface(interfaceName, "charge", "getBalance");
		ServiceProvider p = provider(providerName, implName, stub);
		Diagnostic d = broker.publishImplementation(p, soleImpl(p));
		assertThat(isError(d)).as("publish should succeed: %s", d.getMessage()).isFalse();
		return p;
	}

	// ------------------------------------------------------------------
	// Ownership and catalog validation
	// ------------------------------------------------------------------

	@Test
	void rejectsNullArguments() {
		assertThat(broker.publishImplementation(null, null).getCode())
				.isEqualTo(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION);
	}

	@Test
	void rejectsAnImplementationNotContainedInTheProvider() {
		ServiceInterface si = serviceInterface("Payment", "charge");
		broker.addCatalogEntry(si, "test");

		ServiceProvider owner = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		ServiceProvider stranger = provider("other", "other-impl", serviceInterface("Payment", "charge"));

		// Implementation belongs to `stranger`, not to `owner`.
		Diagnostic d = broker.publishImplementation(owner, soleImpl(stranger));

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION);
	}

	@Test
	void rejectsAnInterfaceThatIsNotInTheCatalog() {
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("NotInCatalog", "charge"));

		Diagnostic d = broker.publishImplementation(p, soleImpl(p));

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG);
	}

	@Test
	void rejectsAnInterfaceWithoutAnIdentifiableName() {
		ServiceInterface nameless = ServicesFactory.eINSTANCE.createServiceInterface();
		nameless.setVersion("1.0.0");
		ServiceProvider p = provider("payments-java", "impl", nameless);

		Diagnostic d = broker.publishImplementation(p, soleImpl(p));

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG);
	}

	@Test
	void publishingAgainstADeprecatedInterfaceWarnsButSucceeds() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		ServiceInterface deprecation = serviceInterface("Payment");
		deprecation.setDeprecationReason("superseded by Payment 2");
		broker.deprecateCatalogEntry(deprecation, "test");

		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		Diagnostic d = broker.publishImplementation(p, soleImpl(p));

		assertThat(isError(d)).as("deprecation must not block a publish").isFalse();
		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_INTERFACE_DEPRECATED);
		assertThat(d.getMessage()).contains("Payment");
		assertThat(broker.getRegistry().getImplementations())
				.as("the implementation is published despite the warning")
				.hasSize(1);
	}

	// ------------------------------------------------------------------
	// Rewiring onto the live catalog
	// ------------------------------------------------------------------

	@Test
	void rewiresTheInterfaceRefToTheLiveCatalogEntry() {
		ServiceInterface live = serviceInterface("Payment", "charge", "getBalance");
		broker.addCatalogEntry(live, "test");

		// The publish body carries its own stub copy, as a wire body does.
		ServiceInterface stub = serviceInterface("Payment", "charge", "getBalance");
		ServiceProvider p = provider("payments-java", "impl", stub);
		broker.publishImplementation(p, soleImpl(p));

		assertThat(soleImpl(p).getServiceInterfaces())
				.as("the stub must be replaced by the catalog instance itself")
				.containsExactly(live);
	}

	@Test
	void rewiresOperationRefsToTheLiveCatalogOperations() {
		ServiceInterface live = serviceInterface("Payment", "charge", "getBalance");
		broker.addCatalogEntry(live, "test");

		ServiceInterface stub = serviceInterface("Payment", "charge", "getBalance");
		ServiceProvider p = provider("payments-java", "impl", stub);
		broker.publishImplementation(p, soleImpl(p));

		RestFlavor flavor = (RestFlavor) soleImpl(p).getFlavors().get(0);
		assertThat(flavor.getOperationFlavors())
				.extracting(of -> ((RestOperationFlavor) of).getOperation())
				.as("operation refs must point at the live catalog operations, not the stub copies")
				.containsExactly(live.getOperations().get(0), live.getOperations().get(1));
	}

	// ------------------------------------------------------------------
	// Dedup — the rules C1 was raised for
	// ------------------------------------------------------------------

	@Test
	void reusesAProviderWithTheSameNameAndVersion() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");

		publishFresh("payments-java", "impl-a", "Payment");
		publishFresh("payments-java", "impl-b", "Payment");

		assertThat(broker.getRegistry().getProviders())
				.as("same (name, version) must not create a second provider")
				.hasSize(1);
		assertThat(broker.getRegistry().getProviders().get(0).getImplementations())
				.as("both implementations hang off the one provider")
				.hasSize(2);
	}

	@Test
	void republishingTheSameImplementationRetiresTheOldOne() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");

		publishFresh("payments-java", "payments-java-rest", "Payment");
		publishFresh("payments-java", "payments-java-rest", "Payment");

		assertThat(broker.getRegistry().getImplementations())
				.as("a republish replaces the implementation instead of accumulating (C1)")
				.hasSize(1);
		assertThat(lookup.getAllServiceReferences("Payment", null, null))
				.as("and leaves exactly one reference behind")
				.hasSize(1);
	}

	@Test
	void repeatedRepublishesDoNotAccumulate() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");

		for (int i = 0; i < 5; i++) {
			publishFresh("payments-java", "payments-java-rest", "Payment");
		}

		assertThat(broker.getRegistry().getImplementations()).hasSize(1);
		assertThat(broker.getRegistry().getProviders()).hasSize(1);
		assertThat(lookup.getAllServiceReferences("Payment", null, null)).hasSize(1);
	}

	@Test
	void differentImplementationNamesCoexist() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");

		publishFresh("payments-java", "impl-a", "Payment");
		publishFresh("payments-java", "impl-b", "Payment");

		assertThat(broker.getRegistry().getImplementations())
				.as("dedup keys on (name, version) — different names are distinct services")
				.hasSize(2);
		assertThat(lookup.getAllServiceReferences("Payment", null, null)).hasSize(2);
	}

	// ------------------------------------------------------------------
	// Withdraw
	// ------------------------------------------------------------------

	@Test
	void withdrawRemovesTheImplementationAndItsReference() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		ServiceProvider p = publishFresh("payments-java", "impl", "Payment");

		Diagnostic d = broker.withdrawImplementation(p, soleImpl(p));

		assertThat(isError(d)).isFalse();
		assertThat(broker.getRegistry().getImplementations()).isEmpty();
		assertThat(lookup.getAllServiceReferences("Payment", null, null)).isEmpty();
	}

	@Test
	void withdrawAlsoDetachesTheImplementationFromItsProvider() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		ServiceProvider p = publishFresh("payments-java", "impl", "Payment");
		ServiceImplementation impl = soleImpl(p);

		broker.withdrawImplementation(p, impl);

		assertThat(p.getImplementations())
				.as("a withdrawn implementation must not stay in the provider tree — "
						+ "it would keep being persisted and keep referencing its catalog interface")
				.isEmpty();
		assertThat(impl.eResource())
				.as("and must be out of the snapshot resource entirely")
				.isNull();
	}

	@Test
	void withdrawOfSomethingNeverPublishedIsAnError() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge"));

		Diagnostic d = broker.withdrawImplementation(p, soleImpl(p));

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED);
	}

	// ------------------------------------------------------------------
	// Catalog governance
	// ------------------------------------------------------------------

	@Test
	void addingTheSameCatalogEntryTwiceIsIdempotent() {
		assertThat(isError(broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test"))).isFalse();

		Diagnostic d = broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");

		assertThat(isError(d)).as("identical content is a no-op, not a conflict").isFalse();
		assertThat(d.getMessage()).contains("already present");
		assertThat(broker.getRegistry().getCatalog()).hasSize(1);
	}

	@Test
	void aDifferentContractUnderTheSameNameCoexists() {
		// (name, sd1) key (ACQUISITION §11.2): same name, different
		// signature — two independent catalog entries, no conflict.
		assertThat(isError(broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test"))).isFalse();

		Diagnostic d = broker.addCatalogEntry(serviceInterface("Payment", "charge", "refund"), "test");

		assertThat(isError(d)).isFalse();
		assertThat(d.getMessage()).contains("coexists");
		assertThat(broker.getRegistry().getCatalog()).hasSize(2);
	}

	@Test
	void aNameOnlyStubIsAmbiguousAcrossCoexistingContracts() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "refund"), "test");

		// name-only stub (no operations) cannot pick between the two
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment"));
		Diagnostic d = broker.publishImplementation(p, soleImpl(p));

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_CATALOG_ENTRY_AMBIGUOUS);
	}

	@Test
	void fullContentAddressesItsContractAmongCoexistingOnes() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "refund"), "test");

		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge", "refund"));
		Diagnostic d = broker.publishImplementation(p, soleImpl(p));

		assertThat(isError(d)).as("content picks the exact contract: %s", d.getMessage()).isFalse();
		// rewired onto the two-operation entry, not the one-operation one
		assertThat(soleImpl(p).getServiceInterfaces().get(0).getOperations()).hasSize(2);
	}

	@Test
	void aDriftedFullContractIsRefusedNotSilentlyRewired() {
		// The fingerprint promise: never false-equal. A full contract that
		// matches no catalog entry is drift — the publisher adds its
		// contract first (it coexists), instead of being silently rewired
		// onto a same-named different contract.
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");

		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		Diagnostic d = broker.publishImplementation(p, soleImpl(p));

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG);
		assertThat(d.getMessage()).contains("contract drift");
	}

	@Test
	void removingOneOfTwoCoexistingContractsIsScopedByIdentity() {
		ServiceInterface small = serviceInterface("Payment", "charge");
		ServiceInterface big = serviceInterface("Payment", "charge", "refund");
		broker.addCatalogEntry(small, "test");
		broker.addCatalogEntry(big, "test");
		// a live impl publishes against the BIG contract only
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge", "refund"));
		assertThat(isError(broker.publishImplementation(p, soleImpl(p)))).isFalse();

		// the unused small contract is removable despite the shared name…
		assertThat(isError(broker.removeCatalogEntry(serviceInterface("Payment", "charge"), "test")))
				.as("strict-reject must not be blocked by a same-named sibling")
				.isFalse();
		// …the used big one is not
		Diagnostic blocked = broker.removeCatalogEntry(serviceInterface("Payment", "charge", "refund"), "test");
		assertThat(blocked.getCode()).isEqualTo(DdsrDiagnostics.CODE_CATALOG_HAS_LIVE_IMPLS);
	}

	@Test
	void deprecationRecordsStatusAndReason() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		ServiceInterface deprecation = serviceInterface("Payment");
		deprecation.setDeprecationReason("superseded");

		Diagnostic d = broker.deprecateCatalogEntry(deprecation, "test");

		assertThat(isError(d)).isFalse();
		ServiceInterface inCatalog = broker.getRegistry().getCatalog().get(0);
		assertThat(inCatalog.getStatus()).isEqualTo(CatalogStatus.DEPRECATED);
		assertThat(inCatalog.getDeprecationReason()).isEqualTo("superseded");
	}

	@Test
	void removalIsRejectedWhileAnImplementationStillPublishesTheInterface() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		publishFresh("payments-java", "impl", "Payment");

		Diagnostic d = broker.removeCatalogEntry(serviceInterface("Payment"), "test");

		assertThat(d.getCode())
				.as("strict reject: a live implementation blocks removal")
				.isEqualTo(DdsrDiagnostics.CODE_CATALOG_HAS_LIVE_IMPLS);
		assertThat(broker.getRegistry().getCatalog()).hasSize(1);
	}

	@Test
	void removalSucceedsOnceTheImplementationIsWithdrawn() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		ServiceProvider p = publishFresh("payments-java", "impl", "Payment");

		Diagnostic withdrawn = broker.withdrawImplementation(p, soleImpl(p));
		assertThat(isError(withdrawn))
				.as("withdraw must succeed first: code=%s %s", withdrawn.getCode(), withdrawn.getMessage())
				.isFalse();
		assertThat(broker.getRegistry().getImplementations())
				.as("nothing published any more")
				.isEmpty();

		Diagnostic d = broker.removeCatalogEntry(serviceInterface("Payment"), "test");

		assertThat(isError(d))
				.as("removal should be free now: code=%s %s", d.getCode(), d.getMessage())
				.isFalse();
		assertThat(broker.getRegistry().getCatalog()).isEmpty();
	}

	@Test
	void removingAnUnknownEntryIsAnError() {
		Diagnostic d = broker.removeCatalogEntry(serviceInterface("Nope"), "test");

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND);
	}

	// ------------------------------------------------------------------
	// Persist and rehydrate — the restart path
	// ------------------------------------------------------------------

	@Test
	void stateSurvivesARestart() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		publishFresh("payments-java", "payments-java-rest", "Payment");

		InMemoryLookupBackend freshLookup = new InMemoryLookupBackend();
		DdsrBrokerImpl restarted = new DdsrBrokerImpl(snapshot, freshLookup);

		assertThat(restarted.getRegistry().getCatalog())
				.extracting(ServiceInterface::getName)
				.containsExactly("Payment");
		assertThat(restarted.getRegistry().getImplementations())
				.extracting(ServiceImplementation::getName)
				.containsExactly("payments-java-rest");
	}

	@Test
	void reindexProducesExactlyOneReferencePerImplementation() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		// Republish a few times first: the pre-C1-fix failure mode was that
		// accumulated implementations each produced a reference on restart.
		for (int i = 0; i < 3; i++) {
			publishFresh("payments-java", "payments-java-rest", "Payment");
		}

		InMemoryLookupBackend freshLookup = new InMemoryLookupBackend();
		new DdsrBrokerImpl(snapshot, freshLookup);

		assertThat(freshLookup.getAllServiceReferences("Payment", null, null))
				.as("one live implementation must yield one reference after rehydration (C1)")
				.hasSize(1);
	}

	@Test
	void rehydrationAssignsFreshReferenceIds() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		publishFresh("payments-java", "impl", "Payment");
		String idBefore = lookup.getAllServiceReferences("Payment", null, null).get(0).getId();

		InMemoryLookupBackend freshLookup = new InMemoryLookupBackend();
		new DdsrBrokerImpl(snapshot, freshLookup);
		ServiceReference after = freshLookup.getAllServiceReferences("Payment", null, null).get(0);

		assertThat(after.getId())
				.as("references are documented as not stable across restarts")
				.isNotEqualTo(idBefore);
		assertThat(after.getId()).isNotBlank();
	}

	@Test
	void aCorruptSnapshotStartsEmptyInsteadOfFailing() throws Exception {
		java.nio.file.Files.writeString(snapshot, "this is not XMI at all");

		DdsrBrokerImpl fresh = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend());

		assertThat(fresh.getRegistry()).isNotNull();
		assertThat(fresh.getRegistry().getCatalog()).isEmpty();
		assertThat(fresh.getRegistry().getImplementations()).isEmpty();
	}

	// ------------------------------------------------------------------
	// Rollback when the snapshot cannot be written
	// ------------------------------------------------------------------

	/**
	 * A broker whose snapshot path is a directory: the model works, but
	 * every {@code resource.save} fails. That is the cheapest way to
	 * exercise the persist-failure branch deterministically.
	 */
	private DdsrBrokerImpl brokerThatCannotPersist() throws IOException {
		Path unwritable = tmp.resolve("unwritable.xmi");
		Files.createDirectory(unwritable);
		return new DdsrBrokerImpl(unwritable, new InMemoryLookupBackend());
	}

	@Test
	void aFailedSaveIsReportedAsAnError() throws IOException {
		DdsrBrokerImpl stuck = brokerThatCannotPersist();

		Diagnostic d = stuck.addCatalogEntry(serviceInterface("Payment", "charge"), "test");

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_PERSISTENCE_FAILED);
	}

	@Test
	void addCatalogEntryRollsBackWhenTheSaveFails() throws IOException {
		DdsrBrokerImpl stuck = brokerThatCannotPersist();

		stuck.addCatalogEntry(serviceInterface("Payment", "charge"), "test");

		assertThat(stuck.getRegistry().getCatalog())
				.as("a rejected add must not stay in the in-memory catalog")
				.isEmpty();
	}

	/**
	 * A broker that already holds the state {@code broker} persisted, but
	 * whose own snapshot target has since become unwritable: the state is
	 * loaded, every further save fails. Needed for the mutations that
	 * change something existing rather than adding to an empty catalog.
	 */
	private DdsrBrokerImpl brokerOnUnwritableCopy(String name) throws IOException {
		return brokerOnUnwritableCopy(name, EventSink.NOOP);
	}

	private DdsrBrokerImpl brokerOnUnwritableCopy(String name, EventSink sink) throws IOException {
		Path copy = tmp.resolve(name);
		Files.copy(snapshot, copy);
		DdsrBrokerImpl stuck = new DdsrBrokerImpl(copy, new InMemoryLookupBackend(), sink);
		// Swap the file for a directory — the model keeps its loaded state,
		// resource.save() from now on throws.
		Files.delete(copy);
		Files.createDirectory(copy);
		return stuck;
	}

	@Test
	void deprecationIsRolledBackWhenTheSaveFails() throws IOException {
		broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		DdsrBrokerImpl stuck = brokerOnUnwritableCopy("deprecate.xmi");
		assertThat(stuck.getRegistry().getCatalog()).hasSize(1);

		ServiceInterface deprecation = serviceInterface("Payment");
		deprecation.setDeprecationReason("superseded");
		Diagnostic d = stuck.deprecateCatalogEntry(deprecation, "test");

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_PERSISTENCE_FAILED);
		ServiceInterface inCatalog = stuck.getRegistry().getCatalog().get(0);
		assertThat(inCatalog.getStatus())
				.as("status must be back to what it was before the failed save")
				.isEqualTo(CatalogStatus.ACTIVE);
		assertThat(inCatalog.getDeprecationReason())
				.as("and the reason must not have been written either")
				.isNull();
	}

	@Test
	void removalIsRolledBackWhenTheSaveFails() throws IOException {
		broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		broker.addCatalogEntry(serviceInterface("OrderQuery", "findById"), "test");
		DdsrBrokerImpl stuck = brokerOnUnwritableCopy("removal.xmi");

		Diagnostic d = stuck.removeCatalogEntry(serviceInterface("Payment"), "test");

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_PERSISTENCE_FAILED);
		assertThat(stuck.getRegistry().getCatalog())
				.extracting(ServiceInterface::getName)
				.as("the entry must be back, and at its original position — "
						+ "catalog cross-refs use positional fragments")
				.containsExactly("Payment", "OrderQuery");
	}

	// ------------------------------------------------------------------
	// Event emission (A1) — transport-agnostic, see EventSink
	// ------------------------------------------------------------------

	/** Records what the broker hands to the sink, in order. */
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

	private DdsrBrokerImpl brokerWith(RecordingSink sink) {
		return new DdsrBrokerImpl(tmp.resolve("events.xmi"), new InMemoryLookupBackend(), sink);
	}

	@Test
	void publishEmitsRegistered() {
		RecordingSink sink = new RecordingSink();
		DdsrBrokerImpl b = brokerWith(sink);
		b.addCatalogEntry(serviceInterface("Payment", "charge"), "test");

		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		b.publishImplementation(p, soleImpl(p));

		assertThat(sink.types()).containsExactly(ServiceEventType.REGISTERED);
		ServiceEvent event = sink.received.get(0);
		assertThat(event.getReference()).isNotNull();
		assertThat(event.getReference().getProvider().getName()).isEqualTo("payments-java");
		assertThat(event.getTimestamp()).as("timestamp is useful for stream ordering").isNotNull();
	}

	@Test
	void addingACatalogEntryEmitsNothing() {
		RecordingSink sink = new RecordingSink();
		DdsrBrokerImpl b = brokerWith(sink);

		b.addCatalogEntry(serviceInterface("Payment", "charge"), "test");

		assertThat(sink.received)
				.as("catalog events are out of scope for the prototype (REQUIREMENTS §9)")
				.isEmpty();
	}

	@Test
	void aRejectedPublishEmitsNothing() {
		RecordingSink sink = new RecordingSink();
		DdsrBrokerImpl b = brokerWith(sink);

		// Interface is not in the catalog — the publish is refused.
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("NotInCatalog", "charge"));
		b.publishImplementation(p, soleImpl(p));

		assertThat(sink.received).isEmpty();
	}

	@Test
	void aPublishThatCannotBePersistedEmitsNothing() throws IOException {
		// The catalog entry has to exist, otherwise the publish is refused
		// before it ever reaches the save and the test would prove nothing.
		broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		RecordingSink sink = new RecordingSink();
		DdsrBrokerImpl stuck = brokerOnUnwritableCopy("emit-blocked.xmi", sink);

		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		Diagnostic d = stuck.publishImplementation(p, soleImpl(p));

		assertThat(d.getCode())
				.as("must fail at the save, not earlier — otherwise this tests the wrong thing")
				.isEqualTo(DdsrDiagnostics.CODE_PERSISTENCE_FAILED);
		assertThat(sink.received)
				.as("no event may escape for a change that was not persisted")
				.isEmpty();
	}

	@Test
	void withdrawEmitsUnregistering() {
		RecordingSink sink = new RecordingSink();
		DdsrBrokerImpl b = brokerWith(sink);
		b.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		b.publishImplementation(p, soleImpl(p));

		b.withdrawImplementation(p, soleImpl(p));

		assertThat(sink.types())
				.containsExactly(ServiceEventType.REGISTERED, ServiceEventType.UNREGISTERING);
	}

	@Test
	void aRepublishRetiresTheOldReferenceBeforeAnnouncingTheNewOne() {
		RecordingSink sink = new RecordingSink();
		DdsrBrokerImpl b = brokerWith(sink);
		b.addCatalogEntry(serviceInterface("Payment", "charge"), "test");

		ServiceProvider first = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		b.publishImplementation(first, soleImpl(first));
		ServiceProvider second = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		b.publishImplementation(second, soleImpl(second));

		assertThat(sink.types())
				.as("a consumer holding the old reference must see it go before the replacement arrives")
				.containsExactly(ServiceEventType.REGISTERED,
						ServiceEventType.UNREGISTERING,
						ServiceEventType.REGISTERED);
	}

	@Test
	void aThrowingSinkDoesNotBreakTheMutation() {
		EventSink hostile = event -> {
			throw new IllegalStateException("subscriber is broken");
		};
		DdsrBrokerImpl b = new DdsrBrokerImpl(tmp.resolve("hostile.xmi"), new InMemoryLookupBackend(), hostile);
		b.addCatalogEntry(serviceInterface("Payment", "charge"), "test");

		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		Diagnostic d = b.publishImplementation(p, soleImpl(p));

		assertThat(isError(d))
				.as("a failing subscriber may not undo a committed mutation")
				.isFalse();
		assertThat(b.getRegistry().getImplementations()).hasSize(1);
	}

	@Test
	void withoutASinkTheBrokerStillWorks() {
		// The two-argument constructor keeps the pre-A1 behaviour.
		DdsrBrokerImpl b = new DdsrBrokerImpl(tmp.resolve("nosink.xmi"), new InMemoryLookupBackend());
		b.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge"));

		assertThat(isError(b.publishImplementation(p, soleImpl(p)))).isFalse();
	}

	// ------------------------------------------------------------------
	// Registration bookkeeping
	// ------------------------------------------------------------------

	@Test
	void registerServiceReturnsARegistrationResolvingBackToItsImplementation() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		ServiceInterface stub = serviceInterface("Payment", "charge", "getBalance");
		ServiceProvider p = provider("payments-java", "impl", stub);

		ServiceRegistration reg = broker.registerService(p, soleImpl(p));

		assertThat(reg).isNotNull();
		assertThat(reg.isUnregistered()).isFalse();
		assertThat(broker.getImplementationForReference(reg.getReference()))
				.as("the reference must resolve back to the implementation it stands for")
				.isSameAs(soleImpl(p));
	}

	@Test
	void registerServiceReturnsNullWhenThePublishIsRejected() {
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("NotInCatalog", "charge"));

		assertThat(broker.registerService(p, soleImpl(p)))
				.as("a rejected publish must not hand out a registration")
				.isNull();
	}

	// ------------------------------------------------------------------
	// Withdraw lifecycle (D1) — self-contained events, wire-object
	// resolution, rollback
	// ------------------------------------------------------------------

	@Test
	void theUnregisteringEventIsSelfContained() {
		RecordingSink sink = new RecordingSink();
		DdsrBrokerImpl b = brokerWith(sink);
		b.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		b.publishImplementation(p, soleImpl(p));

		b.withdrawImplementation(p, soleImpl(p));

		ServiceEvent event = sink.received.get(sink.received.size() - 1);
		assertThat(event.getType()).isEqualTo(ServiceEventType.UNREGISTERING);
		// No lookup available (the impl is long gone) — the event itself
		// must carry enough to route it.
		assertThat(EventDocument.interfaceNamesOf(event, null))
				.as("a transport must be able to name the affected interface without the lookup")
				.containsExactly("Payment");
		assertThat(EventDocument.flavorsOf(event, null)).containsExactly(FlavorKind.REST);
		assertThat(event.getReference().eContainer())
				.as("the reference is a detached copy, not live broker state")
				.isNull();
		assertThat(event.getReference().getRegistration())
				.as("no live registration may leak into the event document")
				.isNull();
		assertThat(event.getReference().getProvider().getImplementations())
				.as("the provider subtree carries exactly the withdrawn implementation")
				.hasSize(1);
	}

	@Test
	void theUnregisteringEventKeepsTheWithdrawnReferenceId() {
		RecordingSink sink = new RecordingSink();
		DdsrBrokerImpl b = brokerWith(sink);
		b.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		b.publishImplementation(p, soleImpl(p));
		String liveId = sink.received.get(0).getReference().getId();

		b.withdrawImplementation(p, soleImpl(p));

		assertThat(sink.received.get(1).getReference().getId())
				.as("consumers match UNREGISTERING by reference id")
				.isEqualTo(liveId);
	}

	@Test
	void withdrawResolvesWireShapedObjectsByName() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		publishFresh("payments-java", "impl", "Payment");

		// A second, freshly parsed provider/impl pair, as DELETE
		// /implementations hands it in: same names, different identity.
		ServiceProvider wireCopy = provider("payments-java", "impl",
				serviceInterface("Payment", "charge", "getBalance"));
		Diagnostic d = broker.withdrawImplementation(wireCopy, soleImpl(wireCopy));

		assertThat(isError(d))
				.as("withdraw over the wire must resolve to the live implementation: %s", d.getMessage())
				.isFalse();
		assertThat(broker.liveRegistry().getImplementations()).isEmpty();
		assertThat(lookup.getAllServiceReferences("Payment", null, null)).isEmpty();
	}

	@Test
	void withdrawIsRolledBackWhenTheSaveFails() throws IOException {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		publishFresh("payments-java", "impl", "Payment");
		RecordingSink sink = new RecordingSink();
		DdsrBrokerImpl stuck = brokerOnUnwritableCopy("withdraw-rollback.xmi", sink);
		ServiceImplementation liveImpl = stuck.liveRegistry().getImplementations().get(0);
		ServiceProvider liveProvider = (ServiceProvider) liveImpl.eContainer();

		Diagnostic d = stuck.withdrawImplementation(liveProvider, liveImpl);

		assertThat(d.getCode()).isEqualTo(DdsrDiagnostics.CODE_PERSISTENCE_FAILED);
		assertThat(sink.received)
				.as("no event may be announced for a withdrawal that was not persisted")
				.isEmpty();
		assertThat(stuck.liveRegistry().getImplementations())
				.as("the implementation must be back in the registry")
				.containsExactly(liveImpl);
		assertThat(liveProvider.getImplementations())
				.as("and back in its provider containment")
				.containsExactly(liveImpl);
		assertThat(stuck.getServiceReferences("Payment", null, null))
				.as("and findable again")
				.hasSize(1);
	}

	// ------------------------------------------------------------------
	// Service properties (D8) and fingerprints (D6) on the reference
	// ------------------------------------------------------------------

	private static void addAllPropertyTypes(ServiceImplementation impl) {
		StringProperty lang = ServicesFactory.eINSTANCE.createStringProperty();
		lang.setName("ddsr.provider.lang");
		lang.setValue("java");
		impl.getProperties().add(lang);
		IntProperty ranking = ServicesFactory.eINSTANCE.createIntProperty();
		ranking.setName("service.ranking");
		ranking.setValue(10);
		impl.getProperties().add(ranking);
		LongProperty maxAmount = ServicesFactory.eINSTANCE.createLongProperty();
		maxAmount.setName("payments.maxAmountCents");
		maxAmount.setValue(5_000_000_000L);
		impl.getProperties().add(maxAmount);
		DoubleProperty feeRate = ServicesFactory.eINSTANCE.createDoubleProperty();
		feeRate.setName("payments.feeRate");
		feeRate.setValue(0.025d);
		impl.getProperties().add(feeRate);
		FloatProperty timeout = ServicesFactory.eINSTANCE.createFloatProperty();
		timeout.setName("payments.timeoutSeconds");
		timeout.setValue(1.5f);
		impl.getProperties().add(timeout);
		ShortProperty retries = ServicesFactory.eINSTANCE.createShortProperty();
		retries.setName("payments.maxRetries");
		retries.setValue((short) 3);
		impl.getProperties().add(retries);
		BoolProperty sandbox = ServicesFactory.eINSTANCE.createBoolProperty();
		sandbox.setName("payments.sandbox");
		sandbox.setValue(true);
		impl.getProperties().add(sandbox);
		StringListProperty tags = ServicesFactory.eINSTANCE.createStringListProperty();
		tags.setName("payments.tags");
		tags.getValue().add("demo");
		tags.getValue().add("payments");
		impl.getProperties().add(tags);
	}

	private ServiceReference publishWithAllPropertyTypes() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge", "getBalance"));
		addAllPropertyTypes(soleImpl(p));
		Diagnostic d = broker.publishImplementation(p, soleImpl(p));
		assertThat(isError(d)).as("publish should succeed: %s", d.getMessage()).isFalse();
		return lookup.getAllServiceReferences("Payment", null, null).get(0);
	}

	private static Property propertyNamed(ServiceReference ref, String name) {
		return ref.getProperties().stream()
				.filter(property -> name.equals(property.getName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no property named " + name + " on the reference"));
	}

	@Test
	void implementationPropertiesLandTypedOnTheReference() {
		ServiceReference ref = publishWithAllPropertyTypes();

		assertThat(((StringProperty) propertyNamed(ref, "ddsr.provider.lang")).getValue()).isEqualTo("java");
		assertThat(((IntProperty) propertyNamed(ref, "service.ranking")).getValue()).isEqualTo(10);
		assertThat(((LongProperty) propertyNamed(ref, "payments.maxAmountCents")).getValue())
				.isEqualTo(5_000_000_000L);
		assertThat(((DoubleProperty) propertyNamed(ref, "payments.feeRate")).getValue()).isEqualTo(0.025d);
		assertThat(((FloatProperty) propertyNamed(ref, "payments.timeoutSeconds")).getValue()).isEqualTo(1.5f);
		assertThat(((ShortProperty) propertyNamed(ref, "payments.maxRetries")).getValue()).isEqualTo((short) 3);
		assertThat(((BoolProperty) propertyNamed(ref, "payments.sandbox")).isValue()).isTrue();
		assertThat(((StringListProperty) propertyNamed(ref, "payments.tags")).getValue())
				.containsExactly("demo", "payments");
	}

	@Test
	void referencePropertiesAreCopiesNotTheImplementationsOwn() {
		ServiceReference ref = publishWithAllPropertyTypes();
		ServiceImplementation liveImpl = broker.liveRegistry().getImplementations().get(0);

		Property onRef = propertyNamed(ref, "ddsr.provider.lang");

		assertThat(liveImpl.getProperties())
				.as("the reference must not steal the implementation's containment children")
				.isNotEmpty();
		assertThat(liveImpl.getProperties()).doesNotContain(onRef);
	}

	@Test
	void anEmptyStringListStaysPresentButEmpty() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
		ServiceProvider p = provider("payments-java", "impl", serviceInterface("Payment", "charge"));
		StringListProperty empty = ServicesFactory.eINSTANCE.createStringListProperty();
		empty.setName("payments.tags");
		soleImpl(p).getProperties().add(empty);
		broker.publishImplementation(p, soleImpl(p));

		ServiceReference ref = lookup.getAllServiceReferences("Payment", null, null).get(0);

		assertThat(((StringListProperty) propertyNamed(ref, "payments.tags")).getValue())
				.as("empty list = no values; the property itself is still present")
				.isEmpty();
	}

	@Test
	void theReferenceCarriesTheFingerprintOfTheLiveCatalogEntry() {
		ServiceReference ref = publishWithAllPropertyTypes();
		ServiceInterface inCatalog = broker.liveRegistry().getCatalog().get(0);

		StringProperty fingerprint = (StringProperty) propertyNamed(ref, "ddsr.fingerprint");

		assertThat(fingerprint.getValue())
				.as("the broker's fingerprint must be computed over its live catalog entry")
				.isEqualTo(ServiceDescriptionFingerprint.fingerprint(inCatalog))
				.startsWith("sd1:");
	}

	@Test
	void addCatalogEntryReportsTheFingerprintToThePublisher() {
		ServiceInterface si = serviceInterface("Payment", "charge");

		Diagnostic d = broker.addCatalogEntry(si, "test");

		assertThat(d.getMessage())
				.as("the producer compares its own fingerprint against this, no extra round trip")
				.contains(ServiceDescriptionFingerprint.fingerprint(si));
	}

	@Test
	void rehydratedReferencesCarryPropertiesAndFingerprintAgain() {
		publishWithAllPropertyTypes();

		InMemoryLookupBackend freshLookup = new InMemoryLookupBackend();
		new DdsrBrokerImpl(snapshot, freshLookup);
		ServiceReference ref = freshLookup.getAllServiceReferences("Payment", null, null).get(0);

		assertThat(((StringProperty) propertyNamed(ref, "ddsr.provider.lang")).getValue()).isEqualTo("java");
		assertThat(((StringProperty) propertyNamed(ref, "ddsr.fingerprint")).getValue()).startsWith("sd1:");
	}

	// ------------------------------------------------------------------
	// LDAP filter end-to-end (D7)
	// ------------------------------------------------------------------

	@Test
	void lookupMatchesAnLdapFilterAgainstTheReferenceProperties() {
		publishWithAllPropertyTypes();

		assertThat(broker.getServiceReferences("Payment",
				"(&(ddsr.provider.lang=java)(service.ranking>=5)(payments.sandbox=true))", null))
				.hasSize(1);
		assertThat(broker.getServiceReferences("Payment", "(payments.tags=demo)", null))
				.as("StringList matches when any element matches")
				.hasSize(1);
		assertThat(broker.getServiceReferences("Payment", "(ddsr.provider.lang=typescript)", null))
				.isEmpty();
		assertThat(broker.getServiceReferences("Payment", "(payments.maxAmountCents>=6000000000)", null))
				.as("numeric comparison, not lexicographic")
				.isEmpty();
	}

	@Test
	void aBrokenFilterYieldsAnEmptyResultNotAnException() {
		publishWithAllPropertyTypes();

		assertThat(broker.getServiceReferences("Payment", "((oops", null))
				.as("a consumer typo must not 500 the broker")
				.isEmpty();
	}

	// ------------------------------------------------------------------
	// getRegistry() — detached copy (D9)
	// ------------------------------------------------------------------

	@Test
	void getRegistryReturnsADetachedCopy() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");

		RemoteServiceRegistry copy = broker.getRegistry();
		copy.getCatalog().clear();

		assertThat(broker.liveRegistry().getCatalog())
				.as("mutating the returned registry must not touch broker state")
				.hasSize(1);
		assertThat(copy).isNotSameAs(broker.liveRegistry());
		assertThat(copy.eResource().getURI().toString())
				.as("the copy is parked in the opaque in-memory resource, not the snapshot file")
				.isEqualTo("services:registry");
	}

	@Test
	void getRegistryRewiresProvidersAndImplementationsToTheCopies() {
		broker.addCatalogEntry(serviceInterface("Payment", "charge", "getBalance"), "test");
		publishFresh("payments-java", "impl", "Payment");

		RemoteServiceRegistry copy = broker.getRegistry();

		assertThat(copy.getProviders()).hasSize(1);
		assertThat(copy.getProviders().get(0))
				.isNotSameAs(broker.liveRegistry().getProviders().get(0));
		assertThat(copy.getImplementations()).hasSize(1);
		assertThat(copy.getImplementations().get(0).eContainer())
				.as("the copied registry references the copied provider tree, not live objects")
				.isSameAs(copy.getProviders().get(0));
	}
}
