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
package org.eclipse.fennec.services.common;

/**
 * A failure that says which declared error it is (#301), for a service that
 * has no exception class per error — one that answers operations by name
 * ({@code OperationHandler}). It is the declared error whose {@code code}
 * it carries or, without a code, whose name it carries.
 */
public class DeclaredFailure extends Exception {

	private static final long serialVersionUID = 1L;

	private final int code;
	private final String error;

	/**
	 * @param code the declared error's code; 0 for none
	 * @param error the declared error's name in the contract, or {@code null}
	 * @param message what went wrong
	 */
	public DeclaredFailure(int code, String error, String message) {
		super(message);
		this.code = code;
		this.error = error;
	}

	/** @return the declared error's code, 0 for none */
	public int code() {
		return code;
	}

	/** @return the declared error's name, or {@code null} */
	public String error() {
		return error;
	}
}
