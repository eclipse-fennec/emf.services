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

package org.eclipse.fennec.services.invocation;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.junit.jupiter.api.Test;

/**
 * The rule of #88, in the two situations that made it necessary.
 *
 * <p>One needs the context and would otherwise lose it; the other is
 * the greedy reading this rule was written to avoid. Both are real
 * broker operations, which is why they are the tests.
 */
class ResultDocumentTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	/** A contract, free-floating, the way a copy made for one answer is. */
	private static ServiceInterface contract(String name) {
		ServiceInterface contract = F.createServiceInterface();
		contract.setName(name);
		contract.setVersion("1.0.0");
		return contract;
	}

	/** The shape `getServiceReferences` answers with: an envelope plus what it names. */
	private static LocalServiceRegistry lookupAnswer(ServiceInterface... contracts) {
		ServiceImplementation implementation = F.createServiceImplementation();
		implementation.setName("payments-java-rest");
		for (ServiceInterface contract : contracts) {
			implementation.getServiceInterfaces().add(contract);
		}
		ServiceProvider provider = F.createServiceProvider();
		provider.setName("payments-java");
		provider.getImplementations().add(implementation);

		LocalServiceRegistry envelope = F.createLocalServiceRegistry();
		envelope.setName("lookup-result");
		envelope.getProviders().add(provider);
		return envelope;
	}

	@Test
	void the_contracts_an_answer_names_travel_with_it() {
		ServiceInterface payment = contract("Payment");

		var roots = ResultDocument.roots(lookupAnswer(payment));

		assertThat(roots)
			.as("a reference to a root that does not travel is a dangling href")
			.hasSize(2);
		assertThat(roots.get(0)).isInstanceOf(LocalServiceRegistry.class);
		assertThat(roots.get(1))
			.as("the answer comes first, so a reader that wants the value takes root zero")
			.isSameAs(payment);
	}

	@Test
	void each_contract_travels_once_however_many_name_it() {
		ServiceInterface payment = contract("Payment");
		ServiceImplementation second = F.createServiceImplementation();
		second.setName("payments-ts-rest");
		second.getServiceInterfaces().add(payment);
		LocalServiceRegistry envelope = lookupAnswer(payment);
		((ServiceProvider) envelope.getProviders().get(0)).getImplementations().add(second);

		assertThat(ResultDocument.roots(envelope)).hasSize(2);
	}

	@Test
	void a_live_catalog_entry_does_not_drag_the_registry_onto_the_wire() {
		// getCatalogEntry answers with one contract whose replacedBy
		// points at another — and that one is contained in the broker's
		// registry, which is in the broker's snapshot resource.
		RemoteServiceRegistry registry = F.createRemoteServiceRegistry();
		ServiceInterface asked = contract("Payment");
		ServiceInterface successor = contract("Payment2");
		asked.setStatus(CatalogStatus.DEPRECATED);
		asked.setReplacedBy(successor);
		registry.getCatalog().add(asked);
		registry.getCatalog().add(successor);

		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		resourceSet.getPackageRegistry().put(ServicesPackage.eNS_URI, ServicesPackage.eINSTANCE);
		Resource snapshot = resourceSet.createResource(URI.createURI("broker-state.xmi"));
		snapshot.getContents().add(registry);

		assertThat(ResultDocument.roots(asked))
			.as("a request for one contract must not answer with the whole registry")
			.containsExactly(asked);
		assertThat(ResultDocument.needsSiblings(asked)).isFalse();
	}

	@Test
	void context_that_needs_context_travels_too() {
		ServiceInterface payment = contract("Payment");
		ServiceInterface successor = contract("Payment2");
		// Both are copies made for this answer, so the link between them
		// is one the reader can follow — and therefore one that has to
		// arrive.
		payment.setReplacedBy(successor);

		assertThat(ResultDocument.roots(lookupAnswer(payment)))
			.as("the rule is transitive, or the second hop dangles instead of the first")
			.hasSize(3)
			.contains(payment, successor);
	}

	@Test
	void a_proxy_is_a_statement_that_the_target_lives_elsewhere() {
		ServiceInterface elsewhere = contract("Payment");
		((InternalEObject) elsewhere)
				.eSetProxyURI(URI.createURI("http://broker:8887/ddsr/rest/catalog/Payment"));

		assertThat(ResultDocument.roots(lookupAnswer(elsewhere)))
			.as("the publish convention is built on exactly this href — following it would fetch")
			.hasSize(1);
	}

	@Test
	void an_answer_that_needs_nothing_stays_one_root() {
		ServiceInterface plain = contract("Payment");

		assertThat(ResultDocument.roots(plain)).containsExactly(plain);
		assertThat(ResultDocument.needsSiblings(plain))
			.as("the common case has to stay byte for byte what it was")
			.isFalse();
		assertThat(ResultDocument.roots(null)).isEmpty();
	}
}
