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
package org.eclipse.fennec.services.common;

import org.eclipse.fennec.services.Principal;

/**
 * Who is making the call currently on this thread, as far as a
 * verifier established it — the twin of {@link CallOrigin}, for the
 * WHO instead of the WHERE.
 *
 * <p>Set by whatever verified the credential at the edge (the bearer
 * filter over REST, later a message handler over MQTT) and read by the
 * broker core, which has no edge of its own and must still say "only
 * the principal that published this registration may withdraw it".
 * Null means the call carried no verified identity: on a broker that
 * runs without authentication, or for an in-process caller that never
 * crossed a system boundary.
 *
 * <p>The holder of a model {@link Principal} must treat it as a
 * template: a model object is contained by what it is put into, so the
 * core copies it into a registration or a session rather than moving
 * the instance.
 */
public final class CallPrincipal {

	private static final ThreadLocal<Principal> CURRENT = new ThreadLocal<>();

	private CallPrincipal() {
		// no instances
	}

	/** Binds the verified principal of the call to this thread; {@code null} unbinds. */
	public static void set(Principal principal) {
		if (principal == null) {
			CURRENT.remove();
		} else {
			CURRENT.set(principal);
		}
	}

	/** The principal of the current call, or {@code null} when the call carried no verified identity. */
	public static Principal current() {
		return CURRENT.get();
	}

	/** The subject of the current call, or {@code null}. */
	public static String subject() {
		Principal principal = CURRENT.get();
		return principal == null ? null : principal.getSubject();
	}

	/** Must be called when the call ends, or a pooled thread lends this identity to the next call. */
	public static void clear() {
		CURRENT.remove();
	}
}
