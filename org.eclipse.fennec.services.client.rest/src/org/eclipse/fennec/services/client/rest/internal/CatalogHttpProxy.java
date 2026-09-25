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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
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

	private static final Logger LOG = Logger.getLogger(CatalogHttpProxy.class.getName());

	@Reference
	private RestTransport tx;

	/**
	 * The requestor a governance call names.
	 *
	 * <p>Until #125 this fell back to {@code "anonymous"}, which meant
	 * the catalog recorded nothing about who changed it in the common
	 * case where a caller passed nothing. The fallback is now this
	 * client's own origin: a caller that knows better still wins, and a
	 * caller that does not is no longer anonymous by accident.
	 */
	private String requestorOr(String requestor) {
		return requestor != null && !requestor.isBlank() ? requestor : tx.originToken();
	}

	@Override
	public Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		Response r = tx.send("BrokerCatalog/addCatalogEntry",
				tx.target().path("catalog")
						.request(MediaType.APPLICATION_XML)
						.header("X-DDSR-Requestor", requestorOr(requestor)),
				request -> request.post(Entity.entity(serviceInterface, MediaType.APPLICATION_XML)));
		return readDiagnostic(r);
	}

	@Override
	public Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		Response r = tx.send("BrokerCatalog/deprecateCatalogEntry",
				tx.target().path("catalog/{name}/deprecate")
						.resolveTemplate("name", serviceInterface.getName())
						.request(MediaType.APPLICATION_XML)
						.header("X-DDSR-Requestor", requestorOr(requestor)),
				request -> request.put(Entity.entity(serviceInterface, MediaType.APPLICATION_XML)));
		return readDiagnostic(r);
	}

	@Override
	public Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor) {
		Response r = tx.send("BrokerCatalog/removeCatalogEntry",
				tx.target().path("catalog/{name}")
						.resolveTemplate("name", serviceInterface.getName())
						.request(MediaType.APPLICATION_XML)
						.header("X-DDSR-Requestor", requestorOr(requestor)),
				Invocation.Builder::delete);
		return readDiagnostic(r);
	}

	@Override
	public RemoteServiceRegistry getRegistry() {
		// One document with several roots: the registry first, then the
		// providers it names without containing (#174). Read as a bundle,
		// so the siblings share the registry's resource and its
		// references resolve instead of staying proxies.
		XmiBundle bundle = tx.send("BrokerCatalog/getRegistry",
				tx.target().path("registry").request(MediaType.APPLICATION_XML),
				request -> request.get(XmiBundle.class));
		return registryOf(bundle);
	}

	/** The registry a /registry answer starts with; its siblings stay in its resource. */
	static RemoteServiceRegistry registryOf(XmiBundle bundle) {
		if (bundle.roots().isEmpty() || !(bundle.roots().get(0) instanceof RemoteServiceRegistry registry)) {
			throw new IllegalStateException("the broker's /registry answer does not start with a registry");
		}
		return registry;
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
				// Buffered so the body can be read a second time when the
				// first read fails. Without it the failure says only that
				// something was wrong, and the one thing that would say
				// what — the body — is gone with the stream.
				try {
					r.bufferEntity();
				} catch (RuntimeException notBufferable) {
					LOG.log(Level.FINE, "[DDSR-Client] response could not be buffered", notBufferable);
				}
				try {
					return r.readEntity(Diagnostic.class);
				} catch (Exception parseFail) {
					return synth(r.getStatus(), "could not parse Diagnostic body: " + parseFail.getMessage()
							+ " [HTTP " + r.getStatus() + ", " + r.getMediaType() + ", body: " + preview(r) + "]");
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

	/**
	 * The beginning of the body, for a failure message.
	 *
	 * <p>This goes into our own log and into the exception a caller sees
	 * — not back over HTTP. The parser's own message is deliberately not
	 * echoed to a remote caller (S7 in the codec), but the operator whose
	 * broker answered something unreadable has to be able to see what it
	 * was; "unparsable" alone has cost more than one debugging session.
	 */
	private static String preview(Response r) {
		try {
			String body = r.readEntity(String.class);
			if (body == null || body.isBlank()) {
				return "<empty>";
			}
			String flat = body.strip().replaceAll("\\s+", " ");
			return flat.length() <= 300 ? flat : flat.substring(0, 300) + "… (" + body.length() + " chars)";
		} catch (Exception unreadable) {
			return "<unreadable: " + unreadable.getMessage() + ">";
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
