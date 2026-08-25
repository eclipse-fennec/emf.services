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

package org.eclipse.fennec.services.client;

/**
 * Entry point of the DDSR Java client SDK. Exposes the two facades
 * that providers and consumers use, on top of whichever broker
 * transport flavor (REST proxy, MQTT proxy, embedded broker) is wired
 * in via the {@code BrokerImplementations} and {@code BrokerLookup}
 * OSGi services.
 *
 * <p>Callers never construct XMI or HTTP requests by hand — they go
 * through the facades; transport details belong to the flavor bundle.
 */
public interface DdsrClient extends AutoCloseable {

	/** Provider-side facade: publish / withdraw implementations. */
	DdsrProvider provider();

	/** Consumer-side facade: lookup references, subscribe to events. */
	DdsrConsumer consumer();

	@Override
	void close();
}
