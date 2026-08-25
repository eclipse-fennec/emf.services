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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.broker.core.ContractAddressing;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceInterface;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.annotations.RequireJakartarsWhiteboard;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST surface for catalog mutation and read:
 * <ul>
 * <li>{@code GET    /catalog}                  — list catalog entries (XMI snapshot of registry.catalog)</li>
 * <li>{@code POST   /catalog}                  — addCatalogEntry; body = ServiceInterface XMI</li>
 * <li>{@code PUT    /catalog/{name}/deprecate} — deprecateCatalogEntry; body = optional ServiceInterface with deprecationReason / replacedBy</li>
 * <li>{@code DELETE /catalog/{name}}           — removeCatalogEntry (strict-reject while implementations exist)</li>
 * </ul>
 *
 * <p>Caller passes {@code X-DDSR-Requestor} as identity for governance
 * audit. Missing header defaults to {@code "anonymous"}.
 */
@RequireHttpWhiteboard
@RequireJakartarsWhiteboard
@JakartarsResource
@JakartarsName("ddsr-catalog")
@Component(service = CatalogResource.class, scope = ServiceScope.PROTOTYPE)
@Path("/catalog")
public class CatalogResource {

	@Reference
	private BrokerCatalog broker;

	@GET
	@Produces(MediaType.APPLICATION_XML)
	public Response list() {
		// Serialize the registry directly — clients get the catalog as part
		// of the registry document. Keeps the wire format simple.
		return Response.ok(broker.getRegistry()).build();
	}

	/**
	 * Canonical per-entry URL. Lets remote publishers reference a
	 * catalog SI by URL (cross-document href) instead of shipping the
	 * SI as a sibling root in the publish body.
	 * <p>
	 * With the {@code (name, sd1)} catalog key (ACQUISITION.md §11.2)
	 * several contracts may share a name: the optional
	 * {@code ?fingerprint=sd1:…} query addresses one exactly; a bare
	 * name is only served while it is unambiguous (409 otherwise, listing
	 * the coexisting fingerprints).
	 */
	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getByName(@PathParam("name") String name,
			@QueryParam("fingerprint") String fingerprint) {
		List<ServiceInterface> matches = entriesNamed(name);
		if (fingerprint != null && !fingerprint.isBlank()) {
			for (ServiceInterface si : matches) {
				if (ContractAddressing.matches(si, fingerprint)) {
					return Response.ok(si).build();
				}
			}
			return Response.status(404)
					.entity("no catalog entry named '" + name + "' with fingerprint " + fingerprint).build();
		}
		if (matches.isEmpty()) {
			return Response.status(404)
					.entity("no catalog entry named '" + name + "'").build();
		}
		if (matches.size() > 1) {
			return Response.status(409)
					.entity("interface name '" + name + "' names " + matches.size()
							+ " coexisting contracts — address one via ?fingerprint=; available: "
							+ matches.stream().map(ContractAddressing::fingerprint)
									.collect(Collectors.joining(", ")))
					.build();
		}
		return Response.ok(matches.get(0)).build();
	}

	private List<ServiceInterface> entriesNamed(String name) {
		List<ServiceInterface> matches = new ArrayList<>();
		for (ServiceInterface si : broker.getRegistry().getCatalog()) {
			if (name.equals(si.getName())) {
				matches.add(si);
			}
		}
		return matches;
	}

	/**
	 * Resolve the governance target: with a fingerprint the exact entry
	 * (as a full copy, so the broker's content addressing hits it even
	 * when several contracts share the name); without one a name-only
	 * stub — the broker then applies its own unambiguity rule.
	 */
	private ServiceInterface governanceTarget(String name, String fingerprint) {
		if (fingerprint != null && !fingerprint.isBlank()) {
			for (ServiceInterface si : entriesNamed(name)) {
				if (ContractAddressing.matches(si, fingerprint)) {
					return si;
				}
			}
			return null;
		}
		ServiceInterface stub = ServicesFactory.eINSTANCE.createServiceInterface();
		stub.setName(name);
		return stub;
	}

	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public Response addEntry(ServiceInterface si,
			@HeaderParam("X-DDSR-Requestor") @DefaultValue("anonymous") String requestor) {
		Diagnostic d = broker.addCatalogEntry(si, requestor);
		return HttpDiagnostics.toResponse(d);
	}

	@PUT
	@Path("/{name}/deprecate")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public Response deprecate(@PathParam("name") String name,
			@QueryParam("fingerprint") String fingerprint, ServiceInterface si,
			@HeaderParam("X-DDSR-Requestor") @DefaultValue("anonymous") String requestor) {
		ServiceInterface target = governanceTarget(name, fingerprint);
		if (target == null) {
			return Response.status(404)
					.entity("no catalog entry named '" + name + "' with fingerprint " + fingerprint).build();
		}
		if (si != null) {
			// Carry the caller's governance fields onto the resolved target.
			if (si.getDeprecationReason() != null) {
				target.setDeprecationReason(si.getDeprecationReason());
			}
			if (si.getReplacedBy() != null) {
				target.setReplacedBy(si.getReplacedBy());
			}
		}
		target.setName(name);
		Diagnostic d = broker.deprecateCatalogEntry(target, requestor);
		return HttpDiagnostics.toResponse(d);
	}

	@DELETE
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_XML)
	public Response remove(@PathParam("name") String name,
			@QueryParam("fingerprint") String fingerprint,
			@HeaderParam("X-DDSR-Requestor") @DefaultValue("anonymous") String requestor) {
		ServiceInterface target = governanceTarget(name, fingerprint);
		if (target == null) {
			return Response.status(404)
					.entity("no catalog entry named '" + name + "' with fingerprint " + fingerprint).build();
		}
		Diagnostic d = broker.removeCatalogEntry(target, requestor);
		return HttpDiagnostics.toResponse(d);
	}
}
