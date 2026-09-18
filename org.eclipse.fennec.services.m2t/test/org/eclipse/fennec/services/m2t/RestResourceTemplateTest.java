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
				.contains("return Response.ok(service.get(id)).build();")
				.contains("return Response.ok(service.list(offset, limit)).build();");
	}

	@Test
	void anArgumentIsReadFromWhereTheFlavorSaysItTravels() throws Exception {
		String source = resource();

		assertThat(source)
				.as("the PATH binding of the contract's id parameter")
				.contains("public Response get(@PathParam(\"id\") String id)");
		assertThat(source)
				.as("a wireName renames the argument on the wire, not in the contract")
				.contains("@QueryParam(\"max\") int limit");
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
	void anOperationWithoutDeclaredErrorsNeedsNoTryBlock() throws Exception {
		String list = resource().substring(resource().indexOf("public Response list("));

		assertThat(list)
				.as("list declares no exceptions, so its body is the delegation alone")
				.doesNotContain("try {");
	}
}
