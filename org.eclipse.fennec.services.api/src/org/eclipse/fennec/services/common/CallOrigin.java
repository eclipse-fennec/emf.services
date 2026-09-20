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

/**
 * The origin of the call being served, for the duration of that call.
 *
 * <p>A side channel, and it is worth saying why it is one. The broker's
 * REST surface is not hand-written: the broker publishes its own
 * contracts and the generic REST distribution answers requests by those
 * documents (#76), which means a transport can only hand the
 * implementation what the CONTRACT declares. Modelling the origin as a
 * parameter of every operation would therefore be the werktreu move —
 * and it would change the sd1 of every broker contract, because
 * parameters are part of the fingerprint and the origin is not part of
 * any contract. An origin is a property of the call, like
 * {@code traceparent} in #126, not of what is being called.
 *
 * <p>So the transport puts it here and the implementation reads it here.
 * Bound to the serving thread, which is sound because the dispatcher
 * invokes synchronously on the thread the request arrived on; should an
 * async path ever appear, this is the assumption that breaks, and it is
 * written down for that reason.
 *
 * <p>{@link #set} is called on every request, with {@code null} when no
 * origin was sent, so a pooled thread cannot carry a previous caller's
 * origin into the next request even if a clear is missed.
 */
public final class CallOrigin {

	private static final ThreadLocal<ClientOrigin> CURRENT = new ThreadLocal<>();

	private CallOrigin() {
	}

	/**
	 * Binds the origin of the request this thread is about to serve.
	 *
	 * @param origin the parsed origin, or {@code null} when the call
	 *        carried none — which must still be set, so that nothing is
	 *        inherited from whatever this thread served before
	 */
	public static void set(ClientOrigin origin) {
		CURRENT.set(origin);
	}

	/** The origin of the call being served, or {@code null}. */
	public static ClientOrigin current() {
		return CURRENT.get();
	}

	/**
	 * Who to record for this call.
	 *
	 * @param explicit an identity the protocol itself carries — the
	 *        catalog's {@code requestor} parameter — which wins, because
	 *        a caller that names itself on purpose knows better than the
	 *        transport does. {@link ClientOrigin#ANONYMOUS} does not
	 *        count as naming itself: the catalog contract declares that
	 *        word as the parameter's DEFAULT value, so the dispatcher
	 *        hands it over for every call that named nobody, and taking
	 *        it at face value made the origin lose to a default that no
	 *        caller ever chose.
	 * @return the explicit identity, else the call's origin, else
	 *         {@link ClientOrigin#ANONYMOUS}; never {@code null}, so an
	 *         audit field is never silently left empty
	 */
	public static String requestor(String explicit) {
		if (explicit != null && !explicit.isBlank() && !ClientOrigin.ANONYMOUS.equals(explicit)) {
			return explicit;
		}
		ClientOrigin origin = CURRENT.get();
		return origin == null ? ClientOrigin.ANONYMOUS : origin.token();
	}

	/** Releases the binding when the request is done. */
	public static void clear() {
		CURRENT.remove();
	}
}
