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

import type { EObject } from '@emfts/core';
import { deserializeFromXmi } from './xmi-support';

/**
 * Parses a fetch Response into a usable result.
 *
 * - application/xml → XMI deserialization → EObject(s)
 * - application/json → JSON.parse
 * - otherwise → raw text
 */
export async function parseResponse(response: Response): Promise<unknown> {
  const contentType = response.headers.get('Content-Type') ?? '';

  if (contentType.includes('xml')) {
    const text = await response.text();
    if (!text.trim()) return undefined;
    return deserializeFromXmi(text);
  }

  if (contentType.includes('json')) {
    return response.json();
  }

  return response.text();
}
