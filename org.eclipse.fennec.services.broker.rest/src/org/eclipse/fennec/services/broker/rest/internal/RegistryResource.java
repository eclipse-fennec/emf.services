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

package org.eclipse.fennec.services.broker.rest.internal;

import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.annotations.RequireJakartarsWhiteboard;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Read-only snapshot of the entire registry — catalog plus
 * implementations plus providers — as XMI. Useful for diagnostics
 * and to seed a local registry from the broker's current state.
 *
 * <pre>
 * GET /registry
 * </pre>
 */
@RequireHttpWhiteboard
@RequireJakartarsWhiteboard
@JakartarsResource
@JakartarsName("ddsr-registry")
@Component(service = RegistryResource.class, scope = ServiceScope.PROTOTYPE)
@Path("/registry")
public class RegistryResource {

	@Reference
	private BrokerCatalog broker;

	@GET
	@Produces(MediaType.APPLICATION_XML)
	public Response snapshot() {
		return Response.ok(broker.getRegistry()).build();
	}
}
