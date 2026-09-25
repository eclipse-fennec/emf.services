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

/**
 * One consumer, and what it holds.
 *
 * <p>A session is an assertion with an expiry date, not a use count: a
 * consumer that crashes does not count itself down, so what is here is
 * what the broker was last told, with the time it was told.
 */
public class SessionDTO extends DTO {

	/** How the consumer names itself. */
	public String consumerId;

	/**
	 * Where it reached the broker from, as the origin token
	 * {@code label/runtimeId} — the same token a span carries as
	 * {@code fennec.origin}, which is what lets a lease be matched to a
	 * call (#166). The consumer id is chosen by the client; this is not.
	 */
	public String origin;

	/** When it last renewed, in epoch milliseconds. */
	public long lastRenewal;

	/** The registrations it acquired, by reference id. */
	public List<String> acquisitions;

	/** Whether its event stream is currently open, when that is known. */
	public boolean connected;
}
