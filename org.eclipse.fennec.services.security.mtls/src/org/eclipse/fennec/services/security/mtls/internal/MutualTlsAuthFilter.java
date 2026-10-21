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

import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.fennec.services.common.CallPrincipal;
import org.eclipse.fennec.services.security.jwt.SecurityNames;
import org.eclipse.fennec.services.security.jwt.VerifiedPrincipal;
import org.eclipse.fennec.services.security.mtls.internal.CertificatePrincipals.CertificateRejectedException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsExtension;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * The filter of mode {@code fennec.auth=mtls}: every request on such an
 * application comes with a client certificate this node accepts, or it
 * is {@code 401} before matching (SECURITY.md §7d).
 *
 * <p>The principal is handed on exactly where the bearer filter hands
 * on its own — the request property {@link SecurityNames#REQUEST_PRINCIPAL},
 * the {@link SecurityContext}, and {@link CallPrincipal} on the thread —
 * so the roles feature, the generic dispatcher and the broker core read
 * a certificate caller as they read a token caller.
 *
 * <p>Where the certificate comes from:
 * <ol>
 * <li>the TLS session, when this framework ended TLS itself and asked
 * for one ({@code org.apache.felix.https.clientcertificate=needs});</li>
 * <li>else a header a gateway that ended TLS put it in — only when a
 * header is configured, and only from the gateway addresses named.</li>
 * </ol>
 *
 * <p>Unlike the bearer filter it needs no verifier to exist: the
 * whiteboard keeps an application of this mode down until this filter
 * is there, and a request without a certificate is turned away by it.
 * The configuration only narrows (trusted issuers) and maps (roles).
 */
@Designate(ocd = MutualTlsAuthFilter.Config.class)
@Component(service = { ContainerRequestFilter.class, ContainerResponseFilter.class },
		configurationPid = MutualTlsAuthFilter.PID, configurationPolicy = ConfigurationPolicy.OPTIONAL,
		scope = ServiceScope.PROTOTYPE)
@JakartarsExtension
@JakartarsName(SecurityNames.MTLS_FILTER)
@JakartarsApplicationSelect("(" + SecurityNames.AUTH + "=" + SecurityNames.MTLS + ")")
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class MutualTlsAuthFilter implements ContainerRequestFilter, ContainerResponseFilter {

	public static final String PID = "org.eclipse.fennec.services.security.mtls";

	/** Where a servlet container puts the client's certificate chain. */
	static final String CERTIFICATE_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

	private static final Logger LOG = Logger.getLogger(MutualTlsAuthFilter.class.getName());

	@ObjectClassDefinition(name = "Fennec Services Client Certificate Verification",
			description = "How this node reads the client certificates of mode fennec.auth=mtls.")
	public @interface Config {

		@AttributeDefinition(name = "Trusted issuers", required = false,
				description = "DNs of the authorities whose client certificates are accepted. Empty: whatever "
						+ "the TLS trust store (or the gateway) accepted.")
		String[] trusted_issuers() default {};

		@AttributeDefinition(name = "Roles", required = false,
				description = "subject=role,role per entry: the roles a certificate's CN holds here. A "
						+ "certificate carries none of its own.")
		String[] roles() default {};

		@AttributeDefinition(name = "Gateway header", required = false,
				description = "The request header a gateway that ended TLS puts the client certificate in "
						+ "(PEM, URL-encoded PEM or base64 DER). Empty: only the TLS session counts.")
		String gateway_header() default "";

		@AttributeDefinition(name = "Gateway addresses", required = false,
				description = "The remote addresses the gateway header is believed from. Empty with a header "
						+ "set: from nobody.")
		String[] gateway_addresses() default {};
	}

	/**
	 * The servlet request, for the remote address the gateway header is
	 * judged by. A proxy Jersey resolves per request — within the one
	 * application that injected it, which is why this component is a
	 * prototype: the whiteboard gives every application its own
	 * instance, and a shared one would ask another application's scope.
	 */
	@Context
	HttpServletRequest servletRequest;

	private volatile CertificatePrincipals principals = new CertificatePrincipals(null, null);
	private volatile String gatewayHeader = "";
	private volatile Set<String> gatewayAddresses = Set.of();

	@Activate
	void activate(Config config) {
		modified(config);
	}

	@Modified
	void modified(Config config) {
		this.principals = new CertificatePrincipals(config.trusted_issuers(), config.roles());
		this.gatewayHeader = config.gateway_header() == null ? "" : config.gateway_header().strip();
		this.gatewayAddresses = Set.of(config.gateway_addresses());
		LOG.info("[DDSR] client certificates: " + config.trusted_issuers().length + " trusted issuer(s), "
				+ config.roles().length + " role mapping(s)"
				+ (gatewayHeader.isEmpty() ? "" : ", gateway header " + gatewayHeader + " from " + gatewayAddresses));
	}

	@Override
	public void filter(ContainerRequestContext request) {
		// Unconditionally first, as in the bearer filter: nothing the
		// previous request left on this thread survives into this one.
		CallPrincipal.clear();
		X509Certificate certificate;
		try {
			certificate = presented(request);
		} catch (CertificateException e) {
			reject(request, e.getMessage());
			return;
		}
		if (certificate == null) {
			reject(request, "no client certificate");
			return;
		}
		VerifiedPrincipal principal;
		try {
			principal = principals.principalOf(certificate);
		} catch (CertificateRejectedException e) {
			reject(request, e.getMessage());
			return;
		}
		boolean secure = request.getSecurityContext() != null && request.getSecurityContext().isSecure();
		request.setProperty(SecurityNames.REQUEST_PRINCIPAL, principal);
		request.setSecurityContext(new CertificateSecurityContext(principal, secure));
		CallPrincipal.set(principal.model());
	}

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) {
		CallPrincipal.clear();
	}

	/** The holder's certificate: from the TLS session, else from a trusted gateway, else none. */
	X509Certificate presented(ContainerRequestContext request) throws CertificateException {
		if (request.getProperty(CERTIFICATE_ATTRIBUTE) instanceof X509Certificate[] chain && chain.length > 0) {
			return chain[0];
		}
		if (gatewayHeader.isEmpty()) {
			return null;
		}
		String value = request.getHeaderString(gatewayHeader);
		if (value == null) {
			return null;
		}
		String from = servletRequest == null ? null : servletRequest.getRemoteAddr();
		if (from == null || !gatewayAddresses.contains(from)) {
			// Somebody wrote the header who is not the gateway: a claim,
			// and claims are not certificates.
			throw new CertificateException(gatewayHeader + " from " + from + ", which is not a trusted gateway");
		}
		return GatewayCertificates.parse(value);
	}

	/** 401 without a body: the reason goes to the log, never to the caller. */
	private static void reject(ContainerRequestContext request, String reason) {
		LOG.info("[DDSR] 401 " + request.getMethod() + " " + request.getUriInfo().getPath() + ": " + reason);
		request.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
	}

	/** What a resource sees: the CN as the user principal, the mapped roles as roles. */
	static final class CertificateSecurityContext implements SecurityContext {
		private final VerifiedPrincipal principal;
		private final boolean secure;

		CertificateSecurityContext(VerifiedPrincipal principal, boolean secure) {
			this.principal = principal;
			this.secure = secure;
		}

		@Override
		public Principal getUserPrincipal() {
			return principal::subject;
		}

		@Override
		public boolean isUserInRole(String role) {
			return principal.hasScope(role);
		}

		@Override
		public boolean isSecure() {
			return secure;
		}

		@Override
		public String getAuthenticationScheme() {
			return SecurityContext.CLIENT_CERT_AUTH;
		}
	}
}
