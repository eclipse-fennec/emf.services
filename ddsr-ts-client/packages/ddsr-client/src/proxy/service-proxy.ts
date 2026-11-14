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
import { DdsrClientError, DdsrServiceError } from '../api/errors.js';

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
        const operation = toArray<{ name?: string; parameters?: unknown; returnValue?: unknown }>(
          locator.serviceInterface.operations
        ).find(o => o.name === prop);
        // The contract may say a slot is a stream (Parameter.delivery).
        // One that comes back is iterated; one the caller hands over is
        // many requests to one responder, which no flavor plugin
        // carries yet - said here, by name, before anything is sent.
        const streams = streamSlots(operation);
        const handedOver = streams.filter(slot => slot !== 'return');
        if (handedOver.length > 0) {
          throw new DdsrClientError(
            `${prop} takes streams as arguments [${handedOver.join(', ')}], and no transport here carries a`
            + ' stream from the caller to the provider yet');
        }
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
        const asGenerated = (error: unknown): unknown =>
          error instanceof DdsrServiceError && exceptions[error.exception.type]
            ? new exceptions[error.exception.type](error.message, error) : error;
        if (streams.includes('return')) {
          // Iterated, not awaited: nothing is sent before the first
          // value is asked for, and a failure - a declared error as the
          // generated class included - ends the iteration.
          return streamOf(() => locator.invoke(prop, params), asGenerated);
        }
        return locator.invoke(prop, params).catch((error: unknown) => {
          throw asGenerated(error);
        });
      };
    },
  }) as unknown as T;
}

/**
 * What a call resolves to, as the stream the caller iterates. The call
 * is made when the first value is asked for; a transport that carries
 * streams resolves it to an async iterable, and one that does not
 * resolves it to something else - which is said instead of iterated.
 */
function streamOf(start: () => Promise<unknown>, told: (error: unknown) => unknown): AsyncIterable<unknown> {
  return {
    [Symbol.asyncIterator]() {
      let values: AsyncIterator<unknown> | undefined;
      const open = async (): Promise<AsyncIterator<unknown>> => {
        const answered = await start();
        const iterable = answered as { [Symbol.asyncIterator]?: () => AsyncIterator<unknown> } | null | undefined;
        if (typeof iterable?.[Symbol.asyncIterator] !== 'function') {
          throw new DdsrClientError('the operation returns a stream, and the transport that carried the call'
            + ' carries no streams');
        }
        return iterable[Symbol.asyncIterator]!();
      };
      return {
        async next() {
          try {
            values ??= await open();
            return await values.next();
          } catch (error) {
            throw told(error);
          }
        },
        async return() {
          await values?.return?.();
          return { done: true, value: undefined };
        },
      };
    },
  };
}

/** The stream slots of an operation, by name - `return` for the return value. */
function streamSlots(operation: { parameters?: unknown; returnValue?: unknown } | undefined): string[] {
  if (!operation) return [];
  // A stream of its own or one every receiver shares (#252): both are iterated.
  const isStream = (slot: unknown) => {
    const delivery = String((slot as { delivery?: unknown } | undefined)?.delivery ?? '');
    return delivery === 'STREAM' || delivery === 'BROADCAST';
  };
  const names = toArray<{ name?: string }>(operation.parameters).filter(isStream).map(slot => slot.name ?? '?');
  return isStream(operation.returnValue) ? [...names, 'return'] : names;
}

/** A generated class per declared error, by its symbolic type. */
export type ExceptionClasses = Record<string, new (message: string, cause: DdsrServiceError) => Error>;
