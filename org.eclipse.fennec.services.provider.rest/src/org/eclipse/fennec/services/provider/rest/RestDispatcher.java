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

package org.eclipse.fennec.services.provider.rest;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.RestExceptionBinding;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.flavor.rest.RestArguments;
import org.eclipse.fennec.services.flavor.rest.RestErrors;
import org.eclipse.fennec.services.flavor.rest.RestRoute;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.component.ComponentServiceObjects;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * One resource for a whole contract: it catches everything under its
 * application's base path and decides from the flavor which operation a
 * request means.
 *
 * <p>Jakarta REST usually reads that from annotations on a class, which is
 * what a generated resource carries. Here the same statements come from
 * the model at runtime, so the routing happens in {@link RestRoute} and
 * the argument reading in {@link RestArguments} — the very rules a
 * consumer writes its requests with.
 *
 * <p>The answers follow the same conventions the generated resource
 * implements, and for the same reason: they are HTTP, not something a
 * contract should have to state.
 *
 * <pre>
 * no operation for method and path   404
 * a body that will not decode        400
 * a required argument is missing     400
 * no result, or an empty collection  204
 * a failing Diagnostic               the status bound to its code, 400 without one
 * a declared error                   the status its binding names, 500 without one
 * anything else                      500
 * </pre>
 */
@Path("/")
public class RestDispatcher {

	private final RestFlavor flavor;
	private final Supplier<ServiceObjects<Object>> implementations;
	private final String contract;
	private final ComponentServiceObjects<ResourceSet> resourceSets;

	/**
	 * @param implementations where the service behind the contract comes
	 *        from, asked per call so a provider that comes and goes is
	 *        followed without bookkeeping here. Finding it is the
	 *        component's business, not this dispatcher's.
	 * @param contract how to name the missing implementation in a 503
	 */
	RestDispatcher(RestFlavor flavor, Supplier<ServiceObjects<Object>> implementations, String contract,
			ComponentServiceObjects<ResourceSet> resourceSets) {
		this.flavor = flavor;
		this.implementations = implementations;
		this.contract = contract;
		this.resourceSets = resourceSets;
	}

	@GET
	@Path("{path:.*}")
	public Response get(@PathParam("path") String path, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
		return dispatch("GET", path, query(uriInfo), headers::getHeaderString, null);
	}

	@POST
	@Path("{path:.*}")
	public Response post(@PathParam("path") String path, @Context UriInfo uriInfo, @Context HttpHeaders headers,
			InputStream entity) {
		return dispatch("POST", path, query(uriInfo), headers::getHeaderString, entity);
	}

	@PUT
	@Path("{path:.*}")
	public Response put(@PathParam("path") String path, @Context UriInfo uriInfo, @Context HttpHeaders headers,
			InputStream entity) {
		return dispatch("PUT", path, query(uriInfo), headers::getHeaderString, entity);
	}

	@DELETE
	@Path("{path:.*}")
	public Response delete(@PathParam("path") String path, @Context UriInfo uriInfo, @Context HttpHeaders headers,
			InputStream entity) {
		return dispatch("DELETE", path, query(uriInfo), headers::getHeaderString, entity);
	}

	private static Function<String, List<String>> query(UriInfo uriInfo) {
		return name -> uriInfo.getQueryParameters().get(name);
	}

	/**
	 * The whole decision, in the terms the model is written in rather
	 * than in Jakarta REST's: which operation, which arguments, what to
	 * answer. The annotated methods above only translate a request into
	 * these terms.
	 */
	Response dispatch(String httpMethod, String path, Function<String, List<String>> queryValues,
			Function<String, String> headerValue, InputStream entity) {
		// The path arrives without its leading slash from the annotated
		// methods and with one from anywhere else; the route rule
		// normalises either, so neither caller has to.
		Optional<RestRoute> route = RestRoute.match(flavor, httpMethod, path);
		if (route.isEmpty()) {
			return Response.status(404)
					.entity("no operation of this contract answers " + httpMethod + " " + path)
					.build();
		}
		RestOperationFlavor operationFlavor = route.get().operationFlavor();
		ServiceOperation operation = operationFlavor.getOperation();

		Object body;
		try {
			body = body(operationFlavor, entity);
		} catch (IOException | RuntimeException undecodable) {
			return Response.status(400)
					.entity("the request body is not readable as what the contract declares: "
							+ undecodable.getMessage())
					.build();
		}

		Map<String, Object> arguments = RestArguments.of(operationFlavor, route.get().pathVariables(),
				queryValues, headerValue, body);

		Optional<String> missing = missingRequired(operation, arguments);
		if (missing.isPresent()) {
			return Response.status(400).entity(missing.get() + " is required").build();
		}

		ServiceObjects<Object> serviceObjects = implementations.get();
		if (serviceObjects == null) {
			return Response.status(503).entity("no service implements " + contract + " here").build();
		}
		Object service = serviceObjects.getService();
		try {
			Object result = invoke(service, operation, arguments);
			return answer(operationFlavor, result);
		} catch (InvocationTargetException failure) {
			return failed(operationFlavor, failure.getCause());
		} catch (ReflectiveOperationException | RuntimeException failure) {
			return Response.serverError().entity(String.valueOf(failure.getMessage())).build();
		} finally {
			serviceObjects.ungetService(service);
		}
	}

	/**
	 * The method behind an operation: by name and by how many arguments the
	 * contract declares. The contract's parameter order is the signature's,
	 * which is what makes this work without parameter names in the bytecode.
	 */
	private static Object invoke(Object service, ServiceOperation operation, Map<String, Object> arguments)
			throws ReflectiveOperationException {
		List<Object> values = new ArrayList<>();
		for (Parameter parameter : operation.getParameters()) {
			values.add(arguments.get(parameter.getName()));
		}
		for (Method method : service.getClass().getMethods()) {
			if (method.getName().equals(operation.getName()) && method.getParameterCount() == values.size()) {
				return method.invoke(service, values.toArray());
			}
		}
		throw new NoSuchMethodException("the service behind this contract, a "
				+ service.getClass().getName() + ", has no method '" + operation.getName()
				+ "' taking " + values.size() + " argument(s)");
	}

	private static Optional<String> missingRequired(ServiceOperation operation, Map<String, Object> arguments) {
		for (Parameter parameter : operation.getParameters()) {
			if (!parameter.isOptional() && arguments.get(parameter.getName()) == null) {
				return Optional.of(parameter.getName());
			}
		}
		return Optional.empty();
	}

	/**
	 * The payload, as the parameter bound to {@code BODY} declares it.
	 *
	 * <p>Two kinds of body exist, and the contract says which: a
	 * parameter typed by an {@code EClass} carries a model, which is XMI
	 * on this wire; anything else carries a value, which is text and is
	 * converted like a value from any other place. An operation that
	 * binds nothing to the body ignores whatever was sent — the contract
	 * has no slot to put it in.
	 */
	private Object body(RestOperationFlavor operationFlavor, InputStream entity) throws IOException {
		Parameter bound = RestArguments.bodyParameter(operationFlavor);
		if (bound == null || entity == null) {
			return null;
		}
		if (bound.getEType() instanceof EClass) {
			return XmiCodec.read(entity, resourceSets);
		}
		String text = new String(entity.readAllBytes(), UTF_8);
		return text.isEmpty() ? null : text;
	}

	/**
	 * No result and an empty collection are both 204; the rest is the
	 * declared success code, in the first media type the flavor says it
	 * produces — that statement is the contract's, not this dispatcher's
	 * guess from the result's Java type.
	 */
	private static Response answer(RestOperationFlavor operationFlavor, Object result) {
		if (result == null || (result instanceof Collection<?> collection && collection.isEmpty())) {
			return Response.noContent().build();
		}
		// A failure that came back as a value rather than as a throw: the
		// body stays what the operation returned, only the status is the
		// transport's word on it.
		if (RestErrors.isFailure(result)) {
			return withMediaType(operationFlavor,
					Response.status(RestErrors.statusFor(operationFlavor, (Diagnostic) result)).entity(result));
		}
		int status = operationFlavor.getReturnCodes().isEmpty() ? 200 : operationFlavor.getReturnCodes().get(0);
		return withMediaType(operationFlavor, Response.status(status).entity(result));
	}

	private static Response withMediaType(RestOperationFlavor operationFlavor, Response.ResponseBuilder answer) {
		if (!operationFlavor.getProduces().isEmpty()) {
			return answer.type(MediaType.valueOf(operationFlavor.getProduces().get(0))).build();
		}
		return answer.build();
	}

	/**
	 * A declared error answers with the status its flavor binds to it. One
	 * the contract does not declare is a 500 — the provider failed, and
	 * saying anything more precise would be inventing it.
	 */
	private static Response failed(RestOperationFlavor operationFlavor, Throwable failure) {
		for (RestExceptionBinding binding : operationFlavor.getExceptionBindings()) {
			ServiceException declared = binding.getException();
			if (declared != null && declared.getType() != null && raises(failure, declared)) {
				return Response.status(binding.getStatus()).entity(String.valueOf(failure.getMessage())).build();
			}
		}
		return Response.serverError().entity(String.valueOf(failure.getMessage())).build();
	}

	/**
	 * Whether a failure is the declared error. The contract names its
	 * exceptions symbolically, so the match is by the simple name of the
	 * Java class — the language binding that would resolve it properly is
	 * build-time knowledge this component deliberately does not have.
	 */
	private static boolean raises(Throwable failure, ServiceException declared) {
		String symbolic = declared.getType();
		String simple = symbolic.contains(".") ? symbolic.substring(symbolic.lastIndexOf('.') + 1) : symbolic;
		for (Class<?> type = failure.getClass(); type != null; type = type.getSuperclass()) {
			if (type.getSimpleName().equals(simple) || type.getSimpleName().equals(simple + "Exception")) {
				return true;
			}
		}
		return false;
	}
}
