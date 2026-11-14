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

import { describe, expect, it } from 'vitest';
import { DDSRFactory } from '@ddsr/model';
import type { ServiceFlavor, ServiceInterface } from '@ddsr/model';
import type { FlavorPlugin } from '../src/api/flavor-plugin';
import { invokeOperation } from '../src/proxy/invocation-handler';

const f = DDSRFactory.eINSTANCE;

/** A contract with one call answered once and one that streams, both bound on a REST and an MQTT flavor. */
function readingsContract(): { contract: ServiceInterface; flavors: ServiceFlavor[] } {
  const contract = f.createServiceInterface();
  contract.name = 'Readings';
  const latest = f.createServiceOperation();
  latest.name = 'latest';
  const one = f.createParameter();
  one.name = 'result';
  one.type = 'double';
  latest.returnValue = one;
  const readings = f.createServiceOperation();
  readings.name = 'readings';
  const many = f.createParameter();
  many.name = 'result';
  many.type = 'double';
  many.upperBound = -1;
  many.delivery = 'STREAM';
  readings.returnValue = many;
  contract.operations.push(latest);
  contract.operations.push(readings);

  const rest = f.createRestFlavor();
  rest.name = 'rest';
  rest.kind = 'REST';
  const mqtt = f.createMessagingFlavor();
  mqtt.name = 'mqtt';
  mqtt.kind = 'MQTT';
  for (const operation of [latest, readings]) {
    const overRest = f.createRestOperationFlavor();
    overRest.name = operation.name;
    overRest.operation = operation;
    rest.operationFlavors.push(overRest);
    const overMqtt = f.createMessagingOperationFlavor();
    overMqtt.name = operation.name;
    overMqtt.operation = operation;
    mqtt.operationFlavors.push(overMqtt);
  }
  return { contract, flavors: [rest, mqtt] };
}

function plugin(kind: string, carriesStreams: boolean, calls: string[]): FlavorPlugin {
  return {
    flavorKind: kind,
    carriesStreams,
    canHandle: flavor => String(flavor.kind) === kind,
    invoke: async operation => { calls.push(`${kind}:${operation.name}`); return 1; },
  };
}

// #251: a service may announce REST first and a messaging flavor beside
// it; the stream travels over the one that carries it.
describe('which flavor carries an operation', () => {
  it('calls answered once over the first announced flavor, a stream over the first that carries one', async () => {
    const { contract, flavors } = readingsContract();
    const calls: string[] = [];
    const plugins = [plugin('REST', false, calls), plugin('MQTT', true, calls)];

    await invokeOperation('latest', {}, contract, flavors, plugins);
    await invokeOperation('readings', {}, contract, flavors, plugins);

    expect(calls).toEqual(['REST:latest', 'MQTT:readings']);
  });

  it('refuses a stream no plugin here carries, and says where it would travel', async () => {
    const { contract, flavors } = readingsContract();
    const calls: string[] = [];

    await expect(invokeOperation('readings', {}, contract, flavors, [plugin('REST', false, calls)]))
      .rejects.toThrow(/carried by a plugin here that carries streams/);
    expect(calls).toEqual([]);
  });
});
