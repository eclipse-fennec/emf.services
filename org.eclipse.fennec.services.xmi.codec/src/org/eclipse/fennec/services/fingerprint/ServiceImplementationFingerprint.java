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
package org.eclipse.fennec.services.fingerprint;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttOperationFlavor;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperationFlavor;

/**
 * Content-based fingerprint of a {@link ServiceImplementation} — scheme
 * tag {@code im1} (ACQUISITION.md §11.1). Answers a provider's
 * reconnect question — <em>"does the broker still hold my registration,
 * unchanged?"</em> — with a string comparison, and makes the check
 * three-valued together with sd1: {@code im1} equal → renew the lease
 * only; {@code im1} different but all {@code sd1} equal → endpoint or
 * property drift → re-publish; an {@code sd1} different → contract
 * drift, a policy question rather than a plain re-publish.
 * <p>
 * <b>Composition, not re-traversal:</b> the contract part folds the
 * {@link ServiceDescriptionFingerprint sd1} values of the referenced
 * interfaces in as opaque tokens (the Merkle principle, exactly the
 * {@code derivationInputs} pattern of Fennec emf.osgi). Consequence:
 * an im1 comparison between two parties is only meaningful when their
 * sd1 components agree — which the three-valued check ensures.
 *
 * <h2>Canonical form (frozen with the tag {@code im1})</h2>
 *
 * <pre>
 * IM|&lt;implementationId&gt;|name=&lt;name&gt;|version=&lt;version&gt;
 *   c|&lt;sd1-value&gt;                                    contracts, DECLARED order of serviceInterfaces
 *   F|&lt;eClass&gt;|&lt;name&gt;|kind=&lt;literal&gt;&lt;type-specific fields&gt;   flavors, DECLARED order
 *     o|&lt;eClass&gt;|&lt;name&gt;|operation=&lt;opName&gt;|consumes=&lt;list&gt;|produces=&lt;list&gt;&lt;type-specific fields&gt;
 *   pr|&lt;tag&gt;|&lt;name&gt;|value=&lt;value&gt;                    properties, sorted by name (sd1 rules)
 * </pre>
 *
 * Type-specific fields:
 * <ul>
 *   <li>{@code RestFlavor}: {@code |host=|basePath=|contentTypes=<list>}</li>
 *   <li>{@code MqttFlavor}: {@code |brokers=<list>|requestTopic=|responseTopic=|defaultQos=<literal>|defaultRetained=<bool>}</li>
 *   <li>{@code RestOperationFlavor}: {@code |method=<literal>|path=|returnCodes=<list>}</li>
 *   <li>{@code MqttOperationFlavor}: {@code |requestTopic=|responseTopic=|qos=<literal>|retained=<bool>|correlation=<bool>|returnPath=<literal>}</li>
 *   <li>an unknown flavor/operation-flavor subclass renders only its
 *       eClass name — conservative: visible, the hash moves.</li>
 * </ul>
 *
 * Shared sd1 rules apply: 2/4-space indentation, lines joined with
 * {@code \n} and no trailing newline; escaping {@code \ | LF CR} in
 * every text field; {@code <list>} renders as
 * {@code <count>:<e1,e2,…>} with {@code ,} additionally escaped; null
 * and the empty string both render empty; enum values render as their
 * literal, explicitly (EMF omits wire defaults, the canonical form does
 * not). Excluded: {@code description} (doc text) and
 * {@code componentDescription} (deployment detail, not endpoint
 * identity). The value is {@code "im1:" + sha256-hex} — the tag freezes
 * the algorithm; changes mean a new tag.
 */
public final class ServiceImplementationFingerprint {

	/** Scheme tag, prefixed to every fingerprint value. */
	public static final String SCHEME = "im1";

	private ServiceImplementationFingerprint() {
	}

	/**
	 * Fingerprint of the given implementation:
	 * {@code im1:<sha256-hex of the canonical form>}, or {@code null}
	 * for a {@code null} implementation.
	 */
	public static String fingerprint(ServiceImplementation implementation) {
		if (implementation == null) {
			return null;
		}
		return SCHEME + ":" + ServiceDescriptionFingerprint.sha256Hex(canonicalForm(implementation));
	}

	/** The canonical text form — exposed for diagnosis and the cross-language golden test. */
	public static String canonicalForm(ServiceImplementation implementation) {
		if (implementation == null) {
			return null;
		}
		List<String> lines = new ArrayList<>();
		lines.add("IM|" + esc(implementation.getImplementationId())
				+ "|name=" + esc(implementation.getName())
				+ "|version=" + esc(implementation.getVersion()));

		for (ServiceInterface si : implementation.getServiceInterfaces()) {
			lines.add("  c|" + ServiceDescriptionFingerprint.fingerprint(si));
		}

		for (ServiceFlavor flavor : implementation.getFlavors()) {
			lines.add("  " + flavorLine(flavor));
			for (ServiceOperationFlavor operationFlavor : flavor.getOperationFlavors()) {
				lines.add("    " + operationFlavorLine(operationFlavor));
			}
		}

		for (Property property : ServiceDescriptionFingerprint.sortedByName(implementation.getProperties())) {
			lines.add("  " + ServiceDescriptionFingerprint.propertyLine(property));
		}
		return String.join("\n", lines);
	}

	// ------------------------------------------------------------------

	private static String flavorLine(ServiceFlavor flavor) {
		StringBuilder line = new StringBuilder("F|")
				.append(flavor.eClass().getName())
				.append('|').append(esc(flavor.getName()))
				.append("|kind=").append(flavor.getKind() != null ? flavor.getKind().getLiteral() : "");
		if (flavor instanceof RestFlavor rest) {
			line.append("|host=").append(esc(rest.getHost()))
					.append("|basePath=").append(esc(rest.getBasePath()))
					.append("|contentTypes=").append(list(rest.getContentTypes()));
		} else if (flavor instanceof MqttFlavor mqtt) {
			line.append("|brokers=").append(list(mqtt.getBrokers()))
					.append("|requestTopic=").append(esc(mqtt.getRequestTopic()))
					.append("|responseTopic=").append(esc(mqtt.getResponseTopic()))
					.append("|defaultQos=").append(mqtt.getDefaultQos() != null ? mqtt.getDefaultQos().getLiteral() : "")
					.append("|defaultRetained=").append(mqtt.isDefaultRetained());
		}
		return line.toString();
	}

	private static String operationFlavorLine(ServiceOperationFlavor operationFlavor) {
		StringBuilder line = new StringBuilder("o|")
				.append(operationFlavor.eClass().getName())
				.append('|').append(esc(operationFlavor.getName()))
				.append("|operation=").append(esc(operationFlavor.getOperation() != null
						? operationFlavor.getOperation().getName()
						: null))
				.append("|consumes=").append(list(operationFlavor.getConsumes()))
				.append("|produces=").append(list(operationFlavor.getProduces()));
		if (operationFlavor instanceof RestOperationFlavor rest) {
			line.append("|method=").append(rest.getMethod() != null ? rest.getMethod().getLiteral() : "")
					.append("|path=").append(esc(rest.getPath()))
					.append("|returnCodes=").append(list(rest.getReturnCodes()));
		} else if (operationFlavor instanceof MqttOperationFlavor mqtt) {
			line.append("|requestTopic=").append(esc(mqtt.getRequestTopic()))
					.append("|responseTopic=").append(esc(mqtt.getResponseTopic()))
					.append("|qos=").append(mqtt.getQos() != null ? mqtt.getQos().getLiteral() : "")
					.append("|retained=").append(mqtt.isRetained())
					.append("|correlation=").append(mqtt.isCorrelation())
					.append("|returnPath=").append(literal(mqtt.getReturnPath()));
		}
		return line.toString();
	}

	/** Enum/object literal, empty for null (never the string "null"). */
	private static String literal(Object value) {
		return value == null ? "" : esc(String.valueOf(value));
	}

	/** {@code <count>:<e1,e2,…>} — the sd1 StringList rule. */
	private static String list(List<?> values) {
		StringBuilder joined = new StringBuilder();
		int count = 0;
		if (values != null) {
			for (Object value : values) {
				if (joined.length() > 0) {
					joined.append(',');
				}
				joined.append(ServiceDescriptionFingerprint.escListElement(String.valueOf(value)));
				count++;
			}
		}
		return count + ":" + joined;
	}

	private static String esc(String raw) {
		return ServiceDescriptionFingerprint.esc(raw);
	}
}
