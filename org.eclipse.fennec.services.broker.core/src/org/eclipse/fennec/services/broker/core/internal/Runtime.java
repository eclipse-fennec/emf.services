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

package org.eclipse.fennec.services.broker.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.broker.core.BrokerSessions.SessionSnapshot;
import org.eclipse.fennec.services.broker.core.ContractAddressing;
import org.eclipse.fennec.services.runtime.BrokerRuntimeDTO;
import org.eclipse.fennec.services.runtime.CatalogEntryDTO;
import org.eclipse.fennec.services.runtime.DeliveryDTO;
import org.eclipse.fennec.services.runtime.RegistrationDTO;
import org.eclipse.fennec.services.runtime.SessionDTO;

/**
 * The broker, as a snapshot somebody else can read.
 *
 * <p>Introspection is its own concern for the same reason the others
 * are: everything here reads state and changes none of it, and a
 * concern that only reads can be handed the lock and trusted with it.
 *
 * <p>Built per call, under the read lock, in one pass — a snapshot
 * assembled from several short reads would show a broker that never
 * existed: registrations from before a withdraw and sessions from
 * after it.
 *
 * <p>The revision is the other half. It counts changes rather than
 * describing them, which is all a watcher needs: the answer to "has
 * anything happened" is a comparison, and the answer to "what" is
 * another snapshot.
 */
final class Runtime {

	private final BrokerState state;

	private final Sessions sessions;

	private final Liveness liveness;

	private final ColdCache cold;

	private final EventDelivery delivery;

	/** How often the answer would have differed since this broker started. */
	private final AtomicLong revision = new AtomicLong();

	Runtime(BrokerState state, Sessions sessions, Liveness liveness, ColdCache cold, EventDelivery delivery) {
		this.state = state;
		this.sessions = sessions;
		this.liveness = liveness;
		this.cold = cold;
		this.delivery = delivery;
	}

	/**
	 * Something changed.
	 *
	 * <p>Called by the concerns that change things, rather than derived
	 * from the event stream: leases and sessions raise no service events,
	 * and a runtime that reported everything except who is holding what
	 * would be reporting the easy half.
	 *
	 * @return the revision after the change
	 */
	long changed() {
		return revision.incrementAndGet();
	}

	/** The revision the next snapshot would carry. */
	long revision() {
		return revision.get();
	}

	BrokerRuntimeDTO snapshot() {
		state.readLock().lock();
		try {
			BrokerRuntimeDTO dto = new BrokerRuntimeDTO();
			dto.name = state.registry().getName();
			dto.revision = revision.get();
			dto.takenAt = System.currentTimeMillis();
			dto.registrations = registrations();
			dto.catalog = catalog();
			dto.sessions = sessions();
			dto.delivery = delivery();
			dto.coldEntries = cold.coldCount();
			return dto;
		} finally {
			state.readLock().unlock();
		}
	}

	private List<RegistrationDTO> registrations() {
		List<RegistrationDTO> all = new ArrayList<>();
		for (ServiceRegistration registration : state.registrations()) {
			if (registration.isUnregistered()) {
				continue;
			}
			all.add(registrationOf(registration));
		}
		return all;
	}

	private RegistrationDTO registrationOf(ServiceRegistration registration) {
		RegistrationDTO dto = new RegistrationDTO();
		ServiceReference reference = registration.getReference();
		if (reference != null) {
			dto.referenceId = reference.getId();
			dto.contractFingerprint = propertyOf(reference, "ddsr.fingerprint");
			dto.implementationFingerprint = propertyOf(reference, "ddsr.impl.fingerprint");
		}
		ServiceImplementation implementation = registration.getImplementation();
		if (implementation != null) {
			dto.implementationId = implementation.getImplementationId();
			dto.version = implementation.getVersion();
			dto.contracts = new ArrayList<>();
			for (ServiceInterface contract : implementation.getServiceInterfaces()) {
				dto.contracts.add(contract.getName());
			}
			dto.flavors = new ArrayList<>();
			for (ServiceFlavor flavor : implementation.getFlavors()) {
				if (flavor.getKind() != null) {
					dto.flavors.add(flavor.getKind().getName());
				}
			}
			if (implementation.getUpdatePolicy() != null) {
				dto.updatePolicy = implementation.getUpdatePolicy().getName();
			}
		}
		ServiceProvider provider = registration.getProvider();
		dto.providerName = provider == null ? null : provider.getName();
		dto.heldBy = holdersOf(registration);
		Liveness.ProviderLease lease = liveness.leaseOf(registration);
		if (lease != null) {
			dto.lastHeartbeat = lease.lastHeartbeat().toEpochMilli();
			dto.heartbeatIntervalSeconds = lease.intervalSeconds();
		}
		return dto;
	}

	/**
	 * Who holds a lease on this registration.
	 *
	 * <p>Read off the registration's own {@code usingSessions}, which is
	 * the eOpposite of the sessions' acquisitions — the model already
	 * answers this question in the direction it is asked here.
	 */
	private static List<String> holdersOf(ServiceRegistration registration) {
		List<String> holders = new ArrayList<>();
		for (ConsumerSession session : registration.getUsingSessions()) {
			if (session.getConsumerId() != null) {
				holders.add(session.getConsumerId());
			}
		}
		return holders;
	}

	private List<CatalogEntryDTO> catalog() {
		List<CatalogEntryDTO> entries = new ArrayList<>();
		for (ServiceInterface contract : state.registry().getCatalog()) {
			CatalogEntryDTO dto = new CatalogEntryDTO();
			dto.name = contract.getName();
			dto.version = contract.getVersion();
			dto.status = contract.getStatus() == null ? null : contract.getStatus().getName();
			dto.deprecationReason = contract.getDeprecationReason();
			dto.fingerprint = ContractAddressing.fingerprint(contract);
			dto.implementations = servedBy(contract);
			entries.add(dto);
		}
		return entries;
	}

	private int servedBy(ServiceInterface contract) {
		int count = 0;
		for (ServiceRegistration registration : state.registrations()) {
			ServiceImplementation implementation = registration.getImplementation();
			if (!registration.isUnregistered() && implementation != null
					&& implementation.getServiceInterfaces().contains(contract)) {
				count++;
			}
		}
		return count;
	}

	private List<SessionDTO> sessions() {
		List<SessionDTO> all = new ArrayList<>();
		for (SessionSnapshot snapshot : sessions.all()) {
			ConsumerSession session = snapshot.session();
			SessionDTO dto = new SessionDTO();
			dto.consumerId = session.getConsumerId();
			dto.lastRenewal = session.getLastRenewal() == null ? 0 : session.getLastRenewal().getTime();
			dto.acquisitions = new ArrayList<>(snapshot.acquiredReferenceIds());
			dto.connected = sessions.isConnected(session.getConsumerId());
			all.add(dto);
		}
		return all;
	}

	private DeliveryDTO delivery() {
		DeliveryDTO dto = new DeliveryDTO();
		dto.dropped = delivery.droppedCount();
		dto.owesResync = delivery.owesResync();
		return dto;
	}

	private static String propertyOf(ServiceReference reference, String name) {
		for (Property property : reference.getProperties()) {
			if (name.equals(property.getName()) && property instanceof StringProperty text) {
				return text.getValue();
			}
		}
		return null;
	}
}
