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

import type { ServiceReference, ServiceImplementation, Diagnostic } from '@ddsr/model';

/**
 * Handle returned by DdsrProvider.publish(). The caller MUST keep this
 * handle — calling withdraw() removes the implementation from the broker.
 */
export interface Registration {
  /** The assigned ServiceReference (with broker-generated id). */
  readonly reference: ServiceReference;

  /** The implementation that was published. */
  readonly implementation: ServiceImplementation;

  /** The diagnostic of the last broker interaction (publish/update/withdraw). */
  diagnostic(): Diagnostic;

  /**
   * Re-sends the implementation as an in-place modification (#55):
   * change flavors, properties, capabilities or description on the
   * model object, then call this. The broker keeps the reference id
   * and every consumer lease and emits MODIFIED; consumers refresh,
   * they do not rebind. The implemented contracts must not change —
   * that is a new publish (the broker answers CODE_IMPL_CONTRACT_CHANGED
   * = 214, HTTP 409).
   */
  update(): Promise<Diagnostic>;

  /**
   * Removes the implementation from the broker. Idempotent. Resolves
   * only after the broker confirmed the unregistration — i.e. after
   * consumers were informed (FR-P3); keep the service endpoint serving
   * until then.
   */
  withdraw(): Promise<Diagnostic>;
}
