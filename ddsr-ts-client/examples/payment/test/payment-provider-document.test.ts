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
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { fingerprint, toArray } from '@ddsr/client';
import type { MqttFlavor, RestFlavor, RestOperationFlavor, RestParameterBinding, ServiceFlavor } from '@ddsr/model';
import { loadPaymentProvider } from '../payment-api';

const golden = fileURLToPath(new URL('../../../../itest/fixtures/fingerprint/payment.sd1', import.meta.url));

function flavor<T>(flavors: unknown, name: string): T | undefined {
  return toArray<ServiceFlavor>(flavors as never)
    .find(f => (f as unknown as { eClass(): { getName(): string } }).eClass().getName() === name) as T | undefined;
}

/**
 * The TypeScript Payment provider states its contract once, as a
 * document (#154) — and it is still the contract the Java side and the
 * golden fixture pin, so both providers announce one sd1.
 */
describe('the Payment provider document', () => {
  it('holds the contract the golden fixture pins', () => {
    const { serviceInterface } = loadPaymentProvider('payments-ts', 'http://localhost:9090/payments');

    expect(fingerprint(serviceInterface)).toBe(readFileSync(golden, 'utf8').trim());
  });

  it('takes only the deployment from the caller', () => {
    const { provider, implementation } = loadPaymentProvider('payments-x', 'http://host.test:9095/pay');

    expect(provider.name).toBe('payments-x');
    expect(implementation.name).toBe('payments-x-rest');
    expect(implementation.implementationId).toBe('ts:payments-x:1.0.0');
    const rest = flavor<RestFlavor>(implementation.flavors, 'RestFlavor')!;
    expect(rest.host).toBe('http://host.test:9095');
    expect(rest.basePath).toBe('/pay');
  });

  it('says where every argument travels, as the bindings the consumers read', () => {
    const { implementation } = loadPaymentProvider('payments-ts', 'http://localhost:9090/payments');
    const rest = flavor<RestFlavor>(implementation.flavors, 'RestFlavor')!;
    const charge = toArray<RestOperationFlavor>(rest.operationFlavors).find(o => o.name === 'charge')!;
    const bindings = toArray<RestParameterBinding>(charge.parameterBindings)
      .map(b => `${b.parameter?.name}:${String(b.binding)}:${b.wireName ?? ''}`);

    expect(charge.path).toBe('/charge/{amount}');
    expect(bindings).toEqual(['amount:PATH:', 'currency:HEADER:X-Currency']);
  });

  it('announces MQTT only where a broker is configured', () => {
    const restOnly = loadPaymentProvider('payments-ts', 'http://localhost:9090/payments');
    expect(flavor(restOnly.implementation.flavors, 'MqttFlavor')).toBeUndefined();

    const both = loadPaymentProvider('payments-ts', 'http://localhost:9090/payments', 'tcp://localhost:1883');
    const mqtt = flavor<MqttFlavor>(both.implementation.flavors, 'MqttFlavor')!;
    expect(toArray<string>(mqtt.brokers)).toEqual(['tcp://localhost:1883']);
    expect(mqtt.requestTopic).toBe('ddsr/rpc/payments-ts');
  });

  it('keeps the typed properties the fidelity probes compare', () => {
    const { implementation } = loadPaymentProvider('payments-ts', 'http://localhost:9090/payments');
    const names = toArray<{ name?: string }>(implementation.properties as never).map(p => p.name);

    expect(names).toEqual(['ddsr.provider.lang', 'service.ranking', 'payments.maxAmountCents', 'payments.feeRate',
      'payments.timeoutSeconds', 'payments.maxRetries', 'payments.sandbox', 'payments.tags']);
  });
});
