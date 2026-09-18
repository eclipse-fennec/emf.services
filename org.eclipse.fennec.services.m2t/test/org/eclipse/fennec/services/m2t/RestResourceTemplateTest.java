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
 * What {@code templates/rest-resource.mtl} produced during this build: the
 * provider-side endpoint of a RestFlavor. Every assertion starts from "the
 * file exists" — a template the generator cannot use writes nothing, reports
 * nothing, and leaves the build green.
 */
class RestResourceTemplateTest {

	private static final Path GENERATED = Path.of("src-gen-rest", "org", "eclipse", "fennec",
			"services", "m2t", "example", "PersonDirectoryRestResource.java");

	private static String resource() throws IOException {
		assertThat(GENERATED)
				.as("the generator wrote the resource — an empty src-gen-rest means the template was not used")
				.exists();
		return Files.readString(GENERATED, StandardCharsets.UTF_8);
	}

	@Test
	void theResourceIsAWhiteboardComponentOnTheFlavorsBasePath() throws Exception {
		assertThat(resource())
				.contains("package org.eclipse.fennec.services.m2t.example;")
				.contains("@JakartarsResource")
				.contains("@JakartarsName(\"directory-rest\")")
				.contains("@Path(\"/directory\")")
				.contains("public class PersonDirectoryRestResource {")
				.doesNotContain("OclInvalid");
	}

	@Test
	void everyMethodDelegatesToTheGeneratedInterface() throws Exception {
		// Generated is transport, never logic: the implementation stays
		// hand-written and the endpoint cannot drift from the contract.
		assertThat(resource())
				.contains("@Reference\n\tprivate PersonDirectory service;")
				.contains("Person result = service.get(id);")
				.contains("List<Person> result = service.list(offset, limit);");
	}

	@Test
	void anAbsentOrEmptyResultAnswers204() throws Exception {
		// Convention, not something anybody should have to model.
		assertThat(resource())
				.contains("return result == null\n\t\t\t\t\t? Response.noContent().build()")
				.contains("return result == null || result.isEmpty()\n\t\t\t\t\t? Response.noContent().build()");
	}

	@Test
	void anUnexpectedFailureAnswers500() throws Exception {
		String source = resource();

		assertThat(source)
				.as("every method, whether the contract declares errors or not")
				.contains("} catch (RuntimeException failure) {\n"
						+ "\t\t\treturn Response.serverError().entity(failure.getMessage()).build();");
		assertThat(source.split("catch \\(RuntimeException").length - 1)
				.as("one catch-all per operation")
				.isEqualTo(2);
	}

	@Test
	void aMissingRequiredArgumentAnswers400() throws Exception {
		assertThat(resource())
				.contains("if (id == null) {\n"
						+ "\t\t\treturn Response.status(400).entity(\"id is required\").build();");
	}

	@Test
	void aViolatedConstraintAnswers400() throws Exception {
		String source = resource();

		assertThat(source)
				.as("StringPatternConstraint on the id")
				.contains("if (!id.matches(\"[A-Za-z0-9-]+\")) {")
				.contains("if (id.length() < 3) {");
		assertThat(source)
				.as("NumericRangeConstraint, written the way the argument's type reads")
				.contains("if (offset < 0) {")
				.contains("if (limit < 1) {")
				.contains("if (limit > 200) {");
		assertThat(source)
				.as("offset declares no upper bound, so nothing checks one")
				.doesNotContain("if (offset > ");
	}

	@Test
	void anOptionalArgumentCarriesTheContractsDefault() throws Exception {
		assertThat(resource())
				.contains("@QueryParam(\"offset\") @DefaultValue(\"0\") int offset")
				.contains("@QueryParam(\"max\") @DefaultValue(\"50\") int limit");
	}

	@Test
	void anArgumentIsReadFromWhereTheFlavorSaysItTravels() throws Exception {
		String source = resource();

		assertThat(source)
				.as("the PATH binding of the contract's id parameter")
				.contains("public Response get(@PathParam(\"id\") String id)");
		assertThat(source)
				.as("a wireName renames the argument on the wire, not in the contract")
				.contains("@QueryParam(\"max\") @DefaultValue(\"50\") int limit");
		assertThat(source)
				.as("only the annotations the flavor actually uses are imported")
				.contains("import jakarta.ws.rs.PathParam;")
				.contains("import jakarta.ws.rs.QueryParam;")
				.doesNotContain("import jakarta.ws.rs.HeaderParam;");
	}

	@Test
	void theHttpMethodAndPathComeFromTheOperationFlavor() throws Exception {
		assertThat(resource())
				.contains("@GET\n\t@Path(\"/persons/{id}\")")
				.contains("@GET\n\t@Path(\"/persons\")");
	}

	@Test
	void aDeclaredErrorIsAnsweredWithTheStatusTheFlavorBinds() throws Exception {
		// Before #73 this mapping was prose in a description — "HTTP 409,
		// code 202" — and no generator could build error handling from it.
		assertThat(resource())
				.contains("} catch (PersonNotFoundException failure) {")
				.contains("return Response.status(404).entity(failure.getMessage()).build();");
	}

	@Test
	void anOperationWithoutDeclaredErrorsStillGuardsAgainstFailure() throws Exception {
		String list = resource().substring(resource().indexOf("public Response list("));

		assertThat(list)
				.as("list declares no exceptions of its own, but a failure is still a 500")
				.doesNotContain("catch (PersonNotFoundException")
				.contains("catch (RuntimeException failure)");
	}
}
