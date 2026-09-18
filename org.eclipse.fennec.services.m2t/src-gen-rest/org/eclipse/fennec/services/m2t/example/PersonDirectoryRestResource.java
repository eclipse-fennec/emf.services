/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.fennec.services.m2t.example;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import org.eclipse.fennec.services.examples.model.ddsrexample.Person;
import jakarta.ws.rs.core.Response;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;

/**
 * REST endpoint of the {@code PersonDirectory} contract, as the
 * {@code directory-rest} flavor of implementation {@code directory-java-rest}
 * declares it.
 *
 * <p>Generated from the flavor — do not edit. Transport only: every method
 * delegates to the service and holds no state of its own.
 */
@JakartarsResource
@JakartarsName("directory-rest")
@Component(service = PersonDirectoryRestResource.class, scope = ServiceScope.PROTOTYPE)
@Path("/directory")
public class PersonDirectoryRestResource {

	@Reference
	private PersonDirectory service;

	@GET
	@Path("/persons/{id}")
	public Response get(@PathParam("id") String id) {
		if (id == null) {
			return Response.status(400).entity("id is required").build();
		}
		if (!id.matches("[A-Za-z0-9-]+")) {
			return Response.status(400).entity("id does not match [A-Za-z0-9-]+").build();
		}
		if (id.length() < 3) {
			return Response.status(400).entity("id is shorter than 3").build();
		}
		try {
			Person result = service.get(id);
			return result == null
					? Response.noContent().build()
					: Response.ok(result).build();
		} catch (PersonNotFoundException failure) {
			return Response.status(404).entity(failure.getMessage()).build();
		} catch (RuntimeException failure) {
			return Response.serverError().entity(failure.getMessage()).build();
		}
	}

	@GET
	@Path("/persons")
	public Response list(@QueryParam("offset") @DefaultValue("0") int offset, @QueryParam("max") @DefaultValue("50") int limit) {
		if (offset < 0) {
			return Response.status(400).entity("offset is below 0").build();
		}
		if (limit < 1) {
			return Response.status(400).entity("limit is below 1").build();
		}
		if (limit > 200) {
			return Response.status(400).entity("limit is above 200").build();
		}
		try {
			List<Person> result = service.list(offset, limit);
			return result == null || result.isEmpty()
					? Response.noContent().build()
					: Response.ok(result).build();
		} catch (RuntimeException failure) {
			return Response.serverError().entity(failure.getMessage()).build();
		}
	}
}