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

import type { Attributes, Meter, ObservableGauge, ObservableResult } from '@opentelemetry/api';
import type { ClientRuntime, ClientRuntimeDTO } from '@ddsr/client';

/** What a watcher can be told about how much to report. */
export interface RuntimeGaugeOptions {
  /**
   * One series per binding beside the counts (#166). On by default, as
   * in Java; off keeps only the counts.
   */
  relationships?: boolean;
}

/** A runtime being watched. Closing it stops reporting it. */
export interface RuntimeWatch {
  close(): void;
}

/**
 * Turns a client runtime into gauges (#167) — the twin of the Java
 * `RuntimeTelemetry`, under the same names and attributes, so that a
 * TypeScript consumer and a Java one look alike in one query.
 *
 * Observed, not pushed: every gauge asks the runtime for a snapshot
 * when the exporter collects. A number recorded when it changed stops
 * being true between changes, and a binding that ends is simply not
 * reported at the next collection.
 */
export function watchRuntime(meter: Meter, runtime: ClientRuntime, options: RuntimeGaugeOptions = {}): RuntimeWatch {
  const gauges: Array<{ gauge: ObservableGauge; callback: (result: ObservableResult) => void }> = [];

  const count = (name: string, unit: string, description: string, reading: (s: ClientRuntimeDTO) => number) => {
    const gauge = meter.createObservableGauge(name, { unit, description });
    const callback = (result: ObservableResult) => {
      const snapshot = runtime.snapshot();
      result.observe(reading(snapshot), node(snapshot));
    };
    gauge.addCallback(callback);
    gauges.push({ gauge, callback });
  };

  count('fennec.services.client.published', '{implementation}',
    'Implementations this runtime published.', s => s.published.length);
  count('fennec.services.client.published.live', '{implementation}',
    'Of those, the ones the broker is still holding.', s => s.published.filter(p => p.live).length);
  count('fennec.services.client.bindings', '{binding}',
    'Contracts this runtime is bound to.', s => s.bindings.length);
  count('fennec.services.client.bindings.rebinding', '{binding}',
    'Of those, the ones whose provider went away.', s => s.bindings.filter(b => b.state !== 'LIVE').length);
  count('fennec.services.client.stream.connected', '{stream}',
    '1 while this runtime is reading the event stream, 0 otherwise.', s => (s.eventStreamConnected ? 1 : 0));
  count('fennec.services.client.changes', '{change}',
    "How often this runtime's state has changed since it started.", s => s.changeCount);

  if (options.relationships ?? true) {
    const gauge = meter.createObservableGauge('fennec.services.client.binding', {
      unit: '{binding}',
      description: 'One per binding: which contract this runtime is bound to, to which registration, in which state.',
    });
    const callback = (result: ObservableResult) => {
      for (const [attributes, value] of bindingSeries(runtime.snapshot())) {
        result.observe(value, attributes);
      }
    };
    gauge.addCallback(callback);
    gauges.push({ gauge, callback });
  }

  return {
    close() {
      for (const { gauge, callback } of gauges.splice(0)) {
        gauge.removeCallback(callback);
      }
    },
  };
}

/** The node attribute, with the fallback Java uses for a runtime that names itself nothing. */
function node(snapshot: ClientRuntimeDTO): Attributes {
  return { 'fennec.node': snapshot.consumerId ?? 'client' };
}

/**
 * One point per binding, counted rather than set to one: two locators
 * for the same contract with different filters may be bound to the same
 * registration, and a gauge carries each set of attributes once.
 */
export function bindingSeries(snapshot: ClientRuntimeDTO): Array<[Attributes, number]> {
  const points = new Map<string, [Attributes, number]>();
  for (const binding of snapshot.bindings) {
    // Absent rather than "undefined": a binding without a registration
    // has no fennec.reference at all, the way Java reports it.
    const attributes: Attributes = { ...node(snapshot), 'rpc.service': binding.contract, 'fennec.state': binding.state };
    if (binding.referenceId) attributes['fennec.reference'] = binding.referenceId;
    const key = JSON.stringify(Object.entries(attributes).sort(([a], [b]) => a.localeCompare(b)));
    const point = points.get(key);
    if (point) point[1]++;
    else points.set(key, [attributes, 1]);
  }
  return [...points.values()];
}
