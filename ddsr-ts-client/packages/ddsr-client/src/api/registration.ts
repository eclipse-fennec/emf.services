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

  /** The diagnostic of the last broker interaction (publish/withdraw). */
  diagnostic(): Diagnostic;

  /**
   * Removes the implementation from the broker. Idempotent. Resolves
   * only after the broker confirmed the unregistration — i.e. after
   * consumers were informed (FR-P3); keep the service endpoint serving
   * until then.
   */
  withdraw(): Promise<Diagnostic>;
}
