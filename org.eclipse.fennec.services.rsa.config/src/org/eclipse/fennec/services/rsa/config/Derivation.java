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

	/** The same two, for a node that serves or hears over MQTT (#98). */
	static final String DISTRIBUTION_MQTT_PID = "org.eclipse.fennec.services.rsa.distribution.mqtt";

	static final String DISCOVERY_MQTT_PID = "org.eclipse.fennec.services.rsa.discovery.mqtt";

	/** The transport that carries the broker's events to an MQTT-hearing node. */
	static final String CLIENT_MQTT_PID = "org.eclipse.fennec.services.client.mqtt";

	static final String ADMIN_FACTORY = "org.eclipse.fennec.services.rsa";

	static final String TOPOLOGY_PID = "org.eclipse.fennec.services.rsa.topology";

	/** Where the token for this node's calls comes from (SECURITY.md §6). */
	static final String OAUTH_PID = "org.eclipse.fennec.services.security.oauth";

	/** Who verifies the tokens of the calls this node serves. */
	static final String VERIFIER_PID = "org.eclipse.fennec.services.security.jwt";

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
		identity(plan, settings, true);
		// An HTTP stack only where something is served over HTTP. A node
		// that exports over MQTT talks to the broker as a client and
		// needs no server of its own.
		if (settings.manageHttp() && settings.servesOverRest()) {
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
		distribution(plan, settings);
		discovery(plan, settings);
		plan.add(admin(settings, "(ddsr.rsa.flavor=" + settings.effectiveDistributionFlavor() + ")"));
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
		identity(plan, settings, false);
		contracts(plan, settings);
		client(plan, settings);
		discovery(plan, settings);
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
		client.put("supported.flavors", settings.supportedFlavors());
		client.put("eventSource.target", "(ddsr.event.transport="
				+ (settings.hearsOverMqtt() ? "mqtt" : "rest") + ")");
		if (!settings.effectiveConsumerId().isBlank()) {
			client.put("consumer.id", settings.effectiveConsumerId());
		}
		plan.add(DerivedConfiguration.of(CLIENT_PID, Map.copyOf(client)));
	}

	/**
	 * One admin per configuration type, named after the flavor — which is
	 * what makes a second flavor (#98) a second line here rather than a
	 * change to the admin.
	 */
	private static DerivedConfiguration admin(RsaSettings settings, String distributionTarget) {
		// The type an admin speaks is the one it exports with — an
		// endpoint's configuration type is a fact about how it is
		// reached, not about how it was announced. Discovery is pointed
		// at separately for exactly that reason.
		String type = settings.effectiveDistributionFlavor();
		return DerivedConfiguration.ofFactory(ADMIN_FACTORY, factoryName(type), Map.of(
				"remote.configs.supported", type,
				"distribution.target", distributionTarget,
				"discovery.target", "(ddsr.rsa.flavor=" + settings.effectiveDiscoveryFlavor() + ")"));
	}

	/**
	 * Where this node serves, in the terms of the transport it serves
	 * over. One line per flavor, which is what #109 promised a second
	 * flavor would cost.
	 */
	private static void distribution(List<DerivedConfiguration> plan, RsaSettings settings) {
		if (RsaSettings.MQTT.equals(settings.effectiveDistributionFlavor())) {
			plan.add(DerivedConfiguration.of(DISTRIBUTION_MQTT_PID, Map.of(
					"broker.url", settings.mqttUrl(),
					"topic.prefix", settings.topicPrefix())));
			return;
		}
		Map<String, Object> rest = new LinkedHashMap<>();
		rest.put("public.url", settings.effectivePublicUrl());
		NodeIdentity identity = settings.identity();
		if (identity.verifies()) {
			// The scheme a guarded contract's flavor names: the same issuer
			// the verifier checks, so what a consumer is told and what this
			// node accepts cannot drift apart.
			rest.put("issuer", identity.issuer());
			rest.put("audiences", identity.audiences().toArray(String[]::new));
			rest.put("jwks.uri", identity.jwksUri());
			rest.put("scope.claim", identity.scopeClaim());
		}
		plan.add(DerivedConfiguration.of(DISTRIBUTION_PID, Map.copyOf(rest)));
	}

	/**
	 * Who this node is (SECURITY.md §6), first in the plan: the token
	 * supplier and the verifier are there before the client calls the
	 * broker and before anything is served — and, the plan coming down in
	 * reverse, they go last. A node without an identity writes neither.
	 */
	private static void identity(List<DerivedConfiguration> plan, RsaSettings settings, boolean serves) {
		NodeIdentity identity = settings.identity();
		if (identity.callsWithToken()) {
			plan.add(DerivedConfiguration.of(OAUTH_PID, Map.of(
					"token.uri", identity.tokenUri(),
					"client.id", identity.clientId(),
					"client.secret", identity.clientSecret(),
					"scope", identity.scope())));
		}
		if (serves && identity.verifies()) {
			plan.add(DerivedConfiguration.of(VERIFIER_PID, Map.of(
					"issuer", identity.issuer(),
					"jwks.uri", identity.jwksUri(),
					"audiences", identity.audiences().toArray(String[]::new),
					"scope.claim", identity.scopeClaim(),
					"subject.claim", identity.subjectClaim())));
		}
	}

	/**
	 * How this node announces and hears.
	 *
	 * <p>Announcing is a publish to the broker either way — that is the
	 * one API it has. What the MQTT variant changes is where the events
	 * come from, so it brings the event transport with it and points the
	 * client at it. Writing both here is the point of a role
	 * configuration: a node that hears over MQTT while its client
	 * listens over SSE is two facts disagreeing, and nobody should have
	 * to keep them in step by hand.
	 */
	private static void discovery(List<DerivedConfiguration> plan, RsaSettings settings) {
		if (!settings.hearsOverMqtt()) {
			plan.add(DerivedConfiguration.of(DISCOVERY_PID, Map.of(
					"broker.url", settings.brokerUrl())));
			return;
		}
		plan.add(DerivedConfiguration.of(CLIENT_MQTT_PID, Map.of(
				"broker.url", settings.mqttUrl(),
				"topic.prefix", "ddsr/events")));
		plan.add(DerivedConfiguration.of(DISCOVERY_MQTT_PID, Map.of(
				"broker.url", settings.brokerUrl())));
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
