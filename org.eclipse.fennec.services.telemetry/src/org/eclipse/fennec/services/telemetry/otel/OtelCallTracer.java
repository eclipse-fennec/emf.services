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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.telemetry.CallSpan;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.telemetry.TraceCarrier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

/**
 * The {@link CallTracer} seam, implemented on OpenTelemetry (#126).
 *
 * <p>Two halves of one trace: {@link #calling} starts a {@code CLIENT}
 * span and writes its context into the carrier; {@link #serving} reads
 * a context out of one and starts a {@code SERVER} span under it. What
 * the carrier is — HTTP headers, CloudEvents extensions — is the call
 * site's business and never this class's.
 *
 * <p>The span is made current for the length of the call, so anything
 * else that instruments itself inside it lands in the same trace
 * without being handed anything.
 */
@Component(service = CallTracer.class, configurationPid = "org.eclipse.fennec.services.telemetry")
@Designate(ocd = TelemetryConfig.class)
public class OtelCallTracer implements CallTracer {

	private static final Logger LOG = Logger.getLogger(OtelCallTracer.class.getName());

	/**
	 * The fields a context can arrive in. W3C reads by name and never
	 * asks for this list, but a composite propagator may, and a getter
	 * that answers "no keys" is a getter that silently extracts nothing.
	 */
	private static final List<String> FIELDS = List.of("traceparent", "tracestate", "baggage");

	private static final TextMapSetter<TraceCarrier> WRITE = (carrier, field, value) -> {
		if (carrier != null && value != null) {
			carrier.set(field, value);
		}
	};

	private static final TextMapGetter<TraceCarrier> READ = new TextMapGetter<>() {

		@Override
		public Iterable<String> keys(TraceCarrier carrier) {
			return FIELDS;
		}

		@Override
		public String get(TraceCarrier carrier, String field) {
			return carrier == null ? null : carrier.get(field);
		}
	};

	/**
	 * The propagators the context travels with, chosen by the target of
	 * this reference — {@code propagators.target} in the configuration.
	 *
	 * <p>By default the W3C pair this bundle registers itself
	 * ({@link W3cPropagators}). The OSGi OpenTelemetry integration builds
	 * its SDK without {@code setPropagators}, so the
	 * {@code ContextPropagators} it publishes is a no-op — asking it to
	 * inject writes nothing, and the second half of every call would
	 * start its own trace while everything looked configured. A
	 * deployment that has real ones names them in the target.
	 */
	@Reference(name = "propagators", target = W3cPropagators.TARGET)
	ContextPropagators propagators;

	@Reference
	TracerProvider tracers;

	private volatile Tracer tracer;

	@Activate
	@Modified
	void configure(TelemetryConfig config) {
		this.tracer = tracers.get(config.scope());
		LOG.info("[DDSR] call tracing is on, scope '" + config.scope() + "', propagators "
				+ config.propagators_target());
	}

	private TextMapPropagator propagator() {
		return propagators.getTextMapPropagator();
	}

	@Override
	public CallSpan calling(String operation, TraceCarrier outbound) {
		return start(operation, SpanKind.CLIENT, null, outbound);
	}

	@Override
	public CallSpan serving(String operation, TraceCarrier inbound) {
		Context caller = propagator().extract(Context.current(), inbound, READ);
		return start(operation, SpanKind.SERVER, caller, null);
	}

	/**
	 * @param parent   the caller's context when there is one, else the
	 *                 current one — an uninstrumented caller starts a
	 *                 trace here rather than being dropped
	 * @param outbound written into once the span exists, because the
	 *                 context that travels must name <em>this</em> span
	 */
	private CallSpan start(String operation, SpanKind kind, Context parent, TraceCarrier outbound) {
		Tracer current = tracer;
		if (current == null) {
			// Configured but not activated yet, or being reconfigured.
			// A call must not wait for telemetry.
			return CallSpan.NONE;
		}
		try {
			SpanBuilder builder = current.spanBuilder(operation).setSpanKind(kind);
			if (parent != null) {
				builder.setParent(parent);
			}
			Span span = builder.startSpan();
			Scope scope = span.makeCurrent();
			OtelSpan started = new OtelSpan(span, scope);
			if (outbound != null) {
				try {
					propagator().inject(Context.current(), outbound, WRITE);
				} catch (RuntimeException broken) {
					// The span is current on this thread by now. Returning
					// NONE without closing it would leave it current for
					// whatever this thread does next.
					started.close();
					throw broken;
				}
			}
			return started;
		} catch (RuntimeException broken) {
			// Telemetry must not break a call. Whatever went wrong here,
			// the call continues untraced.
			LOG.log(Level.WARNING, broken, () -> "[DDSR] could not start a span for " + operation);
			return CallSpan.NONE;
		}
	}

	/** One span and the scope it was made current in, closed together. */
	private static final class OtelSpan implements CallSpan {

		private final Span span;

		private final Scope scope;

		OtelSpan(Span span, Scope scope) {
			this.span = span;
			this.scope = scope;
		}

		@Override
		public CallSpan attribute(String name, String value) {
			if (value != null) {
				span.setAttribute(name, value);
			}
			return this;
		}

		@Override
		public void failed(Throwable error) {
			span.recordException(error);
			span.setStatus(StatusCode.ERROR, String.valueOf(error.getMessage()));
		}

		@Override
		public void close() {
			try {
				scope.close();
			} finally {
				span.end();
			}
		}
	}
}
