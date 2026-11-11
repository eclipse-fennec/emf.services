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

	@AttributeDefinition(name = "Bridge JUL logs",
			description = "Forward this framework's java.util.logging records to OpenTelemetry, "
					+ "correlated with the span they happened in.")
	boolean logs() default true;

	/**
	 * Which {@code ContextPropagators} service the tracer binds, as the
	 * target of its reference.
	 *
	 * <p>The W3C pair of this bundle by default, and that is a statement
	 * about the current state of the OpenTelemetry integration rather
	 * than a preference: it builds its SDK without
	 * {@code setPropagators}, so the service it publishes is a no-op.
	 * Propagation that is configured and silently does nothing is worse
	 * than propagation that is obviously absent. Name another service
	 * here once there is a real one — {@code (!(fennec.propagators=*))}
	 * is every service this bundle did not register.
	 */
	@AttributeDefinition(name = "Propagators",
			description = "LDAP filter selecting the ContextPropagators service the tracer uses. "
					+ "The default is the W3C Trace Context and Baggage pair this bundle registers; "
					+ "the OSGi OpenTelemetry integration currently publishes a no-op.")
	String propagators_target() default W3cPropagators.TARGET;

	/**
	 * Whether to report one series per relationship — a binding on the
	 * client, a registration and a lease on the broker — beside the
	 * counts (#166).
	 *
	 * <p>On by default: what a node holds is small, and it is what a view
	 * of who uses what is drawn from. It grows with the deployment,
	 * though, and a backend that is billed per series may want the
	 * counts alone.
	 */
	@AttributeDefinition(name = "Report relationships",
			description = "Report one series per binding, registration and lease beside the counts. "
					+ "Turn off to keep only the counts.")
	boolean relationships() default true;
}
