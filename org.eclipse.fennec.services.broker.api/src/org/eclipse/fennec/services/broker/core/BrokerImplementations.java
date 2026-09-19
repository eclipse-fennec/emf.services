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

package org.eclipse.fennec.services.broker.core;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceRegistration;

/**
 * Provider-side surface: publishing and withdrawing implementations,
 * plus the local-style {@code registerService} entry point used by
 * in-process callers. Mirrors the {@code BrokerImplementations}
 * ServiceInterface published by {@code BrokerSelfPublisher}.
 */
public interface BrokerImplementations {

	/**
	 * Publishes a {@link ServiceImplementation} into the registry. The
	 * implementation MUST be contained in {@code provider.implementations}
	 * (containment ownership). All referenced ServiceInterfaces must
	 * exist in the catalog. Returns a Diagnostic with severity
	 * {@code WARNING} if any of the referenced interfaces is
	 * {@code DEPRECATED}, {@code OK} otherwise.
	 */
	Diagnostic publishImplementation(ServiceProvider provider, ServiceImplementation implementation);

	/**
	 * Withdraws an implementation. Symmetric to publish: the provider
	 * MUST own the implementation; verified before the action runs.
	 */
	Diagnostic withdrawImplementation(ServiceProvider provider, ServiceImplementation implementation);

	/**
	 * Modifies a live registration in place (#55): flavors, properties,
	 * capabilities, description and update-policy knobs are taken from
	 * the given implementation; the reference id and every lease stay,
	 * consumers receive {@code MODIFIED}. The implemented contracts must
	 * be the same catalog entries — a contract change is refused with
	 * {@code CODE_IMPL_CONTRACT_CHANGED} and is a publish, not a modify.
	 * Not published (or parked cold) → {@code CODE_IMPL_NOT_PUBLISHED}.
	 * Resolution by (name, version), as for withdraw.
	 */
	Diagnostic modifyImplementation(ServiceProvider provider, ServiceImplementation implementation);

	/**
	 * Provider liveness (#52, UPDATE_POLICY.md §4): tells the broker that
	 * the provider behind the registration with this reference id is
	 * still alive and will repeat this call every {@code intervalSeconds}.
	 * Opt-in per registration — a registration that never heartbeats is
	 * never retired for silence. Once armed, the broker retires the
	 * registration after two missed heartbeats ({@code 2 × interval}
	 * without a call), announcing {@code UNREGISTERING} + {@code RETIRED}
	 * with reason {@code PROVIDER_LOST}. Unknown or already retired
	 * reference id → {@code CODE_IMPL_NOT_PUBLISHED}: the provider's cue
	 * to publish again (broker restart, cold cache, lost by an earlier
	 * silence). {@code intervalSeconds <= 0} → {@code CODE_HEARTBEAT_INVALID}.
	 */
	Diagnostic heartbeat(String referenceId, long intervalSeconds);

	/**
	 * Registers a Service for the provider, returning the resulting
	 * {@link ServiceRegistration}. Local-style entry point used by
	 * in-process callers; remote providers go via
	 * {@link #publishImplementation}.
	 */
	ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation);
}
