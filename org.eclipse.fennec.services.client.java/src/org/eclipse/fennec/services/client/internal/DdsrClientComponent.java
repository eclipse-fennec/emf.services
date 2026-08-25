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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.client.DdsrConsumer;
import org.eclipse.fennec.services.client.DdsrProvider;
import org.eclipse.fennec.services.FlavorKind;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi DS adapter for {@link DdsrClientImpl}. References the broker
 * role services ({@link BrokerImplementations}, {@link BrokerLookup})
 * — whichever flavor bundle (client.rest, future client.mqtt, or an
 * in-process broker.core) provides them is what the SDK ends up
 * talking to.
 *
 * <p>No broker URL is configured here on purpose. The wire endpoint is
 * owned by the active flavor bundle (e.g. {@code RestTransport} in
 * client.rest); URLs for individual operations come from the
 * {@code RestFlavor.host} + {@code basePath} the broker self-publishes.
 */
@Component(
		service = DdsrClient.class,
		configurationPid = "org.eclipse.fennec.services.client",
		immediate = true)
@Designate(ocd = DdsrClientComponent.Config.class)
public final class DdsrClientComponent implements DdsrClient {

	private static final Logger LOG = Logger.getLogger(DdsrClientComponent.class.getName());

	@ObjectClassDefinition(name = "DDSR Java Client", description = "Transport-agnostic DDSR client SDK")
	public @interface Config {

		@AttributeDefinition(
				name = "Supported flavors",
				description = "Comma-separated list of FlavorKind values the consumer can speak (REST, MQTT)")
		String supported_flavors() default "REST";

		@AttributeDefinition(
				name = "Consumer ID",
				description = "Optional symbolic identity attached to lookups for auditing",
				required = false)
		String consumer_id() default "";
	}

	// Target the REST-flavor proxies explicitly. broker.core's embedded
	// broker also registers BrokerImplementations / BrokerLookup with
	// no transport property — without this filter DS would non-
	// deterministically bind either side, defeating the point of the
	// flavor split.
	/**
	 * Optional: without an event transport the client works exactly as
	 * before, it just does not notify. Which transport shows up here —
	 * SSE today, MQTT later — is none of the SDK's business.
	 */
	private volatile EventSource eventSource;

	/**
	 * Bind method rather than field injection, because the arrival of a
	 * transport has to be acted on: a listener may already be registered
	 * and waiting. Registration and transport activation are independent
	 * components, so either order happens — and it did, which is how this
	 * was found.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC,
			policyOption = ReferencePolicyOption.GREEDY)
	void bindEventSource(EventSource source) {
		this.eventSource = source;
		DdsrClientImpl current = delegate;
		if (current != null) {
			current.transportAvailable();
		}
	}

	void unbindEventSource(EventSource source) {
		if (this.eventSource == source) {
			this.eventSource = null;
		}
	}

	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerImplementations implementations;

	@Reference(target = "(ddsr.broker.transport=rest)")
	private BrokerLookup lookup;

	private DdsrClientImpl delegate;

	@Activate
	void activate(Config config) {
		try {
			List<FlavorKind> flavors = parseFlavors(config.supported_flavors());
			String consumerId = config.consumer_id() == null || config.consumer_id().isBlank()
					? null
					: config.consumer_id();
			// Indirection, not the value: the reference is optional and
			// dynamic, so reading it once at activation would freeze
			// whatever happened to be bound at that moment — the same
			// mistake the broker side avoids with its fan-out.
			this.delegate = new DdsrClientImpl(implementations, lookup, flavors, consumerId,
					this::openEventStream);
			LOG.info("[DDSR-Client] activated, flavors=" + flavors);
		} catch (Throwable t) {
			LOG.log(Level.WARNING, "[DDSR-Client] activation FAILED", t);
			throw t;
		}
	}

	/**
	 * Opens the stream on whichever event transport is bound right now.
	 * Returns {@code null} when none is — the caller then knows it has no
	 * subscription and can try again later, instead of holding a handle
	 * that never delivers anything.
	 */
	private AutoCloseable openEventStream(EventSource.Handler handler) {
		EventSource current = eventSource;
		if (current == null) {
			LOG.info("[DDSR-Client] no event transport bound yet — will connect when one appears");
			return null;
		}
		return current.open(handler);
	}

	@Deactivate
	void deactivate() {
		if (delegate != null) {
			delegate.close();
			delegate = null;
		}
	}

	private static List<FlavorKind> parseFlavors(String csv) {
		List<FlavorKind> kinds = new ArrayList<>();
		if (csv == null || csv.isBlank()) {
			return kinds;
		}
		for (String s : csv.split(",")) {
			String t = s.trim();
			if (t.isEmpty()) {
				continue;
			}
			FlavorKind k = FlavorKind.getByName(t);
			if (k != null) {
				kinds.add(k);
			}
		}
		return kinds;
	}

	@Override
	public DdsrProvider provider() {
		return delegate.provider();
	}

	@Override
	public DdsrConsumer consumer() {
		return delegate.consumer();
	}

	@Override
	public void close() {
		delegate.close();
	}
}
