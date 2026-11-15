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
 * The card provider of the tag example (#281): one implementation serving
 * two contracts — Payment and InstallmentPlan — from card-provider.xmi.
 * The document asks for the tags: the implementation for
 * payment/method/card, each contract for its payment/terms/… tag.
 *
 *   BROKER_URL=http://localhost:8887/ddsr/rest SERVICE_URL=http://localhost:9095/card \
 *     pnpm exec tsx card-provider.ts
 */

import { readFileSync } from 'node:fs';
import { createServer } from 'node:http';
import { DdsrClientImpl, attachShutdownHooks, deserializeFromXmi, toArray } from '@ddsr/client';
import type { EObject } from '@emfts/core';
import type { RestFlavor, ServiceImplementation, ServiceInterface, ServiceProvider } from '@ddsr/model';
import { restDispatcher } from '@ddsr/flavor-rest';
import { brokerTokens, consumerId, nodeFetch } from './broker-auth';

const BROKER_URL = process.env.BROKER_URL ?? 'http://localhost:8887/ddsr/rest';
const SERVICE_URL = process.env.SERVICE_URL ?? 'http://localhost:9095/card';
const log = (m: string) => console.log(`[card-provider] ${m}`);

const document = new URL('./card-provider.xmi', import.meta.url);
const read = deserializeFromXmi(readFileSync(document, 'utf8'), document.href);
const roots = (Array.isArray(read) ? read : [read]) as EObject[];
const provider = roots.find(root => root.eClass().getName() === 'ServiceProvider') as unknown as ServiceProvider;
const contracts = roots.filter(root => root.eClass().getName() === 'ServiceInterface') as unknown as ServiceInterface[];
const implementation = toArray<ServiceImplementation>(provider.implementations)[0];
const rest = toArray<RestFlavor>(implementation.flavors)[0];
const url = new URL(SERVICE_URL);
rest.host = `${url.protocol}//${url.host}`;
rest.basePath = url.pathname.replace(/\/+$/, '');

// The service: one object for both contracts.
let balance = 1000;
const plans = new Map<string, { amount: number; months: number }>();
const card = {
  charge(amount: number): number {
    balance -= amount;
    return balance;
  },
  getBalance(): number {
    return balance;
  },
  offer(amount: number, months: number): string {
    const id = `plan-${plans.size + 1}`;
    plans.set(id, { amount, months });
    return id;
  },
  accept(planId: string): string {
    const plan = plans.get(planId);
    if (!plan) return `no plan ${planId}`;
    return `${planId}: ${plan.months} x ${(plan.amount / plan.months).toFixed(2)}`;
  },
};

const dispatch = restDispatcher(rest, card, { basePath: rest.basePath, log: m => log(`rest: ${m}`) });
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
      response.writeHead(answer?.status ?? 404, { 'Content-Type': answer?.contentType ?? 'text/plain' });
      response.end(answer?.body ?? 'not found');
    });
  });
});

async function main(): Promise<void> {
  await new Promise<void>(resolve => server.listen(Number(url.port || 80), resolve));
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    requestor: 'payments-card-publisher',
    tokenSupplier: brokerTokens(),
    fetchFn: nodeFetch(),
    consumerId: consumerId(),
  });
  for (const contract of contracts) {
    await client.catalog.ensureEntry(contract);
  }
  const registration = await client.provider.publish(provider, implementation);
  log(`published as reference ${registration.reference.id} — Payment and InstallmentPlan`);
  attachShutdownHooks(client, {
    afterClose: () => new Promise<void>(resolve => server.close(() => resolve())),
    log,
  });
}

main().catch(error => {
  console.error(`[card-provider] startup failed: ${String(error)}`);
  server.close();
  process.exit(1);
});
