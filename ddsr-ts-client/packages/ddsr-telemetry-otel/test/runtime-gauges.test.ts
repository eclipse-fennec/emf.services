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

import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  AggregationTemporality, InstrumentType, MeterProvider, MetricReader, type GaugeMetricData, type MetricData,
} from '@opentelemetry/sdk-metrics';
import type { BindingDTO, ClientRuntime, ClientRuntimeDTO } from '@ddsr/client';
import { watchRuntime } from '../src/runtime-gauges';
import { metricExporter } from '../src/telemetry';

/**
 * Collects on demand, which is what a test needs and a period is not —
 * and with the temporality the exporter uses, because that is what
 * decides whether an ended series disappears.
 */
class OnDemand extends MetricReader {
  constructor() {
    super({ aggregationTemporalitySelector: type => metricExporter('http://unused').selectAggregationTemporality(type) });
  }

  protected async onForceFlush(): Promise<void> {}
  protected async onShutdown(): Promise<void> {}

  async metrics(): Promise<MetricData[]> {
    const { resourceMetrics } = await this.collect();
    return resourceMetrics.scopeMetrics.flatMap(scope => scope.metrics);
  }
}

function binding(contract: string, referenceId: string | undefined, state: BindingDTO['state']): BindingDTO {
  return { contract, filter: undefined, referenceId, endpoint: undefined, state };
}

/** A runtime whose answer the test moves between collections. */
class FakeRuntime implements ClientRuntime {
  asked = 0;
  bindings: BindingDTO[] = [
    binding('Payment', 'ref-1', 'LIVE'),
    binding('Payment', 'ref-1', 'LIVE'),
    binding('Refund', undefined, 'REBIND'),
  ];

  snapshot(): ClientRuntimeDTO {
    this.asked++;
    return {
      consumerId: 'shop',
      changeCount: 4,
      takenAt: Date.now(),
      supportedFlavors: ['REST'],
      published: [
        { implementationId: 'a', version: '1', contracts: ['A'], referenceId: 'r', live: true, failure: undefined },
        { implementationId: 'b', version: '1', contracts: ['B'], referenceId: undefined, live: false, failure: 'ERROR: no' },
      ],
      bindings: this.bindings,
      eventStreamConnected: true,
      eventTransport: 'rest',
    };
  }
}

function points(metrics: MetricData[], name: string) {
  const metric = metrics.find(m => m.descriptor.name === name) as GaugeMetricData | undefined;
  return metric?.dataPoints ?? [];
}

function value(metrics: MetricData[], name: string): number | undefined {
  return points(metrics, name)[0]?.value;
}

describe('watchRuntime (#167)', () => {
  let reader: OnDemand;
  let provider: MeterProvider;

  beforeEach(() => {
    reader = new OnDemand();
    provider = new MeterProvider({ readers: [reader] });
  });

  afterEach(async () => {
    await provider.shutdown();
  });

  it('reports the counts under the names the Java runtime uses', async () => {
    watchRuntime(provider.getMeter('test'), new FakeRuntime());

    const metrics = await reader.metrics();

    expect(value(metrics, 'fennec.services.client.published')).toBe(2);
    expect(value(metrics, 'fennec.services.client.published.live')).toBe(1);
    expect(value(metrics, 'fennec.services.client.bindings')).toBe(3);
    expect(value(metrics, 'fennec.services.client.bindings.rebinding')).toBe(1);
    expect(value(metrics, 'fennec.services.client.stream.connected')).toBe(1);
    expect(value(metrics, 'fennec.services.client.changes')).toBe(4);
    expect(points(metrics, 'fennec.services.client.bindings')[0].attributes).toEqual({ 'fennec.node': 'shop' });
  });

  it('reports one series per binding, counting two locators on one registration once with value 2', async () => {
    watchRuntime(provider.getMeter('test'), new FakeRuntime());

    const series = points(await reader.metrics(), 'fennec.services.client.binding');

    expect(series).toHaveLength(2);
    expect(series.find(p => p.attributes['rpc.service'] === 'Payment')).toMatchObject({
      value: 2,
      attributes: { 'fennec.node': 'shop', 'rpc.service': 'Payment', 'fennec.reference': 'ref-1', 'fennec.state': 'LIVE' },
    });
    const rebinding = series.find(p => p.attributes['rpc.service'] === 'Refund');
    expect(rebinding?.attributes).toEqual({ 'fennec.node': 'shop', 'rpc.service': 'Refund', 'fennec.state': 'REBIND' });
  });

  it('reads the runtime at collection time, so an ended binding is not reported afterwards', async () => {
    const runtime = new FakeRuntime();
    watchRuntime(provider.getMeter('test'), runtime);
    expect(points(await reader.metrics(), 'fennec.services.client.binding')).toHaveLength(2);
    const asked = runtime.asked;

    runtime.bindings = [binding('Payment', 'ref-1', 'LIVE')];
    const series = points(await reader.metrics(), 'fennec.services.client.binding');

    expect(runtime.asked).toBeGreaterThan(asked);
    expect(series.map(p => p.attributes['rpc.service'])).toEqual(['Payment']);
  });

  it('the exporter reports gauges as delta, so what was not observed is not reported', () => {
    // Under cumulative temporality the JavaScript SDK keeps reporting a
    // series nobody observes any more, at its last value; Java drops it.
    // A gauge carries no temporality on the wire, so this changes only
    // what is kept between collections.
    expect(metricExporter('http://unused').selectAggregationTemporality(InstrumentType.OBSERVABLE_GAUGE))
      .toBe(AggregationTemporality.DELTA);
  });

  it('with relationships off reports only the counts', async () => {
    watchRuntime(provider.getMeter('test'), new FakeRuntime(), { relationships: false });

    const metrics = await reader.metrics();

    expect(value(metrics, 'fennec.services.client.bindings')).toBe(3);
    expect(points(metrics, 'fennec.services.client.binding')).toHaveLength(0);
  });

  it('a closed watch stops asking the runtime', async () => {
    const runtime = new FakeRuntime();
    const watch = watchRuntime(provider.getMeter('test'), runtime);
    await reader.metrics();

    watch.close();
    const asked = runtime.asked;
    await reader.metrics();

    expect(runtime.asked).toBe(asked);
  });
});
