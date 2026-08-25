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
import { EResourceSetImpl, EPackageRegistry, URI, XMIResource } from '@emfts/core';
import { DDSRPackage } from '@ddsr/model';

// Register the DDSR package in the global EPackage registry so the XMI
// loader can resolve "ddsr:" prefixed elements to the correct EClasses.
const ddsr = DDSRPackage.eINSTANCE;
EPackageRegistry.INSTANCE.set(DDSRPackage.eNS_URI, ddsr);

function createResourceSet(): InstanceType<typeof EResourceSetImpl> {
  const rs = new EResourceSetImpl();
  rs.getPackageRegistry().set(DDSRPackage.eNS_URI, ddsr);
  return rs;
}

/**
 * Serialize one or more roots into a single XMI document. Multi-root
 * documents get an xmi:XMI wrapper; intra-document cross-references
 * are written as positional fragments ("/1", "//@operations.0") — the
 * same wire convention the Java broker uses.
 */
export function serializeToXmi(eObject: EObject, ...additionalRoots: EObject[]): string {
  const resourceSet = createResourceSet();
  const resource = new XMIResource(URI.createURI('temp://serialize.xmi'));
  resourceSet.getResources().push(resource);
  resource.getContents().push(eObject);
  for (const root of additionalRoots) {
    resource.getContents().push(root);
  }
  return resource.saveToString();
}

/**
 * Deserialize an XMI string. Returns the single root, or all roots for
 * a multi-root (xmi:XMI wrapped) document. Intra-document references
 * are resolved; cross-document hrefs are left as proxies and must be
 * interpreted from their URI alone (the broker never expects a client
 * to dereference them).
 */
export function deserializeFromXmi(xmiString: string): EObject | EObject[] {
  const resourceSet = createResourceSet();
  const resource = new XMIResource(URI.createURI('temp://deserialize.xmi'));
  resourceSet.getResources().push(resource);
  resource.loadFromString(xmiString);

  const contents = resource.getContents();
  const size = contents.size();
  if (size === 0) {
    throw new Error('XMI deserialization produced no content');
  }
  if (size === 1) {
    return contents.get(0);
  }
  const result: EObject[] = [];
  for (let i = 0; i < size; i++) {
    result.push(contents.get(i));
  }
  return result;
}
