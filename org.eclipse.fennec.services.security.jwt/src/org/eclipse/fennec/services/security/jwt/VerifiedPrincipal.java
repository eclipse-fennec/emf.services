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
package org.eclipse.fennec.services.security.jwt;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.fennec.services.Principal;
import org.eclipse.fennec.services.ServicesFactory;

/**
 * Who is calling, as a verifier established it from a credential.
 *
 * <p>A plain value: subject, issuer and the scopes the credential
 * carried. The registry's model has a {@link Principal} for the same
 * thing, made from this one by {@link #model()} — a fresh instance on
 * every call, because a model object is contained by whatever it is
 * put into (a registration, a session), and one instance cannot be in
 * two places. Equality is subject plus issuer: the same subject from
 * another issuer is somebody else.
 */
public final class VerifiedPrincipal {

	private final String subject;
	private final String issuer;
	private final Set<String> scopes;

	public VerifiedPrincipal(String subject, String issuer, Set<String> scopes) {
		this.subject = Objects.requireNonNull(subject, "subject");
		this.issuer = Objects.requireNonNull(issuer, "issuer");
		this.scopes = Set.copyOf(scopes == null ? Set.of() : scopes);
	}

	/** The credential's subject — the OIDC {@code sub}, a client id for a node. */
	public String subject() {
		return subject;
	}

	/** Who vouched for it — the {@code iss} the verifier was configured to trust. */
	public String issuer() {
		return issuer;
	}

	/** The scopes the credential carried, as verified; never {@code null}. */
	public Set<String> scopes() {
		return scopes;
	}

	public boolean hasScope(String scope) {
		return scope != null && scopes.contains(scope);
	}

	/** The registry's view of this principal — a new model object each time. */
	public Principal model() {
		Principal principal = ServicesFactory.eINSTANCE.createPrincipal();
		principal.setSubject(subject);
		principal.setIssuer(issuer);
		principal.getScopes().addAll(List.copyOf(new TreeSet<>(scopes)));
		return principal;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof VerifiedPrincipal p && p.subject.equals(subject) && p.issuer.equals(issuer);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, issuer);
	}

	@Override
	public String toString() {
		return "VerifiedPrincipal[" + subject + " issuer=" + issuer + " scopes=" + new TreeSet<>(scopes) + "]";
	}
}
