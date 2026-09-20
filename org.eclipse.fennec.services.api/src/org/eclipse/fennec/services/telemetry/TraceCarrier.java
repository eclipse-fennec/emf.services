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

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

/**
 * Where trace context travels on a particular wire (#126).
 *
 * <p>Two methods, because that is all context propagation is: a
 * {@code traceparent} written on the way out and read on the way in.
 * What it is written <em>into</em> differs per transport — HTTP headers,
 * CloudEvents extension attributes, an MQTT 5 user property — and the
 * call sites are the only places that know which.
 *
 * <p>This is deliberately not an OpenTelemetry type. A call site that
 * wants to be traceable should not have to import a telemetry SDK, and
 * the bundle that has one should be removable without touching a single
 * invoker.
 */
public interface TraceCarrier {

	/** Writes a context field. Called on the way out. */
	void set(String name, String value);

	/**
	 * Reads a context field, or null when the caller sent none.
	 * Called on the way in.
	 */
	String get(String name);

	/** A carrier over a map — the usual case on both sides. */
	static TraceCarrier over(Map<String, String> fields) {
		return new TraceCarrier() {

			@Override
			public void set(String name, String value) {
				fields.put(name, value);
			}

			@Override
			public String get(String name) {
				return fields.get(name);
			}
		};
	}

	/**
	 * A carrier that can only be written, for an outbound call whose
	 * fields go somewhere that is not a map.
	 */
	static TraceCarrier writing(BiConsumer<String, String> sink) {
		return new TraceCarrier() {

			@Override
			public void set(String name, String value) {
				sink.accept(name, value);
			}

			@Override
			public String get(String name) {
				return null;
			}
		};
	}

	/**
	 * A carrier that can only be read, for an inbound call that has a
	 * lookup rather than a map — a JAX-RS {@code HttpHeaders}, say.
	 */
	static TraceCarrier reading(UnaryOperator<String> lookup) {
		return new TraceCarrier() {

			@Override
			public void set(String name, String value) {
				// Nothing to write into. An inbound carrier is asked to
				// write only by a tracer that got its direction wrong,
				// and dropping it is better than failing the call.
			}

			@Override
			public String get(String name) {
				return lookup.apply(name);
			}
		};
	}
}
