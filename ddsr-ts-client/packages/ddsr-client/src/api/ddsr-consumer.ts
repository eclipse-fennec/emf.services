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

import type { ServiceLocator } from './service-locator';
import type { DdsrServiceListener } from './service-listener';

/**
 * Consumer-side API. Find services and subscribe to lifecycle events.
 */
export interface DdsrConsumer {
  /**
   * Find all matching service locators for an interface name.
   * The consumer's supported flavors (derived from loaded FlavorPlugins)
   * are attached automatically.
   *
   * @param interfaceName  the ServiceInterface.name to look up
   * @param filter         optional LDAP filter (no-op for now, passed to broker)
   */
  find(interfaceName: string, filter?: string, fingerprint?: string): Promise<ServiceLocator[]>;

  /**
   * Convenience: find the best matching locator (first result), or undefined.
   */
  findOne(interfaceName: string, filter?: string, fingerprint?: string): Promise<ServiceLocator | undefined>;

  /**
   * Get a typed service proxy. For known services with generated interfaces,
   * the returned proxy provides full type safety.
   *
   * For unknown services, use find() + locator.invoke() instead.
   */
  getService<T>(interfaceName: string, filter?: string): Promise<T | undefined>;

  /**
   * Says that this consumer no longer uses a reference.
   *
   * A lookup makes the client claim the references it found, and the
   * session it renews carries those claims. A claim is what a
   * `DEPRECATE_AND_DRAIN` handover waits for: the predecessor stays
   * alive until the last consumer has let go. This is how to let go.
   *
   * Not calling it is safe and simply means the claim lasts until the
   * service disappears; the cost is a handover that waits for a
   * consumer which has in truth already moved on.
   *
   * @param referenceId the id of the reference, as
   *   `ServiceLocator.reference` reports it
   */
  release(referenceId: string): void;

  /**
   * Register a listener for lifecycle events of an interface. The event
   * stream opens lazily with the first listener; on every (re)connect a
   * snapshot is pulled before events flow (FR-Sync-Reconnect). Events
   * whose interfaces cannot be determined are delivered to every
   * listener (over-delivery beats a silently dropped UNREGISTERING).
   *
   * @returns an unsubscribe function
   */
  addServiceListener(
    interfaceName: string,
    filter: string | undefined,
    listener: DdsrServiceListener
  ): () => void;

  /** Remove all registrations of this listener. */
  removeServiceListener(listener: DdsrServiceListener): void;
}
