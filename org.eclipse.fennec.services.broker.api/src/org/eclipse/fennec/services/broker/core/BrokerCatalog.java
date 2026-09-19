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

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceInterface;

/**
 * Governance-officer surface for the API catalog plus registry-state
 * reads. Mirrors the {@code BrokerCatalog} ServiceInterface published
 * by {@code BrokerSelfPublisher}.
 *
 * <p>All mutation methods return a {@link Diagnostic}: severity
 * {@code OK} means success; {@code WARNING} means success with a
 * non-blocking issue; {@code ERROR} / {@code CANCEL} mean the action
 * was refused — the registry state is then unchanged.
 */
public interface BrokerCatalog {

	/**
	 * Adds a new {@link ServiceInterface} to the catalog. Idempotent
	 * across identical content; rejects with a Diagnostic if an entry
	 * with the same name/version already exists.
	 *
	 * @param serviceInterface the interface to add; must be fully
	 *                         populated (operations, parameters,
	 *                         exceptions)
	 * @param requestor        symbolic identity that requested the
	 *                         change; carried through future hook
	 *                         pipelines
	 */
	Diagnostic addCatalogEntry(ServiceInterface serviceInterface, String requestor);

	/**
	 * Soft-deprecates an existing catalog entry. Implementations
	 * registered against it continue to work; subsequent publishes
	 * emit a {@code WARNING} diagnostic.
	 */
	Diagnostic deprecateCatalogEntry(ServiceInterface serviceInterface, String requestor);

	/**
	 * Strict removal: rejects with
	 * {@code Diagnostic(ERROR, code=CATALOG_HAS_LIVE_IMPLS)} while
	 * any implementation in the registry references the interface.
	 */
	Diagnostic removeCatalogEntry(ServiceInterface serviceInterface, String requestor);

	/**
	 * Returns the current registry root. The returned object is the
	 * live in-memory state; callers MUST treat it as read-only and
	 * MUST NOT mutate it outside the broker.
	 */
	RemoteServiceRegistry getRegistry();

	/**
	 * Forces a persistent snapshot of the current state to disk.
	 * Returns a Diagnostic describing the snapshot outcome.
	 */
	Diagnostic snapshot();
}
