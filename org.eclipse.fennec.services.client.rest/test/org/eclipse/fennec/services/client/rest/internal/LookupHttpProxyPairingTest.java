/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.client.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.fingerprint.ServiceImplementationFingerprint;
import org.junit.jupiter.api.Test;

/** Reference → implementation pairing in a lookup envelope: by im1 when a provider carries several. */
class LookupHttpProxyPairingTest {

	private static ServiceImplementation implementation(String version, String host, ServiceInterface si) {
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName("payments-rest");
		impl.setVersion(version);
		impl.setImplementationId("org.example.Impl");
		impl.getServiceInterfaces().add(si);
		RestFlavor rest = ServicesFactory.eINSTANCE.createRestFlavor();
		rest.setName("rest");
		rest.setHost(host);
		rest.setBasePath("/payments");
		impl.getFlavors().add(rest);
		return impl;
	}

	private static ServiceReference reference(ServiceProvider provider, String im1) {
		ServiceReference ref = ServicesFactory.eINSTANCE.createServiceReference();
		ref.setId("ref");
		ref.setProvider(provider);
		if (im1 != null) {
			StringProperty p = ServicesFactory.eINSTANCE.createStringProperty();
			p.setName("ddsr.impl.fingerprint");
			p.setValue(im1);
			ref.getProperties().add(p);
		}
		return ref;
	}

	@Test
	void twoVersionsUnderOneProviderArePairedByTheIm1Decoration() {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName("Payment");
		si.setVersion("1.0.0");
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("payments");
		ServiceImplementation v1 = implementation("1.0.0", "http://a:1", si);
		ServiceImplementation v2 = implementation("2.0.0", "http://b:1", si);
		provider.getImplementations().add(v1);
		provider.getImplementations().add(v2);
		LookupHttpProxy proxy = new LookupHttpProxy();

		assertThat(proxy.getImplementationForReference(
				reference(provider, ServiceImplementationFingerprint.fingerprint(v2)))).isSameAs(v2);
		assertThat(proxy.getImplementationForReference(
				reference(provider, ServiceImplementationFingerprint.fingerprint(v1)))).isSameAs(v1);
		assertThat(proxy.getImplementationForReference(reference(provider, null)))
				.as("no key: first one (legacy envelopes)").isSameAs(v1);
	}

	@Test
	void aSingleImplementationNeedsNoKey() {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName("Payment");
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName("payments");
		ServiceImplementation only = implementation("1.0.0", "http://a:1", si);
		provider.getImplementations().add(only);

		assertThat(new LookupHttpProxy().getImplementationForReference(reference(provider, "im1:nonsense"))).isSameAs(only);
		assertThat(new LookupHttpProxy().getImplementationForReference(null)).isNull();
	}
}
