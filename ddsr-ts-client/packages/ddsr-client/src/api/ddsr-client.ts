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

import type { DdsrProvider } from './ddsr-provider';
import type { DdsrConsumer } from './ddsr-consumer';
import type { DdsrCatalog } from './ddsr-catalog';

/**
 * Main entry point for the DDSR client framework.
 */
export interface DdsrClient {
  /** Provider-side API (publish / withdraw). */
  readonly provider: DdsrProvider;

  /** Consumer-side API (find / getService / listeners). */
  readonly consumer: DdsrConsumer;

  /** Catalog-side API (ensure/add/deprecate/remove entries). */
  readonly catalog: DdsrCatalog;

  /** The broker base URL this client talks to. */
  readonly brokerUrl: string;

  /**
   * Graceful shutdown: withdraws all live registrations and WAITS for
   * the broker's confirmation (consumers are informed before this
   * resolves — FR-P3), then closes the event stream. A provider must
   * keep its endpoint serving until close() resolves.
   */
  close(): Promise<void>;
}
