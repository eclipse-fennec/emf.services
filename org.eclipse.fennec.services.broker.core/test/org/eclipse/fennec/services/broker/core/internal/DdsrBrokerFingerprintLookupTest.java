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

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;
import org.eclipse.fennec.services.fingerprint.ServiceImplementationFingerprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Contract addressing over sd1 fingerprints (ACQUISITION.md §11.2):
 * a consumer that declares the contract it speaks — as a
 * {@code ddsr.fingerprint} property on its {@link ConsumerCapability} —
 * only ever receives implementations of exactly that contract.
 * Compatibility = identical sd1; no range semantics.
 * <p>
 * Extra edges pinned here beyond the agreed list: a blank fingerprint
 * property means "no filter"; the filter composes with flavor matching
 * and with the LDAP filter (all conditions must hold); and the
 * comparison runs against the CATALOG truth — a provider whose local
 * stub drifted from the catalog falls out of compatible lookups
 * (§11.1: contract drift is a policy question, not a silent match).
 */
class DdsrBrokerFingerprintLookupTest {

	@TempDir
	Path tmp;

	private DdsrBrokerImpl broker;

	private ServiceInterface catalogEntry;

	@BeforeEach
	void setUp() {
		broker = new DdsrBrokerImpl(tmp.resolve("broker-state.xmi"), new InMemoryLookupBackend());
		catalogEntry = serviceInterface("Payment", "charge", "getBalance");
		broker.addCatalogEntry(catalogEntry, "test");
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

	private static ServiceProvider provider(String providerName, String implName, ServiceInterface... interfaces) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(providerName);
		provider.setVersion("1.0.0");
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(implName);
		impl.setVersion("1.0.0");
		for (ServiceInterface si : interfaces) {
			impl.getServiceInterfaces().add(si);
		}
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName(implName);
		flavor.setBasePath("/payments");
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		return provider;
	}

	private void publish(ServiceProvider p) {
		Diagnostic d = broker.publishImplementation(p, p.getImplementations().get(0));
		assertThat(d.getSeverity()).as("publish: %s", d.getMessage()).isNotEqualTo(DiagnosticSeverity.ERROR);
	}

	/**
	 * A capability declaring the REST flavor (as every real SDK does)
	 * plus, optionally, the contract it speaks. Note the composition
	 * trap this pins implicitly: a capability with EMPTY supportedFlavors
	 * matches nothing (existing matchesFlavor semantics) — a
	 * fingerprint-only capability must therefore always carry the
	 * consumer's flavors too.
	 */
	private static ConsumerCapability speaks(String fingerprint) {
		ConsumerCapability capability = ServicesFactory.eINSTANCE.createConsumerCapability();
		capability.getSupportedFlavors().add(FlavorKind.REST);
		if (fingerprint != null) {
			StringProperty property = ServicesFactory.eINSTANCE.createStringProperty();
			property.setName("ddsr.fingerprint");
			property.setValue(fingerprint);
			capability.getProperties().add(property);
		}
		return capability;
	}

	// ------------------------------------------------------------------
	// Exact-match addressing
	// ------------------------------------------------------------------

	@Test
	void theMatchingContractIsReturnedAForeignOneIsNot() {
		publish(provider("prov-a", "impl-a", serviceInterface("Payment", "charge", "getBalance")));
		String catalogFingerprint = ServiceDescriptionFingerprint.fingerprint(catalogEntry);

		assertThat(broker.getServiceReferences("Payment", null, speaks(catalogFingerprint)))
				.as("identical contract → compatible")
				.hasSize(1);
		assertThat(broker.getServiceReferences("Payment", null,
				speaks("sd1:0000000000000000000000000000000000000000000000000000000000000000")))
				.as("foreign hash → nothing is compatible")
				.isEmpty();
	}

	@Test
	void withoutAFingerprintPropertyNothingIsFiltered() {
		publish(provider("prov-a", "impl-a", serviceInterface("Payment", "charge", "getBalance")));

		assertThat(broker.getServiceReferences("Payment", null, speaks(null))).hasSize(1);
		assertThat(broker.getServiceReferences("Payment", null, null)).hasSize(1);
	}

	@Test
	void aBlankFingerprintPropertyMeansNoFilter() {
		publish(provider("prov-a", "impl-a", serviceInterface("Payment", "charge", "getBalance")));

		assertThat(broker.getServiceReferences("Payment", null, speaks("  ")))
				.as("blank value is treated as absent, not as never-matching")
				.hasSize(1);
	}

	// ------------------------------------------------------------------
	// The catalog is the truth (§11.1 / §11.2)
	// ------------------------------------------------------------------

	@Test
	void theCatalogContractDecidesNotTheProvidersStub() {
		// The provider's local view drifted: its stub knows only one of
		// the two catalog operations. The broker rewires the impl onto the
		// live catalog entry at publish.
		ServiceInterface driftedStub = serviceInterface("Payment", "charge");
		String driftedFingerprint = ServiceDescriptionFingerprint.fingerprint(driftedStub);
		publish(provider("prov-a", "impl-a", driftedStub));

		String catalogFingerprint = ServiceDescriptionFingerprint.fingerprint(catalogEntry);
		assertThat(catalogFingerprint).isNotEqualTo(driftedFingerprint);

		assertThat(broker.getServiceReferences("Payment", null, speaks(catalogFingerprint)))
				.as("a consumer speaking the CATALOG contract finds the impl")
				.hasSize(1);
		assertThat(broker.getServiceReferences("Payment", null, speaks(driftedFingerprint)))
				.as("the provider's drifted local view is not an addressable contract")
				.isEmpty();
	}

	// ------------------------------------------------------------------
	// Multi-interface implementations
	// ------------------------------------------------------------------

	@Test
	void aMultiInterfaceImplMatchesOnAnyOfItsContracts() {
		ServiceInterface billing = serviceInterface("Billing", "invoice");
		broker.addCatalogEntry(billing, "test");
		publish(provider("prov-a", "impl-a",
				serviceInterface("Payment", "charge", "getBalance"),
				serviceInterface("Billing", "invoice")));

		String paymentFingerprint = ServiceDescriptionFingerprint.fingerprint(catalogEntry);
		String billingFingerprint = ServiceDescriptionFingerprint.fingerprint(billing);
		assertThat(broker.getServiceReferences("Payment", null, speaks(paymentFingerprint)))
				.as("matches via ddsr.fingerprint.Payment")
				.hasSize(1);
		assertThat(broker.getServiceReferences("Billing", null, speaks(billingFingerprint)))
				.as("matches via ddsr.fingerprint.Billing")
				.hasSize(1);
		assertThat(broker.getServiceReferences("Payment", null, speaks("sd1:" + "f".repeat(64))))
				.isEmpty();
	}

	// ------------------------------------------------------------------
	// Composition with the other lookup dimensions
	// ------------------------------------------------------------------

	@Test
	void fingerprintAndLdapFilterMustBothMatch() {
		ServiceProvider p = provider("prov-a", "impl-a", serviceInterface("Payment", "charge", "getBalance"));
		StringProperty lang = ServicesFactory.eINSTANCE.createStringProperty();
		lang.setName("lang");
		lang.setValue("java");
		p.getImplementations().get(0).getProperties().add(lang);
		publish(p);
		String catalogFingerprint = ServiceDescriptionFingerprint.fingerprint(catalogEntry);

		assertThat(broker.getServiceReferences("Payment", "(lang=java)", speaks(catalogFingerprint)))
				.hasSize(1);
		assertThat(broker.getServiceReferences("Payment", "(lang=ts)", speaks(catalogFingerprint)))
				.as("matching contract but failing filter → no result")
				.isEmpty();
		assertThat(broker.getServiceReferences("Payment", "(lang=java)", speaks("sd1:" + "e".repeat(64))))
				.as("matching filter but foreign contract → no result")
				.isEmpty();
	}

	@Test
	void getAllServiceReferencesAppliesTheFingerprintFilterToo() {
		publish(provider("prov-a", "impl-a", serviceInterface("Payment", "charge", "getBalance")));
		String catalogFingerprint = ServiceDescriptionFingerprint.fingerprint(catalogEntry);

		assertThat(broker.getAllServiceReferences("Payment", null, speaks(catalogFingerprint))).hasSize(1);
		assertThat(broker.getAllServiceReferences("Payment", null, speaks("sd1:" + "d".repeat(64))))
				.as("'all' waives flavor matching, never contract addressing")
				.isEmpty();
	}

	// ------------------------------------------------------------------
	// The reference keeps advertising its contract
	// ------------------------------------------------------------------

	@Test
	void everyReturnedReferenceCarriesTheCatalogFingerprint() {
		publish(provider("prov-a", "impl-a", serviceInterface("Payment", "charge", "getBalance")));
		String catalogFingerprint = ServiceDescriptionFingerprint.fingerprint(catalogEntry);

		ServiceReference reference = broker.getServiceReferences("Payment", null, null).get(0);
		List<String> fingerprintValues = reference.getProperties().stream()
				.filter(prop -> prop.getName() != null && prop.getName().startsWith("ddsr.fingerprint"))
				.map(prop -> ((StringProperty) prop).getValue())
				.toList();

		assertThat(fingerprintValues)
				.as("the reference advertises the broker-computed catalog contract")
				.containsExactly(catalogFingerprint);
	}

	@Test
	void everyReturnedReferenceCarriesTheImplementationFingerprint() {
		ServiceProvider prov = provider("prov-a", "impl-a", serviceInterface("Payment", "charge", "getBalance"));
		publish(prov);

		ServiceReference reference = broker.getServiceReferences("Payment", null, null).get(0);
		String advertised = reference.getProperties().stream()
				.filter(prop -> "ddsr.impl.fingerprint".equals(prop.getName()))
				.map(prop -> ((StringProperty) prop).getValue())
				.findFirst().orElse(null);

		// the broker computes im1 AFTER rewiring the impl onto the live
		// catalog entry, so the value composes over the CATALOG sd1 — the
		// registry-held impl is exactly that rewired one
		ServiceImplementation held = broker.getImplementationForReference(reference);
		assertThat(advertised)
				.as("the reference advertises the broker-computed im1 (reconnect anchor)")
				.isEqualTo(ServiceImplementationFingerprint.fingerprint(held))
				.startsWith("im1:");
	}
}
