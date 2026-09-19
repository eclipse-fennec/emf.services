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
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Enough Configuration Admin to see what a role wrote, in what order,
 * and what it took back.
 *
 * <p>A fake rather than a mock because the order of the calls is the
 * thing under test, and a recorded list reads better than a verification
 * script.
 */
class FakeConfigurationAdmin implements ConfigurationAdmin {

	/** Every write and delete, in the order it happened. */
	final List<String> journal = new ArrayList<>();

	private final Map<String, Map<String, Object>> stored = new HashMap<>();

	@Override
	public Configuration getConfiguration(String pid, String location) {
		return new FakeConfiguration(pid);
	}

	@Override
	public Configuration getConfiguration(String pid) {
		return getConfiguration(pid, "?");
	}

	@Override
	public Configuration getFactoryConfiguration(String factoryPid, String name, String location) {
		return new FakeConfiguration(factoryPid + "~" + name);
	}

	@Override
	public Configuration getFactoryConfiguration(String factoryPid, String name) {
		return getFactoryConfiguration(factoryPid, name, "?");
	}

	@Override
	public Configuration createFactoryConfiguration(String factoryPid) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Configuration createFactoryConfiguration(String factoryPid, String location) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Configuration[] listConfigurations(String filter) {
		return new Configuration[0];
	}

	/** What is configured right now, by the label a config.json would use. */
	Map<String, Map<String, Object>> contents() {
		return Map.copyOf(stored);
	}

	private final class FakeConfiguration implements Configuration {

		private final String label;

		private FakeConfiguration(String label) {
			this.label = label;
		}

		@Override
		public String getPid() {
			return label;
		}

		@Override
		public Dictionary<String, Object> getProperties() {
			Map<String, Object> current = stored.get(label);
			if (current == null) {
				return null;
			}
			Dictionary<String, Object> copy = new java.util.Hashtable<>(current);
			// Configuration Admin always hands its own bookkeeping back,
			// which is exactly what the comparison has to ignore.
			copy.put("service.pid", label);
			return copy;
		}

		@Override
		public void update(Dictionary<String, ?> properties) throws IOException {
			Map<String, Object> map = new HashMap<>();
			java.util.Enumeration<String> keys = properties.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				map.put(key, properties.get(key));
			}
			stored.put(label, map);
			journal.add("write " + label);
		}

		@Override
		public void delete() {
			stored.remove(label);
			journal.add("delete " + label);
		}

		@Override
		public String getFactoryPid() {
			return label.contains("~") ? label.substring(0, label.indexOf('~')) : null;
		}

		@Override
		public void update() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setBundleLocation(String location) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getBundleLocation() {
			return "?";
		}

		@Override
		public long getChangeCount() {
			return 0;
		}

		@Override
		public Dictionary<String, Object> getProcessedProperties(ServiceReference<?> reference) {
			return getProperties();
		}

		@Override
		public boolean updateIfDifferent(Dictionary<String, ?> properties) throws IOException {
			update(properties);
			return true;
		}

		@Override
		public void addAttributes(ConfigurationAttribute... attributes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<ConfigurationAttribute> getAttributes() {
			return Set.of();
		}

		@Override
		public void removeAttributes(ConfigurationAttribute... attributes) {
			throw new UnsupportedOperationException();
		}
	}

	/** Convenience for assertions that only care about one kind of entry. */
	static List<String> only(Collection<String> journal, String prefix) {
		return journal.stream().filter(entry -> entry.startsWith(prefix)).toList();
	}
}
