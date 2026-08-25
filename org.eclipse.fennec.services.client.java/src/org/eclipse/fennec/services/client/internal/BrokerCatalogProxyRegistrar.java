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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.client.ServiceProxyFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Discovers the broker's self-published {@code BrokerCatalog}
 * service, builds a typed Java proxy via {@link ServiceProxyFactory},
 * and registers it as an OSGi service with
 * {@code service.imported=true}. Consumers can then
 * {@code @Reference BrokerCatalogRemote} and call it like any local
 * service — the proxy translates each method call into a remote
 * invocation under the hood.
 *
 * <p>Hand-wired for {@code BrokerCatalog} as a demo. The same
 * pattern can be generalised once we track which Java interfaces
 * the consumer's bundle declares as "import these from the catalog".
 */
@Component(immediate = true)
public final class BrokerCatalogProxyRegistrar {

	private static final Logger LOG = Logger.getLogger(BrokerCatalogProxyRegistrar.class.getName());

	@Reference
	private DdsrClient client;

	@Reference(target = "(ddsr.broker.transport=rest)")
	private ServiceProxyFactory proxyFactory;

	private ServiceRegistration<BrokerCatalogRemote> registration;

	@Activate
	void activate(BundleContext ctx) {
		try {
			List<ServiceLocator> locators = client.consumer().find("BrokerCatalog", null);
			if (locators.isEmpty()) {
				LOG.warning("[DDSR-Client] no BrokerCatalog provider in catalog — proxy not registered");
				return;
			}
			ServiceLocator locator = locators.get(0);
			BrokerCatalogRemote proxy = proxyFactory.newProxy(BrokerCatalogRemote.class, locator);

			Dictionary<String, Object> props = new Hashtable<>();
			props.put("service.imported", Boolean.TRUE);
			props.put("service.imported.configs", new String[] { "ddsr.rest" });
			props.put("ddsr.service.interface", "BrokerCatalog");
			this.registration = ctx.registerService(BrokerCatalogRemote.class, proxy, props);
			LOG.info("[DDSR-Client] registered BrokerCatalogRemote proxy (service.imported=true)");
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR-Client] BrokerCatalogRemote proxy registration FAILED", t);
		}
	}

	@Deactivate
	void deactivate() {
		if (registration != null) {
			try {
				registration.unregister();
			} catch (Exception ignore) {
				// service may already be gone
			}
			registration = null;
		}
	}
}
