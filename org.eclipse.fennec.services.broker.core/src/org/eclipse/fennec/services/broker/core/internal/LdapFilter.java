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

package org.eclipse.fennec.services.broker.core.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.fennec.services.BoolProperty;
import org.eclipse.fennec.services.DoubleProperty;
import org.eclipse.fennec.services.FloatProperty;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.LongProperty;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ShortProperty;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringProperty;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * RFC-1960 filter over the typed {@link Property} hierarchy of a
 * {@link ServiceReference} — a thin adapter around the OSGi
 * {@link Filter}.
 * <p>
 * The first iteration was a hand-written parser/evaluator that
 * deliberately mimicked the OSGi filter semantics. That is the wrong
 * side of the build/borrow line: {@link FrameworkUtil#createFilter}
 * works <em>standalone</em> (since Core R7 the API carries its own
 * filter implementation, so plain-JUnit tests need no framework), and
 * at runtime Felix supplies the battle-tested implementation — the
 * OSGi semantics this class wants are then true by construction, not
 * by imitation (DECISIONS_PARITY D7).
 * <p>
 * What this adapter owns is only the mapping of the typed property
 * hierarchy onto filter attributes: String→String, Int→Integer,
 * Long→Long, Double→Double, Float→Float, Short→Short, Bool→Boolean,
 * StringList→List (OSGi any-match over collections; an empty list
 * still satisfies presence but no value comparison). A property with a
 * {@code null} name or value is skipped; for duplicate names the first
 * occurrence wins. Matching goes through {@link Filter#match} with a
 * {@link FrameworkUtil#asDictionary Dictionary view}, which gives the
 * OSGi-standard case-insensitive attribute names —
 * {@code matches(Map)} would compare keys case-sensitively.
 * <p>
 * Note one deliberate behavior change against the first iteration:
 * {@code ~=} (approximate matching) is now supported instead of being
 * rejected, because the OSGi filter supports it.
 */
final class LdapFilter {

	private final Filter filter;

	private LdapFilter(Filter filter) {
		this.filter = filter;
	}

	/**
	 * Parses the filter string.
	 *
	 * @throws IllegalArgumentException for {@code null}, blank or
	 *         syntactically invalid input
	 */
	static LdapFilter parse(String filter) {
		if (filter == null || filter.isBlank()) {
			throw new IllegalArgumentException("filter must not be null or blank");
		}
		try {
			return new LdapFilter(FrameworkUtil.createFilter(filter));
		} catch (InvalidSyntaxException invalid) {
			throw new IllegalArgumentException(invalid.getMessage(), invalid);
		}
	}

	/** True when the reference's typed properties satisfy the filter. */
	boolean matches(ServiceReference reference) {
		if (reference == null) {
			return false;
		}
		return filter.match(FrameworkUtil.asDictionary(attributesOf(reference)));
	}

	private static Map<String, Object> attributesOf(ServiceReference reference) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		for (Property property : reference.getProperties()) {
			String name = property.getName();
			if (name == null || attributes.containsKey(name)) {
				continue;
			}
			Object value = valueOf(property);
			if (value != null) {
				attributes.put(name, value);
			}
		}
		return attributes;
	}

	private static Object valueOf(Property property) {
		if (property instanceof StringProperty p) {
			return p.getValue();
		}
		if (property instanceof IntProperty p) {
			return p.getValue();
		}
		if (property instanceof LongProperty p) {
			return p.getValue();
		}
		if (property instanceof DoubleProperty p) {
			return p.getValue();
		}
		if (property instanceof FloatProperty p) {
			return p.getValue();
		}
		if (property instanceof ShortProperty p) {
			return p.getValue();
		}
		if (property instanceof BoolProperty p) {
			return p.isValue();
		}
		if (property instanceof StringListProperty p) {
			List<String> values = new ArrayList<>(p.getValue());
			values.removeIf(Objects::isNull);
			return values;
		}
		// Unknown subclass — no comparable value, treat as absent.
		return null;
	}
}
