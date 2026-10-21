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
package org.eclipse.fennec.services.broker.core.internal;

import java.util.Objects;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.Principal;
import org.eclipse.fennec.services.ServiceRegistration;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.eclipse.fennec.services.common.CallPrincipal;

/**
 * The one rule no contract can state: what a principal registered, only
 * that principal changes (SECURITY.md §3).
 *
 * <p>The principal comes from {@link CallPrincipal}, which the edge set
 * after verifying a credential. Two cases are deliberately left alone,
 * because in both nothing was verified that a refusal could rest on:
 * <ul>
 * <li>a call without a principal — an in-process caller, or a broker
 * running with {@code auth.mode=none};
 * <li>a registration without one — published that way, or rebuilt from
 * a snapshot after a restart, where the principal is not persisted and
 * the provider's next publish claims it again.
 * </ul>
 *
 * <p>An identity is subject and issuer together: the same subject from
 * another issuer is somebody else.
 */
final class Ownership {

	private Ownership() {
		// no instances
	}

	/**
	 * Whether the current call may change this registration, or the
	 * refusal saying why not; {@code null} means it may.
	 */
	static Diagnostic refusal(ServiceRegistration registration, String action) {
		Principal caller = CallPrincipal.current();
		if (caller == null || registration == null) {
			return null;
		}
		Principal owner = registration.getPrincipal();
		if (owner == null || sameIdentity(owner, caller)) {
			return null;
		}
		return DdsrDiagnostics.error(DdsrDiagnostics.CODE_IMPL_OWNERSHIP_VIOLATION,
				action + " " + BrokerState.identityOf(registration) + " refused: it was published by "
						+ describe(owner) + ", not by " + describe(caller));
	}

	/**
	 * Whether the current call may act on the session of this
	 * consumerId; {@code null} means it may. On an authenticated call
	 * the consumerId is the caller's subject, which is what closes the
	 * session takeover (ACQUISITION.md §7).
	 */
	static Diagnostic sessionRefusal(String consumerId, String action) {
		Principal caller = CallPrincipal.current();
		if (caller == null || Objects.equals(consumerId, caller.getSubject())) {
			return null;
		}
		return DdsrDiagnostics.error(DdsrDiagnostics.CODE_SESSION_OWNERSHIP_VIOLATION,
				action + " of session '" + consumerId + "' refused: a session is named by its owner's subject, and the"
						+ " caller is " + describe(caller));
	}

	/**
	 * Writes the caller onto what it just created. A copy, because the
	 * principal is contained by what it is put into and the one on the
	 * thread is still the edge's.
	 */
	static void claim(ServiceRegistration registration) {
		Principal caller = CallPrincipal.current();
		registration.setPrincipal(caller == null ? null : EcoreUtil.copy(caller));
	}

	/** The session twin of {@link #claim(ServiceRegistration)}. */
	static void claim(ConsumerSession session) {
		Principal caller = CallPrincipal.current();
		session.setPrincipal(caller == null ? null : EcoreUtil.copy(caller));
	}

	static boolean sameIdentity(Principal a, Principal b) {
		return Objects.equals(a.getSubject(), b.getSubject()) && Objects.equals(a.getIssuer(), b.getIssuer());
	}

	private static String describe(Principal principal) {
		return "'" + principal.getSubject() + "'"
				+ (principal.getIssuer() == null ? "" : " (" + principal.getIssuer() + ")");
	}
}
