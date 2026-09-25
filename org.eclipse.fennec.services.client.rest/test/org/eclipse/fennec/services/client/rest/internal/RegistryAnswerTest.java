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

package org.eclipse.fennec.services.client.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

/**
 * How the client reads {@code GET /registry} since it is one document
 * with several roots (#174): the registry, then the providers it names.
 * The reader for a single root refuses that, so the proxy reads a bundle.
 */
class RegistryAnswerTest {

	private static final class ResourceSets implements ComponentServiceObjects<ResourceSet> {

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
	}

	private final ResourceSets rsObjects = new ResourceSets();

	/** What the broker sends: the registry first, then its provider. */
	private XmiBundle brokerAnswer() throws IOException {
		ServicesFactory factory = ServicesFactory.eINSTANCE;
		ServiceInterface payment = factory.createServiceInterface();
		payment.setName("Payment");
		payment.setVersion("1.0.0");
		ServiceImplementation implementation = factory.createServiceImplementation();
		implementation.setName("payments-impl");
		implementation.getServiceInterfaces().add(payment);
		ServiceProvider provider = factory.createServiceProvider();
		provider.setName("payments");
		provider.getImplementations().add(implementation);
		RemoteServiceRegistry registry = factory.createRemoteServiceRegistry();
		registry.getCatalog().add(payment);
		registry.getProviders().add(provider);
		registry.getImplementations().add(implementation);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, rsObjects, List.of(registry, provider));
		return XmiCodec.readBundle(new ByteArrayInputStream(out.toByteArray()), rsObjects);
	}

	@Test
	@DisplayName("the registry comes first, and what it names is resolved, not left a proxy")
	void theRegistryAndItsProvidersResolve() throws IOException {
		RemoteServiceRegistry registry = CatalogHttpProxy.registryOf(brokerAnswer());

		assertThat(registry.getProviders()).singleElement()
				.satisfies(provider -> assertThat(provider.eIsProxy()).isFalse());
		assertThat(registry.getImplementations()).singleElement()
				.satisfies(implementation -> {
					assertThat(implementation.eIsProxy()).isFalse();
					assertThat(implementation.getServiceInterfaces()).singleElement()
							.as("and the contract points back into the catalog of the same document")
							.isSameAs(registry.getCatalog().get(0));
				});
	}

	@Test
	@DisplayName("an answer that does not start with a registry is refused with a reason")
	void anAnswerWithoutARegistryIsRefused() {
		assertThatThrownBy(() -> CatalogHttpProxy.registryOf(new XmiBundle(ServicesFactory.eINSTANCE.createServiceProvider())))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("does not start with a registry");
		assertThatThrownBy(() -> CatalogHttpProxy.registryOf(new XmiBundle(List.of())))
				.isInstanceOf(IllegalStateException.class);
	}
}
