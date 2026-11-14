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

package org.eclipse.fennec.services.client;

/**
 * Nothing implements the contract right now.
 *
 * <p>Thrown by a call through a locator or proxy whose implementation
 * went away and for which the lookup finds no other. The contract itself
 * may well still be in the catalog — a contract stands on its own and
 * outlives its implementations (#271) — so this is a state that passes:
 * once an implementation is registered again, the next call through the
 * same proxy binds to it.
 *
 * <p>Not a transport failure, so nothing retries it. A caller that wants
 * to wait listens for {@code REGISTERED} on the interface
 * ({@link DdsrConsumer#addServiceListener}) instead of calling in a loop.
 * The twin of {@code DdsrServiceUnavailableError} in the TypeScript
 * client.
 */
public class DdsrServiceUnavailableException extends DdsrException {

	private static final long serialVersionUID = 1L;

	private final String interfaceName;
	private final boolean parked;

	/**
	 * @param interfaceName the contract that has no implementation
	 * @param lastReference the reference the caller was bound to, for the
	 *                      log; may be {@code null}
	 * @param parked        {@code true} when that reference was moved to
	 *                      the cold cache and could not be brought back
	 */
	public DdsrServiceUnavailableException(String interfaceName, String lastReference, boolean parked) {
		super("no implementation of '" + interfaceName + "' is available"
				+ (parked ? ": the last one is parked and could not be brought back" : "")
				+ (lastReference == null ? "" : " (last bound to " + lastReference + ")"));
		this.interfaceName = interfaceName;
		this.parked = parked;
	}

	/** The contract that has no implementation. */
	public String interfaceName() {
		return interfaceName;
	}

	/** Whether the last implementation was parked rather than withdrawn. */
	public boolean parked() {
		return parked;
	}
}
