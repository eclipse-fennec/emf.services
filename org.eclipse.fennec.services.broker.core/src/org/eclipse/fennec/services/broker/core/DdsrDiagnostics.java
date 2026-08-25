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

package org.eclipse.fennec.services.broker.core;

import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;

/**
 * Static factory methods for {@link Diagnostic} values commonly returned
 * by broker operations. Keeps the verbose EMF setter sequence out of
 * the business code.
 */
public final class DdsrDiagnostics {

	/** Source identifier carried on diagnostics issued by the broker core. */
	public static final String SOURCE = "org.eclipse.fennec.services.broker.core";

	// Diagnostic codes used by the broker. Stable numeric values; documented
	// here so REST and event-stream consumers can branch on them.
	public static final int CODE_OK                          = 0;
	public static final int CODE_NETWORK_PARTITION           = 100;
	public static final int CODE_CATALOG_HAS_LIVE_IMPLS      = 200;
	public static final int CODE_CATALOG_ENTRY_NOT_FOUND     = 201;
	public static final int CODE_CATALOG_ENTRY_ALREADY_EXISTS = 202;
	public static final int CODE_IMPL_INTERFACE_NOT_IN_CATALOG = 210;
	public static final int CODE_IMPL_OWNERSHIP_VIOLATION    = 211;
	public static final int CODE_IMPL_NOT_PUBLISHED          = 212;
	public static final int CODE_SESSION_INVALID             = 230;
	public static final int CODE_INTERFACE_DEPRECATED        = 300;
	public static final int CODE_PERSISTENCE_FAILED          = 500;

	private DdsrDiagnostics() {
		// no instances
	}

	public static Diagnostic ok() {
		return diagnostic(DiagnosticSeverity.OK, CODE_OK, null);
	}

	public static Diagnostic ok(String message) {
		return diagnostic(DiagnosticSeverity.OK, CODE_OK, message);
	}

	public static Diagnostic warning(int code, String message) {
		return diagnostic(DiagnosticSeverity.WARNING, code, message);
	}

	public static Diagnostic error(int code, String message) {
		return diagnostic(DiagnosticSeverity.ERROR, code, message);
	}

	public static Diagnostic diagnostic(DiagnosticSeverity severity, int code, String message) {
		Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
		d.setSeverity(severity);
		d.setCode(code);
		d.setSource(SOURCE);
		if (message != null) {
			d.setMessage(message);
		}
		return d;
	}
}
