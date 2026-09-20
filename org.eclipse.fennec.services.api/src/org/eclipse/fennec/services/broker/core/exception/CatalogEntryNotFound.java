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

package org.eclipse.fennec.services.broker.core.exception;

/**
 * No contract of that name — or none with the fingerprint that was asked for.
 *
 * <p>Declared by the {@code BrokerCatalog} contract under this name and
 * bound there to an HTTP status. An operation that has no value to
 * return cannot report a failure as one, so this is thrown — the
 * transport matches it to the contract's error by name. Operations that
 * do return a value keep reporting through a {@code Diagnostic}
 * (ARCHITECTURE §2.8); that is the same statement in the shape the
 * return type allows.
 */
public class CatalogEntryNotFound extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** The diagnostic code this failure reports, as the contract declares it. */
	public static final int CODE = 201;

	public CatalogEntryNotFound(String message) {
		super(message);
	}
}
