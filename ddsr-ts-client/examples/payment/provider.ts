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
import type { MqttFlavor } from '@ddsr/model';
import { MqttOperationServer } from '@ddsr/transport-mqtt';
import { BROKER_URL, MQTT_URL, PROVIDER_NAME, SERVICE_URL, buildPaymentInterface, buildPaymentProvider } from './payment-api';

const log = (m: string) => console.log(`[ts-provider] ${m}`);

// --- the actual service ------------------------------------------------
const balances = new Map<string, number>();
const DEFAULT_ACCOUNT = 'default';

const serviceUrl = new URL(SERVICE_URL);
const basePath = serviceUrl.pathname.replace(/\/+$/, '');

const server = createServer((request, response) => {
  const url = new URL(request.url ?? '/', SERVICE_URL);
  const reply = (status: number, body: string) => {
    response.writeHead(status, { 'Content-Type': 'text/plain' });
    response.end(body);
  };
  if (request.method === 'POST' && url.pathname === `${basePath}/charge`) {
    const amount = Number(url.searchParams.get('amount'));
    if (Number.isNaN(amount)) return reply(400, 'amount required');
    const balance = (balances.get(DEFAULT_ACCOUNT) ?? 1000) - amount;
    balances.set(DEFAULT_ACCOUNT, balance);
    log(`charge(${amount} ${url.searchParams.get('currency') ?? 'EUR'}) -> ${balance}`);
    return reply(200, String(balance));
  }
  if (request.method === 'GET' && url.pathname === `${basePath}/balance`) {
    const account = url.searchParams.get('accountId') ?? DEFAULT_ACCOUNT;
    return reply(200, String(balances.get(account) ?? balances.get(DEFAULT_ACCOUNT) ?? 1000));
  }
  reply(404, 'not found');
});

// --- registration lifecycle --------------------------------------------
async function main(): Promise<void> {
  await new Promise<void>(resolve => server.listen(Number(serviceUrl.port || 80), resolve));
  log(`serving ${SERVICE_URL}`);

  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    requestor: `${PROVIDER_NAME}-publisher`,
  });

  const payment = buildPaymentInterface();
  log(`local fingerprint: ${fingerprint(payment)}`);
  await client.catalog.ensureEntry(payment);

  const { provider, implementation } = buildPaymentProvider(PROVIDER_NAME, SERVICE_URL, payment, MQTT_URL);

  // The MQTT endpoint listens BEFORE the implementation is announced —
  // same order as the HTTP server above (never advertise a dead
  // endpoint). Both transports share the same balances state.
  let mqttServer: MqttOperationServer | undefined;
  if (MQTT_URL) {
    const mqttFlavor = toArray<MqttFlavor>(implementation.flavors)
      .find(f => (f as { eClass?: () => { name?: string } }).eClass?.()?.name === 'MqttFlavor')!;
    mqttServer = new MqttOperationServer(mqttFlavor, {
      charge: (args) => {
        const amount = Number(args.amount);
        const balance = (balances.get(DEFAULT_ACCOUNT) ?? 1000) - amount;
        balances.set(DEFAULT_ACCOUNT, balance);
        log(`mqtt charge(${amount} ${args.currency ?? 'EUR'}) -> ${balance}`);
        return balance;
      },
      getBalance: (args) => {
        const account = String(args.accountId ?? DEFAULT_ACCOUNT);
        return balances.get(account) ?? balances.get(DEFAULT_ACCOUNT) ?? 1000;
      },
    }, { log: (m) => log(`mqtt: ${m}`) });
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
