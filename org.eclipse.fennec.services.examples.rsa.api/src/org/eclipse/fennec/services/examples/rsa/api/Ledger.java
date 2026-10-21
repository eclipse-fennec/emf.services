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
package org.eclipse.fennec.services.examples.rsa.api;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

/**
 * A plain OSGi service interface that says who may call it, the way a
 * JAX-RS resource would (SECURITY.md §6). Exported through RSA, the
 * annotations become the contract's requirements; imported, every call
 * carries the importing node's token, and the exporting node holds it
 * against them — reading for ledger-reader, writing for ledger-writer,
 * the version for anybody.
 */
@RolesAllowed("ledger-reader")
public interface Ledger {

	/**
	 * @param account whose balance
	 * @return the balance
	 */
	double balance(String account);

	/**
	 * @param account where to book
	 * @param amount what to book
	 * @return the balance afterwards
	 */
	@RolesAllowed("ledger-writer")
	double record(String account, double amount);

	/** @return what this ledger is, for anybody who asks */
	@PermitAll
	String version();
}
