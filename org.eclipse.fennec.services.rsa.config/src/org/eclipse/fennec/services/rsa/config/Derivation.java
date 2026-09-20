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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a node's few decisions into the configurations the bundles
 * below actually read.
 *
 * <p>Pure on purpose: the order and the derived values are the thing
 * worth testing, and neither needs a framework to be wrong.
 *
 * <p>The order runs from infrastructure outwards, and the topology
 * manager is last because it is the one that starts exporting. Taking
 * the plan down in reverse is therefore a withdrawal before the
 * transports go, which is what FR-P3 asks for.
 */
public final class Derivation {

	/** Where the model registry the RSA registry reads from lives. */
	static final String EOBJECT_REGISTRY_FACTORY = "EObjectRegistry";

	/** The name that registry carries, and that the RSA registry looks up. */
	static final String CONTRACT_REGISTRY_NAME = "ddsrContracts";

	static final String FELIX_HTTP_FACTORY = "org.apache.felix.http";

	static final String WHITEBOARD_FACTORY = "JakartarsServletWhiteboardRuntimeComponent";

	static final String CLIENT_PID = "org.eclipse.fennec.services.client";

	static final String CLIENT_REST_PID = "org.eclipse.fennec.services.client.rest";

	static final String LOCAL_REGISTRY_PID = "org.eclipse.fennec.services.rsa.registry";

	static final String DISTRIBUTION_PID = "org.eclipse.fennec.services.rsa.distribution.rest";

	static final String DISCOVERY_PID = "org.eclipse.fennec.services.rsa.discovery.rest";

	static final String ADMIN_FACTORY = "org.eclipse.fennec.services.rsa";

	static final String TOPOLOGY_PID = "org.eclipse.fennec.services.rsa.topology";

	/** The distribution a node points at when it exports nothing. */
	static final String NO_DISTRIBUTION = "(ddsr.rsa.flavor=none)";

	private Derivation() {
	}

	/**
	 * A node that serves and announces: it has an HTTP stack, a
	 * distribution, a discovery and an admin that uses both.
	 */
	public static List<DerivedConfiguration> forProvider(RsaSettings settings) {
		List<DerivedConfiguration> plan = new ArrayList<>();
		if (settings.manageHttp()) {
			plan.add(DerivedConfiguration.ofFactory(FELIX_HTTP_FACTORY, settings.httpId(), Map.of(
					"org.osgi.service.http.port", String.valueOf(settings.httpPort()),
					"org.osgi.service.http.host", settings.httpHost(),
					"org.apache.felix.http.context_path", settings.effectiveContextPath(),
					"org.apache.felix.http.name", settings.registryName() + " HTTP",
					"org.apache.felix.http.runtime.init.id", settings.httpId())));
			plan.add(DerivedConfiguration.ofFactory(WHITEBOARD_FACTORY, settings.httpId(), Map.of(
					"jersey.jakartars.whiteboard.name", settings.registryName() + " Rest",
					"osgi.http.whiteboard.target", "(id=" + settings.httpId() + ")")));
		}
		contracts(plan, settings);
		client(plan, settings);
		plan.add(DerivedConfiguration.of(DISTRIBUTION_PID, Map.of(
				"public.url", settings.effectivePublicUrl())));
		plan.add(DerivedConfiguration.of(DISCOVERY_PID, Map.of(
				"broker.url", settings.brokerUrl())));
		plan.add(admin(settings, "(ddsr.rsa.flavor=" + settings.flavor() + ")"));
		plan.add(topology(settings));
		return List.copyOf(plan);
	}

	/**
	 * A node that only consumes: no HTTP stack of its own, and an admin
	 * whose distribution is the one that exports nothing.
	 *
	 * <p>That last part is a statement, not a gap — the node says it does
	 * not export, rather than being configured with a reference that
	 * happens to find nothing.
	 */
	public static List<DerivedConfiguration> forConsumer(RsaSettings settings) {
		List<DerivedConfiguration> plan = new ArrayList<>();
		contracts(plan, settings);
		client(plan, settings);
		plan.add(DerivedConfiguration.of(DISCOVERY_PID, Map.of(
				"broker.url", settings.brokerUrl())));
		plan.add(admin(settings, NO_DISTRIBUTION));
		plan.add(topology(settings));
		return List.copyOf(plan);
	}

	private static void contracts(List<DerivedConfiguration> plan, RsaSettings settings) {
		plan.add(DerivedConfiguration.ofFactory(EOBJECT_REGISTRY_FACTORY, CONTRACT_REGISTRY_NAME, Map.of(
				"name", "ddsr.contracts",
				"initialProvider.target", "(emf.eobject.provider.name=ddsr.bundle.models)")));
		plan.add(DerivedConfiguration.of(LOCAL_REGISTRY_PID, Map.of(
				"registry.name", settings.registryName(),
				"default.version", settings.defaultVersion())));
	}

	private static void client(List<DerivedConfiguration> plan, RsaSettings settings) {
		plan.add(DerivedConfiguration.of(CLIENT_REST_PID, Map.of(
				"broker.url", settings.brokerUrl())));
		Map<String, Object> client = new LinkedHashMap<>();
		client.put("provider.heartbeat.seconds", String.valueOf(settings.heartbeatSeconds()));
		client.put("session.interval.seconds", String.valueOf(settings.sessionIntervalSeconds()));
		if (!settings.consumerId().isBlank()) {
			client.put("consumer.id", settings.consumerId());
		}
		plan.add(DerivedConfiguration.of(CLIENT_PID, Map.copyOf(client)));
	}

	/**
	 * One admin per configuration type, named after the flavor — which is
	 * what makes a second flavor (#98) a second line here rather than a
	 * change to the admin.
	 */
	private static DerivedConfiguration admin(RsaSettings settings, String distributionTarget) {
		return DerivedConfiguration.ofFactory(ADMIN_FACTORY, factoryName(settings.flavor()), Map.of(
				"remote.configs.supported", settings.flavor(),
				"distribution.target", distributionTarget,
				"discovery.target", "(ddsr.rsa.flavor=" + settings.flavor() + ")"));
	}

	/**
	 * A factory configuration's name shows up in logs and in the
	 * Configurator's own {@code pid~name} spelling, so the flavor's dots
	 * become dashes: {@code fennec.rest} names the admin
	 * {@code …rsa~fennec-rest}.
	 */
	static String factoryName(String flavor) {
		return flavor.replaceAll("[^A-Za-z0-9_-]", "-");
	}

	private static DerivedConfiguration topology(RsaSettings settings) {
		return DerivedConfiguration.of(TOPOLOGY_PID, Map.of(
				"policy", settings.exportPolicy(),
				"import.policy", settings.importPolicy()));
	}
}
