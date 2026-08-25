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

import type { ServiceProvider, ServiceImplementation } from '@ddsr/model';
import type { Registration } from './registration';

/**
 * Provider-side API. Publish and withdraw service implementations
 * against the remote broker.
 */
export interface DdsrProvider {
  /**
   * Publishes an implementation into the broker. The implementation
   * MUST be contained in provider.implementations (EMF containment).
   *
   * Returns a Registration handle. WARNING diagnostics (e.g. deprecated
   * interface) are surfaced but the action still succeeds.
   *
   * @throws on ERROR diagnostics (e.g. interface not in catalog)
   */
  publish(provider: ServiceProvider, implementation: ServiceImplementation): Promise<Registration>;

  /** Look up a previously returned Registration by its reference id. */
  registrationOf(referenceId: string): Registration | undefined;
}
