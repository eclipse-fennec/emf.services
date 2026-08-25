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

/** Get the eClass name of an EMF object, or undefined. */
export function eClassName(obj: unknown): string | undefined {
  if (obj && typeof obj === 'object' && 'eClass' in obj) {
    return (obj as any).eClass?.()?.getName?.();
  }
  return undefined;
}

/** Convert an EList or array-like to a plain array. */
export function toArray<T>(list: unknown): T[] {
  if (!list) return [];
  if (Array.isArray(list)) return list;
  // EList has size() and get()
  if (typeof (list as any).size === 'function') {
    const result: T[] = [];
    const size = (list as any).size();
    for (let i = 0; i < size; i++) {
      result.push((list as any).get(i));
    }
    return result;
  }
  // Iterable
  if (Symbol.iterator in (list as any)) {
    return [...(list as Iterable<T>)];
  }
  return [];
}

/** All roots of a deserialized document as a plain array. */
export function asRoots(result: unknown): unknown[] {
  if (result === undefined || result === null) return [];
  return Array.isArray(result) ? result : [result];
}

/** First root of the given eClass name, or undefined. */
export function firstOfClass<T>(roots: unknown[], className: string): T | undefined {
  return roots.find(r => eClassName(r) === className) as T | undefined;
}
