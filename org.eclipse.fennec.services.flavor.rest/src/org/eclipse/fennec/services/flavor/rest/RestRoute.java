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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceOperationFlavor;

/**
 * Which operation of a flavor serves a request, and what the path
 * template captured.
 *
 * <p>The counterpart of {@link RestPlacement}: a consumer builds a URL
 * from method and path, a provider reads the operation back out of one.
 * Both from the same flavor, so a route that a consumer can address is
 * one a provider answers.
 *
 * <p>More specific paths win. {@code /persons/{id}} and {@code /persons}
 * both match {@code GET /persons/42} in the sense that the first has a
 * variable where the second has nothing, so matching counts literal
 * segments rather than taking the first hit — otherwise the declaration
 * order in a document would decide what an endpoint does.
 */
public final class RestRoute {

	private static final Pattern TEMPLATE_VARIABLE = Pattern.compile("\\{([^}/]+)\\}");

	private final RestOperationFlavor operationFlavor;
	private final Map<String, String> pathVariables;

	private RestRoute(RestOperationFlavor operationFlavor, Map<String, String> pathVariables) {
		this.operationFlavor = operationFlavor;
		this.pathVariables = pathVariables;
	}

	public RestOperationFlavor operationFlavor() {
		return operationFlavor;
	}

	/** What the {@code {name}} segments of the matched path captured. */
	public Map<String, String> pathVariables() {
		return pathVariables;
	}

	/**
	 * The operation of this flavor that answers {@code httpMethod path},
	 * with the path taken relative to the flavor's own mount point.
	 */
	public static Optional<RestRoute> match(RestFlavor flavor, String httpMethod, String path) {
		if (flavor == null || httpMethod == null) {
			return Optional.empty();
		}
		String requested = normalise(path);
		RestRoute best = null;
		int bestLiterals = -1;
		for (ServiceOperationFlavor candidate : flavor.getOperationFlavors()) {
			if (!(candidate instanceof RestOperationFlavor rest) || !methodMatches(rest, httpMethod)) {
				continue;
			}
			Map<String, String> variables = capture(normalise(rest.getPath()), requested);
			if (variables == null) {
				continue;
			}
			int literals = literalSegments(normalise(rest.getPath()));
			if (literals > bestLiterals) {
				best = new RestRoute(rest, variables);
				bestLiterals = literals;
			}
		}
		return Optional.ofNullable(best);
	}

	private static boolean methodMatches(RestOperationFlavor operationFlavor, String httpMethod) {
		HttpMethod declared = operationFlavor.getMethod() != null ? operationFlavor.getMethod() : HttpMethod.GET;
		return declared.getLiteral().equalsIgnoreCase(httpMethod);
	}

	/** {@code null} when the template does not match, the captures otherwise. */
	private static Map<String, String> capture(String template, String requested) {
		List<String> names = new ArrayList<>();
		StringBuilder regex = new StringBuilder();
		Matcher variables = TEMPLATE_VARIABLE.matcher(template);
		int at = 0;
		while (variables.find()) {
			regex.append(Pattern.quote(template.substring(at, variables.start())));
			regex.append("([^/]+)");
			names.add(variables.group(1));
			at = variables.end();
		}
		regex.append(Pattern.quote(template.substring(at)));

		Matcher match = Pattern.compile(regex.toString()).matcher(requested);
		if (!match.matches()) {
			return null;
		}
		Map<String, String> captured = new LinkedHashMap<>();
		for (int i = 0; i < names.size(); i++) {
			captured.put(names.get(i), match.group(i + 1));
		}
		return captured;
	}

	private static int literalSegments(String template) {
		int literals = 0;
		for (String segment : template.split("/")) {
			if (!segment.isEmpty() && !segment.startsWith("{")) {
				literals++;
			}
		}
		return literals;
	}

	/** A path without its trailing slash, and never null — "" is the base itself. */
	private static String normalise(String path) {
		if (path == null || path.isBlank() || "/".equals(path)) {
			return "";
		}
		String trimmed = path.startsWith("/") ? path : "/" + path;
		return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}
}
