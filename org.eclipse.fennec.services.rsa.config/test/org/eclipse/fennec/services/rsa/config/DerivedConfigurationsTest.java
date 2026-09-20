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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The order in which a node's configurations appear and disappear.
 *
 * <p>Both directions matter. Forwards, because a topology manager that
 * starts exporting before the transports are configured exports into
 * nothing. Backwards, because withdrawing an export needs the transport
 * that carries the withdrawal to still be up (FR-P3), and bundle stop
 * order is not something a deployment should have to know.
 */
class DerivedConfigurationsTest {

	private static final DerivedConfiguration FIRST =
			DerivedConfiguration.of("first", Map.of("a", "1"));

	private static final DerivedConfiguration SECOND =
			DerivedConfiguration.ofFactory("second", "name", Map.of("b", "2"));

	private static final DerivedConfiguration THIRD =
			DerivedConfiguration.of("third", Map.of("c", "3"));

	@Test
	@DisplayName("writes in plan order")
	void writesInOrder() throws IOException {
		FakeConfigurationAdmin admin = new FakeConfigurationAdmin();

		try (DerivedConfigurations derived = new DerivedConfigurations(admin, "a node")) {
			derived.apply(List.of(FIRST, SECOND, THIRD));
		}

		assertThat(admin.journal).startsWith("write first", "write second~name", "write third");
	}

	@Test
	@DisplayName("takes them back last written first, so the last thing configured is the first thing to stop")
	void removesInReverse() throws IOException {
		FakeConfigurationAdmin admin = new FakeConfigurationAdmin();

		DerivedConfigurations derived = new DerivedConfigurations(admin, "a node");
		derived.apply(List.of(FIRST, SECOND, THIRD));
		derived.close();

		assertThat(admin.journal)
				.containsSubsequence("delete third", "delete second~name", "delete first");
		assertThat(admin.contents()).isEmpty();
	}

	@Test
	@DisplayName("does not rewrite what already says the same thing")
	void unchangedIsLeftAlone() throws IOException {
		FakeConfigurationAdmin admin = new FakeConfigurationAdmin();

		try (DerivedConfigurations derived = new DerivedConfigurations(admin, "a node")) {
			derived.apply(List.of(FIRST, SECOND));
			admin.journal.clear();
			derived.apply(List.of(FIRST, SECOND));

			assertThat(admin.journal).isEmpty();
		}
	}

	@Test
	@DisplayName("a changed value is written, and only that one")
	void onlyTheChangedOneIsWritten() throws IOException {
		FakeConfigurationAdmin admin = new FakeConfigurationAdmin();

		try (DerivedConfigurations derived = new DerivedConfigurations(admin, "a node")) {
			derived.apply(List.of(FIRST, SECOND));
			admin.journal.clear();
			derived.apply(List.of(FIRST, DerivedConfiguration.ofFactory("second", "name", Map.of("b", "9"))));

			assertThat(admin.journal).containsExactly("write second~name");
		}
	}

	@Test
	@DisplayName("what a new plan no longer contains is taken back")
	void droppedEntriesAreRemoved() throws IOException {
		FakeConfigurationAdmin admin = new FakeConfigurationAdmin();

		try (DerivedConfigurations derived = new DerivedConfigurations(admin, "a node")) {
			derived.apply(List.of(FIRST, SECOND, THIRD));
			admin.journal.clear();
			derived.apply(List.of(FIRST));

			assertThat(admin.journal).containsExactly("delete third", "delete second~name");
			assertThat(admin.contents()).containsOnlyKeys("first");
		}
	}

	@Test
	@DisplayName("a provider's plan ends with the topology manager and therefore starts unwinding there")
	void providerUnwindsFromTheTopologyManager() throws IOException {
		FakeConfigurationAdmin admin = new FakeConfigurationAdmin();
		RsaSettings settings = new RsaSettings("http://broker:8887/ddsr/rest", "", "", 9095, "0.0.0.0",
				"services", true, "ddsrHttp", "node-a", "1.0.0", "fennec.rest", "promiscuous",
				"promiscuous", 30, 0, "", "tcp://localhost:1883", "ddsr/rpc", "", "");

		DerivedConfigurations derived = new DerivedConfigurations(admin, "RSA provider");
		derived.apply(Derivation.forProvider(settings));
		admin.journal.clear();
		derived.close();

		assertThat(admin.journal.get(0)).isEqualTo("delete " + Derivation.TOPOLOGY_PID);
		assertThat(admin.journal)
				.containsSubsequence("delete " + Derivation.TOPOLOGY_PID,
						"delete org.eclipse.fennec.services.rsa~fennec-rest",
						"delete " + Derivation.DISCOVERY_PID,
						"delete " + Derivation.CLIENT_REST_PID);
	}
}
