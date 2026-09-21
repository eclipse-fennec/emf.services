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

/**
 * The complete TS Payment provider: serves the REST endpoint, ensures
 * the catalog entry, publishes the implementation, and on SIGINT/SIGTERM
 * withdraws FIRST (waiting for the broker's confirmation, i.e. after
 * consumers were informed) and only THEN stops the HTTP server — the
 * FR-P3 lifecycle order.
 *
 *   BROKER_URL=http://localhost:8887/ddsr/rest SERVICE_URL=http://localhost:9090/payments \
 *     pnpm exec tsx provider.ts
 */

import { createServer } from 'node:http';
import { DdsrClientImpl, attachShutdownHooks, fingerprint, toArray } from '@ddsr/client';
import type { MqttFlavor, RestFlavor } from '@ddsr/model';
import { MqttOperationServer, operationHandlers } from '@ddsr/transport-mqtt';
import { restDispatcher } from '@ddsr/flavor-rest';
import { BROKER_URL, MQTT_URL, PROVIDER_NAME, SERVICE_URL, buildPaymentInterface, buildPaymentProvider } from './payment-api';

const log = (m: string) => console.log(`[ts-provider] ${m}`);

// --- the actual service ------------------------------------------------
// An object whose methods are named after the contract's operations, and
// nothing else (#154). Where an argument travels, which topic carries a
// call and what a REST answer looks like are the flavor's business; this
// is the business logic, on both transports at once.
const balances = new Map<string, number>();
const DEFAULT_ACCOUNT = 'default';

const payments = {
  charge(amount: number, currency?: string): number {
    const balance = (balances.get(DEFAULT_ACCOUNT) ?? 1000) - amount;
    balances.set(DEFAULT_ACCOUNT, balance);
    log(`charge(${amount} ${currency ?? 'EUR'}) -> ${balance}`);
    return balance;
  },
  getBalance(accountId?: string): number {
    const account = accountId ?? DEFAULT_ACCOUNT;
    return balances.get(account) ?? balances.get(DEFAULT_ACCOUNT) ?? 1000;
  },
};

const serviceUrl = new URL(SERVICE_URL);
const basePath = serviceUrl.pathname.replace(/\/+$/, '');

const paymentInterface = buildPaymentInterface();
const paymentProvider = buildPaymentProvider(PROVIDER_NAME, SERVICE_URL, paymentInterface, MQTT_URL);
const restFlavor = toArray<RestFlavor>(paymentProvider.implementation.flavors)
  .find(f => (f as { eClass?: () => { name?: string } }).eClass?.()?.name === 'RestFlavor')!;

// The routing is the flavor's, read back with the same statements a
// consumer writes a request by (#154). Nothing below knows that
// `charge` takes its amount from the path and its currency from a
// header — the published flavor says so, and both ends read it.
const dispatch = restDispatcher(restFlavor, payments, {
  basePath,
  log: (m) => log(`rest: ${m}`),
});

const server = createServer((request, response) => {
  const chunks: Buffer[] = [];
  request.on('data', chunk => chunks.push(chunk as Buffer));
  request.on('end', () => {
    void dispatch({
      method: request.method ?? 'GET',
      url: request.url ?? '/',
      headers: request.headers,
      body: chunks.length > 0 ? Buffer.concat(chunks).toString('utf8') : undefined,
    }).then(answer => {
      if (!answer) {
        response.writeHead(404, { 'Content-Type': 'text/plain' });
        response.end('not found');
        return;
      }
      response.writeHead(answer.status, { 'Content-Type': answer.contentType });
      response.end(answer.body ?? '');
    }).catch(error => {
      response.writeHead(500, { 'Content-Type': 'text/plain' });
      response.end(String(error));
    });
  });
});

// --- registration lifecycle --------------------------------------------
async function main(): Promise<void> {
  await new Promise<void>(resolve => server.listen(Number(serviceUrl.port || 80), resolve));
  log(`serving ${SERVICE_URL}`);

  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    requestor: `${PROVIDER_NAME}-publisher`,
    // Which system this is, for X-DDSR-Origin (#132). The harness reads
    // it back off the registration, which is how the cross-language
    // half of the origin is demonstrated rather than assumed.
    originLabel: process.env.DDSR_ORIGIN_LABEL ?? 'payments-ts-harness',
  });

  const payment = paymentInterface;
  log(`local fingerprint: ${fingerprint(payment)}`);
  await client.catalog.ensureEntry(payment);

  const { provider, implementation } = paymentProvider;

  // The MQTT endpoint listens BEFORE the implementation is announced —
  // same order as the HTTP server above (never advertise a dead
  // endpoint). Both transports share the same balances state.
  let mqttServer: MqttOperationServer | undefined;
  if (MQTT_URL) {
    const mqttFlavor = toArray<MqttFlavor>(implementation.flavors)
      .find(f => (f as { eClass?: () => { name?: string } }).eClass?.()?.name === 'MqttFlavor')!;
    // The same object as over REST, and no handler written per
    // operation: the contract says what they are called and in which
    // order their arguments come (#154).
    mqttServer = new MqttOperationServer(mqttFlavor, operationHandlers(payment, payments),
      { log: (m) => log(`mqtt: ${m}`) });
    await mqttServer.start();
    log(`serving Payment over MQTT at ${MQTT_URL}`);
  }

  const registration = await client.provider.publish(provider, implementation);
  log(`published as reference ${registration.reference.id}`);

  attachShutdownHooks(client, {
    afterClose: async () => {
      log('unregistration confirmed by broker — stopping the endpoint now');
      await mqttServer?.stop();
      await new Promise<void>((resolve, reject) => {
        server.close(error => (error ? reject(error) : resolve()));
      });
    },
    log,
  });
  log('running — Ctrl-C withdraws before the endpoint stops');
}

main().catch(error => {
  console.error(`[ts-provider] startup failed: ${String(error)}`);
  server.close();
  process.exit(1);
});
