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
import { CE_TYPE_BROADCAST, CE_TYPE_BROADCAST_END, newEnvelope, writeStructured } from '@ddsr/client';
import { MqttEventSource } from '../src/mqtt-event-source';
import type { MqttClientLike } from '../src/mqtt-event-source';

/** The exact wire format the broker-side sink produces (Java parity fixture). */
const EVENT_XMI = `<?xml version="1.0" encoding="UTF-8"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:services="http://eclipse.org/fennec/services/1.0">
  <services:ServiceEvent type="UNREGISTERING" reference="/1"/>
  <services:ServiceReference id="ref-42"/>
</xmi:XMI>`;

/**
 * The wire shape since #252: a value of the broker's broadcast - the
 * event as the value of a result, inside a CloudEvent `broadcast` in
 * structured mode, built with the writer the broker uses.
 */
function lifecycleMessage(document: string): Uint8Array {
  const wrapped = document
    .replace('<xmi:XMI ', '<xmi:XMI xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ')
    .replace(/<services:ServiceEvent\b([^>]*?)\/>/,
      (_whole, attributes) => '<services:ServiceInvocationResult><value xsi:type="services:EObjectProperty" name="result">'
        + `<value xsi:type="services:ServiceEvent"${attributes}/></value></services:ServiceInvocationResult>`);
  return writeStructured(newEnvelope(CE_TYPE_BROADCAST, '/test/broker', 'application/xml'), wrapped);
}

/** The end of the broadcast: the broker lost an event (#124). */
function endOfBroadcast(): Uint8Array {
  return writeStructured(newEnvelope(CE_TYPE_BROADCAST_END, '/test/broker'), undefined);
}

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
    client.emit('message', 'ddsr/events/Payment', lifecycleMessage(EVENT_XMI));
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

  it('reports a dropped connection as lost, and the reconnect as established again (#167)', async () => {
    const client = new FakeMqttClient();
    const order: string[] = [];
    const source = sourceWith(client);
    const subscription = source.open({
      onStreamEstablished: () => {
        order.push('established');
      },
      onStreamLost: () => {
        order.push('lost');
      },
      onEvent: () => undefined,
    });
    await settle();
    client.emit('connect');
    await settle();
    client.emit('close');
    client.emit('connect');
    await settle();

    expect(source.transport).toBe('mqtt');
    expect(order).toEqual(['established', 'lost', 'established']);
    await subscription.close();
  });

  it('decodes the same self-contained payload as SSE, from any topic under the prefix', async () => {
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
    client.emit('message', 'ddsr/events/_unknown', lifecycleMessage(EVENT_XMI));
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
    client.emit('message', 'ddsr/events/Payment', lifecycleMessage(EVENT_XMI));
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
    client.emit('message', 'ddsr/events/Payment', lifecycleMessage(EVENT_XMI));
    client.emit('message', 'ddsr/events/Payment', lifecycleMessage(EVENT_XMI));
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
    client.emit('message', 'ddsr/events/Payment', lifecycleMessage(EVENT_XMI));
    await settle();
    expect(events).toHaveLength(0);
  });

  // ------------------------------------------------------------------
  // The broker admitting a loss (#124)
  // ------------------------------------------------------------------

  it('the end of the broadcast asks for a fresh snapshot, not for decoding', async () => {
    const client = new FakeMqttClient();
    const seen: string[] = [];
    const subscription = sourceWith(client).open({
      onStreamEstablished: () => seen.push('snapshot'),
      onEvent: () => seen.push('event'),
    });
    await settle();
    client.emit('connect');
    await settle();
    seen.length = 0;

    client.emit('message', 'ddsr/events', endOfBroadcast());
    await settle();

    expect(seen).toEqual(['snapshot']);
    await subscription.close();
  });

  it('the end of the broadcast is read on the configured prefix too', async () => {
    const client = new FakeMqttClient();
    const seen: string[] = [];
    const subscription = sourceWith(client, 'acme/ddsr').open({
      onStreamEstablished: () => seen.push('snapshot'),
      onEvent: () => seen.push('event'),
    });
    await settle();
    client.emit('connect');
    await settle();
    seen.length = 0;

    client.emit('message', 'acme/ddsr', endOfBroadcast());
    await settle();

    expect(seen).toEqual(['snapshot']);
    await subscription.close();
  });

  it('events after a resync keep flowing — a loss is not a shutdown', async () => {
    const client = new FakeMqttClient();
    const seen: string[] = [];
    const subscription = sourceWith(client).open({
      onStreamEstablished: () => seen.push('snapshot'),
      onEvent: () => seen.push('event'),
    });
    await settle();
    client.emit('connect');
    await settle();
    seen.length = 0;

    client.emit('message', 'ddsr/events', endOfBroadcast());
    client.emit('message', 'ddsr/events', lifecycleMessage(EVENT_XMI));
    await settle();

    expect(seen).toEqual(['snapshot', 'event']);
    await subscription.close();
  });
});
