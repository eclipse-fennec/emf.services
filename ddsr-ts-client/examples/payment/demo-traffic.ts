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
 * The TypeScript half of the observability demo (#146): a consumer
 * that keeps calling, with OpenTelemetry behind the seam.
 *
 * What it is for is one picture — a trace that starts in a TypeScript
 * process, asks a Java broker who serves a contract, and ends in a
 * Java provider. Nothing crosses between them but a `traceparent`
 * header, which is why this works at all: the format is W3C, and
 * neither end knows the other's language.
 *
 *   OTEL_SERVICE_NAME=fennec-ts-consumer \
 *   OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
 *   BROKER_URL=http://localhost:8887/ddsr/rest \
 *     node --import tsx demo-traffic.ts
 */

import { RestFlavorPlugin } from '@ddsr/flavor-rest';
import { DdsrClientImpl, clientOrigin } from '@ddsr/client';
import { startTelemetry } from '@ddsr/telemetry-otel';
import { BROKER_URL } from './payment-api';

const log = (message: string) => console.log(`[ts-demo] ${message}`);

const serviceName = process.env.OTEL_SERVICE_NAME ?? 'fennec-ts-consumer';
const endpoint = process.env.OTEL_EXPORTER_OTLP_ENDPOINT ?? 'http://localhost:4318';
const intervalMillis = Number(process.env.DEMO_INTERVAL_MILLIS ?? 2000);
const account = process.env.DEMO_ACCOUNT ?? 'demo-account';

const telemetry = startTelemetry({ serviceName, endpoint });

// One identity for both halves: the client stamps it on every broker
// call, the flavor plugin on every invocation, and both spans carry
// it (#125).
const origin = clientOrigin(serviceName);

const client = DdsrClientImpl.create({
  brokerUrl: BROKER_URL,
  flavorPlugins: [new RestFlavorPlugin({
    originLabel: serviceName,
    origin: origin.token,
    tracer: telemetry.tracer,
  })],
  consumerId: serviceName,
  originLabel: serviceName,
  originRuntimeId: origin.runtimeId,
  tracer: telemetry.tracer,
});

let running = true;

/**
 * One round: find, call, let go — the three stages any consumer goes
 * through, inside one span so they read as one unit of work rather
 * than as unrelated traces.
 */
async function tick(): Promise<void> {
  await telemetry.during('Demo/tick', async () => {
    const locator = await client.consumer.findOne('Payment');
    if (!locator) {
      log('nobody serves Payment right now');
      return;
    }
    try {
      const balance = await locator.invoke('getBalance', { accountId: account });
      // Every third round moves money, so a dashboard shows two
      // operations rather than one repeated forever.
      if (Math.floor(Math.random() * 3) === 0) {
        const left = await locator.invoke('charge', { amount: 5 + Math.random() * 15, currency: 'EUR' });
        log(`charged, ${left} left (balance was ${balance})`);
      }
    } finally {
      if (locator.reference?.id) {
        // The claim goes back, so the broker's lease count stays
        // honest — and a graceful handover does not wait for us.
        await client.consumer.release(locator.reference.id).catch(() => undefined);
      }
    }
  });
}

async function loop(): Promise<void> {
  log(`calling Payment every ${intervalMillis} ms, reporting to ${endpoint} as ${serviceName}`);
  while (running) {
    try {
      await tick();
    } catch (error) {
      // A provider that is starting, restarting or gone is the normal
      // state of a distributed demo, and not a reason to stop.
      log(`call failed: ${String(error)}`);
    }
    await new Promise(resolve => setTimeout(resolve, intervalMillis));
  }
}

async function stop(): Promise<void> {
  running = false;
  await client.close().catch(() => undefined);
  // Flush what is pending: a demo that is stopped mid-second should
  // still show its last calls.
  await telemetry.shutdown().catch(() => undefined);
  log('stopped');
  process.exit(0);
}

process.on('SIGINT', () => void stop());
process.on('SIGTERM', () => void stop());

loop().catch(error => {
  console.error(`[ts-demo] failed: ${String(error)}`);
  process.exit(1);
});
