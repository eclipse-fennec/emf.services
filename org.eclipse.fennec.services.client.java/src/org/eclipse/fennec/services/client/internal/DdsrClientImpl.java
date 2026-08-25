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

package org.eclipse.fennec.services.client.internal;

import java.util.List;

import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.client.DdsrConsumer;
import org.eclipse.fennec.services.client.DdsrProvider;
import org.eclipse.fennec.services.FlavorKind;

/**
 * Transport-agnostic client core. Wires the provider/consumer facades
 * over the three broker role interfaces — embedded, REST-proxy, or any
 * other flavor is selected by which OSGi service provides them.
 */
public final class DdsrClientImpl implements DdsrClient {

	private final ProviderImpl provider;
	private final ConsumerImpl consumer;

	public DdsrClientImpl(BrokerImplementations implementations, BrokerLookup lookup,
			List<FlavorKind> supportedFlavors, String consumerId, EventSource eventSource) {
		this.provider = new ProviderImpl(implementations, lookup);
		this.consumer = new ConsumerImpl(lookup, supportedFlavors, consumerId, eventSource);
	}

	@Override
	public DdsrProvider provider() {
		return provider;
	}

	@Override
	public DdsrConsumer consumer() {
		return consumer;
	}

	@Override
	public void close() {
		// no SDK-owned resources — the transport (BrokerCatalog /
		// BrokerImplementations / BrokerLookup) is managed by whichever
		// flavor bundle is wired in.
	}

	/** Lets the component tell the consumer that a transport showed up. */
	public void transportAvailable() {
		consumer.transportAvailable();
	}
}
