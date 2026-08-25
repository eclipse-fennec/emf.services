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

import type { ServiceEvent } from '@ddsr/model';

/**
 * Transport-agnostic source of broker service events — the TS mirror of
 * the Java client's org.gecko.ddsr.client.EventSource. An implementation
 * exists per transport (SSE today, MQTT later); the SDK routes events to
 * listeners without knowing how they arrived.
 */
export interface EventSourceHandler {
  /** Called for every decoded ServiceEvent, on the delivery task. */
  onEvent(event: ServiceEvent): void;

  /**
   * Called after every successful (re)connect — including the FIRST
   * one — and awaited BEFORE events of that connection are delivered.
   * This is where the snapshot refresh happens (FR-Sync-Reconnect).
   */
  onStreamEstablished(): void | Promise<void>;
}

/** Handle for an open event subscription. */
export interface EventSubscription {
  close(): Promise<void>;
}

export interface DdsrEventSource {
  /**
   * Open the stream. May return undefined when the transport is not
   * available; the SDK will retry on the next listener registration.
   */
  open(handler: EventSourceHandler): EventSubscription | undefined;
}
