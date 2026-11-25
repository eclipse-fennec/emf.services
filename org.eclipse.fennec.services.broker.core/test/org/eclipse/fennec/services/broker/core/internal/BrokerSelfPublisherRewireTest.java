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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.RestParameterBinding;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

/**
 * A broker restarted from a snapshot holds a contract whose implementation
 * it withdrew on the way down (#271). Announcing it again must point the
 * whole provider at the contract in the catalog — the flavor's bindings
 * included — or every later snapshot fails on objects in no resource.
 */
class BrokerSelfPublisherRewireTest {

	/** The broker's API documents live in broker.rest, which serves them. */
	private static final Path DOCUMENTS = Path.of("..", "org.eclipse.fennec.services.broker.rest", "resources");

	private final ComponentServiceObjects<ResourceSet> rsObjects = new ComponentServiceObjects<>() {
		@Override
		public ResourceSet getService() {
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
			rs.getPackageRegistry().put(ServicesPackage.eNS_URI, ServicesPackage.eINSTANCE);
			return rs;
		}

		@Override
		public void ungetService(ResourceSet service) {
		}

		@Override
		public ServiceReference<ResourceSet> getServiceReference() {
			throw new UnsupportedOperationException("not needed");
		}
	};

	private List<EObject> roots() throws IOException {
		try (InputStream in = Files.newInputStream(DOCUMENTS.resolve("broker-catalog-api.xmi"))) {
			return XmiCodec.readBundle(in, rsObjects).roots();
		}
	}

	@Test
	void everyReferenceOfTheProviderEndsInTheLiveContract() throws IOException {
		ServiceInterface live = (ServiceInterface) roots().get(1);
		List<EObject> read = roots();
		ServiceProvider provider = (ServiceProvider) read.get(0);
		ServiceInterface contract = (ServiceInterface) read.get(1);

		assertThat(BrokerSelfPublisher.rewire(provider, contract, live)).isTrue();

		ServiceImplementation implementation = provider.getImplementations().get(0);
		assertThat(implementation.getServiceInterfaces()).containsExactly(live);
		RestFlavor flavor = (RestFlavor) implementation.getFlavors().get(0);
		assertThat(flavor.getOperationFlavors()).isNotEmpty();
		for (Object each : flavor.getOperationFlavors()) {
			RestOperationFlavor operationFlavor = (RestOperationFlavor) each;
			assertThat(EcoreUtil.isAncestor(live, operationFlavor.getOperation()))
					.as("operation of %s", operationFlavor.getName()).isTrue();
			for (RestParameterBinding binding : operationFlavor.getParameterBindings()) {
				assertThat(EcoreUtil.isAncestor(live, binding.getParameter()))
						.as("a parameter bound by %s", operationFlavor.getName()).isTrue();
			}
		}
		for (EObject object : (Iterable<EObject>) () -> provider.eAllContents()) {
			for (EObject target : object.eCrossReferences()) {
				assertThat(EcoreUtil.isAncestor(contract, target))
						.as("%s still points into the contract just read", object.eClass().getName()).isFalse();
			}
		}
	}

	@Test
	void anotherContractUnderTheSameNameIsLeftAlone() throws IOException {
		ServiceInterface live = (ServiceInterface) roots().get(1);
		live.getOperations().get(0).setName("renamed");
		List<EObject> read = roots();
		ServiceProvider provider = (ServiceProvider) read.get(0);
		ServiceInterface contract = (ServiceInterface) read.get(1);

		assertThat(BrokerSelfPublisher.rewire(provider, contract, live)).isFalse();

		assertThat(provider.getImplementations().get(0).getServiceInterfaces()).containsExactly(contract);
	}
}
