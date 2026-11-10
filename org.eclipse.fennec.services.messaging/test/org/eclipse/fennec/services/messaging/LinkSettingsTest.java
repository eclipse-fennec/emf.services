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

package org.eclipse.fennec.services.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * What a link asks of the messaging instance behind it. The instance
 * itself is the messaging project's to test; what is ours is the
 * configuration we hand it.
 */
class LinkSettingsTest {

	@Test
	void aPlainBrokerGetsNameAddressAndIdentity() {
		Map<String, Object> configuration = LinkSettings.of("fennec.broker", "tcp://localhost:1883",
				"ddsr-broker").configuration();

		assertThat(configuration).containsOnly(
				Map.entry("osgi.messaging.name", "fennec.broker"),
				Map.entry("uri", "tcp://localhost:1883"),
				Map.entry("clientId", "ddsr-broker"),
				Map.entry("cleanSession", Boolean.TRUE));
	}

	@Test
	void everyMqttSchemeIsSpokenByTheMqtt3Binding() {
		for (String url : new String[] { "tcp://h:1883", "ssl://h:8883", "ws://h:80", "WSS://h:443" }) {
			assertThat(LinkSettings.of("n", url, null).factoryPid()).as(url)
					.isEqualTo("org.gecko.messaging.mqtt3");
		}
	}

	@Test
	void aSchemeNoBindingSpeaksIsRefusedBeforeAnythingIsWritten() {
		assertThatThrownBy(() -> LinkSettings.of("n", "amqp://h:5672", null).factoryPid())
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("amqp");
		assertThatThrownBy(() -> LinkSettings.of("n", "localhost:1883", null).factoryPid())
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void theStoresTravelOnATlsUrlWithTheirPasswordsPrivate() {
		Map<String, Object> configuration = new LinkSettings("n", "ssl://h:8883", "id", "/k.p12", "kpw",
				"/t.p12", "tpw").configuration();

		assertThat(configuration)
				.containsEntry("tls.keystore", "/k.p12")
				.containsEntry(".tls.keystore.password", "kpw")
				.containsEntry("tls.truststore", "/t.p12")
				.containsEntry(".tls.truststore.password", "tpw");
		// A key that starts with a dot stays out of the instance's service
		// properties; the same password under a public key would not.
		assertThat(configuration).doesNotContainKeys("tls.keystore.password", "tls.truststore.password");
	}

	@Test
	void eitherStoreAloneIsEnough() {
		Map<String, Object> configuration = new LinkSettings("n", "ssl://h:8883", "id", "", "", "/t.p12",
				"tpw").configuration();

		assertThat(configuration).containsKey("tls.truststore").doesNotContainKeys("tls.keystore",
				".tls.keystore.password");
	}

	@Test
	void aPlainUrlCarriesNoStoresEvenWhenTheNodeHasThem() {
		// The instance refuses stores on a URL without TLS. A node's
		// certificate sits in a configuration shared by all its
		// connections, and dialling a plain broker with it is not an error.
		Map<String, Object> configuration = new LinkSettings("n", "tcp://h:1883", "id", "/k.p12", "kpw",
				"/t.p12", "tpw").configuration();

		assertThat(configuration.keySet()).noneMatch(key -> key.contains("tls"));
	}

	@Test
	void aLinkNeedsANameAndAnAddress() {
		assertThatThrownBy(() -> LinkSettings.of(" ", "tcp://h:1883", null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> LinkSettings.of("n", "", null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void whatALogLinePrintsCarriesNoPassword() {
		assertThat(new LinkSettings("n", "ssl://h:8883", "id", "/k.p12", "secret", null, "other"))
				.hasToString("n → ssl://h:8883");
	}
}
