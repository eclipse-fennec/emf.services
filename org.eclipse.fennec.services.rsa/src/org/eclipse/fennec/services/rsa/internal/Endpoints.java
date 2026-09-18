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

package org.eclipse.fennec.services.rsa.internal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.rsa.spi.RsaProperties;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * An exported service, described the way Remote Service Admin describes
 * one.
 *
 * <p>Inside this implementation an endpoint is the model — the contract
 * and the implementation with its flavor. An {@link EndpointDescription}
 * is what leaves the framework: a flat map, which is all the
 * specification allows. So this is a narrowing, and what it keeps is
 * what another framework needs to find its way back: which contract,
 * which registration, and that it is this registry's doing.
 *
 * <p>The properties the specification requires are set because it
 * requires them; the {@code ddsr.*} ones are added because a consumer of
 * ours can then look the endpoint up in the broker instead of
 * reconstructing it from a map.
 */
final class Endpoints {

	private Endpoints() {
	}

	/**
	 * @param effective  the service's properties with the exporter's
	 *        overrides already applied — what the export was decided on
	 *        is what the description says
	 * @param exportedAs the Java interface the service was exported as.
	 *        This — not the contract's name — is what goes into
	 *        {@code objectClass}: an importer loads a class by it, and a
	 *        contract name is not a class.
	 * @param intents    what the endpoint promises, as
	 *        {@code service.intents}; the importing side is required to
	 *        repeat them on the proxy
	 * @param configProperties what the distribution wants said under its
	 *        configuration type's prefix — the address, for one
	 */
	static EndpointDescription describe(Map<String, Object> effective, ServiceReference<?> exported,
			Class<?> exportedAs, ServiceInterface contract, ServiceImplementation implementation,
			String frameworkUuid, String configType, Collection<String> intents,
			Map<String, Object> configProperties) {
		Map<String, Object> properties = new LinkedHashMap<>();

		// Whatever the exported service itself said, minus the export
		// instructions — they are what got us here, and repeating them on
		// the imported side would ask the importer to export it again —
		// and minus private properties: a key starting with a dot is the
		// framework's convention for "not for anyone else", and remote is
		// as anyone-else as it gets.
		for (Map.Entry<String, Object> property : effective.entrySet()) {
			String key = property.getKey();
			if (!key.toLowerCase(Locale.ROOT).startsWith("service.exported.") && !key.startsWith(".")) {
				properties.put(key, property.getValue());
			}
		}
		properties.putAll(configProperties);

		properties.put(Constants.OBJECTCLASS, new String[] { exportedAs.getName() });
		properties.put(RemoteConstants.ENDPOINT_ID, endpointId(implementation));
		properties.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, new String[] { configType });
		properties.put(RemoteConstants.SERVICE_INTENTS, intents.toArray(String[]::new));
		properties.put(RemoteConstants.ENDPOINT_SERVICE_ID, exported.getProperty(Constants.SERVICE_ID));
		properties.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID, frameworkUuid);
		properties.put(RemoteConstants.SERVICE_IMPORTED, Boolean.TRUE.toString());

		properties.put(RsaProperties.CONTRACT, contract.getName());
		properties.put(RsaProperties.IMPLEMENTATION, implementation.getImplementationId());

		return new EndpointDescription(properties);
	}

	/**
	 * A stable identity for the endpoint. The implementation id already
	 * names provider, contract and version, which is exactly what makes
	 * two exports of the same service the same endpoint; a random id
	 * would make every restart look like a new one.
	 */
	private static String endpointId(ServiceImplementation implementation) {
		String id = implementation.getImplementationId();
		return id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
	}
}
