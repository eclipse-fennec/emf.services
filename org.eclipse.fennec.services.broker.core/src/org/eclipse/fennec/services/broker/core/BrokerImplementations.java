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
	 * Registers a Service for the provider, returning the resulting
	 * {@link ServiceRegistration}. Local-style entry point used by
	 * in-process callers; remote providers go via
	 * {@link #publishImplementation}.
	 */
	ServiceRegistration registerService(ServiceProvider provider, ServiceImplementation implementation);
}
