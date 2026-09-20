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

/**
 * What a node states about itself, as a value a test can build
 * without a running framework.
 *
 * <p>Everything a deployment has to decide is here, and everything that
 * follows from a decision is not: the public URL derives from the port
 * and the context path unless something states it, the discovery's
 * broker URL is the client's broker URL because discovery talks to the
 * broker through that very client, and the model registry's name is
 * boilerplate that cannot be anything else.
 *
 * @param brokerUrl where the DDSR broker is
 * @param publicUrl the address consumers should dial, or empty to derive it
 * @param publicHost the host to derive the public URL from, or empty for localhost
 * @param httpPort the port this node serves on
 * @param httpHost the interface to bind, usually {@code 0.0.0.0}
 * @param contextPath the servlet context, without slashes
 * @param manageHttp whether this node writes the HTTP and whiteboard configurations
 * @param httpId the id that ties the whiteboard to the HTTP runtime
 * @param registryName the name of this node's local service registry
 * @param defaultVersion the version a contract gets when it names none
 * @param flavor the RSA configuration type, e.g. {@code fennec.rest}
 * @param exportPolicy the topology manager's export policy
 * @param importPolicy the topology manager's import policy
 * @param heartbeatSeconds how often a provider says it is still there
 * @param sessionIntervalSeconds the consumer session interval, 0 to switch it off
 * @param consumerId how this node names itself to the broker
 */
public record RsaSettings(
		String brokerUrl,
		String publicUrl,
		String publicHost,
		int httpPort,
		String httpHost,
		String contextPath,
		boolean manageHttp,
		String httpId,
		String registryName,
		String defaultVersion,
		String flavor,
		String exportPolicy,
		String importPolicy,
		long heartbeatSeconds,
		long sessionIntervalSeconds,
		String consumerId) {

	/**
	 * The address this node tells consumers to dial.
	 *
	 * <p>Derived from the very values that configure the HTTP runtime, so
	 * the two cannot disagree by construction. A deployment behind a
	 * reverse proxy states {@link #publicUrl()} instead, which is the one
	 * case where they legitimately differ.
	 */
	public String effectivePublicUrl() {
		if (!publicUrl.isBlank()) {
			return trimTrailingSlash(publicUrl.strip());
		}
		String host = publicHost.isBlank() ? "localhost" : publicHost.strip();
		String context = trimSlashes(contextPath);
		return "http://" + host + ":" + httpPort + (context.isEmpty() ? "" : "/" + context);
	}

	/** The context path as the HTTP runtime wants it: no slashes around it. */
	public String effectiveContextPath() {
		return trimSlashes(contextPath);
	}

	private static String trimSlashes(String value) {
		String trimmed = value == null ? "" : value.strip();
		while (trimmed.startsWith("/")) {
			trimmed = trimmed.substring(1);
		}
		return trimTrailingSlash(trimmed);
	}

	private static String trimTrailingSlash(String value) {
		String trimmed = value;
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}
}
