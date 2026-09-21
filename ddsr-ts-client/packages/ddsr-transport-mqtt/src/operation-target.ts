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

import type { Parameter, ServiceInterface, ServiceOperation } from '@ddsr/model';
import type { OperationHandlers } from './mqtt-operation-server.js';

/**
 * An implementation, as the operation server wants it (#154).
 *
 * `MqttOperationServer` takes a handler per operation, which is one
 * line of wiring per operation and a name to keep in step by hand. A
 * contract already says what its operations are called and in which
 * order their arguments come, so this derives the map from it: an
 * object whose methods are named after the operations is enough, which
 * is what an implementation is on the Java side too.
 */
// `never[]` rather than `unknown[]`: a method declared with the types
// its contract gives it — `charge(amount: number, currency?: string)` —
// is only assignable this way. A target typed with `unknown[]` would
// force every implementation to take unknowns and cast, which is the
// opposite of what a typed contract is for.
export type OperationTarget = Record<string, (...args: never[]) => unknown | Promise<unknown>>;

export interface HandlersOptions {
  /**
   * What to do about an operation the object does not implement.
   * Skipping is the default: a provider may serve part of a contract
   * while the rest is somebody else's, and a handler that throws
   * "not implemented" is a worse answer than no subscription at all.
   */
  onMissing?: 'skip' | 'fail';
}

/**
 * The handlers of a contract, taken from an object.
 *
 * Arguments arrive named — the invocation document carries names, not
 * positions — and are applied in the order the contract declares,
 * which is the order a Java implementation is called in as well.
 */
export function operationHandlers(
  contract: ServiceInterface,
  implementation: OperationTarget,
  options: HandlersOptions = {}
): OperationHandlers {
  const handlers: OperationHandlers = {};
  for (const operation of toArray<ServiceOperation>(contract.operations)) {
    const name = operation.name;
    if (!name) {
      continue;
    }
    const method = implementation[name];
    if (typeof method !== 'function') {
      if (options.onMissing === 'fail') {
        throw new Error(`the implementation has no operation '${name}' of ${contract.name}`);
      }
      continue;
    }
    const parameters = toArray<Parameter>(operation.parameters)
      .slice()
      .sort((a, b) => (a.index ?? 0) - (b.index ?? 0));
    handlers[name] = (args: Record<string, unknown>) =>
      (method as (...args: unknown[]) => unknown)
        .apply(implementation, parameters.map(p => args[p.name ?? '']));
  }
  return handlers;
}

function toArray<T>(value: unknown): T[] {
  if (!value) return [];
  if (Array.isArray(value)) return value as T[];
  const list = value as { size?: () => number; get?: (i: number) => T } & Iterable<T>;
  if (typeof list.size === 'function' && typeof list.get === 'function') {
    return Array.from({ length: list.size() }, (_, i) => list.get!(i));
  }
  return [...list];
}
