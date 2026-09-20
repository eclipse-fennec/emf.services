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
 * What a broker holds, at one moment.
 *
 * <p>A DTO and not the model, for the reason the OSGi DTOs exist: a
 * reader gets a snapshot it cannot reach through, and it does not have
 * to carry EMF to read one. Everything here is a copy of state that
 * went on living the moment this was built.
 *
 * <p>{@link #revision} is the number that also travels as the runtime
 * service's {@code ddsr.runtime.revision} property. A consumer bound
 * with a dynamic reference is told when it changes, which is the whole
 * of the "something happened, ask again" protocol — see
 * {@link BrokerRuntime}.
 */
public class BrokerRuntimeDTO extends DTO {

	/** The registry's name, as the broker was configured with it. */
	public String name;

	/** How often this snapshot has changed since the broker started. */
	public long revision;

	/** When this snapshot was taken, in epoch milliseconds. */
	public long takenAt;

	/** Every live registration, with the leases on it. */
	public List<RegistrationDTO> registrations;

	/** The contracts the broker governs. */
	public List<CatalogEntryDTO> catalog;

	/** The consumers holding leases. */
	public List<SessionDTO> sessions;

	/** What the event stream has done and owes. */
	public DeliveryDTO delivery;

	/** How many registrations are parked in the cold cache. */
	public int coldEntries;
}
