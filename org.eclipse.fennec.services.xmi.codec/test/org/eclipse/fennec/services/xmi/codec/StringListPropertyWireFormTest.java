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

package org.eclipse.fennec.services.xmi.codec;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.StringListProperty;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

/**
 * Pins the wire form of a multi-valued attribute across the language
 * boundary. {@link StringListProperty#getValue()} is the only
 * many-valued data type attribute on the wire, and EMF has two forms
 * for it: the compact, space-separated attribute, and one element per
 * entry. The compact form is only reversible while no entry carries
 * whitespace and none is empty — {@code ["a b", "c"]} would otherwise
 * read back as three entries — so EMF falls back to the element form
 * for exactly those, in {@code XMLSaveImpl.saveDataTypeMany()}.
 * <p>
 * The two sides do not pick the same form: Java writes an element per
 * entry throughout, while the TypeScript client keeps the compact form
 * for entries that survive it and falls back to elements for the rest
 * (emf.ts 0.2.0). That asymmetry is harmless as long as each side reads
 * both forms, which is what these tests pin - together with their
 * counterpart in
 * {@code ddsr-ts-client/packages/ddsr-client/test/properties.test.ts}.
 */
class StringListPropertyWireFormTest {

	private static final String NS = "http://eclipse.org/fennec/services/1.0";

	/** Plain ResourceSet with the services package and the XMI factory. */
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
			// nothing to release
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

	private static ServiceImplementation implementationWithTags(String... tags) {
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName("impl");
		StringListProperty property = ServicesFactory.eINSTANCE.createStringListProperty();
		property.setName("tags");
		for (String tag : tags) {
			property.getValue().add(tag);
		}
		impl.getProperties().add(property);
		return impl;
	}

	private String write(ServiceImplementation impl) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmiCodec.write(out, rsObjects, impl);
		return out.toString(StandardCharsets.UTF_8);
	}

	private StringListProperty readTags(String xml) throws IOException {
		EObject root = XmiCodec.read(utf8(xml), rsObjects);
		assertThat(root).isInstanceOf(ServiceImplementation.class);
		return (StringListProperty) ((ServiceImplementation) root).getProperties().get(0);
	}

	@Test
	void everyEntryIsWrittenAsItsOwnElement() throws IOException {
		String xml = write(implementationWithTags("payments", "demo"));

		assertThat(xml)
				.as("Java EMF does not use the compact form for EString lists, "
						+ "not even for entries that would survive it")
				.contains("<value>payments</value>")
				.contains("<value>demo</value>")
				.doesNotContain("value=\"payments demo\"");
		assertThat(readTags(xml).getValue()).containsExactly("payments", "demo");
	}

	@Test
	void anEntryCarryingWhitespaceIsWrittenAsElements() throws IOException {
		String xml = write(implementationWithTags("a b", "c"));

		assertThat(xml)
				.as("the compact form would read back as three entries")
				.doesNotContain("value=\"a b c\"");
		assertThat(readTags(xml).getValue()).containsExactly("a b", "c");
	}

	@Test
	void anEmptyEntryStaysAnEntry() throws IOException {
		String xml = write(implementationWithTags("", "c"));

		assertThat(readTags(xml).getValue()).containsExactly("", "c");
	}

	@Test
	void readsTheElementFormWrittenByTheTypeScriptClient() throws IOException {
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<services:ServiceImplementation xmlns:services="%s" xmlns:xmi="http://www.omg.org/XMI"
				                                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				                                xmi:version="2.0" name="impl">
				  <properties xsi:type="services:StringListProperty" name="tags">
				    <value>a b</value>
				    <value>c</value>
				  </properties>
				</services:ServiceImplementation>
				""".formatted(NS);

		assertThat(readTags(xml).getValue()).containsExactly("a b", "c");
	}

	@Test
	void readsTheAttributeFormWrittenByTheTypeScriptClient() throws IOException {
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<services:ServiceImplementation xmlns:services="%s" xmlns:xmi="http://www.omg.org/XMI"
				                                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				                                xmi:version="2.0" name="impl">
				  <properties xsi:type="services:StringListProperty" name="tags" value="payments demo"/>
				</services:ServiceImplementation>
				""".formatted(NS);

		assertThat(readTags(xml).getValue()).containsExactly("payments", "demo");
	}
}
