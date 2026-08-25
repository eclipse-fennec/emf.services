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

import type { ServiceInterface, ServiceProvider, ServiceImplementation } from '@ddsr/model';
import { DDSRFactory } from '@ddsr/model';
import { serializeToXmi } from '../src/xmi/xmi-support';

const factory = DDSRFactory.eINSTANCE;

/** The Payment interface, shaped like the Java PaymentPublisher builds it. */
export function paymentInterface(): ServiceInterface {
  const si = factory.createServiceInterface();
  si.name = 'Payment';
  si.version = '1.0.0';

  const charge = factory.createServiceOperation();
  charge.name = 'charge';
  const amount = factory.createParameter();
  amount.name = 'amount';
  amount.type = 'double';
  amount.index = 0;
  charge.parameters.push(amount);
  const accountId = factory.createParameter();
  accountId.name = 'accountId';
  accountId.type = 'string';
  accountId.index = 1;
  charge.parameters.push(accountId);
  si.operations.push(charge);

  const getBalance = factory.createServiceOperation();
  getBalance.name = 'getBalance';
  const account = factory.createParameter();
  account.name = 'accountId';
  account.type = 'string';
  account.index = 0;
  getBalance.parameters.push(account);
  si.operations.push(getBalance);

  return si;
}

export interface ProviderFixture {
  provider: ServiceProvider;
  implementation: ServiceImplementation;
  serviceInterface: ServiceInterface;
}

/** A provider with one REST implementation of the given interface. */
export function paymentProvider(
  name = 'payments-ts',
  serviceInterface: ServiceInterface = paymentInterface()
): ProviderFixture {
  const provider = factory.createServiceProvider();
  provider.name = name;

  const implementation = factory.createServiceImplementation();
  implementation.name = `${name}-rest`;
  implementation.implementationId = `ts:${name}:1.0.0`;
  implementation.serviceInterfaces.push(serviceInterface);

  const flavor = factory.createRestFlavor();
  flavor.name = 'rest';
  flavor.host = 'http://localhost:9090';
  flavor.basePath = '/payments';
  implementation.flavors.push(flavor);

  provider.implementations.push(implementation);
  return { provider, implementation, serviceInterface };
}

/**
 * A lookup response the way the JAVA broker's LookupResource writes it:
 * one LocalServiceRegistry envelope named "lookup-result" (references +
 * providers with implementations by containment, positional fragment
 * cross-refs) plus the referenced ServiceInterface as a sibling root.
 * Hand-written on purpose — it pins the exact Java wire shape including
 * EMF's default-value omission (no kind= on the RestFlavor, no method=
 * on a GET operation flavor, no severity/code on OK diagnostics).
 */
export function lookupResultXmi(providerName = 'payments-ts', referenceId = 'ref-1'): string {
  return `<?xml version="1.0" encoding="UTF-8"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:services="http://eclipse.org/fennec/services/1.0">
  <services:LocalServiceRegistry name="lookup-result">
    <references id="${referenceId}" provider="/0/@providers.0">
      <properties xsi:type="services:StringProperty" name="ddsr.fingerprint" value="sd1:0000000000000000000000000000000000000000000000000000000000000000"/>
      <properties xsi:type="services:IntProperty" name="service.ranking" value="7"/>
    </references>
    <providers name="${providerName}">
      <implementations name="${providerName}-rest" implementationId="ts:${providerName}:1.0.0" serviceInterfaces="/1">
        <flavors xsi:type="services:RestFlavor" name="rest" host="http://localhost:9090" basePath="/payments">
          <operationFlavors xsi:type="services:RestOperationFlavor" name="charge" path="/charge" method="POST" operation="/1/@operations.0"/>
          <operationFlavors xsi:type="services:RestOperationFlavor" name="getBalance" path="/balance" operation="/1/@operations.1"/>
        </flavors>
      </implementations>
    </providers>
  </services:LocalServiceRegistry>
  <services:ServiceInterface name="Payment" version="1.0.0">
    <operations name="charge">
      <parameters name="amount" type="double"/>
      <parameters name="accountId" type="string" index="1"/>
    </operations>
    <operations name="getBalance">
      <parameters name="accountId" type="string"/>
    </operations>
  </services:ServiceInterface>
</xmi:XMI>`;
}

/** The exact UNREGISTERING document shape the broker-side sink produces. */
export const UNREGISTERING_XMI = `<?xml version="1.0" encoding="UTF-8"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:services="http://eclipse.org/fennec/services/1.0">
  <services:ServiceEvent type="UNREGISTERING" reference="/1"/>
  <services:ServiceReference id="ref-42"/>
</xmi:XMI>`;

/** A self-contained REGISTERED event document for the given interface. */
export function registeredEventXmi(interfaceName: string, referenceId: string): string {
  const serviceInterface = paymentInterface();
  serviceInterface.name = interfaceName;
  const { provider } = paymentProvider('payments-ts', serviceInterface);

  const event = factory.createServiceEvent();
  event.type = 'REGISTERED';
  const reference = factory.createServiceReference();
  reference.id = referenceId;
  reference.provider = provider;
  event.reference = reference;

  return serializeToXmi(event as any, reference as any, provider as any, serviceInterface as any);
}

/** Bare-reference UNREGISTERING for the given reference id. */
export function unregisteringEventXmi(referenceId: string): string {
  return `<?xml version="1.0" encoding="UTF-8"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:services="http://eclipse.org/fennec/services/1.0">
  <services:ServiceEvent type="UNREGISTERING" reference="/1"/>
  <services:ServiceReference id="${referenceId}"/>
</xmi:XMI>`;
}

/** An OK Diagnostic exactly as the broker sends it: defaults omitted. */
export const OK_DIAGNOSTIC_XMI =
  '<?xml version="1.0" encoding="UTF-8"?>\n<services:Diagnostic xmlns:services="http://eclipse.org/fennec/services/1.0" source="org.gecko.ddsr.broker.core"/>';

/** An ERROR Diagnostic with code/message, as HttpDiagnostics sends with 4xx. */
export function errorDiagnosticXmi(code: number, message: string): string {
  return `<?xml version="1.0" encoding="UTF-8"?>\n<services:Diagnostic xmlns:services="http://eclipse.org/fennec/services/1.0" severity="ERROR" code="${code}" message="${message}" source="org.gecko.ddsr.broker.core"/>`;
}

export interface RecordedRequest {
  url: string;
  method: string;
  headers: Record<string, string>;
  body: string | undefined;
}

/**
 * A scripted fetch: routes are matched in order by method + URL
 * substring; every request is recorded.
 */
export function fakeFetch(
  routes: Array<{ method?: string; urlIncludes: string; status?: number; body?: string; contentType?: string }>
): { fetchFn: typeof fetch; requests: RecordedRequest[] } {
  const requests: RecordedRequest[] = [];
  const fetchFn = (async (input: any, init?: any) => {
    const url = String(input);
    const method = init?.method ?? 'GET';
    requests.push({
      url,
      method,
      headers: { ...(init?.headers ?? {}) },
      body: init?.body === undefined ? undefined : String(init.body),
    });
    const route = routes.find(
      r => url.includes(r.urlIncludes) && (r.method ?? 'GET') === method
    );
    if (!route) {
      return new Response('no route', { status: 500 });
    }
    return new Response(route.body ?? '', {
      status: route.status ?? 200,
      headers: { 'Content-Type': route.contentType ?? 'application/xml' },
    });
  }) as typeof fetch;
  return { fetchFn, requests };
}
