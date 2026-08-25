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

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.xmi.codec.XmiBundleMessageBodyReader;
import org.eclipse.fennec.services.xmi.codec.XmiBundleMessageBodyWriter;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyReader;
import org.eclipse.fennec.services.xmi.codec.XmiMessageBodyWriter;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
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
	}

	@Reference
	private ComponentServiceObjects<ResourceSet> rsObjects;

	@Reference
	private ClientBuilder clientBuilder;

	private URI baseUrl;
	private Client client;

	@Activate
	void activate(Config config) {
		this.baseUrl = URI.create(config.broker_url());
		this.client = clientBuilder
				.register(new XmiMessageBodyReader(rsObjects))
				.register(new XmiMessageBodyWriter(rsObjects))
				.register(new XmiBundleMessageBodyReader(rsObjects))
				.register(new XmiBundleMessageBodyWriter(rsObjects))
				.build();
	}

	@Deactivate
	void deactivate() {
		if (client != null) {
			client.close();
			client = null;
		}
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

	URI baseUrl() {
		return baseUrl;
	}
}
