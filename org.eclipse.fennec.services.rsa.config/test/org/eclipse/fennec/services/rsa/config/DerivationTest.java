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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What one node configuration has to turn into.
 *
 * <p>The values matter because they used to be stated twice and could
 * disagree; the order matters because taking the plan down in reverse
 * is what withdraws an export before the transport that carries the
 * withdrawal goes away.
 */
class DerivationTest {

	private static RsaSettings provider() {
		return new RsaSettings("http://broker:8887/ddsr/rest", "", "", 9095, "0.0.0.0", "services",
				true, "ddsrHttp", "node-a", "1.0.0", "fennec.rest", "promiscuous", "promiscuous",
				30, 0, "", "tcp://localhost:1883", "ddsr/rpc", "", "");
	}

	@Test
	@DisplayName("the broker is stated once and reaches both the client and the discovery")
	void oneBrokerUrl() {
		List<DerivedConfiguration> plan = Derivation.forProvider(provider());

		assertThat(byLabel(plan, Derivation.CLIENT_REST_PID).properties())
				.containsEntry("broker.url", "http://broker:8887/ddsr/rest");
		assertThat(byLabel(plan, Derivation.DISCOVERY_PID).properties())
				.containsEntry("broker.url", "http://broker:8887/ddsr/rest");
	}

	@Test
	@DisplayName("the public URL derives from the very port and context path that configure the HTTP stack")
	void publicUrlDerived() {
		List<DerivedConfiguration> plan = Derivation.forProvider(provider());

		assertThat(byLabel(plan, Derivation.DISTRIBUTION_PID).properties())
				.containsEntry("public.url", "http://localhost:9095/services");
		assertThat(byLabel(plan, "org.apache.felix.http~ddsrHttp").properties())
				.containsEntry("org.osgi.service.http.port", "9095")
				.containsEntry("org.apache.felix.http.context_path", "services");
	}

	@Test
	@DisplayName("a stated public URL wins, because a node behind a proxy publishes another address on purpose")
	void publicUrlStated() {
		RsaSettings behindProxy = new RsaSettings("http://broker:8887/ddsr/rest",
				"https://edge.example.org/api/", "", 9095, "0.0.0.0", "services", true, "ddsrHttp",
				"node-a", "1.0.0", "fennec.rest", "promiscuous", "promiscuous", 30, 0, "", "tcp://localhost:1883", "ddsr/rpc", "", "");

		assertThat(behindProxy.effectivePublicUrl()).isEqualTo("https://edge.example.org/api");
	}

	@Test
	@DisplayName("the topology manager is written last, so taking the plan down withdraws first")
	void topologyIsLast() {
		List<DerivedConfiguration> plan = Derivation.forProvider(provider());

		assertThat(plan.get(plan.size() - 1).label()).isEqualTo(Derivation.TOPOLOGY_PID);
		assertThat(plan.get(plan.size() - 2).label()).isEqualTo("org.eclipse.fennec.services.rsa~fennec-rest");
	}

	@Test
	@DisplayName("the HTTP stack comes first, and only when this node owns it")
	void httpFirstAndOptional() {
		assertThat(Derivation.forProvider(provider()).get(0).label()).isEqualTo("org.apache.felix.http~ddsrHttp");

		RsaSettings borrowed = new RsaSettings("http://broker:8887/ddsr/rest",
				"http://node-a:9095/services", "", 9095, "0.0.0.0", "services", false, "ddsrHttp",
				"node-a", "1.0.0", "fennec.rest", "promiscuous", "promiscuous", 30, 0, "", "tcp://localhost:1883", "ddsr/rpc", "", "");

		assertThat(Derivation.forProvider(borrowed).stream().map(DerivedConfiguration::label))
				.doesNotContain("org.apache.felix.http~ddsrHttp",
						"JakartarsServletWhiteboardRuntimeComponent~ddsrHttp");
	}

	@Test
	@DisplayName("a consumer says it exports nothing instead of pointing at a distribution that is missing")
	void consumerSaysItExportsNothing() {
		RsaSettings settings = new RsaSettings("http://broker:8887/ddsr/rest", "", "", 0, "", "",
				false, "", "node-b", "1.0.0", "fennec.rest", "promiscuous", "promiscuous", 30, 0, "node-b", "tcp://localhost:1883", "ddsr/rpc", "", "");

		List<DerivedConfiguration> plan = Derivation.forConsumer(settings);

		assertThat(byLabel(plan, "org.eclipse.fennec.services.rsa~fennec-rest").properties())
				.containsEntry("distribution.target", "(ddsr.rsa.flavor=none)")
				.containsEntry("discovery.target", "(ddsr.rsa.flavor=fennec.rest)");
		assertThat(plan.stream().map(DerivedConfiguration::label))
				.doesNotContain(Derivation.DISTRIBUTION_PID);
	}

	@Test
	@DisplayName("an empty consumer id is left out rather than written as an empty one")
	void emptyConsumerIdIsAbsent() {
		List<DerivedConfiguration> plan = Derivation.forProvider(provider());

		assertThat(byLabel(plan, Derivation.CLIENT_PID).properties()).doesNotContainKey("consumer.id");
	}

	private static DerivedConfiguration byLabel(List<DerivedConfiguration> plan, String label) {
		return plan.stream().filter(entry -> entry.label().equals(label)).findFirst()
				.orElseThrow(() -> new AssertionError("no " + label + " in " + plan.stream()
						.map(DerivedConfiguration::label).toList()));
	}

	/** A node that serves over MQTT and announces over REST — the pairing #98 calls the proof. */
	private static RsaSettings mqttProvider() {
		return new RsaSettings("http://broker:8887/ddsr/rest", "", "", 9095, "0.0.0.0", "services",
				true, "ddsrHttp", "node-a", "1.0.0", "fennec.rest", "promiscuous", "promiscuous",
				30, 0, "", "tcp://mosquitto:1883", "ddsr/rpc", "fennec.mqtt", "");
	}

	@Test
	@DisplayName("serving over MQTT configures the MQTT distribution and no HTTP stack")
	void mqttDistribution() {
		List<DerivedConfiguration> plan = Derivation.forProvider(mqttProvider());

		assertThat(byLabel(plan, Derivation.DISTRIBUTION_MQTT_PID).properties())
				.containsEntry("broker.url", "tcp://mosquitto:1883")
				.containsEntry("topic.prefix", "ddsr/rpc");
		assertThat(plan).extracting(DerivedConfiguration::label)
				.as("an MQTT export answers on topics; it needs no HTTP server of its own")
				.doesNotContain(Derivation.DISTRIBUTION_PID, Derivation.FELIX_HTTP_FACTORY);
	}

	@Test
	@DisplayName("the admin speaks the type it exports with and listens where discovery is")
	void adminFollowsBothHalves() {
		List<DerivedConfiguration> plan = Derivation.forProvider(mqttProvider());

		// By the factory name the flavor gives it, not by prefix: the
		// local registry's PID starts with the admin factory's.
		DerivedConfiguration admin = byLabel(plan,
				Derivation.ADMIN_FACTORY + "~" + Derivation.factoryName("fennec.mqtt"));
		assertThat(admin.properties())
				.as("an endpoint's configuration type says how it is reached, not how it was announced")
				.containsEntry("remote.configs.supported", "fennec.mqtt")
				.containsEntry("distribution.target", "(ddsr.rsa.flavor=fennec.mqtt)")
				.containsEntry("discovery.target", "(ddsr.rsa.flavor=fennec.rest)");
	}

	@Test
	@DisplayName("hearing over MQTT brings the event transport with it and points the client at it")
	void mqttDiscoveryConfiguresTheEventSource() {
		RsaSettings hearsOverMqtt = new RsaSettings("http://broker:8887/ddsr/rest", "", "", 9095,
				"0.0.0.0", "services", true, "ddsrHttp", "node-a", "1.0.0", "fennec.rest",
				"promiscuous", "promiscuous", 30, 0, "", "tcp://mosquitto:1883", "ddsr/rpc",
				"", "fennec.mqtt");

		List<DerivedConfiguration> plan = Derivation.forProvider(hearsOverMqtt);

		assertThat(byLabel(plan, Derivation.CLIENT_MQTT_PID).properties())
				.containsEntry("broker.url", "tcp://mosquitto:1883")
				.containsEntry("topic.prefix", "ddsr/events");
		assertThat(byLabel(plan, Derivation.DISCOVERY_MQTT_PID).properties())
				.as("announcing is still a publish to the DDSR broker — that is the one API it has")
				.containsEntry("broker.url", "http://broker:8887/ddsr/rest");
		assertThat(byLabel(plan, Derivation.CLIENT_PID).properties())
				.as("a node that hears over MQTT while its client listens over SSE is two facts disagreeing")
				.containsEntry("eventSource.target", "(ddsr.event.transport=mqtt)");
		assertThat(plan).extracting(DerivedConfiguration::label)
				.doesNotContain(Derivation.DISCOVERY_PID);
	}

	@Test
	@DisplayName("a node that speaks one transport still says it once")
	void oneFlavorStillMeansBoth() {
		List<DerivedConfiguration> plan = Derivation.forProvider(provider());

		assertThat(byLabel(plan, Derivation.DISTRIBUTION_PID)).isNotNull();
		assertThat(byLabel(plan, Derivation.DISCOVERY_PID)).isNotNull();
		assertThat(byLabel(plan, Derivation.CLIENT_PID).properties())
				.containsEntry("eventSource.target", "(ddsr.event.transport=rest)");
	}
}
