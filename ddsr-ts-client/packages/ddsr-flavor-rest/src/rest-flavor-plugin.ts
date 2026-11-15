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

import type { FlavorPlugin, TokenSupplier } from '@ddsr/client';
import {
  CE_EXTENSION_CORRELATION_ID, CE_HEADER_PREFIX, CE_TYPE_INVOKE, DdsrClientError, DdsrServiceError,
  DdsrTransportError, apiKeyScheme, bearerScheme, demandsAuthentication, describing, isEObject, newEnvelope,
  resultValues, toArray, toHeaders,
} from '@ddsr/client';
import type {
  ServiceFlavor,
  ServiceOperationFlavor,
  ServiceOperation,
  RestExceptionBinding,
  RestFlavor,
  RestOperationFlavor,
} from '@ddsr/model';
import { FlavorKind, HttpMethod } from '@ddsr/model';
import { buildRequest } from './request-builder.js';
import { parseResponse } from './response-parser.js';
import { NO_TRACER, carrierOver, type CallTracer } from '@ddsr/telemetry';
import { STREAM_MEDIA_TYPE, eventsOf, streams, valuesOf } from './rest-streams.js';

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
  /**
   * Where this node's tokens come from (SECURITY.md §6), e.g. the same
   * `clientCredentials(...)` the client shows the broker. Asked per call
   * for the audience the target's bearer scheme names — never the
   * broker's token: a provider must not receive one it could replay.
   */
  tokenSupplier?: TokenSupplier;
  /**
   * The API keys this node presents to endpoints whose flavor offers an
   * `ApiKeyScheme` (#299), asked by host (`scheme://host[:port]`) and the
   * scheme's name; undefined when it holds none. Used when no bearer token
   * is. The twin of Java's ApiKeySupplier.
   */
  apiKeys?: ApiKeySupplier;
}

/** The key this node presents to one endpoint, or undefined (#299). */
export type ApiKeySupplier = (host: string, scheme: string) => string | undefined | Promise<string | undefined>;

/** An API key and where it travels. */
interface PlacedKey {
  in: string;
  name: string;
  value: string;
}

export class RestFlavorPlugin implements FlavorPlugin {
  readonly flavorKind = 'REST';
  /** A stream comes back as Server-Sent Events (#252). */
  readonly carriesStreams = true;
  private readonly fetchFn: typeof fetch;
  private readonly timeoutMillis: number;
  private readonly source: string;
  private readonly log: (message: string) => void;
  private readonly tracer: CallTracer;
  private readonly origin: string | undefined;
  private readonly tokenSupplier: TokenSupplier | undefined;
  private readonly apiKeys: ApiKeySupplier | undefined;

  constructor(fetchFnOrOptions?: typeof fetch | RestFlavorPluginOptions) {
    const options: RestFlavorPluginOptions =
      typeof fetchFnOrOptions === 'function' ? { fetchFn: fetchFnOrOptions } : (fetchFnOrOptions ?? {});
    this.fetchFn = options.fetchFn ?? globalThis.fetch.bind(globalThis);
    this.timeoutMillis = options.timeoutMillis ?? 10_000;
    this.source = `/consumer/${options.originLabel ?? 'ts'}`;
    this.log = options.log ?? ((m) => console.error(`[ddsr-rest] ${m}`));
    this.tracer = options.tracer ?? NO_TRACER;
    this.origin = options.origin;
    this.tokenSupplier = options.tokenSupplier;
    this.apiKeys = options.apiKeys;
  }

  /**
   * The token a call to this target carries: one of the audience its
   * bearer scheme names, or none without a scheme. A guarded operation
   * without a supplier fails here, saying why — the twin of the Java
   * RestServiceInvoker.bearerFor.
   */
  private async bearerFor(flavor: ServiceFlavor, operation: ServiceOperation): Promise<string | undefined> {
    const scheme = bearerScheme(flavor);
    if (!scheme) {
      return undefined;
    }
    if (!this.tokenSupplier) {
      if (demandsAuthentication(operation) && !apiKeyScheme(flavor)) {
        throw new DdsrClientError(
          `'${operation.name}' needs a bearer token of ${scheme.issuer}, and this node has no token supplier`);
      }
      return undefined;
    }
    const audiences = Array.from(scheme.audiences ?? []);
    return this.tokenSupplier(audiences[0]);
  }

  /**
   * The API key a call to this target presents, when its flavor offers an
   * API-key scheme and no bearer token is used: the one configured for the
   * host and scheme. A guarded operation without one fails here, saying
   * why — the twin of the Java RestServiceInvoker.apiKeyFor.
   */
  private async apiKeyFor(flavor: ServiceFlavor, operation: ServiceOperation, url: string): Promise<PlacedKey | undefined> {
    const scheme = apiKeyScheme(flavor);
    if (!scheme) {
      return undefined;
    }
    const host = new URL(url).origin;
    const value = this.apiKeys ? await this.apiKeys(host, scheme.name) : undefined;
    if (value === undefined || value === null) {
      if (demandsAuthentication(operation)) {
        throw new DdsrClientError(
          `'${operation.name}' needs an API key (scheme ${scheme.name}) for ${host}, and this node holds none — give the plugin apiKeys`);
      }
      return undefined;
    }
    // HEADER is the default literal and may be absent from a document
    const location = scheme.in ? String(scheme.in) : 'HEADER';
    return { in: location, name: scheme.parameterName, value };
  }

  private streamOf(operation: ServiceOperation, restOpFlavor: RestOperationFlavor, url: string,
    init: RequestInit): AsyncIterable<unknown> {
    return streamValues(this.fetchFn, this.timeoutMillis, operation, restOpFlavor, url, init);
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
    // A stream (Parameter.delivery) comes back as Server-Sent Events,
    // sent when the first value is asked for (#252).
    const streamed = streams(operation);
    const bearer = await this.bearerFor(flavor, operation);
    const key = bearer ? undefined : await this.apiKeyFor(flavor, operation, request.url);
    if (key?.in === 'QUERY') {
      const url = new URL(request.url);
      url.searchParams.append(key.name, key.value);
      request.url = url.toString();
    }
    // Binary mode (#101): the attributes as ce-* headers, the body
    // untouched. The envelope goes in FIRST, so a contract that binds a
    // parameter to a header wins over it — the call is what the
    // contract says, the envelope is what carries it.
    const envelope = newEnvelope(CE_TYPE_INVOKE, this.source);
    envelope.subject = operation.name ?? undefined;
    // A call whose body is a model object says what schema it adheres
    // to; one without a body names none.
    describing(envelope, request.body);
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
        ...(bearer ? { Authorization: `Bearer ${bearer}` } : {}),
        ...(key?.in === 'HEADER' ? { [key.name]: key.value } : {}),
        ...(key?.in === 'COOKIE' ? { Cookie: `${key.name}=${encodeURIComponent(key.value)}` } : {}),
      },
      ...(this.timeoutMillis > 0 && !streamed ? { signal: AbortSignal.timeout(this.timeoutMillis) } : {}),
    };
    if (streamed) {
      span.end();
      return this.streamOf(operation, restOpFlavor, request.url, init);
    }
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
      // A status the flavor binds to a declared error is that error: the
      // provider side answers a raised ServiceException with exactly this
      // status (RestDispatcher.failed), and the body is its message.
      const declared = toArray<RestExceptionBinding>(restOpFlavor?.exceptionBindings)
        .find(binding => Number(binding.status) === response.status && binding.exception);
      const failed = declared
        ? new DdsrServiceError(body || `${declared.exception.name} (${response.status})`,
          declared.exception, response.status)
        : new Error(`REST call failed: ${response.status} ${response.statusText} — ${request.url}\n${body}`);
      span.failed(failed);
      span.end();
      throw failed;
    }

    try {
      const answered = await parseResponse(response);
      const returned = operation.returnValue as { upperBound?: unknown; eType?: unknown } | undefined;
      if (returned && manyValued(returned.upperBound)
        && (isEObject(answered) || (Array.isArray(answered) && answered.every(isEObject)))) {
        // Many values (#302): every root of the return type, in order.
        return resultValues(Array.isArray(answered) ? answered : [answered], returned.eType);
      }
      return answered;
    } finally {
      span.end();
    }
  }
}

/**
 * The values of a stream answered over REST. The request goes out when
 * the first value is asked for; leaving the loop early aborts it, which
 * closes the connection - the provider's sign that nobody listens.
 */
async function* streamValues(
  fetchFn: typeof fetch, timeoutMillis: number, operation: ServiceOperation, restOpFlavor: RestOperationFlavor,
  url: string, init: RequestInit,
): AsyncGenerator<unknown> {
  const connection = new AbortController();
  const waiting = timeoutMillis > 0
    ? setTimeout(() => connection.abort(new Error(`no answer within ${timeoutMillis}ms`)), timeoutMillis)
    : undefined;
  let response: Response;
  try {
    response = await fetchFn(url, {
      ...init,
      headers: { ...(init.headers as Record<string, string>), Accept: STREAM_MEDIA_TYPE },
      signal: connection.signal,
    });
  } catch (error) {
    throw new DdsrTransportError(`invoking ${operation.name} at ${url} failed: ${describe(error)}`, error);
  } finally {
    clearTimeout(waiting);
  }
  try {
    if (!response.ok) {
      const body = await response.text();
      const declared = toArray<RestExceptionBinding>(restOpFlavor?.exceptionBindings)
        .find(binding => Number(binding.status) === response.status && binding.exception);
      throw declared
        ? new DdsrServiceError(body || `${declared.exception.name} (${response.status})`,
          declared.exception, response.status)
        : new Error(`REST call failed: ${response.status} ${response.statusText} — ${url}\n${body}`);
    }
    if (!response.body) {
      throw new DdsrTransportError(`the stream of ${operation.name} came without a body`);
    }
    yield* valuesOf(operation, eventsOf(response.body as unknown as AsyncIterable<Uint8Array>));
  } finally {
    connection.abort();
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

/** Whether a declared upper bound says more than one value. */
function manyValued(raw: unknown): boolean {
  if (raw === undefined || raw === null || raw === '') return false;
  const bound = typeof raw === 'number' ? raw : Number.parseInt(String(raw), 10);
  return Number.isFinite(bound) && bound !== 1;
}
