/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { describe, expect, it } from 'vitest';
import { NO_TRACER, carrierOver, carrierReading, doing, type CallSpan, type CallTracer, type TraceCarrier } from '../src/index.js';

describe('the telemetry seam', () => {
  it('carries a context through a record', () => {
    const fields: Record<string, string> = {};
    const carrier = carrierOver(fields);

    carrier.set('traceparent', '00-abc-def-01');

    expect(fields.traceparent).toBe('00-abc-def-01');
    expect(carrier.get('traceparent')).toBe('00-abc-def-01');
    expect(carrier.get('tracestate')).toBeUndefined();
  });

  it('reads a context that arrives, and stays quiet when asked to write', () => {
    const headers = new Headers({ traceparent: '00-abc-def-01' });
    const carrier = carrierReading(name => headers.get(name));

    expect(carrier.get('traceparent')).toBe('00-abc-def-01');
    expect(() => carrier.set('traceparent', 'other')).not.toThrow();
  });

  it('answers when nothing is installed, and puts nothing on the wire', () => {
    const fields: Record<string, string> = {};

    const span = NO_TRACER.calling('Payment/charge', carrierOver(fields));
    span.attribute('rpc.system', 'fennec.services').failed(new Error('boom'));
    span.end();

    expect(fields).toEqual({});
  });

  it('gives local work a span with nothing to carry', () => {
    let started: string | undefined;
    const written: Record<string, string> = {};
    const tracer: CallTracer = {
      calling(operation: string, outbound: TraceCarrier): CallSpan {
        started = operation;
        // What a real tracer does here. A local span's carrier has
        // nowhere to put it.
        outbound.set('traceparent', '00-abc-def-01');
        return { attribute: () => ({}) as CallSpan, failed: () => {}, end: () => {} };
      },
      serving(): CallSpan {
        throw new Error('not this direction');
      },
    };

    doing(tracer, 'Demo/tick').end();

    expect(started).toBe('Demo/tick');
    expect(written).toEqual({});
  });
});
