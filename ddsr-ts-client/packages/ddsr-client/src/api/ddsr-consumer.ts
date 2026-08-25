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
  find(interfaceName: string, filter?: string): Promise<ServiceLocator[]>;

  /**
   * Convenience: find the best matching locator (first result), or undefined.
   */
  findOne(interfaceName: string, filter?: string): Promise<ServiceLocator | undefined>;

  /**
   * Get a typed service proxy. For known services with generated interfaces,
   * the returned proxy provides full type safety.
   *
   * For unknown services, use find() + locator.invoke() instead.
   */
  getService<T>(interfaceName: string, filter?: string): Promise<T | undefined>;

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
