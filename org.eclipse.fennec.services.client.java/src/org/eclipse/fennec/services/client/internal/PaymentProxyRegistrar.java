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

import java.util.ArrayList;
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
 * Discovers every {@code Payment} provider in the broker and
 * registers a typed Java proxy ({@link PaymentRemote}) per provider
 * with {@code service.imported=true} plus a
 * {@code ddsr.provider.name=<provider>} property. Consumers can then
 * pick the specific provider they want via
 * {@code @Reference(target="(ddsr.provider.name=payments-java)")} —
 * deterministic instead of "whichever locator was returned first".
 */
@Component(immediate = true)
public final class PaymentProxyRegistrar {

	private static final Logger LOG = Logger.getLogger(PaymentProxyRegistrar.class.getName());

	@Reference
	private DdsrClient client;

	@Reference(target = "(ddsr.broker.transport=rest)")
	private ServiceProxyFactory proxyFactory;

	private final List<ServiceRegistration<PaymentRemote>> registrations = new ArrayList<>();

	@Activate
	void activate(BundleContext ctx) {
		try {
			List<ServiceLocator> locators = client.consumer().find("Payment", null);
			if (locators.isEmpty()) {
				LOG.info("[DDSR-Client] Payment has no providers yet — no proxy registered");
				return;
			}
			for (ServiceLocator locator : locators) {
				String providerName = providerNameOf(locator);
				PaymentRemote proxy = proxyFactory.newProxy(PaymentRemote.class, locator);
				Dictionary<String, Object> props = new Hashtable<>();
				props.put("service.imported", Boolean.TRUE);
				props.put("service.imported.configs", new String[] { "ddsr.rest" });
				props.put("ddsr.service.interface", "Payment");
				props.put("ddsr.provider.name", providerName);
				registrations.add(ctx.registerService(PaymentRemote.class, proxy, props));
				LOG.info("[DDSR-Client] registered PaymentRemote proxy "
						+ "(service.imported=true, ddsr.provider.name=" + providerName + ")");
			}
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR-Client] PaymentRemote proxy registration FAILED", t);
		}
	}

	@Deactivate
	void deactivate() {
		for (ServiceRegistration<PaymentRemote> reg : registrations) {
			try {
				reg.unregister();
			} catch (Exception ignore) {
				// best-effort
			}
		}
		registrations.clear();
	}

	private static String providerNameOf(ServiceLocator locator) {
		if (locator.reference() != null && locator.reference().getProvider() != null) {
			String name = locator.reference().getProvider().getName();
			if (name != null && !name.isBlank()) {
				return name;
			}
		}
		return "unknown";
	}
}
