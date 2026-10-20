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

import type { ServiceLocator } from '../api/service-locator.js';
import { toArray } from '../internal/emf-util.js';
import { DdsrServiceError } from '../api/errors.js';

/**
 * A calling surface for a located service: `service.charge(100, 'EUR')`
 * instead of `locator.invoke('charge', { amount: 100, currency: 'EUR' })`.
 *
 * Positional arguments are matched against the operation's parameters in
 * declaration order, which is what the ServiceInterface defines them in.
 * A single object argument for an operation the catalog does not know is
 * passed through as the parameter map, so a service can be called before
 * its description arrives.
 *
 * `then` and symbol properties answer undefined, so awaiting the proxy
 * is safe — without that guard `await service` would call `then()` on it
 * and hang.
 *
 * `exceptions` maps a declared error's symbolic type to the class a
 * binding generated for it: a DdsrServiceError of that type is thrown
 * as an instance of the class instead, so `catch` can tell one declared
 * error from another by `instanceof`, as a Java caller does.
 */
export function serviceProxy<T>(locator: ServiceLocator, exceptions: ExceptionClasses = {}): T {
  return new Proxy<object>({}, {
    get: (_target, prop) => {
      if (typeof prop !== 'string' || prop === 'then') return undefined;
      return (...args: unknown[]) => {
        const operation = toArray<{ name?: string; parameters?: unknown }>(
          locator.serviceInterface.operations
        ).find(o => o.name === prop);
        const params: Record<string, unknown> = {};
        if (operation) {
          const parameters = toArray<{ name?: string }>(operation.parameters);
          for (let i = 0; i < parameters.length && i < args.length; i++) {
            const name = parameters[i].name;
            if (name) params[name] = args[i];
          }
        } else if (args.length === 1 && typeof args[0] === 'object' && args[0] !== null) {
          Object.assign(params, args[0]);
        }
        return locator.invoke(prop, params).catch((error: unknown) => {
          if (error instanceof DdsrServiceError && exceptions[error.exception.type]) {
            throw new exceptions[error.exception.type](error.message, error);
          }
          throw error;
        });
      };
    },
  }) as unknown as T;
}

/** A generated class per declared error, by its symbolic type. */
export type ExceptionClasses = Record<string, new (message: string, cause: DdsrServiceError) => Error>;
