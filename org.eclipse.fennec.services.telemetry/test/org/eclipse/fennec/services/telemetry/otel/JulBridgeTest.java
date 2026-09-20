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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

/**
 * The project's logs, where its traces are (#126).
 *
 * <p>This project logs with JUL by convention, and the OSGi
 * OpenTelemetry integration bridges the OSGi LogService — so without a
 * bridge of our own, nothing this code writes ever reaches a backend.
 *
 * <p>The assertion worth having is the last one: a line written inside
 * a call carries that call's trace id. Correlation is the whole reason
 * to send logs through the same pipeline rather than reading them from
 * a file.
 */
class JulBridgeTest {

	private static final class Collected implements LogRecordExporter {

		private final List<LogRecordData> lines = new ArrayList<>();

		@Override
		public CompletableResultCode export(Collection<LogRecordData> batch) {
			lines.addAll(batch);
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

	private SdkLoggerProvider loggers;

	private JulBridge bridge;

	private Logger julLogger;

	@BeforeEach
	void setUp() {
		collected = new Collected();
		loggers = SdkLoggerProvider.builder()
				.addLogRecordProcessor(SimpleLogRecordProcessor.create(collected))
				.build();
		bridge = new JulBridge();
		bridge.loggers = loggers;
		bridge.activate(Configs.defaults());
		julLogger = Logger.getLogger("org.eclipse.fennec.services.test");
		julLogger.setLevel(Level.ALL);
	}

	@AfterEach
	void tearDown() {
		bridge.deactivate();
		loggers.close();
	}

	@Test
	@DisplayName("a JUL line arrives with its level, its logger and its message")
	void aLineArrives() {
		julLogger.warning("[DDSR] a provider stopped answering");

		assertThat(collected.lines).hasSize(1);
		LogRecordData line = collected.lines.get(0);
		assertThat(line.getBodyValue().asString()).isEqualTo("[DDSR] a provider stopped answering");
		assertThat(line.getSeverity()).isEqualTo(Severity.WARN);
		assertThat(line.getSeverityText()).isEqualTo("WARNING");
		assertThat(line.getAttributes().get(AttributeKey.stringKey("logger.name")))
				.isEqualTo("org.eclipse.fennec.services.test");
	}

	@Test
	@DisplayName("an exception travels as the attributes a backend groups errors by")
	void anExceptionTravels() {
		julLogger.log(Level.SEVERE, "publish failed", new IllegalStateException("broker refused"));

		LogRecordData line = collected.lines.get(0);
		assertThat(line.getSeverity()).isEqualTo(Severity.ERROR);
		assertThat(line.getAttributes().get(AttributeKey.stringKey("exception.type")))
				.isEqualTo("java.lang.IllegalStateException");
		assertThat(line.getAttributes().get(AttributeKey.stringKey("exception.message")))
				.isEqualTo("broker refused");
		assertThat(line.getAttributes().get(AttributeKey.stringKey("exception.stacktrace")))
				.contains("IllegalStateException");
	}

	@Test
	@DisplayName("JUL's nine levels land on the OpenTelemetry scale where they mean the same thing")
	void levelsAreMapped() {
		assertThat(JulBridge.severityOf(Level.SEVERE)).isEqualTo(Severity.ERROR);
		assertThat(JulBridge.severityOf(Level.WARNING)).isEqualTo(Severity.WARN);
		assertThat(JulBridge.severityOf(Level.INFO)).isEqualTo(Severity.INFO);
		assertThat(JulBridge.severityOf(Level.CONFIG)).isEqualTo(Severity.DEBUG);
		assertThat(JulBridge.severityOf(Level.FINE))
			.as("FINE is this project's diagnosis level, which is what DEBUG means elsewhere")
			.isEqualTo(Severity.DEBUG);
		assertThat(JulBridge.severityOf(Level.FINEST)).isEqualTo(Severity.TRACE);
	}

	@Test
	@DisplayName("a line written inside a call belongs to that call's trace")
	void linesAreCorrelatedWithTheSpan() {
		SdkTracerProvider tracers = SdkTracerProvider.builder().build();
		OtelCallTracer tracer = new OtelCallTracer();
		tracer.tracers = tracers;
		tracer.configure(Configs.defaults());

		try (CallSpan span = tracer.calling("Payment/charge", TraceCarrier.over(new HashMap<>()))) {
			julLogger.info("[DDSR] charging");
		}
		tracers.close();

		// Not the first line: configuring the tracer logs one of its own,
		// which the bridge picked up too — as it should.
		LogRecordData line = collected.lines.stream()
				.filter(record -> "[DDSR] charging".equals(record.getBodyValue().asString()))
				.findFirst()
				.orElseThrow();
		assertThat(line.getSpanContext().isValid())
			.as("this is the whole reason to route logs through the same pipeline")
			.isTrue();
		assertThat(line.getSpanContext().getTraceId()).isNotEqualTo("00000000000000000000000000000000");
	}

	@Test
	@DisplayName("switched off, the bridge puts no handler on the root logger")
	void switchedOff() {
		bridge.deactivate();
		JulBridge off = new JulBridge();
		off.loggers = loggers;
		off.activate(Configs.of("org.eclipse.fennec.services", false, false));

		julLogger.info("[DDSR] nobody is listening");

		assertThat(collected.lines).isEmpty();
		off.deactivate();
	}
}
