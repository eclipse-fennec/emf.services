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

package org.eclipse.fennec.services.messaging;

import java.util.Hashtable;
import java.util.Locale;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * What a {@link MessagingLink} is opened with: everything its owner
 * knows about the broker, and nothing about the library behind it.
 *
 * @param name               the messaging instance, unique in the
 *                           framework — what its services are selected by
 * @param url                where the broker is, e.g.
 *                           {@code tcp://localhost:1883} or {@code ssl://…}
 * @param clientId           this node's identity on the broker — see
 *                           {@link #clientId(BundleContext, String, String)}
 * @param keystore           the certificate and key this node shows
 *                           (PKCS12), or empty
 * @param keystorePassword   its password, also of the key
 * @param truststore         the authorities whose broker certificate
 *                           this node believes (PKCS12), or empty for the
 *                           JVM's
 * @param truststorePassword its password
 * @param exchange           on AMQP, the topic exchange this link
 *                           publishes to and binds on; empty for the
 *                           broker's own ({@code amq.topic})
 * @param username           who this node is to a broker that asks for
 *                           a name and a password, or empty
 * @param password           the password
 * @param maxBodyBytes       the largest document a message of this link
 *                           carries; zero or less leaves the instance's
 *                           own limit
 * @param mqttVersion        which MQTT this link speaks on an MQTT broker:
 *                           3 (3.1.1, the default — also for 0) or 5.
 *                           Means nothing on AMQP
 */
public record LinkSettings(String name, String url, String clientId, String keystore,
		String keystorePassword, String truststore, String truststorePassword, String exchange,
		String username, String password, long maxBodyBytes, int mqttVersion) {

	/**
	 * What an envelope adds around its document, generously: a handful
	 * of attributes and extensions, none of them long.
	 */
	static final long ENVELOPE_BYTES = 16 * 1024;

	/** MQTT 3.1.1 — what an MQTT link speaks unless it is told otherwise. */
	static final String MQTT3_FACTORY_PID = "org.gecko.messaging.mqtt3";

	/** MQTT 5 (#138). */
	static final String MQTT5_FACTORY_PID = "org.gecko.messaging.mqtt";

	/** AMQP 0-9-1. */
	static final String AMQP_FACTORY_PID = "org.gecko.messaging.amqp";

	/**
	 * The identity a connection shows the broker when its configuration
	 * names none: the role and the first characters of the framework's
	 * UUID.
	 *
	 * <p>A fixed name is a trap. A broker takes the older of two
	 * connections with one client id off the wire, the instance
	 * reconnects, and two nodes that were given the same default throw
	 * each other out for as long as both run. The framework's UUID is
	 * new with every start and one framework holds one connection of a
	 * role, so nothing more random is needed — and nothing is lost by it
	 * changing: a link has no session to come back to.
	 *
	 * <p>A configured id is taken as it is. A deployment whose broker
	 * writes its access rules against client ids names them.
	 *
	 * @param context    the framework this connection is opened in
	 * @param configured what the configuration says, possibly nothing
	 * @param role       what the connection is, e.g. {@code ddsr-consumer}
	 */
	public static String clientId(BundleContext context, String configured, String role) {
		if (configured != null && !configured.isBlank()) {
			return configured;
		}
		String framework = context.getProperty(Constants.FRAMEWORK_UUID);
		if (framework == null || framework.isBlank()) {
			// Every framework since R4.3 has one; this is for a context
			// that is not a framework's, such as a test double.
			framework = UUID.randomUUID().toString();
		}
		return role + "-" + framework.substring(0, Math.min(8, framework.length()));
	}

	/** A link on a broker that has no exchanges to choose from. */
	public LinkSettings(String name, String url, String clientId, String keystore, String keystorePassword,
			String truststore, String truststorePassword) {
		this(name, url, clientId, keystore, keystorePassword, truststore, truststorePassword, null, null,
				null, 0, 0);
	}

	public LinkSettings {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("a link needs a name");
		}
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("a link needs a broker URL");
		}
		if (mqttVersion != 0 && mqttVersion != 3 && mqttVersion != 5) {
			throw new IllegalArgumentException("MQTT is spoken as 3 (3.1.1) or as 5, not as " + mqttVersion);
		}
	}

	/** A link without a certificate of its own. */
	public static LinkSettings of(String name, String url, String clientId) {
		return new LinkSettings(name, url, clientId, null, null, null, null, null, null, null, 0, 0);
	}

	/**
	 * The same link, as this user. Stated beside the URL rather than
	 * inside it, because the URL is what a flavor announces to everyone.
	 */
	public LinkSettings as(String username, String password) {
		return new LinkSettings(name, url, clientId, keystore, keystorePassword, truststore,
				truststorePassword, exchange, username, password, maxBodyBytes, mqttVersion);
	}

	/**
	 * The same link, carrying documents up to this size (#219). Zero or
	 * less leaves the instance's own limit, which is one megabyte per
	 * message.
	 */
	public LinkSettings carrying(long maxBodyBytes) {
		return new LinkSettings(name, url, clientId, keystore, keystorePassword, truststore,
				truststorePassword, exchange, username, password, maxBodyBytes, mqttVersion);
	}

	/**
	 * The same link, speaking this MQTT version to an MQTT broker (#138).
	 *
	 * <p>The URL cannot say it — {@code tcp://} is both — so the owner
	 * of the connection does. Nothing on the wire depends on two nodes
	 * agreeing: a broker carries a message between a 3.1.1 and a 5
	 * client, and what a call needs beyond its payload travels in the
	 * envelope either way. What version 5 adds is the connection's own:
	 * the broker's flow control ({@code receiveMaximum}) and the
	 * protocol's response topic and correlation data beside the
	 * envelope's.
	 *
	 * @param version 3 or 5; 0 leaves the default, 3
	 */
	public LinkSettings speaking(int version) {
		return new LinkSettings(name, url, clientId, keystore, keystorePassword, truststore,
				truststorePassword, exchange, username, password, maxBodyBytes, version);
	}

	/** The same link, on this exchange. */
	public LinkSettings onExchange(String exchange) {
		return new LinkSettings(name, url, clientId, keystore, keystorePassword, truststore,
				truststorePassword, exchange, username, password, maxBodyBytes, mqttVersion);
	}

	/**
	 * Which binding speaks to this broker — said by the scheme of its
	 * URL, the way the URL has always said how to connect.
	 */
	String factoryPid() {
		String scheme = scheme();
		return switch (scheme) {
		case "tcp", "ssl", "ws", "wss" -> mqttVersion == 5 ? MQTT5_FACTORY_PID : MQTT3_FACTORY_PID;
		case "amqp", "amqps" -> AMQP_FACTORY_PID;
		default -> throw new IllegalArgumentException("no messaging binding for a '" + scheme
				+ "' broker: " + url);
		};
	}

	/**
	 * The factory configuration of the messaging instance behind this
	 * link.
	 *
	 * <p>The stores travel only on a TLS URL. A node that carries its
	 * certificate in a configuration shared by several connections is
	 * not wrong to dial a plain broker as well — and the instance would
	 * refuse stores nobody asks for.
	 */
	Hashtable<String, Object> configuration() {
		Hashtable<String, Object> configuration = new Hashtable<>();
		configuration.put("osgi.messaging.name", name);
		configuration.put("uri", url);
		if (clientId != null && !clientId.isBlank()) {
			configuration.put("clientId", clientId);
		}
		String binding = factoryPid();
		if (MQTT3_FACTORY_PID.equals(binding)) {
			// No session to come back to: what a subscriber missed while
			// it was away it reads again (FR-Sync-Reconnect), and the
			// instance restores its subscriptions itself.
			configuration.put("cleanSession", Boolean.TRUE);
		} else if (MQTT5_FACTORY_PID.equals(binding)) {
			// The same statement in the words of MQTT 5.
			configuration.put("cleanStart", Boolean.TRUE);
		} else {
			put(configuration, "exchange", exchange);
		}
		// The instance writes and reads the envelope: every message of
		// this project is a CloudEvent, and in structured mode it is one
		// JSON document on every protocol — which is also what gives a
		// protocol without message properties (MQTT 3.1.1) a reply
		// address and a correlation.
		configuration.put("cloudEvents", "structured");
		if (maxBodyBytes > 0) {
			// The instance bounds the message, the deployment states the
			// document. A document that is not text travels in the
			// envelope as base64 — four bytes for every three — so the
			// message that carries the largest document allowed is that
			// much larger, plus the envelope around it.
			configuration.put("maxMessageSize", (maxBodyBytes + 2) / 3 * 4 + ENVELOPE_BYTES);
		}
		put(configuration, "username", username);
		put(configuration, ".password", password);
		if (secured()) {
			put(configuration, "tls.keystore", keystore);
			put(configuration, ".tls.keystore.password", keystorePassword);
			put(configuration, "tls.truststore", truststore);
			put(configuration, ".tls.truststore.password", truststorePassword);
		}
		return configuration;
	}

	/** Whether this link's URL asks for TLS. */
	boolean secured() {
		String scheme = scheme();
		return scheme.equals("ssl") || scheme.equals("wss") || scheme.equals("amqps");
	}

	private String scheme() {
		int separator = url.indexOf("://");
		return separator < 0 ? "" : url.substring(0, separator).toLowerCase(Locale.ROOT);
	}

	private static void put(Hashtable<String, Object> configuration, String key, String value) {
		if (value != null && !value.isBlank()) {
			configuration.put(key, value);
		}
	}

	/** Never the passwords: this is what a log line prints. */
	@Override
	public String toString() {
		// The MQTT version only where it is not the default: it is the
		// one thing about a connection its URL does not show.
		return name + " → " + address() + (MQTT5_FACTORY_PID.equals(factoryPid()) ? " over MQTT 5" : "");
	}

	/**
	 * The URL without what an AMQP URL may carry before the host: user
	 * and password.
	 */
	public String address() {
		int scheme = url.indexOf("://");
		int at = url.indexOf('@', scheme < 0 ? 0 : scheme + 3);
		int path = url.indexOf('/', scheme < 0 ? 0 : scheme + 3);
		if (scheme < 0 || at < 0 || (path >= 0 && path < at)) {
			return url;
		}
		return url.substring(0, scheme + 3) + url.substring(at + 1);
	}
}
