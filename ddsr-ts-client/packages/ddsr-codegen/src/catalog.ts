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

import { readFileSync } from 'node:fs';
import { asRoots, deserializeFromXmi, eClassName, toArray } from '@ddsr/client';
import type { ServiceInterface } from '@ddsr/model';

/**
 * Where the contracts come from: the broker's catalog endpoint, or a
 * document someone saved from it.
 *
 * A file is the form a build wants — the same input produces the same
 * output, and a generator run does not need a broker to be up. The URL
 * is for looking at what is actually deployed.
 */
export type CatalogSource = { url: string } | { file: string } | { xmi: string };

export async function loadCatalog(source: CatalogSource, fetchFn: typeof fetch = fetch): Promise<ServiceInterface[]> {
  const xmi = 'xmi' in source ? source.xmi
    : 'file' in source ? readFileSync(source.file, 'utf8')
    : await fetchCatalog(source.url, fetchFn);
  return interfacesOf(xmi);
}

async function fetchCatalog(url: string, fetchFn: typeof fetch): Promise<string> {
  const endpoint = url.replace(/\/+$/, '') + (url.includes('/catalog') ? '' : '/catalog');
  const response = await fetchFn(endpoint);
  if (!response.ok) {
    throw new Error(`catalog request failed: ${response.status} ${response.statusText} — ${endpoint}`);
  }
  return response.text();
}

/**
 * The ServiceInterfaces in a catalog document. The broker answers with a
 * RemoteServiceRegistry whose `catalog` holds them; a hand-saved file may
 * just as well carry them as roots, so both shapes are accepted.
 */
export function interfacesOf(xmi: string): ServiceInterface[] {
  const roots = asRoots(deserializeFromXmi(xmi));
  const found: ServiceInterface[] = [];
  for (const root of roots) {
    if (eClassName(root) === 'ServiceInterface') {
      found.push(root as ServiceInterface);
      continue;
    }
    const catalog = (root as { catalog?: unknown }).catalog;
    for (const entry of toArray<ServiceInterface>(catalog)) {
      if (entry) found.push(entry);
    }
  }
  return found;
}
