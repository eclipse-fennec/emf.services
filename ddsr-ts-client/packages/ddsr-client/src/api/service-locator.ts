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

import type {
  ServiceReference,
  ServiceImplementation,
  ServiceInterface,
  RestFlavor,
  ServiceFlavor,
} from '@ddsr/model';

/**
 * Result of a consumer lookup. Carries everything needed to invoke a
 * service: reference, implementation details, flavor info, and the
 * service interface model for dynamic invoke().
 */
export interface ServiceLocator {
  /** The ServiceReference from the broker. */
  readonly reference: ServiceReference;

  /** The implementation that backs this reference. */
  readonly implementation: ServiceImplementation;

  /** The ServiceInterface definition from the catalog. */
  readonly serviceInterface: ServiceInterface;

  /** First RestFlavor in implementation.flavors, or undefined. */
  restFlavor(): RestFlavor | undefined;

  /** All flavors on this implementation. */
  flavors(): ServiceFlavor[];

  /**
   * Dynamically invoke an operation by name. Works for any service,
   * known or unknown — no generated interface needed.
   *
   * The proxy resolves the operation in the ServiceInterface,
   * finds the matching OperationFlavor, validates parameters
   * against constraints, and dispatches via the FlavorPlugin.
   */
  invoke(operationName: string, params?: Record<string, unknown>): Promise<unknown>;
}

/** Why a tracked locator is not (or no longer) bound to a live registration. */
export type LocatorState = 'LIVE' | 'MODIFIED' | 'STALE' | 'REBIND';

/**
 * A {@link ServiceLocator} that follows its service (#57). Locators
 * returned by `find()` are bound to one registration and watch the
 * event stream for it: MODIFIED refreshes in place (same reference id);
 * UNREGISTERING/COLDIFIED marks it STALE (a lookup rehydrates it under a
 * fresh id); any other UNREGISTERING and RETIRED mark it REBIND (another
 * registration of the same interface/filter is chosen on next use);
 * UPGRADE_AVAILABLE rebinds only a greedy consumer. A transport failure
 * on invoke rebinds away from the failed registration and retries once.
 * Rebinding is lazy — nothing talks to the broker until the next use.
 */
export interface TrackedServiceLocator extends ServiceLocator {
  readonly state: LocatorState;
  readonly interfaceName: string;
  readonly filter: string | undefined;
  /** Re-resolves now; with excludeCurrent the bound registration is skipped even if still listed. */
  rebind(excludeCurrent: boolean): Promise<boolean>;
}
