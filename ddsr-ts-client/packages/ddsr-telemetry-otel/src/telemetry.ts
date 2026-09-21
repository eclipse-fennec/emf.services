/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import {
  SpanKind,
  SpanStatusCode,
  context as activeContext,
  propagation,
  trace,
  type Context,
  type Span,
  type Tracer,
} from '@opentelemetry/api';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { resourceFromAttributes } from '@opentelemetry/resources';
import { NodeSDK } from '@opentelemetry/sdk-node';
import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';
import { NO_TRACER, type CallSpan, type CallTracer, type TraceCarrier } from '@ddsr/telemetry';

/**
 * OpenTelemetry behind the seam (#146) — the TypeScript twin of the
 * `…telemetry` bundle on the Java side.
 *
 * What makes a cross-language trace work is that neither side has to
 * know about the other: both write a W3C `traceparent`, both read one,
 * and what sits behind the seam is each runtime's own business.
 */
export interface TelemetryOptions {
  /** How this process names itself. Shows up as `service.name`. */
  serviceName: string;
  /** The group it belongs to; `fennec.services` unless a caller says otherwise. */
  serviceNamespace?: string;
  serviceVersion?: string;
  /** The collector's OTLP/HTTP base, default `http://localhost:4318`. */
  endpoint?: string;
  /** The instrumentation scope of every span. */
  scope?: string;
}

export interface Telemetry {
  /** Hand this to the client and the flavor plugins. */
  readonly tracer: CallTracer;

  /**
   * Runs work with a span of its own, and with that span current — so
   * the calls made inside it become its children.
   *
   * <p>This is where JavaScript and Java differ and the difference is
   * the language's: Java makes a span current for a scope with
   * try-with-resources, and here the only way to enter a context is to
   * run inside a callback.
   */
  during<T>(operation: string, run: () => Promise<T>): Promise<T>;

  /** Flushes what is pending and stops the exporter. */
  shutdown(): Promise<void>;
}

/** What a process gets when telemetry is switched off. */
export const NO_TELEMETRY: Telemetry = {
  tracer: NO_TRACER,
  during: (_operation, run) => run(),
  shutdown: async () => {},
};

/**
 * Starts the SDK and answers with the seam the client expects.
 *
 * The exporter is OTLP over HTTP, which is what the demo's collector
 * listens on and what needs no agent beside the process.
 */
export function startTelemetry(options: TelemetryOptions): Telemetry {
  const endpoint = (options.endpoint ?? 'http://localhost:4318').replace(/\/+$/, '');
  const sdk = new NodeSDK({
    resource: resourceFromAttributes({
      [ATTR_SERVICE_NAME]: options.serviceName,
      [ATTR_SERVICE_VERSION]: options.serviceVersion ?? '0.1.0',
      'service.namespace': options.serviceNamespace ?? 'fennec.services',
    }),
    traceExporter: new OTLPTraceExporter({ url: `${endpoint}/v1/traces` }),
  });
  sdk.start();

  const tracer = trace.getTracer(options.scope ?? 'org.eclipse.fennec.services');
  return {
    tracer: new OtelCallTracer(tracer),
    during: (operation, run) => {
      const span = tracer.startSpan(operation, { kind: SpanKind.INTERNAL });
      return activeContext
        .with(trace.setSpan(activeContext.active(), span), run)
        .then(
          value => {
            span.end();
            return value;
          },
          error => {
            record(span, error);
            span.end();
            throw error;
          });
    },
    shutdown: () => sdk.shutdown(),
  };
}

class OtelCallTracer implements CallTracer {
  constructor(private readonly tracer: Tracer) {}

  calling(operation: string, outbound: TraceCarrier): CallSpan {
    const span = this.tracer.startSpan(operation, { kind: SpanKind.CLIENT });
    // Injected with the new span current, because the context that
    // travels has to name THIS call and not the one around it.
    activeContext.with(trace.setSpan(activeContext.active(), span), () => {
      propagation.inject(activeContext.active(), outbound, {
        set: (carrier, key, value) => carrier.set(key, String(value)),
      });
    });
    return wrap(span);
  }

  serving(operation: string, inbound: TraceCarrier): CallSpan {
    const parent: Context = propagation.extract(activeContext.active(), inbound, {
      get: (carrier, key) => carrier.get(key),
      keys: () => ['traceparent', 'tracestate', 'baggage'],
    });
    return wrap(this.tracer.startSpan(operation, { kind: SpanKind.SERVER }, parent));
  }
}

function wrap(span: Span): CallSpan {
  const call: CallSpan = {
    attribute(name, value) {
      if (value !== undefined) {
        span.setAttribute(name, value);
      }
      return call;
    },
    failed(error) {
      if (error instanceof Error) {
        span.recordException(error);
      }
      span.setStatus({ code: SpanStatusCode.ERROR, message: describe(error) });
    },
    end() {
      span.end();
    },
  };
  return call;
}

function record(span: Span, error: unknown): void {
  if (error instanceof Error) {
    span.recordException(error);
  }
  span.setStatus({ code: SpanStatusCode.ERROR, message: describe(error) });
}

function describe(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
