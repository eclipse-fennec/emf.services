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

import org.eclipse.fennec.services.runtime.BindingDTO;
import org.eclipse.fennec.services.runtime.BrokerRuntime;
import org.eclipse.fennec.services.runtime.BrokerRuntimeDTO;
import org.eclipse.fennec.services.runtime.ClientRuntime;
import org.eclipse.fennec.services.runtime.ClientRuntimeDTO;
import org.eclipse.fennec.services.runtime.DeliveryDTO;
import org.eclipse.fennec.services.runtime.RegistrationDTO;
import org.eclipse.fennec.services.runtime.SessionDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.LongPointData;
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
			one.referenceId = "ref-1";
			one.providerName = "payments";
			one.implementationId = "payments-rest";
			one.contracts = List.of("Payment", "Refund");
			one.flavors = List.of("REST", "MQTT");
			one.heldBy = List.of("consumer-1", "consumer-2");
			RegistrationDTO two = new RegistrationDTO();
			two.referenceId = "ref-2";
			two.contracts = List.of("Payment");
			two.heldBy = List.of();
			return count == 1 ? List.of(one) : List.of(one, two);
		}

		private static SessionDTO session(String consumerId) {
			SessionDTO session = new SessionDTO();
			session.consumerId = consumerId;
			// Only the first one says where it came from, so the lease
			// series has to cope with a session that does not.
			session.origin = "consumer-1".equals(consumerId) ? "shop-prod/5f2b8c1e" : null;
			return session;
		}
	}

	/** A client runtime with two locators on the same registration and one rebinding. */
	private static final class FakeClient implements ClientRuntime {

		@Override
		public ClientRuntimeDTO snapshot() {
			ClientRuntimeDTO dto = new ClientRuntimeDTO();
			dto.consumerId = "shop";
			dto.published = List.of();
			dto.bindings = List.of(binding("Payment", "ref-1", "LIVE"), binding("Payment", "ref-1", "LIVE"),
					binding("Refund", null, "REBIND"));
			return dto;
		}

		private static BindingDTO binding(String contract, String referenceId, String state) {
			BindingDTO binding = new BindingDTO();
			binding.contract = contract;
			binding.referenceId = referenceId;
			binding.state = state;
			return binding;
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

	@Test
	@DisplayName("the broker reports one series per registration and contract, carrying the provider side (#166)")
	void registrationSeries() {
		telemetry.setBroker(new FakeBroker(), Map.of());

		List<LongPointData> points = pointsOf(reader.collect(), "fennec.services.broker.registration");

		assertThat(points).hasSize(2).allSatisfy(point -> assertThat(point.getValue()).isEqualTo(1L));
		assertThat(points).extracting(point -> attribute(point, "rpc.service"))
				.containsExactlyInAnyOrder("Payment", "Refund");
		assertThat(points).allSatisfy(point -> {
			assertThat(attribute(point, "fennec.node")).isEqualTo("registry");
			assertThat(attribute(point, "fennec.reference")).isEqualTo("ref-1");
			assertThat(attribute(point, "fennec.provider")).isEqualTo("payments");
			assertThat(attribute(point, "fennec.implementation")).isEqualTo("payments-rest");
			assertThat(attribute(point, "fennec.flavor")).isEqualTo("REST,MQTT");
		});
	}

	@Test
	@DisplayName("the broker reports one series per lease, with the holder's origin where the session has one")
	void leaseSeries() {
		telemetry.setBroker(new FakeBroker(), Map.of());

		List<LongPointData> points = pointsOf(reader.collect(), "fennec.services.broker.lease");

		assertThat(points).hasSize(2);
		assertThat(points).extracting(point -> attribute(point, "fennec.consumer"))
				.containsExactlyInAnyOrder("consumer-1", "consumer-2");
		assertThat(points).allSatisfy(point -> {
			assertThat(attribute(point, "fennec.reference")).isEqualTo("ref-1");
			assertThat(attribute(point, "fennec.provider")).as("the provider lives on the registration series")
					.isNull();
		});
		assertThat(points).filteredOn(point -> "consumer-1".equals(attribute(point, "fennec.consumer")))
				.singleElement()
				.satisfies(point -> assertThat(attribute(point, "fennec.origin")).isEqualTo("shop-prod/5f2b8c1e"));
		assertThat(points).filteredOn(point -> "consumer-2".equals(attribute(point, "fennec.consumer")))
				.singleElement()
				.satisfies(point -> assertThat(attribute(point, "fennec.origin")).as("absent, not \"null\"").isNull());
	}

	@Test
	@DisplayName("a registration that ends is not reported at the next collection, rather than kept at its last value")
	void anEndedRegistrationDisappears() {
		FakeBroker broker = new FakeBroker();
		broker.registrations = 2;
		telemetry.setBroker(broker, Map.of());
		assertThat(pointsOf(reader.collect(), "fennec.services.broker.registration")).hasSize(3);

		broker.registrations = 1;

		assertThat(pointsOf(reader.collect(), "fennec.services.broker.registration"))
				.extracting(point -> attribute(point, "fennec.reference"))
				.as("ref-2 is gone from the snapshot, so it is gone from the series").containsOnly("ref-1");
	}

	@Test
	@DisplayName("the client reports its bindings, counting two locators on one registration once with value 2")
	void bindingSeries() {
		telemetry.setClient(new FakeClient(), Map.of());

		List<LongPointData> points = pointsOf(reader.collect(), "fennec.services.client.binding");

		assertThat(points).hasSize(2);
		assertThat(points).filteredOn(point -> "Payment".equals(attribute(point, "rpc.service")))
				.singleElement()
				.satisfies(point -> {
					assertThat(point.getValue()).isEqualTo(2L);
					assertThat(attribute(point, "fennec.reference")).isEqualTo("ref-1");
					assertThat(attribute(point, "fennec.state")).isEqualTo("LIVE");
					assertThat(attribute(point, "fennec.node")).isEqualTo("shop");
				});
		assertThat(points).filteredOn(point -> "Refund".equals(attribute(point, "rpc.service")))
				.singleElement()
				.satisfies(point -> {
					assertThat(attribute(point, "fennec.reference")).as("rebinding has no registration").isNull();
					assertThat(attribute(point, "fennec.state")).isEqualTo("REBIND");
				});
	}

	@Test
	@DisplayName("with relationships off only the counts are reported, and switching back brings the series back")
	void relationshipsCanBeTurnedOff() {
		telemetry.setBroker(new FakeBroker(), Map.of());
		telemetry.setClient(new FakeClient(), Map.of());

		telemetry.configure(Configs.relationships(false));
		List<MetricData> off = reader.collect();

		assertThat(readingsOf(off)).containsEntry("fennec.services.broker.leases", 2L)
				.containsEntry("fennec.services.client.bindings", 3L)
				.doesNotContainKeys("fennec.services.broker.registration", "fennec.services.broker.lease",
						"fennec.services.client.binding");

		telemetry.configure(Configs.relationships(true));

		assertThat(pointsOf(reader.collect(), "fennec.services.broker.lease")).hasSize(2);
	}

	private static List<LongPointData> pointsOf(List<MetricData> metrics, String name) {
		return metrics.stream()
				.filter(metric -> metric.getName().equals(name))
				.flatMap(metric -> metric.getLongGaugeData().getPoints().stream())
				.toList();
	}

	private static String attribute(LongPointData point, String key) {
		Attributes attributes = point.getAttributes();
		return attributes.get(AttributeKey.stringKey(key));
	}

	private static Map<String, Long> readingsOf(List<MetricData> metrics) {
		return metrics.stream()
				.filter(metric -> !metric.getLongGaugeData().getPoints().isEmpty())
				.collect(Collectors.toMap(MetricData::getName,
						metric -> metric.getLongGaugeData().getPoints().iterator().next().getValue()));
	}

}
