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

package org.eclipse.fennec.services.rsa.topology;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/** The few framework objects a topology manager is handed. */
final class Fakes {

	private Fakes() {
	}

	/** A service that asks to be exported and says nothing else. */
	static ServiceReference<Object> serviceReference() {
		return new ServiceReference<>() {

			private final Dictionary<String, Object> properties = properties();

			private Dictionary<String, Object> properties() {
				Dictionary<String, Object> values = new Hashtable<>();
				values.put("objectClass", new String[] { "com.acme.Thing" });
				values.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, "*");
				values.put("service.id", 1L);
				return values;
			}

			@Override
			public Object getProperty(String key) {
				return properties.get(key);
			}

			@Override
			public String[] getPropertyKeys() {
				return java.util.Collections.list(properties.keys()).toArray(String[]::new);
			}

			@Override
			public Bundle getBundle() {
				return null;
			}

			@Override
			public Bundle[] getUsingBundles() {
				return new Bundle[0];
			}

			@Override
			public boolean isAssignableTo(Bundle bundle, String className) {
				return true;
			}

			@Override
			public int compareTo(Object other) {
				return 0;
			}

			@Override
			public Dictionary<String, Object> getProperties() {
				return properties;
			}

			@Override
			public <A> A adapt(Class<A> type) {
				return null;
			}

			@Override
			public String toString() {
				return "com.acme.Thing";
			}
		};
	}

	/** An endpoint with the three properties the specification requires. */
	static EndpointDescription endpoint(String id) {
		return new EndpointDescription(Map.of(
				"objectClass", new String[] { "com.acme.Thing" },
				RemoteConstants.ENDPOINT_ID, id,
				RemoteConstants.SERVICE_IMPORTED_CONFIGS, new String[] { "fennec.rest" }));
	}

	static EndpointEvent added(EndpointDescription endpoint) {
		return new EndpointEvent(EndpointEvent.ADDED, endpoint);
	}

	static EndpointEvent removed(EndpointDescription endpoint) {
		return new EndpointEvent(EndpointEvent.REMOVED, endpoint);
	}
}
