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

/** The change could not be kept; it was rolled back. */
public class TagStoreException extends TagException {

	private static final long serialVersionUID = 1L;

	public TagStoreException(String message) {
		super(message);
	}

	public TagStoreException(String message, Throwable cause) {
		super(message, cause);
	}
}
