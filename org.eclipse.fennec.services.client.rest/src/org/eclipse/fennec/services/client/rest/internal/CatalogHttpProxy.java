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

import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceInterface;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST proxy that exposes {@link BrokerCatalog} as an OSGi service
 * but routes every call through the remote broker via HTTP+XMI.
 *
 * <p>4xx responses are not exceptions — the broker emits the
 * {@link Diagnostic} XMI in the body for both 2xx and 4xx, so we read
 * the entity ourselves rather than letting Jakarta Client throw a
 * generic {@code ClientErrorException}.
 */
@Component(
		service = BrokerCatalog.class,
		property = "ddsr.broker.transport=rest")
@ServiceDescription("DDSR BrokerCatalog REST proxy")
public final class CatalogHttpProxy implements BrokerCatalog {

	@Reference
	private RestTransport tx;

	@Override
	public Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		Response r = tx.target().path("catalog")
				.request(MediaType.APPLICATION_XML)
				.header("X-DDSR-Requestor", requestor != null ? requestor : "anonymous")
				.post(Entity.entity(serviceInterface, MediaType.APPLICATION_XML));
		return readDiagnostic(r);
	}

	@Override
	public Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		Response r = tx.target().path("catalog/{name}/deprecate")
				.resolveTemplate("name", serviceInterface.getName())
				.request(MediaType.APPLICATION_XML)
				.header("X-DDSR-Requestor", requestor != null ? requestor : "anonymous")
				.put(Entity.entity(serviceInterface, MediaType.APPLICATION_XML));
		return readDiagnostic(r);
	}

	@Override
	public Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		Response r = tx.target().path("catalog/{name}")
				.resolveTemplate("name", serviceInterface.getName())
				.request(MediaType.APPLICATION_XML)
				.header("X-DDSR-Requestor", requestor != null ? requestor : "anonymous")
				.delete();
		return readDiagnostic(r);
	}

	@Override
	public RemoteServiceRegistry getRegistry() {
		return tx.target().path("registry")
				.request(MediaType.APPLICATION_XML)
				.get(RemoteServiceRegistry.class);
	}

	@Override
	public Diagnostic snapshot() {
		// Not exposed over REST — snapshot is a server-side persistence
		// concern. Embedded callers use the in-process BrokerCatalog
		// instead.
		throw new UnsupportedOperationException("snapshot() is not available via REST proxy");
	}

	/**
	 * Read a {@link Diagnostic} body regardless of HTTP status — the
	 * broker emits one for 2xx and 4xx alike. Falls back to a
	 * synthesised diagnostic if the body is empty or unparsable.
	 */
	static Diagnostic readDiagnostic(Response r) {
		try {
			if (r.hasEntity()) {
				try {
					return r.readEntity(Diagnostic.class);
				} catch (Exception parseFail) {
					return synth(r.getStatus(), "could not parse Diagnostic body: " + parseFail.getMessage());
				}
			}
			if (r.getStatus() / 100 == 2) {
				return ok();
			}
			return synth(r.getStatus(), "HTTP " + r.getStatus() + " with empty body");
		} finally {
			r.close();
		}
	}

	private static Diagnostic ok() {
		Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
		d.setSeverity(DiagnosticSeverity.OK);
		d.setSource("org.eclipse.fennec.services.client.rest");
		return d;
	}

	private static Diagnostic synth(int status, String message) {
		Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
		d.setSeverity(DiagnosticSeverity.ERROR);
		d.setSource("org.eclipse.fennec.services.client.rest");
		d.setMessage(message);
		d.setCode(status);
		return d;
	}
}
