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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.logging.Logger;

import org.eclipse.fennec.services.runtime.BindingDTO;
import org.eclipse.fennec.services.runtime.BrokerRuntime;
import org.eclipse.fennec.services.runtime.BrokerRuntimeDTO;
import org.eclipse.fennec.services.runtime.ClientRuntime;
import org.eclipse.fennec.services.runtime.ClientRuntimeDTO;
import org.eclipse.fennec.services.runtime.PublishedDTO;
import org.eclipse.fennec.services.runtime.RegistrationDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.metrics.ObservableLongGauge;

/**
 * What a node holds, as metrics (#126).
 *
 * <p>This is the bundle the runtime services were built for. It binds
 * whatever is in the framework — a broker, a client runtime, both, or
 * neither — and turns each into a handful of gauges. It reaches into
 * nothing: everything here comes from a DTO that the node itself
 * decided to hand out.
 *
 * <p><strong>Gauges are observed, not pushed.</strong> An instrument
 * asks its runtime for a snapshot when the exporter collects, which is
 * the OpenTelemetry idiom and the honest one: a number recorded when it
 * changed would be a number that stops being true between changes, and
 * a runtime that goes quiet would export a stale reading forever.
 *
 * <p>The change count is still worth having, and it is what the
 * {@code modified} callback is for: it says <em>how often</em> the node
 * changed, which no gauge over the current state can answer.
 */
// Same configuration as the tracer, one scope for the whole bundle. The
// object class definition is declared there and only there — two
// components may share a pid, but only one may describe it.
@Component(configurationPid = "org.eclipse.fennec.services.telemetry", immediate = true)
public class RuntimeTelemetry {

	private static final Logger LOG = Logger.getLogger(RuntimeTelemetry.class.getName());

	private static final AttributeKey<String> NODE = AttributeKey.stringKey("fennec.node");

	@Reference
	MeterProvider meters;

	/** The instruments of one watched runtime, closed when it goes. */
	private final Map<Object, List<ObservableLongGauge>> watched = new ConcurrentHashMap<>();

	private volatile Meter meter;

	@Activate
	@Modified
	void configure(TelemetryConfig config) {
		this.meter = meters.get(config.scope());
	}

	@Deactivate
	void deactivate() {
		for (Object runtime : List.copyOf(watched.keySet())) {
			close(runtime);
		}
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void setBroker(BrokerRuntime broker, Map<String, Object> properties) {
		BrokerRuntimeDTO first = broker.snapshot();
		Attributes node = Attributes.of(NODE, first.name == null ? "broker" : first.name);
		List<ObservableLongGauge> gauges = new ArrayList<>();
		gauges.add(gauge("fennec.services.broker.registrations", "{registration}",
				"Live registrations this broker holds.", node, () -> broker.snapshot(),
				snapshot -> size(snapshot.registrations)));
		gauges.add(gauge("fennec.services.broker.catalog.entries", "{contract}",
				"Contracts in this broker's catalogue.", node, () -> broker.snapshot(),
				snapshot -> size(snapshot.catalog)));
		gauges.add(gauge("fennec.services.broker.sessions", "{session}",
				"Consumer sessions this broker is holding.", node, () -> broker.snapshot(),
				snapshot -> size(snapshot.sessions)));
		gauges.add(gauge("fennec.services.broker.leases", "{lease}",
				"Claims consumers hold on registrations — what a graceful handover waits for.", node,
				() -> broker.snapshot(), RuntimeTelemetry::leases));
		gauges.add(gauge("fennec.services.broker.events.dropped", "{event}",
				"Events the broker could not deliver.", node, () -> broker.snapshot(),
				snapshot -> snapshot.delivery == null ? 0 : snapshot.delivery.dropped));
		gauges.add(gauge("fennec.services.broker.cold.entries", "{registration}",
				"Registrations in the cold cache.", node, () -> broker.snapshot(),
				snapshot -> snapshot.coldEntries));
		gauges.add(gauge("fennec.services.broker.changes", "{change}",
				"How often this broker's state has changed since it started.", node,
				() -> broker.snapshot(), snapshot -> snapshot.changeCount));
		watched.put(broker, gauges);
		LOG.info("[DDSR] watching broker '" + first.name + "'");
	}

	/**
	 * The broker said something changed.
	 *
	 * <p>Nothing to re-register — the gauges read the current snapshot
	 * on collection. What this is for is the record that a change
	 * happened at all, which the gauges cannot show: a broker that gains
	 * and loses a registration between two collections looks unchanged
	 * to every one of them.
	 *
	 * <p>Named {@code updated}, not {@code modified}: DS calls the first
	 * one when a <em>bound service's properties</em> change and the
	 * second one when the <em>component's configuration</em> does. A
	 * watcher that writes {@code modified} here compiles, binds, and is
	 * never told anything.
	 */
	void updatedBroker(BrokerRuntime broker, Map<String, Object> properties) {
		LOG.fine(() -> "[DDSR] broker changed, now at " + properties.get("service.changecount"));
	}

	void unsetBroker(BrokerRuntime broker) {
		close(broker);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void setClient(ClientRuntime client, Map<String, Object> properties) {
		ClientRuntimeDTO first = client.snapshot();
		Attributes node = Attributes.of(NODE, first.consumerId == null ? "client" : first.consumerId);
		List<ObservableLongGauge> gauges = new ArrayList<>();
		gauges.add(gauge("fennec.services.client.published", "{implementation}",
				"Implementations this runtime published.", node, () -> client.snapshot(),
				snapshot -> size(snapshot.published)));
		gauges.add(gauge("fennec.services.client.published.live", "{implementation}",
				"Of those, the ones the broker is still holding.", node, () -> client.snapshot(),
				RuntimeTelemetry::live));
		gauges.add(gauge("fennec.services.client.bindings", "{binding}",
				"Contracts this runtime is bound to.", node, () -> client.snapshot(),
				snapshot -> size(snapshot.bindings)));
		gauges.add(gauge("fennec.services.client.bindings.rebinding", "{binding}",
				"Of those, the ones whose provider went away.", node, () -> client.snapshot(),
				RuntimeTelemetry::rebinding));
		gauges.add(gauge("fennec.services.client.stream.connected", "{stream}",
				"1 while this runtime is reading the event stream, 0 otherwise.", node,
				() -> client.snapshot(), snapshot -> snapshot.eventStreamConnected ? 1 : 0));
		gauges.add(gauge("fennec.services.client.changes", "{change}",
				"How often this runtime's state has changed since it started.", node,
				() -> client.snapshot(), snapshot -> snapshot.changeCount));
		watched.put(client, gauges);
		LOG.info("[DDSR] watching client runtime '" + first.consumerId + "'");
	}

	void updatedClient(ClientRuntime client, Map<String, Object> properties) {
		LOG.fine(() -> "[DDSR] client runtime changed, now at " + properties.get("service.changecount"));
	}

	void unsetClient(ClientRuntime client) {
		close(client);
	}

	/**
	 * One gauge over one reading of a snapshot.
	 *
	 * <p>{@code snapshot} is called per collection rather than per
	 * gauge-definition, which is the point: six instruments over one
	 * runtime take six snapshots per collection, and that is still
	 * cheaper than keeping a copy here that could be stale.
	 */
	private <T> ObservableLongGauge gauge(String name, String unit, String description, Attributes node,
			Supplier<T> snapshot, ToLongFunction<T> reading) {
		return meter.gaugeBuilder(name)
				.ofLongs()
				.setUnit(unit)
				.setDescription(description)
				.buildWithCallback(measurement -> measurement.record(reading.applyAsLong(snapshot.get()), node));
	}

	private void close(Object runtime) {
		List<ObservableLongGauge> gauges = watched.remove(runtime);
		if (gauges == null) {
			return;
		}
		for (ObservableLongGauge gauge : gauges) {
			gauge.close();
		}
	}

	private static long size(List<?> values) {
		return values == null ? 0 : values.size();
	}

	private static long leases(BrokerRuntimeDTO snapshot) {
		if (snapshot.registrations == null) {
			return 0;
		}
		long held = 0;
		for (RegistrationDTO registration : snapshot.registrations) {
			held += size(registration.heldBy);
		}
		return held;
	}

	private static long live(ClientRuntimeDTO snapshot) {
		if (snapshot.published == null) {
			return 0;
		}
		long live = 0;
		for (PublishedDTO published : snapshot.published) {
			if (published.live) {
				live++;
			}
		}
		return live;
	}

	private static long rebinding(ClientRuntimeDTO snapshot) {
		if (snapshot.bindings == null) {
			return 0;
		}
		long rebinding = 0;
		for (BindingDTO binding : snapshot.bindings) {
			if (!"LIVE".equals(binding.state)) {
				rebinding++;
			}
		}
		return rebinding;
	}
}
