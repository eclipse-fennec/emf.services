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
 * What a contract asks of a caller, operation by operation — the twin of
 * the Java `SecurityRequirements` (SECURITY.md §2): the operation's own
 * requirement where it states one, else the interface's default.
 */
import type {
  BearerTokenScheme,
  SecurityRequirement,
  ServiceFlavor,
  ServiceInterface,
  ServiceOperation,
} from '@ddsr/model';

/** The requirement that applies to the operation, or undefined when neither it nor its interface states one. */
export function effectiveRequirement(operation: ServiceOperation | undefined): SecurityRequirement | undefined {
  if (!operation) {
    return undefined;
  }
  if (operation.security) {
    return operation.security;
  }
  const container = (operation as unknown as { eContainer?: () => unknown }).eContainer?.();
  return (container as ServiceInterface | undefined)?.security ?? undefined;
}

/** Whether calling the operation needs a verified principal; scopes presuppose one. */
export function demandsAuthentication(operation: ServiceOperation | undefined): boolean {
  const requirement = effectiveRequirement(operation);
  return !!requirement && (requirement.authenticated || toArray(requirement.scopes).length > 0);
}

/** The first bearer scheme the flavor offers, or undefined. */
export function bearerScheme(flavor: ServiceFlavor | undefined): BearerTokenScheme | undefined {
  for (const scheme of toArray(flavor?.securitySchemes)) {
    const name = (scheme as unknown as { eClass?: () => { getName?: () => string } }).eClass?.()?.getName?.();
    if (name === 'BearerTokenScheme' || 'issuer' in (scheme as object)) {
      return scheme as BearerTokenScheme;
    }
  }
  return undefined;
}

function toArray<T>(list: Iterable<T> | undefined | null): T[] {
  return list ? Array.from(list) : [];
}
