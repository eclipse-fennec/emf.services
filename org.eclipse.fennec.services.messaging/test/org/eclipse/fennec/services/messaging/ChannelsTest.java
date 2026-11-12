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

import org.junit.jupiter.api.Test;

/** One name, spelled the way the broker behind a URL spells it (#138). */
class ChannelsTest {

	@Test
	void everyMqttSchemeKeepsTheNameAsItIsGiven() {
		for (String url : new String[] { "tcp://h:1883", "ssl://h:8883", "ws://h:80", "WSS://h:443", "", null }) {
			Channels channels = Channels.of(url);

			assertThat(channels.protocol()).as(String.valueOf(url)).isEqualTo("mqtt");
			assertThat(channels.under("ddsr/events", "Payment")).isEqualTo("ddsr/events/Payment");
			assertThat(channels.everythingUnder("ddsr/events")).isEqualTo("ddsr/events/#");
		}
	}

	@Test
	void amqpSpellsTheSameNameAsARoutingKey() {
		for (String url : new String[] { "amqp://h:5672", "AMQPS://user:secret@h:5671/vhost" }) {
			Channels channels = Channels.of(url);

			assertThat(channels.protocol()).as(url).isEqualTo("amqp");
			assertThat(channels.under("ddsr/events", "Payment")).isEqualTo("ddsr.events.Payment");
			assertThat(channels.everythingUnder("ddsr/events")).isEqualTo("ddsr.events.#");
		}
	}

	@Test
	void aChannelIsRecognisedByItsLastSegmentOnEitherSpelling() {
		assertThat(Channels.MQTT.endsWith("ddsr/events/_resync", "_resync")).isTrue();
		assertThat(Channels.AMQP.endsWith("ddsr.events._resync", "_resync")).isTrue();
		assertThat(Channels.AMQP.endsWith("ddsr.events.Payment", "_resync")).isFalse();
		assertThat(Channels.MQTT.endsWith(null, "_resync")).isFalse();
	}
}
