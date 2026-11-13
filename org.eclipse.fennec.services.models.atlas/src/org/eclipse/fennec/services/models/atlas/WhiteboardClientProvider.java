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

import org.eclipse.fennec.model.atlas.rest.client.impl.spi.DefaultJakartaRsClientProvider;

import jakarta.ws.rs.client.ClientBuilder;

/**
 * The atlas client's HTTP client, built from the {@link ClientBuilder}
 * the Jakarta REST whiteboard registers: {@code ClientBuilder.newBuilder()}
 * finds no provider inside OSGi. Timeouts and authentication stay the
 * default provider's.
 */
final class WhiteboardClientProvider extends DefaultJakartaRsClientProvider {

	private final ClientBuilder builder;

	WhiteboardClientProvider(ClientBuilder builder) {
		this.builder = builder;
	}

	@Override
	protected ClientBuilder newClientBuilder() {
		return builder;
	}
}
