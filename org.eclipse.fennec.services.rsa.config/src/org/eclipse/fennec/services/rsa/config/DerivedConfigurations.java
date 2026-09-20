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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * The set of configurations a node owns: written in the order the plan
 * gives them, and removed in the opposite order.
 *
 * <p>The order is the whole point. Writing runs from infrastructure
 * outwards — HTTP, the model registry, the client, the transports, the
 * admin, and the topology manager last, because that is the one that
 * starts exporting. Removing runs back the other way, so exports are
 * withdrawn while the transports that carry the withdrawal are still up
 * (FR-P3). Today that order is whatever bundle stop order happens to be,
 * which is not something a deployment should have to know.
 *
 * <p>Nothing here writes a configuration that already says the same
 * thing. Configuration Admin delivers an update per write, every
 * delivery restarts or modifies components below, and a node that
 * rewrites its nine configurations on every change would shake the stack
 * for nothing.
 */
final class DerivedConfigurations implements AutoCloseable {

	private static final Logger LOG = Logger.getLogger(DerivedConfigurations.class.getName());

	/**
	 * Configuration Admin's own bookkeeping, which is in the properties it
	 * hands back but never in the properties we wrote.
	 */
	private static final Set<String> ADMIN_KEYS = Set.of("service.pid", "service.factoryPid",
			"service.bundleLocation", ":configurator:resource-version", ":configurator:version",
			":configurator:symbolic-name");

	/**
	 * Location {@code "?"} binds a configuration to no bundle in
	 * particular, which is what the Configurator writes too — anything
	 * else would let the first bundle to read it claim it.
	 */
	private static final String ANY_LOCATION = "?";

	private final ConfigurationAdmin admin;

	private final String owner;

	/** What we wrote, in the order we wrote it. */
	private final List<DerivedConfiguration> written = new ArrayList<>();

	DerivedConfigurations(ConfigurationAdmin admin, String owner) {
		this.admin = Objects.requireNonNull(admin);
		this.owner = owner;
	}

	/**
	 * Brings the derived set to exactly this plan.
	 *
	 * <p>Entries are written in plan order, and anything written earlier
	 * that the plan no longer contains is removed afterwards — a node that
	 * turns off its own HTTP stack takes those two configurations with it
	 * rather than leaving them behind.
	 */
	void apply(List<DerivedConfiguration> plan) throws IOException {
		for (DerivedConfiguration entry : plan) {
			write(entry);
		}
		List<DerivedConfiguration> dropped = written.stream()
				.filter(earlier -> plan.stream().noneMatch(kept -> kept.label().equals(earlier.label())))
				.toList();
		for (int i = dropped.size() - 1; i >= 0; i--) {
			remove(dropped.get(i));
		}
		written.clear();
		written.addAll(plan);
	}

	private void write(DerivedConfiguration entry) throws IOException {
		Configuration configuration = entry.isFactory()
				? admin.getFactoryConfiguration(entry.pid(), entry.name(), ANY_LOCATION)
				: admin.getConfiguration(entry.pid(), ANY_LOCATION);
		Map<String, Object> current = asMap(configuration.getProperties());
		if (current.equals(entry.properties())) {
			LOG.fine(() -> "[DDSR] " + owner + " leaves " + entry.label() + " as it is");
			return;
		}
		configuration.update(asDictionary(entry.properties()));
		LOG.info("[DDSR] " + owner + " configured " + entry.label());
	}

	private void remove(DerivedConfiguration entry) {
		try {
			Configuration configuration = entry.isFactory()
					? admin.getFactoryConfiguration(entry.pid(), entry.name(), ANY_LOCATION)
					: admin.getConfiguration(entry.pid(), ANY_LOCATION);
			configuration.delete();
			LOG.info("[DDSR] " + owner + " withdrew " + entry.label());
		} catch (IOException | IllegalStateException failure) {
			// One configuration that will not go must not stop the rest:
			// what follows it in the reverse order is what carries the
			// withdrawals out.
			LOG.log(Level.WARNING, "[DDSR] " + owner + " could not withdraw " + entry.label(), failure);
		}
	}

	/**
	 * Takes the whole set down, last written first.
	 *
	 * <p>Which means the topology manager stops exporting before the admin
	 * goes, and the admin goes before the transports that carry its
	 * withdrawals.
	 */
	@Override
	public void close() {
		for (int i = written.size() - 1; i >= 0; i--) {
			remove(written.get(i));
		}
		written.clear();
	}

	private static Map<String, Object> asMap(Dictionary<String, Object> properties) {
		if (properties == null) {
			return Map.of();
		}
		Map<String, Object> map = new HashMap<>();
		Enumeration<String> keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (!ADMIN_KEYS.contains(key)) {
				map.put(key, properties.get(key));
			}
		}
		return map;
	}

	private static Dictionary<String, Object> asDictionary(Map<String, Object> properties) {
		Dictionary<String, Object> dictionary = new Hashtable<>();
		new LinkedHashMap<>(properties).forEach(dictionary::put);
		return dictionary;
	}
}
