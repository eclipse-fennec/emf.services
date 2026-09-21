/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/**
 * The seam between a call and whoever is watching it (#146) — the
 * TypeScript twin of the Java `CallTracer`.
 *
 * A remote call has two halves in two processes, and a trace is one
 * thing only if the second half is told about the first. That telling
 * is all this is: `calling` writes the context into a carrier,
 * `serving` reads it back out. The formats are W3C Trace Context and
 * the CloudEvents tracing extension, neither of which is tied to a
 * language — which is why a TypeScript consumer and a Java provider
 * can be one trace.
 *
 * Deliberately not an OpenTelemetry type. `@ddsr/client` must not need
 * a telemetry SDK to be traceable, and a deployment that installs none
 * loses its traces and nothing else.
 */

/** Where trace context travels on a particular wire. */
export interface TraceCarrier {
  /** Writes a context field. Called on the way out. */
  set(name: string, value: string): void;
  /** Reads a context field, or undefined when the caller sent none. */
  get(name: string): string | undefined;
}

/** One call, while it is happening. */
export interface CallSpan {
  /** A detail worth grouping by later. Undefined values are dropped. */
  attribute(name: string, value: string | undefined): CallSpan;
  /** The call did not succeed. */
  failed(error: unknown): void;
  /** Ends the call. Never throws. */
  end(): void;
}

export interface CallTracer {
  /**
   * A call this runtime is about to make. Writes the current context
   * into `outbound`, so the other end continues this trace.
   *
   * @param operation what is being called, as `Contract/operation`
   */
  calling(operation: string, outbound: TraceCarrier): CallSpan;

  /**
   * A call this runtime is about to answer. Reads the caller's context
   * out of `inbound`; a request that carries none starts a trace here,
   * which is the right answer for an uninstrumented caller.
   */
  serving(operation: string, inbound: TraceCarrier): CallSpan;
}

/** What nothing records. Every method is a no-op. */
export const NO_SPAN: CallSpan = {
  attribute() {
    return NO_SPAN;
  },
  failed() {
    // nothing records this
  },
  end() {
    // nothing to end
  },
};

/** A tracer that records nothing, for when no bundle provides one. */
export const NO_TRACER: CallTracer = {
  calling() {
    return NO_SPAN;
  },
  serving() {
    return NO_SPAN;
  },
};

/** A carrier over a record — the usual case on both sides. */
export function carrierOver(fields: Record<string, string>): TraceCarrier {
  return {
    set(name, value) {
      fields[name] = value;
    },
    get(name) {
      return fields[name];
    },
  };
}

/** A carrier that can only be read: HTTP headers on the way in. */
export function carrierReading(lookup: (name: string) => string | null | undefined): TraceCarrier {
  return {
    set() {
      // Nothing to write into. An inbound carrier is asked to write
      // only by a tracer that got its direction wrong, and dropping it
      // is better than failing the call.
    },
    get(name) {
      return lookup(name) ?? undefined;
    },
  };
}

/**
 * Work that stays here, as the parent of the calls it makes.
 *
 * For a caller whose unit of work is several calls — discover, invoke,
 * release — and who wants them in one trace rather than three. Nothing
 * is injected, because nothing travels.
 */
export function doing(tracer: CallTracer, operation: string): CallSpan {
  return tracer.calling(operation, {
    set() {
      // nowhere to write: this span does not leave the process
    },
    get() {
      return undefined;
    },
  });
}
