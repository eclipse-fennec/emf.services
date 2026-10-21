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

import { readFileSync } from 'node:fs';
import type { EObject } from '@emfts/core';
import type { MqttFlavor, RestFlavor, ServiceFlavor, ServiceImplementation, ServiceInterface, ServiceProvider } from '@ddsr/model';
import { deserializeFromXmi, toArray } from '@ddsr/client';

/** The provider document this example serves from (#154). */
export const PAYMENT_PROVIDER_DOCUMENT = new URL('./payment-provider.xmi', import.meta.url);

/**
 * The Payment provider, read from its document — the contract, the
 * implementation with the same property keys as the Java
 * PaymentPublisher (only ddsr.provider.lang differs, which the property
 * fidelity probes compare), and where every argument travels on both
 * transports. Nothing here states the contract: that is the document's
 * job, the same construction a Java provider served by configuration
 * uses (#84, #154).
 *
 * What is filled in is only what a deployment decides: the provider's
 * name (and the implementation's, which derive from it), where it is
 * reachable, and whether it speaks MQTT — without an MQTT broker the
 * MQTT flavor is not announced at all.
 */
export function loadPaymentProvider(
  providerName: string,
  serviceUrl: string,
  mqttUrl?: string,
  document: URL = PAYMENT_PROVIDER_DOCUMENT
): { provider: ServiceProvider; implementation: ServiceImplementation; serviceInterface: ServiceInterface } {
  const read = deserializeFromXmi(readFileSync(document, 'utf8'), document.href);
  const roots = Array.isArray(read) ? read : [read];
  const provider = ofClass<ServiceProvider>(roots, 'ServiceProvider');
  const serviceInterface = ofClass<ServiceInterface>(roots, 'ServiceInterface');
  const implementation = toArray<ServiceImplementation>(provider.implementations)[0];

  provider.name = providerName;
  implementation.name = `${providerName}-rest`;
  implementation.implementationId = `ts:${providerName}:1.0.0`;

  const url = new URL(serviceUrl);
  const rest = flavorOf<RestFlavor>(implementation, 'RestFlavor');
  rest.host = `${url.protocol}//${url.host}`;
  rest.basePath = url.pathname.replace(/\/+$/, '') || rest.basePath;

  // The same implementation, a second transport (A2 Etappe 2): announced
  // only where an MQTT broker is configured. Topics follow the frozen
  // request/response convention (mqtt-rpc.ts), per provider.
  const mqtt = flavorOf<MqttFlavor>(implementation, 'MqttFlavor');
  if (mqttUrl) {
    mqtt.brokers.push(mqttUrl);
    mqtt.requestTopic = `ddsr/rpc/${providerName}`;
  } else {
    implementation.flavors.remove(mqtt);
  }

  return { provider, implementation, serviceInterface };
}

function ofClass<T>(roots: EObject[], name: string): T {
  const found = roots.find(root => root.eClass().getName() === name);
  if (!found) throw new Error(`${PAYMENT_PROVIDER_DOCUMENT.pathname} holds no ${name}`);
  return found as unknown as T;
}

function flavorOf<T extends ServiceFlavor>(implementation: ServiceImplementation, name: string): T {
  const found = toArray<ServiceFlavor>(implementation.flavors)
    .find(flavor => (flavor as unknown as EObject).eClass().getName() === name);
  if (!found) throw new Error(`the Payment implementation has no ${name}`);
  return found as T;
}

export const BROKER_URL = process.env.BROKER_URL ?? 'http://localhost:8887/ddsr/rest';
export const SERVICE_URL = process.env.SERVICE_URL ?? 'http://localhost:9090/payments';
export const PROVIDER_NAME = process.env.PROVIDER_NAME ?? 'payments-ts';
/** Optional: when set, the provider additionally serves Payment over MQTT. */
export const MQTT_URL = process.env.MQTT_URL;
