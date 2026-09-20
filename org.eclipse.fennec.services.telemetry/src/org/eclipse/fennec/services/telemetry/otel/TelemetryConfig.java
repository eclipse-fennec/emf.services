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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * What a deployment can say about its telemetry.
 *
 * <p>Deliberately short. Which exporter runs, where it sends and how
 * often is the OpenTelemetry integration's configuration, not ours —
 * this bundle only decides what to call things and whose propagators to
 * trust.
 */
@ObjectClassDefinition(name = "Fennec Services Telemetry",
		description = "Traces and metrics for the broker and the client SDK (#126).")
public @interface TelemetryConfig {

	/**
	 * The instrumentation scope every span and instrument is created
	 * under. A backend groups by it, so it should name this project
	 * rather than the deployment.
	 */
	@AttributeDefinition(name = "Instrumentation scope",
			description = "Reported as the instrumentation scope name of every span and instrument.")
	String scope() default "org.eclipse.fennec.services";

	/**
	 * Whether to propagate with the {@code ContextPropagators} service
	 * that the OpenTelemetry integration publishes, instead of the W3C
	 * propagators this bundle brings.
	 *
	 * <p>Off by default, and that is a statement about the current state
	 * of the integration rather than a preference: it builds its SDK
	 * without {@code setPropagators}, so the service it publishes is a
	 * no-op. Propagation that is configured and silently does nothing is
	 * worse than propagation that is obviously absent. Turn this on once
	 * the integration supplies real ones.
	 */
	@AttributeDefinition(name = "Bridge JUL logs",
			description = "Forward this framework's java.util.logging records to OpenTelemetry, "
					+ "correlated with the span they happened in.")
	boolean logs() default true;

	@AttributeDefinition(name = "Use the published propagators",
			description = "Use the registered ContextPropagators service instead of the built-in W3C propagators. "
					+ "The OSGi OpenTelemetry integration currently publishes a no-op, so this is off by default.")
	boolean useRegisteredPropagators() default false;
}
