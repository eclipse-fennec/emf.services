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
 * Where a node gets the API key it presents to an endpoint whose flavor
 * offers an {@code ApiKeyScheme} (#299) — the calling half of that
 * scheme, as {@link TokenSupplier} is of a bearer scheme.
 *
 * <p>A key is issued by the endpoint to one caller and names no audience
 * and no scope, so it is asked for by the endpoint it goes to: the host
 * the call is sent to and the scheme's name in that endpoint's flavor.
 * Never for the broker: a broker is not called with an API key.
 *
 * <p>Several suppliers may be registered, one per endpoint for example; the
 * first that answers wins. Which key a node presents is its configuration's
 * statement and never part of a document.
 */
@FunctionalInterface
public interface ApiKeySupplier {

	/**
	 * @param host where the call goes, {@code scheme://host[:port]}
	 * @param scheme the name of the {@code ApiKeyScheme} in the endpoint's flavor
	 * @return the key, or {@code null} when this supplier holds none for that endpoint
	 */
	String key(String host, String scheme);
}
