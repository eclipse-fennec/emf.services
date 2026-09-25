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

package org.eclipse.fennec.services.runtime;

import org.osgi.dto.DTO;

/**
 * One contract this runtime is bound to.
 *
 * <p>A binding is a locator that has found something, so what is worth
 * reporting is both what it found and whether that still holds: a
 * locator in {@code REBIND} is one whose provider went away and which
 * has nothing to offer until another arrives.
 */
public class BindingDTO extends DTO {

	public String contract;

	/** The LDAP filter it was tracked with, when there was one. */
	public String filter;

	/** The registration it is bound to, or null while rebinding. */
	public String referenceId;

	/** Where it calls, when the flavor says. */
	public String endpoint;

	/**
	 * The locator's state: LIVE, MODIFIED (changed in place, refreshed on
	 * next use), STALE (parked in the cold cache) or REBIND.
	 */
	public String state;
}
