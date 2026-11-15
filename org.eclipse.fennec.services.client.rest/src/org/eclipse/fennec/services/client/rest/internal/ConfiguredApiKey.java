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

import org.eclipse.fennec.services.common.ApiKeySupplier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * One API key a node presents, from configuration (#299): a factory
 * configuration per endpoint, e.g.
 *
 * <pre>
 * "org.eclipse.fennec.services.client.rest.apikey~petstore": {
 *   "host": "http://petstore:8080", "scheme": "api_key", "key": "$[env:PETSTORE_KEY]" }
 * </pre>
 *
 * The REST invoker asks every such supplier when an endpoint's flavor offers
 * an {@code ApiKeyScheme} and no bearer token is used.
 */
@Component(name = ConfiguredApiKey.PID, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = ConfiguredApiKey.Config.class, factory = true)
public final class ConfiguredApiKey implements ApiKeySupplier {

	/** The factory PID. */
	public static final String PID = "org.eclipse.fennec.services.client.rest.apikey";

	/** Matches every host or every scheme. */
	static final String ANY = "*";

	@ObjectClassDefinition(name = "Fennec Services API key",
			description = "An API key this node presents to an endpoint whose flavor offers an ApiKeyScheme")
	public @interface Config {

		@AttributeDefinition(name = "Host",
				description = "Where the key goes, scheme://host[:port] as the endpoint's flavor names it; * for any")
		String host() default ANY;

		@AttributeDefinition(name = "Scheme",
				description = "The name of the ApiKeyScheme in the endpoint's flavor, e.g. api_key; * for any")
		String scheme() default ANY;

		@AttributeDefinition(name = "Key", type = AttributeType.PASSWORD, description = "The key itself")
		String key();
	}

	private final String host;
	private final String scheme;
	private final String key;

	@Activate
	public ConfiguredApiKey(Config config) {
		this(config.host(), config.scheme(), config.key());
	}

	ConfiguredApiKey(String host, String scheme, String key) {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("an API key configuration without a key");
		}
		this.host = host == null || host.isBlank() ? ANY : strip(host);
		this.scheme = scheme == null || scheme.isBlank() ? ANY : scheme.strip();
		this.key = key;
	}

	@Override
	public String key(String host, String scheme) {
		boolean hostMatches = ANY.equals(this.host) || this.host.equalsIgnoreCase(strip(host));
		boolean schemeMatches = ANY.equals(this.scheme) || this.scheme.equals(scheme);
		return hostMatches && schemeMatches ? key : null;
	}

	private static String strip(String host) {
		String h = host == null ? "" : host.strip();
		while (h.endsWith("/")) {
			h = h.substring(0, h.length() - 1);
		}
		return h;
	}

	@Override
	public String toString() {
		return "ConfiguredApiKey[" + host + ", " + scheme + "]";
	}
}
