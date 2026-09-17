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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.RestParameterBinding;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.ServiceInvoker;
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
 * <p><b>Where an argument travels is the provider's statement.</b> A
 * {@link RestParameterBinding} on the operation flavor says it per
 * parameter — {@code PATH} into a template segment, {@code QUERY} as a
 * query parameter, {@code HEADER} as a request header, {@code BODY} in
 * the payload — under {@code wireName} where one is given, otherwise
 * under the parameter's own name. The broker never reads a flavor: a
 * provider declares its transport and a consumer follows it, and this
 * is the consumer side of that (#74).
 *
 * <p>An argument whose parameter carries <em>no</em> binding keeps the
 * older convention: a single {@link EObject} becomes the XMI body, and
 * anything else is appended as a query parameter. The model documents
 * {@code BODY} as the default, but there is no encoding for several
 * primitive arguments in one body — {@code XmiBundle} is a multi-root
 * envelope for the broker's own API, not an invocation format — so
 * until that wire shape exists, an undeclared parameter travels the way
 * it always has rather than into a body nobody can read.
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
		try {
			response = send(target, method, place(op, safeArgs), accept);
		} catch (ProcessingException unreachable) {
			// Connect refused, connect/read timeout, reset: the provider is
			// registered but not answering. Marked as a transport failure so
			// the proxy can rebind and retry once (#59).
			throw DdsrException.transport("invoking " + operationName + " at " + url + " failed: "
					+ unreachable.getMessage(), unreachable);
		}
		return readResponse(response);
	}

	private static Response send(WebTarget target, HttpMethod method, Placement placement, String accept) {
		// The body first: it decides whether an undeclared argument was
		// consumed as the payload or still has to travel as a query
		// parameter.
		Entity<?> body = placement.body();

		for (Map.Entry<String, Object> e : placement.path.entrySet()) {
			target = target.resolveTemplate(e.getKey(), String.valueOf(e.getValue()));
		}
		for (Map.Entry<String, Object> e : placement.queryParameters().entrySet()) {
			target = target.queryParam(e.getKey(), String.valueOf(e.getValue()));
		}

		Invocation.Builder request = target.request(accept);
		for (Map.Entry<String, Object> e : placement.header.entrySet()) {
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
	 * Sort the arguments into the places the flavor declares for them.
	 * Keyed by wire name, so a binding can rename a parameter on the wire
	 * without touching the contract.
	 */
	static Placement place(RestOperationFlavor op, Map<String, Object> args) {
		Placement placement = new Placement();
		for (Map.Entry<String, Object> arg : args.entrySet()) {
			RestParameterBinding binding = bindingFor(op, arg.getKey());
			if (binding == null) {
				placement.undeclared.put(arg.getKey(), arg.getValue());
				continue;
			}
			String wireName = binding.getWireName() != null && !binding.getWireName().isBlank()
					? binding.getWireName()
					: arg.getKey();
			ParameterBinding where = binding.getBinding() != null ? binding.getBinding() : ParameterBinding.BODY;
			switch (where) {
			case PATH -> placement.path.put(wireName, arg.getValue());
			case QUERY -> placement.query.put(wireName, arg.getValue());
			case HEADER -> placement.header.put(wireName, arg.getValue());
			case BODY -> placement.declaredBody.put(wireName, arg.getValue());
			}
		}
		return placement;
	}

	private static RestParameterBinding bindingFor(RestOperationFlavor op, String parameterName) {
		for (RestParameterBinding binding : op.getParameterBindings()) {
			Parameter bound = binding.getParameter();
			if (bound != null && parameterName.equals(bound.getName())) {
				return binding;
			}
		}
		return null;
	}

	/** Where each argument of one call travels. */
	static final class Placement {

		final Map<String, Object> path = new LinkedHashMap<>();
		final Map<String, Object> query = new LinkedHashMap<>();
		final Map<String, Object> header = new LinkedHashMap<>();
		final Map<String, Object> declaredBody = new LinkedHashMap<>();
		final Map<String, Object> undeclared = new LinkedHashMap<>();

		private boolean bodyTakenFromUndeclared;

		/**
		 * The request body, or {@code null} for a call without one.
		 * Undeclared arguments fall back to the older convention: one
		 * EObject is the body, anything else goes as a query parameter.
		 */
		Entity<?> body() {
			Map<String, Object> candidates = declaredBody.isEmpty() ? undeclared : declaredBody;
			if (candidates.size() == 1) {
				Object only = candidates.values().iterator().next();
				if (only instanceof EObject eObject) {
					bodyTakenFromUndeclared = declaredBody.isEmpty();
					return Entity.entity(eObject, MediaType.APPLICATION_XML);
				}
			}
			if (!declaredBody.isEmpty()) {
				throw new DdsrException("operation declares " + declaredBody.size()
						+ " BODY parameter(s) that are not a single EObject — there is no wire encoding"
						+ " for that yet; bind them as QUERY, HEADER or PATH");
			}
			return null;
		}

		/**
		 * Everything that goes into the query string: what the flavor
		 * bound there, plus the undeclared arguments that did not become
		 * the body. Call after {@link #body()}.
		 */
		Map<String, Object> queryParameters() {
			Map<String, Object> all = new LinkedHashMap<>(query);
			if (!bodyTakenFromUndeclared) {
				all.putAll(undeclared);
			}
			return all;
		}
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
