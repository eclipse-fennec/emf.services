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

	@Test
	@DisplayName("serving over AMQP: its own distribution, announced over REST, no HTTP stack")
	void amqpDistribution() {
		RsaSettings servesOverAmqp = new RsaSettings("http://broker:8887/ddsr/rest", "", "", 9095,
				"0.0.0.0", "services", true, "ddsrHttp", "node-a", "1.0.0", "fennec.rest",
				"promiscuous", "promiscuous", 30, 0, "", "tcp://mosquitto:1883", "ddsr/rpc",
				"fennec.amqp", "").withAmqpUrl("amqp://rabbit:5672/calls");

		List<DerivedConfiguration> plan = Derivation.forProvider(servesOverAmqp);

		assertThat(byLabel(plan, Derivation.DISTRIBUTION_AMQP_PID).properties())
				.containsEntry("broker.url", "amqp://rabbit:5672/calls")
				.as("nobody named an encoding, so the flavor's default applies").containsEntry("content.type", "");
		assertThat(byLabel(Derivation.forProvider(servesOverAmqp.withEncoding(" application/json ")),
				Derivation.DISTRIBUTION_AMQP_PID).properties())
				.as("an exported interface has no contract document, so the role says what its calls"
						+ " are encoded in")
				.containsEntry("content.type", "application/json");
		assertThat(plan).extracting(DerivedConfiguration::label)
				.as("a node that serves over AMQP is a client of its broker and needs no server of its own")
				.doesNotContain(Derivation.DISTRIBUTION_PID, Derivation.DISTRIBUTION_MQTT_PID)
				.noneMatch(label -> label.startsWith(Derivation.FELIX_HTTP_FACTORY));
		assertThat(byLabel(plan, Derivation.ADMIN_FACTORY + "~" + Derivation.factoryName("fennec.amqp"))
				.properties())
				.containsEntry("remote.configs.supported", "fennec.amqp")
				.containsEntry("distribution.target", "(ddsr.rsa.flavor=fennec.amqp)")
				.containsEntry("discovery.target", "(ddsr.rsa.flavor=fennec.rest)");
		assertThat(byLabel(plan, Derivation.CLIENT_PID).properties())
				.containsEntry("supported.flavors", "AMQP,REST")
				.as("and the client waits for an invoker for each flavor it says it speaks")
				.containsEntry("invokers.target", "(&(fennec.invokers=AMQP)(fennec.invokers=REST))");
		assertThat(Derivation.invokersFor("REST")).isEqualTo("(fennec.invokers=REST)");
	}

	@Test
	@DisplayName("#138: MQTT 5 is said once on the role and reaches every MQTT connection of the node")
	void mqttVersionReachesEveryConnection() {
		RsaSettings speaksMqtt = new RsaSettings("http://broker:8887/ddsr/rest", "", "", 9095,
				"0.0.0.0", "services", true, "ddsrHttp", "node-a", "1.0.0", "fennec.mqtt",
				"promiscuous", "promiscuous", 30, 0, "", "tcp://mosquitto:1883", "ddsr/rpc", "", "");

		List<DerivedConfiguration> five = Derivation.forProvider(speaksMqtt.withMqttVersion(5));

		assertThat(byLabel(five, Derivation.CLIENT_MQTT_PID).properties()).containsEntry("mqtt.version", "5");
		assertThat(byLabel(five, Derivation.INVOKER_MESSAGING_PID).properties()).containsEntry("mqtt.version", "5");
		assertThat(byLabel(five, Derivation.CONNECTION_MESSAGING_PID).properties())
				.containsEntry("mqtt.version", "5");

		List<DerivedConfiguration> three = Derivation.forProvider(speaksMqtt);
		assertThat(byLabel(three, Derivation.CLIENT_MQTT_PID).properties())
				.as("3.1.1 is what a connection speaks unless told otherwise, so nothing is said")
				.doesNotContainKey("mqtt.version");
		assertThat(three).extracting(DerivedConfiguration::label)
				.as("and a node with nothing to tell its connections writes no configuration for them")
				.doesNotContain(Derivation.INVOKER_MESSAGING_PID, Derivation.CONNECTION_MESSAGING_PID);
		assertThat(speaksMqtt.withMqttVersion(4).mqttVersion()).as("anything else is 3").isEqualTo(3);
	}

	@Test
	@DisplayName("#138: a consumer whose one flavor is AMQP hears over AMQP — one event transport, another URL")
	void amqpConsumerHearsOverAmqp() {
		RsaSettings speaksAmqp = new RsaSettings("http://broker:8887/ddsr/rest", "", "", 0, "", "",
				false, "", "node-b", "1.0.0", "fennec.amqp", "promiscuous", "promiscuous", 30, 0, "",
				"tcp://mosquitto:1883", "ddsr/rpc", "", "").withAmqpUrl("amqp://rabbit:5672");

		List<DerivedConfiguration> plan = Derivation.forConsumer(speaksAmqp);

		assertThat(byLabel(plan, Derivation.CLIENT_MQTT_PID).properties())
				.as("the same event transport as over MQTT; the scheme of its URL says which protocol")
				.containsEntry("broker.url", "amqp://rabbit:5672")
				.as("said the way MQTT writes it — the transport spells it as a routing key")
				.containsEntry("topic.prefix", "ddsr/events");
		assertThat(byLabel(plan, Derivation.DISCOVERY_AMQP_PID).properties())
				.as("announcing is still a publish to the registry")
				.containsEntry("broker.url", "http://broker:8887/ddsr/rest");
		assertThat(byLabel(plan, Derivation.CLIENT_PID).properties())
				.containsEntry("supported.flavors", "AMQP")
				.containsEntry("eventSource.target", "(ddsr.event.transport=amqp)");
		assertThat(byLabel(plan, Derivation.ADMIN_FACTORY + "~" + Derivation.factoryName("fennec.amqp"))
				.properties())
				.containsEntry("remote.configs.supported", "fennec.amqp")
				.containsEntry("discovery.target", "(ddsr.rsa.flavor=fennec.amqp)");
		assertThat(plan).extracting(DerivedConfiguration::label)
				.doesNotContain(Derivation.DISCOVERY_PID, Derivation.DISCOVERY_MQTT_PID);
	}

	@Test
	@DisplayName("#138: where the registry publishes no events over AMQP, the node says it hears over REST")
	void amqpConsumerCanStillHearOverRest() {
		RsaSettings callsOverAmqp = new RsaSettings("http://broker:8887/ddsr/rest", "", "", 0, "", "",
				false, "", "node-b", "1.0.0", "fennec.amqp", "promiscuous", "promiscuous", 30, 0, "",
				"tcp://mosquitto:1883", "ddsr/rpc", "", "fennec.rest");

		List<DerivedConfiguration> plan = Derivation.forConsumer(callsOverAmqp);

		assertThat(byLabel(plan, Derivation.DISCOVERY_PID).properties())
				.containsEntry("broker.url", "http://broker:8887/ddsr/rest");
		assertThat(byLabel(plan, Derivation.CLIENT_PID).properties())
				.as("a lookup filters by what the consumer says it speaks: without AMQP here the node"
						+ " is never shown a service that is reachable only that way")
				.containsEntry("supported.flavors", "AMQP,REST")
				.containsEntry("eventSource.target", "(ddsr.event.transport=rest)");
		assertThat(plan).extracting(DerivedConfiguration::label)
				.doesNotContain(Derivation.CLIENT_MQTT_PID, Derivation.DISCOVERY_AMQP_PID);
	}

	@Test
	@DisplayName("#219: the largest body is said once, on the role, and reaches the part that reads bodies")
	void maxBodyBytes() {
		assertThat(byLabel(Derivation.forProvider(provider()), Derivation.DISTRIBUTION_PID).properties())
				.as("nobody named a limit, so the default of the part applies").doesNotContainKey("max.body.bytes");
		assertThat(byLabel(Derivation.forProvider(provider().withMaxBodyBytes(8_000_000)),
				Derivation.DISTRIBUTION_PID).properties()).containsEntry("max.body.bytes", "8000000");

		List<DerivedConfiguration> overMqtt = Derivation.forProvider(mqttProvider().withMaxBodyBytes(8_000_000));
		assertThat(byLabel(overMqtt, Derivation.CONNECTION_MESSAGING_PID).properties())
				.as("over a messaging broker the connection reads the call").containsEntry("max.body.bytes", "8000000");
		assertThat(byLabel(overMqtt, Derivation.INVOKER_MESSAGING_PID).properties())
				.as("and the invoker the answer").containsEntry("max.body.bytes", "8000000");
		assertThat(Derivation.forProvider(mqttProvider())).extracting(DerivedConfiguration::label)
				.as("a node with neither a certificate nor a limit leaves its connections unconfigured")
				.doesNotContain(Derivation.CONNECTION_MESSAGING_PID, Derivation.INVOKER_MESSAGING_PID);
		assertThat(Derivation.forProvider(provider().withMaxBodyBytes(8_000_000)))
				.extracting(DerivedConfiguration::label)
				.as("a node that only speaks REST has no messaging connection to tell")
				.doesNotContain(Derivation.CONNECTION_MESSAGING_PID, Derivation.INVOKER_MESSAGING_PID);
	}
}
