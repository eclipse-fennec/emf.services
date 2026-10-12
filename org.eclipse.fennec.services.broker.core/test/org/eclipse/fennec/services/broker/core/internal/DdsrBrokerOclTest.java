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
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclEngineImpl;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclSettingDelegateFactory;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclValidationDelegateFactory;
import org.eclipse.fennec.m2x.ocl.parser.OclParserSupport;
import org.eclipse.fennec.services.BearerTokenScheme;
import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.ConnectionScheme;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.SecurityRequirement;
import org.eclipse.fennec.services.SecuritySchemeKind;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
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
		impl.setImplementationId("org.example.impl." + providerName + "-impl");
		impl.getServiceInterfaces().add(serviceInterface("Payment", "charge"));
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName(providerName + "-impl");
		flavor.setBasePath("/payments");
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		assertThat(broker.publishImplementation(provider, impl).getSeverity().getValue())
				.isLessThan(DiagnosticSeverity.ERROR_VALUE);
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
		impl.setImplementationId("org.example.impl." + "empty");

		Diagnostic diagnostic = Diagnostician.INSTANCE.validate(impl);

		assertThat(diagnostic.getSeverity()).isEqualTo(Diagnostic.ERROR);
		assertThat(diagnostic.toString()).contains("atLeastOneInterface");
	}

	// ------------------------------------------------------------------
	// Parameter invariants (#41)

	@Test
	void aParameterMustSayWhatItCarries() {
		Parameter untyped = ServicesFactory.eINSTANCE.createParameter();
		untyped.setName("amount");

		assertThat(Diagnostician.INSTANCE.validate(untyped).toString()).contains("typeOrEType");

		untyped.setEType(ServicesPackage.eINSTANCE.getParameter());
		assertThat(Diagnostician.INSTANCE.validate(untyped).getSeverity())
				.as("a metamodel type alone is a complete statement")
				.isEqualTo(Diagnostic.OK);
	}

	@Test
	void boundsMustNotBeInverted() {
		Parameter p = ServicesFactory.eINSTANCE.createParameter();
		p.setName("ids");
		p.setType("string");
		p.setLowerBound(3);
		p.setUpperBound(2);

		assertThat(Diagnostician.INSTANCE.validate(p).toString()).contains("boundsOrdered");

		p.setUpperBound(-1);
		assertThat(Diagnostician.INSTANCE.validate(p).getSeverity())
				.as("-1 is unbounded, not a smaller upper bound")
				.isEqualTo(Diagnostic.OK);
	}

	@Test
	void onlyTheGenuineContradictionBetweenOptionalAndLowerBoundIsRejected() {
		Parameter required = ServicesFactory.eINSTANCE.createParameter();
		required.setName("amount");
		required.setType("double");
		required.setLowerBound(0);

		assertThat(Diagnostician.INSTANCE.validate(required).toString())
				.as("not optional but a minimum of zero values is a contradiction")
				.contains("requiredSlotHasLowerBound");

		// The shape every contract written before #41 has: optional, and
		// lowerBound left at the model default. It must stay valid — for
		// a single value 'optional' is the authoritative statement.
		Parameter legacy = ServicesFactory.eINSTANCE.createParameter();
		legacy.setName("currency");
		legacy.setType("string");
		legacy.setOptional(true);

		assertThat(Diagnostician.INSTANCE.validate(legacy).getSeverity())
				.as("a contract that never heard of bounds stays valid")
				.isEqualTo(Diagnostic.OK);
	}

	// ------------------------------------------------------------------
	// Security invariants: the WHAT on the contract, the HOW on the flavor
	// ------------------------------------------------------------------

	private static SecurityRequirement requirement(boolean authenticated, String... scopes) {
		SecurityRequirement r = ServicesFactory.eINSTANCE.createSecurityRequirement();
		r.setAuthenticated(authenticated);
		r.getScopes().addAll(List.of(scopes));
		return r;
	}

	private static BearerTokenScheme bearer() {
		BearerTokenScheme scheme = ServicesFactory.eINSTANCE.createBearerTokenScheme();
		scheme.setName("keycloak");
		scheme.setKind(SecuritySchemeKind.BEARER_TOKEN);
		scheme.setIssuer("https://idp.example/realms/dim");
		return scheme;
	}

	private static ConnectionScheme connection() {
		ConnectionScheme scheme = ServicesFactory.eINSTANCE.createConnectionScheme();
		scheme.setName("mosquitto-acl");
		scheme.setKind(SecuritySchemeKind.CONNECTION);
		return scheme;
	}

	/** An implementation of the given contract with one REST flavor and no scheme. */
	private static ServiceImplementation implementationOf(ServiceInterface contract) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("prov");
		provider.setVersion("1.0.0");
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName("prov-impl");
		impl.setVersion("1.0.0");
		impl.setImplementationId("org.example.impl." + "prov-impl");
		impl.getServiceInterfaces().add(contract);
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName("prov-impl");
		flavor.setBasePath("/secure");
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		return impl;
	}

	@Test
	void scopesWithoutAuthenticationAreAContradiction() {
		SecurityRequirement r = requirement(false, "payment:write");

		assertThat(Diagnostician.INSTANCE.validate(r).toString()).contains("scopesImplyAuthenticated");

		r.setAuthenticated(true);
		assertThat(Diagnostician.INSTANCE.validate(r).getSeverity())
				.as("scopes presuppose a principal to carry them")
				.isEqualTo(Diagnostic.OK);
	}

	@Test
	void anAuthenticatedContractNeedsASchemeOnEveryFlavor() {
		ServiceInterface contract = serviceInterface("Secure", "charge");
		contract.setSecurity(requirement(true));
		ServiceImplementation impl = implementationOf(contract);

		assertThat(Diagnostician.INSTANCE.validate(impl).toString())
				.as("a flavor that offers no way to prove a principal cannot serve this contract")
				.contains("authenticatedContractsHaveAScheme");

		impl.getFlavors().get(0).getSecuritySchemes().add(bearer());
		assertThat(Diagnostician.INSTANCE.validate(impl).getSeverity())
				.as("the bearer scheme is one way to prove it")
				.isEqualTo(Diagnostic.OK);
	}

	@Test
	void anOperationMayWaiveTheInterfaceRequirement() {
		// The interface demands authentication, its only operation waives
		// it: the effective requirement is the operation's, so a flavor
		// without a scheme is fine.
		ServiceInterface contract = serviceInterface("Secure", "ping");
		contract.setSecurity(requirement(true));
		contract.getOperations().get(0).setSecurity(requirement(false));

		assertThat(Diagnostician.INSTANCE.validate(implementationOf(contract)).getSeverity())
				.isEqualTo(Diagnostic.OK);
	}

	@Test
	void scopesCannotBeCheckedOnAConnectionScheme() {
		ServiceInterface contract = serviceInterface("Secure", "charge");
		contract.getOperations().get(0).setSecurity(requirement(true, "payment:write"));
		ServiceImplementation impl = implementationOf(contract);
		impl.getFlavors().get(0).getSecuritySchemes().add(connection());

		assertThat(Diagnostician.INSTANCE.validate(impl).toString())
				.as("identity once per connection says nothing about a scope per call")
				.contains("scopesNeedAPerCallScheme");

		impl.getFlavors().get(0).getSecuritySchemes().add(bearer());
		assertThat(Diagnostician.INSTANCE.validate(impl).getSeverity())
				.as("one per-call scheme beside the connection one suffices")
				.isEqualTo(Diagnostic.OK);
	}

	// ------------------------------------------------------------------
	// The gate: the broker refuses what the invariants refuse
	// ------------------------------------------------------------------

	@Test
	void theBrokerRefusesAnImplementationThatCannotServeItsContract() {
		ServiceInterface contract = serviceInterface("Secure", "charge");
		contract.setSecurity(requirement(true));
		assertThat(broker.addCatalogEntry(contract, "test").getSeverity().getValue())
				.isLessThan(DiagnosticSeverity.ERROR_VALUE);

		// The publisher sends the contract along, as over the wire — the
		// same contract, requirement included, since the requirement is
		// part of sd1. The broker resolves it to the live entry and only
		// then asks the invariants.
		ServiceInterface sent = serviceInterface("Secure", "charge");
		sent.setSecurity(requirement(true));
		ServiceImplementation impl = implementationOf(sent);
		var refusal = broker.publishImplementation(
				(ServiceProvider) impl.eContainer(), impl);

		assertThat(refusal.getSeverity()).isEqualTo(DiagnosticSeverity.ERROR);
		assertThat(refusal.getCode()).isEqualTo(DdsrDiagnostics.CODE_IMPL_INVALID);
		assertThat(refusal.getMessage()).contains("authenticatedContractsHaveAScheme");
		assertThat(broker.getServiceReferences("Secure", null, null))
				.as("a refused publish leaves nothing behind")
				.isEmpty();

		impl.getFlavors().get(0).getSecuritySchemes().add(bearer());
		assertThat(broker.publishImplementation((ServiceProvider) impl.eContainer(), impl).getSeverity().getValue())
				.as("with a scheme the same document is taken")
				.isLessThan(DiagnosticSeverity.ERROR_VALUE);
		assertThat(broker.getServiceReferences("Secure", null, null)).hasSize(1);
	}

	@Test
	void aParameterTypedByAModelTheBrokerDoesNotHaveIsNoDefect() {
		// PersonStore's parameters are typed by the examples model. The
		// broker carries documents and does not interpret payloads (#100),
		// so on the broker that eType is a proxy — by design, and no
		// reason to refuse the contract.
		ServiceInterface contract = serviceInterface("PersonStore", "store");
		EClass person = EcoreFactory.eINSTANCE.createEClass();
		((InternalEObject) person).eSetProxyURI(URI.createURI("http://geckoprojects.org/ddsr/example#//Person"));
		Parameter parameter = ServicesFactory.eINSTANCE.createParameter();
		parameter.setName("person");
		parameter.setEType(person);
		contract.getOperations().get(0).getParameters().add(parameter);

		assertThat(broker.addCatalogEntry(contract, "test").getSeverity().getValue())
				.isLessThan(DiagnosticSeverity.ERROR_VALUE);
		assertThat(broker.getRegistry().getCatalog()).extracting(ServiceInterface::getName).contains("PersonStore");
	}

	@Test
	void theBrokerRefusesAContractThatContradictsItself() {
		ServiceInterface contract = serviceInterface("Broken", "charge");
		contract.setSecurity(requirement(false, "payment:write"));

		var refusal = broker.addCatalogEntry(contract, "test");

		assertThat(refusal.getCode()).isEqualTo(DdsrDiagnostics.CODE_CATALOG_ENTRY_INVALID);
		assertThat(refusal.getMessage()).contains("scopesImplyAuthenticated");
		assertThat(broker.getRegistry().getCatalog()).extracting(ServiceInterface::getName)
				.doesNotContain("Broken");
	}
}
