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
export class RestFlavorPlugin implements FlavorPlugin {
  readonly flavorKind = 'REST';

  private readonly fetchFn: typeof fetch;

  constructor(fetchFn?: typeof fetch) {
    this.fetchFn = fetchFn ?? globalThis.fetch.bind(globalThis);
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
    const response = await this.fetchFn(request.url, request.init);

    if (!response.ok) {
      const body = await response.text();
      throw new Error(
        `REST call failed: ${response.status} ${response.statusText} — ${request.url}\n${body}`
      );
    }

    return parseResponse(response);
  }
}
