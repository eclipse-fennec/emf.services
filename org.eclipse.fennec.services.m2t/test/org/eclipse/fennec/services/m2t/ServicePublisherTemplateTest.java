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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the publisher template writes.
 *
 * <p>Read from what the generator actually produced, like the other two
 * template tests, and for the same reason: a template this generator
 * cannot use writes no file and says nothing, so the net has to be a
 * test that looks for the file.
 *
 * <p>Asserted on content rather than against a byte-pinned golden file.
 * A golden file for a template churns on every whitespace change, while
 * what protects here is that the output is checked in and compiles — a
 * broken signature is a red build, not a diff nobody reads.
 */
class ServicePublisherTemplateTest {

	private static String publisher() throws Exception {
		Path file = Path.of("src-gen-publisher/org/eclipse/fennec/services/m2t/example",
				"DirectoryJavaRestPublisher.java");
		assertThat(file)
			.as("the generator wrote the publisher — an empty src-gen-publisher means "
					+ "the template was not used")
			.exists();
		return Files.readString(file);
	}

	@Test
	@DisplayName("the class is named after the implementation, in Java's shape rather than the wire's")
	void namedAfterTheImplementation() throws Exception {
		assertThat(publisher())
			.contains("public class DirectoryJavaRestPublisher {")
			.as("the wire name is kebab case; a class name is not")
			.doesNotContain("directory-java-restPublisher");
	}

	@Test
	@DisplayName("it announces on activation and withdraws before anything else stops")
	void theFrP3Order() throws Exception {
		String source = publisher();
		int publish = source.indexOf("client.provider().publish(");
		int withdraw = source.indexOf("registration.withdraw()");
		assertThat(publish).isPositive();
		assertThat(withdraw)
			.as("the withdrawal is in the file, and after the publish — FR-P3 is the point of this template")
			.isGreaterThan(publish);
		assertThat(source)
			.contains("@Deactivate")
			.contains("Diagnostic withdrawn = registration.withdraw();");
	}

	@Test
	@DisplayName("the contract is read from the document, never restated in code")
	void theContractStaysInTheDocument() throws Exception {
		String source = publisher();
		assertThat(source)
			.as("one statement of the contract, one fingerprint")
			.doesNotContain("createServiceInterface()")
			.doesNotContain("createServiceOperation()");
		assertThat(source).contains("ServiceProvider provider = (ServiceProvider) document.getContents().get(0);");
		assertThat(source).contains("\"directory-java-rest\".equals(candidate.getName())");
	}

	@Test
	@DisplayName("what a deployment states is deployment: where it runs, and what it supersedes")
	void theConfigurationSurface() throws Exception {
		String source = publisher();
		assertThat(source)
			.contains("String public_url() default \"http://localhost:9095\"")
			.as("the modelled host is the default, so a document that names a reachable one is runnable as it is")
			.contains("String replaces_version() default \"\"")
			.contains("String update_policy() default \"UNSPECIFIED\"");
		assertThat(source)
			.as("the document's location is required rather than guessed")
			.contains("String model_bundle() default \"\"")
			.contains("ConfigurationPolicy.REQUIRE");
	}

	@Test
	@DisplayName("the announcement references the catalog entry instead of copying the contract")
	void theContractTravelsAsAReference() throws Exception {
		assertThat(publisher())
			.contains("/catalog/")
			.contains("createResource(org.eclipse.emf.common.util.URI.createURI(entryUrl))");
	}
}
