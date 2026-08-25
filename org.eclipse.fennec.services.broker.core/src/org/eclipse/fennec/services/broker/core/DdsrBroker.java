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

import org.eclipse.fennec.services.RemoteServiceRegistry;

/**
 * The DDSR broker — Java face of the Remote Service Registry. Composite
 * role-union of {@link BrokerCatalog}, {@link BrokerImplementations},
 * {@link BrokerLookup} and {@link BrokerSessions}; matches the three ServiceInterfaces
 * published into the catalog by {@code BrokerSelfPublisher}.
 *
 * <p>Embedded consumers should normally reference the narrowest role
 * interface they need (e.g. {@code @Reference BrokerLookup lookup})
 * — this composite exists for cross-cutting callers (e.g. snapshot
 * tooling) and as a single binding point during tests.
 *
 * <p>The broker maintains an in-memory {@link RemoteServiceRegistry}
 * persisted as XMI after every mutation, guards its state with a
 * read-write lock, and exposes the operations needed by providers,
 * consumers, and governance officers. HTTP/SSE-facing modules
 * (e.g. {@code broker.rest}) sit on top of these interfaces; they
 * do not reach into the model directly.
 *
 * <p>Threading: read operations may run concurrently; mutations are
 * exclusive (see {@code NFR-RemoteRegistry-Concurrency} in
 * REQUIREMENTS).
 */
public interface DdsrBroker extends BrokerCatalog, BrokerImplementations, BrokerLookup, BrokerSessions {
}
