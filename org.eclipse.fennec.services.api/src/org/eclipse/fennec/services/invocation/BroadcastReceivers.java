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

package org.eclipse.fennec.services.invocation;

import java.util.Map;

/**
 * A service whose broadcast wants to know who receives it (#252).
 *
 * <p>A broadcast is not called, and its contract takes nothing. Where a
 * transport knows its receivers one by one — over REST each holds a
 * connection — it tells a service that implements this when one comes and
 * when it goes, with the parameters of the receiver's request: what a
 * receiver says about itself beside the contract, such as the session it
 * belongs to. A transport that does not know its receivers (a messaging
 * broker fans out on its own) tells nothing.
 *
 * <p>Called on the thread that serves the receiver; it must not block.
 */
public interface BroadcastReceivers {

	/**
	 * @param operation  the broadcast's operation
	 * @param parameters the receiver's request parameters, the first value of each
	 */
	void joined(String operation, Map<String, String> parameters);

	/** The same receiver is gone. */
	void left(String operation, Map<String, String> parameters);
}
