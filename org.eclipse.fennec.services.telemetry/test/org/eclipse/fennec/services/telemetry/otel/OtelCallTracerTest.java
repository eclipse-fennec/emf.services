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

package org.eclipse.fennec.services.telemetry.otel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * One trace across two processes (#126).
 *
 * <p>This is the test the whole exercise is for. A call that is traced
 * on the caller's side and traced again on the provider's is not two
 * traces if — and only if — the context travelled. So the assertions
 * that matter are about trace ids, not about counts: the server span
 * must carry the client's trace id and name the client span as its
 * parent, and it must do that through nothing but the carrier.
 */
class OtelCallTracerTest {

	/** Keeps every finished span, in order. */
	private static final class Collected implements SpanExporter {

		private final List<SpanData> spans = new ArrayList<>();

		@Override
		public CompletableResultCode export(Collection<SpanData> batch) {
			spans.addAll(batch);
			return CompletableResultCode.ofSuccess();
		}

		@Override
		public CompletableResultCode flush() {
			return CompletableResultCode.ofSuccess();
		}

		@Override
		public CompletableResultCode shutdown() {
			return CompletableResultCode.ofSuccess();
		}
	}

	private Collected collected;

	private SdkTracerProvider provider;

	private OtelCallTracer tracer;

	@BeforeEach
	void setUp() {
		collected = new Collected();
		provider = SdkTracerProvider.builder()
				.addSpanProcessor(SimpleSpanProcessor.create(collected))
				.build();
		tracer = new OtelCallTracer();
		tracer.tracers = provider;
		tracer.configure(Configs.defaults());
	}

	@AfterEach
	void tearDown() {
		provider.close();
	}

	@Test
	@DisplayName("what the caller writes, the provider reads — and it is one trace")
	void oneTraceAcrossTheCarrier() {
		Map<String, String> wire = new HashMap<>();

		try (CallSpan calling = tracer.calling("Payment/charge", TraceCarrier.over(wire))) {
			calling.attribute("rpc.system", "fennec.services");
		}
		assertThat(wire)
			.as("the context travels in the W3C field, which is what an instrumented provider reads")
			.containsKey("traceparent");

		// The other process: nothing but the carrier crosses.
		try (CallSpan serving = tracer.serving("Payment/charge", TraceCarrier.over(wire))) {
			// the provider does its work
		}

		assertThat(collected.spans).hasSize(2);
		SpanData client = collected.spans.get(0);
		SpanData server = collected.spans.get(1);

		assertThat(client.getKind()).isEqualTo(SpanKind.CLIENT);
		assertThat(server.getKind()).isEqualTo(SpanKind.SERVER);
		assertThat(server.getTraceId())
			.as("one call is one trace, and this is the whole point of #126")
			.isEqualTo(client.getTraceId());
		assertThat(server.getParentSpanId())
			.as("and the provider's span hangs under the caller's, not beside it")
			.isEqualTo(client.getSpanId());
		assertThat(client.getAttributes().get(AttributeKey.stringKey("rpc.system")))
			.isEqualTo("fennec.services");
	}

	@Test
	@DisplayName("a caller nobody instrumented starts the trace at the provider")
	void uninstrumentedCallerStillGetsATrace() {
		try (CallSpan serving = tracer.serving("Payment/charge", TraceCarrier.over(new HashMap<>()))) {
			// nothing arrived in the carrier
		}

		assertThat(collected.spans).hasSize(1);
		assertThat(collected.spans.get(0).getParentSpanId())
			.as("no parent to continue, so this is the root — not a dropped call")
			.isEqualTo("0000000000000000");
		assertThat(collected.spans.get(0).getKind()).isEqualTo(SpanKind.SERVER);
	}

	@Test
	@DisplayName("a call that failed says so, with the exception on the span")
	void failureIsRecorded() {
		try (CallSpan calling = tracer.calling("Payment/charge", TraceCarrier.over(new HashMap<>()))) {
			calling.failed(new IllegalStateException("provider refused"));
		}

		SpanData span = collected.spans.get(0);
		assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
		assertThat(span.getStatus().getDescription()).isEqualTo("provider refused");
		assertThat(span.getEvents())
			.as("the exception itself, so a reader does not have to guess from the message")
			.isNotEmpty();
	}

	@Test
	@DisplayName("a null attribute is dropped rather than recorded as the word null")
	void nullAttributesAreDropped() {
		try (CallSpan calling = tracer.calling("Payment/charge", TraceCarrier.over(new HashMap<>()))) {
			calling.attribute("server.address", null).attribute("rpc.method", "charge");
		}

		assertThat(collected.spans.get(0).getAttributes().get(AttributeKey.stringKey("rpc.method")))
			.isEqualTo("charge");
		assertThat(collected.spans.get(0).getAttributes().get(AttributeKey.stringKey("server.address")))
			.as("a null value is not an attribute with the text 'null' in it")
			.isNull();
		assertThat(collected.spans.get(0).getAttributes().size()).isEqualTo(1);
	}

}
