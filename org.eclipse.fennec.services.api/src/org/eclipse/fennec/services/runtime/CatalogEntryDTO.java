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

/** One contract the broker governs. */
public class CatalogEntryDTO extends DTO {

	public String name;

	public String version;

	/** ACTIVE or DEPRECATED — a deprecation is one-way. */
	public String status;

	/** sd1, which is what a lookup can address this entry by. */
	public String fingerprint;

	/** Why it was deprecated, when it was. */
	public String deprecationReason;

	/** How many live registrations serve it. */
	public int implementations;
}
