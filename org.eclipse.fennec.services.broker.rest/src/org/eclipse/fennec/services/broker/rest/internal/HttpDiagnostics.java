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

package org.eclipse.fennec.services.broker.rest.internal;

import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Maps {@link Diagnostic} values to HTTP responses. Severity OK and
 * WARNING become {@code 200 OK} (the WARNING is conveyed in the body
 * so the client can inspect it without losing the success semantic);
 * ERROR/CANCEL become 4xx based on the diagnostic code.
 *
 * <p>The Diagnostic itself is set as the entity — the XMI
 * {@code MessageBodyWriter} serializes it on response write.
 */
final class HttpDiagnostics {

	private HttpDiagnostics() {
	}

	static Response toResponse(Diagnostic d) {
		return Response.status(statusFor(d)).entity(d).type(MediaType.APPLICATION_XML).build();
	}

	private static int statusFor(Diagnostic d) {
		DiagnosticSeverity sev = d.getSeverity();
		if (sev == DiagnosticSeverity.OK || sev == DiagnosticSeverity.INFO || sev == DiagnosticSeverity.WARNING) {
			return 200;
		}
		switch (d.getCode()) {
		case DdsrDiagnostics.CODE_CATALOG_ENTRY_NOT_FOUND:
		case DdsrDiagnostics.CODE_IMPL_NOT_PUBLISHED:
			return 404;
		case DdsrDiagnostics.CODE_CATALOG_ENTRY_ALREADY_EXISTS:
		case DdsrDiagnostics.CODE_CATALOG_HAS_LIVE_IMPLS:
			return 409;
		case DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION:
			return 403;
		case DdsrDiagnostics.CODE_IMPL_INTERFACE_NOT_IN_CATALOG:
			return 422;
		case DdsrDiagnostics.CODE_PERSISTENCE_FAILED:
		case DdsrDiagnostics.CODE_NETWORK_PARTITION:
			return 503;
		default:
			return 400;
		}
	}
}
