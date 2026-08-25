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

import type { ServiceOperation, RestFlavor, RestOperationFlavor } from '@ddsr/model';
import { HttpMethod } from '@ddsr/model';
import type { EObject } from '@emfts/core';
import { serializeToXmi } from './xmi-support';

export interface RestRequest {
  url: string;
  init: RequestInit;
}

/**
 * Builds a fetch-compatible request from DDSR model metadata.
 *
 * - GET/DELETE: params go into query string
 * - POST/PUT: if params contain EObjects, serialize to XMI body;
 *   otherwise JSON body
 */
export function buildRequest(
  _operation: ServiceOperation,
  params: Record<string, unknown>,
  flavor: RestFlavor,
  opFlavor: RestOperationFlavor
): RestRequest {
  const base = (flavor.host ?? '') + flavor.basePath;
  const method = httpMethodString(opFlavor.method);

  // Substitute {name}-style path templates from params; used params
  // are consumed and do not additionally appear in the query string.
  const usedInPath = new Set<string>();
  const path = (opFlavor.path ?? '').replace(/\{([^}]+)\}/g, (match, key: string) => {
    const value = params[key];
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
  };

  // Check if params contain an EObject (XMI body for broker calls)
  const bodyObj = findEObject(params);

  if (bodyObj) {
    // EObject → XMI body (broker API calls)
    headers['Content-Type'] = 'application/xml';
    const body = serializeToXmi(bodyObj);
    const url = appendQueryParams(base + path, withoutEObjects(params));
    return { url, init: { method, headers, body } };
  }

  // Simple params → always query parameters, regardless of HTTP method.
  // Many JAX-RS services use @QueryParam even on POST (e.g. Payment.charge).
  const url = appendQueryParams(base + path, params);
  return { url, init: { method, headers } };
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
