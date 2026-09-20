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
 * What the event stream has done, and what it owes.
 *
 * <p>Worth reporting because both numbers mean something a consumer
 * cannot see for itself: the broker keeps no per-client history and the
 * stream carries no sequence numbers, so a subscriber has no way of
 * knowing it missed anything.
 */
public class DeliveryDTO extends DTO {

	/** Events dropped because a subscriber stopped reading. */
	public long dropped;

	/** Whether subscribers are still owed a "read everything again". */
	public boolean owesResync;
}
