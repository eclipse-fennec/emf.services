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

import type {
  Parameter,
  RestExceptionBinding,
  RestFlavor,
  RestOperationFlavor,
  RestParameterBinding,
  ServiceOperation,
} from '@ddsr/model';
import { HttpMethod, ParameterBinding } from '@ddsr/model';
import { bearerScheme, demandsAuthentication, effectiveRequirement } from '@ddsr/client';
import type { JwtVerifier } from '@ddsr/client';

/**
 * The provider side of a REST flavor, in TypeScript (#154).
 *
 * The inverse of `buildRequest`, and deliberately built from the same
 * statements: where an argument travels is the flavor's, so the side
 * that reads a value back reads it from the same `parameterBindings`
 * the caller wrote it with. Nothing here is written per contract — this
 * is the twin of the Java `RestDispatcher`, and the reason a provider
 * can be an object and a document.
 *
 * What it does not do is HTTP: it takes a request as four plain fields
 * and answers with a status and a body, so it fits a `node:http`
 * server, a test, or anything else that speaks those.
 */

/** A request, reduced to what a dispatch decision needs. */
export interface DispatchRequest {
  method: string;
  /** The path, without scheme and authority; a query string is allowed. */
  url: string;
  headers?: Record<string, string | string[] | undefined>;
  /** The body, already read. */
  body?: string;
}

export interface DispatchResult {
  status: number;
  body?: string;
  contentType: string;
  /** Headers the answer needs beyond its content type — `WWW-Authenticate` on a 401. */
  headers?: Record<string, string>;
}

/**
 * An object whose methods are named after the contract's operations.
 * The arguments arrive in the order the contract declares, which is
 * what makes a plain class an implementation.
 */
// `never[]` rather than `unknown[]`: a method declared with the types
// its contract gives it — `charge(amount: number, currency?: string)` —
// is only assignable this way. A target typed with `unknown[]` would
// force every implementation to take unknowns and cast, which is the
// opposite of what a typed contract is for.
export type OperationTarget = Record<string, (...args: never[]) => unknown | Promise<unknown>>;

export interface DispatchOptions {
  /** Where the flavor is mounted, if the request URLs carry a prefix. */
  basePath?: string;
  log?: (message: string) => void;
  /**
   * Checks the bearer token of a call (SECURITY.md §7b), e.g.
   * `createJwtVerifier(...)`. Needed when the flavor names a bearer
   * scheme: a dispatcher that cannot verify what its flavor announces is
   * not built — fail closed.
   */
  verifier?: JwtVerifier;
  /**
   * The mode, when it is not what the flavor implies. `bearer` turns
   * every call without a token away, `bearer-optional` lets one through
   * without a principal for the contract to decide per operation (what a
   * `@PermitAll` operation needs), `none` serves a guarded contract open
   * — the explicit choice of a node for development, said once in the
   * log. By default: `bearer` when every operation is guarded,
   * `bearer-optional` otherwise.
   */
  auth?: 'bearer' | 'bearer-optional' | 'none';
}

/**
 * Builds the dispatcher for one flavor.
 *
 * Answers `undefined` when no operation of this flavor matches, so a
 * host can fall through to whatever else it serves rather than being
 * told 404 by a component that only knows one contract.
 */
export function restDispatcher(
  flavor: RestFlavor,
  implementation: OperationTarget,
  options: DispatchOptions = {}
): (request: DispatchRequest) => Promise<DispatchResult | undefined> {
  const basePath = (options.basePath ?? flavor.basePath ?? '').replace(/\/+$/, '');
  const log = options.log ?? (() => {});
  const mode = modeOf(flavor, options, log);

  return async (request: DispatchRequest) => {
    const [rawPath, rawQuery] = request.url.split('?', 2);
    const path = decodePath(rawPath, basePath);
    if (path === undefined) {
      return undefined;
    }
    const match = matchOperation(flavor, request.method, path);
    if (!match) {
      return undefined;
    }
    const operation = match.opFlavor.operation as ServiceOperation | undefined;
    if (!operation?.name) {
      return { status: 500, body: 'the flavor binds no operation', contentType: 'text/plain' };
    }

    // Before the arguments are read: a caller who may not call the
    // operation learns nothing about what it would have accepted.
    if (mode !== 'none') {
      const refused = await refusal(operation, request.headers ?? {}, mode, options.verifier!, log);
      if (refused) {
        return refused;
      }
    }

    const args = readArguments(match.opFlavor, operation, match.pathValues,
      new URLSearchParams(rawQuery ?? ''), request.headers ?? {}, request.body);

    const missing = parametersOf(operation).find(p => !p.optional && args[p.name ?? ''] === undefined);
    if (missing) {
      return { status: 400, body: `${missing.name} is required`, contentType: 'text/plain' };
    }

    const method = implementation[operation.name];
    if (typeof method !== 'function') {
      return {
        status: 501,
        body: `the implementation has no operation '${operation.name}'`,
        contentType: 'text/plain',
      };
    }

    try {
      const ordered = parametersOf(operation).map(p => args[p.name ?? '']);
      const answer = await (method as (...args: unknown[]) => unknown).apply(implementation, ordered);
      return answerOf(match.opFlavor, answer);
    } catch (failure) {
      // A failing implementation is an answer, not a dropped request:
      // the caller is waiting, and silence would cost it a timeout to
      // learn nothing.
      log(`${operation.name} failed: ${String(failure)}`);
      return {
        status: declaredStatus(match.opFlavor, failure) ?? 500,
        body: String((failure as Error)?.message ?? failure),
        contentType: 'text/plain',
      };
    }
  };
}

type Mode = 'bearer' | 'bearer-optional' | 'none';

/**
 * How this flavor is guarded, decided once — the twin of the Java
 * ServedSecurity: a bearer scheme needs a verifier, a guarded contract
 * without a scheme is served open only when that is said.
 */
function modeOf(flavor: RestFlavor, options: DispatchOptions, log: (m: string) => void): Mode {
  const operations = toArray<RestOperationFlavor>(flavor.operationFlavors).map(of => of.operation as ServiceOperation | undefined);
  const guarded = operations.filter(op => demandsAuthentication(op)).length;
  if (options.auth === 'none') {
    if (guarded > 0) {
      log(`${flavor.name} demands authentication but is served in mode none — its requirements are passed over`);
    }
    return 'none';
  }
  if (bearerScheme(flavor) || options.auth) {
    if (!options.verifier) {
      throw new Error(`${flavor.name} names a bearer scheme, and the dispatcher has no verifier — not served`);
    }
    return options.auth ?? (guarded === operations.length && guarded > 0 ? 'bearer' : 'bearer-optional');
  }
  if (guarded > 0) {
    throw new Error(`${flavor.name} demands authentication, and its flavor offers no scheme to prove it with`
      + ' — not served (say auth: \'none\' for an open node)');
  }
  return 'none';
}

/**
 * The contract's requirement for this operation, held against the
 * verified caller: 401 without a valid token where one is needed, 403
 * naming the scopes (roles) the caller lacks. A token that is there and
 * does not verify is 401 in both bearer modes.
 */
async function refusal(operation: ServiceOperation, headers: Record<string, string | string[] | undefined>,
  mode: Mode, verifier: JwtVerifier, log: (m: string) => void): Promise<DispatchResult | undefined> {
  const unauthorized: DispatchResult = {
    status: 401, contentType: 'text/plain', headers: { 'WWW-Authenticate': 'Bearer' },
  };
  const header = headerValue(headers, 'authorization');
  let scopes: readonly string[] | undefined;
  if (header !== undefined) {
    if (!/^bearer\s+\S+/i.test(header)) {
      return unauthorized;
    }
    try {
      scopes = (await verifier.verify(header.replace(/^bearer\s+/i, '').trim())).scopes;
    } catch (rejected) {
      // The reason is for the log, never for the caller.
      log(`401 ${operation.name}: ${String((rejected as Error)?.message ?? rejected)}`);
      return unauthorized;
    }
  } else if (mode === 'bearer') {
    return unauthorized;
  }
  if (!demandsAuthentication(operation)) {
    return undefined;
  }
  if (scopes === undefined) {
    return unauthorized;
  }
  const required = toArray<string>(effectiveRequirement(operation)?.scopes);
  const missing = required.filter(scope => !scopes!.includes(scope));
  return missing.length > 0
    ? { status: 403, body: `${operation.name} needs ${missing.join(', ')}`, contentType: 'text/plain' }
    : undefined;
}

/**
 * The status the flavor binds to the declared error this failure is, if
 * it is one - the twin of RestDispatcher.failed(). The contract names an
 * error symbolically, so the match is by name: the error's class name or
 * its `name`, against the last segment of the symbolic type with or
 * without an `Exception` suffix, as RestDispatcher.raises() does. A class
 * a binding generated carries the symbolic type itself and matches by it.
 */
function declaredStatus(opFlavor: RestOperationFlavor, failure: unknown): number | undefined {
  if (!(failure instanceof Error)) return undefined;
  const generatedType = (failure.constructor as { type?: unknown }).type;
  const names = new Set([failure.constructor.name, failure.name]);
  for (const binding of toArray<RestExceptionBinding>(opFlavor.exceptionBindings)) {
    const symbolic = binding.exception?.type;
    if (!symbolic) continue;
    const simple = symbolic.slice(symbolic.lastIndexOf('.') + 1);
    if (generatedType === symbolic || names.has(simple) || names.has(`${simple}Exception`)) {
      return Number(binding.status);
    }
  }
  return undefined;
}

/** The path below the mount point, or undefined when the request is elsewhere. */
function decodePath(rawPath: string, basePath: string): string | undefined {
  const path = rawPath.startsWith('//') ? rawPath.slice(1) : rawPath;
  if (basePath === '') {
    return path;
  }
  if (path === basePath) {
    return '';
  }
  return path.startsWith(`${basePath}/`) ? path.slice(basePath.length) : undefined;
}

interface Match {
  opFlavor: RestOperationFlavor;
  pathValues: Record<string, string>;
}

/**
 * Which operation a method and a path mean.
 *
 * Templates are matched segment by segment, with `{name}` taking one
 * segment — the same rule the consumer fills them by. A literal
 * segment wins nothing special: a flavor whose two operations could
 * both match one request is a flavor with a problem, and the first
 * declared one answers.
 */
function matchOperation(flavor: RestFlavor, method: string, path: string): Match | undefined {
  const wanted = method.toUpperCase();
  for (const opFlavor of toArray<RestOperationFlavor>(flavor.operationFlavors)) {
    if (httpMethodString(opFlavor.method) !== wanted) {
      continue;
    }
    const pathValues = matchTemplate(opFlavor.path ?? '', path);
    if (pathValues) {
      return { opFlavor, pathValues };
    }
  }
  return undefined;
}

function matchTemplate(template: string, path: string): Record<string, string> | undefined {
  const wanted = segmentsOf(template);
  const got = segmentsOf(path);
  if (wanted.length !== got.length) {
    return undefined;
  }
  const values: Record<string, string> = {};
  for (let i = 0; i < wanted.length; i++) {
    const segment = wanted[i];
    if (segment.startsWith('{') && segment.endsWith('}')) {
      values[segment.slice(1, -1)] = decodeURIComponent(got[i]);
      continue;
    }
    if (segment !== got[i]) {
      return undefined;
    }
  }
  return values;
}

function segmentsOf(path: string): string[] {
  return path.split('/').filter(segment => segment.length > 0);
}

/**
 * Every argument, read from where the flavor says it travels.
 *
 * A parameter without a binding keeps the older convention — a query
 * parameter, or the body when the operation declares exactly one
 * unbound parameter and a body arrived. That mirrors what the consumer
 * does with an undeclared parameter, so the two stay symmetric even
 * where the model says nothing.
 */
function readArguments(
  opFlavor: RestOperationFlavor,
  operation: ServiceOperation,
  pathValues: Record<string, string>,
  query: URLSearchParams,
  headers: Record<string, string | string[] | undefined>,
  body: string | undefined
): Record<string, unknown> {
  const bindings = toArray<RestParameterBinding>(opFlavor.parameterBindings);
  const parameters = parametersOf(operation);
  const args: Record<string, unknown> = {};
  const unbound: Parameter[] = [];

  for (const parameter of parameters) {
    const name = parameter.name ?? '';
    const binding = bindings.find(b => boundParameterName(b, parameters) === name);
    if (!binding) {
      unbound.push(parameter);
      continue;
    }
    const wireName = binding.wireName && binding.wireName.trim().length > 0 ? binding.wireName : name;
    switch (binding.binding) {
      case ParameterBinding.PATH:
        args[name] = coerce(pathValues[wireName], parameter);
        break;
      case ParameterBinding.QUERY:
        args[name] = coerce(query.get(wireName) ?? undefined, parameter);
        break;
      case ParameterBinding.HEADER:
        args[name] = coerce(headerValue(headers, wireName), parameter);
        break;
      default:
        args[name] = body === undefined || body === '' ? undefined : body;
        break;
    }
  }

  for (const parameter of unbound) {
    const name = parameter.name ?? '';
    const fromQuery = query.get(name);
    if (fromQuery !== null) {
      args[name] = coerce(fromQuery, parameter);
      continue;
    }
    if (unbound.length === 1 && body !== undefined && body !== '') {
      args[name] = body;
    }
  }
  return args;
}

function headerValue(
  headers: Record<string, string | string[] | undefined>,
  name: string
): string | undefined {
  const wanted = name.toLowerCase();
  for (const [key, value] of Object.entries(headers)) {
    if (key.toLowerCase() === wanted) {
      return Array.isArray(value) ? value[0] : value;
    }
  }
  return undefined;
}

/**
 * A value in the shape the contract declares.
 *
 * Only the primitives the contract names — a modelled type stays the
 * text it arrived as, because decoding it needs the resource set the
 * caller has and this module deliberately does not.
 */
function coerce(value: string | undefined, parameter: Parameter): unknown {
  if (value === undefined) {
    return undefined;
  }
  switch ((parameter.type ?? '').toLowerCase()) {
    case 'int':
    case 'integer':
    case 'long':
    case 'short':
      return Number.parseInt(value, 10);
    case 'double':
    case 'float':
      return Number.parseFloat(value);
    case 'boolean':
      return value === 'true';
    default:
      return value;
  }
}

/**
 * What to answer.
 *
 * No result is 204: an operation that returns nothing has nothing to
 * put in a body, and an empty 200 says something was computed. The
 * status of a result is the flavor's first declared success code,
 * because the flavor is where a contract states what it answers.
 */
function answerOf(opFlavor: RestOperationFlavor, answer: unknown): DispatchResult {
  const produces = toArray<string>(opFlavor.produces)[0] ?? 'text/plain';
  if (answer === undefined || answer === null) {
    return { status: 204, contentType: produces };
  }
  // `returnCodes` is multi-valued: the flavor may declare 200 and 204
  // for one operation, and the first is the one with a body.
  const codes = toArray<string>(opFlavor.returnCodes)
    .flatMap(code => String(code).trim().split(/\s+/))
    .filter(code => code.length > 0);
  const status = codes.length > 0 ? Number.parseInt(codes[0], 10) : 200;
  return {
    status: Number.isFinite(status) ? status : 200,
    body: typeof answer === 'string' ? answer : String(answer),
    contentType: produces,
  };
}

function parametersOf(operation: ServiceOperation): Parameter[] {
  return toArray<Parameter>(operation.parameters)
    .slice()
    .sort((a, b) => (a.index ?? 0) - (b.index ?? 0));
}

/**
 * The parameter a binding refers to. It may arrive as an unresolved
 * proxy — the flavor travels while the contract stays behind its
 * catalog URL — so the positional fragment is the fallback, exactly as
 * on the consumer side.
 */
function boundParameterName(binding: RestParameterBinding, parameters: Array<{ name?: string }>): string {
  const target = binding.parameter as { name?: string; eProxyURI?: () => { fragment?: string } } | undefined;
  if (target?.name) {
    return target.name;
  }
  const fragment = (binding.parameter as { eProxyURI?: () => { fragment?: string } } | undefined)
    ?.eProxyURI?.()?.fragment;
  const index = fragment ? Number.parseInt(fragment.replace(/.*@parameters\.(\d+).*/, '$1'), 10) : NaN;
  return Number.isFinite(index) ? (parameters[index]?.name ?? '') : '';
}

function httpMethodString(method: unknown): string {
  if (typeof method === 'string') {
    return method.toUpperCase();
  }
  if (method === HttpMethod.POST) return 'POST';
  if (method === HttpMethod.PUT) return 'PUT';
  if (method === HttpMethod.DELETE) return 'DELETE';
  if (method === HttpMethod.PATCH) return 'PATCH';
  return 'GET';
}

function toArray<T>(value: unknown): T[] {
  if (!value) return [];
  if (Array.isArray(value)) return value as T[];
  const list = value as { size?: () => number; get?: (i: number) => T } & Iterable<T>;
  if (typeof list.size === 'function' && typeof list.get === 'function') {
    return Array.from({ length: list.size() }, (_, i) => list.get!(i));
  }
  return [...list];
}
