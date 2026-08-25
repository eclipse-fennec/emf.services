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

import { describe, expect, it } from 'vitest';
import type { ServiceEvent } from '@ddsr/model';
import { MqttEventSource } from '../src/mqtt-event-source';
import type { MqttClientLike } from '../src/mqtt-event-source';

/** The exact wire format the broker-side sink produces (Java parity fixture). */
const EVENT_XMI = `<?xml version="1.0" encoding="UTF-8"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:services="http://eclipse.org/fennec/services/1.0">
  <services:ServiceEvent type="UNREGISTERING" reference="/1"/>
  <services:ServiceReference id="ref-42"/>
</xmi:XMI>`;

class FakeMqttClient implements MqttClientLike {
  listeners = new Map<string, Array<(...args: any[]) => void>>();
  subscriptions: Array<{ filter: string; qos?: number }> = [];
  ended = false;

  on(event: string, listener: (...args: any[]) => void): this {
    const list = this.listeners.get(event) ?? [];
    list.push(listener);
    this.listeners.set(event, list);
    return this;
  }

  emit(event: string, ...args: unknown[]): void {
    for (const listener of this.listeners.get(event) ?? []) listener(...args);
  }

  async subscribeAsync(topicFilter: string, options?: { qos?: 0 | 1 | 2 }): Promise<unknown> {
    this.subscriptions.push({ filter: topicFilter, qos: options?.qos });
    return [];
  }

  async endAsync(): Promise<void> {
    this.ended = true;
  }
}

function sourceWith(client: FakeMqttClient, topicPrefix?: string) {
  return new MqttEventSource({
    brokerUrl: 'mqtt://test:1883',
    topicPrefix,
    clientFactory: () => client,
    log: () => undefined,
  });
}

async function settle(): Promise<void> {
  await new Promise(resolve => setTimeout(resolve, 10));
}

describe('MqttEventSource', () => {
  it('subscribes <prefix>/# because open() carries no interest set', async () => {
    const client = new FakeMqttClient();
    const source = sourceWith(client);
    const subscription = source.open({ onEvent: () => undefined, onStreamEstablished: () => undefined });
    await settle();
    client.emit('connect');
    await settle();

    expect(client.subscriptions).toEqual([{ filter: 'ddsr/events/#', qos: 0 }]);
    await subscription.close();
  });

  it('honors a custom topic prefix', () => {
    expect(sourceWith(new FakeMqttClient(), 'acme/ddsr/').topicFilter()).toBe('acme/ddsr/#');
  });

  it('signals establish after subscribe and BEFORE messages of that connection', async () => {
    const client = new FakeMqttClient();
    const order: string[] = [];
    const subscription = sourceWith(client).open({
      onStreamEstablished: async () => {
        await new Promise(resolve => setTimeout(resolve, 20));
        order.push('snapshot-done');
      },
      onEvent: () => order.push('event'),
    });
    await settle();
    client.emit('connect');
    // message races in while the snapshot refresh is still running
    client.emit('message', 'ddsr/events/Payment', new TextEncoder().encode(EVENT_XMI));
    await new Promise(resolve => setTimeout(resolve, 60));

    expect(order).toEqual(['snapshot-done', 'event']);
    await subscription.close();
  });

  it('re-fires establish on reconnect (snapshot-on-reconnect parity)', async () => {
    const client = new FakeMqttClient();
    let establishes = 0;
    const subscription = sourceWith(client).open({
      onStreamEstablished: () => {
        establishes++;
      },
      onEvent: () => undefined,
    });
    await settle();
    client.emit('connect');
    await settle();
    client.emit('connect'); // mqtt.js automatic reconnect
    await settle();

    expect(establishes).toBe(2);
    expect(client.subscriptions).toHaveLength(2);
    await subscription.close();
  });

  it('decodes the same self-contained payload as SSE, from any topic incl. _unknown', async () => {
    const client = new FakeMqttClient();
    const events: ServiceEvent[] = [];
    const subscription = sourceWith(client).open({
      onStreamEstablished: () => undefined,
      onEvent: e => {
        events.push(e);
      },
    });
    await settle();
    client.emit('connect');
    await settle();
    client.emit('message', 'ddsr/events/_unknown', new TextEncoder().encode(EVENT_XMI));
    await settle();

    expect(events).toHaveLength(1);
    expect(events[0].type).toBe('UNREGISTERING');
    expect(events[0].reference?.id).toBe('ref-42');
    await subscription.close();
  });

  it('ignores empty payloads and skips undecodable ones without dying', async () => {
    const client = new FakeMqttClient();
    const events: ServiceEvent[] = [];
    const subscription = sourceWith(client).open({
      onStreamEstablished: () => undefined,
      onEvent: e => {
        events.push(e);
      },
    });
    await settle();
    client.emit('connect');
    await settle();
    client.emit('message', 'ddsr/events/Payment', new TextEncoder().encode(''));
    client.emit('message', 'ddsr/events/Payment', new TextEncoder().encode('<kaputt'));
    client.emit('message', 'ddsr/events/Payment', new TextEncoder().encode(EVENT_XMI));
    await settle();

    expect(events).toHaveLength(1);
    await subscription.close();
  });

  it('a throwing handler does not break subsequent deliveries', async () => {
    const client = new FakeMqttClient();
    let calls = 0;
    const subscription = sourceWith(client).open({
      onStreamEstablished: () => undefined,
      onEvent: () => {
        calls++;
        if (calls === 1) throw new Error('boom');
      },
    });
    await settle();
    client.emit('connect');
    await settle();
    client.emit('message', 'ddsr/events/Payment', new TextEncoder().encode(EVENT_XMI));
    client.emit('message', 'ddsr/events/Payment', new TextEncoder().encode(EVENT_XMI));
    await settle();

    expect(calls).toBe(2);
    await subscription.close();
  });

  it('close ends the client and drops later messages', async () => {
    const client = new FakeMqttClient();
    const events: ServiceEvent[] = [];
    const subscription = sourceWith(client).open({
      onStreamEstablished: () => undefined,
      onEvent: e => {
        events.push(e);
      },
    });
    await settle();
    client.emit('connect');
    await settle();
    await subscription.close();
    expect(client.ended).toBe(true);
    client.emit('message', 'ddsr/events/Payment', new TextEncoder().encode(EVENT_XMI));
    await settle();
    expect(events).toHaveLength(0);
  });
});
