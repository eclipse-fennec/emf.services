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

import java.util.Map;

import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceInterface;

/**
 * Makes a service reachable over one flavor.
 *
 * <p>This is the only thing the RSA core knows about transports. REST is
 * one implementation, MQTT will be another, and the core does not change
 * for the second — which is the point of having this interface at all.
 *
 * <p>Which implementation is used is an RSA decision, taken the way the
 * specification says: a service asks for a configuration type through
 * {@code service.exported.configs}, and the provider that
 * {@link #supportedConfigs() offers} it answers.
 */
public interface FlavorDistribution {

	/**
	 * The RSA configuration types this provider answers to, e.g.
	 * {@code fennec.rest}. A service naming one of them in
	 * {@code service.exported.configs} is exported by this provider.
	 */
	String[] supportedConfigs();

	/** The flavor kind this provider speaks, as the model names it. */
	FlavorKind flavor();

	/**
	 * The intents this provider can promise — {@code osgi.basic} for a
	 * synchronous call with arguments and results carried across and
	 * exceptions coming back. A service that asks for an intent nobody
	 * here offers is not exported, which is what the specification wants
	 * and what a consumer relying on that intent would want too.
	 */
	default String[] supportedIntents() {
		return new String[] { "osgi.basic" };
	}

	/**
	 * Make {@code service} reachable, and say in the model what a
	 * consumer has to do to reach it.
	 *
	 * <p>The contract arrives ready. Whether it was written by its
	 * provider or derived from a Java interface is the registry's
	 * business and deliberately not visible here — a distribution that
	 * could tell the two apart would be able to treat them differently,
	 * and then the promise that it cannot would be worth nothing.
	 *
	 * @param service    the object that answers calls
	 * @param contract   what it promises
	 * @param properties the effective service properties — the RSA ones
	 *                   and whatever the exporter added
	 * @return the live endpoint; closing it takes it down again
	 */
	ExportedEndpoint export(Object service, ServiceInterface contract, Map<String, ?> properties);
}
