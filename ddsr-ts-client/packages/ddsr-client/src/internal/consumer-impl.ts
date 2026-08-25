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

import type {
  ServiceImplementation,
  ServiceInterface,
  ServiceReference,
  ServiceProvider,
  ServiceFlavor,
  LocalServiceRegistry,
} from '@ddsr/model';
import type { DdsrConsumer } from '../api/ddsr-consumer';
import type { ServiceLocator } from '../api/service-locator';
import type { FlavorPlugin } from '../api/flavor-plugin';
import type { DdsrServiceListener } from '../api/service-listener';
import { ServiceLocatorImpl } from '../proxy/service-locator-impl';
import { ServiceListenerRegistry } from './service-listener-registry';
import type { BrokerHttp } from './broker-http';
import { eClassName, toArray } from './emf-util';

/**
 * DdsrConsumer implementation. Lookups go directly against the broker
 * REST API (BrokerHttp); invocation of found services goes through the
 * FlavorPlugin pipeline. Listener registration and the snapshot-on-
 * reconnect refresh mirror the Java client's ConsumerImpl:
 * find() stays pull-through (no local cache); only the reference-id →
 * interface-names map is retained for event routing.
 */
export class DdsrConsumerImpl implements DdsrConsumer {
  private readonly broker: BrokerHttp;
  private readonly flavorPlugins: FlavorPlugin[];
  private readonly supportedFlavors: string[];
  private readonly consumerId: string | undefined;
  readonly listeners: ServiceListenerRegistry;

  constructor(
    broker: BrokerHttp,
    flavorPlugins: FlavorPlugin[],
    supportedFlavors: string[],
    listeners: ServiceListenerRegistry,
    consumerId?: string
  ) {
    this.broker = broker;
    this.flavorPlugins = flavorPlugins;
    this.supportedFlavors = supportedFlavors;
    this.listeners = listeners;
    this.consumerId = consumerId;
  }

  async find(interfaceName: string, filter?: string): Promise<ServiceLocator[]> {
    const flavors = this.supportedFlavors.length > 0 ? this.supportedFlavors.join(',') : undefined;
    const roots = await this.broker.getReferences(interfaceName, filter, flavors, this.consumerId);
    return this.parseLocators(roots, interfaceName);
  }

  async findOne(interfaceName: string, filter?: string): Promise<ServiceLocator | undefined> {
    const locators = await this.find(interfaceName, filter);
    return locators[0];
  }

  addServiceListener(
    interfaceName: string,
    filter: string | undefined,
    listener: DdsrServiceListener
  ): () => void {
    return this.listeners.add(interfaceName, filter, listener);
  }

  removeServiceListener(listener: DdsrServiceListener): void {
    this.listeners.remove(listener);
  }

  /**
   * FR-Sync-Reconnect: pull one find() per distinct subscribed
   * interface. Each find() re-populates the reference-id → interfaces
   * map, which is what routes later UNREGISTERING events. A failing
   * lookup is logged per interface and does not stop the others.
   */
  async refreshFromSnapshot(): Promise<void> {
    for (const interfaceName of this.listeners.subscribedInterfaces()) {
      try {
        await this.find(interfaceName, undefined);
      } catch (error) {
        console.error(`[ddsr] snapshot refresh for '${interfaceName}' failed: ${String(error)}`);
      }
    }
  }

  async getService<T>(interfaceName: string, filter?: string): Promise<T | undefined> {
    const locator = await this.findOne(interfaceName, filter);
    if (!locator) return undefined;

    // Proxy that maps method calls to invoke(); 'then' and symbols are
    // guarded so awaiting the proxy is safe.
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

  /**
   * Parse the broker's multi-root lookup response (LookupResource
   * shape: one LocalServiceRegistry envelope named "lookup-result"
   * containing references + providers→implementations, plus the
   * referenced ServiceInterfaces as sibling roots).
   */
  private parseLocators(roots: unknown[], queriedInterface: string): ServiceLocator[] {
    const locators: ServiceLocator[] = [];
    const interfacesByName = new Map<string, ServiceInterface>();

    for (const root of roots) {
      if (eClassName(root) === 'ServiceInterface') {
        const si = root as ServiceInterface;
        if (si.name) interfacesByName.set(si.name, si);
      }
    }

    for (const root of roots) {
      if (eClassName(root) !== 'LocalServiceRegistry') continue;
      const registry = root as LocalServiceRegistry;
      const providers = toArray<ServiceProvider>(registry.providers);
      const references = toArray<ServiceReference>(registry.references);

      for (const provider of providers) {
        for (const impl of toArray<ServiceImplementation>(provider.implementations)) {
          const interfaceNames: string[] = [];
          let serviceInterface: ServiceInterface | undefined;

          // The impl's cross-refs resolve against the sibling roots.
          for (const si of toArray<ServiceInterface>(impl.serviceInterfaces)) {
            const name = si?.name;
            if (!name) continue;
            interfaceNames.push(name);
            serviceInterface ??= interfacesByName.get(name) ?? si;
          }
          // Fallbacks: the queried name, then any sibling interface.
          serviceInterface ??= interfacesByName.get(queriedInterface)
            ?? interfacesByName.values().next().value;
          if (!serviceInterface) continue;
          if (interfaceNames.length === 0 && serviceInterface.name) {
            interfaceNames.push(serviceInterface.name);
          }

          const reference = references.find(
            r => r.provider === provider || r.provider?.name === provider.name
          );
          this.listeners.noteReference(reference?.id, interfaceNames);

          locators.push(new ServiceLocatorImpl(
            reference ?? stubReference(impl),
            impl,
            serviceInterface,
            toArray<ServiceFlavor>(impl.flavors),
            this.flavorPlugins
          ));
        }
      }
    }
    return locators;
  }
}

function stubReference(impl: ServiceImplementation): ServiceReference {
  return { id: `lookup:${impl.name}` } as ServiceReference;
}
