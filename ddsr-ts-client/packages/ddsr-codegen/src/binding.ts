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
import { dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { asRoots, deserializeFromXmi, eClassName, toArray } from '@ddsr/client';
import type { ServiceInterface } from '@ddsr/model';
import { interfacesOf } from './catalog.js';

/**
 * How the contracts are rendered in TypeScript: the model's
 * TypeScriptBinding, read into what the emitter needs. The features are
 * LanguageBinding's, the same ones java-types.mtl reads for Java, so one
 * binding document per language over the same contracts is what keeps
 * the two renderings comparable.
 */
export interface Binding {
  /** The npm name of the generated package ('@acme/payment'). */
  targetPackage?: string;
  /** Header lines without comment markers; the emitter wraps them. */
  fileHeader: string[];
  /** Symbolic names (exception types) and what they are called here. */
  typeMappings: TypeMapping[];
  /** Where the types of a referenced metamodel live: nsURI to module. */
  packageMappings: PackageMapping[];
}

export interface TypeMapping {
  neutralType: string;
  target: string;
  /** Whether the generator writes the target type, or it already exists. */
  generated: boolean;
}

export interface PackageMapping {
  nsURI: string;
  target: string;
}

/** What rendering without a binding means: nothing mapped, no header. */
export const NO_BINDING: Binding = { fileHeader: [], typeMappings: [], packageMappings: [] };

export interface LoadedBinding {
  binding: Binding;
  /** The contracts the binding renders, read from the documents it points at. */
  interfaces: ServiceInterface[];
}

/**
 * Read a TypeScriptBinding document and the contracts it references.
 *
 * The binding does not contain its contracts; it points at them, usually
 * as hrefs into the published contract documents beside it. Those are
 * read relative to the binding file, the way EMF would resolve them.
 */
export function loadBinding(file: string): LoadedBinding {
  const roots = asRoots(deserializeFromXmi(readFileSync(file, 'utf8'), pathToFileURL(resolve(file)).href));
  const root = roots[0] as Record<string, unknown> | undefined;
  const kind = eClassName(root);
  if (kind !== 'TypeScriptBinding') {
    throw new Error(`${file} holds a ${kind ?? 'nothing'}, not a TypeScriptBinding`);
  }

  const interfaces: ServiceInterface[] = [];
  for (const reference of toArray<{ eProxyURI?: () => { toString(): string } | null }>(root!.serviceInterfaces)) {
    const uri = reference.eProxyURI?.()?.toString();
    if (!uri) {
      throw new Error(`${file} references a contract it holds itself; a binding only points at contracts`);
    }
    const location = uri.split('#')[0];
    const document = location.startsWith('file:') ? fileURLToPath(location) : resolve(dirname(file), location);
    const found = interfacesOf(readFileSync(document, 'utf8'));
    if (found.length === 0) throw new Error(`${document} holds no ServiceInterface`);
    interfaces.push(...found);
  }

  return {
    binding: {
      targetPackage: text(root!.targetPackage),
      fileHeader: toArray<unknown>(root!.fileHeader).map(line => text(line) ?? ''),
      typeMappings: toArray<Record<string, unknown>>(root!.typeMappings).map(mapping => ({
        neutralType: text(mapping.neutralType) ?? '',
        target: text(mapping.target) ?? '',
        generated: mapping.generated === true || mapping.generated === 'true',
      })),
      packageMappings: toArray<Record<string, unknown>>(root!.packageMappings).map(mapping => ({
        nsURI: text(mapping.nsURI) ?? '',
        target: text(mapping.target) ?? '',
      })),
    },
    interfaces,
  };
}

function text(value: unknown): string | undefined {
  return value === undefined || value === null ? undefined : String(value);
}
