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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceEventType;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.Registration;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.rsa.spi.EndpointScopes;
import org.eclipse.fennec.services.rsa.spi.OsgiProperties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * Carries endpoint descriptions between frameworks — any of them, not
 * only ours.
 *
 * <p>{@link org.eclipse.fennec.services.rsa.spi.ServiceDiscovery} announces
 * what this implementation exported: a contract and an implementation,
 * the model all the way. A topology manager, however, may hand a
 * discovery an {@link EndpointDescription} that came from anywhere —
 * another vendor's admin, a description read from a bundle's header, one
 * a test wrote by hand. It is a flat map of properties and nothing else,
 * and 122.6 says a discovery has to carry it all the same.
 *
 * <p>So it travels as what it is: one well-known contract,
 * {@code osgi.rsa.endpoint}, whose implementations are endpoints, and
 * whose properties are the description. The registry's lifecycle carries
 * it for free — publish, withdraw, events, heartbeat, the loss of a
 * framework — and a consumer in another language sees these endpoints in
 * the same catalog as everything else.
 *
 * <p>Both directions run through here, which is why the loop has to be
 * cut somewhere: <strong>only endpoints of this framework are
 * published</strong>. One that arrives over the wire is delivered to the
 * listeners here and never announced again, whoever hands it back.
 */
@SuppressWarnings("deprecation")
@Component(service = { EndpointEventListener.class, EndpointListener.class }, immediate = true,
		configurationPid = RestDiscoveryProvider.PID, configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = EndpointEventListener.ENDPOINT_LISTENER_SCOPE + "=(endpoint.id=*)")
public class EndpointBridge implements EndpointEventListener, EndpointListener {

	/**
	 * The contract every foreign endpoint is an implementation of. It has
	 * no operations: what a caller needs to reach the service is in the
	 * description, said the way whoever exported it says it — this
	 * registry is carrying the description, not promising to call it.
	 */
	public static final String ENDPOINT_CONTRACT = "osgi.rsa.endpoint";

	private static final String ENDPOINT_CONTRACT_VERSION = "1.0.0";

	private static final Logger LOG = Logger.getLogger(EndpointBridge.class.getName());

	@Reference
	private DdsrClient client;

	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerCatalog catalog;

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	private volatile String brokerUrl;

	/** The listeners here, with the scopes they asked for. */
	private final Map<EndpointEventListener, List<String>> eventListeners = new ConcurrentHashMap<>();

	private final Map<EndpointListener, List<String>> legacyListeners = new ConcurrentHashMap<>();

	/** What this framework announced, by endpoint id. */
	private final Map<String, Announcements.Announced> announced = new ConcurrentHashMap<>();

	/** What arrived over the wire, by the registry's reference id. */
	private final Map<String, EndpointDescription> arrived = new ConcurrentHashMap<>();

	private String frameworkUuid;

	private Object componentId;

	private AutoCloseable watch;

	// Method injection, and the properties with it: a listener is only
	// half the news — the scope it asked for is the other half, and it
	// changes as listeners come and go (#113).
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addEventListener(EndpointEventListener listener, Map<String, Object> properties) {
		if (someoneElse(properties)) {
			eventListeners.put(listener, EndpointScopes.of(properties));
			replay(listener, EndpointScopes.of(properties));
		}
	}

	void removeEventListener(EndpointEventListener listener) {
		eventListeners.remove(listener);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addLegacyListener(EndpointListener listener, Map<String, Object> properties) {
		if (someoneElse(properties)) {
			legacyListeners.put(listener, EndpointScopes.of(properties));
			replay(listener, EndpointScopes.of(properties));
		}
	}

	void removeLegacyListener(EndpointListener listener) {
		legacyListeners.remove(listener);
	}

	/**
	 * Whether these properties belong to something other than this
	 * component. We are registered as a listener ourselves — to be handed
	 * endpoints — and delivering to ourselves would be a loop with no end.
	 */
	private boolean someoneElse(Map<String, Object> properties) {
		Object id = properties.get("component.id");
		return id == null || !id.equals(componentId);
	}

	@Activate
	void activate(ComponentContext context, BundleContext bundleContext, RestDiscoveryProvider.Config config) {
		this.brokerUrl = config.broker_url();
		this.componentId = context.getProperties().get("component.id");
		this.frameworkUuid = bundleContext.getProperty(Constants.FRAMEWORK_UUID);

		// What is there now, then what changes — a duplicate report is
		// harmless, a missed one is an endpoint nobody ever sees.
		for (ServiceLocator known : client.consumer().find(ENDPOINT_CONTRACT, null)) {
			appeared(referenceIdOf(known), known.implementation());
		}
		watch = client.consumer().addServiceListener(ENDPOINT_CONTRACT, null, this::onWire);
		LOG.info("[DDSR] endpoint bridge ready — carrying foreign endpoints as " + ENDPOINT_CONTRACT);
	}

	/** The broker moved; the next announcement goes to the new one (#107). */
	@Modified
	void modified(RestDiscoveryProvider.Config config) {
		this.brokerUrl = config.broker_url();
	}

	@Deactivate
	void deactivate() {
		close(watch);
		watch = null;
		for (Announcements.Announced announcement : announced.values()) {
			try {
				announcement.close();
			} catch (RuntimeException failure) {
				LOG.log(Level.WARNING, "[DDSR] withdrawing an announced endpoint failed", failure);
			}
		}
		announced.clear();
		arrived.clear();
	}

	// ---------------------------------------------------------------- in

	@Override
	public void endpointChanged(EndpointEvent event, String matchedFilter) {
		EndpointDescription endpoint = event.getEndpoint();
		switch (event.getType()) {
		case EndpointEvent.ADDED, EndpointEvent.MODIFIED -> announce(endpoint);
		case EndpointEvent.REMOVED, EndpointEvent.MODIFIED_ENDMATCH -> unannounce(endpoint);
		default -> LOG.fine(() -> "[DDSR] endpoint event " + event.getType() + " is not one we carry");
		}
	}

	@Override
	public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
		announce(endpoint);
	}

	@Override
	public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
		unannounce(endpoint);
	}

	/**
	 * Tell the world about an endpoint of this framework.
	 *
	 * <p>Only of this framework: an endpoint that arrived over the wire
	 * and is handed back to us is already known everywhere, and
	 * announcing it again would make every framework echo every other.
	 */
	private void announce(EndpointDescription endpoint) {
		if (!ours(endpoint)) {
			LOG.fine(() -> "[DDSR] not announcing " + endpoint.getId() + ": it belongs to framework "
					+ endpoint.getFrameworkUUID());
			return;
		}
		Announcements.Announced known = announced.get(endpoint.getId());
		if (known != null) {
			// A modification, and it has to reach the other side as one:
			// withdrawing and announcing again would tell every consumer
			// the endpoint went away and a new one arrived, and a
			// listener waiting for a change would wait forever. The
			// registry modifies in place when the identity is unchanged.
			modify(endpoint, known);
			return;
		}
		try {
			ServiceInterface contract = endpointContract();
			Diagnostic added = catalog.addCatalogEntry(contract, frameworkUuid);
			if (added.getSeverity() == DiagnosticSeverity.ERROR) {
				throw new IllegalStateException("the broker refused " + ENDPOINT_CONTRACT + ": "
						+ added.getMessage() + " (code " + added.getCode() + ")");
			}

			ServiceImplementation implementation = ServicesFactory.eINSTANCE.createServiceImplementation();
			implementation.setName("endpoint-" + endpoint.getId());
			implementation.setVersion(ENDPOINT_CONTRACT_VERSION);
			implementation.setImplementationId(frameworkUuid + ":" + endpoint.getId());
			implementation.getServiceInterfaces().add(contract);
			implementation.getProperties().addAll(OsgiProperties.toModel(endpoint.getProperties()));

			announced.put(endpoint.getId(), Announcements.publish(client, resourceSets, brokerUrl, contract,
					implementation, "rsa-endpoints-" + frameworkUuid));
			LOG.info("[DDSR] announced foreign-format endpoint " + endpoint.getId() + " "
					+ endpoint.getInterfaces());
		} catch (RuntimeException failure) {
			// A topology manager is calling us; it has nowhere to put an
			// exception, and one endpoint that cannot be announced must
			// not stop the next.
			LOG.log(Level.WARNING, "[DDSR] announcing " + endpoint.getId() + " failed", failure);
		}
	}

	/** Say the same endpoint anew, keeping its identity in the registry. */
	private void modify(EndpointDescription endpoint, Announcements.Announced announcement) {
		try {
			ServiceImplementation published = announcement.implementation();
			published.getProperties().clear();
			published.getProperties().addAll(OsgiProperties.toModel(endpoint.getProperties()));
			announcement.update();
			LOG.info("[DDSR] modified endpoint " + endpoint.getId());
		} catch (RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] modifying " + endpoint.getId() + " failed", failure);
		}
	}

	private void unannounce(EndpointDescription endpoint) {
		Announcements.Announced announcement = announced.remove(endpoint.getId());
		if (announcement == null) {
			return;
		}
		try {
			announcement.close();
			LOG.info("[DDSR] withdrew endpoint " + endpoint.getId());
		} catch (RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] withdrawing " + endpoint.getId() + " failed", failure);
		}
	}

	private boolean ours(EndpointDescription endpoint) {
		return frameworkUuid != null && frameworkUuid.equals(endpoint.getFrameworkUUID());
	}

	/** The contract foreign endpoints are implementations of. */
	private static ServiceInterface endpointContract() {
		ServiceInterface contract = ServicesFactory.eINSTANCE.createServiceInterface();
		contract.setName(ENDPOINT_CONTRACT);
		contract.setVersion(ENDPOINT_CONTRACT_VERSION);
		return contract;
	}

	// --------------------------------------------------------------- out

	private void onWire(ServiceEvent event) {
		ServiceReference reference = event.getReference();
		String id = reference == null ? null : reference.getId();
		if (id == null) {
			return;
		}
		ServiceEventType type = event.getType();
		if (type != ServiceEventType.REGISTERED && type != ServiceEventType.MODIFIED) {
			gone(id);
			return;
		}
		ServiceImplementation implementation = implementationOf(reference);
		if (implementation == null) {
			implementation = client.consumer().find(ENDPOINT_CONTRACT, null).stream()
					.filter(locator -> id.equals(referenceIdOf(locator)))
					.map(ServiceLocator::implementation)
					.findFirst()
					.orElse(null);
		}
		if (implementation == null) {
			LOG.fine(() -> "[DDSR] " + type + " for " + id + " carried no endpoint — ignored");
			return;
		}
		appeared(id, implementation);
	}

	private void appeared(String referenceId, ServiceImplementation implementation) {
		EndpointDescription endpoint = endpointOf(implementation);
		if (endpoint == null || ours(endpoint)) {
			// Our own, come back to us through the registry. The
			// framework it belongs to already knows.
			return;
		}
		EndpointDescription known = arrived.put(referenceId, endpoint);
		int type = known == null ? EndpointEvent.ADDED : EndpointEvent.MODIFIED;
		deliver(new EndpointEvent(type, endpoint));
	}

	private void gone(String referenceId) {
		EndpointDescription endpoint = arrived.remove(referenceId);
		if (endpoint != null) {
			deliver(new EndpointEvent(EndpointEvent.REMOVED, endpoint));
		}
	}

	/** An endpoint description, as the registry carried it. */
	private static EndpointDescription endpointOf(ServiceImplementation implementation) {
		Map<String, Object> properties = OsgiProperties.toOsgi(implementation.getProperties());
		if (!properties.containsKey(RemoteConstants.ENDPOINT_ID)) {
			return null;
		}
		try {
			return new EndpointDescription(properties);
		} catch (IllegalArgumentException incomplete) {
			// Someone published something under our contract that is not
			// an endpoint description. Say so once and move on.
			LOG.log(Level.WARNING, "[DDSR] " + implementation.getImplementationId()
					+ " is not a usable endpoint description", incomplete);
			return null;
		}
	}

	/**
	 * Hand the event to every listener whose scope matches, naming the
	 * filter that did — the listener is told why it was called, which is
	 * what the specification asks of a discovery.
	 */
	private void deliver(EndpointEvent event) {
		EndpointDescription endpoint = event.getEndpoint();
		eventListeners.forEach((listener, scope) -> {
			String matched = EndpointScopes.matching(scope, endpoint);
			if (matched != null) {
				safely(() -> listener.endpointChanged(event, matched), listener);
			}
		});
		legacyListeners.forEach((listener, scope) -> {
			String matched = EndpointScopes.matching(scope, endpoint);
			if (matched == null) {
				return;
			}
			safely(() -> {
				if (event.getType() == EndpointEvent.REMOVED
						|| event.getType() == EndpointEvent.MODIFIED_ENDMATCH) {
					listener.endpointRemoved(endpoint, matched);
				} else {
					listener.endpointAdded(endpoint, matched);
				}
			}, listener);
		});
	}

	/** Everything already known, for a listener that just showed up. */
	private void replay(Object listener, List<String> scope) {
		for (EndpointDescription endpoint : arrived.values()) {
			String matched = EndpointScopes.matching(scope, endpoint);
			if (matched == null) {
				continue;
			}
			EndpointEvent event = new EndpointEvent(EndpointEvent.ADDED, endpoint);
			if (listener instanceof EndpointEventListener events) {
				safely(() -> events.endpointChanged(event, matched), listener);
			} else if (listener instanceof EndpointListener legacy) {
				safely(() -> legacy.endpointAdded(endpoint, matched), listener);
			}
		}
	}



	private static void safely(Runnable delivery, Object listener) {
		try {
			delivery.run();
		} catch (RuntimeException failure) {
			// One listener that throws must not stop the others hearing.
			LOG.log(Level.WARNING, "[DDSR] " + listener.getClass().getName() + " threw on an endpoint event",
					failure);
		}
	}

	private static ServiceImplementation implementationOf(ServiceReference reference) {
		return reference.getProvider() == null || reference.getProvider().getImplementations().isEmpty()
				? null
				: reference.getProvider().getImplementations().get(0);
	}

	private static String referenceIdOf(ServiceLocator locator) {
		return locator.reference() == null ? null : locator.reference().getId();
	}

	private static void close(AutoCloseable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception failure) {
			LOG.log(Level.WARNING, "[DDSR] closing the endpoint watch failed", failure);
		}
	}
}
