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

package org.eclipse.fennec.services.rsa.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jakartars.runtime.JakartarsServiceRuntime;
import org.osgi.service.jakartars.runtime.JakartarsServiceRuntimeConstants;

/**
 * Compares the address a provider tells consumers to dial with the one
 * the whiteboard actually serves.
 *
 * <p>A separate component, and it has to be: {@link RsaProvider} writes
 * the HTTP configuration, so it cannot wait for the runtime that
 * configuration brings up. This one can, because it writes nothing — it
 * holds the runtime mandatory and static, which means it exists only
 * once there is something to compare against.
 *
 * <p>It never corrects anything. A node behind a reverse proxy serves
 * one address and publishes another on purpose, and guessing which case
 * this is would be worse than saying what was found.
 */
@Component(name = PublicEndpointCheck.NAME, configurationPid = RsaProvider.PID,
		configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class PublicEndpointCheck {

	static final String NAME = "org.eclipse.fennec.services.rsa.provider.check";

	private static final Logger LOG = Logger.getLogger(PublicEndpointCheck.class.getName());

	/**
	 * The whiteboard, held for its service properties rather than its
	 * methods: {@code osgi.jakartars.endpoint} is what it really serves.
	 */
	@Reference
	private ServiceReference<JakartarsServiceRuntime> runtime;

	@Activate
	void activate(RsaProvider.Config config) {
		check(config);
	}

	@Modified
	void modified(RsaProvider.Config config) {
		check(config);
	}

	private void check(RsaProvider.Config config) {
		String published = RsaProvider.settings(config).effectivePublicUrl();
		List<String> served = endpoints();
		if (served.isEmpty()) {
			LOG.warning("[DDSR] the whiteboard reports no endpoint, so nothing confirms that " + published
					+ " is served");
			return;
		}
		if (served.stream().anyMatch(endpoint -> sameAddress(endpoint, published))) {
			LOG.info("[DDSR] provider publishes " + published + ", which the whiteboard serves");
		} else {
			LOG.warning("[DDSR] provider publishes " + published + " but the whiteboard serves "
					+ String.join(", ", served) + ": right behind a proxy, a typo otherwise");
		}
	}

	private List<String> endpoints() {
		Object value = runtime.getProperty(JakartarsServiceRuntimeConstants.JAKARTA_RS_SERVICE_ENDPOINT);
		if (value instanceof String[] many) {
			return List.of(many);
		}
		if (value instanceof String one) {
			return List.of(one);
		}
		if (value instanceof List<?> listed) {
			return listed.stream().map(String::valueOf).toList();
		}
		return List.of();
	}

	/**
	 * Whether two addresses name the same place.
	 *
	 * <p>Port and path only, on purpose. The whiteboard reports the
	 * interfaces it bound - the machine's addresses, one per stack, IPv6
	 * included - and never the name a consumer would use, so comparing
	 * hosts would report a mismatch on every correct setup. What is worth
	 * comparing is what a typo actually gets wrong: the port and the
	 * context path.
	 */
	static boolean sameAddress(String endpoint, String published) {
		try {
			URI served = new URI(endpoint.strip());
			URI stated = new URI(published.strip());
			return served.getPort() == stated.getPort() && path(served).equals(path(stated));
		} catch (URISyntaxException unreadable) {
			return false;
		}
	}

	private static String path(URI uri) {
		String value = uri.getPath() == null ? "" : uri.getPath();
		while (value.startsWith("/")) {
			value = value.substring(1);
		}
		while (value.endsWith("/")) {
			value = value.substring(0, value.length() - 1);
		}
		return value;
	}
}
