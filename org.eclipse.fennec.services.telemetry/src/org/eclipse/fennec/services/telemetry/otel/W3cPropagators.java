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

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;

/**
 * W3C Trace Context plus Baggage, as the {@code ContextPropagators}
 * service the tracer binds unless its configuration names another.
 *
 * <p>A service rather than a fallback in code, so that which
 * propagators a node uses is one statement — the target of the
 * tracer's reference — and the tracer always has some. Registered with
 * the lowest ranking: whoever asks for {@code ContextPropagators}
 * without a target keeps getting what they got before this bundle was
 * installed.
 */
@Component(property = { W3cPropagators.KIND + "=" + W3cPropagators.KIND_W3C,
		Constants.SERVICE_RANKING + ":Integer=" + Integer.MIN_VALUE })
public class W3cPropagators implements ContextPropagators {

	/** The service property a propagators service says its kind in. */
	public static final String KIND = "fennec.propagators";

	public static final String KIND_W3C = "w3c";

	/** The target that selects this service. */
	public static final String TARGET = "(" + KIND + "=" + KIND_W3C + ")";

	private static final TextMapPropagator W3C = TextMapPropagator
			.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance());

	@Override
	public TextMapPropagator getTextMapPropagator() {
		return W3C;
	}
}
