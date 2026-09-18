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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.RestParameterBinding;

/**
 * The arguments of one call, read back out of a request the way the
 * flavor says they were put in.
 *
 * <p>The mirror of {@link RestPlacement}: that one decides where a value
 * goes, this one looks for it there. Both read the same bindings, so a
 * provider cannot develop its own idea of where a consumer put something.
 *
 * <p>Values arrive as text. What they become is the contract's business:
 * {@code eType} names a metamodel type and an Ecore {@code EDataType}
 * knows how to parse its own literals; otherwise the language-neutral
 * {@code type} decides. A parameter that is absent falls back to its
 * {@code defaultValue}, and to {@code null} without one — an absent
 * required argument is not rejected here, because what to answer with is
 * the transport's decision, not this rule's.
 */
public final class RestArguments {

	private RestArguments() {
	}

	/**
	 * Read the arguments of an operation, keyed by model parameter name.
	 *
	 * @param pathVariables what the path template captured
	 * @param queryValues   all values of a query parameter, by wire name
	 * @param headerValue   a request header, by wire name
	 * @param body          the decoded payload, or {@code null}
	 */
	public static Map<String, Object> of(RestOperationFlavor operationFlavor,
			Map<String, String> pathVariables,
			Function<String, List<String>> queryValues,
			Function<String, String> headerValue,
			Object body) {

		Map<String, Object> arguments = new LinkedHashMap<>();
		if (operationFlavor == null || operationFlavor.getOperation() == null) {
			return arguments;
		}
		for (Parameter parameter : operationFlavor.getOperation().getParameters()) {
			RestParameterBinding binding = RestPlacement.bindingFor(operationFlavor, parameter.getName());
			ParameterBinding where = binding == null || binding.getBinding() == null
					? ParameterBinding.QUERY
					: binding.getBinding();
			String wireName = binding == null
					? parameter.getName()
					: RestPlacement.wireNameOf(binding, parameter.getName());

			arguments.put(parameter.getName(), switch (where) {
				case PATH -> single(parameter, pathVariables.get(wireName));
				case HEADER -> single(parameter, headerValue.apply(wireName));
				case BODY -> body;
				case QUERY -> fromQuery(parameter, queryValues.apply(wireName));
			});
		}
		return arguments;
	}

	/**
	 * An undeclared parameter is read from the query — the convention the
	 * SDKs used before the bindings were driven, and the one
	 * {@link RestPlacement} still writes with.
	 */
	private static Object fromQuery(Parameter parameter, List<String> values) {
		if (parameter.getUpperBound() != 1) {
			List<Object> all = new ArrayList<>();
			if (values != null) {
				for (String value : values) {
					all.add(convert(parameter, value));
				}
			}
			return all;
		}
		return single(parameter, values == null || values.isEmpty() ? null : values.get(0));
	}

	private static Object single(Parameter parameter, String raw) {
		if (raw == null) {
			return parameter.getDefaultValue() != null ? convert(parameter, parameter.getDefaultValue()) : null;
		}
		return convert(parameter, raw);
	}

	/**
	 * Text to the type the contract declares. An Ecore data type parses
	 * its own literals, which is also what keeps an enum or a date
	 * readable without a table here.
	 */
	static Object convert(Parameter parameter, String raw) {
		EClassifier eType = parameter.getEType();
		if (eType instanceof EDataType dataType && !dataType.eIsProxy()) {
			return dataType.getEPackage().getEFactoryInstance().createFromString(dataType, raw);
		}
		String type = parameter.getType();
		if (type == null) {
			return raw;
		}
		return switch (type) {
			case "int" -> Integer.valueOf(raw);
			case "long" -> Long.valueOf(raw);
			case "short" -> Short.valueOf(raw);
			case "byte" -> Byte.valueOf(raw);
			case "float" -> Float.valueOf(raw);
			case "double" -> Double.valueOf(raw);
			case "boolean" -> Boolean.valueOf(raw);
			default -> raw;
		};
	}
}
