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

package org.eclipse.fennec.services.rsa.discovery.rest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.Registration;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.eclipse.fennec.services.rsa.spi.ServiceDiscovery;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Discovery over the broker's REST surface.
 *
 * <p>Announcing is a publish: the contract goes into the catalog and the
 * implementation is registered, which is what makes it findable — the
 * registry is the discovery. Hearing is the other direction of the same
 * thing: a lookup for what is there now, and the broker's event stream
 * for what changes.
 *
 * <p>Nothing here is new machinery. The value is that Remote Service
 * Admin gets a discovery it can drive, and that the transport it runs
 * over is a choice: an MQTT discovery is another implementation of this
 * interface, not a change to anything that uses it.
 */
@Designate(ocd = RestDiscoveryProvider.Config.class)
@Component(service = ServiceDiscovery.class,
		configurationPid = "org.eclipse.fennec.services.rsa.discovery.rest",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RestDiscoveryProvider implements ServiceDiscovery {

	/** The RSA configuration type a deployment names to discover this way. */
	public static final String CONFIG_TYPE = "fennec.rest";

	private static final Logger LOG = Logger.getLogger(RestDiscoveryProvider.class.getName());

	@ObjectClassDefinition(name = "Fennec Services RSA REST Discovery",
			description = "Which broker hears about exported services, and is asked about others'.")
	public @interface Config {

		@AttributeDefinition(name = "Broker URL",
				description = "Only used to name a contract in the publish body: the announcement references "
						+ "<broker>/catalog/<name> instead of carrying a second copy of the contract. The "
						+ "transport to the broker is configured on the client's REST proxy.")
		String broker_url();
	}

	@Reference
	private DdsrClient client;

	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerCatalog catalog;

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	private Config config;

	@Activate
	void activate(Config config) {
		this.config = config;
		LOG.info("[DDSR] discovery ready — announcing to and watching " + config.broker_url());
	}

	/**
	 * A changed configuration is taken, not died of — see the same method
	 * on the REST distribution (#107). A discovery that is destroyed and
	 * rebuilt mid-export hands out service objects that are already dead,
	 * and the export fails for a reason no log explains.
	 */
	@Modified
	void modified(Config config) {
		if (!config.broker_url().equals(this.config.broker_url())) {
			LOG.info("[DDSR] discovery now talks to " + config.broker_url());
		}
		this.config = config;
	}

	@Override
	public String[] supportedConfigs() {
		return new String[] { CONFIG_TYPE };
	}

	@Override
	public AutoCloseable announce(ExportedEndpoint endpoint) {
		ServiceInterface contract = endpoint.contract();
		ServiceImplementation implementation = endpoint.implementation();

		Diagnostic added = catalog.addCatalogEntry(contract, implementation.getName());
		if (added.getSeverity() == DiagnosticSeverity.ERROR) {
			throw new IllegalStateException("the broker refused " + contract.getName() + ": " + added.getMessage()
					+ " (code " + added.getCode() + ")");
		}

		Registration registration;
		ResourceSet resourceSet = resourceSets.getService();
		try {
			// The publish body is built from copies, and that is not
			// bookkeeping: putting an EObject into a Resource's contents
			// takes it out of the one it was in, so parking the registry's
			// own contract here would quietly empty the local registry of
			// the very thing it exists to hold. Copier keeps the two in
			// step — the copied flavors point at the copied operations,
			// not back at the originals.
			EcoreUtil.Copier copier = new EcoreUtil.Copier();
			ServiceInterface parked = (ServiceInterface) copier.copy(contract);
			ServiceImplementation published = (ServiceImplementation) copier.copy(implementation);
			copier.copyReferences();

			// Parked under its catalog URL so the publish body references
			// the contract rather than carrying a second copy of it.
			String entryUrl = config.broker_url().replaceFirst("/+$", "") + "/catalog/" + contract.getName();
			resourceSet.createResource(URI.createURI(entryUrl)).getContents().add(parked);

			ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
			provider.setName(implementation.getName());
			provider.setVersion(implementation.getVersion());
			provider.getImplementations().add(published);

			registration = client.provider().publish(provider, published);
		} finally {
			// A prototype ResourceSet is a service instance like any
			// other: what is taken has to be given back, or every export
			// leaves one behind for the lifetime of the framework.
			resourceSets.ungetService(resourceSet);
		}
		LOG.info("[DDSR] announced " + contract.getName() + " as " + implementation.getImplementationId());

		AtomicBoolean announced = new AtomicBoolean(true);
		return () -> {
			if (announced.compareAndSet(true, false)) {
				registration.withdraw();
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
