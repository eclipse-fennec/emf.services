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

package org.eclipse.fennec.services.client;

import org.eclipse.fennec.services.Diagnostic;

/**
 * Thrown by the SDK when a broker action is refused (ERROR or CANCEL
 * severity) or when transport fails. The original {@link Diagnostic}
 * — when the broker returned one — is attached.
 */
public class DdsrException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient Diagnostic diagnostic;

	public DdsrException(String message) {
		super(message);
		this.diagnostic = null;
	}

	public DdsrException(String message, Throwable cause) {
		super(message, cause);
		this.diagnostic = null;
	}

	public DdsrException(Diagnostic diagnostic) {
		super(diagnostic.getMessage() != null
				? diagnostic.getMessage() + " (code=" + diagnostic.getCode() + ")"
				: "broker error (code=" + diagnostic.getCode() + ")");
		this.diagnostic = diagnostic;
	}

	/** May be null if no diagnostic was available (e.g. transport failure). */
	public Diagnostic diagnostic() {
		return diagnostic;
	}
}
