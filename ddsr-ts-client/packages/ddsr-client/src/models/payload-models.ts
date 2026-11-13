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

/**
 * Payload models (#256): the packages a contract's slots are typed with.
 *
 * A provider hands in the Ecore documents of its payload models before it
 * registers its contract, and pins each with the fingerprint the broker
 * answers - the broker computes it, so this side need not. A consumer
 * fetches what it does not have before it calls: a TypeScript node has no
 * generated model to fall back on, and a payload of a package it does not
 * know it cannot read.
 */

import type { EPackage } from '@emfts/core';
import { EPackageRegistry } from '@emfts/core';
import type { ServiceInterface } from '@ddsr/model';
import { DDSRFactory } from '@ddsr/model';
import { deserializeFromXmi } from '../xmi/xmi-support.js';
import { toArray } from '../internal/emf-util.js';

/** What a broker offers for payload models. */
export interface ModelBroker {
  /** PUT /models: the broker's fingerprint, or undefined when it takes no models. */
  handInModel(ecore: string): Promise<string | undefined>;
  /** GET /models: the Ecore, or undefined when the broker has no such version. */
  fetchModel(nsURI: string, fingerprint?: string): Promise<string | undefined>;
}

/**
 * Reads an Ecore document and registers its package, so that payloads of
 * it can be read from now on.
 */
export function registerModel(ecore: string): EPackage {
  const root = deserializeFromXmi(ecore, 'temp://payload-model.ecore');
  const ePackage = (Array.isArray(root) ? root[0] : root) as unknown as EPackage;
  const nsURI = (ePackage as unknown as { getNsURI?: () => string | null }).getNsURI?.();
  if (!nsURI) throw new Error('the document defines no package with an nsURI');
  EPackageRegistry.INSTANCE.set(nsURI, ePackage);
  return ePackage;
}

/** Whether this node can read payloads of a package. */
export function knowsModel(nsURI: string): boolean {
  return EPackageRegistry.INSTANCE.has(nsURI);
}

/**
 * Hands each Ecore document to the broker and pins it in the contract with
 * the broker's fingerprint. A pin the contract carries stays. A broker that
 * takes no models leaves the contract as it was.
 */
export async function pinModels(contract: ServiceInterface, ecores: string[], broker: ModelBroker): Promise<void> {
  for (const ecore of ecores) {
    const fingerprint = await broker.handInModel(ecore);
    if (fingerprint === undefined) return;
    const ePackage = registerModel(ecore);
    const nsURI = (ePackage as unknown as { getNsURI: () => string }).getNsURI();
    const models = (contract as unknown as { models: { push(m: unknown): void } & Iterable<unknown> }).models;
    if (toArray<{ nsURI?: string }>(models).some(model => model.nsURI === nsURI)) continue;
    const pinned = DDSRFactory.eINSTANCE.createModelReference();
    pinned.nsURI = nsURI;
    pinned.fingerprint = fingerprint;
    models.push(pinned);
  }
}

/**
 * Fetches and registers every model the contract pins that this node does
 * not know yet.
 *
 * @returns the nsURIs fetched
 */
export async function ensureModels(contract: ServiceInterface, broker: ModelBroker): Promise<string[]> {
  const fetched: string[] = [];
  for (const model of toArray<{ nsURI?: string | null; fingerprint?: string | null }>(
    (contract as unknown as { models?: unknown }).models)) {
    if (!model.nsURI || knowsModel(model.nsURI)) continue;
    const ecore = await broker.fetchModel(model.nsURI, model.fingerprint ?? undefined);
    if (ecore === undefined) {
      throw new Error(`the broker has no payload model ${model.nsURI}${model.fingerprint ? ` at ${model.fingerprint}` : ''}`);
    }
    registerModel(ecore);
    fetched.push(model.nsURI);
  }
  return fetched;
}
