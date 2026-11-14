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

/** The tree would break: an empty id, a '/' in it, an id taken among the siblings, or a tag under itself. */
public class InvalidTagException extends TagException {

	private static final long serialVersionUID = 1L;

	public InvalidTagException(String message) {
		super(message);
	}

	public InvalidTagException(String message, Throwable cause) {
		super(message, cause);
	}
}
