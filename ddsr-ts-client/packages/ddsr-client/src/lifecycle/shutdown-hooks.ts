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

import type { DdsrClient } from '../api/ddsr-client';

export interface ShutdownHookOptions {
  /** Called after the client closed (e.g. stop the HTTP server). */
  afterClose?: () => void | Promise<void>;
  /** Signals to handle; default SIGINT + SIGTERM. */
  signals?: NodeJS.Signals[];
  log?: (message: string) => void;
}

/**
 * Opt-in process shutdown hooks for Node providers: on SIGINT/SIGTERM
 * the client is closed FIRST — withdrawing all registrations and
 * waiting for the broker's confirmation, so consumers are informed
 * while the endpoint still serves (FR-P3) — and only then does
 * afterClose() run (typically the HTTP server shutdown) before the
 * process exits.
 *
 * Returns a detach function that removes the hooks (and does not close
 * the client).
 */
export function attachShutdownHooks(client: DdsrClient, options?: ShutdownHookOptions): () => void {
  const signals = options?.signals ?? ['SIGINT', 'SIGTERM'];
  const log = options?.log ?? ((m: string) => console.error(`[ddsr-shutdown] ${m}`));
  let shuttingDown = false;

  const handler = (signal: NodeJS.Signals) => {
    if (shuttingDown) return;
    shuttingDown = true;
    log(`${signal} received — withdrawing registrations before exit`);
    void (async () => {
      let exitCode = 0;
      try {
        await client.close();
        log('broker confirmed unregistration');
      } catch (error) {
        exitCode = 1;
        log(`close failed: ${String(error)}`);
      }
      try {
        await options?.afterClose?.();
      } catch (error) {
        exitCode = 1;
        log(`afterClose failed: ${String(error)}`);
      }
      process.exit(exitCode);
    })();
  };

  for (const signal of signals) process.on(signal, handler);
  return () => {
    for (const signal of signals) process.off(signal, handler);
  };
}
