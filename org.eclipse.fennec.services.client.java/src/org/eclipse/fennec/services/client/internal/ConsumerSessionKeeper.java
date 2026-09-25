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

package org.eclipse.fennec.services.client.internal;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerSessions;

/**
 * This consumer's session at the broker, for as long as the client
 * lives (ACQUISITION.md §4).
 *
 * <p>Lifted out of {@code DdsrClientComponent} so it can be tested at
 * all. It used to be two private methods on a DS component, reachable
 * only through {@code @Activate}/{@code @Deactivate} with an OSGi
 * runtime and a configuration — which is why this, the part that holds
 * the leases, was the one corner of the Java client with no tests
 * whatsoever, while the TypeScript side had nine. The acquisition-list
 * bug of #124 lived exactly here and was found by reading.
 *
 * <p>The same rule the broker follows since #110 applies: what the
 * component does and what a plain-Java caller does has to be the same
 * thing, so the component keeps the schedule and this keeps the
 * behaviour.
 */
final class ConsumerSessionKeeper {

	private static final Logger LOG = Logger.getLogger(ConsumerSessionKeeper.class.getName());

	private final BrokerSessions sessions;

	private final String consumerId;

	private final List<FlavorKind> supportedFlavors;

	/**
	 * What this consumer currently claims. Asked on every renewal rather
	 * than remembered: the list IS the client's live view, and a copy
	 * would start lying the moment a lookup or a release changed it.
	 */
	private final Supplier<Collection<String>> acquisitions;

	ConsumerSessionKeeper(BrokerSessions sessions, String consumerId, List<FlavorKind> supportedFlavors,
			Supplier<Collection<String>> acquisitions) {
		this.sessions = sessions;
		this.consumerId = consumerId;
		this.supportedFlavors = supportedFlavors;
		this.acquisitions = acquisitions;
	}

	/**
	 * The idempotent full replace: the current set of known reference
	 * ids IS the acquisition list, and an unchanged PUT is the
	 * heartbeat.
	 *
	 * <p>Best effort. A missed renewal is silence like any other, and
	 * the broker's TTL already covers it — so a broker that is briefly
	 * unreachable must not surface as an error in a client that is
	 * otherwise working.
	 */
	void renew() {
		if (sessions == null || consumerId == null || consumerId.isBlank()) {
			return;
		}
		try {
			ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
			session.setConsumerId(consumerId);
			ConsumerCapability capability = ServicesFactory.eINSTANCE.createConsumerCapability();
			capability.setConsumerId(consumerId);
			if (supportedFlavors != null) {
				capability.getSupportedFlavors().addAll(supportedFlavors);
			}
			session.setCapabilities(capability);
			Diagnostic answer = sessions.putSession(session, acquisitions.get());
			if (refused(answer)) {
				// A refusal does not throw: the broker, or a proxy in front
				// of it, answered, and the answer was no. Said as loudly as
				// a failure to reach it, because until the next renewal the
				// broker holds none of this consumer's leases (#170).
				LOG.warning("[DDSR-Client] session renewal refused (" + answer.getCode()
						+ "), retrying next interval: " + answer.getMessage());
			}
		} catch (RuntimeException renewalFailure) {
			LOG.warning("[DDSR-Client] session renewal failed, retrying next interval: " + renewalFailure);
		}
	}

	private static boolean refused(Diagnostic answer) {
		return answer != null && (answer.getSeverity() == DiagnosticSeverity.ERROR
				|| answer.getSeverity() == DiagnosticSeverity.CANCEL);
	}

	/**
	 * Tells the broker the session is over, so it releases the leases
	 * now instead of waiting out the TTL.
	 *
	 * <p>Called BEFORE the streams close (FR-P3 order) and best effort
	 * in the strongest sense: a broker that is already gone must not be
	 * able to stall a client's shutdown. Reported on stdout rather than
	 * through JUL, because this runs on the shutdown path where JUL's
	 * own cleanup hook may already have reset the LogManager
	 * (DECISIONS_PARITY D14).
	 */
	void release() {
		if (sessions == null || consumerId == null || consumerId.isBlank()) {
			return;
		}
		try {
			sessions.deleteSession(consumerId);
			System.out.println("[DDSR-Client] session released at broker: " + consumerId);
		} catch (RuntimeException deleteFailure) {
			System.err.println("[DDSR-Client] session release failed, broker will expire it: " + deleteFailure);
		}
	}
}
