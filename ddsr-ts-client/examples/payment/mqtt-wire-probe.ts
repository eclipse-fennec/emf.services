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
 * MQTT wire proof (A2/D11): against a REAL MQTT broker (MQTT_URL,
 * default mqtt://localhost:1883), subscribe via MqttEventSource and
 * publish the exact self-contained event document the Java broker-side
 * sink produces — once on <prefix>/<interface>, once on
 * <prefix>/_unknown. Asserts both arrive decoded over TCP.
 *
 * Output contract: MQTT_PROBE_OK / MQTT_PROBE_FAIL <reason>.
 */

import { connectAsync } from 'mqtt';
import type { ServiceEvent } from '@ddsr/model';
import { MqttEventSource } from '@ddsr/transport-mqtt';

const MQTT_URL = process.env.MQTT_URL ?? 'mqtt://localhost:1883';
const PREFIX = process.env.TOPIC_PREFIX ?? 'ddsr/events';

const EVENT_XMI = `<?xml version="1.0" encoding="UTF-8"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:services="http://eclipse.org/fennec/services/1.0">
  <services:ServiceEvent type="UNREGISTERING" reference="/1"/>
  <services:ServiceReference id="ref-42"/>
</xmi:XMI>`;

function fail(reason: string): never {
  console.log(`MQTT_PROBE_FAIL ${reason}`);
  process.exit(1);
}

async function main(): Promise<void> {
  const received: Array<ServiceEvent> = [];
  let established = false;

  const source = new MqttEventSource({
    brokerUrl: MQTT_URL,
    topicPrefix: PREFIX,
    clientId: 'ddsr-mqtt-probe',
  });
  const subscription = source.open({
    onStreamEstablished: () => {
      established = true;
    },
    onEvent: event => {
      received.push(event);
    },
  });

  for (let waited = 0; waited < 10000 && !established; waited += 100) {
    await new Promise(r => setTimeout(r, 100));
  }
  if (!established) fail('no MQTT connection within 10s');

  const publisher = await connectAsync(MQTT_URL, { clientId: 'ddsr-mqtt-probe-pub' });
  await publisher.publishAsync(`${PREFIX}/Payment`, EVENT_XMI, { qos: 0, retain: false });
  await publisher.publishAsync(`${PREFIX}/_unknown`, EVENT_XMI, { qos: 0, retain: false });
  await publisher.endAsync();

  for (let waited = 0; waited < 10000 && received.length < 2; waited += 100) {
    await new Promise(r => setTimeout(r, 100));
  }
  await subscription.close();

  if (received.length < 2) fail(`only ${received.length}/2 events arrived over TCP`);
  if (received.some(e => e.type !== 'UNREGISTERING' || e.reference?.id !== 'ref-42')) {
    fail('event decoded incorrectly');
  }
  console.log('MQTT_PROBE_OK');
  process.exit(0);
}

main().catch(error => fail(String(error)));
