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

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST proxy for {@link BrokerImplementations}. {@code registerService}
 * is local-style and not exposed via REST — calling it on this proxy
 * throws.
 *
 * <p>The wire body is a single-root XMI document: the provider tree.
 * {@code impl.serviceInterfaces} entries are expected to live in a
 * {@link org.eclipse.emf.ecore.resource.Resource} whose URI points
 * at the broker's canonical {@code /catalog/{name}} URL — EMF then
 * emits cross-document {@code <serviceInterfaces href="…"/>} hrefs,
 * which the broker resolves to its live catalog entries.
 */
@Component(
		service = BrokerImplementations.class,
		property = "ddsr.broker.transport=rest")
@ServiceDescription("DDSR BrokerImplementations REST proxy")
public final class ImplementationsHttpProxy implements BrokerImplementations {

	@Reference
	private RestTransport tx;

	@Override
	public Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		Response r = tx.send("BrokerImplementations/publishImplementation",
				tx.target().path("implementations").request(MediaType.APPLICATION_XML),
				request -> request.post(bodyFor(provider, implementation)));
		return CatalogHttpProxy.readDiagnostic(r);
	}

	/**
	 * The provider is the body; a detached {@code replaces} stub (a
	 * (name, version) ServiceImplementation, UPDATE_POLICY.md §2) travels
	 * as a sibling root so the cross-reference is resolvable — it lives
	 * in no resource and would otherwise be a dangling href.
	 */
	private static Entity<?> bodyFor(ServiceProvider provider, ServiceImplementation implementation) {
		ServiceImplementation replaces = implementation != null ? implementation.getReplaces() : null;
		if (replaces != null && replaces.eResource() == null && replaces.eContainer() == null) {
			return Entity.entity(new XmiBundle(provider, replaces), MediaType.APPLICATION_XML);
		}
		return Entity.entity(provider, MediaType.APPLICATION_XML);
	}

	@Override
	public Diagnostic modifyImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		Response r = tx.send("BrokerImplementations/modifyImplementation",
				tx.target().path("implementations").request(MediaType.APPLICATION_XML),
				request -> request.put(bodyFor(provider, implementation)));
		return CatalogHttpProxy.readDiagnostic(r);
	}

	@Override
	public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		// POST /implementations/withdraw — not the body-carrying DELETE:
		// Jersey refuses a DELETE entity client-side ("Entity must be
		// null for http method DELETE"), which silently killed every
		// withdraw this proxy ever attempted (DECISIONS_PARITY D14).
		Response r = tx.send("BrokerImplementations/withdrawImplementation",
				tx.target().path("implementations").path("withdraw")
						.request(MediaType.APPLICATION_XML),
				request -> request.post(Entity.entity(provider, MediaType.APPLICATION_XML)));
		return CatalogHttpProxy.readDiagnostic(r);
	}

	@Override
	public Diagnostic heartbeat(String referenceId, long intervalSeconds) {
		// PUT /references/{id}/heartbeat?intervalSeconds=N — no body on the
		// wire; the empty text entity only satisfies Jersey's client-side
		// "PUT needs an entity" check.
		Response r = tx.send("BrokerImplementations/heartbeat",
				tx.target().path("references").path(referenceId).path("heartbeat")
						.queryParam("intervalSeconds", intervalSeconds)
						.request(MediaType.APPLICATION_XML),
				request -> request.put(Entity.text("")));
		return CatalogHttpProxy.readDiagnostic(r);
	}

	@Override
	public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
		throw new UnsupportedOperationException(
				"registerService is a local-style operation — use publishImplementation against this REST proxy");
	}
}
