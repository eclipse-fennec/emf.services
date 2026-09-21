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

package org.eclipse.fennec.services.common;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Where a call came from, as far as the registry is entitled to know.
 *
 * <p>The broker could not say who published, withdrew or looked up
 * anything (#125). For a statement about how information flows between
 * systems — what GDPR Art. 30 and ISO 27001 A.8 ask for, and what an
 * OpenTelemetry span would carry — "anonymous" is not an answer.
 *
 * <p>An origin is two things and deliberately not a third:
 *
 * <ul>
 * <li>a <b>label</b>, configured per deployment, that says which system
 *     this is — {@code payments-prod-eu}, not a machine;
 * <li>a <b>runtime id</b>, which is the OSGi framework UUID where there
 *     is one and a per-JVM UUID where there is not. It is new on every
 *     start, which is the point: an origin explains where a session came
 *     from, it is not a durable identity;
 * <li>and <b>not</b> a host name or an IP address. In some deployments
 *     those are personal data, and the question here is which system
 *     told which system what, not which person sat behind it.
 * </ul>
 *
 * <p>RSA already had the right answer for its half of the world in
 * {@code endpoint.framework.uuid}; this is the same idea for the broker
 * side, which is why the runtime id is the framework UUID whenever the
 * client runs in a framework at all.
 *
 * <p>On the wire it is one token, {@code label/runtimeId}, because it
 * travels in a header and headers are strings. Both halves are kept
 * free of {@code /} so the token always splits at the first and only
 * separator.
 */
public final class ClientOrigin {

	private static final Logger LOG = Logger.getLogger(ClientOrigin.class.getName());

	/**
	 * The header that carries it over REST. Additive, so it is not one
	 * of the frozen wire names of #4 — but it is spelled like them, so
	 * a rename sweep finds it with its siblings.
	 */
	public static final String HEADER = "X-DDSR-Origin";

	/**
	 * The same identity as a span attribute (#126).
	 *
	 * <p>On both halves of a call it names the side that <em>made</em>
	 * it: this runtime on a client span, the caller on a server span.
	 * That is what turns a trace from "what happened" into "which
	 * system told which system what".
	 */
	public static final String ATTRIBUTE = "fennec.origin";

	/** The OSGi framework property that names the framework instance. */
	public static final String FRAMEWORK_UUID = "org.osgi.framework.uuid";

	/** What a client without a configured label calls itself. */
	public static final String UNNAMED = "unnamed";

	/**
	 * What the broker records for a call that carried no origin, and
	 * what the catalog's {@code requestor} has always defaulted to.
	 */
	public static final String ANONYMOUS = "anonymous";

	private static final char SEPARATOR = '/';

	/** One id per JVM, for clients that do not run in a framework. */
	private static final String JVM_RUNTIME_ID = UUID.randomUUID().toString();

	private final String label;

	private final String runtimeId;

	private ClientOrigin(String label, String runtimeId) {
		this.label = label;
		this.runtimeId = runtimeId;
	}

	/**
	 * The origin of this runtime.
	 *
	 * @param label      the configured deployment label; blank or
	 *                   {@code null} becomes {@link #UNNAMED}, because a
	 *                   client that forgot to name itself should still be
	 *                   distinguishable from one that is not there
	 * @param frameworkUuid the framework UUID, or {@code null} outside a
	 *                   framework — then one id per JVM is used, stable
	 *                   for as long as the JVM lives
	 */
	public static ClientOrigin of(String label, String frameworkUuid) {
		String runtimeId = frameworkUuid == null || frameworkUuid.isBlank()
				? JVM_RUNTIME_ID
				: sanitize(frameworkUuid);
		return new ClientOrigin(sanitize(label == null || label.isBlank() ? UNNAMED : label), runtimeId);
	}

	/**
	 * Reads an origin token as it arrived.
	 *
	 * <p>Forgiving on purpose, like {@link Flavors#parse}: a token the
	 * broker cannot make sense of is recorded as a label with no runtime
	 * id rather than refused. Losing the audit detail of one call is
	 * recoverable; refusing the call is not, and a registry that rejects
	 * work over a malformed audit header is worse than one that writes
	 * down what it got.
	 *
	 * @param token the header value, possibly {@code null} or blank
	 * @return the origin, or {@code null} when nothing was sent — which
	 *         the caller reads as {@link #ANONYMOUS}
	 */
	public static ClientOrigin parse(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}
		String trimmed = token.trim();
		int separator = trimmed.indexOf(SEPARATOR);
		if (separator < 0) {
			LOG.fine(() -> "[DDSR] origin '" + trimmed + "' carries no runtime id");
			return new ClientOrigin(trimmed, "");
		}
		return new ClientOrigin(trimmed.substring(0, separator), trimmed.substring(separator + 1));
	}

	/** Which system this is, as the deployment named it. */
	public String label() {
		return label;
	}

	/**
	 * Which run of it: the framework UUID, or the JVM's id outside a
	 * framework. Empty when the sender only gave a label.
	 */
	public String runtimeId() {
		return runtimeId;
	}

	/** The token as it travels: {@code label/runtimeId}. */
	public String token() {
		return runtimeId.isEmpty() ? label : label + SEPARATOR + runtimeId;
	}

	@Override
	public String toString() {
		return token();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClientOrigin o
				&& label.equals(o.label) && runtimeId.equals(o.runtimeId);
	}

	@Override
	public int hashCode() {
		return 31 * label.hashCode() + runtimeId.hashCode();
	}

	/**
	 * Keeps a half of the token free of the separator and of anything a
	 * header cannot carry. A label is configuration and a runtime id is
	 * a UUID, so this normally changes nothing; it exists so that a
	 * careless label cannot make the token unparseable for everyone who
	 * reads it afterwards.
	 */
	private static String sanitize(String value) {
		StringBuilder clean = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			clean.append(c == SEPARATOR || c < ' ' || c == ',' ? '_' : c);
		}
		return clean.toString();
	}
}
