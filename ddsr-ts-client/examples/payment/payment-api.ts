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

import type { ServiceImplementation, ServiceInterface, ServiceOperation, ServiceProvider } from '@ddsr/model';
import { DDSRFactory } from '@ddsr/model';
import { props, toArray } from '@ddsr/client';

const factory = DDSRFactory.eINSTANCE;

/**
 * The Payment service description — field-for-field the same contract
 * the Java PaymentPublisher builds, so both sides produce the SAME sd1
 * fingerprint (that is the point of FR-P6: divergence becomes a string
 * comparison).
 */
export function buildPaymentInterface(): ServiceInterface {
  const payment = factory.createServiceInterface();
  payment.name = 'Payment';
  payment.version = '1.0.0';
  payment.description = 'Payment processing service — charges accounts and reports balances.';

  const charge = factory.createServiceOperation();
  charge.name = 'charge';
  charge.returnType = 'double';
  charge.description = 'Charge an amount. Returns the remaining balance.';
  charge.parameters.push(parameter('amount', 0, 'double', false, undefined, 'Amount to charge.'));
  charge.parameters.push(parameter('currency', 1, 'string', true, 'EUR', 'ISO 4217 currency code.'));
  payment.operations.push(charge);

  const getBalance = factory.createServiceOperation();
  getBalance.name = 'getBalance';
  getBalance.returnType = 'double';
  getBalance.description = 'Get current account balance.';
  getBalance.parameters.push(parameter('accountId', 0, 'string', false, undefined, 'The account identifier.'));
  payment.operations.push(getBalance);

  return payment;
}

function parameter(
  name: string,
  index: number,
  type: string,
  optional: boolean,
  defaultValue: string | undefined,
  description: string
) {
  const p = factory.createParameter();
  p.name = name;
  p.index = index;
  p.type = type;
  p.optional = optional;
  if (defaultValue !== undefined) p.defaultValue = defaultValue;
  p.description = description;
  return p;
}

/**
 * Provider + implementation with the SAME property keys as the Java
 * PaymentPublisher (only ddsr.provider.lang differs) — the property
 * fidelity probes of the cross-language harness compare these.
 */
export function buildPaymentProvider(
  providerName: string,
  serviceUrl: string,
  serviceInterface: ServiceInterface
): { provider: ServiceProvider; implementation: ServiceImplementation } {
  const provider = factory.createServiceProvider();
  provider.name = providerName;

  const implementation = factory.createServiceImplementation();
  implementation.name = `${providerName}-rest`;
  implementation.implementationId = `ts:${providerName}:1.0.0`;
  implementation.serviceInterfaces.push(serviceInterface);

  implementation.properties.push(props.string('ddsr.provider.lang', 'typescript'));
  implementation.properties.push(props.int('service.ranking', 10));
  implementation.properties.push(props.long('payments.maxAmountCents', 5000000000));
  implementation.properties.push(props.double('payments.feeRate', 0.025));
  implementation.properties.push(props.float('payments.timeoutSeconds', 1.5));
  implementation.properties.push(props.short('payments.maxRetries', 3));
  implementation.properties.push(props.bool('payments.sandbox', true));
  implementation.properties.push(props.stringList('payments.tags', ['demo', 'payments']));

  const url = new URL(serviceUrl);
  const flavor = factory.createRestFlavor();
  flavor.name = 'payments-rest';
  flavor.host = `${url.protocol}//${url.host}`;
  flavor.basePath = url.pathname.replace(/\/+$/, '') || '/payments';

  const operations = toArray<ServiceOperation>(serviceInterface.operations);

  const chargeFlavor = factory.createRestOperationFlavor();
  chargeFlavor.name = 'charge';
  chargeFlavor.method = 'POST';
  chargeFlavor.path = '/charge';
  chargeFlavor.operation = operations.find(o => o.name === 'charge')!;
  flavor.operationFlavors.push(chargeFlavor);

  const balanceFlavor = factory.createRestOperationFlavor();
  balanceFlavor.name = 'getBalance';
  balanceFlavor.method = 'GET';
  balanceFlavor.path = '/balance';
  balanceFlavor.operation = operations.find(o => o.name === 'getBalance')!;
  flavor.operationFlavors.push(balanceFlavor);

  implementation.flavors.push(flavor);
  provider.implementations.push(implementation);
  return { provider, implementation };
}

export const BROKER_URL = process.env.BROKER_URL ?? 'http://localhost:8887/ddsr/rest';
export const SERVICE_URL = process.env.SERVICE_URL ?? 'http://localhost:9090/payments';
export const PROVIDER_NAME = process.env.PROVIDER_NAME ?? 'payments-ts';
