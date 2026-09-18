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

import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.flavor.rest.RestPlacement;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST-flavor implementation of {@link ServiceInvoker}.
 *
 * <p><b>Where an argument travels is the provider's statement.</b>
 * {@link RestPlacement} holds that rule — it is the same one the
 * provider side reads the values back with, so the two cannot develop
 * their own reading of a flavor. This class only turns the placement
 * into an HTTP request.
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
		String url = locator.endpointFor(operationName).orElseThrow(
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
		try {
			response = send(target, method, RestPlacement.of(op, safeArgs), accept);
		} catch (ProcessingException unreachable) {
			// Connect refused, connect/read timeout, reset: the provider is
			// registered but not answering. Marked as a transport failure so
			// the proxy can rebind and retry once (#59).
			throw DdsrException.transport("invoking " + operationName + " at " + url + " failed: "
					+ unreachable.getMessage(), unreachable);
		}
		return readResponse(response);
	}

	private static Response send(WebTarget target, HttpMethod method, RestPlacement placement, String accept) {
		// The body first: it decides whether an undeclared argument was
		// consumed as the payload or still has to travel as a query
		// parameter.
		Entity<?> body = placement.body()
				.map(payload -> Entity.entity(payload, MediaType.APPLICATION_XML))
				.orElse(null);

		for (Map.Entry<String, Object> e : placement.path().entrySet()) {
			target = target.resolveTemplate(e.getKey(), String.valueOf(e.getValue()));
		}
		for (Map.Entry<String, Object> e : placement.query().entrySet()) {
			target = target.queryParam(e.getKey(), String.valueOf(e.getValue()));
		}

		Invocation.Builder request = target.request(accept);
		for (Map.Entry<String, Object> e : placement.header().entrySet()) {
			request = request.header(e.getKey(), e.getValue());
		}

		switch (method) {
		case GET:
			return request.get();
		case POST:
		case PUT:
		case DELETE:
			return body == null ? request.method(method.name()) : request.method(method.name(), body);
		default:
			throw new UnsupportedOperationException("HTTP method not supported: " + method);
		}
	}

	/**
	 * The flavor for an operation: by the flavor's own name first, then by
	 * the name of the operation it binds — a provider may name a flavor
	 * differently from the operation it serves.
	 */
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
