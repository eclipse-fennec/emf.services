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

package org.eclipse.fennec.services.telemetry;

import java.util.function.Supplier;

/**
 * The seam between a call and whoever is watching it (#126).
 *
 * <p>A remote call has two halves in two processes, and a trace is one
 * thing only if the second half is told about the first. That telling is
 * all this interface does: {@link #calling} writes the context into the
 * carrier, {@link #serving} reads it back out and continues the trace
 * the caller started.
 *
 * <p><strong>Optional by construction.</strong> A call site binds it
 * dynamically and falls back to {@link #NONE}:
 *
 * <pre>
 * &#64;Reference(policy = DYNAMIC, cardinality = OPTIONAL)
 * volatile CallTracer tracer;
 *
 * private CallTracer tracer() {
 *     CallTracer bound = tracer;
 *     return bound == null ? CallTracer.NONE : bound;
 * }
 * </pre>
 *
 * <p>So nothing in the call path depends on a telemetry bundle being
 * installed, and installing one changes no wire format: the context
 * travels in fields that a reader who does not know them ignores.
 *
 * <p>This is not an OpenTelemetry interface on purpose, even though the
 * implementation is one. The OSGi OpenTelemetry integration is a
 * snapshot, its propagators are currently a no-op, and none of that
 * should be visible from an invoker. What an invoker knows is that a
 * call has two ends.
 */
public interface CallTracer {

	/** A tracer that records nothing, for when no bundle provides one. */
	CallTracer NONE = new CallTracer() {

		@Override
		public CallSpan calling(String operation, TraceCarrier outbound) {
			return CallSpan.NONE;
		}

		@Override
		public CallSpan serving(String operation, TraceCarrier inbound) {
			return CallSpan.NONE;
		}
	};

	/**
	 * A tracer that asks again on every call.
	 *
	 * <p>For a call site that is built once and lives long — a JAX-RS
	 * resource, a topic dispatcher — while the tracer behind it comes
	 * and goes with its bundle. Capturing the tracer at construction
	 * would mean a dispatcher built before the telemetry bundle stays
	 * untraced for as long as it lives, and one built after it keeps
	 * calling a tracer that was unregistered.
	 *
	 * @param current what is bound right now, or null when nothing is
	 */
	static CallTracer deferred(Supplier<CallTracer> current) {
		return new CallTracer() {

			@Override
			public CallSpan calling(String operation, TraceCarrier outbound) {
				return bound().calling(operation, outbound);
			}

			@Override
			public CallSpan serving(String operation, TraceCarrier inbound) {
				return bound().serving(operation, inbound);
			}

			private CallTracer bound() {
				CallTracer tracer = current.get();
				return tracer == null ? NONE : tracer;
			}
		};
	}

	/**
	 * A call this runtime is about to make.
	 *
	 * <p>Writes the current context into {@code outbound}, so the other
	 * end can continue this trace rather than start its own.
	 *
	 * @param operation what is being called, as {@code Contract/operation}
	 * @param outbound  where the context is written
	 */
	CallSpan calling(String operation, TraceCarrier outbound);

	/**
	 * A call this runtime is about to answer.
	 *
	 * <p>Reads the caller's context out of {@code inbound}. A request
	 * that carries none starts a trace here, which is the right answer
	 * for a caller that is not instrumented.
	 *
	 * @param operation what is being served, as {@code Contract/operation}
	 * @param inbound   where the caller's context is read from
	 */
	CallSpan serving(String operation, TraceCarrier inbound);
}
