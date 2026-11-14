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

package org.eclipse.fennec.tags.ddsr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.Principal;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.common.CallPrincipal;
import org.eclipse.fennec.services.xmi.codec.WireBody;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.eclipse.fennec.services.xmi.codec.XmiCodecException;
import org.eclipse.fennec.services.xmi.codec.jakartars.XmiHttpErrors;
import org.osgi.service.component.ComponentServiceObjects;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * A consumer's session through the facade: the broker's own sessions, on
 * the wire as the broker's {@code /consumers} resource has them — one
 * idempotent full replace, the acquisitions as id-only reference roots
 * (ACQUISITION.md §4). A session is the consumer's, not a tag's, so it is
 * handed through unchanged; what the facade asks is the role.
 */
final class ScopedSessions {

	private final BrokerSessions sessions;

	private final ComponentServiceObjects<ResourceSet> resourceSets;

	ScopedSessions(BrokerSessions sessions, ComponentServiceObjects<ResourceSet> resourceSets) {
		this.sessions = sessions;
		this.resourceSets = resourceSets;
	}

	Response put(String consumerId, InputStream entity) throws IOException {
		Response refused = refusal();
		if (refused != null) {
			return refused;
		}
		XmiBundle bundle;
		try {
			bundle = XmiCodec.readBundle(new ByteArrayInputStream(WireBody.readFully(entity)), resourceSets);
		} catch (XmiCodecException unreadable) {
			throw XmiHttpErrors.toHttp(unreadable);
		}
		ConsumerSession session = null;
		List<String> referenceIds = new ArrayList<>();
		for (EObject root : bundle.roots()) {
			if (root instanceof ConsumerSession found && session == null) {
				session = found;
			} else if (root instanceof ServiceReference reference && reference.getId() != null
					&& !reference.getId().isBlank()) {
				referenceIds.add(reference.getId());
			}
		}
		if (session == null) {
			return Response.status(400).entity("body must contain a <services:ConsumerSession> root").build();
		}
		if (session.getConsumerId() == null || session.getConsumerId().isBlank()) {
			session.setConsumerId(consumerId);
		} else if (!session.getConsumerId().equals(consumerId)) {
			return Response.status(400).entity("body consumerId '" + session.getConsumerId()
					+ "' contradicts the path ('" + consumerId + "')").build();
		}
		return answer(sessions.putSession(session, referenceIds));
	}

	Response get(String consumerId) {
		Response refused = refusal();
		if (refused != null) {
			return refused;
		}
		return sessions.getSession(consumerId).map(snapshot -> {
			List<EObject> roots = new ArrayList<>();
			roots.add(snapshot.session());
			for (String referenceId : snapshot.acquiredReferenceIds()) {
				ServiceReference stub = ServicesFactory.eINSTANCE.createServiceReference();
				stub.setId(referenceId);
				roots.add(stub);
			}
			return Response.ok(new XmiBundle(roots)).type(MediaType.APPLICATION_XML).build();
		}).orElseGet(() -> Response.status(404).entity("no session for consumer '" + consumerId + "'").build());
	}

	Response delete(String consumerId) {
		Response refused = refusal();
		return refused != null ? refused : answer(sessions.deleteSession(consumerId));
	}

	/** {@code null} when the caller may go on: no principal is mode none, a principal needs the role. */
	private static Response refusal() {
		Principal caller = CallPrincipal.current();
		if (caller == null || caller.getScopes().contains(TagFacade.ROLE)) {
			return null;
		}
		return Response.status(Response.Status.FORBIDDEN).entity("this needs " + TagFacade.ROLE).build();
	}

	private static Response answer(Diagnostic diagnostic) {
		int status = switch (diagnostic.getSeverity()) {
			case OK, INFO, WARNING -> 200;
			default -> diagnostic.getCode() == DdsrDiagnostics.CODE_SESSION_OWNERSHIP_VIOLATION ? 403 : 400;
		};
		return Response.status(status).entity(diagnostic).type(MediaType.APPLICATION_XML).build();
	}
}
