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

package org.eclipse.fennec.services.provider.mqtt;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.Registration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * A contract served over MQTT from its model document, and announced
 * from the same one — the twin of the generic REST distribution (#84).
 *
 * <p>Until this existed, MQTT was the transport you had to write code
 * for: REST could be a factory configuration and a service, while an
 * MQTT provider had to call {@code MqttDistribution.serve} itself and
 * publish itself. The two transports are symmetric on the wire and
 * were not symmetric to operate, which is the kind of difference that
 * decides architectures for the wrong reason.
 *
 * <p>What a deployment states here is what is deployment: which broker
 * to listen on. The topics, the QoS and the operations are the
 * contract's, read from the document that is also announced — nothing
 * says the same thing twice.
 *
 * <p>Serving and announcing stay separable, as they are on the REST
 * side: with {@code publish} off this listens and tells nobody, which
 * is what a deployment wants when something else owns the
 * announcement.
 */
@Component(configurationPid = "org.eclipse.fennec.services.provider.mqtt",
		property = "ddsr.distribution=mqtt",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = GenericMqttDistribution.Config.class, factory = true)
public class GenericMqttDistribution {

	private static final Logger LOG = Logger.getLogger(GenericMqttDistribution.class.getName());

	@ObjectClassDefinition(
			name = "Generic MQTT distribution",
			description = "Serves one contract over MQTT from its published model.")
	public @interface Config {

		@AttributeDefinition(name = "Contract",
				description = "Name of the contract this configuration serves.")
		String ddsr_contract() default "";

		@AttributeDefinition(name = "Service filter",
				description = "Which service implements the contract, as an LDAP filter — usually "
						+ "(ddsr.contract=<name>). A filter rather than a typed reference, for the reason "
						+ "the REST twin gives: an implementation registers under whatever interface it "
						+ "has, and a generic transport must not require it to be one this bundle knows.")
		String service_filter();

		@AttributeDefinition(name = "Model bundle",
				description = "Symbolic name of the bundle carrying the implementation document.")
		String model_bundle();

		@AttributeDefinition(name = "Model entry",
				description = "Path of the document inside that bundle, e.g. model/payment-mqtt.xmi.")
		String model_entry();

		@AttributeDefinition(name = "Publish",
				description = "Whether this component also announces the implementation to the broker.")
		boolean publish() default false;

		@AttributeDefinition(name = "MQTT broker",
				description = "Where this deployment listens, e.g. tcp://localhost:1883. Where an instance "
						+ "runs is deployment and not contract, so it is stated here and written into the "
						+ "announced flavor. Empty keeps what the document says.")
		String mqtt_broker() default "";

		@AttributeDefinition(name = "Broker URL",
				description = "Only used to name the contract in the publish body: the announcement "
						+ "references <broker>/catalog/<name> instead of carrying a second copy of it.")
		String broker_url() default "";
	}

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	@Reference
	private MqttDistribution distribution;

	private volatile DdsrClient client;

	private volatile BrokerCatalog catalog;

	private MqttDistribution.Served served;

	private Registration registration;

	private ResourceSet publishedFrom;

	private Config config;

	private ServiceImplementation implementation;

	/**
	 * Optional, and bound by method, for the reason the REST twin gives:
	 * serving and announcing are separate jobs, and whichever of the two
	 * services arrives last completes the pair.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL,
			policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.RELUCTANT)
	void setClient(DdsrClient client) {
		this.client = client;
		announceIfReady();
	}

	void unsetClient(DdsrClient client) {
		if (this.client == client) {
			withdraw();
			this.client = null;
		}
	}

	/**
	 * The REST proxy of the catalog explicitly: a deployment that
	 * carries broker.core for the interface alone also has that bundle's
	 * in-process catalog, and announcing into an empty local broker
	 * looks like success and reaches nobody.
	 */
	@Reference(target = "(ddsr.broker.transport=rest)",
			cardinality = ReferenceCardinality.OPTIONAL,
			policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.RELUCTANT)
	void setCatalog(BrokerCatalog catalog) {
		this.catalog = catalog;
		announceIfReady();
	}

	void unsetCatalog(BrokerCatalog catalog) {
		if (this.catalog == catalog) {
			this.catalog = null;
		}
	}

	@Activate
	void activate(ComponentContext componentContext, BundleContext context, Config config) {
		Object self = componentContext.getProperties().get("component.id");
		// Held as long as the model is: releasing it while still reading
		// the documents it loaded would pull the ground out from under
		// the dispatcher.
		this.publishedFrom = resourceSets.getService();
		ServiceImplementation implementation = load(context, config, publishedFrom);
		MqttFlavor flavor = listenOn(mqttFlavorOf(implementation), config.mqtt_broker());
		String contract = implementation.getServiceInterfaces().get(0).getName();
		if (flavor.getBrokers().isEmpty()) {
			// Nothing to connect to, said plainly and once. A document
			// that leaves the broker to the deployment is the normal
			// case here, and a deployment that has not named one yet is
			// not an error — it is a component with nothing to do. The
			// alternative, failing activation, turns every launch that
			// merely carries this configuration into a broken one.
			LOG.info("[DDSR] " + contract + " is not served over MQTT: no broker configured");
			return;
		}

		this.served = distribution.serve(flavor,
				() -> service(context, config.service_filter(), self), contract);
		LOG.info("[DDSR] serving " + contract + " generically on " + served.topics());

		synchronized (this) {
			this.config = config;
			this.implementation = implementation;
		}
		announceIfReady();
	}

	@Deactivate
	void deactivate() {
		// Withdraw first, then stop listening: a consumer is told before
		// the topics go quiet, which is FR-P3 on this transport.
		withdraw();
		if (served != null) {
			served.close();
			served = null;
		}
		if (publishedFrom != null) {
			resourceSets.ungetService(publishedFrom);
			publishedFrom = null;
		}
	}

	private synchronized void announceIfReady() {
		if (registration != null || config == null || !config.publish() || implementation == null) {
			return;
		}
		if (client == null || catalog == null) {
			return;
		}
		announce(config, implementation);
	}

	/**
	 * Announce what this component serves.
	 *
	 * <p>The contract moves into a resource named by its catalog URL
	 * before publishing, so the body references it instead of carrying a
	 * second copy. A failure here leaves the subscription in place: an
	 * unreachable broker is a reason not to be discovered, not a reason
	 * to stop answering the consumers that already found us.
	 */
	private void announce(Config config, ServiceImplementation implementation) {
		try {
			ServiceInterface contract = implementation.getServiceInterfaces().get(0);
			Diagnostic added = catalog.addCatalogEntry(contract, config.ddsr_contract() + "-distribution");
			if (added.getSeverity() == DiagnosticSeverity.ERROR && added.getCode() != 202) {
				throw new IllegalStateException("the broker refused the contract: " + added.getMessage());
			}

			String entryUrl = config.broker_url().replaceFirst("/+$", "") + "/catalog/" + contract.getName();
			publishedFrom.createResource(org.eclipse.emf.common.util.URI.createURI(entryUrl))
					.getContents().add(contract);

			this.registration = client.provider().publish(
					(ServiceProvider) implementation.eContainer(), implementation);
			LOG.info("[DDSR] published " + contract.getName() + " on "
					+ mqttFlavorOf(implementation).getBrokers() + " (contract → " + entryUrl + ")");
		} catch (RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] publishing " + config.ddsr_contract()
					+ " failed — the subscription keeps answering", failure);
		}
	}

	private synchronized void withdraw() {
		if (registration == null) {
			return;
		}
		try {
			registration.withdraw();
		} catch (RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] withdrawing failed", failure);
		}
		registration = null;
	}

	private static ServiceImplementation load(BundleContext context, Config config, ResourceSet resourceSet) {
		Bundle owner = null;
		for (Bundle candidate : context.getBundles()) {
			if (candidate.getSymbolicName().equals(config.model_bundle())) {
				owner = candidate;
				break;
			}
		}
		if (owner == null) {
			throw new IllegalStateException("no bundle " + config.model_bundle() + " to read the model from");
		}
		URL entry = owner.getEntry(config.model_entry());
		if (entry == null) {
			throw new IllegalStateException(config.model_bundle() + " carries no entry " + config.model_entry());
		}
		Resource document = resourceSet.getResource(
				org.eclipse.emf.common.util.URI.createURI(entry.toString()), true);
		ServiceProvider provider = (ServiceProvider) document.getContents().get(0);
		return provider.getImplementations().get(0);
	}

	/**
	 * Where this instance listens.
	 *
	 * <p>Configuration wins over the document, and an empty setting is
	 * not a configuration: a deployment that says nothing listens where
	 * the model says, which is what makes a document that already names
	 * a broker runnable as it is.
	 *
	 * <p>Replaces rather than appends. The list is "the brokers this
	 * implementation is reachable on", and a deployment that has moved
	 * is not also still at the old address.
	 */
	static MqttFlavor listenOn(MqttFlavor flavor, String configured) {
		if (configured != null && !configured.isBlank()) {
			flavor.getBrokers().clear();
			flavor.getBrokers().add(configured);
		}
		return flavor;
	}

	static MqttFlavor mqttFlavorOf(ServiceImplementation implementation) {
		for (ServiceFlavor flavor : implementation.getFlavors()) {
			if (flavor instanceof MqttFlavor mqtt) {
				return mqtt;
			}
		}
		throw new IllegalStateException("the implementation " + implementation.getName()
				+ " announces no MQTT flavor to serve");
	}

	/**
	 * The service implementing this contract, resolved per call so a
	 * provider that comes and goes is followed without bookkeeping, and
	 * never this component itself: DS propagates configuration
	 * properties onto the service it registers, so a configuration that
	 * names the contract makes this component match the very filter it
	 * searches with.
	 */
	private static Object service(BundleContext context, String serviceFilter, Object self) {
		try {
			ServiceReference<?>[] candidates = context.getServiceReferences((String) null, serviceFilter);
			if (candidates == null) {
				return null;
			}
			for (ServiceReference<?> candidate : candidates) {
				// Another transport of the same contract is not an
				// implementation of it: DS copies `ddsr.contract` from a
				// distribution's configuration onto the service it
				// registers, so without this the two generic
				// distributions serve each other.
				if (candidate.getProperty("ddsr.distribution") != null) {
					continue;
				}
				if (self == null || !self.equals(candidate.getProperty("component.id"))) {
					ServiceObjects<?> objects = context.getServiceObjects(candidate);
					return objects == null ? null : objects.getService();
				}
			}
			return null;
		} catch (InvalidSyntaxException malformed) {
			throw new IllegalStateException("service filter is not a filter: " + serviceFilter, malformed);
		}
	}
}
