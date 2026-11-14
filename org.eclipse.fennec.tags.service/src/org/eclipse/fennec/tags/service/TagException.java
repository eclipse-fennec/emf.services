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

/** Base of what the tag service refuses. */
public class TagException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TagException(String message) {
		super(message);
	}

	public TagException(String message, Throwable cause) {
		super(message, cause);
	}
}
