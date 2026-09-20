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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The seam itself (#126).
 *
 * <p>What is worth pinning here is not that it carries strings, but
 * that a call site can use it without checking anything: no null
 * tracer, no null span, nothing that throws. Everything else in this
 * package is one bundle's opinion about telemetry; this is the promise
 * every invoker relies on.
 */
class TelemetrySeamTest {

	@Test
	@DisplayName("a carrier over a map is read and written through the map")
	void mapCarrier() {
		Map<String, String> fields = new HashMap<>();
		TraceCarrier carrier = TraceCarrier.over(fields);

		carrier.set("traceparent", "00-abc-def-01");

		assertThat(fields).containsEntry("traceparent", "00-abc-def-01");
		assertThat(carrier.get("traceparent")).isEqualTo("00-abc-def-01");
		assertThat(carrier.get("tracestate")).isNull();
	}

	@Test
	@DisplayName("a one-way carrier answers the direction it has and stays quiet about the other")
	void oneWayCarriers() {
		Map<String, String> written = new HashMap<>();
		TraceCarrier out = TraceCarrier.writing(written::put);
		TraceCarrier in = TraceCarrier.reading(name -> "traceparent".equals(name) ? "00-abc-def-01" : null);

		out.set("traceparent", "00-abc-def-01");
		assertThat(written).containsEntry("traceparent", "00-abc-def-01");
		assertThat(out.get("traceparent"))
			.as("an outbound carrier has nothing to read back")
			.isNull();

		assertThat(in.get("traceparent")).isEqualTo("00-abc-def-01");
		assertThatCode(() -> in.set("traceparent", "other"))
			.as("writing into an inbound carrier is a tracer's mistake, not a failed call")
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("nothing installed still answers, and the answer does nothing")
	void noTracer() {
		Map<String, String> fields = new HashMap<>();

		try (CallSpan span = CallTracer.NONE.calling("Payment/charge", TraceCarrier.over(fields))) {
			span.attribute("rpc.system", "fennec.services").failed(new IllegalStateException("boom"));
		}

		assertThat(fields)
			.as("a call site that is not being watched writes nothing on the wire")
			.isEmpty();
	}

	@Test
	@DisplayName("a deferred tracer follows what is bound, in both directions")
	void deferredFollowsTheBinding() {
		AtomicReference<CallTracer> bound = new AtomicReference<>();
		AtomicReference<String> served = new AtomicReference<>();
		CallTracer deferred = CallTracer.deferred(bound::get);

		assertThat(deferred.serving("Payment/charge", TraceCarrier.reading(name -> null)))
			.as("nothing bound is not a reason to fail a call")
			.isSameAs(CallSpan.NONE);

		bound.set(new CallTracer() {

			@Override
			public CallSpan calling(String operation, TraceCarrier outbound) {
				return CallSpan.NONE;
			}

			@Override
			public CallSpan serving(String operation, TraceCarrier inbound) {
				served.set(operation);
				return CallSpan.NONE;
			}
		});
		deferred.serving("Payment/charge", TraceCarrier.reading(name -> null));
		assertThat(served)
			.as("a tracer that arrives after the call site was built is still used")
			.hasValue("Payment/charge");

		served.set(null);
		bound.set(null);
		deferred.serving("Payment/refund", TraceCarrier.reading(name -> null));
		assertThat(served)
			.as("and one that leaves is not called again")
			.hasValue(null);
	}
}
