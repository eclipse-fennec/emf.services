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
 * <p>Both halves already exist in this registry and neither is invented
 * here: announcing is publishing the implementation to the broker, and
 * hearing is the broker's event stream plus a lookup. What varies is the
 * transport those two run over — REST with SSE today, MQTT next — which
 * is why this is an interface and not a class.
 *
 * <p>Separate from {@link FlavorDistribution} on purpose: how a service
 * is reached and how one learns it exists are different questions, and a
 * deployment may well answer them over different transports.
 */
public interface FlavorDiscovery {

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
