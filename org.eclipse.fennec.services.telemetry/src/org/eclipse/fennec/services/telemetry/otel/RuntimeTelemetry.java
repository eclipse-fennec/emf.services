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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
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
import org.eclipse.fennec.services.runtime.SessionDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
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

	// What the relationship series are told apart by (#166). The contract
	// is rpc.service because that is what a span calls it; a view that
	// joins metrics and traces joins on the same name.
	private static final AttributeKey<String> SERVICE = AttributeKey.stringKey("rpc.service");
	private static final AttributeKey<String> REFERENCE = AttributeKey.stringKey("fennec.reference");
	private static final AttributeKey<String> PROVIDER = AttributeKey.stringKey("fennec.provider");
	private static final AttributeKey<String> IMPLEMENTATION = AttributeKey.stringKey("fennec.implementation");
	private static final AttributeKey<String> FLAVOR = AttributeKey.stringKey("fennec.flavor");
	private static final AttributeKey<String> CONSUMER = AttributeKey.stringKey("fennec.consumer");
	private static final AttributeKey<String> ORIGIN = AttributeKey.stringKey("fennec.origin");
	private static final AttributeKey<String> STATE = AttributeKey.stringKey("fennec.state");

	/**
	 * Where the instruments come from, taken in the constructor.
	 *
	 * <p>Not a field reference, and that is the whole lesson of this
	 * class: DS binds the dynamic references below while it is building
	 * the component, and a field reference may not be injected yet at
	 * that moment. A constructor parameter is: it is the one thing that
	 * is certainly there before any bind method runs.
	 */
	private final MeterProvider meters;

	/** What is watched, and the instruments that watch it. */
	private final Map<Object, Watched> watched = new ConcurrentHashMap<>();

	/**
	 * The instrumentation scope, with a default that holds before the
	 * configuration arrives.
	 *
	 * <p>It has to: DS binds a reference <em>before</em> it activates the
	 * component, so the first runtime can be bound before
	 * {@link #configure} has run. A meter taken in the activate method
	 * and used in a bind method is a null meter on exactly the service
	 * that was there first — which is every service, in a framework that
	 * starts everything at once.
	 */
	private volatile String scope = "org.eclipse.fennec.services";

	/** Whether the series per relationship are reported, on until configured. */
	private volatile boolean relationships = true;

	@Activate
	public RuntimeTelemetry(@Reference MeterProvider meters) {
		this.meters = meters;
	}

	@Activate
	@Modified
	void configure(TelemetryConfig config) {
		if (config.scope().equals(scope) && config.relationships() == relationships) {
			return;
		}
		this.scope = config.scope();
		this.relationships = config.relationships();
		// A new scope is a new meter, and the switch decides which
		// instruments there are, so either way they are built again.
		for (Watched entry : watched.values()) {
			entry.rebuild();
		}
	}

	/** One watched runtime: how to instrument it, and what is live. */
	private final class Watched {

		private final Supplier<List<ObservableLongGauge>> instruments;

		private List<ObservableLongGauge> live;

		Watched(Supplier<List<ObservableLongGauge>> instruments) {
			this.instruments = instruments;
			this.live = instruments.get();
		}

		synchronized void rebuild() {
			close();
			this.live = instruments.get();
		}

		synchronized void close() {
			for (ObservableLongGauge gauge : live) {
				gauge.close();
			}
			live = List.of();
		}
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
		watched.put(broker, new Watched(() -> brokerGauges(broker, node)));
		LOG.info("[DDSR] watching broker '" + first.name + "'");
	}

	private List<ObservableLongGauge> brokerGauges(BrokerRuntime broker, Attributes node) {
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
		if (relationships) {
			gauges.add(series("fennec.services.broker.registration", "{registration}",
					"One per registration and contract: who provides what, over which flavors.",
					() -> broker.snapshot(), snapshot -> registrationSeries(snapshot, node)));
			gauges.add(series("fennec.services.broker.lease", "{lease}",
					"One per registration and holder: which consumer holds a claim on which registration.",
					() -> broker.snapshot(), snapshot -> leaseSeries(snapshot, node)));
		}
		return gauges;
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
		watched.put(client, new Watched(() -> clientGauges(client, node)));
		LOG.info("[DDSR] watching client runtime '" + first.consumerId + "'");
	}

	private List<ObservableLongGauge> clientGauges(ClientRuntime client, Attributes node) {
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
		if (relationships) {
			gauges.add(series("fennec.services.client.binding", "{binding}",
					"One per binding: which contract this runtime is bound to, to which registration, in which state.",
					() -> client.snapshot(), snapshot -> bindingSeries(snapshot, node)));
		}
		return gauges;
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
		return meters.get(scope).gaugeBuilder(name)
				.ofLongs()
				.setUnit(unit)
				.setDescription(description)
				.buildWithCallback(measurement -> measurement.record(reading.applyAsLong(snapshot.get()), node));
	}

	/**
	 * One gauge with a point per relationship.
	 *
	 * <p>Observed like the others, and that is what makes it right for
	 * relationships: a binding that ends is simply not reported at the
	 * next collection, rather than lingering at its last value.
	 */
	private <T> ObservableLongGauge series(String name, String unit, String description, Supplier<T> snapshot,
			Function<T, Map<Attributes, Long>> points) {
		return meters.get(scope).gaugeBuilder(name)
				.ofLongs()
				.setUnit(unit)
				.setDescription(description)
				.buildWithCallback(measurement -> points.apply(snapshot.get())
						.forEach((attributes, value) -> measurement.record(value, attributes)));
	}

	private void close(Object runtime) {
		Watched entry = watched.remove(runtime);
		if (entry != null) {
			entry.close();
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

	/**
	 * One point per registration and contract.
	 *
	 * <p>Everything about the provider side is here and only here. A
	 * lease names the registration it is on and nothing else, so the
	 * provider is not repeated for every consumer holding it — a view
	 * joins the two on {@code fennec.reference}.
	 */
	static Map<Attributes, Long> registrationSeries(BrokerRuntimeDTO snapshot, Attributes node) {
		Map<Attributes, Long> points = new HashMap<>();
		if (snapshot.registrations == null) {
			return points;
		}
		for (RegistrationDTO registration : snapshot.registrations) {
			// put ignores a null value, which is what an unnamed provider
			// or a registration without flavors should look like: the
			// attribute absent, not the string "null".
			Attributes base = node.toBuilder()
					.put(REFERENCE, registration.referenceId)
					.put(PROVIDER, registration.providerName)
					.put(IMPLEMENTATION, registration.implementationId)
					.put(FLAVOR, registration.flavors == null || registration.flavors.isEmpty() ? null
							: String.join(",", registration.flavors))
					.build();
			if (registration.contracts == null || registration.contracts.isEmpty()) {
				points.merge(base, 1L, Long::sum);
				continue;
			}
			for (String contract : registration.contracts) {
				points.merge(base.toBuilder().put(SERVICE, contract).build(), 1L, Long::sum);
			}
		}
		return points;
	}

	/**
	 * One point per registration and holder.
	 *
	 * <p>The origin comes from the holder's session in the same snapshot.
	 * The consumer id is what the client calls itself; the origin is the
	 * token its spans carry, and only that one joins a lease to a call.
	 */
	static Map<Attributes, Long> leaseSeries(BrokerRuntimeDTO snapshot, Attributes node) {
		Map<Attributes, Long> points = new HashMap<>();
		if (snapshot.registrations == null) {
			return points;
		}
		Map<String, String> originOf = new HashMap<>();
		if (snapshot.sessions != null) {
			for (SessionDTO session : snapshot.sessions) {
				if (session.consumerId != null && session.origin != null) {
					originOf.put(session.consumerId, session.origin);
				}
			}
		}
		for (RegistrationDTO registration : snapshot.registrations) {
			if (registration.heldBy == null) {
				continue;
			}
			for (String holder : registration.heldBy) {
				Attributes attributes = node.toBuilder()
						.put(REFERENCE, registration.referenceId)
						.put(CONSUMER, holder)
						.put(ORIGIN, originOf.get(holder))
						.build();
				points.merge(attributes, 1L, Long::sum);
			}
		}
		return points;
	}

	/**
	 * One point per binding.
	 *
	 * <p>Counted rather than set to one: two locators for the same
	 * contract with different filters may well be bound to the same
	 * registration, and a gauge may carry each set of attributes once.
	 */
	static Map<Attributes, Long> bindingSeries(ClientRuntimeDTO snapshot, Attributes node) {
		Map<Attributes, Long> points = new HashMap<>();
		if (snapshot.bindings == null) {
			return points;
		}
		for (BindingDTO binding : snapshot.bindings) {
			Attributes attributes = node.toBuilder()
					.put(SERVICE, binding.contract)
					.put(REFERENCE, binding.referenceId)
					.put(STATE, binding.state)
					.build();
			points.merge(attributes, 1L, Long::sum);
		}
		return points;
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
