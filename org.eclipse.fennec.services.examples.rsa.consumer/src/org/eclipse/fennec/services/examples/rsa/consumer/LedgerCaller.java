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
package org.eclipse.fennec.services.examples.rsa.consumer;

import java.util.logging.Logger;

import org.eclipse.fennec.services.examples.rsa.api.Ledger;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Calls the ledger as this node — which holds ledger-reader and not
 * ledger-writer. A plain {@code @Reference}: the token is the node's,
 * shown by the imported proxy, and nothing here knows about it.
 */
@Component(immediate = true)
public class LedgerCaller {

	private static final Logger LOG = Logger.getLogger(LedgerCaller.class.getName());

	@Reference
	private Ledger ledger;

	@Activate
	void activate() {
		LOG.info("[RSA-Consumer] ledger says " + ledger.version());
		LOG.info("[RSA-Consumer] ledger balance of demo: " + ledger.balance("demo"));
		try {
			ledger.record("demo", 5.0);
			LOG.warning("[RSA-Consumer] ledger record went through — this node should not hold ledger-writer");
		} catch (RuntimeException refused) {
			LOG.info("[RSA-Consumer] ledger record refused, as it should be: " + refused.getMessage());
		}
	}
}
