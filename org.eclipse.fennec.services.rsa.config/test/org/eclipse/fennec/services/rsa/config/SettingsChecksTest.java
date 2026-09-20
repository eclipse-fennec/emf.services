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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What a node says before anything comes up, and what it only
 * mentions.
 */
class SettingsChecksTest {

	private static RsaSettings with(String brokerUrl, String policy, String registryName) {
		return new RsaSettings(brokerUrl, "", "", 9095, "0.0.0.0", "services", true, "ddsrHttp",
				registryName, "1.0.0", "fennec.rest", policy, "promiscuous", 30, 0, "", "tcp://localhost:1883", "ddsr/rpc", "", "");
	}

	@Test
	@DisplayName("a usable provider configuration has nothing to report")
	void silentWhenSound() {
		assertThat(SettingsChecks.problems(with("http://broker:8887/ddsr/rest", "promiscuous", "node-a"), true))
				.isEmpty();
	}

	@Test
	@DisplayName("a broker URL that is not a URL is named, not swallowed")
	void brokerUrlChecked() {
		assertThat(SettingsChecks.problems(with("broker:8887", "promiscuous", "node-a"), true))
				.anySatisfy(problem -> assertThat(problem).contains("broker.url").contains("absolute"));
	}

	@Test
	@DisplayName("an unknown policy is named together with the ones that exist")
	void policyChecked() {
		assertThat(SettingsChecks.problems(with("http://broker:8887/ddsr/rest", "eager", "node-a"), true))
				.anySatisfy(problem -> assertThat(problem).contains("eager").contains("promiscuous"));
	}

	@Test
	@DisplayName("an empty registry name is a problem, because it is what services are registered under")
	void registryNameChecked() {
		assertThat(SettingsChecks.problems(with("http://broker:8887/ddsr/rest", "promiscuous", " "), true))
				.anySatisfy(problem -> assertThat(problem).contains("registry.name"));
	}

	@Test
	@DisplayName("a consumer is not asked about ports it does not open")
	void consumerHasNoPort() {
		RsaSettings consumer = new RsaSettings("http://broker:8887/ddsr/rest", "", "", 0, "", "",
				false, "", "node-b", "1.0.0", "fennec.rest", "promiscuous", "promiscuous", 30, 0, "node-b", "tcp://localhost:1883", "ddsr/rpc", "", "");

		assertThat(SettingsChecks.problems(consumer, false)).isEmpty();
	}

	@Test
	@DisplayName("a public URL on another port is mentioned, not refused — that is what a proxy looks like")
	void mismatchIsMentioned() {
		RsaSettings proxied = new RsaSettings("http://broker:8887/ddsr/rest",
				"https://edge.example.org:443/api", "", 9095, "0.0.0.0", "services", true, "ddsrHttp",
				"node-a", "1.0.0", "fennec.rest", "promiscuous", "promiscuous", 30, 0, "", "tcp://localhost:1883", "ddsr/rpc", "", "");

		assertThat(SettingsChecks.problems(proxied, true)).isEmpty();
		assertThat(SettingsChecks.mismatches(proxied))
				.anySatisfy(note -> assertThat(note).contains("443").contains("9095"))
				.anySatisfy(note -> assertThat(note).contains("api").contains("services"));
	}

	@Test
	@DisplayName("a derived public URL cannot disagree with the stack it was derived from")
	void derivedNeverMismatches() {
		assertThat(SettingsChecks.mismatches(with("http://broker:8887/ddsr/rest", "promiscuous", "node-a")))
				.isEmpty();
	}
}
