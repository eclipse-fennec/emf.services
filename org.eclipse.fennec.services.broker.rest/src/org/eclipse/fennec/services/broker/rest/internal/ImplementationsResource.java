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
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Publish / withdraw service implementations. The body is an XMI
 * document (single-root or multi-root via {@code <xmi:XMI>}). The
 * first {@code <services:ServiceProvider>} root is the publishing
 * provider; any sibling {@code <services:ServiceInterface>} roots are
 * stubs that the impl's {@code serviceInterfaces} cross-refs point
 * at, so the broker can resolve them against its catalog by name.
 *
 * <p>To aid debugging of cross-language publish bodies (TS, Python,
 * curl), a bounded preview of the raw request body is logged at FINE
 * before parsing.
 */
@RequireHttpWhiteboard
@RequireJakartarsWhiteboard
@JakartarsResource
@JakartarsName("ddsr-implementations")
@Component(service = ImplementationsResource.class, scope = ServiceScope.PROTOTYPE)
@Path("/implementations")
public class ImplementationsResource {

	private static final Logger LOG = Logger.getLogger(ImplementationsResource.class.getName());

	@Reference
	private BrokerImplementations broker;

	@Reference
	private ComponentServiceObjects<ResourceSet> rsObjects;

	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public Response publish(InputStream entityStream) throws IOException {
		XmiBundle bundle = logAndParse("POST /implementations", entityStream);
		ServiceProvider provider = extractProvider(bundle);
		if (provider == null) {
			return Response.status(400)
					.entity("body must contain a <services:ServiceProvider> root").build();
		}
		ServiceImplementation impl = soleImplementation(provider);
		if (impl == null) {
			return Response.status(400)
					.entity("body must be a <services:ServiceProvider> with exactly one implementation").build();
		}
		Diagnostic d = broker.publishImplementation(provider, impl);
		return HttpDiagnostics.toResponse(d);
	}

	/**
	 * Canonical withdraw. A POST, not a body-carrying DELETE: Jersey's
	 * client refuses a DELETE entity outright ("Entity must be null for
	 * http method DELETE"), so the Java client could never have used the
	 * DELETE variant — its withdraw failed client-side from day one and
	 * the failure was swallowed (found by the FR-P4 harness). The DELETE
	 * route below stays for wire compatibility with clients whose HTTP
	 * stack can send it (fetch can).
	 */
	@POST
	@Path("withdraw")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public Response withdrawViaPost(InputStream entityStream) throws IOException {
		return doWithdraw("POST /implementations/withdraw", entityStream);
	}

	@DELETE
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public Response withdraw(InputStream entityStream) throws IOException {
		return doWithdraw("DELETE /implementations", entityStream);
	}

	private Response doWithdraw(String label, InputStream entityStream) throws IOException {
		XmiBundle bundle = logAndParse(label, entityStream);
		ServiceProvider provider = extractProvider(bundle);
		if (provider == null) {
			return Response.status(400)
					.entity("body must contain a <services:ServiceProvider> root").build();
		}
		ServiceImplementation impl = soleImplementation(provider);
		if (impl == null) {
			return Response.status(400)
					.entity("body must be a <services:ServiceProvider> with exactly one implementation").build();
		}
		Diagnostic d = broker.withdrawImplementation(provider, impl);
		return HttpDiagnostics.toResponse(d);
	}

	/**
	 * Size of the body preview logged at FINE. The dump exists to make
	 * cross-language publish bodies inspectable (see the class comment),
	 * which a first screenful achieves — writing an unbounded,
	 * caller-controlled body into the log is its own resource problem
	 * (S7) and lets a caller push arbitrary content into the operator's
	 * console (S5).
	 */
	private static final int BODY_PREVIEW_BYTES = 4096;

	private XmiBundle logAndParse(String label, InputStream entityStream) throws IOException {
		// Bounded read: the bytes are needed twice (dump + parse), so they
		// have to be held — but never more than the wire limit allows.
		try {
			byte[] body = WireBody.readFully(entityStream);
			LOG.fine(() -> "====================== " + label + " ======================");
			LOG.fine(() -> preview(body));
			LOG.fine(() -> "====================== end of body ======================");
			return XmiCodec.readBundle(new ByteArrayInputStream(body), rsObjects);
		} catch (XmiCodecException refusal) {
			// Both the size check and the parse can refuse the body. The
			// codec speaks no HTTP; this is the layer that does.
			throw XmiHttpErrors.toHttp(refusal);
		}
	}

	private static String preview(byte[] body) {
		if (body.length <= BODY_PREVIEW_BYTES) {
			return new String(body, StandardCharsets.UTF_8);
		}
		return new String(body, 0, BODY_PREVIEW_BYTES, StandardCharsets.UTF_8)
				+ System.lineSeparator()
				+ "… truncated, " + body.length + " bytes total";
	}

	private static ServiceProvider extractProvider(XmiBundle bundle) {
		if (bundle == null) {
			return null;
		}
		for (EObject root : bundle.roots()) {
			if (root instanceof ServiceProvider) {
				return (ServiceProvider) root;
			}
		}
		return null;
	}

	private static ServiceImplementation soleImplementation(ServiceProvider p) {
		if (p == null || p.getImplementations().size() != 1) {
			return null;
		}
		return p.getImplementations().get(0);
	}
}
