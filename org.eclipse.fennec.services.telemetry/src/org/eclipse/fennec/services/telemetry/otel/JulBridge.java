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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;

/**
 * This project's logs, in the same place as its traces (#126).
 *
 * <p>The OSGi OpenTelemetry integration bridges the OSGi LogService.
 * Everything here logs with JUL instead — a deliberate choice recorded
 * in the conventions, and the reason a bridge has to exist at all.
 *
 * <p>What it buys is correlation. A log emitted inside a call carries
 * the current span's trace id, so a failed invocation and the line the
 * provider wrote about it are one thing in the backend rather than two
 * that happened around the same time.
 *
 * <p>The handler stays on the root logger for as long as this component
 * lives and is taken off again on the way out — a handler left behind
 * would hold a logger provider whose SDK has been shut down.
 */
@Component(configurationPid = "org.eclipse.fennec.services.telemetry", immediate = true)
public class JulBridge {

	private static final AttributeKey<String> LOGGER = AttributeKey.stringKey("logger.name");

	private static final AttributeKey<String> THREAD = AttributeKey.stringKey("thread.name");

	private static final AttributeKey<String> EXCEPTION_TYPE = AttributeKey.stringKey("exception.type");

	private static final AttributeKey<String> EXCEPTION_MESSAGE = AttributeKey.stringKey("exception.message");

	private static final AttributeKey<String> EXCEPTION_STACKTRACE =
			AttributeKey.stringKey("exception.stacktrace");

	@Reference
	LoggerProvider loggers;

	private Handler handler;

	@Activate
	void activate(TelemetryConfig config) {
		if (!config.logs()) {
			return;
		}
		this.handler = new Forwarding(loggers, config.scope());
		Logger.getLogger("").addHandler(handler);
	}

	@Deactivate
	void deactivate() {
		if (handler != null) {
			Logger.getLogger("").removeHandler(handler);
			handler = null;
		}
	}

	/**
	 * A JUL handler that emits what it is given and never logs itself.
	 *
	 * <p>A handler that logs is a handler that calls itself. Everything
	 * here that can fail is swallowed for that reason, and because a
	 * telemetry pipeline must not be able to break the thing it watches.
	 */
	private static final class Forwarding extends Handler {

		private final io.opentelemetry.api.logs.Logger logger;

		Forwarding(LoggerProvider loggers, String scope) {
			this.logger = loggers.get(scope);
		}

		@Override
		public void publish(LogRecord record) {
			if (record == null || !isLoggable(record)) {
				return;
			}
			try {
				LogRecordBuilder line = logger.logRecordBuilder()
						.setTimestamp(record.getInstant())
						// The current span, which is what makes a log
						// line and the call it happened in one thing.
						.setContext(Context.current())
						.setSeverity(severityOf(record.getLevel()))
						.setSeverityText(record.getLevel().getName())
						.setBody(String.valueOf(record.getMessage()))
						.setAttribute(LOGGER, String.valueOf(record.getLoggerName()))
						.setAttribute(THREAD, Thread.currentThread().getName());
				Throwable thrown = record.getThrown();
				if (thrown != null) {
					line = line.setAttribute(EXCEPTION_TYPE, thrown.getClass().getName())
							.setAttribute(EXCEPTION_MESSAGE, String.valueOf(thrown.getMessage()))
							.setAttribute(EXCEPTION_STACKTRACE, stackTraceOf(thrown));
				}
				line.emit();
			} catch (RuntimeException | LinkageError telemetryFailed) {
				// Nothing to report it to: reporting is what just failed.
			}
		}

		@Override
		public void flush() {
			// The SDK's processor decides when to export; there is
			// nothing buffered here to push.
		}

		@Override
		public void close() {
			// Nothing this handler owns needs closing. The SDK is the
			// integration's, and shutting it down here would take it
			// away from every other watcher.
		}
	}

	/**
	 * JUL levels are a scale of nine, OpenTelemetry's is a scale of
	 * twenty-four. This maps the six that carry meaning here and leaves
	 * the rest to fall where they belong.
	 */
	static Severity severityOf(Level level) {
		if (level == null) {
			return Severity.UNDEFINED_SEVERITY_NUMBER;
		}
		int value = level.intValue();
		if (value >= Level.SEVERE.intValue()) {
			return Severity.ERROR;
		}
		if (value >= Level.WARNING.intValue()) {
			return Severity.WARN;
		}
		if (value >= Level.INFO.intValue()) {
			return Severity.INFO;
		}
		if (value >= Level.FINE.intValue()) {
			// CONFIG and FINE both land here: this project uses FINE for
			// diagnosis, which is what DEBUG means everywhere else.
			return Severity.DEBUG;
		}
		return Severity.TRACE;
	}

	private static String stackTraceOf(Throwable thrown) {
		StringWriter text = new StringWriter();
		thrown.printStackTrace(new PrintWriter(text));
		return text.toString();
	}
}
