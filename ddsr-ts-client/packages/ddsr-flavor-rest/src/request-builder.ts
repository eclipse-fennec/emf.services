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

import type { ServiceOperation, RestFlavor, RestOperationFlavor, RestParameterBinding } from '@ddsr/model';
import { HttpMethod, ParameterBinding } from '@ddsr/model';
import type { EObject } from '@emfts/core';
import { serializeToXmi } from './xmi-support';

export interface RestRequest {
  url: string;
  init: RequestInit;
}

/**
 * Builds a fetch-compatible request from DDSR model metadata.
 *
 * Where an argument travels is the provider's statement: a
 * RestParameterBinding on the operation flavor says it per parameter —
 * PATH into a template segment, QUERY into the query string, HEADER as a
 * request header, BODY into the payload — under `wireName` where one is
 * given (#74). The mirror of the Java RestServiceInvoker, field for field.
 *
 * An argument whose parameter carries no binding keeps the older
 * convention: a single EObject becomes the XMI body, anything else a
 * query parameter. The model names BODY as the default, but there is no
 * encoding for several primitive arguments in one payload, so an
 * undeclared parameter travels the way it always has.
 */
export function buildRequest(
  _operation: ServiceOperation,
  params: Record<string, unknown>,
  flavor: RestFlavor,
  opFlavor: RestOperationFlavor
): RestRequest {
  const base = (flavor.host ?? '') + flavor.basePath;
  const method = httpMethodString(opFlavor.method);

  const placed = place(params, opFlavor);
  params = placed.rest;

  // Substitute {name}-style path templates: from the PATH bindings first,
  // then from any remaining argument of the same name, so a flavor that
  // declares nothing keeps working. Used arguments are consumed and do not
  // additionally appear in the query string.
  const usedInPath = new Set<string>();
  const path = (opFlavor.path ?? '').replace(/\{([^}]+)\}/g, (match, key: string) => {
    const value = placed.path[key] ?? params[key];
    if (value === undefined || value === null) return match;
    usedInPath.add(key);
    return encodeURIComponent(String(value));
  });
  if (usedInPath.size > 0) {
    params = Object.fromEntries(Object.entries(params).filter(([k]) => !usedInPath.has(k)));
  }

  // Use the produces hint from the OperationFlavor if available,
  // falling back to application/xml (broker APIs use XMI).
  const produces = opFlavor.produces;
  const producesList = produces && typeof (produces as any).size === 'function'
    ? Array.from({ length: (produces as any).size() }, (_, i) => (produces as any).get(i))
    : Array.isArray(produces) ? produces : [];
  const accept = producesList.length > 0 ? producesList[0] : 'application/xml';

  const headers: Record<string, string> = {
    'Accept': accept,
    ...Object.fromEntries(Object.entries(placed.header).map(([k, v]) => [k, String(v)])),
  };

  // A declared BODY argument wins; otherwise an undeclared EObject is the
  // body, as it always was.
  const bodyObj = findEObject(placed.body) ?? findEObject(params);

  if (bodyObj) {
    headers['Content-Type'] = 'application/xml';
    const body = serializeToXmi(bodyObj);
    const url = appendQueryParams(base + path, { ...placed.query, ...withoutEObjects(params) });
    return { url, init: { method, headers, body } };
  }

  const url = appendQueryParams(base + path, { ...placed.query, ...params });
  return { url, init: { method, headers } };
}

interface Placed {
  path: Record<string, unknown>;
  query: Record<string, unknown>;
  header: Record<string, unknown>;
  body: Record<string, unknown>;
  /** Arguments whose parameter carries no binding. */
  rest: Record<string, unknown>;
}

/** Sort the arguments into the places the flavor declares for them. */
function place(params: Record<string, unknown>, opFlavor: RestOperationFlavor): Placed {
  const placed: Placed = { path: {}, query: {}, header: {}, body: {}, rest: {} };
  const bindings = toArray<RestParameterBinding>(opFlavor.parameterBindings);

  for (const [name, value] of Object.entries(params)) {
    const binding = bindings.find(b => b.parameter?.name === name);
    if (!binding) {
      placed.rest[name] = value;
      continue;
    }
    const wireName = binding.wireName && binding.wireName.trim().length > 0 ? binding.wireName : name;
    switch (binding.binding) {
      case ParameterBinding.PATH: placed.path[wireName] = value; break;
      case ParameterBinding.QUERY: placed.query[wireName] = value; break;
      case ParameterBinding.HEADER: placed.header[wireName] = value; break;
      default: placed.body[wireName] = value; break;
    }
  }
  return placed;
}

/** Convert an EList or array-like to a plain array. */
function toArray<T>(list: unknown): T[] {
  if (!list) return [];
  if (Array.isArray(list)) return list;
  if (typeof (list as any).size === 'function') {
    const size = (list as any).size();
    return Array.from({ length: size }, (_, i) => (list as any).get(i) as T);
  }
  return [];
}

function httpMethodString(method: HttpMethod): string {
  switch (method) {
    case HttpMethod.GET: return 'GET';
    case HttpMethod.POST: return 'POST';
    case HttpMethod.PUT: return 'PUT';
    case HttpMethod.DELETE: return 'DELETE';
    case HttpMethod.PATCH: return 'PATCH';
    default: return 'GET';
  }
}

function appendQueryParams(url: string, params: Record<string, unknown>): string {
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== null);
  if (entries.length === 0) return url;
  const qs = entries
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&');
  return url + (url.includes('?') ? '&' : '?') + qs;
}

function withoutEObjects(params: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const [k, v] of Object.entries(params)) {
    if (!(v && typeof v === 'object' && 'eClass' in v)) {
      result[k] = v;
    }
  }
  return result;
}

function findEObject(params: Record<string, unknown>): EObject | undefined {
  for (const value of Object.values(params)) {
    if (value && typeof value === 'object' && 'eClass' in value && typeof (value as EObject).eClass === 'function') {
      return value as EObject;
    }
  }
  return undefined;
}
