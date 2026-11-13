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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import org.eclipse.fennec.services.models.ModelDocument;
import org.eclipse.fennec.services.models.ModelSource;
import org.eclipse.fennec.services.models.ModelStore;
import org.eclipse.fennec.services.security.jwt.SecurityNames;
import org.eclipse.fennec.services.xmi.codec.WireBody;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.annotations.RequireJakartarsWhiteboard;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsExtensionSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The payload models a broker knows (#256).
 *
 * <pre>
 * PUT /models                                 body: the Ecore of one package → 201, the fingerprint
 * GET /models?nsURI=…[&amp;fingerprint=fp1:…]     → the Ecore, 404 when the source has no such version
 * </pre>
 *
 * A provider hands in the packages its contracts use before it registers
 * them; the broker computes the fingerprint itself and answers with it,
 * which is what a provider that cannot compute one (TypeScript) pins. A
 * consumer fetches what it cannot read a payload without. Handing in
 * needs {@code service-provider}, fetching {@code service-consumer}.
 *
 * <p>What is fetched comes from the broker's model source - the same one
 * its gate asks, named by {@code models.target} in the broker's
 * configuration; what is handed in goes to the broker's own store.
 */
@RequireHttpWhiteboard
@RequireJakartarsWhiteboard
@JakartarsResource
@JakartarsExtensionSelect(SecurityNames.SELECT_ANY_AUTH)
@JakartarsName("ddsr-models")
@Component(service = ModelsResource.class, scope = ServiceScope.PROTOTYPE,
		configurationPid = "org.eclipse.fennec.services.broker.core",
		configurationPolicy = ConfigurationPolicy.OPTIONAL,
		property = "models.target=(" + ModelSource.PROPERTY + "=embedded)")
@Path("/models")
public class ModelsResource {

	/** The header that carries a model's fingerprint, beside the Ecore. */
	static final String FINGERPRINT_HEADER = "X-Fennec-Model-Fingerprint";

	@Reference(target = "(" + ModelSource.PROPERTY + "=embedded)")
	ModelStore store;

	@Reference(name = "models")
	ModelSource source;

	@PUT
	@Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML, "application/xmi+xml", MediaType.APPLICATION_OCTET_STREAM })
	@Produces(MediaType.TEXT_PLAIN)
	public Response put(InputStream entityStream) throws IOException {
		Response refused = BrokerRoles.refusal(BrokerRoles.PROVIDER);
		if (refused != null) {
			return refused;
		}
		ModelDocument kept;
		try {
			kept = store.store(WireBody.readFully(entityStream));
		} catch (IllegalArgumentException unreadable) {
			return Response.status(Response.Status.BAD_REQUEST).entity(unreadable.getMessage()).build();
		}
		return Response.created(URI.create("models?nsURI=" + kept.nsURI() + "&fingerprint=" + kept.fingerprint()))
				.header(FINGERPRINT_HEADER, kept.fingerprint())
				.entity(kept.fingerprint())
				.build();
	}

	@GET
	@Produces(MediaType.APPLICATION_XML)
	public Response get(@QueryParam("nsURI") String nsURI, @QueryParam("fingerprint") String fingerprint) {
		Response refused = BrokerRoles.refusal(BrokerRoles.CONSUMER);
		if (refused != null) {
			return refused;
		}
		if (nsURI == null || nsURI.isBlank()) {
			return Response.status(Response.Status.BAD_REQUEST).entity("which model? nsURI is missing").build();
		}
		Optional<ModelDocument> found = source.resolve(nsURI, fingerprint == null || fingerprint.isBlank() ? null : fingerprint);
		if (found.isEmpty()) {
			return Response.status(Response.Status.NOT_FOUND)
					.entity("no model " + nsURI + (fingerprint == null ? "" : " at " + fingerprint) + " here").build();
		}
		ModelDocument model = found.get();
		return Response.ok(model.ecore()).header(FINGERPRINT_HEADER, model.fingerprint()).build();
	}
}
