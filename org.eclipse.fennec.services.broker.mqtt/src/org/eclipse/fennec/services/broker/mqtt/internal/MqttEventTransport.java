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

package org.eclipse.fennec.services.broker.mqtt.internal;

import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.broker.core.EventSink;
import org.eclipse.fennec.services.messaging.LinkSettings;
import org.eclipse.fennec.services.messaging.MessagingLink;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Owns the MQTT connection and registers {@link MqttEventSink} as an
 * {@link EventSink}, which the broker's whiteboard then picks up.
 * <p>
 * Registered with {@code configurationPolicy = REQUIRE} deliberately:
 * an MQTT broker URL is deployment-specific, so this transport only
 * starts where someone configured it. Nothing else has to know it
 * exists.
 */
// Registers its EventSink itself rather than letting DS do it, and the
// reason is the container (#156): a deployable image ships this
// configuration so that MQTT is one environment variable away, and a
// deployment that does not want MQTT leaves the variable unset. A
// component that registered the sink regardless would offer the broker
// a transport that cannot deliver; one that failed activation would
// make every REST-only container log an error about a transport nobody
// asked for. So: no broker URL, no sink, one line in the log.
@Component(
		configurationPid = "org.eclipse.fennec.services.broker.mqtt",
		configurationPolicy = org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE)
@Designate(ocd = MqttEventTransport.Config.class)
public final class MqttEventTransport implements EventSink {

	private static final Logger LOG = Logger.getLogger(MqttEventTransport.class.getName());

	@ObjectClassDefinition(name = "DDSR Broker MQTT Event Transport",
			description = "Publishes lifecycle events to MQTT. Requires configuration to start.")
	public @interface Config {

		@AttributeDefinition(name = "MQTT broker URL",
				description = "e.g. tcp://localhost:1883. Empty means this broker publishes no MQTT "
						+ "events at all — the deployment did not ask for them.")
		String broker_url() default "tcp://localhost:1883";

		@AttributeDefinition(name = "Topic prefix",
				description = "Events are published to <prefix>/<interface>.")
		String topic_prefix() default "ddsr/events";

		@AttributeDefinition(name = "Event source",
				description = "The CloudEvents 'source' attribute of every event this transport"
						+ " sends: the context the event happened in, as a URI reference. Together"
						+ " with the event id it is what makes an event identifiable, so a"
						+ " deployment running more than one broker gives each its own.")
		String event_source() default "/fennec/services/broker";

		@AttributeDefinition(name = "Client id",
				description = "MQTT client identifier of the broker-side publisher.")
		String client_id() default "ddsr-broker";

		@AttributeDefinition(name = "QoS",
				description = "0 fits a stream whose consumers re-snapshot on reconnect anyway.")
		int qos() default 0;

		@AttributeDefinition(name = "Publish timeout (ms)",
				description = "How long to wait for the MQTT client to accept an event before"
						+ " treating it as lost (#124). Without a wait, a publish that fails after"
						+ " the call — a dropped connection, the in-flight limit — is never noticed"
						+ " at all. Costs delivery latency, never broker availability: this runs on"
						+ " the broker's delivery thread, not under its lock.")
		long publish_timeout_millis() default 5000;

		@AttributeDefinition(name = "TLS key store", required = false,
				description = "The certificate and key this node shows the MQTT broker (PKCS12), with an "
						+ "ssl:// broker URL. Its CN is the node's name there — the user an ACL is written for.")
		String tls_keystore() default "";

		@AttributeDefinition(name = "TLS key store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the key store and of its key.")
		String tls_keystore_password() default "";

		@AttributeDefinition(name = "TLS trust store", required = false,
				description = "The authorities whose MQTT broker certificate this node believes (PKCS12). "
						+ "Empty: the JVM's.")
		String tls_truststore() default "";

		@AttributeDefinition(name = "TLS trust store password", type = AttributeType.PASSWORD, required = false,
				description = "The password of the trust store.")
		String tls_truststore_password() default "";
	}

	@Reference
	private BrokerLookup broker;

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> rsObjects;

	private final BundleContext context;

	private MessagingLink link;

	private MqttEventSink sink;

	private ServiceRegistration<EventSink> registration;

	private int qos;

	private long publishTimeoutMillis;

	@Activate
	public MqttEventTransport(BundleContext context) {
		this.context = context;
	}

	@Activate
	void activate(Config config) throws Exception {
		if (config.broker_url() == null || config.broker_url().isBlank()) {
			// Configured to do nothing, which is a normal state for a
			// container that speaks REST only. Said once, at INFO,
			// because silence here reads like a broken transport.
			LOG.info("[DDSR-MQTT] no broker configured — this broker publishes its events over SSE only");
			return;
		}
		this.qos = config.qos();
		this.publishTimeoutMillis = config.publish_timeout_millis();
		this.sink = new MqttEventSink(this::send, broker, rsObjects, config.topic_prefix(),
				config.event_source());
		// The node's certificate on an ssl:// connection: its CN is the
		// user the MQTT broker's ACL names (SECURITY.md §7d).
		this.link = MessagingLink.open(context, new LinkSettings(INSTANCE, config.broker_url(),
				config.client_id(), config.tls_keystore(), config.tls_keystore_password(),
				config.tls_truststore(), config.tls_truststore_password()));
		// Registered with the first connection, not before: a sink that is
		// offered before it can deliver is a sink the broker counts on for
		// nothing. It stays registered through a later loss — the link
		// reconnects on its own, and what could not be sent meanwhile is a
		// resync the sink owes its subscribers.
		link.onConnected(again -> register(config));
	}

	/** The messaging instance behind this transport, by name. */
	static final String INSTANCE = "fennec.services.broker.events";

	private synchronized void register(Config config) {
		if (registration != null || link == null) {
			return;
		}
		this.registration = context.registerService(EventSink.class, this, null);
		LOG.info("[DDSR-MQTT] event transport connected to " + config.broker_url()
				+ ", topic prefix " + config.topic_prefix());
	}

	private void send(String topic, byte[] payload) throws Exception {
		MessagingLink current = link;
		if (current == null) {
			throw new IllegalStateException("the event transport is closed");
		}
		// Not retained: an event describes a transition, not a state. A
		// late subscriber must not be told about a registration that has
		// long been withdrawn — it pulls a snapshot instead
		// (FR-Sync-Reconnect).
		//
		// Waited on, not fired and forgotten. An async publish reports
		// almost nothing: with the connection down or the in-flight limit
		// reached, the failure happens after the call returns and used to
		// be invisible to everyone, broker and consumers alike. Waiting
		// turns it into an exception the sink can act on, and it is
		// affordable because this thread is the delivery thread (#124).
		current.publish(topic, payload, qos, publishTimeoutMillis);
	}

	@Override
	public void publish(ServiceEvent event) {
		MqttEventSink current = sink;
		if (current != null) {
			current.publish(event);
		}
	}

	@Deactivate
	synchronized void deactivate() {
		if (registration != null) {
			try {
				registration.unregister();
			} catch (IllegalStateException alreadyGone) {
				// The framework got there first.
			}
			registration = null;
		}
		sink = null;
		if (link != null) {
			link.close();
			link = null;
		}
	}
}
