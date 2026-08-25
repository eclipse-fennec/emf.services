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
  ServiceInterface,
  ServiceOperation,
  ServiceFlavor,
  ServiceOperationFlavor,
  RestFlavor,
  RestOperationFlavor,
} from '@ddsr/model';
import { FlavorKind } from '@ddsr/model';
import type { FlavorPlugin } from '../api/flavor-plugin';

/**
 * Resolves an operation by name against a ServiceInterface and dispatches
 * it through the matching FlavorPlugin.
 */
export async function invokeOperation(
  operationName: string,
  params: Record<string, unknown>,
  serviceInterface: ServiceInterface,
  flavors: ServiceFlavor[],
  flavorPlugins: FlavorPlugin[]
): Promise<unknown> {
  // 1. Find the operation in the interface
  const operation = findOperation(serviceInterface, operationName);
  if (!operation) {
    throw new Error(
      `Operation '${operationName}' not found on interface '${serviceInterface.name}'`
    );
  }

  // 2. Find a flavor + operationFlavor + plugin that can handle it
  for (const flavor of flavors) {
    const plugin = flavorPlugins.find(p => p.canHandle(flavor));
    if (!plugin) continue;

    const opFlavor = findOperationFlavor(flavor, operationName);
    if (!opFlavor) continue;

    // 3. Dispatch
    return plugin.invoke(operation, params, flavor, opFlavor);
  }

  throw new Error(
    `No FlavorPlugin can handle operation '${operationName}' on '${serviceInterface.name}'. ` +
    `Available flavors: [${flavors.map(f => f.kind).join(', ')}]`
  );
}

function findOperation(
  serviceInterface: ServiceInterface,
  name: string
): ServiceOperation | undefined {
  for (const op of serviceInterface.operations) {
    if (op.name === name) return op;
  }
  return undefined;
}

function findOperationFlavor(
  flavor: ServiceFlavor,
  operationName: string
): ServiceOperationFlavor | undefined {
  for (const opFlavor of flavor.operationFlavors) {
    if (opFlavor.name === operationName || opFlavor.operation?.name === operationName) {
      return opFlavor;
    }
  }
  return undefined;
}
