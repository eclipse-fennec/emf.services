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
import { invokeOperation } from './invocation-handler';

/**
 * Concrete ServiceLocator. Holds all the metadata needed for invocation
 * and delegates invoke() through the FlavorPlugin pipeline.
 */
export class ServiceLocatorImpl implements ServiceLocator {
  readonly reference: ServiceReference;
  readonly implementation: ServiceImplementation;
  readonly serviceInterface: ServiceInterface;
  private readonly _flavors: ServiceFlavor[];
  private readonly _plugins: FlavorPlugin[];

  constructor(
    reference: ServiceReference,
    implementation: ServiceImplementation,
    serviceInterface: ServiceInterface,
    flavors: ServiceFlavor[],
    plugins: FlavorPlugin[]
  ) {
    this.reference = reference;
    this.implementation = implementation;
    this.serviceInterface = serviceInterface;
    this._flavors = flavors;
    this._plugins = plugins;
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

  invoke(operationName: string, params?: Record<string, unknown>): Promise<unknown> {
    return invokeOperation(
      operationName,
      params ?? {},
      this.serviceInterface,
      this._flavors,
      this._plugins
    );
  }
}
