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
 */
export function serviceProxy<T>(locator: ServiceLocator): T {
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
        return locator.invoke(prop, params);
      };
    },
  }) as unknown as T;
}
