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
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import type { DdsrEventSource, EventSourceHandler, EventSubscription } from '@ddsr/client';

export const BROKER_URL = process.env.BROKER_URL ?? 'http://broker.example/ddsr/rest';

/**
 * A broker and a payment endpoint, in this process, so the example runs
 * with nothing installed. Point BROKER_URL at a real broker and this is
 * not used at all — the modules do not change either way, because
 * fetchFn and eventSource are seams the client offers for exactly this.
 *
 * The lookup answer is assembled around the catalog document that
 * @ddsr/stubs was generated from, so the contract the stub serves and
 * the contract the component was compiled against are the same one.
 */
class BrokerStub {
  readonly fetchFn: typeof fetch = async (input) => {
    const url = String(input instanceof Request ? input.url : input);
    if (url.includes('/references')) {
      return new Response(lookupResult(), { status: 200, headers: { 'content-type': 'application/xml' } });
    }
    if (url.includes('/payments/charge')) {
      return new Response('800', { status: 200, headers: { 'content-type': 'application/json' } });
    }
    if (url.includes('/payments/balance')) {
      return new Response('5000', { status: 200, headers: { 'content-type': 'application/json' } });
    }
    return new Response('not found', { status: 404 });
  };

  readonly eventSource: DdsrEventSource = {
    open: (handler: EventSourceHandler): EventSubscription => {
      void handler.onStreamEstablished();
      return { close: async () => undefined };
    },
  };
}

export const stub: BrokerStub | undefined = process.env.BROKER_URL ? undefined : new BrokerStub();

/**
 * The reference and its provider, wrapped around the Payment contract
 * taken verbatim from the catalog @ddsr/stubs was generated from — so
 * the sd1 the module looks up by is the sd1 this answer hashes to.
 */
function lookupResult(): string {
  const here = dirname(fileURLToPath(import.meta.url));
  const catalog = readFileSync(
    join(here, '..', '..', 'packages', 'ddsr-stubs', 'catalog', 'services-catalog.xmi'), 'utf8');

  const payment = catalog.match(/ {2}<catalog name="Payment"[\s\S]*?\n {2}<\/catalog>/)?.[0];
  if (!payment) throw new Error('no Payment entry in the catalog document');
  const contract = payment
    .replace('<catalog ', '<services:ServiceInterface ')
    .replace('</catalog>', '</services:ServiceInterface>');

  return `<?xml version="1.0" encoding="UTF-8"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:services="http://eclipse.org/fennec/services/1.0">
  <services:LocalServiceRegistry name="lookup-result">
    <references id="ref-payments-1" provider="/0/@providers.0">
      <properties xsi:type="services:StringProperty" name="ddsr.provider.lang" value="java"/>
      <properties xsi:type="services:IntProperty" name="service.ranking" value="10"/>
    </references>
    <providers name="payments-java">
      <implementations name="payments-java-rest" implementationId="java:payments:1.0.0" serviceInterfaces="/1">
        <flavors xsi:type="services:RestFlavor" name="rest" host="http://provider.example:9091" basePath="/payments">
          <operationFlavors xsi:type="services:RestOperationFlavor" name="charge" path="/charge" method="POST" operation="/1/@operations.0"/>
          <operationFlavors xsi:type="services:RestOperationFlavor" name="getBalance" path="/balance" operation="/1/@operations.1"/>
        </flavors>
      </implementations>
    </providers>
  </services:LocalServiceRegistry>
${contract}
</xmi:XMI>`;
}
