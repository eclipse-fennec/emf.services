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

import java.lang.annotation.Annotation;

/**
 * The configuration a component would get from Configuration Admin,
 * built by hand because a test has no Configuration Admin.
 *
 * <p>In one place so that adding an attribute stays a one-line change
 * rather than a compile error in every test that configures anything.
 */
final class Configs {

	private Configs() {
	}

	/** The defaults, which is what most tests want. */
	static TelemetryConfig defaults() {
		return of("org.eclipse.fennec.services", true, false);
	}

	static TelemetryConfig of(String scope, boolean logs, boolean useRegisteredPropagators) {
		return new TelemetryConfig() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return TelemetryConfig.class;
			}

			@Override
			public String scope() {
				return scope;
			}

			@Override
			public boolean logs() {
				return logs;
			}

			@Override
			public boolean useRegisteredPropagators() {
				return useRegisteredPropagators;
			}
		};
	}
}
