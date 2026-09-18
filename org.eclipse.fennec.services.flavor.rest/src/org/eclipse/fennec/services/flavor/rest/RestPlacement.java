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

package org.eclipse.fennec.services.flavor.rest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.RestParameterBinding;

/**
 * Where the arguments of one call travel, as the operation's flavor
 * declares it: {@code PATH} into a template segment, {@code QUERY} into
 * the query string, {@code HEADER} into a request header, {@code BODY}
 * into the payload — each under its {@code wireName} where one is given.
 *
 * <p>This is the rule both ends of a REST distribution obey, and the
 * reason it lives in one place: a consumer puts values where it says and
 * a provider reads them from there, so a second reading of the same
 * flavor would be a silent disagreement about the wire. The broker never
 * reads a flavor at all — a provider declares its own transport and a
 * consumer picks one it can speak.
 *
 * <p>An argument whose parameter carries <em>no</em> binding keeps the
 * convention the SDKs used before the bindings were driven: a single
 * {@link EObject} is the payload, anything else a query parameter. The
 * model names {@code BODY} as the default, but there is no encoding for
 * several primitive arguments in one payload, so an undeclared parameter
 * travels the way it always has until that wire shape exists.
 */
public final class RestPlacement {

	private final Map<String, Object> path = new LinkedHashMap<>();
	private final Map<String, Object> query = new LinkedHashMap<>();
	private final Map<String, Object> header = new LinkedHashMap<>();
	private final Map<String, Object> declaredBody = new LinkedHashMap<>();
	private final Map<String, Object> undeclared = new LinkedHashMap<>();

	private RestPlacement() {
	}

	/**
	 * Sort the arguments of one call, keyed by their model parameter name,
	 * into the places the flavor declares for them.
	 */
	public static RestPlacement of(RestOperationFlavor operationFlavor, Map<String, Object> arguments) {
		RestPlacement placement = new RestPlacement();
		if (operationFlavor == null || arguments == null) {
			return placement;
		}
		for (Map.Entry<String, Object> argument : arguments.entrySet()) {
			RestParameterBinding binding = bindingFor(operationFlavor, argument.getKey());
			if (binding == null) {
				placement.undeclared.put(argument.getKey(), argument.getValue());
				continue;
			}
			String wireName = wireNameOf(binding, argument.getKey());
			ParameterBinding where = binding.getBinding() != null ? binding.getBinding() : ParameterBinding.BODY;
			switch (where) {
			case PATH -> placement.path.put(wireName, argument.getValue());
			case QUERY -> placement.query.put(wireName, argument.getValue());
			case HEADER -> placement.header.put(wireName, argument.getValue());
			case BODY -> placement.declaredBody.put(wireName, argument.getValue());
			}
		}
		return placement;
	}

	/** Values for the {@code {name}} segments of the operation's path. */
	public Map<String, Object> path() {
		return Collections.unmodifiableMap(path);
	}

	/** Request headers, keyed by their wire name. */
	public Map<String, Object> header() {
		return Collections.unmodifiableMap(header);
	}

	/**
	 * Everything that goes into the query string: what the flavor bound
	 * there, plus the undeclared arguments that did not become the body.
	 */
	public Map<String, Object> query() {
		Map<String, Object> all = new LinkedHashMap<>(query);
		if (body().isEmpty()) {
			all.putAll(undeclared);
		}
		return Collections.unmodifiableMap(all);
	}

	/**
	 * The payload, or empty for a call without one. A declared BODY
	 * argument wins; otherwise a single undeclared {@link EObject} is the
	 * body, as it always was.
	 *
	 * @throws IllegalStateException if the flavor binds several arguments
	 *         to the body that are not a single EObject — there is no wire
	 *         encoding for that yet, and sending something else silently
	 *         would be worse than saying so
	 */
	public Optional<Object> body() {
		Map<String, Object> candidates = declaredBody.isEmpty() ? undeclared : declaredBody;
		if (candidates.size() == 1) {
			Object only = candidates.values().iterator().next();
			if (only instanceof EObject) {
				return Optional.of(only);
			}
		}
		if (!declaredBody.isEmpty()) {
			throw new IllegalStateException("operation declares " + declaredBody.size()
					+ " BODY parameter(s) that are not a single EObject — there is no wire encoding"
					+ " for that yet; bind them as QUERY, HEADER or PATH");
		}
		return Optional.empty();
	}

	/**
	 * The parameter a binding refers to. It reaches a consumer as an
	 * unresolved proxy: the flavor travels in the lookup envelope while the
	 * contract stays behind its catalog URL, so the reference is a
	 * cross-document one and its fragment is positional
	 * ({@code …#//@operations.N/@parameters.M}). Resolving it by position
	 * against the operation the flavor already points at keeps the binding
	 * readable without fetching the contract — the same positional rule the
	 * rest of this wire format uses.
	 */
	public static Parameter boundParameter(RestOperationFlavor operationFlavor, RestParameterBinding binding) {
		Parameter bound = binding.getParameter();
		if (bound == null || !bound.eIsProxy()) {
			return bound;
		}
		URI proxyURI = ((InternalEObject) bound).eProxyURI();
		if (proxyURI == null || operationFlavor.getOperation() == null) {
			return bound;
		}
		int index = positionalIndex(proxyURI.fragment());
		List<Parameter> declared = operationFlavor.getOperation().getParameters();
		return index >= 0 && index < declared.size() ? declared.get(index) : bound;
	}

	/** The name a parameter travels under: its binding's, or its own. */
	public static String wireNameOf(RestParameterBinding binding, String parameterName) {
		return binding.getWireName() != null && !binding.getWireName().isBlank()
				? binding.getWireName()
				: parameterName;
	}

	/** The binding for a parameter of this operation, or {@code null}. */
	public static RestParameterBinding bindingFor(RestOperationFlavor operationFlavor, String parameterName) {
		for (RestParameterBinding binding : operationFlavor.getParameterBindings()) {
			Parameter bound = boundParameter(operationFlavor, binding);
			if (bound != null && parameterName.equals(bound.getName())) {
				return binding;
			}
		}
		return null;
	}

	/** The {@code N} of a trailing {@code @parameters.N} in a URI fragment, or -1. */
	private static int positionalIndex(String fragment) {
		if (fragment == null) {
			return -1;
		}
		int at = fragment.lastIndexOf("@parameters.");
		if (at < 0) {
			return -1;
		}
		try {
			return Integer.parseInt(fragment.substring(at + "@parameters.".length()));
		} catch (NumberFormatException notPositional) {
			return -1;
		}
	}
}
