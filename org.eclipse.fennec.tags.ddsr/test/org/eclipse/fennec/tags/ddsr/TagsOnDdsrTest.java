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


package org.eclipse.fennec.tags.ddsr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.InputStream;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclEngineImpl;
import org.eclipse.fennec.m2x.ocl.engine.internal.OclSettingDelegateFactory;
import org.eclipse.fennec.m2x.ocl.parser.OclParserSupport;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.invocation.ServiceMethods;
import org.eclipse.fennec.tags.EffectiveTag;
import org.eclipse.fennec.tags.MatchMode;
import org.eclipse.fennec.tags.TagOrigin;
import org.eclipse.fennec.tags.TagsPackage;
import org.eclipse.fennec.tags.service.TagStore;
import org.eclipse.fennec.tags.service.TagSubjects.Subject;
import org.eclipse.fennec.tags.service.core.TagServiceImpl;
import org.eclipse.fennec.tags.TagRegistry;
import org.eclipse.fennec.tags.TagsFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The tags on the services registry (#275): the registry's contracts and
 * implementations as targets, and the Tags contract over the service —
 * including that every operation the document declares finds the method
 * the generic distribution will call.
 */
class TagsOnDdsrTest {

	@BeforeAll
	static void registerOcl() {
		EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.put(
				"http://www.eclipse.org/fennec/m2x/ocl/1.0",
				new OclSettingDelegateFactory(new OclEngineImpl(new OclParserSupport())));
		((EStructuralFeature.Internal) TagsPackage.Literals.TAG__PATH).setSettingDelegate(null);
	}

	/** A catalog holding a registry the test builds. */
	private static final class FakeCatalog implements BrokerCatalog {
		final RemoteServiceRegistry registry = ServicesFactory.eINSTANCE.createRemoteServiceRegistry();

		@Override
		public Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor) {
			throw new UnsupportedOperationException();
		}

		@Override
		public RemoteServiceRegistry getRegistry() {
			return registry;
		}

		@Override
		public Diagnostic snapshot() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class MemoryStore implements TagStore {
		@Override
		public TagRegistry load() {
			return TagsFactory.eINSTANCE.createTagRegistry();
		}

		@Override
		public void save(TagRegistry registry) {
		}
	}

	private final FakeCatalog catalog = new FakeCatalog();
	private TagsEndpoint endpoint;

	@BeforeEach
	void setUp() {
		ServiceInterface payment = contract("Payment");
		payment.getTags().add("payment/terms/instant");
		catalog.registry.getCatalog().add(payment);
		catalog.registry.getCatalog().add(contract("InstallmentPlan"));

		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("payments-card");
		ServiceImplementation card = ServicesFactory.eINSTANCE.createServiceImplementation();
		card.setName("card-rest");
		card.getServiceInterfaces().add(payment);
		card.getServiceInterfaces().add(catalog.registry.getCatalog().get(1));
		card.getTags().add("payment/method/card");
		StringProperty scheme = ServicesFactory.eINSTANCE.createStringProperty();
		scheme.setName("payment.scheme");
		scheme.setValue("card");
		card.getProperties().add(scheme);
		provider.getImplementations().add(card);
		catalog.registry.getProviders().add(provider);
		catalog.registry.getImplementations().add(card);

		TagServiceImpl service = new TagServiceImpl(new MemoryStore());
		service.addSubjects(new DdsrTagSubjects(catalog));
		service.putTag(null, "payment", null, null, false);
		service.putTag("payment", "method", null, null, false);
		service.putTag("payment/method", "card", null, null, true);
		service.putTag("payment", "terms", null, null, false);
		service.putTag("payment/terms", "instant", null, null, true);
		service.putTag("payment/terms", "installment", null, null, false);
		endpoint = new TagsEndpoint(service);
	}

	@Test
	void contractsAreKeyedByNameAndImplementationsByProviderAndContract() {
		DdsrTagSubjects subjects = new DdsrTagSubjects(catalog);

		assertThat(subjects.subjects(DdsrTagSubjects.CONTRACT)).extracting(Subject::key)
				.containsExactly("Payment", "InstallmentPlan");
		assertThat(subjects.subjects(DdsrTagSubjects.IMPLEMENTATION)).extracting(Subject::key)
				.containsExactly("payments-card/Payment", "payments-card/InstallmentPlan");
		assertThat(subjects.subjects(DdsrTagSubjects.IMPLEMENTATION).iterator().next().properties())
				.containsEntry("payment.scheme", "card")
				.containsEntry("ddsr.provider.name", "payments-card");
	}

	@Test
	void whatTheRegistryAsksForIsCarriedWhereTheTagAllowsIt() {
		assertThat(endpoint.tagsOf("contract", "Payment").getTags())
				.extracting(EffectiveTag::getPath, EffectiveTag::getOrigin)
				.containsExactly(tuple("payment/terms/instant", TagOrigin.SOURCE));
		assertThat(endpoint.find(List.of("payment/method"), MatchMode.ANY, "implementation").getKeys())
				.containsExactly("payments-card/InstallmentPlan", "payments-card/Payment");
	}

	@Test
	void anAdministratorTagsAContractTheRegistryHolds() {
		endpoint.assign("payment/terms/installment", "contract", "InstallmentPlan");

		assertThat(endpoint.find(List.of("payment/terms"), MatchMode.ANY, "contract").getKeys())
				.containsExactly("InstallmentPlan", "Payment");
		assertThat(endpoint.find(List.of("payment/terms/installment"), null, "contract").getKeys())
				.as("no mode means ANY").containsExactly("InstallmentPlan");
	}

	@Test
	void theTreeTravelsAsOneRegistryWithoutAssignments() {
		endpoint.assign("payment/terms/installment", "contract", "InstallmentPlan");

		TagRegistry tree = endpoint.tags();

		assertThat(tree.getTags()).hasSize(1);
		assertThat(tree.getAssignments()).isEmpty();
	}

	@Test
	void everyOperationOfTheContractFindsItsMethod() throws Exception {
		ServiceInterface contract = contractDocument();

		assertThat(contract.getOperations()).hasSize(11);
		for (ServiceOperation operation : contract.getOperations()) {
			Optional<?> method = ServiceMethods.find(endpoint, operation.getName(), operation.getParameters().size());
			assertThat(method).as(operation.getName()).isPresent();
		}
	}

	@Test
	void theContractsTypesResolveToTheTagModel() throws Exception {
		ServiceInterface contract = contractDocument();
		EcoreUtil.resolveAll(contract);

		ServiceOperation find = contract.getOperations().get(6);
		assertThat(find.getParameters().get(1).getEType()).isSameAs(TagsPackage.Literals.MATCH_MODE);
		assertThat(find.getReturnValue().getEType()).isSameAs(TagsPackage.Literals.FOUND_TARGETS);
	}

	// ------------------------------------------------------------------

	private static ServiceInterface contract(String name) {
		ServiceInterface contract = ServicesFactory.eINSTANCE.createServiceInterface();
		contract.setName(name);
		contract.setVersion("1.0.0");
		return contract;
	}

	private static ServiceInterface contractDocument() throws Exception {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getPackageRegistry().put(ServicesPackage.eNS_URI, ServicesPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(TagsPackage.eNS_URI, TagsPackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		Resource resource = resourceSet.createResource(URI.createURI("tags-api.xmi"));
		// Read where it is authored: the bundle carries it, the test classpath does not.
		try (InputStream in = Files.newInputStream(Path.of("resources", "tags-api.xmi"))) {
			resource.load(in, null);
		}
		return (ServiceInterface) resource.getContents().get(1);
	}

	@Test
	void aRegistrationAsksForTheTagsOfItsImplementationAndContracts() {
		DdsrTagSubjects subjects = new DdsrTagSubjects(catalog);
		List<Subject> heard = new ArrayList<>();
		subjects.onRegistered(heard::addAll);

		subjects.publish(registered(catalog.registry.getImplementations().get(0)));

		assertThat(heard).extracting(Subject::kind, Subject::key, Subject::requestedTags).contains(
				tuple("implementation", "payments-card/Payment", List.of("payment/method/card")),
				tuple("contract", "Payment", List.of("payment/terms/instant")));
	}

	@Test
	void whatARegistrationMayNotGiveItselfBecomesARequest() {
		TagServiceImpl service = new TagServiceImpl(new MemoryStore());
		DdsrTagSubjects subjects = new DdsrTagSubjects(catalog);
		service.addSubjects(subjects);
		service.putTag(null, "payment", null, null, false);
		TagsEndpoint tags = new TagsEndpoint(service);
		assertThat(tags.requestedTags().getRequests()).extracting(r -> r.getPath() + " " + r.getTarget().getKey())
				.as("present when the source arrived: nothing exists yet, everything is asked — once per target")
				.containsExactlyInAnyOrder("payment/method/card payments-card/Payment",
						"payment/method/card payments-card/InstallmentPlan", "payment/terms/instant Payment");

		tags.discardRequestedTag("payment/terms/instant", "contract", "Payment");
		tags.acceptRequestedTag("payment/method/card", "implementation", "payments-card/Payment");
		tags.acceptRequestedTag("payment/method/card", "implementation", "payments-card/InstallmentPlan");
		assertThat(tags.requestedTags().getRequests()).isEmpty();
		assertThat(tags.tagsOf("implementation", "payments-card/Payment").getTags())
				.extracting(EffectiveTag::getPath).contains("payment/method/card");

		subjects.publish(registered(catalog.registry.getImplementations().get(0)));
		assertThat(tags.requestedTags().getRequests()).extracting(r -> r.getPath())
				.as("the discarded one comes back with the next registration").containsExactly("payment/terms/instant");
	}

	private static ServiceEvent registered(ServiceImplementation implementation) {
		ServiceProvider copy = EcoreUtil.copy((ServiceProvider) implementation.eContainer());
		ServiceReference reference = ServicesFactory.eINSTANCE.createServiceReference();
		reference.setId("ref-1");
		reference.setProvider(copy);
		ServiceEvent event = ServicesFactory.eINSTANCE.createServiceEvent();
		event.setType(ServiceEventType.REGISTERED);
		event.setReference(reference);
		return event;
	}
}
