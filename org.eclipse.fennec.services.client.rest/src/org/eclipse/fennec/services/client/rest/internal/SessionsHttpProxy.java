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
package org.eclipse.fennec.services.client.rest.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST proxy for the broker's acquisition endpoints (ACQUISITION.md §4).
 * The wire form follows the publish convention: the acquisition list
 * travels as sibling {@code ServiceReference} id-stubs beside the
 * {@code ConsumerSession} root — the model's {@code acquisitions}
 * reference is transient and never serialized.
 */
@Component(
		service = BrokerSessions.class,
		property = "ddsr.broker.transport=rest")
@ServiceDescription("DDSR BrokerSessions REST proxy")
public final class SessionsHttpProxy implements BrokerSessions {

	@Reference
	private RestTransport tx;

	@Override
	public Diagnostic putSession(ConsumerSession session, Collection<String> acquiredReferenceIds) {
		List<EObject> roots = new ArrayList<>();
		roots.add(session);
		if (acquiredReferenceIds != null) {
			for (String referenceId : acquiredReferenceIds) {
				ServiceReference stub = ServicesFactory.eINSTANCE.createServiceReference();
				stub.setId(referenceId);
				roots.add(stub);
			}
		}
		Response r = tx.target().path("consumers").path(session.getConsumerId())
				.request(MediaType.APPLICATION_XML)
				.put(Entity.entity(new XmiBundle(roots), MediaType.APPLICATION_XML));
		return CatalogHttpProxy.readDiagnostic(r);
	}

	@Override
	public Diagnostic deleteSession(String consumerId) {
		Response r = tx.target().path("consumers").path(consumerId)
				.request(MediaType.APPLICATION_XML)
				.delete();
		return CatalogHttpProxy.readDiagnostic(r);
	}

	@Override
	public Optional<SessionSnapshot> getSession(String consumerId) {
		Response r = tx.target().path("consumers").path(consumerId)
				.request(MediaType.APPLICATION_XML)
				.get();
		if (r.getStatus() == 404) {
			r.close();
			return Optional.empty();
		}
		XmiBundle bundle = r.readEntity(XmiBundle.class);
		ConsumerSession session = null;
		List<String> ids = new ArrayList<>();
		for (EObject root : bundle.roots()) {
			if (root instanceof ConsumerSession cs && session == null) {
				session = cs;
			} else if (root instanceof ServiceReference ref && ref.getId() != null) {
				ids.add(ref.getId());
			}
		}
		return session == null ? Optional.empty() : Optional.of(new SessionSnapshot(session, ids));
	}

	@Override
	public int expireSessions(Instant cutoff) {
		throw new UnsupportedOperationException(
				"expireSessions is a broker-side maintenance operation — not exposed over REST");
	}

	@Override
	public int sessionCount() {
		throw new UnsupportedOperationException(
				"sessionCount is a broker-side maintenance operation — not exposed over REST");
	}
}
