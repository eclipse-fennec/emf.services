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

package org.eclipse.fennec.services.examples.persons;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 *
 * <p>It follows the broker's events for {@code Payment}: when a provider
 * comes, changes or goes, the proxies are registered anew — and that
 * listener is what opens the client's event stream in this launch.
 */
@Component(immediate = true)
public final class PaymentProxyRegistrar {

	private static final Logger LOG = Logger.getLogger(PaymentProxyRegistrar.class.getName());

	@Reference
	private DdsrClient client;

	// No target: the proxy factory is transport-agnostic since #98 and
	// picks an invoker by the flavor the service announces.
	@Reference
	private ServiceProxyFactory proxyFactory;

	/** One proxy per provider, by its name. */
	private final Map<String, ServiceRegistration<PaymentRemote>> registrations = new HashMap<>();

	private AutoCloseable listening;

	@Activate
	void activate(BundleContext context) {
		follow(context);
		listening = client.consumer().addServiceListener("Payment", null, event -> {
			LOG.info("[DDSR-Client] Payment " + event.getType() + " — following the providers");
			follow(context);
		});
	}

	/**
	 * Registers a proxy for every provider the broker lists that has none
	 * yet, and takes away the proxies of providers it no longer lists — a
	 * proxy that stays is left alone, so whoever uses it is not restarted.
	 */
	private synchronized void follow(BundleContext context) {
		try {
			List<ServiceLocator> locators = client.consumer().find("Payment", null);
			if (locators.isEmpty() && registrations.isEmpty()) {
				LOG.info("[DDSR-Client] Payment has no providers yet — no proxy registered");
			}
			Set<String> listed = new HashSet<>();
			for (ServiceLocator locator : locators) {
				String providerName = providerNameOf(locator);
				listed.add(providerName);
				if (registrations.containsKey(providerName)) {
					continue;
				}
				PaymentRemote proxy = proxyFactory.newProxy(PaymentRemote.class, locator);
				Dictionary<String, Object> props = new Hashtable<>();
				props.put("service.imported", Boolean.TRUE);
				props.put("service.imported.configs", new String[] { "ddsr.rest" });
				props.put("ddsr.service.interface", "Payment");
				props.put("ddsr.provider.name", providerName);
				registrations.put(providerName, context.registerService(PaymentRemote.class, proxy, props));
				LOG.info("[DDSR-Client] registered PaymentRemote proxy "
						+ "(service.imported=true, ddsr.provider.name=" + providerName + ")");
			}
			for (String gone : new HashSet<>(registrations.keySet())) {
				if (!listed.contains(gone)) {
					unregister(registrations.remove(gone));
					LOG.info("[DDSR-Client] took the PaymentRemote proxy of " + gone + " away");
				}
			}
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR-Client] PaymentRemote proxy registration FAILED", t);
		}
	}

	@Deactivate
	synchronized void deactivate() {
		if (listening != null) {
			try {
				listening.close();
			} catch (Exception ignored) {
				// going down anyway
			}
		}
		registrations.values().forEach(PaymentProxyRegistrar::unregister);
		registrations.clear();
	}

	private static void unregister(ServiceRegistration<PaymentRemote> registration) {
		try {
			registration.unregister();
		} catch (Exception alreadyGone) {
			// best-effort
		}
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
