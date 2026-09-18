/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.client.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.Test;

/**
 * How the proxies turn an HTTP response into the Diagnostic the SDK
 * hands to the application (#56). Every path must close the response.
 */
class CatalogHttpProxyTest {

	/** A Response whose entity is either a Diagnostic, a parse failure, or absent. */
	private static final class StubResponse extends Response {
		private final int status;
		private final Object entity;
		private final boolean unreadable;
		boolean closed;

		StubResponse(int status, Object entity, boolean unreadable) {
			this.status = status;
			this.entity = entity;
			this.unreadable = unreadable;
		}

		@Override public int getStatus() { return status; }
		@Override public StatusType getStatusInfo() { return Status.fromStatusCode(status); }
		@Override public Object getEntity() { return entity; }
		@Override public boolean hasEntity() { return entity != null || unreadable; }
		@Override public void close() { closed = true; }

		@SuppressWarnings("unchecked")
		@Override
		public <T> T readEntity(Class<T> entityType) {
			if (unreadable) {
				throw new IllegalStateException("malformed or unacceptable XMI body");
			}
			return (T) entity;
		}

		@SuppressWarnings("unchecked")
		@Override public <T> T readEntity(GenericType<T> entityType) { return readEntity((Class<T>) entityType.getRawType()); }
		@Override public <T> T readEntity(Class<T> entityType, Annotation[] annotations) { return readEntity(entityType); }
		@Override public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) { return readEntity(entityType); }
		@Override public boolean bufferEntity() { return false; }
		@Override public MediaType getMediaType() { return MediaType.APPLICATION_XML_TYPE; }
		@Override public Locale getLanguage() { return null; }
		@Override public int getLength() { return -1; }
		@Override public Set<String> getAllowedMethods() { return Set.of(); }
		@Override public Map<String, NewCookie> getCookies() { return Map.of(); }
		@Override public EntityTag getEntityTag() { return null; }
		@Override public Date getDate() { return null; }
		@Override public Date getLastModified() { return null; }
		@Override public URI getLocation() { return null; }
		@Override public Set<Link> getLinks() { return Set.of(); }
		@Override public boolean hasLink(String relation) { return false; }
		@Override public Link getLink(String relation) { return null; }
		@Override public Link.Builder getLinkBuilder(String relation) { return null; }
		@Override public MultivaluedMap<String, Object> getMetadata() { return new MultivaluedHashMap<>(); }
		@Override public MultivaluedMap<String, String> getStringHeaders() { return new MultivaluedHashMap<>(); }
		@Override public String getHeaderString(String name) { return null; }
	}

	private static Diagnostic diagnostic(DiagnosticSeverity severity, int code) {
		Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
		d.setSeverity(severity);
		d.setCode(code);
		return d;
	}

	@Test
	void aDiagnosticBodyIsReturnedAsIsWhateverTheStatus() {
		StubResponse notFound = new StubResponse(404, diagnostic(DiagnosticSeverity.ERROR, 212), false);
		Diagnostic d = CatalogHttpProxy.readDiagnostic(notFound);
		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.ERROR);
		assertThat(d.getCode()).as("the broker's code, not the HTTP status").isEqualTo(212);
		assertThat(notFound.closed).isTrue();
	}

	@Test
	void aSuccessWithoutABodyIsASyntheticOk() {
		StubResponse noContent = new StubResponse(204, null, false);
		Diagnostic d = CatalogHttpProxy.readDiagnostic(noContent);
		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		assertThat(noContent.closed).isTrue();
	}

	@Test
	void aFailureWithoutABodyBecomesAnErrorCarryingTheHttpStatusAsCode() {
		StubResponse gatewayDown = new StubResponse(503, null, false);
		Diagnostic d = CatalogHttpProxy.readDiagnostic(gatewayDown);
		assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.ERROR);
		assertThat(d.getCode()).isEqualTo(503);
		assertThat(d.getMessage()).contains("HTTP 503");
		assertThat(gatewayDown.closed).isTrue();
	}

	@Test
	void anUnparseableBodyBecomesAnErrorNamingTheParseFailure() {
		StubResponse garbage = new StubResponse(200, null, true);
		Diagnostic d = CatalogHttpProxy.readDiagnostic(garbage);
		assertThat(d.getSeverity()).as("a 200 with garbage is not a success").isEqualTo(DiagnosticSeverity.ERROR);
		assertThat(d.getMessage()).contains("could not parse Diagnostic body").contains("malformed");
		assertThat(garbage.closed).isTrue();
	}
}
