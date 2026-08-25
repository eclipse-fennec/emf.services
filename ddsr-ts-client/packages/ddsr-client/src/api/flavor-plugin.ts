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

import type { ServiceFlavor, ServiceOperationFlavor, ServiceOperation } from '@ddsr/model';

/**
 * SPI for transport flavor plugins. Each plugin handles one flavor kind
 * (e.g. REST, MQTT) and translates ServiceOperation calls into actual
 * network requests.
 *
 * Plugins are discovered as TSM services under "ddsr.flavor.*".
 */
export interface FlavorPlugin {
  /** The flavor kind this plugin handles, e.g. "REST" or "MQTT". */
  readonly flavorKind: string;

  /** Returns true if this plugin can handle the given flavor instance. */
  canHandle(flavor: ServiceFlavor): boolean;

  /**
   * Invokes a service operation over the transport this plugin provides.
   *
   * @param operation     the ServiceOperation being called
   * @param params        named parameters for the operation
   * @param flavor        the ServiceFlavor (e.g. RestFlavor with basePath)
   * @param operationFlavor the per-operation flavor (e.g. RestOperationFlavor with method + path)
   * @returns the deserialized response
   */
  invoke(
    operation: ServiceOperation,
    params: Record<string, unknown>,
    flavor: ServiceFlavor,
    operationFlavor: ServiceOperationFlavor
  ): Promise<unknown>;
}
