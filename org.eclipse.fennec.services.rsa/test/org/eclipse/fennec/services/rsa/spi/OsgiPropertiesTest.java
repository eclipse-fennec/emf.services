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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.StringListProperty;
import org.eclipse.fennec.services.StringProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What survives the trip between a service's properties and the model's.
 *
 * <p>This matters beyond tidiness: the broker filters on the model's
 * types, so a value that arrives as the wrong one is a service an LDAP
 * filter stops finding.
 */
class OsgiPropertiesTest {

	@Test
	@DisplayName("every type the model has comes back as itself")
	void roundTrip() {
		Map<String, Object> original = Map.of(
				"a.string", "text",
				"an.int", 42,
				"a.long", 5_000_000_000L,
				"a.double", 0.025d,
				"a.float", 1.5f,
				"a.short", (short) 3,
				"a.boolean", true);

		Map<String, Object> back = OsgiProperties.toOsgi(OsgiProperties.toModel(original));

		assertThat(back).isEqualTo(original);
	}

	@Test
	@DisplayName("an array and a collection both come back as String[], which is what OSGi's own types demand")
	void several() {
		Map<String, Object> original = Map.of(
				"objectClass", new String[] { "com.example.A", "com.example.B" },
				"tags", List.of("demo", "payments"));

		Map<String, Object> back = OsgiProperties.toOsgi(OsgiProperties.toModel(original));

		// EndpointDescription refuses an objectClass that is not a
		// String[] — a List would travel and then be rejected on arrival.
		assertThat(back.get("objectClass")).isInstanceOf(String[].class);
		assertThat((String[]) back.get("objectClass")).containsExactly("com.example.A", "com.example.B");
		assertThat((String[]) back.get("tags")).containsExactly("demo", "payments");
	}

	@Test
	@DisplayName("a type the model has no property for keeps its text rather than vanishing")
	void unknownType() {
		List<Property> modelled = OsgiProperties.toModel(Map.of("a.char", 't', "a.byte", (byte) 7));

		assertThat(modelled).allMatch(StringProperty.class::isInstance);
		assertThat(OsgiProperties.toOsgi(modelled))
				.containsEntry("a.char", "t")
				.containsEntry("a.byte", "7");
	}

	@Test
	@DisplayName("the name travels with the value")
	void namesAreKept() {
		List<Property> modelled = OsgiProperties.toModel(Map.of("endpoint.id", "someURI"));

		assertThat(modelled).singleElement()
				.satisfies(property -> assertThat(property.getName()).isEqualTo("endpoint.id"));
	}

	@Test
	@DisplayName("a property with no name is not a property anyone can ask for")
	void namelessIsDropped() {
		StringListProperty nameless = org.eclipse.fennec.services.ServicesFactory.eINSTANCE
				.createStringListProperty();
		nameless.getValue().add("orphan");

		assertThat(OsgiProperties.toOsgi(List.of(nameless))).isEmpty();
	}

	@Test
	@DisplayName("nothing in, nothing out — and no exception either way")
	void empties() {
		assertThat(OsgiProperties.toModel(null)).isEmpty();
		assertThat(OsgiProperties.toOsgi(null)).isEmpty();
		assertThat(OsgiProperties.toModel(Map.of())).isEmpty();
	}
}
