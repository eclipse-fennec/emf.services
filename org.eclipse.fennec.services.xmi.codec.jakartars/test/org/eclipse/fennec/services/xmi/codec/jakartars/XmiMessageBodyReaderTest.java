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

package org.eclipse.fennec.services.xmi.codec.jakartars;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.xmi.codec.WireBody;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import jakarta.ws.rs.WebApplicationException;

/** The codec's refusals, as the HTTP statuses a JAX-RS provider answers with (moved here with #265). */
class XmiMessageBodyReaderTest {

	private static final String NS = "http://eclipse.org/fennec/services/1.0";

	private static final class ResourceSetObjects implements ComponentServiceObjects<ResourceSet> {

		@Override
		public ResourceSet getService() {
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
			rs.getPackageRegistry().put(NS, ServicesPackage.eINSTANCE);
			return rs;
		}

		@Override
		public void ungetService(ResourceSet service) {
			// nothing to release in a test
		}

		@Override
		public ServiceReference<ResourceSet> getServiceReference() {
			throw new UnsupportedOperationException("not needed for these tests");
		}
	}

	private final ResourceSetObjects rsObjects = new ResourceSetObjects();

	private static InputStream utf8(String xml) {
		return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
	}

	private static String paddedProvider(int targetBytes) {
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<services:ServiceProvider xmlns:services=\"").append(NS).append("\"\n")
				.append("    xmlns:xmi=\"http://www.omg.org/XMI\"\n")
				.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
				.append("    xmi:version=\"2.0\" name=\"payments-java\" version=\"1.0.0\">\n")
				.append("  <implementations name=\"payments-java-rest\" version=\"1.0.0\">\n")
				.append("    <flavors xsi:type=\"services:RestFlavor\" name=\"rest\"")
				.append(" host=\"http://h\" basePath=\"/p\">\n");
		String filler = "x".repeat(1000);
		while (xml.length() < targetBytes) {
			xml.append("      <contentTypes>").append(filler).append("</contentTypes>\n");
		}
		xml.append("    </flavors>\n  </implementations>\n</services:ServiceProvider>\n");
		return xml.toString();
	}

	// ------------------------------------------------------------------
	// The JAX-RS providers translate, the codec does not
	// ------------------------------------------------------------------

	@Test
	void theProviderTurnsAnOversizedBodyInto413() {
		XmiMessageBodyReader reader = new XmiMessageBodyReader(rsObjects);
		// Valid XML, just too much of it. A buffer of zero bytes would be
		// rejected as malformed at the first byte and would prove nothing
		// about the size limit.
		String oversized = paddedProvider(WireBody.MAX_BYTES + 1);

		assertThatThrownBy(() -> reader.readFrom(null, null, null, null, null, utf8(oversized)))
				.as("a status code belongs to the transport, and this is the transport")
				.isInstanceOf(WebApplicationException.class)
				.extracting(e -> ((WebApplicationException) e).getResponse().getStatus())
				.isEqualTo(413);
	}

	@Test
	void theProviderTurnsAMalformedBodyInto400() {
		XmiMessageBodyReader reader = new XmiMessageBodyReader(rsObjects);

		assertThatThrownBy(() -> reader.readFrom(null, null, null, null, null, utf8("<not-xml")))
				.isInstanceOf(WebApplicationException.class)
				.extracting(e -> ((WebApplicationException) e).getResponse().getStatus())
				.isEqualTo(400);
	}
}
