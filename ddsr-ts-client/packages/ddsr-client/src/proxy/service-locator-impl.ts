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
  ServiceReference,
  ServiceImplementation,
  ServiceInterface,
  ServiceFlavor,
  RestFlavor,
} from '@ddsr/model';
import { FlavorKind } from '@ddsr/model';
import type { ServiceLocator } from '../api/service-locator';
import type { FlavorPlugin } from '../api/flavor-plugin';
import type { LocatorState, TrackedServiceLocator } from '../api/service-locator';
import { DdsrClientError, DdsrTransportError } from '../api/errors';
import type { ServiceEvent, ServiceProvider } from '@ddsr/model';
import { toArray } from '../internal/emf-util';
import { invokeOperation } from './invocation-handler';

/**
 * Concrete ServiceLocator. Holds all the metadata needed for invocation
 * and delegates invoke() through the FlavorPlugin pipeline.
 */
/** One live registration as the consumer resolves it: reference plus its implementation and flavors. */
export interface ResolvedRegistration {
  reference: ServiceReference;
  implementation: ServiceImplementation;
  serviceInterface: ServiceInterface;
  flavors: ServiceFlavor[];
}

/** How a tracked locator gets back to the broker: the consumer that created it. */
export interface LocatorTracking {
  interfaceName: string;
  filter: string | undefined;
  resolve(interfaceName: string, filter: string | undefined): Promise<ResolvedRegistration[]>;
  /** Called after a rebind changed the bound reference id, so the tracker can re-key. */
  onRebound?(locator: ServiceLocatorImpl, previousReferenceId: string | undefined): void;
}

export class ServiceLocatorImpl implements TrackedServiceLocator {
  reference: ServiceReference;
  implementation: ServiceImplementation;
  serviceInterface: ServiceInterface;
  private _flavors: ServiceFlavor[];
  private readonly _plugins: FlavorPlugin[];
  private readonly tracking: LocatorTracking | undefined;
  private _state: LocatorState = 'LIVE';

  constructor(
    reference: ServiceReference,
    implementation: ServiceImplementation,
    serviceInterface: ServiceInterface,
    flavors: ServiceFlavor[],
    plugins: FlavorPlugin[],
    tracking?: LocatorTracking
  ) {
    this.reference = reference;
    this.implementation = implementation;
    this.serviceInterface = serviceInterface;
    this._flavors = flavors;
    this._plugins = plugins;
    this.tracking = tracking;
  }

  get state(): LocatorState {
    return this._state;
  }

  get interfaceName(): string {
    return this.tracking?.interfaceName ?? this.serviceInterface.name ?? '';
  }

  get filter(): string | undefined {
    return this.tracking?.filter;
  }

  restFlavor(): RestFlavor | undefined {
    return this._flavors.find(f =>
      f.kind === FlavorKind.REST ||
      (f as any).eClass?.()?.getName?.() === 'RestFlavor'
    ) as RestFlavor | undefined;
  }

  flavors(): ServiceFlavor[] {
    return [...this._flavors];
  }

  /** Called by the consumer's tracker when an event names this locator's reference. */
  onEvent(event: ServiceEvent, greedy: boolean): void {
    switch (event.type) {
      case 'MODIFIED': {
        const fresh = selfContainedImplementation(event.reference);
        if (fresh && event.reference) {
          this.reference = event.reference;
          this.implementation = fresh;
          this._flavors = toArray<ServiceFlavor>(fresh.flavors);
          this._state = 'LIVE';
        } else {
          this._state = 'MODIFIED';
        }
        break;
      }
      case 'UNREGISTERING':
        this._state = event.reasonCode === 'COLDIFIED' ? 'STALE' : 'REBIND';
        break;
      case 'RETIRED':
        this._state = 'REBIND';
        break;
      case 'UPGRADE_AVAILABLE':
        if (greedy) this._state = 'REBIND';
        break;
      default:
        break;
    }
  }

  async rebind(excludeCurrent: boolean): Promise<boolean> {
    if (!this.tracking) return this._state === 'LIVE';
    const currentId = this.reference.id;
    const candidates = await this.tracking.resolve(this.tracking.interfaceName, this.tracking.filter);
    let chosen: ResolvedRegistration | undefined;
    if (!excludeCurrent && currentId) {
      chosen = candidates.find(c => c.reference.id === currentId);
    }
    chosen ??= candidates.find(c => !currentId || c.reference.id !== currentId);
    if (!chosen) return false;
    this.reference = chosen.reference;
    this.implementation = chosen.implementation;
    this.serviceInterface = chosen.serviceInterface;
    this._flavors = chosen.flavors;
    this._state = 'LIVE';
    if (currentId !== this.reference.id) {
      console.info(`[ddsr] ${this.tracking.interfaceName}: rebound ${currentId} -> ${this.reference.id}`);
      this.tracking.onRebound?.(this, currentId);
    }
    return true;
  }

  private async ensureBound(): Promise<void> {
    if (this._state === 'LIVE' || !this.tracking) return;
    const was = this._state;
    if (!(await this.rebind(was === 'REBIND'))) {
      throw new DdsrClientError(
        `service ${this.interfaceName} (${this.reference.id}) is not available: ` +
        (was === 'STALE' ? 'parked and not rehydrated' : 'no other registration matches')
      );
    }
  }

  async invoke(operationName: string, params?: Record<string, unknown>): Promise<unknown> {
    await this.ensureBound();
    try {
      return await this.invokeBound(operationName, params ?? {});
    } catch (error) {
      // #59: the registered provider did not answer. Rebind away from it
      // and retry exactly once; a second failure is the caller's.
      if (!(error instanceof DdsrTransportError) || !this.tracking || !(await this.rebind(true))) {
        throw error;
      }
      return this.invokeBound(operationName, params ?? {});
    }
  }

  private invokeBound(operationName: string, params: Record<string, unknown>): Promise<unknown> {
    return invokeOperation(operationName, params, this.serviceInterface, this._flavors, this._plugins);
  }
}

function selfContainedImplementation(reference: ServiceReference | undefined): ServiceImplementation | undefined {
  const provider = reference?.provider as ServiceProvider | undefined;
  const implementations = toArray<ServiceImplementation>(provider?.implementations);
  return implementations.length === 1 ? implementations[0] : undefined;
}
