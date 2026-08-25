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

package org.eclipse.fennec.services.fingerprint;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringProperty;
import org.junit.jupiter.api.Test;

/**
 * The {@code sd1} scheme. The golden test against
 * {@code itest/fixtures/fingerprint/} is the cross-language parity
 * anchor: the TS implementation must produce byte-identical canonical
 * text and the same hash for the same fixture — a drift on either side
 * fails one of the two suites.
 */
class ServiceDescriptionFingerprintTest {

	// ------------------------------------------------------------------
	// Golden fixture — shared with the TS test suite
	// ------------------------------------------------------------------

	private static Path fixtureDir() {
		Path dir = Path.of("").toAbsolutePath();
		for (int up = 0; up < 4 && dir != null; up++, dir = dir.getParent()) {
			Path candidate = dir.resolve("itest/fixtures/fingerprint");
			if (Files.isDirectory(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("itest/fixtures/fingerprint not found above " + Path.of("").toAbsolutePath());
	}

	private static String fixture(String name) throws Exception {
		String content = Files.readString(fixtureDir().resolve(name));
		// Editors and git may add a trailing newline; the canonical form
		// has none by definition.
		return content.endsWith("\n") ? content.substring(0, content.length() - 1) : content;
	}

	private static ServiceInterface loadFixtureInterface() throws Exception {
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		rs.getPackageRegistry().put(ServicesPackage.eNS_URI, ServicesPackage.eINSTANCE);
		Resource resource = rs.getResource(
				URI.createFileURI(fixtureDir().resolve("payment.xmi").toString()), true);
		return (ServiceInterface) resource.getContents().get(0);
	}

	@Test
	void goldenCanonicalForm() throws Exception {
		ServiceInterface payment = loadFixtureInterface();

		assertThat(ServiceDescriptionFingerprint.canonicalForm(payment))
				.as("canonical text is frozen with the sd1 tag — a diff here is either "
						+ "a broken implementation or requires a new scheme tag")
				.isEqualTo(fixture("payment.canonical.txt"));
	}

	@Test
	void goldenFingerprint() throws Exception {
		ServiceInterface payment = loadFixtureInterface();

		assertThat(ServiceDescriptionFingerprint.fingerprint(payment))
				.isEqualTo(fixture("payment.sd1"));
	}

	// ------------------------------------------------------------------
	// Scheme rules
	// ------------------------------------------------------------------

	private static ServiceInterface minimal(String name) {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName(name);
		si.setVersion("1.0.0");
		return si;
	}

	private static ServiceOperation operation(String name, String returnType) {
		ServiceOperation op = ServicesFactory.eINSTANCE.createServiceOperation();
		op.setName(name);
		op.setReturnType(returnType);
		return op;
	}

	@Test
	void nullInterfaceHasNoFingerprint() {
		assertThat(ServiceDescriptionFingerprint.fingerprint(null)).isNull();
		assertThat(ServiceDescriptionFingerprint.canonicalForm(null)).isNull();
	}

	@Test
	void valueFormatIsTagColonSixtyFourHex() {
		String fingerprint = ServiceDescriptionFingerprint.fingerprint(minimal("A"));

		assertThat(fingerprint).matches("sd1:[0-9a-f]{64}");
	}

	@Test
	void descriptionsDoNotMoveTheHash() {
		ServiceInterface plain = minimal("Payment");
		ServiceInterface documented = minimal("Payment");
		documented.setDescription("doc text is not contract");

		assertThat(ServiceDescriptionFingerprint.fingerprint(documented))
				.isEqualTo(ServiceDescriptionFingerprint.fingerprint(plain));
	}

	@Test
	void deprecationMetadataDoesNotMoveTheHashButStatusDoes() {
		ServiceInterface plain = minimal("Payment");

		ServiceInterface withReason = minimal("Payment");
		withReason.setDeprecationReason("superseded");
		assertThat(ServiceDescriptionFingerprint.fingerprint(withReason))
				.as("deprecationReason is catalog state, not contract")
				.isEqualTo(ServiceDescriptionFingerprint.fingerprint(plain));

		ServiceInterface deprecated = minimal("Payment");
		deprecated.setStatus(CatalogStatus.DEPRECATED);
		assertThat(ServiceDescriptionFingerprint.fingerprint(deprecated))
				.as("status is part of the canonical form")
				.isNotEqualTo(ServiceDescriptionFingerprint.fingerprint(plain));
	}

	@Test
	void operationOrderIsContract() {
		ServiceInterface ab = minimal("Payment");
		ab.getOperations().add(operation("a", "void"));
		ab.getOperations().add(operation("b", "void"));

		ServiceInterface ba = minimal("Payment");
		ba.getOperations().add(operation("b", "void"));
		ba.getOperations().add(operation("a", "void"));

		assertThat(ServiceDescriptionFingerprint.fingerprint(ab))
				.as("positional cross-refs (//@operations.N) make declared order meaningful")
				.isNotEqualTo(ServiceDescriptionFingerprint.fingerprint(ba));
	}

	@Test
	void exceptionOrderIsNot() {
		ServiceInterface first = minimal("Payment");
		first.getExceptions().add(exception("A"));
		first.getExceptions().add(exception("B"));

		ServiceInterface second = minimal("Payment");
		second.getExceptions().add(exception("B"));
		second.getExceptions().add(exception("A"));

		assertThat(ServiceDescriptionFingerprint.fingerprint(first))
				.isEqualTo(ServiceDescriptionFingerprint.fingerprint(second));
	}

	private static ServiceException exception(String name) {
		ServiceException ex = ServicesFactory.eINSTANCE.createServiceException();
		ex.setName(name);
		ex.setType("com.example." + name);
		return ex;
	}

	@Test
	void exceptionPropertiesSortByName() {
		ServiceException ex1 = exception("Boom");
		StringProperty z = ServicesFactory.eINSTANCE.createStringProperty();
		z.setName("z");
		z.setValue("1");
		StringProperty a = ServicesFactory.eINSTANCE.createStringProperty();
		a.setName("a");
		a.setValue("2");
		ex1.getProperties().add(z);
		ex1.getProperties().add(a);
		ServiceInterface first = minimal("Payment");
		first.getExceptions().add(ex1);

		ServiceException ex2 = exception("Boom");
		StringProperty a2 = ServicesFactory.eINSTANCE.createStringProperty();
		a2.setName("a");
		a2.setValue("2");
		StringProperty z2 = ServicesFactory.eINSTANCE.createStringProperty();
		z2.setName("z");
		z2.setValue("1");
		ex2.getProperties().add(a2);
		ex2.getProperties().add(z2);
		ServiceInterface second = minimal("Payment");
		second.getExceptions().add(ex2);

		assertThat(ServiceDescriptionFingerprint.fingerprint(first))
				.isEqualTo(ServiceDescriptionFingerprint.fingerprint(second));
	}

	@Test
	void parameterDetailsAreContract() {
		ServiceInterface base = minimal("Payment");
		ServiceOperation op = operation("charge", "double");
		Parameter p = ServicesFactory.eINSTANCE.createParameter();
		p.setName("amount");
		p.setType("double");
		p.setIndex(0);
		op.getParameters().add(p);
		base.getOperations().add(op);
		String baseFingerprint = ServiceDescriptionFingerprint.fingerprint(base);

		p.setOptional(true);

		assertThat(ServiceDescriptionFingerprint.fingerprint(base)).isNotEqualTo(baseFingerprint);
	}

	@Test
	void pipesAndBackslashesInNamesCannotForgeLines() {
		ServiceInterface tricky = minimal("Pay|ment\\");
		ServiceInterface other = minimal("Pay");

		String canonical = ServiceDescriptionFingerprint.canonicalForm(tricky);

		assertThat(canonical).startsWith("I|Pay\\|ment\\\\|version=");
		assertThat(ServiceDescriptionFingerprint.fingerprint(tricky))
				.isNotEqualTo(ServiceDescriptionFingerprint.fingerprint(other));
	}

	@Test
	void emptyStringListStaysDistinguishableFromSingleEmptyElement() {
		ServiceInterface withEmptyList = minimal("Payment");
		ServiceException exEmpty = exception("Boom");
		StringListProperty emptyList = ServicesFactory.eINSTANCE.createStringListProperty();
		emptyList.setName("tags");
		exEmpty.getProperties().add(emptyList);
		withEmptyList.getExceptions().add(exEmpty);

		ServiceInterface withEmptyElement = minimal("Payment");
		ServiceException exElement = exception("Boom");
		StringListProperty singleEmpty = ServicesFactory.eINSTANCE.createStringListProperty();
		singleEmpty.setName("tags");
		singleEmpty.getValue().add("");
		exElement.getProperties().add(singleEmpty);
		withEmptyElement.getExceptions().add(exElement);

		assertThat(ServiceDescriptionFingerprint.fingerprint(withEmptyList))
				.as("the count prefix keeps 0 elements apart from 1 empty element")
				.isNotEqualTo(ServiceDescriptionFingerprint.fingerprint(withEmptyElement));
	}
}
