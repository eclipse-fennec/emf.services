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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.fennec.services.BoolProperty;
import org.eclipse.fennec.services.CatalogStatus;
import org.eclipse.fennec.services.DoubleProperty;
import org.eclipse.fennec.services.FloatProperty;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.LongProperty;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ShortProperty;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringProperty;

/**
 * Content-based fingerprint of a {@link ServiceInterface} — scheme tag
 * {@code sd1}. Producer, broker and consumer each hold their own copy of
 * a service description; comparing the fingerprints answers "do we see
 * the same contract?" with a string comparison, across language borders.
 * <p>
 * Modelled on the {@code fp1} scheme of Fennec emf.osgi: the hash is
 * computed over a canonical, line-oriented text form produced by
 * traversing the <em>in-memory model</em> — never over serialized XMI
 * bytes, so serialization order, whitespace, XMI ids and href styles can
 * never move the hash.
 *
 * <h2>Canonical form (frozen with the tag {@code sd1})</h2>
 *
 * One line per element, fields separated by {@code |}, nesting expressed
 * by 2/4-space indentation, lines joined with {@code \n}, no trailing
 * newline:
 *
 * <pre>
 * I|&lt;name&gt;|version=&lt;version&gt;|status=&lt;ACTIVE|DEPRECATED&gt;
 *   X|&lt;name&gt;|type=&lt;type&gt;|version=&lt;version&gt;          interface exceptions, sorted by name
 *     pr|&lt;tag&gt;|&lt;name&gt;|value=&lt;value&gt;                 exception payload properties, sorted by name
 *   O|&lt;name&gt;|returnType=&lt;returnType&gt;                 operations, DECLARED order
 *     p|&lt;name&gt;|type=&lt;type&gt;|index=&lt;index&gt;|optional=&lt;true|false&gt;|defaultValue=&lt;default&gt;
 *     x|&lt;name&gt;                                       referenced exceptions, sorted by name
 * </pre>
 *
 * Rules, all part of the frozen scheme:
 * <ul>
 *   <li><b>Operations, parameters keep their declared order</b> —
 *       positional URI fragments ({@code //@operations.N}) make order
 *       part of the contract. Exceptions and properties are sorted by
 *       name (their order carries no meaning).</li>
 *   <li><b>Excluded:</b> all {@code description} attributes (doc text,
 *       like fp1 drops the {@code documentation} GenModel key),
 *       {@code deprecationReason} and {@code replacedBy} (catalog
 *       state, not contract), invariants / pre- / postconditions and
 *       parameter constraints (the constraint layer is unimplemented;
 *       adding it later means a new scheme tag), XMI ids,
 *       {@code ServiceReference} ids, object identity.</li>
 *   <li><b>Null values render as an empty field</b> ({@code type=}).
 *       An empty string renders the same way; sd1 accepts that
 *       collision.</li>
 *   <li><b>Escaping</b> in every text field: {@code \} → {@code \\},
 *       {@code |} → {@code \|}, LF → {@code \n} (two chars), CR →
 *       {@code \r}.</li>
 *   <li><b>Property tags:</b> {@code S}=String, {@code i}=Int,
 *       {@code l}=Long, {@code d}=Double, {@code f}=Float,
 *       {@code s}=Short, {@code b}=Bool, {@code SL}=StringList.</li>
 *   <li><b>Numeric property values:</b> int/long/short render as
 *       decimal; bool as {@code true}/{@code false}; double and float
 *       render as their IEEE-754 bit pattern in lowercase hex
 *       ({@code bits:&lt;16 hex&gt;} / {@code bits:&lt;8 hex&gt;}) — decimal
 *       rendering of floating point differs between languages, the bit
 *       pattern does not.</li>
 *   <li><b>StringList values</b> render as
 *       {@code &lt;count&gt;:&lt;e1,e2,…&gt;} with {@code ,} additionally
 *       escaped as {@code \,} inside elements. The count prefix keeps
 *       an empty list distinguishable from a single empty element.</li>
 *   <li><b>status</b> always renders explicitly (EMF omits the default
 *       {@code ACTIVE} on the wire; the canonical form does not).</li>
 * </ul>
 *
 * The fingerprint value is {@code "sd1:" + sha256-hex(utf8(canonicalForm))}
 * — lowercase hex, 68 chars total. The tag versions the algorithm; a
 * published scheme is frozen, changes mean a new tag. Two values with
 * different tags are not comparable.
 * <p>
 * Conservative direction: an unresolved detail (e.g. a proxy interface)
 * may make two logically equal descriptions hash differently, never the
 * other way round.
 */
public final class ServiceDescriptionFingerprint {

	/** Scheme tag, prefixed to every fingerprint value. */
	public static final String SCHEME = "sd1";

	private ServiceDescriptionFingerprint() {
	}

	/**
	 * Fingerprint of the given description:
	 * {@code sd1:<sha256-hex of the canonical form>}, or {@code null}
	 * for a {@code null} interface.
	 */
	public static String fingerprint(ServiceInterface serviceInterface) {
		if (serviceInterface == null) {
			return null;
		}
		return SCHEME + ":" + sha256Hex(canonicalForm(serviceInterface));
	}

	/**
	 * The canonical text form the hash is computed over. Exposed so a
	 * mismatch can be diagnosed line by line, and so the cross-language
	 * golden test can pin the exact form, not only the hash.
	 */
	public static String canonicalForm(ServiceInterface serviceInterface) {
		if (serviceInterface == null) {
			return null;
		}
		List<String> lines = new ArrayList<>();
		CatalogStatus status = serviceInterface.getStatus();
		lines.add("I|" + esc(serviceInterface.getName())
				+ "|version=" + esc(serviceInterface.getVersion())
				+ "|status=" + (status != null ? status.getLiteral() : CatalogStatus.ACTIVE.getLiteral()));

		List<ServiceException> exceptions = new ArrayList<>(serviceInterface.getExceptions());
		exceptions.sort(Comparator.comparing(ServiceException::getName,
				Comparator.nullsFirst(Comparator.naturalOrder())));
		for (ServiceException ex : exceptions) {
			lines.add("  X|" + esc(ex.getName())
					+ "|type=" + esc(ex.getType())
					+ "|version=" + esc(ex.getVersion()));
			for (Property property : sortedByName(ex.getProperties())) {
				lines.add("    " + propertyLine(property));
			}
		}

		for (ServiceOperation op : serviceInterface.getOperations()) {
			lines.add("  O|" + esc(op.getName()) + "|returnType=" + esc(op.getReturnType()));
			for (Parameter p : op.getParameters()) {
				lines.add("    p|" + esc(p.getName())
						+ "|type=" + esc(p.getType())
						+ "|index=" + p.getIndex()
						+ "|optional=" + p.isOptional()
						+ "|defaultValue=" + esc(p.getDefaultValue()));
			}
			List<ServiceException> raised = new ArrayList<>(op.getExceptions());
			raised.sort(Comparator.comparing(ServiceException::getName,
					Comparator.nullsFirst(Comparator.naturalOrder())));
			for (ServiceException ex : raised) {
				lines.add("    x|" + esc(ex.getName()));
			}
		}
		return String.join("\n", lines);
	}

	// ------------------------------------------------------------------

	private static List<Property> sortedByName(List<Property> properties) {
		List<Property> sorted = new ArrayList<>(properties);
		sorted.sort(Comparator.comparing(Property::getName,
				Comparator.nullsFirst(Comparator.naturalOrder())));
		return sorted;
	}

	/**
	 * {@code pr|<tag>|<name>|value=<value>} — the shared property
	 * rendering. Also used to fold property values into other canonical
	 * contexts, so the value rules live in exactly one place.
	 */
	private static String propertyLine(Property property) {
		String tag;
		String value;
		if (property instanceof StringProperty p) {
			tag = "S";
			value = esc(p.getValue());
		} else if (property instanceof IntProperty p) {
			tag = "i";
			value = Integer.toString(p.getValue());
		} else if (property instanceof LongProperty p) {
			tag = "l";
			value = Long.toString(p.getValue());
		} else if (property instanceof DoubleProperty p) {
			tag = "d";
			value = "bits:" + String.format("%016x", Double.doubleToLongBits(p.getValue()));
		} else if (property instanceof FloatProperty p) {
			tag = "f";
			value = "bits:" + String.format("%08x", Float.floatToIntBits(p.getValue()));
		} else if (property instanceof ShortProperty p) {
			tag = "s";
			value = Short.toString(p.getValue());
		} else if (property instanceof BoolProperty p) {
			tag = "b";
			value = Boolean.toString(p.isValue());
		} else if (property instanceof StringListProperty p) {
			tag = "SL";
			StringBuilder joined = new StringBuilder();
			for (String element : p.getValue()) {
				if (joined.length() > 0) {
					joined.append(',');
				}
				joined.append(escListElement(element));
			}
			value = p.getValue().size() + ":" + joined;
		} else {
			// Unknown subclass — a model extension this scheme predates.
			// Conservative: make it visible (hash moves) instead of
			// silently dropping content.
			tag = "?";
			value = esc(property.eClass().getName());
		}
		return "pr|" + tag + "|" + esc(property.getName()) + "|value=" + value;
	}

	private static String esc(String raw) {
		if (raw == null || raw.isEmpty()) {
			return "";
		}
		StringBuilder out = new StringBuilder(raw.length());
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			switch (c) {
			case '\\' -> out.append("\\\\");
			case '|' -> out.append("\\|");
			case '\n' -> out.append("\\n");
			case '\r' -> out.append("\\r");
			default -> out.append(c);
			}
		}
		return out.toString();
	}

	private static String escListElement(String raw) {
		String escaped = esc(raw);
		return escaped.replace(",", "\\,");
	}

	private static String sha256Hex(String content) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				hex.append(Character.forDigit((b >> 4) & 0xF, 16));
				hex.append(Character.forDigit(b & 0xF, 16));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException impossible) {
			// SHA-256 is mandatory for every JRE.
			throw new IllegalStateException("SHA-256 unavailable", impossible);
		}
	}
}
