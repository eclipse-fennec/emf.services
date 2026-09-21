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
import {
  CE_EXTENSION_CORRELATION_ID, CE_HEADER_PREFIX, CE_TYPE_INVOKE, DdsrTransportError,
  newEnvelope, toHeaders,
} from '@ddsr/client';
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
import { NO_TRACER, carrierOver, type CallTracer } from '@ddsr/telemetry';

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
  /**
   * Which system this is, for the CloudEvents `source` of every call
   * (#101). The same identity the origin header carries, worn twice
   * because the readers are different: the header is read by this
   * registry, the attribute by anything that reads CloudEvents.
   */
  originLabel?: string;
  log?: (message: string) => void;
  /**
   * Whoever is watching calls (#146). The invocation is the half a
   * cross-language trace hangs on: the context written here is what a
   * Java provider reads back out.
   */
  tracer?: CallTracer;
  /**
   * The origin token of the runtime this plugin belongs to (#125), for
   * the `X-DDSR-Origin` header of an invocation and for the span. The
   * Java client puts it on every call through one filter; here the
   * broker calls get it from the client and an invocation from this.
   */
  origin?: string;
}

export class RestFlavorPlugin implements FlavorPlugin {
  readonly flavorKind = 'REST';
  private readonly fetchFn: typeof fetch;
  private readonly timeoutMillis: number;
  private readonly source: string;
  private readonly log: (message: string) => void;
  private readonly tracer: CallTracer;
  private readonly origin: string | undefined;

  constructor(fetchFnOrOptions?: typeof fetch | RestFlavorPluginOptions) {
    const options: RestFlavorPluginOptions =
      typeof fetchFnOrOptions === 'function' ? { fetchFn: fetchFnOrOptions } : (fetchFnOrOptions ?? {});
    this.fetchFn = options.fetchFn ?? globalThis.fetch.bind(globalThis);
    this.timeoutMillis = options.timeoutMillis ?? 10_000;
    this.source = `/consumer/${options.originLabel ?? 'ts'}`;
    this.log = options.log ?? ((m) => console.error(`[ddsr-rest] ${m}`));
    this.tracer = options.tracer ?? NO_TRACER;
    this.origin = options.origin;
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
    // Binary mode (#101): the attributes as ce-* headers, the body
    // untouched. The envelope goes in FIRST, so a contract that binds a
    // parameter to a header wins over it — the call is what the
    // contract says, the envelope is what carries it.
    const envelope = newEnvelope(CE_TYPE_INVOKE, this.source);
    envelope.subject = operation.name ?? undefined;
    // The trace context travels as the W3C header, which is what an
    // instrumented provider reads — in any language. Written after the
    // span exists, because what travels must name THIS call.
    const context: Record<string, string> = {};
    const span = this.tracer.calling(spanNameOf(operation), carrierOver(context));
    span
      .attribute('rpc.system', 'fennec.services')
      .attribute('rpc.method', operation.name ?? undefined)
      .attribute('server.address', request.url)
      .attribute('fennec.flavor', 'REST')
      .attribute('fennec.origin', this.origin);
    const init: RequestInit = {
      ...request.init,
      headers: {
        ...toHeaders(envelope),
        ...(this.origin ? { 'X-DDSR-Origin': this.origin } : {}),
        ...(request.init.headers as Record<string, string>),
        ...context,
      },
      ...(this.timeoutMillis > 0 ? { signal: AbortSignal.timeout(this.timeoutMillis) } : {}),
    };
    let response: Response;
    try {
      response = await this.fetchFn(request.url, init);
      span.attribute('http.response.status_code', String(response.status));
    } catch (error) {
      span.failed(error);
      span.end();
      // fetch rejects (TypeError) when the peer is unreachable, and with an
      // AbortError/TimeoutError when the signal fires: the provider is
      // registered but not answering (#59).
      throw new DdsrTransportError(
        `invoking ${operation.name} at ${request.url} failed: ${describe(error)}`, error);
    }

    // An answer says which call it answers. Over HTTP the connection
    // already said it, so a mismatch cannot normally happen — which is
    // why it is worth a line if it ever does.
    const correlation = response.headers.get(`${CE_HEADER_PREFIX}${CE_EXTENSION_CORRELATION_ID}`);
    if (correlation && correlation !== envelope.id) {
      this.log(`the answer to ${operation.name} correlates with ${correlation},`
        + ` not with the request ${envelope.id}`);
    }

    if (!response.ok) {
      const body = await response.text();
      const failed = new Error(
        `REST call failed: ${response.status} ${response.statusText} — ${request.url}\n${body}`
      );
      span.failed(failed);
      span.end();
      throw failed;
    }

    try {
      return await parseResponse(response);
    } finally {
      span.end();
    }
  }
}

/**
 * What to call this call in a trace: the contract and the operation,
 * which is what a reader groups by and what the Java side calls the
 * serving half. Never the URL — a path with an id in it is a name
 * nobody can group by.
 */
function spanNameOf(operation: ServiceOperation): string {
  const contract = (operation as { eContainer?: () => unknown }).eContainer?.() as
    { name?: string } | undefined;
  return contract?.name ? `${contract.name}/${operation.name}` : String(operation.name);
}

function describe(error: unknown): string {
  if (error instanceof Error) return error.name === 'TimeoutError' || error.name === 'AbortError'
    ? `timeout after the configured limit (${error.name})`
    : `${error.name}: ${error.message}`;
  return String(error);
}
