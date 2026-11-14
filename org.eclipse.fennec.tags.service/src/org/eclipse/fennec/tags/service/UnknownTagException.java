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

package org.eclipse.fennec.tags.service;

/** No tag has this path, neither now nor before a rename. */
public class UnknownTagException extends TagException {

	private static final long serialVersionUID = 1L;

	public UnknownTagException(String message) {
		super(message);
	}

	public UnknownTagException(String message, Throwable cause) {
		super(message, cause);
	}
}
