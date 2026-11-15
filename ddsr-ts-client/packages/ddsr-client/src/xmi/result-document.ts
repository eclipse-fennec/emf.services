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

/**
 * What travels with an answer — the twin of Java's `ResultDocument`
 * (#88, #302).
 *
 * The roots of the document for an answer: the values in their order,
 * followed by every object they reference that belongs to nobody (no
 * container, no resource), and from those on the same terms, each object
 * once. A proxy is never followed.
 */
export function resultRoots(values: readonly EObject[]): EObject[] {
  const roots: EObject[] = [];
  const known = new Set<EObject>();
  const pending: EObject[] = [];
  for (const value of values) {
    if (value && !known.has(value)) {
      known.add(value);
      roots.push(value);
      pending.push(value);
    }
  }
  while (pending.length > 0) {
    const root = pending.shift()!;
    for (const referenced of referencedBy(root)) {
      if (!travelsWithTheAnswer(referenced) || known.has(referenced)) continue;
      known.add(referenced);
      roots.push(referenced);
      pending.push(referenced);
    }
  }
  return roots;
}

/**
 * The values of a many-valued answer (#302): every root of the return
 * type, in document order; the other roots are context. The type is
 * compared as `<nsURI>#//<Name>`, from a proxy's URI or the classifier
 * itself, and a root whose class inherits from it is a value too.
 * Without a type every root is a value.
 */
export function resultValues(roots: readonly EObject[], eType: unknown): EObject[] {
  const wanted = typeUri(eType);
  if (!wanted) return [...roots];
  return roots.filter(root => {
    const eClass = root.eClass() as unknown as { getEAllSuperTypes?: () => Iterable<unknown> };
    if (typeUri(eClass) === wanted) return true;
    const supers = eClass.getEAllSuperTypes?.();
    return supers ? [...supers].some(superType => typeUri(superType) === wanted) : false;
  });
}

/** Whether something is a model object rather than a value. */
export function isEObject(value: unknown): value is EObject {
  return typeof (value as { eClass?: unknown } | null)?.eClass === 'function';
}

function referencedBy(root: EObject): EObject[] {
  const referenced = [...root.eCrossReferences()];
  for (const contained of root.eAllContents()) {
    referenced.push(...contained.eCrossReferences());
  }
  return referenced;
}

function travelsWithTheAnswer(referenced: EObject | null | undefined): referenced is EObject {
  return !!referenced && !referenced.eIsProxy() && !referenced.eContainer() && !referenced.eResource();
}

function typeUri(eType: unknown): string | undefined {
  if (!eType) return undefined;
  const proxyURI = (eType as { eProxyURI?: () => { toString(): string } | null }).eProxyURI?.();
  if (proxyURI) return proxyURI.toString();
  const classifier = eType as {
    getEPackage?: () => { getNsURI?: () => string | undefined } | null;
    getName?: () => string | undefined;
  };
  const name = classifier.getName?.();
  if (!name) return undefined;
  return `${classifier.getEPackage?.()?.getNsURI?.() ?? ''}#//${name}`;
}
