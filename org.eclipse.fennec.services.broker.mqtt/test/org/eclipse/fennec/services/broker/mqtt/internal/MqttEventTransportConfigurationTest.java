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

package org.eclipse.fennec.services.broker.mqtt.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Annotation;
import java.util.Dictionary;

import org.eclipse.fennec.services.broker.core.EventSink;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;

/**
 * What an unconfigured MQTT transport does (#156).
 *
 * <p>The deployable image ships this configuration so that MQTT is one
 * environment variable away. A container that does not set it must
 * still be a healthy broker: no connection attempt, no error, and — the
 * part that matters — no {@code EventSink} offered to the broker that
 * could not deliver anything.
 */
class MqttEventTransportConfigurationTest {

	@Test
	@DisplayName("no broker URL: nothing is registered and nothing fails")
	void unconfiguredRegistersNothing() throws Exception {
		BundleContext context = mock(BundleContext.class);
		MqttEventTransport transport = new MqttEventTransport(context);

		assertThatCode(() -> transport.activate(config("")))
			.as("a REST-only container must not log a failure about a transport nobody asked for")
			.doesNotThrowAnyException();

		// The typed overload, named exactly: BundleContext has three
		// registerService methods and only this one is the sink.
		verify(context, never()).registerService(eq(EventSink.class), any(EventSink.class), nullable(Dictionary.class));
	}

	@Test
	@DisplayName("a blank URL is the same as none — a deployment that says nothing said nothing")
	void blankIsUnconfigured() throws Exception {
		BundleContext context = mock(BundleContext.class);

		new MqttEventTransport(context).activate(config("   "));

		// The typed overload, named exactly: BundleContext has three
		// registerService methods and only this one is the sink.
		verify(context, never()).registerService(eq(EventSink.class), any(EventSink.class), nullable(Dictionary.class));
	}

	private static MqttEventTransport.Config config(String url) {
		return new MqttEventTransport.Config() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return MqttEventTransport.Config.class;
			}

			@Override
			public String broker_url() {
				return url;
			}

			@Override
			public String topic_prefix() {
				return "ddsr/events";
			}

			@Override
			public String event_source() {
				return "/fennec/services/broker";
			}

			@Override
			public String client_id() {
				return "ddsr-broker";
			}

			@Override
			public int qos() {
				return 0;
			}

			@Override
			public long publish_timeout_millis() {
				return 5000;
			}
		};
	}
}
