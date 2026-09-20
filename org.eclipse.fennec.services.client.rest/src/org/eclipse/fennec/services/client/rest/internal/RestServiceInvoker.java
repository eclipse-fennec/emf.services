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
import java.util.logging.Logger;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.ServiceInvoker;
import org.eclipse.fennec.services.cloudevents.CloudEventCodec;
import org.eclipse.fennec.services.cloudevents.CloudEvents;
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

import io.cloudevents.model.ce.CloudEvent;

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

	private static final Logger LOG = Logger.getLogger(RestServiceInvoker.class.getName());

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

		// What the body is written AS, which is a different question from
		// what we accept back (#100): consumes is the provider's statement
		// about its input, produces about its output, and an operation may
		// well take protobuf and answer XML.
		String contentType = !op.getConsumes().isEmpty()
				? op.getConsumes().get(0)
				: MediaType.APPLICATION_XML;

		HttpMethod method = op.getMethod() != null ? op.getMethod() : HttpMethod.GET;
		// Binary mode (#101): the attributes ride as ce-* headers and the
		// body stays exactly the payload it was. That is why this is
		// additive over HTTP and breaking nowhere — a provider that does
		// not read the headers reads the same request it always did.
		CloudEvent envelope = CloudEvents.newEnvelope(CloudEvents.TYPE_INVOKE,
				tx.originReference(), null);
		envelope.setSubject(subjectOf(op, operationName));
		Response response;
		try {
			response = send(target, method, RestPlacement.of(op, safeArgs), accept, contentType,
					CloudEventCodec.toHeaders(envelope));
		} catch (ProcessingException unreachable) {
			// Connect refused, connect/read timeout, reset: the provider is
			// registered but not answering. Marked as a transport failure so
			// the proxy can rebind and retry once (#59).
			throw DdsrException.transport("invoking " + operationName + " at " + url + " failed: "
					+ unreachable.getMessage(), unreachable);
		}
		checkCorrelation(envelope, response, operationName);
		return readResponse(response);
	}

	/**
	 * What the call is about: the contract and the operation, which is
	 * what a reader of the attribute wants and what a trace can group
	 * by. Falls back to the operation name when the flavor's operation
	 * is not resolvable — an envelope with a thinner subject is still
	 * better than no envelope.
	 */
	private static String subjectOf(RestOperationFlavor op, String operationName) {
		ServiceOperation operation = op.getOperation();
		if (operation != null && operation.eContainer() instanceof ServiceInterface contract
				&& contract.getName() != null) {
			return contract.getName() + "/" + operation.getName();
		}
		return operationName;
	}

	/**
	 * An answer says which call it answers. Over HTTP the connection
	 * already said it, so a mismatch cannot normally happen — which is
	 * precisely why it is worth a line: if it ever does, something
	 * between here and the provider is handing out somebody else's
	 * answer, and that is not a thing to discover from the values.
	 *
	 * <p>A missing envelope is not a mismatch. Binary mode is additive
	 * and a provider is free not to have gained it.
	 */
	private static void checkCorrelation(CloudEvent request, Response response, String operationName) {
		String correlation = response.getHeaderString(
				CloudEvents.HEADER_PREFIX + CloudEvents.EXTENSION_CORRELATION_ID);
		if (correlation != null && !correlation.equals(request.getEventId())) {
			LOG.warning("[DDSR-Client] the answer to " + operationName + " correlates with "
					+ correlation + ", not with the request " + request.getEventId());
		}
	}

	/**
	 * Whether this response can be read back as a model.
	 *
	 * <p>Asked by trying, because the providers are the authority on
	 * what they can decode and duplicating their answer here is how the
	 * two drift. A failure means "not a model", which is exactly what
	 * the text fallback is for.
	 */
	private static boolean readsAsModel(Response response, MediaType mediaType) {
		if (MediaType.APPLICATION_XML_TYPE.isCompatible(mediaType)) {
			return true;
		}
		return response.getMediaType() != null
				&& !MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType)
				&& !MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType);
	}

	private static Response send(WebTarget target, HttpMethod method, RestPlacement placement, String accept,
			String contentType, Map<String, String> envelopeHeaders) {
		// The body first: it decides whether an undeclared argument was
		// consumed as the payload or still has to travel as a query
		// parameter.
		Entity<?> body = placement.body()
				.map(payload -> Entity.entity(payload, contentType))
				.orElse(null);

		for (Map.Entry<String, Object> e : placement.path().entrySet()) {
			target = target.resolveTemplate(e.getKey(), String.valueOf(e.getValue()));
		}
		for (Map.Entry<String, Object> e : placement.query().entrySet()) {
			target = target.queryParam(e.getKey(), String.valueOf(e.getValue()));
		}

		Invocation.Builder request = target.request(accept);
		for (Map.Entry<String, String> e : envelopeHeaders.entrySet()) {
			request = request.header(e.getKey(), e.getValue());
		}
		// After the envelope, so a contract that binds a parameter to a
		// header wins over it: the call is what the contract says, and
		// the envelope is what carries it.
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
			// A model comes back whenever a provider is registered for what
			// arrived — the message body reader answers that question, and
			// since #100 it answers it for every encoding the deployment
			// knows, not only for XML. Anything else is text, as before.
			MediaType mt = response.getMediaType();
			if (mt != null && response.hasEntity() && readsAsModel(response, mt)) {
				return response.readEntity(EObject.class);
			}
			return Optional.ofNullable(response.readEntity(String.class)).orElse(null);
		} finally {
			response.close();
		}
	}
}
