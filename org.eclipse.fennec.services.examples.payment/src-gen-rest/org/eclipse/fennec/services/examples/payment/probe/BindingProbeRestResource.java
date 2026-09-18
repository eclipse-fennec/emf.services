/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.fennec.services.examples.payment.probe;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;

/**
 * REST endpoint of the {@code BindingProbe} contract, as the
 * {@code binding-probe-rest} flavor of implementation {@code binding-probe-java}
 * declares it.
 *
 * <p>Generated from the flavor — do not edit. Transport only: every method
 * delegates to the service and holds no state of its own.
 */
@JakartarsResource
@JakartarsName("binding-probe-rest")
@Component(service = BindingProbeRestResource.class, scope = ServiceScope.PROTOTYPE)
@Path("/probe")
public class BindingProbeRestResource {

	@Reference
	private BindingProbe service;

	@GET
	@Path("/echo/{id}")
	@Produces("text/plain")
	public Response echo(@PathParam("id") String id, @QueryParam("currency") String currency, @HeaderParam("X-Tenant") String tenant) {
		if (id == null) {
			return Response.status(400).entity("id is required").build();
		}
		if (currency == null) {
			return Response.status(400).entity("currency is required").build();
		}
		if (tenant == null) {
			return Response.status(400).entity("tenant is required").build();
		}
		try {
			String result = service.echo(id, currency, tenant);
			return result == null
					? Response.noContent().build()
					: Response.ok(result).build();
		} catch (RuntimeException failure) {
			return Response.serverError().entity(failure.getMessage()).build();
		}
	}
}