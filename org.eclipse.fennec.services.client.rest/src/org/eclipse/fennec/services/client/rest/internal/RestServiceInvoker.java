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

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST-flavor implementation of {@link ServiceInvoker}.
 *
 * <p>Marshalling convention:
 * <ul>
 *   <li>{@code GET}: each named argument is appended as a query
 *       parameter using {@code String.valueOf}.</li>
 *   <li>{@code POST}/{@code PUT}/{@code DELETE}: exactly one
 *       {@link EObject} argument is sent as XMI body via the
 *       registered providers. Other shapes throw
 *       {@link UnsupportedOperationException} for now — we'll widen
 *       the convention once the wire shape for primitive parameters
 *       (JSON? form?) is decided.</li>
 * </ul>
 *
 * <p>Response handling: {@code application/xml} bodies are parsed as
 * {@link EObject}; everything else comes back as {@code String}.
 * Empty bodies return {@code null}.
 */
@Component(
		service = ServiceInvoker.class,
		property = "ddsr.broker.transport=rest")
@ServiceDescription("DDSR REST-flavor reflective service invoker")
public final class RestServiceInvoker implements ServiceInvoker {

	@Reference
	private RestTransport tx;

	@Override
	public Object invoke(ServiceLocator locator, String operationName, Map<String, Object> args) {
		if (locator == null || operationName == null) {
			throw new DdsrException("locator and operationName must not be null");
		}
		RestFlavor rf = locator.restFlavor().orElseThrow(
				() -> new DdsrException("service does not expose a REST flavor"));
		RestOperationFlavor op = findOperationFlavor(rf, operationName);
		if (op == null) {
			throw new DdsrException("operation '" + operationName + "' has no REST flavor on this service");
		}
		URI url = locator.urlFor(operationName).orElseThrow(
				() -> new DdsrException("cannot build URL for '" + operationName + "' — RestFlavor.host is missing"));

		Map<String, Object> safeArgs = args != null ? args : Map.of();
		WebTarget target = tx.targetFor(url);

		// Accept header reflects what the OperationFlavor advertises;
		// xml fallback covers the broker's own catalog/registry/etc.
		String accept = !op.getProduces().isEmpty()
				? op.getProduces().get(0)
				: MediaType.APPLICATION_XML;

		HttpMethod method = op.getMethod() != null ? op.getMethod() : HttpMethod.GET;
		Response response;
		switch (method) {
		case GET:
			for (Map.Entry<String, Object> e : safeArgs.entrySet()) {
				target = target.queryParam(e.getKey(), String.valueOf(e.getValue()));
			}
			response = target.request(accept).get();
			break;
		case POST:
		case PUT:
		case DELETE: {
			Entity<?> body = bodyFor(safeArgs);
			if (body == null) {
				// No EObject body — pass primitive/string args as query
				// parameters. Matches the broker-self-published catalog
				// operations and the TS Payment service convention.
				for (Map.Entry<String, Object> e : safeArgs.entrySet()) {
					target = target.queryParam(e.getKey(), String.valueOf(e.getValue()));
				}
			}
			Invocation.Builder b = target.request(accept);
			if (body == null) {
				response = b.method(method.name());
			} else {
				response = b.method(method.name(), body);
			}
			break;
		}
		default:
			throw new UnsupportedOperationException("HTTP method not supported: " + method);
		}

		return readResponse(response);
	}

	private static RestOperationFlavor findOperationFlavor(RestFlavor rf, String name) {
		for (ServiceOperationFlavor of : rf.getOperationFlavors()) {
			if (of instanceof RestOperationFlavor && name.equals(of.getName())) {
				return (RestOperationFlavor) of;
			}
		}
		for (ServiceOperationFlavor of : rf.getOperationFlavors()) {
			if (of instanceof RestOperationFlavor && of.getOperation() != null
					&& name.equals(of.getOperation().getName())) {
				return (RestOperationFlavor) of;
			}
		}
		return null;
	}

	/**
	 * Pick a body for a non-GET call. A single {@code EObject} arg
	 * goes as XMI body (catalog/impl publish style). Anything else
	 * returns {@code null} so the caller can fall back to passing
	 * primitive / string args as query parameters.
	 */
	private static Entity<?> bodyFor(Map<String, Object> args) {
		if (args.size() == 1) {
			Object only = args.values().iterator().next();
			if (only instanceof EObject) {
				return Entity.entity((EObject) only, MediaType.APPLICATION_XML);
			}
		}
		return null;
	}

	private static Object readResponse(Response response) {
		try {
			if (response.getStatus() / 100 != 2) {
				String body = response.hasEntity() ? response.readEntity(String.class) : "";
				throw new DdsrException("HTTP " + response.getStatus() + ": " + body);
			}
			if (!response.hasEntity()) {
				return null;
			}
			MediaType mt = response.getMediaType();
			if (mt != null && MediaType.APPLICATION_XML_TYPE.isCompatible(mt)) {
				return response.readEntity(EObject.class);
			}
			return Optional.ofNullable(response.readEntity(String.class)).orElse(null);
		} finally {
			response.close();
		}
	}
}
