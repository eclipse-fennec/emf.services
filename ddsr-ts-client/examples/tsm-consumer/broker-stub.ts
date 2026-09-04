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

import type { DdsrEventSource, EventSourceHandler, EventSubscription } from '@ddsr/client';
import { deserializeFromXmi } from '@ddsr/client';
import type { ServiceEvent } from '@ddsr/model';

/**
 * A broker and a payment provider, small enough to run in this process.
 *
 * It exists so the example runs with nothing installed: point BROKER_URL
 * at a real broker and the same code talks to that instead. Both seams
 * it uses — `fetchFn` and `eventSource` — are the ones the client offers
 * for exactly this, so nothing in the modules changes with the switch.
 */
export class BrokerStub {
  private present = true;
  private handler: EventSourceHandler | undefined;
  readonly charges: Array<{ amount: unknown; accountId: unknown }> = [];

  /** Stands in for the broker's REST API and the provider's endpoint. */
  readonly fetchFn: typeof fetch = async (input) => {
    const url = String(input instanceof Request ? input.url : input);

    if (url.includes('/references')) {
      return new Response(this.present ? LOOKUP_RESULT : EMPTY_RESULT,
        { status: 200, headers: { 'content-type': 'application/xml' } });
    }
    if (url.includes('/payments/charge')) {
      return new Response(JSON.stringify({ remaining: 4200 }),
        { status: 200, headers: { 'content-type': 'application/json' } });
    }
    return new Response('not found', { status: 404 });
  };

  /** The event stream, as a transport the client already knows how to use. */
  readonly eventSource: DdsrEventSource = {
    open: (handler: EventSourceHandler): EventSubscription => {
      this.handler = handler;
      void handler.onStreamEstablished();
      return { close: async () => { this.handler = undefined; } };
    },
  };

  /** The provider goes away, as it would on a shutdown. */
  withdraw(): void {
    this.present = false;
    this.emit(UNREGISTERING);
  }

  /** And comes back. */
  restore(): void {
    this.present = true;
    this.emit(REGISTERED);
  }

  private emit(xmi: string): void {
    const event = deserializeFromXmi(xmi);
    this.handler?.onEvent((Array.isArray(event) ? event[0] : event) as ServiceEvent);
  }
}

const NS = 'xmlns:xmi="http://www.omg.org/XMI" '
  + 'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" '
  + 'xmlns:services="http://eclipse.org/fennec/services/1.0"';

/** What the broker answers a lookup with: the reference, its provider, the contract. */
const LOOKUP_RESULT = `<?xml version="1.0" encoding="UTF-8"?>
<xmi:XMI xmi:version="2.0" ${NS}>
  <services:LocalServiceRegistry name="lookup-result">
    <references id="ref-payments-1" provider="/0/@providers.0">
      <properties xsi:type="services:StringProperty" name="ddsr.provider.lang" value="java"/>
      <properties xsi:type="services:IntProperty" name="service.ranking" value="7"/>
      <properties xsi:type="services:BoolProperty" name="payments.sandbox" value="true"/>
    </references>
    <providers name="payments-java">
      <implementations name="payments-java-rest" implementationId="java:payments:1.0.0" serviceInterfaces="/1">
        <flavors xsi:type="services:RestFlavor" name="rest" host="http://provider.example:9090" basePath="/payments">
          <operationFlavors xsi:type="services:RestOperationFlavor" name="charge" path="/charge" method="POST" operation="/1/@operations.0"/>
          <operationFlavors xsi:type="services:RestOperationFlavor" name="getBalance" path="/balance" operation="/1/@operations.1"/>
        </flavors>
      </implementations>
    </providers>
  </services:LocalServiceRegistry>
  <services:ServiceInterface name="Payment" version="1.0.0">
    <operations name="charge" returnType="double">
      <parameters name="amount" type="double"/>
      <parameters name="currency" type="string" index="1" optional="true" defaultValue="EUR"/>
    </operations>
    <operations name="getBalance" returnType="double">
      <parameters name="accountId" type="string"/>
    </operations>
  </services:ServiceInterface>
</xmi:XMI>`;

/** The same envelope with nothing in it — the provider is gone. */
const EMPTY_RESULT = `<?xml version="1.0" encoding="UTF-8"?>
<xmi:XMI xmi:version="2.0" ${NS}>
  <services:LocalServiceRegistry name="lookup-result"/>
</xmi:XMI>`;

const UNREGISTERING = `<?xml version="1.0" encoding="UTF-8"?>
<services:ServiceEvent ${NS} xmi:version="2.0" type="UNREGISTERING">
  <reference id="ref-payments-1"/>
</services:ServiceEvent>`;

const REGISTERED = `<?xml version="1.0" encoding="UTF-8"?>
<services:ServiceEvent ${NS} xmi:version="2.0" type="REGISTERED">
  <reference id="ref-payments-1"/>
</services:ServiceEvent>`;
