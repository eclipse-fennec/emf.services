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
package org.eclipse.fennec.services.common;

/**
 * Where a node gets the bearer token it shows the broker — the calling
 * half of {@link CallPrincipal} (SECURITY.md §4).
 *
 * <p>A service, so that the transport does not know how a token is
 * obtained: a client-credentials flow against an identity provider, a
 * token mounted into a container, a test handing out a fixed one. The
 * transport asks once per call and puts the answer into the
 * {@code Authorization} header of calls to the broker — and only those:
 * a token minted for the broker's audience must not travel to a
 * provider, where it could be replayed.
 *
 * <p>No service, no header: a node talking to a broker in mode
 * {@code none} needs nothing registered.
 */
@FunctionalInterface
public interface TokenSupplier {

	/**
	 * A token valid now, without the {@code Bearer} prefix.
	 *
	 * <p>An implementation caches and renews as it sees fit; this is
	 * called on every call to the broker.
	 *
	 * @throws IllegalStateException when no token can be had — the call
	 *         fails here, where the reason is known, instead of at the
	 *         broker as a bare 401
	 */
	String token();

	/**
	 * A token for one target (SECURITY.md §1): the audience a provider's
	 * {@code BearerTokenScheme} names. A token minted for the broker must
	 * not travel to a provider — whoever holds it could replay it there —
	 * so a provider call asks for a token of the provider's own audience.
	 *
	 * <p>The default answers {@link #token()}, which is right for a
	 * supplier that only ever has one token to give, such as one mounted
	 * into a container. The client-credentials supplier asks its identity
	 * provider for the scope of that name, which maps to the audience.
	 */
	default String token(String audience) {
		return token();
	}
}
