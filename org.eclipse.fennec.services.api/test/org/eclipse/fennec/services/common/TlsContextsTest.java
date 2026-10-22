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
package org.eclipse.fennec.services.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A node's client shows its certificate and believes its authorities
 * only when told to, and a store it was told about and cannot read is a
 * failure, not a quiet call without it (SECURITY.md §7d).
 */
class TlsContextsTest {

	@TempDir
	static Path dir;

	private static Path store;

	@BeforeAll
	static void createStore() throws Exception {
		store = dir.resolve("node.p12");
		Process keytool = new ProcessBuilder(List.of(
				Path.of(System.getProperty("java.home"), "bin", "keytool").toString(), "-genkeypair", "-alias",
				"node", "-dname", "CN=java-client,O=Fennec", "-keyalg", "EC", "-validity", "2", "-keystore",
				store.toString(), "-storetype", "PKCS12", "-storepass", "changeit", "-noprompt"))
				.redirectErrorStream(true).start();
		String output = new String(keytool.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		assertThat(keytool.waitFor()).as(output).isZero();
	}

	@Test
	void withoutStoresThereIsNoContext() {
		assertThat(TlsContexts.of("", "", "PKCS12", " ", "", "PKCS12")).isNull();
	}

	@Test
	void aKeyStoreAloneGivesAContextWithTheJvmsAuthorities() {
		assertThat(TlsContexts.of(store.toString(), "changeit", "PKCS12", "", "", "")).isNotNull();
	}

	@Test
	void bothStoresGiveOneContext() {
		assertThat(TlsContexts.of(store.toString(), "changeit", "PKCS12", store.toString(), "changeit", "PKCS12"))
				.isNotNull();
	}

	@Test
	void theCommonNameIsTheNodesName() {
		assertThat(TlsContexts.commonName(store.toString(), "changeit", "PKCS12")).isEqualTo("java-client");
		assertThat(TlsContexts.commonName("", "", "PKCS12")).as("no store, no name").isNull();
	}

	@Test
	void aStoreThatCannotBeReadFailsLoudly() {
		assertThatThrownBy(() -> TlsContexts.of(dir.resolve("missing.p12").toString(), "changeit", "PKCS12", "", "",
				""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("missing.p12");
		assertThatThrownBy(() -> TlsContexts.of(store.toString(), "wrong", "PKCS12", "", "", ""))
				.isInstanceOf(IllegalStateException.class);
	}
}
