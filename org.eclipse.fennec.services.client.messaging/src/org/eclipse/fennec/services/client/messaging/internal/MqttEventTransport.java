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

package org.eclipse.fennec.services.client.messaging.internal;

import java.util.Hashtable;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.services.client.EventSource;
import org.eclipse.fennec.services.messaging.Channels;
import org.eclipse.fennec.services.messaging.LinkSettings;
import org.eclipse.fennec.services.messaging.MessagingLink;
import org.gecko.messaging.api.annotations.RequireMessaging;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Owns the consumer-side MQTT connection and registers
 * {@link MqttEventSource} as an {@link EventSource}, which the SDK then
 * binds like any other transport.
 * <p>
 * {@code configurationPolicy = REQUIRE}: a broker URL is
 * deployment-specific, so this transport only starts where someone
 * configured it. A client with both this and the SSE source installed
 * has two event sources, and which one it uses is said in the client's
 * own configuration — {@code eventSource.target} against the
 * {@code ddsr.event.transport} property below. Not by ranking: a
 * deployment that configures a client is describing the setup it
 * expects, not entering a contest.
 */
// Registers itself rather than letting DS do it, because what it is
// depends on its configuration (#138): the scheme of the broker URL
// says whether these are events over MQTT or over AMQP, and
// ddsr.event.transport — what the client's eventSource.target is
// written against — has to say the same. A property fixed on the
// component could only ever say one of them.
@Component(
		service = {},
		configurationPid = "org.eclipse.fennec.services.client.mqtt",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = MqttEventTransport.Config.class)
@RequireMessaging(protocol = "mqtt3")
public final class MqttEventTransport implements EventSource {

	private static final Logger LOG = Logger.getLogger(MqttEventTransport.class.getName());

	@ObjectClassDefinition(name = "DDSR Client MQTT Event Transport",
			description = "Receives lifecycle events over MQTT. Requires configuration to start.")
	public @interface Config {

		@AttributeDefinition(name = "Messaging broker URL",
				description = "e.g. tcp://localhost:1883 for MQTT or amqp://localhost:5672 for AMQP. The "
						+ "scheme says which, and with it what this source is: ddsr.event.transport=mqtt "
						+ "or =amqp.")
		String broker_url() default "tcp://localhost:1883";

		@AttributeDefinition(name = "Topic prefix",
				description = "Must match the broker-side prefix; the source subscribes to <prefix>/#. "
						+ "Given the way MQTT writes it; on AMQP it is the routing key ddsr.events.#.")
		String topic_prefix() default "ddsr/events";

		@AttributeDefinition(name = "User name", required = false,
				description = "Who this node is to a messaging broker that asks for a name and a "
						+ "password. Never in the URL.")
		String username() default "";

		@AttributeDefinition(name = "Password", type = AttributeType.PASSWORD, required = false,
				description = "The password of that user.")
		String password() default "";

		@AttributeDefinition(name = "Client id", required = false,
				description = "MQTT client identifier of this consumer. Empty: ddsr-consumer- and the first "
						+ "characters of the framework's UUID, so that two nodes never share one — a broker "
						+ "disconnects the older of two connections with the same id.")
		String client_id() default "";

		@AttributeDefinition(name = "QoS", description = "Subscription quality of service.")
		int qos() default 0;

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

	@Reference(target = "(emf.name=services)")
	private ComponentServiceObjects<ResourceSet> rsObjects;

	private MessagingLink link;

	private MqttEventSource source;

	private int qos;

	/** The messaging instance behind this transport, by name. */
	static final String INSTANCE = "fennec.services.client.events";

	/** What this connection is to the broker, where its configuration names no client id. */
	static final String CLIENT_ROLE = "ddsr-consumer";

	@Activate
	void activate(BundleContext context, Config config) {
		this.qos = config.qos();
		// The node's certificate on an ssl:// connection: its CN is the
		// user the MQTT broker's ACL names (SECURITY.md §7d).
		this.link = MessagingLink.open(context, new LinkSettings(INSTANCE, config.broker_url(),
				LinkSettings.clientId(context, config.client_id(), CLIENT_ROLE), config.tls_keystore(),
				config.tls_keystore_password(),
				config.tls_truststore(), config.tls_truststore_password())
				.as(config.username(), config.password()));
		Channels channels = Channels.of(config.broker_url());
		this.source = new MqttEventSource(this::subscribe, rsObjects, config.topic_prefix(), channels);
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(TRANSPORT, channels.protocol());
		this.registration = context.registerService(EventSource.class, this, properties);
		LOG.info("[DDSR-MQTT] client transport connecting to " + config.broker_url()
				+ " — events over " + channels.protocol());
	}

	/** What a client's {@code eventSource.target} is written against. */
	static final String TRANSPORT = "ddsr.event.transport";

	private ServiceRegistration<EventSource> registration;

	private AutoCloseable subscribe(String topicFilter, MqttEventSource.MessageListener onMessage,
			Runnable onEstablished) {
		MessagingLink current = link;
		// The subscription first, the listener second: when the link is
		// connected already, the listener is told at once, and by then the
		// subscription has to be there. When it is not, the link restores
		// its subscriptions before it says it is connected — the same
		// order, kept for us.
		AutoCloseable subscription = current.subscribe(topicFilter, qos, onMessage::onMessage);
		AutoCloseable connects = current.onConnected(again -> onEstablished.run());
		return () -> {
			connects.close();
			subscription.close();
		};
	}

	@Override
	public AutoCloseable open(Handler handler) {
		MqttEventSource current = source;
		return current != null ? current.open(handler) : null;
	}

	@Deactivate
	void deactivate() {
		// Withdrawn first: whoever is bound to this source is unbound
		// while it can still close what it opened.
		if (registration != null) {
			try {
				registration.unregister();
			} catch (IllegalStateException alreadyGone) {
				// The framework got there first.
			}
			registration = null;
		}
		source = null;
		if (link != null) {
			link.close();
			link = null;
		}
	}
}
