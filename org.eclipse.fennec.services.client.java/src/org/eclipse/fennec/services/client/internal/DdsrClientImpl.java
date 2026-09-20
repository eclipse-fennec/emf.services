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
import java.util.function.Supplier;
import java.util.Set;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.DdsrConsumer;
import org.eclipse.fennec.services.client.DdsrProvider;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.runtime.ClientRuntimeDTO;

/**
 * Transport-agnostic client core. Wires the provider/consumer facades
 * over the three broker role interfaces — embedded, REST-proxy, or any
 * other flavor is selected by which OSGi service provides them.
 */
public final class DdsrClientImpl implements DdsrClient {

	private final ProviderImpl provider;
	private final ConsumerImpl consumer;

	/** What this runtime looks like from outside (#126). */
	private final ClientRuntimeImpl runtime;

	public DdsrClientImpl(BrokerImplementations implementations, BrokerLookup lookup,
			List<FlavorKind> supportedFlavors, String consumerId, EventSource eventSource) {
		this(implementations, lookup, supportedFlavors, consumerId, eventSource, false);
	}

	public DdsrClientImpl(BrokerImplementations implementations, BrokerLookup lookup,
			List<FlavorKind> supportedFlavors, String consumerId, EventSource eventSource, boolean greedyRebind) {
		this(implementations, lookup, supportedFlavors, consumerId, eventSource, greedyRebind, () -> null);
	}

	/**
	 * @param eventTransport which transport the event source is, asked
	 *        rather than held: the source is a dynamic reference, and a
	 *        value read once at construction would name whichever one
	 *        happened to be bound then (#126)
	 */
	public DdsrClientImpl(BrokerImplementations implementations, BrokerLookup lookup,
			List<FlavorKind> supportedFlavors, String consumerId, EventSource eventSource, boolean greedyRebind,
			Supplier<String> eventTransport) {
		this.provider = new ProviderImpl(implementations, lookup);
		this.consumer = new ConsumerImpl(lookup, supportedFlavors, consumerId, eventSource, greedyRebind);
		this.runtime = new ClientRuntimeImpl(provider, consumer, consumer::streamConnected, eventTransport);
	}

	/** What this runtime holds, for anything that wants to watch it (#126). */
	public ClientRuntimeDTO runtimeSnapshot() {
		return runtime.snapshot();
	}

	public long runtimeChangeCount() {
		return runtime.changeCount();
	}

	/**
	 * Which snapshot the next answer would be — derived from the state,
	 * not counted, so no call site can forget to report a change.
	 */

	@Override
	public DdsrProvider provider() {
		return provider;
	}

	@Override
	public DdsrConsumer consumer() {
		return consumer;
	}

	/** Reference ids known from lookups — the session acquisition list. */
	public Set<String> knownReferenceIds() {
		return consumer.knownReferenceIds();
	}

	/** Provider liveness (#52): one heartbeat per live registration; see {@code ProviderImpl.heartbeatAll}. */
	public int heartbeatRegistrations(long intervalSeconds) {
		return provider.heartbeatAll(intervalSeconds);
	}

	@Override
	public void close() {
		// The transport (BrokerCatalog / BrokerImplementations /
		// BrokerLookup) belongs to whichever flavor bundle is wired in;
		// the event stream the consumer opened on it is ours to close.
		consumer.close();
	}

}
