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


package org.eclipse.fennec.services.broker.core;

import java.net.URL;

/**
 * A contract served beside the broker that the broker announces as its
 * own (#287).
 *
 * <p>The broker announces its own API from documents in its bundle: each
 * holds the provider that serves a contract and the contract, the same
 * document the generic REST distribution serves it from. A bundle that
 * serves another contract beside the broker — the tag overlay, say —
 * offers its document as this service, and the broker announces it the
 * same way: the contract into the catalog, the implementation with the
 * broker's public address and security scheme added. What is deployment
 * stays in one place, the broker's configuration.
 *
 * <p>Withdrawn when the service goes.
 */
public interface BrokerApiDocument {

	/**
	 * The document: a {@code ServiceProvider} with one implementation and
	 * its {@code RestFlavor}, and the {@code ServiceInterface} it serves,
	 * in that order — the shape of the broker's own API documents.
	 */
	URL document();
}
