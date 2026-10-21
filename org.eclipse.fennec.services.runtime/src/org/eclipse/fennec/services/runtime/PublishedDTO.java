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

import java.util.List;

import org.osgi.dto.DTO;

/** One implementation this runtime published, as the broker took it. */
public class PublishedDTO extends DTO {

	public String implementationId;

	public String version;

	/** The contracts it serves. */
	public List<String> contracts;

	/** What the broker calls the registration, or null if it was refused. */
	public String referenceId;

	/** Whether the last publish or heartbeat was accepted. */
	public boolean live;

	/**
	 * Why it is not live, when the broker said.
	 *
	 * <p>The same service a whiteboard runtime does with its failed
	 * DTOs: something that was offered and not taken is worth reporting
	 * <em>with the reason</em>, because the alternative is a watcher
	 * that sees an absence and has nowhere to look. Null while the
	 * broker is happy.
	 */
	public String failure;
}
