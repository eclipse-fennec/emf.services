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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

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
				Map.entry("cloudEvents", "structured"),
				Map.entry("cleanSession", Boolean.TRUE));
	}

	@Test
	void aConfiguredClientIdIsTakenAsItIs() {
		BundleContext framework = frameworkWithUuid("0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0");

		assertThat(LinkSettings.clientId(framework, "billing-node", "ddsr-consumer")).isEqualTo("billing-node");
	}

	/**
	 * The reason the default is not a fixed name: a broker disconnects
	 * the older of two connections with one client id, so two nodes on
	 * the default kept throwing each other out.
	 */
	@Test
	void withoutOneTwoFrameworksNeverShareAClientId() {
		BundleContext one = frameworkWithUuid("0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0");
		BundleContext other = frameworkWithUuid("a1b2c3d4-0000-1111-2222-333344445555");

		assertThat(LinkSettings.clientId(one, "", "ddsr-consumer")).isEqualTo("ddsr-consumer-0f1e2d3c");
		assertThat(LinkSettings.clientId(one, null, "ddsr-consumer")).isEqualTo("ddsr-consumer-0f1e2d3c");
		assertThat(LinkSettings.clientId(other, " ", "ddsr-consumer")).isEqualTo("ddsr-consumer-a1b2c3d4");
		// the same framework, another role: still two ids
		assertThat(LinkSettings.clientId(one, "", "ddsr-broker")).isEqualTo("ddsr-broker-0f1e2d3c");
	}

	@Test
	void aContextWithoutAFrameworkUuidStillGetsAnIdOfItsOwn() {
		BundleContext bare = frameworkWithUuid(null);

		String first = LinkSettings.clientId(bare, "", "ddsr-consumer");
		String second = LinkSettings.clientId(bare, "", "ddsr-consumer");

		assertThat(first).matches("ddsr-consumer-[0-9a-f]{8}");
		assertThat(second).isNotEqualTo(first);
	}

	private static BundleContext frameworkWithUuid(String uuid) {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(Constants.FRAMEWORK_UUID)).thenReturn(uuid);
		return context;
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
		assertThatThrownBy(() -> LinkSettings.of("n", "kafka://h:9092", null).factoryPid())
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("kafka");
		assertThatThrownBy(() -> LinkSettings.of("n", "localhost:1883", null).factoryPid())
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void anAmqpUrlIsSpokenByTheAmqpBindingOnItsExchange() {
		LinkSettings link = LinkSettings.of("fennec.calls", "amqp://localhost:5672", "node-1")
				.onExchange("calls");

		assertThat(link.factoryPid()).isEqualTo("org.gecko.messaging.amqp");
		assertThat(link.configuration()).containsOnly(
				Map.entry("osgi.messaging.name", "fennec.calls"),
				Map.entry("uri", "amqp://localhost:5672"),
				Map.entry("clientId", "node-1"),
				Map.entry("cloudEvents", "structured"),
				Map.entry("exchange", "calls"));
	}

	@Test
	void aNameAndPasswordTravelBesideTheUrlThePasswordPrivate() {
		Map<String, Object> configuration = LinkSettings.of("n", "amqp://rabbit:5672", null)
				.as("payments", "secret").configuration();

		assertThat(configuration).containsEntry("username", "payments").containsEntry(".password", "secret")
				.doesNotContainKey("password");
		assertThat(LinkSettings.of("n", "amqp://rabbit:5672", null).as("", null).configuration())
				.doesNotContainKeys("username", ".password");
	}

	@Test
	void withoutAnExchangeTheBindingKeepsItsOwn() {
		assertThat(LinkSettings.of("n", "amqp://h:5672", null).configuration())
				.doesNotContainKeys("exchange", "cleanSession");
	}

	@Test
	void amqpsCarriesTheStores() {
		Map<String, Object> configuration = new LinkSettings("n", "amqps://h:5671", "id", "/k.p12", "kpw",
				"/t.p12", "tpw").configuration();

		assertThat(configuration).containsKeys("tls.keystore", ".tls.keystore.password", "tls.truststore",
				".tls.truststore.password");
	}

	@Test
	void theCredentialsOfAnAmqpUrlStayOutOfLogLines() {
		LinkSettings link = LinkSettings.of("n", "amqp://guest:secret@rabbit:5672/vhost", null);

		assertThat(link.address()).isEqualTo("amqp://rabbit:5672/vhost");
		assertThat(link).hasToString("n → amqp://rabbit:5672/vhost");
		// An '@' in a path is not user info.
		assertThat(LinkSettings.of("n", "amqp://rabbit:5672/a@b", null).address())
				.isEqualTo("amqp://rabbit:5672/a@b");
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

	@Test
	void aLinkThatNamesNoSizeLeavesTheInstanceItsOwnLimit() {
		assertThat(LinkSettings.of("n", "tcp://h:1883", null).configuration())
				.doesNotContainKey("maxMessageSize");
		assertThat(LinkSettings.of("n", "tcp://h:1883", null).carrying(0).configuration())
				.doesNotContainKey("maxMessageSize");
	}

	@Test
	void theMessageIsLargerThanTheDocumentItCarries() {
		long document = 3L * 1024 * 1024;

		for (String url : List.of("tcp://h:1883", "amqp://h:5672")) {
			Map<String, Object> configuration = LinkSettings.of("n", url, null).carrying(document)
					.onExchange("calls").as("user", "secret").configuration();

			assertThat(configuration.get("maxMessageSize")).as(url
					+ ": a document that is not text travels as base64, four bytes for three, inside an envelope")
					.isEqualTo(4L * 1024 * 1024 + LinkSettings.ENVELOPE_BYTES);
		}
	}
}
