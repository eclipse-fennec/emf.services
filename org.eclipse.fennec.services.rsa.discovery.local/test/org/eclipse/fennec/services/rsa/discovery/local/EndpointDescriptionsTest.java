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

package org.eclipse.fennec.services.rsa.discovery.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Reading the format of 122.6.2, in the shape the specification's own
 * tests write it.
 *
 * <p>Types are the point: an endpoint is found by LDAP filters, and a
 * filter compares a {@code Long} to a {@code Long}. A reader that hands
 * back the text of a number produces an endpoint nobody matches.
 */
class EndpointDescriptionsTest {

	private static List<EndpointDescription> read(String xml) throws IOException {
		return EndpointDescriptions.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	private static String document(String properties) {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<endpoint-descriptions xmlns="http://www.osgi.org/xmlns/rsa/v1.0.0">
				<endpoint-description>
				 <property name="objectClass" value-type="String">
				  <array><value>com.example.A</value></array>
				 </property>
				 <property name="endpoint.id" value-type="String" value="someURI"></property>
				 <property name="service.imported.configs" value-type="String" value="A"></property>
				%s
				</endpoint-description>
				</endpoint-descriptions>
				""".formatted(properties);
	}

	@Test
	@DisplayName("the three mandatory properties make an endpoint")
	void mandatory() throws IOException {
		List<EndpointDescription> endpoints = read(document(""));

		assertThat(endpoints).singleElement().satisfies(endpoint -> {
			assertThat(endpoint.getId()).isEqualTo("someURI");
			assertThat(endpoint.getInterfaces()).containsExactly("com.example.A");
			assertThat(endpoint.getConfigurationTypes()).containsExactly("A");
		});
	}

	@Test
	@DisplayName("a number said to be a Long comes back a Long, not its text")
	void typedScalars() throws IOException {
		List<EndpointDescription> endpoints = read(document("""
				 <property name="endpoint.service.id" value-type="Long" value="12345"></property>
				 <property name="myfloat" value-type="Float" value="3.1415"></property>
				 <property name="mydouble" value-type="Double" value="-3.1415"></property>
				 <property name="mychar" value-type="Character" value="t"></property>
				 <property name="good test" value-type="Boolean" value="true"></property>
				"""));

		EndpointDescription endpoint = endpoints.get(0);
		assertThat(endpoint.getServiceId()).isEqualTo(12345L);
		assertThat(endpoint.getProperties()).containsEntry("myfloat", 3.1415f)
				.containsEntry("mydouble", -3.1415d)
				.containsEntry("mychar", 't')
				.containsEntry("good test", Boolean.TRUE);
	}

	@Test
	@DisplayName("a list stays a list and a set stays a set")
	@SuppressWarnings("unchecked")
	void containers() throws IOException {
		List<EndpointDescription> endpoints = read(document("""
				 <property name="mylist" value-type="String">
				  <list><value>first</value><value>second</value></list>
				 </property>
				 <property name="myset" value-type="String">
				  <set><value>one</value><value>two</value></set>
				 </property>
				"""));

		assertThat(endpoints.get(0).getProperties()).containsEntry("mylist", List.of("first", "second"));
		assertThat(endpoints.get(0).getProperties().get("myset")).isInstanceOf(Set.class);
		assertThat((Set<Object>) endpoints.get(0).getProperties().get("myset"))
				.containsExactlyInAnyOrder("one", "two");
	}

	@Test
	@DisplayName("a property that is itself XML keeps its markup")
	void xmlValued() throws IOException {
		List<EndpointDescription> endpoints = read(document("""
				 <property name="myxml">
				  <xml>
				<myxml>test</myxml>
				  </xml>
				 </property>
				"""));

		assertThat(endpoints.get(0).getProperties()).containsEntry("myxml", "<myxml>test</myxml>");
	}

	@Test
	@DisplayName("a document that points outside itself is refused, not fetched")
	void noExternalEntities() {
		String hostile = """
				<?xml version="1.0"?>
				<!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
				<endpoint-descriptions xmlns="http://www.osgi.org/xmlns/rsa/v1.0.0">
				<endpoint-description>
				 <property name="endpoint.id" value="&xxe;"></property>
				</endpoint-description>
				</endpoint-descriptions>
				""";

		assertThatThrownBy(() -> read(hostile)).isInstanceOf(IOException.class);
	}

	@Test
	@DisplayName("several descriptions in one document are several endpoints")
	void severalDescriptions() throws IOException {
		String two = """
				<?xml version="1.0" encoding="UTF-8"?>
				<endpoint-descriptions xmlns="http://www.osgi.org/xmlns/rsa/v1.0.0">
				<endpoint-description>
				 <property name="objectClass" value-type="String"><array><value>com.example.A</value></array></property>
				 <property name="endpoint.id" value="one"></property>
				 <property name="service.imported.configs" value="A"></property>
				</endpoint-description>
				<endpoint-description>
				 <property name="objectClass" value-type="String"><array><value>com.example.B</value></array></property>
				 <property name="endpoint.id" value="two"></property>
				 <property name="service.imported.configs" value="A"></property>
				</endpoint-description>
				</endpoint-descriptions>
				""";

		assertThat(read(two)).extracting(EndpointDescription::getId).containsExactly("one", "two");
	}
}
