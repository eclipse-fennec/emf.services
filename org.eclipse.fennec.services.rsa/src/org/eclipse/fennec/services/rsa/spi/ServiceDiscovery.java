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

package org.eclipse.fennec.services.rsa.spi;

import org.eclipse.fennec.services.ServiceImplementation;

/**
 * Tells others about an exported service, and hears about theirs.
 *
 * <p>Deliberately not named after a flavor. This registry's own
 * discovery — publish to the broker, listen on its event stream — is one
 * implementation of it, and it has to be replaceable: a deployment may
 * find its services by cluster gossip, from a file, by something that
 * does not exist yet. None of that is a reason to give up the model.
 * What travels through here is a {@code ServiceImplementation} either
 * way, so another discovery changes where the knowledge comes from and
 * nothing about what is known.
 *
 * <p>Separate from {@link FlavorDistribution} for the same reason: how a
 * service is reached and how one learns it exists are different
 * questions, and a deployment may answer them differently.
 */
public interface ServiceDiscovery {

	/**
	 * The RSA configuration types this provider answers to. A
	 * deployment selects its discovery the same way it selects its
	 * distribution.
	 */
	String[] supportedConfigs();

	/**
	 * Announce an exported endpoint, and keep it announced.
	 *
	 * @return closing it withdraws the announcement — before the
	 *         endpoint goes down, never after, so nobody is sent to an
	 *         address that has already stopped answering
	 */
	AutoCloseable announce(ExportedEndpoint endpoint);

	/**
	 * Watch for implementations of a contract that others announced.
	 *
	 * @param contractName the contract to watch
	 * @param listener     told about every implementation that arrives,
	 *                     changes or goes away
	 * @return closing it stops the watch
	 */
	AutoCloseable watch(String contractName, DiscoveryListener listener);

	/**
	 * What a watch reports.
	 *
	 * <p>A registration is identified by its reference id, and that is
	 * the only thing still available when it is gone: the announcement
	 * that carried the implementation is exactly what was withdrawn. So
	 * departure is reported by id alone — which is also all a consumer
	 * needs to drop what it holds.
	 */
	interface DiscoveryListener {

		void appeared(String referenceId, ServiceImplementation implementation);

		void changed(String referenceId, ServiceImplementation implementation);

		void gone(String referenceId);
	}
}
