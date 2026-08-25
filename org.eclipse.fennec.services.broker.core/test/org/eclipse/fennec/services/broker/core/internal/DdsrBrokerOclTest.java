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

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.fennec.m2x.ocl.engine.OclEngineImpl;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclSettingDelegateFactory;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclValidationDelegateFactory;
import org.eclipse.fennec.m2x.ocl.parser.OclParserSupport;
import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.util.ServicesValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The OCL activation (issue #7): the model's invariants are armed via
 * EMF validation delegates, and {@code ServiceRegistration.consumerCount}
 * derives from the lease relation through the setting delegate — never
 * stored, so it cannot drift.
 * <p>
 * Plain JUnit with the Fennec OCL engine registered standalone — in the
 * broker launches the same factories arrive as DS services and the
 * emf.osgi delegate-registry whiteboard registers them (no code here).
 * <p>
 * <b>Caching caveat this class pins implicitly:</b> EMF resolves a
 * feature's setting delegate once and caches it on the feature AND per
 * instance — the registration below therefore happens in
 * {@code @BeforeAll} and resets the feature's cache, and instances are
 * created only afterwards. In a launch the delegate factory must be up
 * before the first {@code getConsumerCount()} call.
 */
class DdsrBrokerOclTest {

	@TempDir
	Path tmp;

	private DdsrBrokerImpl broker;

	/** The Fennec OCL delegate URI — the value of the ecore annotations. */
	private static final String DELEGATE_URI = "http://www.eclipse.org/fennec/m2x/ocl/1.0";

	@BeforeAll
	static void registerOclDelegates() {
		// Plain JUnit runs against the buildpath classpath, so the
		// engine-internal factory classes are reachable — in OSGi the
		// same classes arrive as DS services and the emf.osgi whiteboard
		// does this registration.
		OclEngineImpl engine = new OclEngineImpl(new OclParserSupport());
		EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.put(
				DELEGATE_URI, new OclSettingDelegateFactory(engine));
		EValidator.ValidationDelegate.Registry.INSTANCE.put(
				DELEGATE_URI, new OclValidationDelegateFactory(engine));
		EValidator.Registry.INSTANCE.put(ServicesPackage.eINSTANCE, ServicesValidator.INSTANCE);
		// Another test in this JVM may already have touched the feature —
		// EMF would then have cached a "no factory found" delegate. Reset
		// so the freshly registered factory is picked up.
		((EStructuralFeature.Internal) ServicesPackage.Literals.SERVICE_REGISTRATION__CONSUMER_COUNT)
				.setSettingDelegate(null);
	}

	@BeforeEach
	void setUp() {
		broker = new DdsrBrokerImpl(tmp.resolve("broker-state.xmi"), new InMemoryLookupBackend());
		broker.addCatalogEntry(serviceInterface("Payment", "charge"), "test");
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

	private ServiceReference publishAndRef(String providerName) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(providerName);
		provider.setVersion("1.0.0");
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(providerName + "-impl");
		impl.setVersion("1.0.0");
		impl.getServiceInterfaces().add(serviceInterface("Payment", "charge"));
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName(providerName + "-impl");
		flavor.setBasePath("/payments");
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		assertThat(broker.publishImplementation(provider, impl).getSeverity().getValue())
				.isLessThan(org.eclipse.fennec.services.DiagnosticSeverity.ERROR_VALUE);
		return broker.getServiceReferences("Payment", null, null).get(0);
	}

	private static ConsumerSession session(String consumerId) {
		ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
		session.setConsumerId(consumerId);
		return session;
	}

	// ------------------------------------------------------------------
	// consumerCount — the derived usage count (ACQUISITION §9)
	// ------------------------------------------------------------------

	@Test
	void consumerCountDerivesFromTheLeases() {
		ServiceReference ref = publishAndRef("prov-a");

		assertThat(ref.getRegistration().getConsumerCount())
				.as("no session yet")
				.isZero();

		broker.putSession(session("consumer-1"), List.of(ref.getId()));
		broker.putSession(session("consumer-2"), List.of(ref.getId()));
		assertThat(ref.getRegistration().getConsumerCount())
				.as("two leases held")
				.isEqualTo(2);

		broker.deleteSession("consumer-1");
		assertThat(ref.getRegistration().getConsumerCount())
				.as("release is visible immediately — derived, never stored")
				.isEqualTo(1);
	}

	// ------------------------------------------------------------------
	// Armed invariants (validation delegates)
	// ------------------------------------------------------------------

	@Test
	void validSemverFlagsAGarbageVersion() {
		ServiceInterface si = serviceInterface("Payment", "charge");
		si.setVersion("definitely-not-semver");

		Diagnostic diagnostic = Diagnostician.INSTANCE.validate(si);

		assertThat(diagnostic.getSeverity()).isEqualTo(Diagnostic.ERROR);
		assertThat(diagnostic.toString()).contains("validSemver");
	}

	@Test
	void replacedByRequiresDeprecatedStatus() {
		ServiceInterface old = serviceInterface("Payment", "charge");
		ServiceInterface successor = serviceInterface("Payment2", "charge");
		old.setReplacedBy(successor);
		// status still ACTIVE — the invariant must fire
		assertThat(Diagnostician.INSTANCE.validate(old).getSeverity())
				.isEqualTo(Diagnostic.ERROR);

		old.setStatus(CatalogStatus.DEPRECATED);
		assertThat(Diagnostician.INSTANCE.validate(old).getSeverity())
				.as("deprecated + replacedBy is the intended shape")
				.isEqualTo(Diagnostic.OK);
	}

	@Test
	void aHealthyRegistryValidatesClean() {
		publishAndRef("prov-a");

		Diagnostic diagnostic = Diagnostician.INSTANCE.validate(broker.getRegistry());

		assertThat(diagnostic.getSeverity())
				.as("published registry state satisfies the armed invariants: %s", diagnostic)
				.isEqualTo(Diagnostic.OK);
	}

	@Test
	void anImplementationWithoutInterfacesIsInvalid() {
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName("empty");
		impl.setVersion("1.0.0");

		Diagnostic diagnostic = Diagnostician.INSTANCE.validate(impl);

		assertThat(diagnostic.getSeverity()).isEqualTo(Diagnostic.ERROR);
		assertThat(diagnostic.toString()).contains("atLeastOneInterface");
	}
}
