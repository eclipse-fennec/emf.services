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

import type { FlavorPlugin } from '@ddsr/client';
import { DdsrTransportError } from '@ddsr/client';
import type {
  ServiceFlavor,
  ServiceOperationFlavor,
  ServiceOperation,
  RestFlavor,
  RestOperationFlavor,
} from '@ddsr/model';
import { FlavorKind, HttpMethod } from '@ddsr/model';
import { buildRequest } from './request-builder';
import { parseResponse } from './response-parser';

/**
 * REST FlavorPlugin — translates ServiceOperation invocations into
 * fetch() calls based on RestFlavor and RestOperationFlavor metadata.
 *
 * Used for both broker communication and provider service calls.
 */
/** Construction options; a bare fetch function is still accepted for compatibility. */
export interface RestFlavorPluginOptions {
  fetchFn?: typeof fetch;
  /**
   * Per-invocation timeout in milliseconds (#59). A registered provider
   * that no longer answers surfaces as a DdsrTransportError instead of
   * a hang, and the tracked locator rebinds and retries once. 0 = none.
   * Default 10000.
   */
  timeoutMillis?: number;
}

export class RestFlavorPlugin implements FlavorPlugin {
  readonly flavorKind = 'REST';
  private readonly fetchFn: typeof fetch;
  private readonly timeoutMillis: number;

  constructor(fetchFnOrOptions?: typeof fetch | RestFlavorPluginOptions) {
    const options: RestFlavorPluginOptions =
      typeof fetchFnOrOptions === 'function' ? { fetchFn: fetchFnOrOptions } : (fetchFnOrOptions ?? {});
    this.fetchFn = options.fetchFn ?? globalThis.fetch.bind(globalThis);
    this.timeoutMillis = options.timeoutMillis ?? 10_000;
  }

  canHandle(flavor: ServiceFlavor): boolean {
    return (
      flavor.kind === FlavorKind.REST ||
      (flavor as any).eClass?.()?.getName?.() === 'RestFlavor'
    );
  }

  async invoke(
    operation: ServiceOperation,
    params: Record<string, unknown>,
    flavor: ServiceFlavor,
    operationFlavor: ServiceOperationFlavor
  ): Promise<unknown> {
    const restFlavor = flavor as RestFlavor;
    const restOpFlavor = operationFlavor as RestOperationFlavor;

    const request = buildRequest(operation, params, restFlavor, restOpFlavor);
    const init: RequestInit = this.timeoutMillis > 0
      ? { ...request.init, signal: AbortSignal.timeout(this.timeoutMillis) }
      : request.init;
    let response: Response;
    try {
      response = await this.fetchFn(request.url, init);
    } catch (error) {
      // fetch rejects (TypeError) when the peer is unreachable, and with an
      // AbortError/TimeoutError when the signal fires: the provider is
      // registered but not answering (#59).
      throw new DdsrTransportError(
        `invoking ${operation.name} at ${request.url} failed: ${describe(error)}`, error);
    }

    if (!response.ok) {
      const body = await response.text();
      throw new Error(
        `REST call failed: ${response.status} ${response.statusText} — ${request.url}\n${body}`
      );
    }

    return parseResponse(response);
  }
}

function describe(error: unknown): string {
  if (error instanceof Error) return error.name === 'TimeoutError' || error.name === 'AbortError'
    ? `timeout after the configured limit (${error.name})`
    : `${error.name}: ${error.message}`;
  return String(error);
}
