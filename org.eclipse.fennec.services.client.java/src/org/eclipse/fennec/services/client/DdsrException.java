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
	private final boolean transportFailure;

	public DdsrException(String message) {
		super(message);
		this.diagnostic = null;
		this.transportFailure = false;
	}

	public DdsrException(String message, Throwable cause) {
		super(message, cause);
		this.diagnostic = null;
		this.transportFailure = false;
	}

	private DdsrException(String message, Throwable cause, boolean transportFailure) {
		super(message, cause);
		this.diagnostic = null;
		this.transportFailure = transportFailure;
	}

	/**
	 * The remote end could not be reached or did not answer in time
	 * (connect refused, connect/read timeout, connection reset) — the call
	 * may never have arrived. A tracked locator rebinds to another
	 * registration and the proxy retries once on this kind of failure (#59).
	 */
	public static DdsrException transport(String message, Throwable cause) {
		return new DdsrException(message, cause, true);
	}

	public boolean isTransportFailure() {
		return transportFailure;
	}

	public DdsrException(Diagnostic diagnostic) {
		super(diagnostic.getMessage() != null
				? diagnostic.getMessage() + " (code=" + diagnostic.getCode() + ")"
				: "broker error (code=" + diagnostic.getCode() + ")");
		this.diagnostic = diagnostic;
		this.transportFailure = false;
	}

	/** May be null if no diagnostic was available (e.g. transport failure). */
	public Diagnostic diagnostic() {
		return diagnostic;
	}
}
