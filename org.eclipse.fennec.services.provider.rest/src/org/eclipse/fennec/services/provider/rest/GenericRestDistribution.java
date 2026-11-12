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

package org.eclipse.fennec.services.provider.rest;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.common.Contracts;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.client.announce.ContractAnnouncement;
import org.eclipse.fennec.services.client.announce.ServedContract;
import org.eclipse.fennec.services.telemetry.CallTracer;
import org.eclipse.fennec.services.xmi.codec.XmiBundleMessageBodyWriter;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyReader;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyWriter;
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
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jakartars.runtime.JakartarsServiceRuntime;
import org.osgi.service.jakartars.runtime.dto.RuntimeDTO;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jakarta.ws.rs.core.Application;

/**
 * Serves one contract over REST, from its model and nothing else.
 *
 * <p>A factory configuration names the implementation document and the
 * service that implements the contract; this component reads the flavor
 * out of the document and registers itself as a Jakarta REST application
 * under the path the flavor states. The dispatcher inside it routes every
 * request through the same bindings a consumer reads — there is no code
 * per contract, and no generation step.
 *
 * <p>It is registered as an {@link Application} rather than as a resource
 * so each distributed implementation gets its own base path: two contracts
 * of one provider would otherwise collide at the root, and the whiteboard
 * offers exactly this with {@code osgi.jakartars.application.base}.
 *
 * <p>The service is taken through {@link ComponentServiceObjects} so a
 * PROTOTYPE-scoped implementation gets an instance per call rather than
 * one shared across every consumer.
 *
 * <p>With {@code publish} what it serves is also announced, which is
 * what makes a headless provider possible: a configuration and a
 * service, no code — in the FR-P3 order both ways (#204). Announcing is
 * not this component's job, though; it is {@link ContractAnnouncement}'s,
 * which needs a broker client and a catalog and has them as mandatory
 * references. This component says <em>when</em>: it registers a
 * {@link ServedContract} — a condition — whose {@code publish} it
 * raises once a whiteboard lists the application, and derives the
 * announcement's configuration with a target on exactly that. Declarative
 * Services then brings the announcement up after the endpoint answers
 * and takes it down before the endpoint goes.
 *
 * <p>It registers the application itself rather than letting
 * Declarative Services do it, because DS would register it only after
 * {@code activate} and take it away before {@code deactivate} — the
 * wrong side of the announcement both times.
 */
@Designate(ocd = GenericRestDistribution.Config.class, factory = true)
// `ddsr.distribution` says what this service is. Declarative Services
// copies a component's configuration onto the service it registers, so
// this application carries `ddsr.contract=<name>` — the very property a
// transport searches an implementation by. Without the marker the MQTT
// distribution of the same contract finds this application and tries to
// invoke the contract's operations on it (found in harness scenario J).
@Component(
		service = {},
		property = "ddsr.distribution=rest",
		configurationPid = "org.eclipse.fennec.services.provider.rest",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class GenericRestDistribution extends Application {

	private static final Logger LOG = Logger.getLogger(GenericRestDistribution.class.getName());

	@ObjectClassDefinition(
			name = "Generic REST distribution",
			description = "Serves one contract over REST from its published model.")
	public @interface Config {

		@AttributeDefinition(
				name = "Contract",
				description = "Name of the contract this configuration serves.")
		String ddsr_contract() default "";

		@AttributeDefinition(
				name = "Service filter",
				description = "Which service implements the contract, as an LDAP filter — usually "
						+ "(ddsr.contract=<name>). A filter rather than a typed reference on purpose: an "
						+ "implementation registers under whatever interface it has, and a generic transport "
						+ "must not require it to be one this bundle knows.")
		String service_filter();

		@AttributeDefinition(
				name = "Model bundle",
				description = "Symbolic name of the bundle carrying the implementation document. The model "
						+ "travels with the provider that serves it, not with this component.")
		String model_bundle();

		@AttributeDefinition(
				name = "Model entry",
				description = "Path of the document inside that bundle, e.g. model/binding-probe.xmi. It "
						+ "holds the ServiceProvider whose implementation is served, and the contract it "
						+ "references — one document, so every reference in it resolves wherever it is read.")
		String model_entry();

		@AttributeDefinition(
				name = "Publish",
				description = "Whether this component also announces the implementation to the broker. "
						+ "With it, a provider is a configuration and a service and no code at all; "
						+ "without it, something else does the announcing and this only serves.")
		boolean publish() default false;

		@AttributeDefinition(
				name = "Public URL",
				description = "Where consumers reach this deployment, e.g. http://host:9091/payments. Where "
						+ "an instance runs is deployment, not contract, so it is stated here and written "
						+ "into the published flavor — the modelled basePath is appended to it.")
		String public_url() default "";

		@AttributeDefinition(
				name = "Broker URL",
				description = "Only used to name the contract in the publish body: the announcement "
						+ "references <broker>/catalog/<name> instead of carrying a second copy of the "
						+ "contract. The transport to the broker is configured on the client's REST "
						+ "proxy, not here.")
		String broker_url() default "";

		@AttributeDefinition(
				name = "Application base",
				description = "Where the Jakarta REST whiteboard mounts this application. Leave empty to take "
						+ "the flavor's basePath, which is the usual case.")
		String osgi_jakartars_application_base() default "";

		@AttributeDefinition(name = "Token issuer", required = false,
				description = "Mode bearer: the issuer this node's verifier trusts, announced in the "
						+ "published flavor's bearer scheme when the document names none — so a caller "
						+ "knows which token to bring.")
		String issuer() default "";

		@AttributeDefinition(name = "Token audiences", required = false,
				description = "Mode bearer: the audiences a token for this endpoint carries; a caller asks "
						+ "for a token of the first.")
		String[] audiences() default {};

		@AttributeDefinition(name = "JWKS URI", required = false, description = "Mode bearer: where the keys are.")
		String jwks_uri() default "";

		@AttributeDefinition(name = "Scope claim", required = false,
				description = "Mode bearer: where the verifier reads roles from.")
		String scope_claim() default "";

		@AttributeDefinition(name = "Trusted certificate issuers", required = false,
				description = "Mode mtls: the DNs of the authorities this node accepts, announced in the "
						+ "flavor's client-certificate scheme.")
		String[] trusted_issuers() default {};

		@AttributeDefinition(name = "Largest request body", required = false,
				description = "How many bytes of a request body are read before the call is refused "
						+ "with 413. Zero leaves the default of one megabyte.")
		long max_body_bytes() default 0;
	}

	/**
	 * Read through the ResourceSet that carries the services model, so the
	 * document resolves against the registered package rather than against
	 * whatever happens to be on a classpath.
	 */
	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> resourceSets;

	/**
	 * Whoever is watching calls, if anyone is (#126). Optional and
	 * dynamic, and read per call through {@link CallTracer#deferred}:
	 * this component outlives the telemetry bundle in both directions.
	 */
	private final AtomicReference<CallTracer> tracer = new AtomicReference<>();

	@Reference(name = "tracer", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	void setTracer(CallTracer tracer) {
		this.tracer.set(tracer);
	}

	void unsetTracer(CallTracer tracer) {
		// Only the one that is bound: a replacement is set before the one
		// it replaces is unset.
		this.tracer.compareAndSet(tracer, null);
	}

	private RestDispatcher dispatcher;

	/** The implementation, kept in use between calls. */
	private HeldImplementation held;

	private ResourceSet publishedFrom;

	private Config config;

	private ServiceImplementation implementation;

	/** The authentication mode this application states, for the scheme it announces. */
	private Object mode;

	/**
	 * This application's registration, made by this component and not by
	 * DS, so that it can come before the announcement and go after the
	 * withdrawal. Its service id is what the whiteboard's DTOs name it by.
	 */
	private ServiceRegistration<Application> application;

	private final Set<JakartarsServiceRuntime> runtimes = ConcurrentHashMap.newKeySet();

	/** The whiteboard's refusal, said once rather than on every runtime update. */
	private String reportedRefusal;

	/**
	 * Where the announcement's configuration is written. Mandatory: this
	 * component only exists where someone configured it, so Configuration
	 * Admin is there.
	 */
	@Reference
	private ConfigurationAdmin configurations;

	/** What is served, as a condition whoever announces waits for. */
	private ServedContract served;

	private ServiceRegistration<?> servedRegistration;

	/** Whether the condition currently says {@code publish=true}. */
	private boolean publishing;

	/** The announcement's configuration, derived from this one and deleted with it. */
	private Configuration announcement;

	/**
	 * The whiteboards, watched for the one moment that matters here: the
	 * application this component is shows up in a runtime's DTO (#204).
	 * A runtime bumps its {@code service.changecount} whenever its DTOs
	 * change, which DS delivers as an update of this reference.
	 *
	 * <p>Announcing before that is the FR-P3 order turned around — a
	 * consumer would dial an address that answers 404 — and this
	 * component cannot wait for it in {@code activate}: it is the
	 * application, and its deployment only starts once activation is
	 * over and the service is registered.
	 */
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
			updated = "updatedRuntime")
	void bindRuntime(JakartarsServiceRuntime runtime) {
		runtimes.add(runtime);
		reconsider();
	}

	void updatedRuntime(JakartarsServiceRuntime runtime) {
		reconsider();
	}

	void unbindRuntime(JakartarsServiceRuntime runtime) {
		runtimes.remove(runtime);
		reconsider();
	}

	/** Where this application stands in the whiteboards, by its own service id. */
	private Deployment deployment() {
		ServiceReference<Application> self;
		try {
			self = application == null ? null : application.getReference();
		} catch (IllegalStateException unregistered) {
			self = null;
		}
		if (self == null) {
			return Deployment.PENDING;
		}
		List<RuntimeDTO> dtos = new ArrayList<>();
		for (JakartarsServiceRuntime runtime : runtimes) {
			dtos.add(runtime.getRuntimeDTO());
		}
		return Deployment.of((Long) self.getProperty(Constants.SERVICE_ID), dtos);
	}

	@Activate
	void activate(ComponentContext componentContext, BundleContext context, Config config) {
		Object self = componentContext.getProperties().get("component.id");
		// The ResourceSet is held for as long as the model is: releasing
		// it while still reading the documents it loaded would pull the
		// ground out from under the dispatcher.
		this.publishedFrom = resourceSets.getService();
		ServiceImplementation implementation = load(context, config, publishedFrom);
		RestFlavor flavor = restFlavorOf(implementation);
		String contract = implementation.getServiceInterfaces().get(0).getName();
		// A contract may say a slot is a stream; this distribution answers
		// a request once. Refused here, by name, instead of on a call.
		Contracts.requireNoStreams(implementation.getServiceInterfaces().get(0), "the generic REST distribution");

		// The mode comes with the configuration — Declarative Services puts
		// it on this application too, together with the selector that keeps
		// a bearer application down without a verifier.
		boolean enforce = ServedSecurity.enforcedByConfiguration(contract, flavor,
				componentContext.getProperties().get(ServedSecurity.AUTH));
		HeldImplementation holding = new HeldImplementation(context);
		this.held = holding;
		this.dispatcher = new RestDispatcher(flavor,
				() -> holding.holding(implementation(context, config.service_filter(), self)), contract,
				resourceSets,
				CallTracer.deferred(tracer::get), enforce, config.max_body_bytes());
		LOG.info("[DDSR] serving " + contract + " generically at " + flavor.getBasePath());

		String pid = String.valueOf(componentContext.getProperties().get(Constants.SERVICE_PID));
		// Under the same monitor reconsider takes: a binding on another
		// thread must either see all of these or none.
		synchronized (this) {
			this.config = config;
			this.implementation = implementation;
			this.mode = componentContext.getProperties().get(ServedSecurity.AUTH);
			// The condition first and saying "not yet": whoever announces
			// can only bind it once the whiteboard has listed what is
			// registered next.
			this.served = new ServedContract(pid, config.ddsr_contract() + "-distribution", config.broker_url(),
					implementation, publishedFrom, () -> deployed(config, implementation));
			this.servedRegistration = context.registerService(ServedContract.serviceTypes(), served,
					served.properties(false));
			// Registered under the monitor, so a runtime update that lists
			// it cannot slip in before the field is set and be missed.
			this.application = context.registerService(Application.class, this,
					serviceProperties(componentContext.getProperties()));
		}
		if (config.publish()) {
			this.announcement = announcementFor(pid);
		}
		reconsider();
	}

	/** The configuration of the component that announces this contract. */
	private Configuration announcementFor(String pid) {
		try {
			return ServedContract.announcementFor(configurations, pid);
		} catch (IOException | RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] " + config.ddsr_contract() + " is served but cannot be announced: "
					+ "its announcement could not be configured", failure);
			return null;
		}
	}

	/**
	 * What only the deployment knows, written into the flavor before it
	 * is announced: where this instance is reachable, and how a caller
	 * proves itself where the document cannot say it.
	 */
	private String deployed(Config config, ServiceImplementation implementation) {
		RestFlavor flavor = restFlavorOf(implementation);
		URI url = URI.createURI(config.public_url());
		String mountPath = flavor.getBasePath() != null ? flavor.getBasePath() : "";
		flavor.setHost(url.scheme() + "://" + url.authority());
		flavor.setBasePath((url.path() != null ? url.path() : "") + mountPath);
		ServedSecurity.announceScheme(implementation.getServiceInterfaces().get(0).getName(), flavor, mode,
				config.issuer(), config.audiences(), config.jwks_uri(), config.scope_claim(),
				config.trusted_issuers());
		return "at " + flavor.getHost() + flavor.getBasePath();
	}

	/**
	 * What DS would have put on the service: the configuration and the
	 * component's own properties, which is where the whiteboard reads the
	 * base, the name, the mode and the extension selector from — minus
	 * what only the framework may set on a registration.
	 */
	static Dictionary<String, Object> serviceProperties(Dictionary<String, Object> component) {
		Dictionary<String, Object> properties = new Hashtable<>();
		for (Enumeration<String> keys = component.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement();
			if (key.startsWith(".") || FRAMEWORK_PROPERTIES.contains(key)) {
				continue;
			}
			properties.put(key, component.get(key));
		}
		return properties;
	}

	private static final Set<String> FRAMEWORK_PROPERTIES = Set.of(Constants.OBJECTCLASS, Constants.SERVICE_ID,
			Constants.SERVICE_BUNDLEID, Constants.SERVICE_SCOPE);

	/**
	 * Say whether the contract is to be announced now: asked to, and
	 * served. Called from activation and whenever a whiteboard changes,
	 * because the deployment is the one condition that changes by
	 * itself — served first, then told (FR-P3, #204). The answer is a
	 * property of the condition, so a change of it is a service event
	 * and the announcement follows without being called.
	 */
	private void reconsider() {
		ServiceRegistration<?> condition;
		Dictionary<String, Object> properties;
		synchronized (this) {
			if (servedRegistration == null || config == null || !config.publish()) {
				return;
			}
			Deployment deployment = deployment();
			if (deployment.state() == Deployment.State.FAILED && !deployment.reason().equals(reportedRefusal)) {
				reportedRefusal = deployment.reason();
				LOG.warning("[DDSR] the whiteboard refused " + config.ddsr_contract() + " ("
						+ deployment.reason() + ") — not announced");
			}
			boolean publish = deployment.deployed();
			if (publish == publishing) {
				return;
			}
			publishing = publish;
			condition = servedRegistration;
			properties = served.properties(publish);
		}
		// Outside the monitor: the change is a service event, delivered
		// on this thread, and the announcement it activates calls the
		// broker.
		try {
			condition.setProperties(properties);
		} catch (IllegalStateException unregistered) {
			// on the way down
		}
	}

	@Deactivate
	void deactivate() {
		// FR-P3 on the way down: the condition goes first, which takes the
		// announcement down with it — consumers hear of the withdrawal
		// while the endpoint still answers, and only then does it go.
		ServiceRegistration<?> condition;
		synchronized (this) {
			condition = servedRegistration;
			servedRegistration = null;
			publishing = false;
		}
		if (condition != null) {
			try {
				condition.unregister();
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
		synchronized (this) {
			if (application != null) {
				try {
					application.unregister();
				} catch (IllegalStateException alreadyGone) {
					// the framework is on its way out and took it already
				}
				application = null;
			}
		}
		if (held != null) {
			// After the application is gone: no call is on its way to the
			// implementation any more.
			held.release();
			held = null;
		}
		if (publishedFrom != null) {
			resourceSets.ungetService(publishedFrom);
			publishedFrom = null;
		}
	}


	/**
	 * The service implementing this contract, looked up per call so a
	 * provider that comes and goes is followed without bookkeeping.
	 * Through {@link ServiceObjects} so a PROTOTYPE-scoped implementation
	 * yields an instance per call instead of one shared across consumers.
	 *
	 * <p>By filter rather than by a typed reference on purpose: an
	 * implementation registers under whatever interface it has, and a
	 * generic transport must not require it to be one this bundle knows.
	 */
	@SuppressWarnings("unchecked")
	private static ServiceObjects<Object> implementation(BundleContext context, String serviceFilter, Object self) {
		try {
			ServiceReference<?>[] candidates = context.getServiceReferences((String) null, serviceFilter);
			if (candidates == null) {
				return null;
			}
			ServiceReference<?> chosen = someoneElse(candidates, self);
			return chosen == null ? null : (ServiceObjects<Object>) context.getServiceObjects(chosen);
		} catch (InvalidSyntaxException malformed) {
			throw new IllegalStateException("service filter is not a filter: " + serviceFilter, malformed);
		}
	}


	/**
	 * The first candidate that is not this component itself.
	 *
	 * <p>Declarative Services propagates configuration properties onto
	 * the service it registers, so a configuration that names the
	 * contract makes this component match the very filter it uses to
	 * look for an implementation of that contract. Left alone it would
	 * find itself and try to invoke the contract's operations on this
	 * class — which fails with a message about a missing method and
	 * tells nobody what actually went wrong.
	 */
	static ServiceReference<?> someoneElse(ServiceReference<?>[] candidates, Object self) {
		if (candidates == null) {
			return null;
		}
		for (ServiceReference<?> candidate : candidates) {
			// Not this component, and not another transport of the same
			// contract either: a distribution serves an implementation,
			// it is not one.
			if (candidate.getProperty("ddsr.distribution") != null) {
				continue;
			}
			if (self == null || !self.equals(candidate.getProperty("component.id"))) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * The implementation document, from the bundle that owns it. The model
	 * stays where the provider keeps it — this component only needs to be
	 * told which bundle and which entry.
	 */
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
		Resource document = resourceSet.getResource(URI.createURI(entry.toString()), true);
		ServiceProvider provider = (ServiceProvider) document.getContents().get(0);
		return provider.getImplementations().get(0);
	}

	/**
	 * The dispatcher, and the XMI codec it needs.
	 *
	 * <p>The codec's providers are registered as whiteboard extensions
	 * elsewhere, which attaches them to the default application — not to
	 * this one. An application carries its own, so a contract whose
	 * operations take or return a model is served here the same way the
	 * hand-written broker endpoints serve theirs.
	 */
	@Override
	public Set<Object> getSingletons() {
		if (dispatcher == null) {
			return Set.of();
		}
		// The bundle writer is here because an answer may need siblings
		// (#88): a value that references contracts nobody else carries
		// travels with them, as one multi-root document. Every
		// application brings its own providers — a whiteboard extension
		// is never asked when an application names its own, which is
		// what made the lookup a hand-written resource until now.
		return Set.of(dispatcher,
				new XmiMessageBodyReader(resourceSets),
				new XmiMessageBodyWriter(resourceSets),
				new XmiBundleMessageBodyWriter(resourceSets));
	}

	private static RestFlavor restFlavorOf(ServiceImplementation implementation) {
		for (ServiceFlavor flavor : implementation.getFlavors()) {
			if (flavor instanceof RestFlavor rest) {
				return rest;
			}
		}
		throw new IllegalStateException("implementation " + implementation.getName()
				+ " has no RestFlavor — nothing to serve over REST");
	}
}
