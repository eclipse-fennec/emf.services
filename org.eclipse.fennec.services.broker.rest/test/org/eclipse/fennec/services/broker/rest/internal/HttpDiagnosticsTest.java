/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.broker.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.fennec.services.broker.rest.internal.RestTestSupport.diagnostic;

import java.util.stream.Stream;

import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** The Diagnostic → HTTP status table (#56); consumers branch on both. */
class HttpDiagnosticsTest {

	static Stream<Arguments> mapping() {
		return Stream.of(
			Arguments.of(DiagnosticSeverity.OK, DdsrDiagnostics.CODE_OK, 200),
			Arguments.of(DiagnosticSeverity.INFO, DdsrDiagnostics.CODE_OK, 200),
			Arguments.of(DiagnosticSeverity.WARNING, DdsrDiagnostics.CODE_INTERFACE_DEPRECATED, 200),
			Arguments.of(DiagnosticSeverity.WARNING, DdsrDiagnostics.CODE_IMPL_REPLACES_NOT_FOUND, 200),
			Arguments.of(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND, 404),
			Arguments.of(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED, 404),
			Arguments.of(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_CATALOG_ENTRY_ALREADY_EXISTS, 409),
			Arguments.of(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_CATALOG_HAS_LIVE_IMPLS, 409),
			Arguments.of(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_IMPL_CONTRACT_CHANGED, 409),
			Arguments.of(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION, 403),
			Arguments.of(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG, 422),
			Arguments.of(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_PERSISTENCE_FAILED, 503),
			Arguments.of(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_NETWORK_PARTITION, 503),
			Arguments.of(DiagnosticSeverity.ERROR, DdsrDiagnostics.CODE_SESSION_INVALID, 400),
			Arguments.of(DiagnosticSeverity.CANCEL, DdsrDiagnostics.CODE_CATALOG_ENTRY_AMBIGUOUS, 400));
	}

	@ParameterizedTest(name = "{0} code {1} -> HTTP {2}")
	@MethodSource("mapping")
	void severityAndCodeDecideTheStatus(DiagnosticSeverity severity, int code, int status) {
		var response = HttpDiagnostics.toResponse(diagnostic(severity, code, "m"));
		assertThat(response.getStatus()).isEqualTo(status);
		assertThat(response.getEntity()).as("the Diagnostic itself is the body").isNotNull();
		assertThat(response.getMediaType().toString()).isEqualTo("application/xml");
	}
}
