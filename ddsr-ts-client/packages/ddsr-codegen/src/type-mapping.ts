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
 * A DDSR parameter type is a string in the catalog — the contract is
 * language-neutral, so it names types the way both sides can read them.
 * This turns one into TypeScript.
 *
 * Three cases. The primitives map as you would expect. A name the
 * services model defines (ServiceInterface, Diagnostic, …) becomes an
 * import from @ddsr/model, because that type exists on this side
 * already. Anything else is a type this generator has never heard of,
 * and it stays `unknown`: a guess would compile and then be wrong at the
 * one place it matters.
 */
const PRIMITIVES: Record<string, string> = {
  string: 'string', String: 'string',
  double: 'number', float: 'number', int: 'number', integer: 'number',
  long: 'number', short: 'number', byte: 'number', number: 'number',
  boolean: 'boolean', bool: 'boolean',
  void: 'void',
  any: 'unknown', object: 'unknown',
};

/** Types the services model owns; imported rather than redeclared. */
const MODEL_TYPES = new Set([
  'ServiceInterface', 'ServiceOperation', 'ServiceProvider', 'ServiceImplementation',
  'ServiceReference', 'ServiceRegistration', 'ServiceEvent', 'ServiceRegistry',
  'LocalServiceRegistry', 'RemoteServiceRegistry', 'Diagnostic', 'Parameter',
  'Property', 'ServiceException', 'ConsumerSession', 'ConsumerCapability',
  'ComponentDescription', 'ComponentConfiguration', 'ServiceFlavor',
]);

export interface MappedType {
  /** The TypeScript type as it appears in the signature. */
  text: string;
  /** The @ddsr/model name to import, if any. */
  modelImport?: string;
}

export function mapType(ddsrType: string | undefined): MappedType {
  const raw = (ddsrType ?? '').trim();
  if (raw === '') return { text: 'unknown' };

  // list<string>, List<Payment>, string[] — a many-valued parameter.
  const list = /^(?:list|List|array|Array)<(.+)>$/.exec(raw) ?? /^(.+)\[\]$/.exec(raw);
  if (list) {
    const element = mapType(list[1]);
    return { text: `${element.text}[]`, modelImport: element.modelImport };
  }

  const primitive = PRIMITIVES[raw];
  if (primitive) return { text: primitive };

  // A fully qualified name names the same thing as its last segment:
  // org.eclipse.fennec.services.Diagnostic is our Diagnostic.
  const simple = raw.includes('.') ? raw.slice(raw.lastIndexOf('.') + 1) : raw;
  if (MODEL_TYPES.has(simple)) return { text: simple, modelImport: simple };

  return { text: 'unknown' };
}

/** A name the catalog carries, as a TypeScript identifier. */
export function identifier(name: string | undefined, fallback: string): string {
  const cleaned = (name ?? '').replace(/[^A-Za-z0-9_$]/g, '_');
  if (cleaned === '' || /^[0-9]/.test(cleaned)) return fallback;
  return cleaned;
}
