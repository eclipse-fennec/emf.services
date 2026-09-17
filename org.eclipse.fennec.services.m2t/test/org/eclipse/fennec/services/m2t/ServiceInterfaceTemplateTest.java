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

package org.eclipse.fennec.services.m2t;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * What {@code templates/service-interface.mtl} produced during this build.
 * <p>
 * The generator's quietest failure mode is silence: a template it cannot use
 * writes no file, reports no diagnostic, and the build stays green — bnd empties
 * the output directory before every run, so the previous result is gone too. Every
 * assertion here therefore starts from "the file exists and says something".
 */
class ServiceInterfaceTemplateTest {

	private static final Path GENERATED = Path.of("src-gen", "org", "eclipse", "fennec",
			"services", "m2t", "example");

	private static String generated(String name) throws IOException {
		Path file = GENERATED.resolve(name);
		assertThat(file)
				.as("the generator wrote %s — an empty src-gen means the template was not used at all", name)
				.exists();
		return Files.readString(file, StandardCharsets.UTF_8);
	}

	@Test
	void everyContractOfTheBindingBecomesAnInterface() throws Exception {
		assertThat(generated("Payment.java")).contains("public interface Payment {");
		assertThat(generated("PersonDirectory.java")).contains("public interface PersonDirectory {");
	}

	@Test
	void noSlotRendersAsAnEvaluationError() throws Exception {
		// An OCL expression that goes wrong renders its result as text instead
		// of failing the build — 'OclInvalid' in the output is the symptom.
		for (String name : new String[] { "Payment.java", "PersonDirectory.java", "package-info.java",
				"PersonNotFoundException.java" }) {
			assertThat(generated(name))
					.as("%s holds no unevaluated expression", name)
					.doesNotContain("OclInvalid");
		}
	}

	@Test
	void aLanguageNeutralTypeBecomesItsJavaCounterpart() throws Exception {
		assertThat(generated("Payment.java"))
				.contains("double charge(double amount, String currency);")
				.contains("double getBalance(String accountId);");
	}

	@Test
	void anEClassTypedSlotIsImportedThroughThePackageMapping() throws Exception {
		String source = generated("PersonDirectory.java");

		assertThat(source)
				.as("the binding maps the metamodel's nsURI to a Java package")
				.contains("import org.eclipse.fennec.services.examples.model.ddsrexample.Person;");
		assertThat(source).contains("Person get(String id)");
	}

	@Test
	void aMultiValuedSlotBecomesAList() throws Exception {
		String source = generated("PersonDirectory.java");

		assertThat(source)
				.as("upperBound = -1 is what separates list() from get()")
				.contains("List<Person> list(int offset, int limit);")
				.contains("import java.util.List;");
	}

	@Test
	void declaredExceptionsReachTheSignatureAndTheJavadoc() throws Exception {
		String source = generated("PersonDirectory.java");

		assertThat(source).contains("Person get(String id) throws PersonNotFoundException;");
		assertThat(source).contains("@throws PersonNotFoundException No person is registered");
		assertThat(source)
				.as("a type of the target package needs no import")
				.doesNotContain("import org.eclipse.fennec.services.m2t.example.PersonNotFoundException;");
	}

	@Test
	void descriptionsBecomeJavadocAndOptionalityIsSpelledOut() throws Exception {
		assertThat(generated("Payment.java"))
				.contains(" * Charge an amount. Returns the remaining balance.")
				.contains("@param currency ISO 4217 currency code. Optional, defaults to {@code EUR}.");
	}

	@Test
	void theApiTypeOfTheBindingDecidesTheAnnotation() throws Exception {
		assertThat(generated("Payment.java"))
				.contains("import org.osgi.annotation.versioning.ProviderType;")
				.contains("@ProviderType");
	}

	@Test
	void aDeclaredExceptionIsGeneratedWhereTheBindingSaysSo() throws Exception {
		// The contract names the exception symbolically
		// (fennec.services.example.PersonNotFound); the Java class behind it and
		// the decision to write it at all are the binding's, not the contract's.
		String source = generated("PersonNotFoundException.java");

		assertThat(source)
				.contains("package org.eclipse.fennec.services.m2t.example;")
				.contains("public class PersonNotFoundException extends Exception {")
				.contains(" * No person is registered under the given identifier.");
	}

	@Test
	void theDeclaredPayloadBecomesTypedState() throws Exception {
		String source = generated("PersonNotFoundException.java");

		assertThat(source)
				.as("a payload field is what lets a consumer read structured data instead of parsing a message")
				.contains("private final String personId;")
				.contains("public PersonNotFoundException(String message, String personId) {")
				.contains("public String getPersonId() {");
	}

	@Test
	void theContractNamesNoJavaType() throws Exception {
		// The symbolic token never reaches the generated code — if it did, the
		// contract would be carrying Java, and the TypeScript side could not
		// render the same exception from the same document.
		assertThat(generated("PersonDirectory.java")).doesNotContain("fennec.services.example.PersonNotFound");
		assertThat(generated("PersonNotFoundException.java")).doesNotContain("fennec.services.example.PersonNotFound");
	}

	@Test
	void everyGeneratedFileCarriesTheBindingsFileHeader() throws Exception {
		// The licence text is language-neutral and lives in the binding; the
		// comment syntax around it belongs to the template, so the same header
		// serves a Java, a TypeScript and a Python generator.
		for (String name : new String[] { "Payment.java", "PersonDirectory.java",
				"PersonNotFoundException.java", "package-info.java" }) {
			assertThat(generated(name))
					.as("%s opens with the header the binding declares", name)
					.startsWith("/*\n * Copyright (c) 2026 Contributors to the Eclipse Foundation.\n")
					.contains(" * SPDX-License-Identifier: EPL-2.0\n */\n");
		}
	}

	@Test
	void anEmptyHeaderLineCarriesNoTrailingSpace() throws Exception {
		assertThat(generated("Payment.java"))
				.as("a blank line in a comment is ' *', not ' * '")
				.contains("\n *\n")
				.doesNotContain(" * \n");
	}

	@Test
	void thePackageIsExportedAtTheVersionTheContractsAgreeOn() throws Exception {
		assertThat(generated("package-info.java"))
				.contains("@org.osgi.annotation.bundle.Export")
				.contains("@org.osgi.annotation.versioning.Version(\"1.0.0\")")
				.contains("package org.eclipse.fennec.services.m2t.example;");
	}
}
