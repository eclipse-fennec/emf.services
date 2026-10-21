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
package org.eclipse.fennec.services.examples.rsa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.eclipse.fennec.services.examples.rsa.api.Ledger;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * The ledger, exported like the greeter: properties and nothing else.
 * Nothing here checks a caller — the interface's annotations say who may
 * call, and the node that serves it holds every call to them.
 */
@Component(
		service = Ledger.class,
		configurationPid = "org.eclipse.fennec.services.examples.rsa.ledger",
		property = {
				"service.exported.interfaces=*",
				"service.exported.configs=fennec.rest",
				"ddsr.provider.name=rsa-example" })
public class LedgerImpl implements Ledger {

	private static final Logger LOG = Logger.getLogger(LedgerImpl.class.getName());

	private final Map<String, Double> balances = new ConcurrentHashMap<>(Map.of("demo", 100.0));

	@Activate
	void activate(Map<String, Object> properties) {
		LOG.info("[RSA-Example] Ledger asks to be exported as " + properties.get("service.exported.configs"));
	}

	@Override
	public double balance(String account) {
		return balances.getOrDefault(account, 0.0);
	}

	@Override
	public double record(String account, double amount) {
		LOG.info("[RSA-Example] record(" + account + ", " + amount + ")");
		return balances.merge(account, amount, Double::sum);
	}

	@Override
	public String version() {
		return "ledger 1.0";
	}
}
