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

import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceRegistration;
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
		Response r = tx.target().path("implementations")
				.request(MediaType.APPLICATION_XML)
				.post(Entity.entity(provider, MediaType.APPLICATION_XML));
		return CatalogHttpProxy.readDiagnostic(r);
	}

	@Override
	public Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation) {
		// POST /implementations/withdraw — not the body-carrying DELETE:
		// Jersey refuses a DELETE entity client-side ("Entity must be
		// null for http method DELETE"), which silently killed every
		// withdraw this proxy ever attempted (DECISIONS_PARITY D14).
		Invocation.Builder b = tx.target().path("implementations").path("withdraw")
				.request(MediaType.APPLICATION_XML);
		Response r = b.post(Entity.entity(provider, MediaType.APPLICATION_XML));
		return CatalogHttpProxy.readDiagnostic(r);
	}

	@Override
	public ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation) {
		throw new UnsupportedOperationException(
				"registerService is a local-style operation — use publishImplementation against this REST proxy");
	}
}
