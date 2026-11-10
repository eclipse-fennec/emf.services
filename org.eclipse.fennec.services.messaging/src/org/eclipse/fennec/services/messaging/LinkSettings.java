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

/**
 * What a {@link MessagingLink} is opened with: everything its owner
 * knows about the broker, and nothing about the library behind it.
 *
 * @param name               the messaging instance, unique in the
 *                           framework — what its services are selected by
 * @param url                where the broker is, e.g.
 *                           {@code tcp://localhost:1883} or {@code ssl://…}
 * @param clientId           this node's identity on the broker
 * @param keystore           the certificate and key this node shows
 *                           (PKCS12), or empty
 * @param keystorePassword   its password, also of the key
 * @param truststore         the authorities whose broker certificate
 *                           this node believes (PKCS12), or empty for the
 *                           JVM's
 * @param truststorePassword its password
 */
public record LinkSettings(String name, String url, String clientId, String keystore,
		String keystorePassword, String truststore, String truststorePassword) {

	/** MQTT 3.1.1 — the wire every MQTT part of this workspace speaks. */
	static final String MQTT3_FACTORY_PID = "org.gecko.messaging.mqtt3";

	public LinkSettings {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("a link needs a name");
		}
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("a link needs a broker URL");
		}
	}

	/** A link without a certificate of its own. */
	public static LinkSettings of(String name, String url, String clientId) {
		return new LinkSettings(name, url, clientId, null, null, null, null);
	}

	/**
	 * Which binding speaks to this broker — said by the scheme of its
	 * URL, the way the URL has always said how to connect.
	 */
	String factoryPid() {
		String scheme = scheme();
		return switch (scheme) {
		case "tcp", "ssl", "ws", "wss" -> MQTT3_FACTORY_PID;
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
		// No session to come back to: what a subscriber missed while it
		// was away it reads again (FR-Sync-Reconnect), and the instance
		// restores its subscriptions itself.
		configuration.put("cleanSession", Boolean.TRUE);
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
		return scheme.equals("ssl") || scheme.equals("wss");
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
		return name + " → " + url;
	}
}
