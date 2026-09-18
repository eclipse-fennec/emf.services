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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
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
		try {
			return Response.ok(service.get(id)).build();
		} catch (PersonNotFoundException failure) {
			return Response.status(404).entity(failure.getMessage()).build();
		}
	}

	@GET
	@Path("/persons")
	public Response list(@QueryParam("offset") int offset, @QueryParam("max") int limit) {
		return Response.ok(service.list(offset, limit)).build();
	}
}