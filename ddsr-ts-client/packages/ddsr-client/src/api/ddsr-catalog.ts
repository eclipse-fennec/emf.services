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

import type { Diagnostic, ServiceInterface } from '@ddsr/model';

/**
 * Catalog-side API — the TS counterpart of the Java BrokerCatalog
 * operations a provider needs before publishing.
 */
export interface DdsrCatalog {
  /**
   * Add the entry if it does not exist yet; an ALREADY_EXISTS answer
   * (code 202) counts as success (idempotent provider activation, same
   * as the Java PaymentPublisher).
   *
   * @throws DdsrClientError on any other ERROR/CANCEL diagnostic
   */
  ensureEntry(serviceInterface: ServiceInterface, requestor?: string, options?: CatalogEntryOptions): Promise<Diagnostic>;

  /** POST /catalog — the raw add; returns the diagnostic unthrown. */
  addEntry(serviceInterface: ServiceInterface, requestor?: string, options?: CatalogEntryOptions): Promise<Diagnostic>;

  /**
   * GET /catalog/{name} — undefined when the entry does not exist. The
   * optional sd1 fingerprint addresses one of several coexisting
   * contracts of the same name; a bare name works while it is
   * unambiguous. The same holds for deprecateEntry and removeEntry.
   */
  getEntry(name: string, fingerprint?: string): Promise<ServiceInterface | undefined>;

  /** PUT /catalog/{name}/deprecate — returns the diagnostic unthrown. */
  deprecateEntry(name: string, details?: ServiceInterface, requestor?: string, fingerprint?: string): Promise<Diagnostic>;

  /**
   * DELETE /catalog/{name} — returns the diagnostic unthrown. A contract
   * nobody implements can be removed; while an implementation (live or
   * parked) still uses it, the broker refuses with code 200
   * (CATALOG_HAS_LIVE_IMPLS, HTTP 409). Needs the service-admin role.
   */
  removeEntry(name: string, requestor?: string, fingerprint?: string): Promise<Diagnostic>;
}

/** What goes to the broker with a catalog entry. */
export interface CatalogEntryOptions {
  /**
   * The Ecore documents of the payload models the contract's slots are
   * typed with (#256). Each is handed to the broker first and pinned in the
   * contract with the fingerprint the broker answers.
   */
  payloadModels?: string[];
}
