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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceInterface;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Throwaway smoke check: calls {@link BrokerCatalogRemote#listCatalog()}
 * directly on the imported-service proxy. No lookup boilerplate, no
 * reflective invoker call — the consumer just talks to the broker
 * through a typed Java method.
 *
 * <p>The proxy is built by {@link BrokerCatalogProxyRegistrar} and
 * registered as an OSGi service with {@code service.imported=true}.
 *
 * <p>Delete this class once the roundtrip is no longer interesting.
 */
@Component(immediate = true)
public final class ClientRoundtripDebug {

	private static final Logger LOG = Logger.getLogger(ClientRoundtripDebug.class.getName());

	@Reference(target = "(service.imported=true)")
	private BrokerCatalogRemote catalog;

	@Activate
	void activate() {
		LOG.info("[DDSR-Debug] calling BrokerCatalogRemote.listCatalog() on imported proxy");
		try {
			RemoteServiceRegistry result = catalog.listCatalog();
			if (result == null) {
				LOG.info("[DDSR-Debug] result: null");
				return;
			}
			LOG.info("[DDSR-Debug] result: RemoteServiceRegistry, "
					+ result.getCatalog().size() + " catalog entries:");
			for (ServiceInterface si : result.getCatalog()) {
				LOG.info("[DDSR-Debug]    - " + si.getName() + " v" + si.getVersion());
			}
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR-Debug] listCatalog FAILED", t);
		}
	}
}
