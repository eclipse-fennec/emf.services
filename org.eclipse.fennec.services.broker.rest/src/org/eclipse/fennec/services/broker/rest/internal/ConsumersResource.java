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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.xmi.codec.WireBody;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.eclipse.fennec.services.xmi.codec.XmiCodecException;
import org.eclipse.fennec.services.xmi.codec.XmiHttpErrors;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.annotations.RequireJakartarsWhiteboard;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The acquisition stage on the wire (ACQUISITION.md §4): one idempotent
 * full replace instead of acquire/release/heartbeat verbs.
 *
 * <pre>
 * PUT    /consumers/{consumerId}   body: ConsumerSession + ServiceReference id-stub siblings
 * GET    /consumers/{consumerId}   → same shape back (diagnosis: what does the broker believe?)
 * DELETE /consumers/{consumerId}   → shutdown-notify, releases all acquisitions
 * </pre>
 *
 * The wire form follows the publish convention: the acquisition list
 * travels as sibling {@code ServiceReference} roots carrying only their
 * {@code id} — the model's {@code acquisitions} reference is transient
 * and never serialized.
 */
@RequireHttpWhiteboard
@RequireJakartarsWhiteboard
@JakartarsResource
@JakartarsName("ddsr-consumers")
@Component(service = ConsumersResource.class, scope = ServiceScope.PROTOTYPE)
@Path("/consumers")
public class ConsumersResource {

	@Reference
	private BrokerSessions sessions;

	@Reference
	private ComponentServiceObjects<ResourceSet> rsObjects;

	@PUT
	@Path("{consumerId}")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public Response put(@PathParam("consumerId") String consumerId, InputStream entityStream)
			throws IOException {
		XmiBundle bundle = parse(entityStream);
		ConsumerSession session = null;
		List<String> referenceIds = new ArrayList<>();
		for (EObject root : bundle.roots()) {
			if (root instanceof ConsumerSession cs && session == null) {
				session = cs;
			} else if (root instanceof ServiceReference ref && ref.getId() != null && !ref.getId().isBlank()) {
				referenceIds.add(ref.getId());
			}
		}
		if (session == null) {
			return Response.status(400)
					.entity("body must contain a <services:ConsumerSession> root").build();
		}
		// The path owns the identity; a body id may confirm but not
		// contradict it (an idempotent PUT to someone else's session
		// would silently replace it).
		String bodyId = session.getConsumerId();
		if (bodyId == null || bodyId.isBlank()) {
			session.setConsumerId(consumerId);
		} else if (!bodyId.equals(consumerId)) {
			return Response.status(400)
					.entity("body consumerId '" + bodyId + "' contradicts the path ('" + consumerId + "')")
					.build();
		}
		Diagnostic d = sessions.putSession(session, referenceIds);
		return HttpDiagnostics.toResponse(d);
	}

	@GET
	@Path("{consumerId}")
	@Produces(MediaType.APPLICATION_XML)
	public Response get(@PathParam("consumerId") String consumerId) {
		return sessions.getSession(consumerId)
				.map(snapshot -> {
					List<EObject> roots = new ArrayList<>();
					roots.add(snapshot.session());
					for (String referenceId : snapshot.acquiredReferenceIds()) {
						ServiceReference stub = ServicesFactory.eINSTANCE.createServiceReference();
						stub.setId(referenceId);
						roots.add(stub);
					}
					return Response.ok(new XmiBundle(roots)).type(MediaType.APPLICATION_XML).build();
				})
				.orElseGet(() -> Response.status(404)
						.entity("no session for consumer '" + consumerId + "'").build());
	}

	@DELETE
	@Path("{consumerId}")
	@Produces(MediaType.APPLICATION_XML)
	public Response delete(@PathParam("consumerId") String consumerId) {
		return HttpDiagnostics.toResponse(sessions.deleteSession(consumerId));
	}

	private XmiBundle parse(InputStream entityStream) throws IOException {
		try {
			byte[] body = WireBody.readFully(entityStream);
			return XmiCodec.readBundle(new ByteArrayInputStream(body), rsObjects);
		} catch (XmiCodecException refusal) {
			throw XmiHttpErrors.toHttp(refusal);
		}
	}
}
