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

import java.util.List;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceReference;

/**
 * Consumer-side surface: matching ServiceReferences for an interface
 * name (plus optional LDAP filter and flavor capability) and the
 * implementation lookup needed to bundle transport details. Mirrors
 * the {@code BrokerLookup} ServiceInterface published by
 * {@code BrokerSelfPublisher}.
 */
public interface BrokerLookup {

	/**
	 * Returns one matching {@link ServiceReference} by ranking +
	 * service-id, or {@code null} when nothing matches.
	 */
	ServiceReference getServiceReference(String interfaceName);

	/**
	 * Returns all matching references. The {@code capability}
	 * determines flavor-side filtering; pass {@code null} to receive
	 * results for any flavor (administrative usage only).
	 */
	List<ServiceReference> getServiceReferences(String interfaceName, String filter, ConsumerCapability capability);

	/**
	 * Like {@link #getServiceReferences} but bypasses
	 * visibility-constraints — administrative tooling only.
	 */
	List<ServiceReference> getAllServiceReferences(String interfaceName, String filter, ConsumerCapability capability);

	/**
	 * Returns the {@link ServiceImplementation} that owns the given
	 * reference, or {@code null} if the reference is unknown.
	 */
	ServiceImplementation getImplementationForReference(ServiceReference reference);
}
