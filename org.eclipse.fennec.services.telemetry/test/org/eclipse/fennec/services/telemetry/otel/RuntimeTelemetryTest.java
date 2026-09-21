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

package org.eclipse.fennec.services.telemetry.otel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.fennec.services.runtime.BrokerRuntime;
import org.eclipse.fennec.services.runtime.BrokerRuntimeDTO;
import org.eclipse.fennec.services.runtime.DeliveryDTO;
import org.eclipse.fennec.services.runtime.RegistrationDTO;
import org.eclipse.fennec.services.runtime.SessionDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;

/**
 * What a watcher sees of a node it never reaches into (#126).
 *
 * <p>The assertion that matters is that a gauge is read <em>when the
 * exporter collects</em> and not when the runtime changed: a number
 * recorded at change time stops being true between changes, and a
 * runtime that goes quiet would export a stale reading forever.
 */
class RuntimeTelemetryTest {

	/** Collects on demand, which is what a test needs and a period is not. */
	private static final class OnDemand implements MetricReader {

		private CollectionRegistration collection = CollectionRegistration.noop();

		@Override
		public void register(CollectionRegistration registration) {
			this.collection = registration;
		}

		@Override
		public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
			return AggregationTemporality.CUMULATIVE;
		}

		@Override
		public CompletableResultCode forceFlush() {
			return CompletableResultCode.ofSuccess();
		}

		@Override
		public CompletableResultCode shutdown() {
			return CompletableResultCode.ofSuccess();
		}

		List<MetricData> collect() {
			return List.copyOf(collection.collectAllMetrics());
		}
	}

	/** A broker whose answer the test moves between collections. */
	private static final class FakeBroker implements BrokerRuntime {

		private final AtomicInteger asked = new AtomicInteger();

		private int registrations = 1;

		@Override
		public BrokerRuntimeDTO snapshot() {
			asked.incrementAndGet();
			BrokerRuntimeDTO dto = new BrokerRuntimeDTO();
			dto.name = "registry";
			dto.changeCount = 7;
			dto.coldEntries = 2;
			dto.registrations = registrationsOf(registrations);
			dto.catalog = List.of();
			dto.sessions = List.of(session("consumer-1"), session("consumer-2"));
			dto.delivery = new DeliveryDTO();
			dto.delivery.dropped = 3;
			return dto;
		}

		private static List<RegistrationDTO> registrationsOf(int count) {
			RegistrationDTO one = new RegistrationDTO();
			one.heldBy = List.of("consumer-1", "consumer-2");
			RegistrationDTO two = new RegistrationDTO();
			two.heldBy = List.of();
			return count == 1 ? List.of(one) : List.of(one, two);
		}

		private static SessionDTO session(String consumerId) {
			SessionDTO session = new SessionDTO();
			session.consumerId = consumerId;
			return session;
		}
	}

	private OnDemand reader;

	private SdkMeterProvider meters;

	private RuntimeTelemetry telemetry;

	@BeforeEach
	void setUp() {
		reader = new OnDemand();
		meters = SdkMeterProvider.builder().registerMetricReader(reader).build();
		telemetry = new RuntimeTelemetry(meters);
		telemetry.configure(Configs.defaults());
	}

	@AfterEach
	void tearDown() {
		telemetry.deactivate();
		meters.close();
	}

	@Test
	@DisplayName("a bound broker becomes gauges, and they say what the snapshot says")
	void brokerBecomesGauges() {
		FakeBroker broker = new FakeBroker();
		telemetry.setBroker(broker, Map.of());

		Map<String, Long> readings = readingsOf(reader.collect());

		assertThat(readings).containsEntry("fennec.services.broker.registrations", 1L);
		assertThat(readings).containsEntry("fennec.services.broker.sessions", 2L);
		assertThat(readings)
			.as("the leases, which is the thing no consumer of the wire can count")
			.containsEntry("fennec.services.broker.leases", 2L);
		assertThat(readings).containsEntry("fennec.services.broker.events.dropped", 3L);
		assertThat(readings).containsEntry("fennec.services.broker.cold.entries", 2L);
		assertThat(readings).containsEntry("fennec.services.broker.changes", 7L);
	}

	@Test
	@DisplayName("the gauge is read at collection time, so a changed broker needs no announcement")
	void gaugesAreObserved() {
		FakeBroker broker = new FakeBroker();
		telemetry.setBroker(broker, Map.of());
		reader.collect();
		int askedOnce = broker.asked.get();

		broker.registrations = 2;
		Map<String, Long> second = readingsOf(reader.collect());

		assertThat(second).containsEntry("fennec.services.broker.registrations", 2L);
		assertThat(broker.asked.get())
			.as("asked again at the second collection, without anything having told this component")
			.isGreaterThan(askedOnce);
	}

	@Test
	@DisplayName("a runtime bound before the configuration arrives is still instrumented")
	void boundBeforeConfigured() {
		// What DS actually does: bind first, activate second. A component
		// that takes its meter in the activate method has none while the
		// first service is being bound — and in a framework that starts
		// everything at once, that is every service.
		RuntimeTelemetry beforeConfigure = new RuntimeTelemetry(meters);
		beforeConfigure.setBroker(new FakeBroker(), Map.of());
		beforeConfigure.configure(Configs.defaults());

		assertThat(readingsOf(reader.collect()))
			.containsEntry("fennec.services.broker.registrations", 1L);
		beforeConfigure.deactivate();
	}

	@Test
	@DisplayName("a broker that goes away takes its gauges with it")
	void unboundStopsReporting() {
		FakeBroker broker = new FakeBroker();
		telemetry.setBroker(broker, Map.of());
		assertThat(readingsOf(reader.collect())).isNotEmpty();

		telemetry.unsetBroker(broker);

		assertThat(readingsOf(reader.collect()))
			.as("a gauge over a service that is gone would report a number nobody stands behind")
			.isEmpty();
	}

	private static Map<String, Long> readingsOf(List<MetricData> metrics) {
		return metrics.stream()
				.filter(metric -> !metric.getLongGaugeData().getPoints().isEmpty())
				.collect(Collectors.toMap(MetricData::getName,
						metric -> metric.getLongGaugeData().getPoints().iterator().next().getValue()));
	}

}
