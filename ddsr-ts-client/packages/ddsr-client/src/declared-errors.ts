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

import type { Property, ServiceException, ServiceOperation } from '@ddsr/model';
import { propertyValue } from './properties.js';
import { toArray } from './internal/emf-util.js';

/**
 * How an error a contract declares is recognised - the one rule both
 * ends of every transport share, and the twin of DeclaredErrors in the
 * Java api bundle (#185).
 *
 * A contract names its errors symbolically and may give each a `code`
 * as a property. That code is the identity an error has on the wire: a
 * failing Diagnostic with the same code is that error. A transport
 * with a vocabulary of its own translates on top of it - REST binds
 * the error to a status - and one without, such as MQTT, carries the
 * Diagnostic and nothing else is needed.
 *
 * An error declared without a code cannot be recognised in a
 * Diagnostic. That is a statement about the contract, not a failure
 * here.
 */

/** The `code` an error declares, if it declares one. */
export function declaredCode(exception: ServiceException | undefined): number | undefined {
  for (const property of toArray<Property>(exception?.properties)) {
    if (property.name === 'code') {
      const value = propertyValue(property);
      return typeof value === 'number' ? value : undefined;
    }
  }
  return undefined;
}

/**
 * The error of this operation a Diagnostic of that code is. Zero is no
 * code: it is what a Diagnostic carries when nobody gave it one.
 */
export function declaredByCode(
  operation: ServiceOperation | undefined, code: number | undefined,
): ServiceException | undefined {
  if (!code) return undefined;
  return toArray<ServiceException>(operation?.exceptions)
    .find(exception => declaredCode(exception) === code);
}

/**
 * Whether a thrown failure is the declared error. The contract names an
 * error symbolically, so the match is by name: the error's class name
 * or its `name`, against the last segment of the symbolic type with or
 * without an `Exception` suffix. A class a binding generated carries
 * the symbolic type itself and matches by it.
 */
export function raisesDeclared(failure: unknown, exception: ServiceException | undefined): boolean {
  const symbolic = exception?.type;
  if (!symbolic || !(failure instanceof Error)) return false;
  if ((failure.constructor as { type?: unknown }).type === symbolic) return true;
  const simple = symbolic.slice(symbolic.lastIndexOf('.') + 1);
  const names = [failure.constructor.name, failure.name];
  return names.includes(simple) || names.includes(`${simple}Exception`);
}

/** The error of this operation a thrown failure is, if it is one. */
export function declaredRaisedBy(
  operation: ServiceOperation | undefined, failure: unknown,
): ServiceException | undefined {
  return toArray<ServiceException>(operation?.exceptions)
    .find(exception => raisesDeclared(failure, exception));
}
