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
 * Pluggable lookup engine. The broker delegates {@code getServiceReference[s]}
 * and {@code getAllServiceReferences} calls to a {@code LookupBackend}.
 *
 * <p>An {@code InMemoryLookupBackend} ships with the core module and is
 * sufficient for a small catalog. A separate module
 * {@code org.eclipse.fennec.services.broker.lookup.lucene} provides a Lucene-indexed
 * backend for larger deployments; the broker picks it up via DS service
 * binding without code changes here.
 *
 * <p>Implementations are responsible for:
 * <ul>
 * <li>keeping their index in sync with the in-memory
 *     {@link org.eclipse.fennec.services.RemoteServiceRegistry} via the
 *     {@link #serviceAdded(ServiceReference)} /
 *     {@link #serviceModified(ServiceReference)} /
 *     {@link #serviceRemoved(ServiceReference)} callbacks issued by the
 *     broker after every state mutation,</li>
 * <li>evaluating LDAP filters against the indexed service properties,</li>
 * <li>filtering candidates by the consumer's
 *     {@link ConsumerCapability#getSupportedFlavors() supported flavors}
 *     before returning results.</li>
 * </ul>
 *
 * <p>Methods MUST be thread-safe; the broker holds a read-lock during
 * lookup calls but a backend may receive concurrent read calls from
 * multiple threads.
 */
public interface LookupBackend {

	/**
	 * Returns one matching reference ordered by service ranking
	 * (descending) and tie-broken by service id (ascending). Returns
	 * {@code null} if no candidate matches the interface, the filter, and
	 * the consumer's supported flavors.
	 *
	 * @param interfaceName the {@code ServiceInterface.name} to look up;
	 *                      must not be {@code null}
	 * @param filter        optional LDAP filter over service properties;
	 *                      may be {@code null} or empty for no filter
	 * @param capability    consumer capability used for flavor matching;
	 *                      may be {@code null} for in-process callers
	 *                      that accept any flavor
	 * @return a matching reference, or {@code null} if none
	 */
	ServiceReference getServiceReference(String interfaceName, String filter, ConsumerCapability capability);

	/**
	 * Returns all matching references ordered by service ranking and
	 * service id, filtered by interface, LDAP filter, and consumer
	 * capability (flavor matching). Returns an empty list if nothing
	 * matches; never {@code null}.
	 */
	List<ServiceReference> getServiceReferences(String interfaceName, String filter, ConsumerCapability capability);

	/**
	 * Like {@link #getServiceReferences} but ignores consumer-visibility
	 * constraints. Used by administrative tooling — see the
	 * {@code getAllServiceReferences} contract on
	 * {@code ServiceRegistry}.
	 */
	List<ServiceReference> getAllServiceReferences(String interfaceName, String filter, ConsumerCapability capability);

	/**
	 * Notifies the backend that a new ServiceReference is now part of
	 * the registry. The {@code implementation} is passed alongside so
	 * the backend has access to flavors and declared service interfaces
	 * — this avoids relying on a back-link from reference to
	 * implementation that the model does not directly carry.
	 */
	void serviceAdded(ServiceImplementation implementation, ServiceReference reference);

	/**
	 * Notifies the backend that the reference's properties have changed.
	 * Implementations re-index or update the index entry.
	 */
	void serviceModified(ServiceImplementation implementation, ServiceReference reference);

	/**
	 * Notifies the backend that the reference is being removed from the
	 * registry. Implementations drop the index entry.
	 */
	void serviceRemoved(ServiceImplementation implementation, ServiceReference reference);
}
