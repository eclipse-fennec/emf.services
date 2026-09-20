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
  ServiceEvent,
} from '@ddsr/model';
import type { DdsrConsumer } from '../api/ddsr-consumer';
import type { ServiceLocator } from '../api/service-locator';
import type { FlavorPlugin } from '../api/flavor-plugin';
import type { DdsrServiceListener } from '../api/service-listener';
import { ServiceLocatorImpl } from '../proxy/service-locator-impl';
import type { LocatorTracking, ResolvedRegistration } from '../proxy/service-locator-impl';
import { ServiceListenerRegistry } from './service-listener-registry';
import type { BrokerHttp } from './broker-http';
import { eClassName, toArray } from './emf-util';
import { implementationFingerprint } from '../fingerprint/service-implementation-fingerprint';
import { propertyOf } from '../properties';

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
  /** UPDATE_POLICY.md §3: rebind to the successor on UPGRADE_AVAILABLE instead of waiting for the retire. */
  private readonly greedyRebind: boolean;
  /** Tracked locators by the reference id they are bound to (#57). */
  private readonly trackedByReference = new Map<string, Set<ServiceLocatorImpl>>();
  /** Interfaces for which the internal tracking listener is registered. */
  private readonly trackedInterfaces = new Set<string>();
  private readonly trackingListener: DdsrServiceListener = (event) => this.onTrackedEvent(event);

  constructor(
    broker: BrokerHttp,
    flavorPlugins: FlavorPlugin[],
    supportedFlavors: string[],
    listeners: ServiceListenerRegistry,
    consumerId?: string,
    greedyRebind = false
  ) {
    this.broker = broker;
    this.flavorPlugins = flavorPlugins;
    this.supportedFlavors = supportedFlavors;
    this.listeners = listeners;
    this.consumerId = consumerId;
    this.greedyRebind = greedyRebind;
  }

  /**
   * @param fingerprint contract addressing (ACQUISITION.md §11.2): when
   *   set, the broker only returns implementations whose catalog
   *   contract hashes to exactly this sd1 value — compatibility by
   *   identity, no range semantics.
   */
  async find(interfaceName: string, filter?: string, fingerprint?: string): Promise<ServiceLocator[]> {
    const flavors = this.supportedFlavors.length > 0 ? this.supportedFlavors.join(',') : undefined;
    const roots = await this.broker.getReferences(
      interfaceName, filter, flavors, this.consumerId, fingerprint);
    const locators = this.parseLocators(roots, interfaceName, filter);
    for (const locator of locators) this.track(locator);
    return locators;
  }

  // ------------------------------------------------------------------
  // Locator tracking (#57): locators follow their service
  // ------------------------------------------------------------------

  /** Raw resolution for a rebinding locator — the same lookup as find(), no new locators. */
  private async resolve(interfaceName: string, filter: string | undefined): Promise<ResolvedRegistration[]> {
    const flavors = this.supportedFlavors.length > 0 ? this.supportedFlavors.join(',') : undefined;
    const roots = await this.broker.getReferences(interfaceName, filter, flavors, this.consumerId);
    return this.parseLocators(roots, interfaceName, filter).map(l => ({
      reference: l.reference,
      implementation: l.implementation,
      serviceInterface: l.serviceInterface,
      flavors: l.flavors(),
    }));
  }

  private track(locator: ServiceLocatorImpl): void {
    this.rekey(locator, undefined);
    const interfaceName = locator.interfaceName;
    // Interest in the interface keeps the event stream open and puts the
    // interface into the reconnect snapshot: one internal listener per interface.
    if (interfaceName && !this.trackedInterfaces.has(interfaceName)) {
      this.trackedInterfaces.add(interfaceName);
      this.listeners.add(interfaceName, undefined, this.trackingListener);
    }
  }

  private rekey(locator: ServiceLocatorImpl, previousReferenceId: string | undefined): void {
    if (previousReferenceId) this.trackedByReference.get(previousReferenceId)?.delete(locator);
    const id = locator.reference.id;
    if (!id) return;
    let set = this.trackedByReference.get(id);
    if (!set) {
      set = new Set();
      this.trackedByReference.set(id, set);
    }
    set.add(locator);
  }

  private onTrackedEvent(event: ServiceEvent): void {
    const id = event.reference?.id;
    if (!id) return;
    for (const locator of this.trackedByReference.get(id) ?? []) {
      locator.onEvent(event, this.greedyRebind);
    }
  }

  /** Test hook: number of locators currently tracked. */
  trackedLocatorCount(): number {
    let n = 0;
    for (const set of this.trackedByReference.values()) n += set.size;
    return n;
  }

  async findOne(interfaceName: string, filter?: string, fingerprint?: string): Promise<ServiceLocator | undefined> {
    const locators = await this.find(interfaceName, filter, fingerprint);
    return locators[0];
  }

  addServiceListener(
    interfaceName: string,
    filter: string | undefined,
    listener: DdsrServiceListener
  ): () => void {
    return this.listeners.add(interfaceName, filter, listener);
  }

  release(referenceId: string): void {
    this.listeners.forgetReference(referenceId);
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
  private parseLocators(roots: unknown[], queriedInterface: string, filter?: string): ServiceLocatorImpl[] {
    const locators: ServiceLocatorImpl[] = [];
    const tracking: LocatorTracking = {
      interfaceName: queriedInterface,
      filter,
      resolve: (i, f) => this.resolve(i, f),
      onRebound: (locator, previous) => this.rekey(locator, previous),
    };
    const interfacesByName = new Map<string, ServiceInterface>();
    for (const root of roots) {
      if (eClassName(root) === 'ServiceInterface') {
        const si = root as ServiceInterface;
        if (si.name) interfacesByName.set(si.name, si);
      }
    }
    const locatorFor = (reference: ServiceReference, impl: ServiceImplementation): ServiceLocatorImpl | undefined => {
      const interfaceNames: string[] = [];
      let serviceInterface: ServiceInterface | undefined;
      for (const si of toArray<ServiceInterface>(impl.serviceInterfaces)) {
        const name = si?.name;
        if (!name) continue;
        interfaceNames.push(name);
        serviceInterface ??= interfacesByName.get(name) ?? si;
      }
      serviceInterface ??= interfacesByName.get(queriedInterface) ?? interfacesByName.values().next().value;
      if (!serviceInterface) return undefined;
      if (interfaceNames.length === 0 && serviceInterface.name) interfaceNames.push(serviceInterface.name);
      this.listeners.noteReference(reference.id, interfaceNames);
      return new ServiceLocatorImpl(
        reference, impl, serviceInterface, toArray<ServiceFlavor>(impl.flavors), this.flavorPlugins, tracking);
    };
    for (const root of roots) {
      if (eClassName(root) !== 'LocalServiceRegistry') continue;
      const registry = root as LocalServiceRegistry;
      const providers = toArray<ServiceProvider>(registry.providers);
      const covered = new Set<ServiceImplementation>();
      // One locator per reference. The model has no reference→implementation
      // pointer; a provider with several (hit) implementations — two
      // versions under one provider name — is disambiguated by the im1
      // decoration the broker put on the reference.
      for (const reference of toArray<ServiceReference>(registry.references)) {
        const provider = reference.provider as ServiceProvider | undefined;
        const candidates = toArray<ServiceImplementation>(provider?.implementations);
        if (candidates.length === 0) continue;
        const im1 = propertyOf(reference, 'ddsr.impl.fingerprint');
        const impl = candidates.length === 1
          ? candidates[0]
          : (candidates.find(c => implementationFingerprint(c) === im1) ?? candidates[0]);
        covered.add(impl);
        const locator = locatorFor(reference, impl);
        if (locator) locators.push(locator);
      }
      // Legacy envelopes without any reference: one stub locator per
      // implementation. With references present, an implementation nobody
      // references is not a hit (the broker prunes those since #58).
      if (toArray<ServiceReference>(registry.references).length === 0) {
        for (const provider of providers) {
          for (const impl of toArray<ServiceImplementation>(provider.implementations)) {
            if (covered.has(impl)) continue;
            const locator = locatorFor(stubReference(impl), impl);
            if (locator) locators.push(locator);
          }
        }
      }
    }
    return locators;
  }
}

function stubReference(impl: ServiceImplementation): ServiceReference {
  return { id: `lookup:${impl.name}` } as ServiceReference;
}
