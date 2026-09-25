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

package org.eclipse.fennec.services.broker.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

/**
 * What {@code GET /registry} sends (#174).
 *
 * <p>The registry names its implementations and providers without
 * containing them. It used to be written alone, and those references
 * went out as hrefs into {@code services:registry} — a document nobody
 * was sent, which every reader left as unresolved proxies.
 */
class RegistryResourceTest {

	private final RestTestSupport.ResourceSets rsObjects = new RestTestSupport.ResourceSets();

	/** What BrokerState.getRegistry hands out: a copy, its providers in no resource. */
	private static RemoteServiceRegistry registryWithOneProvider() {
		ServiceInterface payment = RestTestSupport.payment();
		ServiceProvider provider = RestTestSupport.provider("payments", payment, FlavorKind.REST);
		RemoteServiceRegistry registry = ServicesFactory.eINSTANCE.createRemoteServiceRegistry();
		registry.setName("ddsr-broker");
		registry.getCatalog().add(payment);
		registry.getProviders().add(provider);
		registry.getImplementations().add(provider.getImplementations().get(0));
		return registry;
	}

	private String answer() throws IOException {
		RegistryResource resource = new RegistryResource();
		resource.broker = mock(BrokerCatalog.class);
		when(resource.broker.getRegistry()).thenReturn(registryWithOneProvider());

		Response response = resource.snapshot();

		assertThat(response.getEntity()).isInstanceOf(XmiBundle.class);
		return RestTestSupport.xml(rsObjects, (XmiBundle) response.getEntity());
	}

	@Test
	@DisplayName("the snapshot is one document: the registry, then its providers, referenced from within")
	void providersTravelWithTheRegistry() throws IOException {
		String xml = answer();

		assertThat(xml).doesNotContain("services:registry").doesNotContain("href=");
		assertThat(xml).contains("<services:ServiceProvider");
	}

	@Test
	@DisplayName("a reader resolves the registry's implementations and providers, nothing is left a proxy")
	void aReaderResolvesEverything() throws IOException {
		// As a bundle, the way the client reads it: a single-root read
		// refuses a document with siblings, and rightly so.
		RemoteServiceRegistry read = (RemoteServiceRegistry) XmiCodec.readBundle(RestTestSupport.bytes(answer()),
				rsObjects).roots().get(0);

		assertThat(read.getProviders()).singleElement()
				.satisfies(provider -> {
					assertThat(provider.eIsProxy()).isFalse();
					assertThat(provider.getName()).isEqualTo("payments");
				});
		assertThat(read.getImplementations()).singleElement()
				.satisfies(implementation -> {
					assertThat(implementation.eIsProxy()).isFalse();
					assertThat(implementation.eContainer()).isSameAs(read.getProviders().get(0));
				});
	}
}
