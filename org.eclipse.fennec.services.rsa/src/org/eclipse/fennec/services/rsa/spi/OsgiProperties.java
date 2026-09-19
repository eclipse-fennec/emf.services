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

package org.eclipse.fennec.services.rsa.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.fennec.services.BoolProperty;
import org.eclipse.fennec.services.DoubleProperty;
import org.eclipse.fennec.services.FloatProperty;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.LongProperty;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ShortProperty;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringProperty;

/**
 * OSGi service properties as the model says them, and back.
 *
 * <p>A service's properties are a flat map of objects; the model says
 * them as typed {@link Property} objects, because a registry that other
 * languages read cannot hand out {@code Object} and hope. The broker
 * filters on exactly these types, so what goes through here is what an
 * LDAP filter can later match.
 *
 * <p>The round trip is not perfectly lossless and says so: a type the
 * model has no property for becomes its {@code toString()}, and a
 * collection comes back as a {@code String[]}. That keeps the value
 * visible and filterable instead of dropping it, and the alternative —
 * refusing the whole map because one entry is a {@code Character} —
 * would lose far more.
 */
public final class OsgiProperties {

	private OsgiProperties() {
	}

	/** The properties a service or endpoint carries, as the model says them. */
	public static List<Property> toModel(Map<String, ?> properties) {
		List<Property> modelled = new ArrayList<>();
		if (properties == null) {
			return modelled;
		}
		for (Map.Entry<String, ?> entry : properties.entrySet()) {
			Property property = propertyOf(entry.getValue());
			if (property == null) {
				continue;
			}
			property.setName(entry.getKey());
			modelled.add(property);
		}
		return modelled;
	}

	/** The same properties as a service registration wants them. */
	public static Map<String, Object> toOsgi(Collection<? extends Property> properties) {
		Map<String, Object> flat = new LinkedHashMap<>();
		if (properties == null) {
			return flat;
		}
		for (Property property : properties) {
			if (property == null || property.getName() == null) {
				continue;
			}
			Object value = valueOf(property);
			if (value != null) {
				flat.put(property.getName(), value);
			}
		}
		return flat;
	}

	private static Property propertyOf(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof String s) {
			StringProperty property = ServicesFactory.eINSTANCE.createStringProperty();
			property.setValue(s);
			return property;
		}
		if (value instanceof Integer i) {
			IntProperty property = ServicesFactory.eINSTANCE.createIntProperty();
			property.setValue(i);
			return property;
		}
		if (value instanceof Long l) {
			LongProperty property = ServicesFactory.eINSTANCE.createLongProperty();
			property.setValue(l);
			return property;
		}
		if (value instanceof Double d) {
			DoubleProperty property = ServicesFactory.eINSTANCE.createDoubleProperty();
			property.setValue(d);
			return property;
		}
		if (value instanceof Float f) {
			FloatProperty property = ServicesFactory.eINSTANCE.createFloatProperty();
			property.setValue(f);
			return property;
		}
		if (value instanceof Short s) {
			ShortProperty property = ServicesFactory.eINSTANCE.createShortProperty();
			property.setValue(s);
			return property;
		}
		if (value instanceof Boolean b) {
			BoolProperty property = ServicesFactory.eINSTANCE.createBoolProperty();
			property.setValue(b);
			return property;
		}
		List<String> several = severalOf(value);
		if (several != null) {
			StringListProperty property = ServicesFactory.eINSTANCE.createStringListProperty();
			property.getValue().addAll(several);
			return property;
		}
		// Byte, Character, an enum, something of the caller's own: the
		// model has no property for it, and its text is still something a
		// filter can match.
		StringProperty property = ServicesFactory.eINSTANCE.createStringProperty();
		property.setValue(String.valueOf(value));
		return property;
	}

	/**
	 * The entries of an array or collection, as text — {@code null} when
	 * the value is neither.
	 */
	private static List<String> severalOf(Object value) {
		if (value instanceof Object[] array) {
			List<String> entries = new ArrayList<>(array.length);
			for (Object entry : array) {
				entries.add(entry == null ? null : String.valueOf(entry));
			}
			return entries;
		}
		if (value instanceof Collection<?> collection) {
			List<String> entries = new ArrayList<>(collection.size());
			for (Object entry : collection) {
				entries.add(entry == null ? null : String.valueOf(entry));
			}
			return entries;
		}
		return null;
	}

	/** The same reading of a property the broker's filter uses. */
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
			// An array, not a list: OSGi's own types insist on it —
			// EndpointDescription refuses an objectClass that is not a
			// String[], and a service registration reads arrays too.
			return p.getValue().toArray(String[]::new);
		}
		return null;
	}
}
