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
import { ServiceListenerRegistry } from '../src/internal/service-listener-registry';
import type { DdsrEventSource, EventSourceHandler, EventSubscription } from '../src/events/event-source';
import { deserializeFromXmi } from '../src/xmi/xmi-support';
import { asRoots, firstOfClass } from '../src/internal/emf-util';
import { registeredEventXmi, unregisteringEventXmi } from './fixtures';

class FakeSource implements DdsrEventSource {
  opened = 0;
  closed = 0;
  handler: EventSourceHandler | undefined;

  open(handler: EventSourceHandler): EventSubscription {
    this.opened++;
    this.handler = handler;
    return {
      close: async () => {
        this.closed++;
        this.handler = undefined;
      },
    };
  }
}

function eventOf(xmi: string): ServiceEvent {
  const event = firstOfClass<ServiceEvent>(asRoots(deserializeFromXmi(xmi)), 'ServiceEvent');
  if (!event) throw new Error('fixture without ServiceEvent');
  return event;
}

const noRefresh = () => undefined;

describe('ServiceListenerRegistry', () => {
  it('opens the stream lazily on the first listener only', () => {
    const source = new FakeSource();
    const registry = new ServiceListenerRegistry(source, noRefresh);
    expect(source.opened).toBe(0);
    registry.add('Payment', undefined, () => undefined);
    registry.add('Payment', undefined, () => undefined);
    expect(source.opened).toBe(1);
  });

  it('closes the stream when the last listener is removed', () => {
    const source = new FakeSource();
    const registry = new ServiceListenerRegistry(source, noRefresh);
    const listener = () => undefined;
    const unsubscribe = registry.add('Payment', undefined, listener);
    unsubscribe();
    expect(source.closed).toBe(1);
  });

  it('routes a self-contained event to the matching listener only', () => {
    const source = new FakeSource();
    const registry = new ServiceListenerRegistry(source, noRefresh);
    const payment: ServiceEvent[] = [];
    const other: ServiceEvent[] = [];
    registry.add('Payment', undefined, e => payment.push(e));
    registry.add('Other', undefined, e => other.push(e));

    registry.onEvent(eventOf(registeredEventXmi('Payment', 'ref-1')));
    expect(payment).toHaveLength(1);
    expect(other).toHaveLength(0);
  });

  it('delivers an event with unknown interfaces to every listener (over-delivery rule)', () => {
    const registry = new ServiceListenerRegistry(new FakeSource(), noRefresh);
    const seen: string[] = [];
    registry.add('Payment', undefined, () => seen.push('payment'));
    registry.add('Other', undefined, () => seen.push('other'));

    registry.onEvent(eventOf(unregisteringEventXmi('unknown-ref')));
    expect(seen).toEqual(['payment', 'other']);
  });

  it('memoizes interfaces from REGISTERED and routes a later bare UNREGISTERING precisely', () => {
    const registry = new ServiceListenerRegistry(new FakeSource(), noRefresh);
    const payment: string[] = [];
    const other: string[] = [];
    registry.add('Payment', undefined, e => payment.push(e.type));
    registry.add('Other', undefined, e => other.push(e.type));

    registry.onEvent(eventOf(registeredEventXmi('Payment', 'ref-7')));
    registry.onEvent(eventOf(unregisteringEventXmi('ref-7')));

    expect(payment).toEqual(['REGISTERED', 'UNREGISTERING']);
    expect(other).toEqual([]);
  });

  it('forgets a reference after UNREGISTERING — the next bare event broadcasts again', () => {
    const registry = new ServiceListenerRegistry(new FakeSource(), noRefresh);
    const other: string[] = [];
    registry.add('Other', undefined, e => other.push(e.type));

    registry.onEvent(eventOf(registeredEventXmi('Payment', 'ref-9')));
    registry.onEvent(eventOf(unregisteringEventXmi('ref-9')));
    expect(other).toEqual([]); // routed away while known
    registry.onEvent(eventOf(unregisteringEventXmi('ref-9')));
    expect(other).toEqual(['UNREGISTERING']); // unknown again → broadcast
  });

  it('noteReference routes events for references learned from lookups', () => {
    const registry = new ServiceListenerRegistry(new FakeSource(), noRefresh);
    const payment: string[] = [];
    const other: string[] = [];
    registry.add('Payment', undefined, e => payment.push(e.type));
    registry.add('Other', undefined, e => other.push(e.type));

    registry.noteReference('ref-11', ['Payment']);
    registry.onEvent(eventOf(unregisteringEventXmi('ref-11')));
    expect(payment).toEqual(['UNREGISTERING']);
    expect(other).toEqual([]);
  });

  it('a throwing listener does not stop the others', () => {
    const registry = new ServiceListenerRegistry(new FakeSource(), noRefresh, () => undefined);
    const seen: string[] = [];
    registry.add('Payment', undefined, () => {
      throw new Error('boom');
    });
    registry.add('Payment', undefined, () => seen.push('ok'));

    registry.onEvent(eventOf(registeredEventXmi('Payment', 'ref-1')));
    expect(seen).toEqual(['ok']);
  });

  it('without an event source the registration is accepted and non-delivering', () => {
    const registry = new ServiceListenerRegistry(undefined, noRefresh);
    expect(() => registry.add('Payment', undefined, () => undefined)).not.toThrow();
  });

  it('transportAvailable opens the stream for waiting listeners', () => {
    const registry = new ServiceListenerRegistry(undefined, noRefresh);
    registry.add('Payment', undefined, () => undefined);
    const source = new FakeSource();
    registry.transportAvailable(source);
    expect(source.opened).toBe(1);
  });

  it('onStreamEstablished triggers the snapshot refresh', async () => {
    let refreshes = 0;
    const source = new FakeSource();
    const registry = new ServiceListenerRegistry(source, () => {
      refreshes++;
    });
    registry.add('Payment', undefined, () => undefined);
    await source.handler?.onStreamEstablished();
    expect(refreshes).toBe(1);
  });

  it('subscribedInterfaces deduplicates', () => {
    const registry = new ServiceListenerRegistry(new FakeSource(), noRefresh);
    registry.add('Payment', undefined, () => undefined);
    registry.add('Payment', undefined, () => undefined);
    registry.add('Other', undefined, () => undefined);
    expect(registry.subscribedInterfaces().sort()).toEqual(['Other', 'Payment']);
  });
});
