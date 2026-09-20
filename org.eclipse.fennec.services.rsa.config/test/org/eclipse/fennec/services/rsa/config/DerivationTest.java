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
				30, 0, "");
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
				"node-a", "1.0.0", "fennec.rest", "promiscuous", "promiscuous", 30, 0, "");

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
				"node-a", "1.0.0", "fennec.rest", "promiscuous", "promiscuous", 30, 0, "");

		assertThat(Derivation.forProvider(borrowed).stream().map(DerivedConfiguration::label))
				.doesNotContain("org.apache.felix.http~ddsrHttp",
						"JakartarsServletWhiteboardRuntimeComponent~ddsrHttp");
	}

	@Test
	@DisplayName("a consumer says it exports nothing instead of pointing at a distribution that is missing")
	void consumerSaysItExportsNothing() {
		RsaSettings settings = new RsaSettings("http://broker:8887/ddsr/rest", "", "", 0, "", "",
				false, "", "node-b", "1.0.0", "fennec.rest", "promiscuous", "promiscuous", 30, 0, "node-b");

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
}
