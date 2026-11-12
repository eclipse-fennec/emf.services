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

package org.eclipse.fennec.services.provider.messaging;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.MessagingFlavor;
import org.eclipse.fennec.services.common.Contracts;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.client.announce.ContractAnnouncement;
import org.eclipse.fennec.services.client.announce.ServedContract;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * A contract served over a messaging broker from its model document,
 * and announced from the same one — the twin of the generic REST
 * distribution (#84), for every flavor that travels over messaging.
 *
 * <p>Until this existed, messaging was the transport you had to write
 * code for: REST could be a factory configuration and a service, while
 * a provider on a broker had to call {@code MessagingDistribution.serve}
 * itself and publish itself. The transports are symmetric on the wire
 * and were not symmetric to operate, which is the kind of difference
 * that decides architectures for the wrong reason.
 *
 * <p>What a deployment states here is what is deployment: which broker
 * to listen on. The channels, the QoS and the operations are the
 * contract's, read from the document that is also announced — nothing
 * says the same thing twice. Which protocol the document's flavor speaks makes no difference to
 * this component.
 *
 * <p>Serving and announcing are two jobs, as they are on the REST
 * side, and this component does the first. With {@code publish} it
 * says when what it serves may be announced — a {@link ServedContract},
 * a condition whose {@code publish} it raises once the broker has the
 * subscriptions — and derives the configuration of the
 * {@link ContractAnnouncement} that does it. With {@code publish} off
 * this listens and tells nobody, which is what a deployment wants when
 * something else owns the announcement.
 */
@Component(configurationPid = "org.eclipse.fennec.services.provider.messaging",
		property = "ddsr.distribution=messaging",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = GenericMessagingDistribution.Config.class, factory = true)
public class GenericMessagingDistribution {

	private static final Logger LOG = Logger.getLogger(GenericMessagingDistribution.class.getName());

	@ObjectClassDefinition(
			name = "Generic messaging distribution",
			description = "Serves one contract over a messaging broker (MQTT, AMQP) from its published model.")
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

		@AttributeDefinition(name = "Messaging broker",
				description = "Where this deployment listens, e.g. tcp://localhost:1883 or "
						+ "amqp://localhost:5672 — a URL of the protocol the document's flavor speaks. Where "
						+ "an instance runs is deployment and not contract, so it is stated here and written "
						+ "into the announced flavor. Empty keeps what the document says.")
		String messaging_broker() default "";

		@AttributeDefinition(name = "Broker URL",
				description = "Only used to name the contract in the publish body: the announcement "
						+ "references <broker>/catalog/<name> instead of carrying a second copy of it.")
		String broker_url() default "";
	}

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	@Reference
	private MessagingDistribution distribution;

	/**
	 * Where the announcement's configuration is written. Mandatory: this
	 * component only exists where someone configured it, so Configuration
	 * Admin is there.
	 */
	@Reference
	private ConfigurationAdmin configurations;

	private MessagingDistribution.Served served;

	private ResourceSet publishedFrom;

	/** What is served, as a condition whoever announces waits for. */
	private ServiceRegistration<?> condition;

	/** The announcement's configuration, derived from this one and deleted with it. */
	private Configuration announcement;

	@Activate
	void activate(ComponentContext componentContext, BundleContext context, Config config) {
		Object self = componentContext.getProperties().get("component.id");
		// Held as long as the model is: releasing it while still reading
		// the documents it loaded would pull the ground out from under
		// the dispatcher.
		this.publishedFrom = resourceSets.getService();
		ServiceImplementation implementation = load(context, config, publishedFrom);
		MessagingFlavor flavor = listenOn(messagingFlavorOf(implementation), config.messaging_broker());
		String contract = implementation.getServiceInterfaces().get(0).getName();
		// A stream a service returns travels over a messaging broker as
		// many answers to one call. One it is handed does not travel yet,
		// and that is said here, by name, instead of on a call.
		Contracts.requireNoStreamArguments(implementation.getServiceInterfaces().get(0),
				"the generic messaging distribution");
		if (flavor.getBrokers().isEmpty()) {
			// Nothing to connect to, said plainly and once. A document
			// that leaves the broker to the deployment is the normal
			// case here, and a deployment that has not named one yet is
			// not an error — it is a component with nothing to do. The
			// alternative, failing activation, turns every launch that
			// merely carries this configuration into a broken one.
			LOG.info("[DDSR] " + contract + " is not served over messaging: no broker configured");
			return;
		}

		this.served = distribution.serve(flavor,
				() -> service(context, config.service_filter(), self), contract);
		LOG.info("[DDSR] serving " + contract + " generically on " + served.channels());

		if (!config.publish()) {
			return;
		}
		String pid = String.valueOf(componentContext.getProperties().get(Constants.SERVICE_PID));
		ServedContract contractServed = new ServedContract(pid, config.ddsr_contract() + "-distribution",
				config.broker_url(), implementation, publishedFrom, () -> "on " + flavor.getBrokers());
		ServiceRegistration<?> registered = context.registerService(ServedContract.serviceTypes(),
				contractServed, contractServed.properties(false));
		synchronized (this) {
			this.condition = registered;
		}
		try {
			this.announcement = ServedContract.announcementFor(configurations, pid);
		} catch (IOException | RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] " + contract + " is served but cannot be announced: its "
					+ "announcement could not be configured", failure);
		}
		// To be announced once the broker has the subscriptions, not once
		// they were asked for: the connection comes up on its own time,
		// and a consumer that hears of this service calls it straight
		// away. Raising the condition is a service event — the
		// announcement follows without being called.
		served.whenListening(() -> {
			ServiceRegistration<?> current;
			synchronized (this) {
				current = condition;
			}
			if (current != null) {
				try {
					current.setProperties(contractServed.properties(true));
				} catch (IllegalStateException unregistered) {
					// on the way down
				}
			}
		});
	}

	@Deactivate
	void deactivate() {
		// The condition goes first, which takes the announcement down
		// with it: a consumer is told before the topics go quiet, which
		// is FR-P3 on this transport.
		ServiceRegistration<?> current;
		synchronized (this) {
			current = condition;
			condition = null;
		}
		if (current != null) {
			try {
				current.unregister();
			} catch (IllegalStateException alreadyGone) {
				// the framework is on its way out and took it already
			}
		}
		if (announcement != null) {
			try {
				announcement.delete();
			} catch (IOException | RuntimeException failure) {
				LOG.log(Level.FINE, "[DDSR] the announcement's configuration could not be deleted", failure);
			}
			announcement = null;
		}
		if (served != null) {
			served.close();
			served = null;
		}
		if (publishedFrom != null) {
			resourceSets.ungetService(publishedFrom);
			publishedFrom = null;
		}
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
				URI.createURI(entry.toString()), true);
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
	static MessagingFlavor listenOn(MessagingFlavor flavor, String configured) {
		if (configured != null && !configured.isBlank()) {
			flavor.getBrokers().clear();
			flavor.getBrokers().add(configured);
		}
		return flavor;
	}

	static MessagingFlavor messagingFlavorOf(ServiceImplementation implementation) {
		for (ServiceFlavor flavor : implementation.getFlavors()) {
			if (flavor instanceof MessagingFlavor messaging) {
				return messaging;
			}
		}
		throw new IllegalStateException("the implementation " + implementation.getName()
				+ " announces no flavor that travels over messaging to serve");
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
