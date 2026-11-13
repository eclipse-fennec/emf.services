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

package org.eclipse.fennec.services.models.atlas;

import java.net.URI;

import org.eclipse.fennec.model.atlas.rest.client.api.ModelAtlasClient;
import org.eclipse.fennec.model.atlas.rest.client.api.ModelAtlasClientFactory;

/** Opens the atlas client from the source's configuration, refusing one that names no atlas. */
final class AtlasClients {

	private AtlasClients() {
	}

	static ModelAtlasClient open(ModelAtlasClientFactory clients, String baseUri, String scope, int connectTimeoutMs,
			int readTimeoutMs) {
		if (baseUri == null || baseUri.isBlank()) {
			throw new IllegalArgumentException("base.uri names no atlas");
		}
		if (scope == null || scope.isBlank()) {
			throw new IllegalArgumentException("scope names no atlas scope");
		}
		return clients.builder()
				.baseUri(URI.create(baseUri))
				.connectTimeoutMs(connectTimeoutMs)
				.readTimeoutMs(readTimeoutMs)
				.build();
	}
}
