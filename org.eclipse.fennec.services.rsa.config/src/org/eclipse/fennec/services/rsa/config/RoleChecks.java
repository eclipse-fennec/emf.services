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

package org.eclipse.fennec.services.rsa.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What a role configuration has to say before anything comes up.
 *
 * <p>Every one of these used to surface much later and much further
 * away: a broker URL with a typo as a client that never connects, a
 * public URL on the wrong port as a consumer dialling an address nobody
 * serves, an unknown policy as a topology manager that quietly exports
 * nothing. Saying it here costs one message at activation.
 */
final class RoleChecks {

	private static final Set<String> POLICIES = Set.of("promiscuous", "manual");

	private RoleChecks() {
	}

	/** Everything wrong with these settings, in the order it was found. */
	static List<String> problems(RoleSettings settings, boolean serves) {
		List<String> problems = new ArrayList<>();
		checkUrl(problems, "broker.url", settings.brokerUrl());
		if (settings.registryName().isBlank()) {
			problems.add("registry.name is empty: it is what this node's services are registered under");
		}
		if (!POLICIES.contains(settings.exportPolicy())) {
			problems.add("policy is '" + settings.exportPolicy() + "', expected one of " + POLICIES);
		}
		if (!POLICIES.contains(settings.importPolicy())) {
			problems.add("import.policy is '" + settings.importPolicy() + "', expected one of " + POLICIES);
		}
		if (settings.flavor().isBlank()) {
			problems.add("flavor is empty: it names the RSA configuration type this node speaks");
		}
		if (serves) {
			if (settings.httpPort() < 0 || settings.httpPort() > 65535) {
				problems.add("http.port is " + settings.httpPort() + ", which is not a port");
			}
			checkUrl(problems, "the public URL", settings.effectivePublicUrl());
		}
		return List.copyOf(problems);
	}

	/**
	 * Where the derived public URL and the HTTP runtime this role
	 * configures disagree.
	 *
	 * <p>Not a problem in itself: a node behind a reverse proxy states a
	 * public URL that is deliberately different from what it binds. It is
	 * worth saying out loud, though, because the other reason for a
	 * mismatch is a typo, and that one is invisible until a consumer
	 * fails to reach the endpoint.
	 */
	static List<String> mismatches(RoleSettings settings) {
		if (!settings.manageHttp() || settings.publicUrl().isBlank()) {
			return List.of();
		}
		List<String> notes = new ArrayList<>();
		try {
			URI stated = new URI(settings.effectivePublicUrl());
			if (stated.getPort() != settings.httpPort()) {
				notes.add("the public URL says port " + stated.getPort() + " but this node binds "
						+ settings.httpPort());
			}
			String context = settings.effectiveContextPath();
			String path = stated.getPath() == null ? "" : stated.getPath().replaceAll("^/+", "");
			if (!path.equals(context)) {
				notes.add("the public URL's path is '" + path + "' but the context path is '" + context + "'");
			}
		} catch (URISyntaxException unreadable) {
			// The URL itself is reported by problems(); nothing to compare.
			return List.of();
		}
		return List.copyOf(notes);
	}

	private static void checkUrl(List<String> problems, String what, String value) {
		if (value == null || value.isBlank()) {
			problems.add(what + " is empty");
			return;
		}
		try {
			URI uri = new URI(value);
			if (!uri.isAbsolute() || uri.getHost() == null) {
				problems.add(what + " is '" + value + "', which is not an absolute http(s) URL");
				return;
			}
			String scheme = uri.getScheme().toLowerCase();
			if (!"http".equals(scheme) && !"https".equals(scheme)) {
				problems.add(what + " uses scheme '" + scheme + "', expected http or https");
			}
		} catch (URISyntaxException unreadable) {
			problems.add(what + " is '" + value + "', which is not a URL: " + unreadable.getReason());
		}
	}
}
