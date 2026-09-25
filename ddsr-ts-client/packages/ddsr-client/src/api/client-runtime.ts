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

import type { LocatorState } from './service-locator';

/**
 * One implementation this runtime published — the twin of the Java
 * `PublishedDTO` (#167).
 */
export interface PublishedDTO {
  implementationId: string | undefined;
  version: string | undefined;
  contracts: string[];
  /** The broker's reference id, or undefined when it holds none. */
  referenceId: string | undefined;
  /** Whether the broker is still holding it. */
  live: boolean;
  /** What the broker objected to, as "SEVERITY: message", or undefined. */
  failure: string | undefined;
}

/**
 * One contract this runtime is bound to — the twin of the Java
 * `BindingDTO`.
 */
export interface BindingDTO {
  contract: string;
  /** The LDAP filter it was tracked with, when there was one. */
  filter: string | undefined;
  /** The registration it is bound to. */
  referenceId: string | undefined;
  /** Where it calls, when the flavor says. */
  endpoint: string | undefined;
  state: LocatorState;
}

/**
 * What a client runtime holds, as data — the twin of the Java
 * `ClientRuntimeDTO`, field for field, so that a watcher reports both
 * sides alike.
 */
export interface ClientRuntimeDTO {
  /** How this runtime names itself to the broker, when it does. */
  consumerId: string | undefined;
  /** How often this runtime has changed since it was created. */
  changeCount: number;
  /** When the snapshot was taken, in epoch milliseconds. */
  takenAt: number;
  /** The transports this runtime told the broker it speaks. */
  supportedFlavors: string[];
  published: PublishedDTO[];
  bindings: BindingDTO[];
  /**
   * Whether the event stream is being read right now. The broker sees a
   * subscription; only the consumer knows whether it is reading one.
   */
  eventStreamConnected: boolean;
  /** Which transport carries those events — `rest` or `mqtt`. */
  eventTransport: string | undefined;
}

/** The runtime of one client, as its watcher sees it. */
export interface ClientRuntime {
  /**
   * What the client holds right now. Taken fresh on every call, so a
   * watcher that asks at collection time never reports a stale value.
   */
  snapshot(): ClientRuntimeDTO;
}
