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
package org.eclipse.fennec.services.security.mtls.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.eclipse.fennec.services.common.CallPrincipal;
import org.eclipse.fennec.services.security.jwt.SecurityNames;
import org.eclipse.fennec.services.security.jwt.VerifiedPrincipal;
import org.eclipse.fennec.services.security.mtls.internal.CertificatePrincipals.CertificateRejectedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

/**
 * A client certificate becomes the same principal a token does — subject
 * from the CN, issuer from the authority, roles from this node's mapping
 * — and anything less is 401 (SECURITY.md §7d).
 */
class MutualTlsAuthFilterTest {

	private static final String HEADER = "X-Client-Cert";
	private static final String GATEWAY = "10.0.0.5";

	@TempDir
	static Path dir;

	private static X509Certificate client;
	private static X509Certificate admin;
	private static X509Certificate foreign;
	private static X509Certificate nameless;

	private MutualTlsAuthFilter filter;
	private ContainerRequestContext request;

	@BeforeAll
	static void issue() throws Exception {
		TestCertificates certificates = new TestCertificates(dir);
		client = certificates.issue("client", "CN=java-client,O=Fennec");
		admin = certificates.issue("admin", "CN=harness,O=Fennec");
		foreign = certificates.issue("foreign", "CN=java-client,O=Elsewhere", "other-ca");
		nameless = certificates.issue("nameless", "O=Fennec,OU=nobody");
	}

	@BeforeEach
	void setUp() {
		filter = new MutualTlsAuthFilter();
		filter.activate(config(new String[] { TestCertificates.CA_DN },
				new String[] { "java-client=service-consumer,service-provider", "harness=service-admin" }, HEADER,
				new String[] { GATEWAY }));
		request = mock(ContainerRequestContext.class);
		UriInfo uri = mock(UriInfo.class);
		when(uri.getPath()).thenReturn("/catalog");
		when(request.getUriInfo()).thenReturn(uri);
		when(request.getMethod()).thenReturn("GET");
	}

	@AfterEach
	void clearThread() {
		CallPrincipal.clear();
	}

	@Test
	void aCertificateOfTheSessionIsThePrincipal() {
		fromSession(client);

		filter.filter(request);

		VerifiedPrincipal principal = principal();
		assertThat(principal.subject()).as("the CN, the twin of azp").isEqualTo("java-client");
		assertThat(principal.issuer()).isEqualTo(client.getIssuerX500Principal().getName());
		assertThat(principal.scopes()).containsExactlyInAnyOrder("service-consumer", "service-provider");
		assertThat(CallPrincipal.subject()).isEqualTo("java-client");
		ArgumentCaptor<SecurityContext> security = ArgumentCaptor.forClass(SecurityContext.class);
		verify(request).setSecurityContext(security.capture());
		assertThat(security.getValue().isUserInRole("service-provider")).isTrue();
		assertThat(security.getValue().getAuthenticationScheme()).isEqualTo(SecurityContext.CLIENT_CERT_AUTH);
	}

	@Test
	void aSubjectNobodyMappedIsAuthenticatedWithNoRoles() throws Exception {
		filter.activate(config(new String[0], new String[0], "", new String[0]));
		fromSession(admin);

		filter.filter(request);

		assertThat(principal().scopes()).isEmpty();
	}

	@Test
	void withoutACertificateTheCallIs401() {
		filter.filter(request);

		assertThat(refusal().getStatus()).isEqualTo(401);
		assertThat(CallPrincipal.current()).isNull();
	}

	@Test
	void anAuthorityThatIsNotTrustedIs401() {
		fromSession(foreign);

		filter.filter(request);

		assertThat(refusal().getStatus()).isEqualTo(401);
	}

	@Test
	void aCertificateWithoutACommonNameIs401() {
		fromSession(nameless);

		filter.filter(request);

		assertThat(refusal().getStatus()).isEqualTo(401);
	}

	@Test
	void theGatewaysHeaderCountsFromTheGateway() throws Exception {
		fromAddress(GATEWAY);
		when(request.getHeaderString(HEADER)).thenReturn(TestCertificates.pem(admin));

		filter.filter(request);

		assertThat(principal().subject()).isEqualTo("harness");
		assertThat(principal().scopes()).containsExactly("service-admin");
	}

	@Test
	void theSameHeaderFromAnybodyElseIs401() throws Exception {
		fromAddress("10.0.0.99");
		when(request.getHeaderString(HEADER)).thenReturn(TestCertificates.pem(admin));

		filter.filter(request);

		assertThat(refusal().getStatus()).isEqualTo(401);
		verify(request, never()).setProperty(eq(SecurityNames.REQUEST_PRINCIPAL), any());
	}

	@Test
	void theSessionsCertificateWinsOverAHeader() throws Exception {
		fromSession(client);
		fromAddress(GATEWAY);
		when(request.getHeaderString(HEADER)).thenReturn(TestCertificates.pem(admin));

		filter.filter(request);

		assertThat(principal().subject()).isEqualTo("java-client");
	}

	@Test
	void everyGatewaySpellingIsRead() throws Exception {
		String pem = TestCertificates.pem(client);
		String der = Base64.getEncoder().encodeToString(client.getEncoded());
		StringBuilder escaped = new StringBuilder();
		for (byte b : pem.getBytes(StandardCharsets.US_ASCII)) {
			char c = (char) b;
			escaped.append(Character.isLetterOrDigit(c) ? String.valueOf(c) : String.format("%%%02X", b));
		}

		assertThat(GatewayCertificates.parse(pem)).as("PEM").isEqualTo(client);
		assertThat(GatewayCertificates.parse(escaped.toString())).as("URL-encoded PEM").isEqualTo(client);
		assertThat(GatewayCertificates.parse(der)).as("base64 DER, '+' and all").isEqualTo(client);
		assertThatThrownBy(() -> GatewayCertificates.parse("not a certificate"))
				.isInstanceOf(CertificateException.class);
	}

	@Test
	void trustedIssuersCompareCanonically() throws Exception {
		CertificatePrincipals spaced = new CertificatePrincipals(new String[] { " cn=Fennec Test CA, o=Fennec " },
				new String[0]);

		assertThat(spaced.principalOf(client).subject()).isEqualTo("java-client");
		assertThatThrownBy(() -> spaced.principalOf(foreign)).isInstanceOf(CertificateRejectedException.class);
	}

	private void fromSession(X509Certificate certificate) {
		when(request.getProperty(MutualTlsAuthFilter.CERTIFICATE_ATTRIBUTE))
				.thenReturn(new X509Certificate[] { certificate });
	}

	private void fromAddress(String address) {
		HttpServletRequest servlet = mock(HttpServletRequest.class);
		when(servlet.getRemoteAddr()).thenReturn(address);
		filter.servletRequest = servlet;
	}

	private VerifiedPrincipal principal() {
		verify(request, never()).abortWith(any());
		ArgumentCaptor<Object> principal = ArgumentCaptor.forClass(Object.class);
		verify(request).setProperty(eq(SecurityNames.REQUEST_PRINCIPAL), principal.capture());
		return (VerifiedPrincipal) principal.getValue();
	}

	private Response refusal() {
		ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
		verify(request).abortWith(response.capture());
		return response.getValue();
	}

	private static MutualTlsAuthFilter.Config config(String[] issuers, String[] roles, String header,
			String[] gateways) {
		return new MutualTlsAuthFilter.Config() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return MutualTlsAuthFilter.Config.class;
			}

			@Override
			public String[] trusted_issuers() {
				return issuers;
			}

			@Override
			public String[] roles() {
				return roles;
			}

			@Override
			public String gateway_header() {
				return header;
			}

			@Override
			public String[] gateway_addresses() {
				return gateways;
			}
		};
	}
}
