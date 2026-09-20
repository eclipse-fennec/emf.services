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

package org.eclipse.fennec.services.client.rest.internal;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.common.ClientOrigin;
import org.eclipse.fennec.services.xmi.codec.XmiBundleMessageBodyReader;
import org.eclipse.fennec.services.xmi.codec.XmiBundleMessageBodyWriter;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyReader;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyWriter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;

/**
 * Shared Jakarta REST {@link Client} for the three proxy components.
 * The {@link ClientBuilder} comes from the OSGi service registry —
 * org.eclipse.osgitech.rest.client registers it backed by Jersey, so
 * we don't have to fiddle with the thread-context classloader to make
 * the JAX-RS SPI lookup work inside OSGi.
 *
 * <p>The four shared MBR/MBW providers are registered on the client
 * as plain instances. They reuse the same classes the broker side
 * uses (DS-registered there as Jakartars Whiteboard extensions); the
 * OSGi-side DS bindings of those classes are orthogonal to the
 * instance-level registration we do here.
 */
@Component(
		service = RestTransport.class,
		configurationPid = "org.eclipse.fennec.services.client.rest",
		immediate = true)
@Designate(ocd = RestTransport.Config.class)
public final class RestTransport {

	@ObjectClassDefinition(name = "DDSR REST Client Transport",
			description = "HTTP target for the BrokerCatalog/Implementations/Lookup REST proxies")
	public @interface Config {

		@AttributeDefinition(
				name = "Broker URL",
				description = "Base URL of the broker REST endpoint, e.g. http://localhost:8887/ddsr/rest")
		String broker_url() default "http://localhost:8887/ddsr/rest";

		@AttributeDefinition(
				name = "Connect timeout (ms)",
				description = "TCP connect timeout for broker and service calls (#59). 0 = transport default.",
				required = false)
		long connect_timeout_millis() default 3000;

		@AttributeDefinition(
				name = "Read timeout (ms)",
				description = "Time to wait for a response after the request was sent (#59). A registered "
						+ "provider that no longer answers surfaces as a transport failure instead of a hang. "
						+ "0 = transport default.",
				required = false)
		long read_timeout_millis() default 10000;

		@AttributeDefinition(
				name = "Origin label",
				description = "Which system this is, for the X-DDSR-Origin header every call carries (#125): "
						+ "a deployment name such as payments-prod-eu. The other half of the origin is the "
						+ "framework UUID and is taken from the runtime. Deliberately not a host name — name "
						+ "the system, not the machine.",
				required = false)
		String origin_label() default "";
	}

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> rsObjects;

	@Reference
	private ClientBuilder clientBuilder;

	private static final Logger LOG = Logger.getLogger(RestTransport.class.getName());

	private URI baseUrl;
	private Client client;
	private Config config;

	/**
	 * Who this runtime is, for every call it makes (#125). Fixed at
	 * activation: the framework half cannot change while the framework
	 * lives, and a label that changes mid-session would make the audit
	 * trail of one session read like two.
	 */
	private ClientOrigin origin;

	@Activate
	void activate(BundleContext context, Config config) {
		this.config = config;
		this.baseUrl = URI.create(config.broker_url());
		this.origin = ClientOrigin.of(config.origin_label(),
				context.getProperty(ClientOrigin.FRAMEWORK_UUID));
		this.client = build(config);
		LOG.info("[DDSR-Client] origin " + origin.token());
	}

	/**
	 * A changed configuration is taken, not died of.
	 *
	 * <p>This transport sits under every broker proxy, which sit under
	 * the client, which sits under discovery. Letting the component
	 * runtime destroy and rebuild it takes that whole column down with
	 * it — and Configuration Admin delivers the same configuration more
	 * than once while a framework starts, so it happened for no reason
	 * at all (#107). An identical configuration now changes nothing.
	 *
	 * <p>Changed timeouts do need a new client; the old one is closed
	 * once the new one is in place, which is no worse than the restart
	 * this method replaces.
	 */
	@Modified
	void modified(Config config) {
		this.baseUrl = URI.create(config.broker_url());
		boolean timeoutsChanged = config.connect_timeout_millis() != this.config.connect_timeout_millis()
				|| config.read_timeout_millis() != this.config.read_timeout_millis();
		// The runtime half stays; only the deployment's name for itself
		// is configuration. The filter reads the field, so a new label
		// takes effect without rebuilding the client.
		this.origin = ClientOrigin.of(config.origin_label(), origin.runtimeId());
		this.config = config;
		if (!timeoutsChanged) {
			return;
		}
		LOG.info("[DDSR-Client] REST transport timeouts changed — building a new client");
		Client previous = this.client;
		this.client = build(config);
		if (previous != null) {
			previous.close();
		}
	}

	private Client build(Config config) {
		if (config.connect_timeout_millis() > 0) {
			clientBuilder.connectTimeout(config.connect_timeout_millis(), TimeUnit.MILLISECONDS);
		}
		if (config.read_timeout_millis() > 0) {
			clientBuilder.readTimeout(config.read_timeout_millis(), TimeUnit.MILLISECONDS);
		}
		return clientBuilder
				.register(new XmiMessageBodyReader(rsObjects))
				.register(new XmiMessageBodyWriter(rsObjects))
				.register(new XmiBundleMessageBodyReader(rsObjects))
				.register(new XmiBundleMessageBodyWriter(rsObjects))
				// #125 asks for the origin on EVERY call, and this is the
				// only place that is true by construction: a filter on the
				// shared client cannot be forgotten by a new proxy, and a
				// per-call header can. It rides on service invocations too,
				// not only on broker traffic — "which system told which
				// system what" is the question, and the answer is worth
				// nothing if it stops at the registry's door.
				.register((ClientRequestFilter) request ->
						request.getHeaders().putSingle(ClientOrigin.HEADER, origin.token()))
				.build();
	}

	/**
	 * Who this runtime is, as the URI reference a CloudEvents
	 * {@code source} has to be (#101). The same identity the origin
	 * header carries — one answer to "who is calling", worn twice
	 * because the two readers are different: the header is read by this
	 * registry, the attribute by anything that reads CloudEvents.
	 */
	String originReference() {
		return "/consumer/" + (origin != null ? origin.token() : ClientOrigin.ANONYMOUS);
	}

	@Deactivate
	void deactivate() {
		if (client != null) {
			client.close();
			client = null;
		}
	}

	/**
	 * The origin token this client stamps on every request, for the
	 * places that also have to name a requestor in the body of the
	 * protocol rather than only in a header.
	 */
	String originToken() {
		return origin.token();
	}

	WebTarget target() {
		return client.target(baseUrl);
	}

	/** Build a target for an absolute URL (e.g. the one a ServiceLocator
	 *  resolves from a RestFlavor.host + opPath). The configured
	 *  base URL is only relevant for proxy calls against the
	 *  ddsr-broker itself; other services may live on different hosts. */
	WebTarget targetFor(URI absoluteUrl) {
		return client.target(absoluteUrl);
	}

	/**
	 * A target from the endpoint as the flavor states it — path templates
	 * included, which {@link URI} could not carry. The caller closes them
	 * with {@code resolveTemplate}.
	 */
	WebTarget targetFor(String absoluteUrl) {
		return client.target(absoluteUrl);
	}

	URI baseUrl() {
		return baseUrl;
	}
}
