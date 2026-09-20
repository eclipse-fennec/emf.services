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

package org.eclipse.fennec.services.rsa.discovery;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.eclipse.fennec.services.rsa.spi.ServiceDiscovery;
import org.osgi.service.component.ComponentServiceObjects;

/**
 * Discovery over the broker, whatever the events arrive on.
 *
 * <p>Announcing is a publish: the contract goes into the catalog and
 * the implementation is registered, which is what makes it findable —
 * the registry is the discovery. Hearing is the other direction of the
 * same thing: a lookup for what is there now, and the broker's events
 * for what changes.
 *
 * <p>Which transport those events arrive on is not decided here and
 * does not need to be. The SDK's consumer already abstracts it: a
 * deployment wires the client to the SSE source or to the MQTT one, and
 * this class hears the same events either way. That is why the two
 * discovery providers over it — {@code fennec.rest} and
 * {@code fennec.mqtt} — are components rather than copies: what differs
 * between them is which configuration type a deployment names and which
 * event source it insists on, not what discovery means.
 */
public final class BrokerDiscovery implements ServiceDiscovery {

	private static final Logger LOG = Logger.getLogger(BrokerDiscovery.class.getName());

	private final String[] configTypes;

	private final DdsrClient client;

	private final BrokerCatalog catalog;

	private final ComponentServiceObjects<ResourceSet> resourceSets;

	private final String brokerUrl;

	/**
	 * @param configType    the RSA configuration type the owning component
	 *                      answers to
	 * @param brokerUrl     only used to name a contract in the publish
	 *                      body: the announcement references
	 *                      {@code <broker>/catalog/<name>} instead of
	 *                      carrying a second copy of the contract
	 */
	public BrokerDiscovery(String configType, DdsrClient client, BrokerCatalog catalog,
			ComponentServiceObjects<ResourceSet> resourceSets, String brokerUrl) {
		this.configTypes = new String[] { configType };
		this.client = client;
		this.catalog = catalog;
		this.resourceSets = resourceSets;
		this.brokerUrl = brokerUrl;
	}

	@Override
	public String[] supportedConfigs() {
		return configTypes.clone();
	}

	@Override
	public AutoCloseable announce(ExportedEndpoint endpoint) {
		ServiceInterface contract = endpoint.contract();
		ServiceImplementation implementation = endpoint.implementation();

		Diagnostic added = catalog.addCatalogEntry(contract, implementation.getName());
		if (added.getSeverity() == DiagnosticSeverity.ERROR) {
			throw new IllegalStateException("the broker refused " + contract.getName() + ": "
					+ added.getMessage() + " (code " + added.getCode() + ")");
		}

		Announcements.Announced announcement = Announcements.publish(client, resourceSets, brokerUrl,
				contract, implementation, implementation.getName());
		LOG.info("[DDSR] announced " + contract.getName() + " as " + implementation.getImplementationId());

		AtomicBoolean announced = new AtomicBoolean(true);
		return () -> {
			if (announced.compareAndSet(true, false)) {
				announcement.close();
				LOG.info("[DDSR] withdrew " + contract.getName());
			}
		};
	}

	@Override
	public AutoCloseable watch(String contractName, DiscoveryListener listener) {
		// What is there now, then what changes — in that order, so a
		// registration that already existed is not missed in the gap
		// between asking and subscribing. A duplicate report is harmless;
		// a missed one is a service nobody ever sees.
		for (ServiceLocator known : client.consumer().find(contractName, null)) {
			listener.appeared(referenceId(known), known.implementation());
		}
		return client.consumer().addServiceListener(contractName, null,
				event -> report(event, contractName, listener));
	}

	private void report(ServiceEvent event, String contractName, DiscoveryListener listener) {
		ServiceReference reference = event.getReference();
		String id = reference == null ? null : reference.getId();
		if (id == null) {
			return;
		}
		ServiceEventType type = event.getType();
		if (type != ServiceEventType.REGISTERED && type != ServiceEventType.MODIFIED) {
			// UNREGISTERING, RETIRED, PROVIDER_LOST: whatever the reason,
			// the answer for a consumer is the same — stop using it.
			listener.gone(id);
			return;
		}
		ServiceImplementation implementation = implementationOf(reference);
		if (implementation == null) {
			// The event document did not carry it. Ask, rather than
			// report half an arrival.
			implementation = client.consumer().find(contractName, null).stream()
					.filter(locator -> id.equals(referenceId(locator)))
					.map(ServiceLocator::implementation)
					.findFirst()
					.orElse(null);
		}
		if (implementation == null) {
			LOG.log(Level.FINE, () -> "[DDSR] " + type + " for " + id
					+ " carried no implementation and none was found — ignored");
			return;
		}
		if (type == ServiceEventType.REGISTERED) {
			listener.appeared(id, implementation);
		} else {
			listener.changed(id, implementation);
		}
	}

	private static String referenceId(ServiceLocator locator) {
		return locator.reference() == null ? null : locator.reference().getId();
	}

	/** The implementation an event's reference points at, when it travelled with it. */
	private static ServiceImplementation implementationOf(ServiceReference reference) {
		ServiceProvider provider = reference.getProvider();
		if (provider == null || provider.eIsProxy() || provider.getImplementations().isEmpty()) {
			return null;
		}
		return provider.getImplementations().get(0);
	}
}
