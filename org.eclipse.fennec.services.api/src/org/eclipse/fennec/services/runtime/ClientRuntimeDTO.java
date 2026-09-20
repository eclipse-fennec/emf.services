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

package org.eclipse.fennec.services.runtime;

import java.util.List;

import org.osgi.dto.DTO;

/**
 * What one runtime using the SDK holds, at one moment.
 *
 * <p>The counterpart of {@link BrokerRuntimeDTO} and deliberately a
 * different question. The broker knows what exists; a client knows what
 * <em>it</em> published, what it is bound to, and whether it is still
 * hearing anything — and only the client can answer the last one.
 */
public class ClientRuntimeDTO extends DTO {

	/** How this runtime names itself to the broker. */
	public String consumerId;

	/** How often this runtime has changed since it started. */
	public long changeCount;

	/** When it was taken, in epoch milliseconds. */
	public long takenAt;

	/** The transports this runtime told the broker it speaks. */
	public List<String> supportedFlavors;

	/** What this runtime published, and what the broker made of it. */
	public List<PublishedDTO> published;

	/** What it is bound to, and whether those bindings still hold. */
	public List<BindingDTO> bindings;

	/**
	 * Whether the event stream is up.
	 *
	 * <p>The one thing nobody else can answer: the broker sees a
	 * subscription, and a consumer sees a stream, and only the consumer
	 * knows whether it is currently reading one.
	 */
	public boolean eventStreamConnected;

	/** Which transport carries those events — {@code rest} or {@code mqtt}. */
	public String eventTransport;
}
