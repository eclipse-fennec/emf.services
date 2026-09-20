/**
 * Copyright (c) 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.services.broker.core.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceEvent;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.UpdatePolicy;
import org.eclipse.fennec.services.broker.core.EventDocument;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The lifecycle constellation matrix (#54). Every row states, for one
 * transition a consumer has to survive, the three things it depends on
 * at once: the <b>event sequence</b> on the sink (type and reasonCode,
 * in order), the <b>lookup result</b> afterwards ({@code getServiceReferences}
 * vs. {@code getAllServiceReferences}, by implementation identity), and the
 * <b>lease view</b> (which consumer still holds which registration).
 * Every emitted event must also be self-contained — that is what keeps
 * MQTT off the {@code _unknown} topic and SSE routing precise.
 * <p>
 * Adding a constellation costs one row in {@link #rows()}.
 */
class DdsrBrokerLifecycleMatrixTest {

	private static final String INTERFACE = "Payment";
	private static final Instant FAR_FUTURE = Instant.now().plusSeconds(3600);

	/**
	 * One constellation.
	 *
	 * @param name     row label
	 * @param given    arrangement; the sink is cleared afterwards
	 * @param when     the transition under test
	 * @param events   expected {@code TYPE} / {@code TYPE/REASON} signatures, in order
	 * @param visible  identities {@code impl/version} from getServiceReferences
	 * @param all      identities from getAllServiceReferences
	 * @param leases   identity → consumer ids still holding a lease (registrations that are gone are simply absent)
	 * @param andAlso  optional extra checks on the context and the recorded events
	 */
	record Row(String name, Consumer<Ctx> given, Consumer<Ctx> when, List<String> events,
			List<String> visible, List<String> all, Map<String, List<String>> leases,
			Consumer<Ctx> andAlso) {
		Row(String name, Consumer<Ctx> given, Consumer<Ctx> when, List<String> events,
				List<String> visible, List<String> all, Map<String, List<String>> leases) {
			this(name, given, when, events, visible, all, leases, ctx -> { });
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/** Broker under test plus the fixture vocabulary the rows speak. */
	static final class Ctx {
		final Path snapshot;
		final RecordingEventSink sink = new RecordingEventSink();
		DdsrBrokerImpl broker;
		final ServiceInterface payment;
		/** Reference ids observed per identity, to assert id changes across transitions. */
		final Map<String, String> referenceIdBefore = new java.util.HashMap<>();

		Ctx(Path snapshot) {
			this.snapshot = snapshot;
			this.broker = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend(), sink);
			sink.deliveredBy(this.broker);
			this.payment = serviceInterface(INTERFACE, "charge", "getBalance");
			broker.addCatalogEntry(payment, "matrix");
			sink.clear();
		}

		ServiceProvider publish(String provider, String impl, String version) {
			return publish(provider, impl, version, UpdatePolicy.UNSPECIFIED, null, 0L);
		}

		ServiceProvider publish(String providerName, String implName, String version, UpdatePolicy policy,
				String replacesVersion, long graceMillis) {
			ServiceProvider provider = provider(providerName, implName, version, payment);
			ServiceImplementation impl = provider.getImplementations().get(0);
			impl.setUpdatePolicy(policy);
			impl.setCutoverGraceMillis(graceMillis);
			if (replacesVersion != null) {
				ServiceImplementation stub = ServicesFactory.eINSTANCE.createServiceImplementation();
				stub.setName(implName);
				stub.setVersion(replacesVersion);
				stub.setImplementationId("stub");
				impl.setReplaces(stub);
			}
			Diagnostic d = broker.publishImplementation(provider, impl);
			assertThat(d.getSeverity().getValue())
					.as("publish %s/%s: %s", implName, version, d.getMessage())
					.isLessThan(DiagnosticSeverity.ERROR_VALUE);
			return provider;
		}

		/** In-place modification (#55): same identities, the REST endpoint moves to another host. */
		void modify(String providerName, String implName, String version, String host) {
			ServiceProvider modification = provider(providerName, implName, version, payment);
			((RestFlavor) modification.getImplementations().get(0).getFlavors().get(0)).setHost(host);
			Diagnostic d = broker.modifyImplementation(modification, modification.getImplementations().get(0));
			assertThat(d.getSeverity().getValue()).as(d.getMessage()).isLessThan(DiagnosticSeverity.ERROR_VALUE);
		}

		void withdraw(String providerName, String implName, String version) {
			// Identity stub, like the Java SDK does since #50.
			ServiceProvider stub = ServicesFactory.eINSTANCE.createServiceProvider();
			stub.setName(providerName);
			stub.setVersion(version);
			ServiceImplementation implStub = ServicesFactory.eINSTANCE.createServiceImplementation();
			implStub.setName(implName);
			implStub.setVersion(version);
			implStub.setImplementationId("stub");
			stub.getImplementations().add(implStub);
			Diagnostic d = broker.withdrawImplementation(stub, implStub);
			assertThat(d.getSeverity().getValue()).as(d.getMessage()).isLessThan(DiagnosticSeverity.ERROR_VALUE);
		}

		void lease(String consumerId, String... identities) {
			ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
			session.setConsumerId(consumerId);
			List<String> ids = Stream.of(identities).map(this::referenceOf).map(ServiceReference::getId).toList();
			Diagnostic d = broker.putSession(session, ids);
			assertThat(d.getSeverity()).isEqualTo(DiagnosticSeverity.OK);
		}

		void rememberReferenceIds() {
			for (ServiceReference ref : broker.getAllServiceReferences(INTERFACE, null, null)) {
				referenceIdBefore.put(identityOf(ref), ref.getId());
			}
		}

		/** Simulates a broker restart: a fresh instance over the same snapshot, fresh sink. */
		void restart() {
			sink.clear();
			broker = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend(), sink);
			sink.deliveredBy(broker);
		}

		ServiceReference referenceOf(String identity) {
			return broker.getAllServiceReferences(INTERFACE, null, null).stream()
					.filter(ref -> identity.equals(identityOf(ref)))
					.findFirst()
					.orElseThrow(() -> new AssertionError("no reference for " + identity));
		}

		List<String> visible() {
			return broker.getServiceReferences(INTERFACE, null, null).stream().map(Ctx::identityOf).sorted().toList();
		}

		List<String> all() {
			return broker.getAllServiceReferences(INTERFACE, null, null).stream().map(Ctx::identityOf).sorted().toList();
		}

		Map<String, List<String>> leases() {
			Map<String, List<String>> result = new java.util.TreeMap<>();
			for (ServiceReference ref : broker.getAllServiceReferences(INTERFACE, null, null)) {
				result.put(identityOf(ref), ref.getRegistration().getUsingSessions().stream()
						.map(ConsumerSession::getConsumerId).sorted().toList());
			}
			return result;
		}

		static String identityOf(ServiceReference ref) {
			ServiceImplementation impl = ref.getRegistration().getImplementation();
			return impl.getName() + "/" + impl.getVersion();
		}
	}

	// ------------------------------------------------------------------
	// The matrix
	// ------------------------------------------------------------------

	static Stream<Arguments> rows() {
		return Stream.of(
			new Row("publish announces REGISTERED and becomes visible",
				ctx -> { },
				ctx -> ctx.publish("payments", "impl-a", "1.0.0"),
				List.of("REGISTERED"),
				List.of("impl-a/1.0.0"), List.of("impl-a/1.0.0"), Map.of("impl-a/1.0.0", List.of())),

			new Row("withdraw announces UNREGISTERING/WITHDRAWN and vanishes",
				ctx -> ctx.publish("payments", "impl-a", "1.0.0"),
				ctx -> ctx.withdraw("payments", "impl-a", "1.0.0"),
				List.of("UNREGISTERING/WITHDRAWN"),
				List.of(), List.of(), Map.of()),

			new Row("withdraw under a held lease releases the lease, the session survives",
				ctx -> { ctx.publish("payments", "impl-a", "1.0.0"); ctx.lease("c1", "impl-a/1.0.0"); },
				ctx -> ctx.withdraw("payments", "impl-a", "1.0.0"),
				List.of("UNREGISTERING/WITHDRAWN"),
				List.of(), List.of(), Map.of(),
				ctx -> assertThat(ctx.broker.sessionCount()).as("the session itself is the consumer's, not the service's").isEqualTo(1)),

			new Row("withdrawing one of two implementations leaves the neighbour untouched",
				ctx -> { ctx.publish("payments", "impl-a", "1.0.0"); ctx.publish("payments-b", "impl-b", "1.0.0"); },
				ctx -> ctx.withdraw("payments", "impl-a", "1.0.0"),
				List.of("UNREGISTERING/WITHDRAWN"),
				List.of("impl-b/1.0.0"), List.of("impl-b/1.0.0"), Map.of("impl-b/1.0.0", List.of())),

			new Row("republish of the same identity: old goes as REPLACED, new comes, fresh reference id, leases released",
				ctx -> { ctx.publish("payments", "impl-a", "1.0.0"); ctx.lease("c1", "impl-a/1.0.0"); ctx.rememberReferenceIds(); },
				ctx -> ctx.publish("payments", "impl-a", "1.0.0"),
				List.of("UNREGISTERING/REPLACED", "REGISTERED"),
				List.of("impl-a/1.0.0"), List.of("impl-a/1.0.0"), Map.of("impl-a/1.0.0", List.of()),
				ctx -> {
					assertThat(ctx.sink.received().get(0).getReference().getId())
							.as("the UNREGISTERING names the old reference")
							.isEqualTo(ctx.referenceIdBefore.get("impl-a/1.0.0"));
					assertThat(ctx.referenceOf("impl-a/1.0.0").getId())
							.as("a republish mints a fresh reference id — the consumer must re-lookup")
							.isNotEqualTo(ctx.referenceIdBefore.get("impl-a/1.0.0"));
				}),

			new Row("modify in place: MODIFIED under the same reference id, leases kept, endpoint moved",
				ctx -> { ctx.publish("payments", "impl-a", "1.0.0"); ctx.lease("c1", "impl-a/1.0.0"); ctx.rememberReferenceIds(); },
				ctx -> ctx.modify("payments", "impl-a", "1.0.0", "http://elsewhere:9999"),
				List.of("MODIFIED"),
				List.of("impl-a/1.0.0"), List.of("impl-a/1.0.0"), Map.of("impl-a/1.0.0", List.of("c1")),
				ctx -> {
					ServiceReference ref = ctx.referenceOf("impl-a/1.0.0");
					assertThat(ref.getId()).as("a modification is not a new service").isEqualTo(ctx.referenceIdBefore.get("impl-a/1.0.0"));
					assertThat(((RestFlavor) ref.getRegistration().getImplementation().getFlavors().get(0)).getHost())
							.isEqualTo("http://elsewhere:9999");
				}),

			new Row("idle sweep parks the entry: UNREGISTERING/COLDIFIED, still discoverable",
				ctx -> ctx.publish("payments", "impl-a", "1.0.0"),
				ctx -> assertThat(ctx.broker.coldifyIdle(FAR_FUTURE)).isEqualTo(1),
				List.of("UNREGISTERING/COLDIFIED"),
				// the visibility check itself rehydrates — see the next row
				List.of("impl-a/1.0.0"), List.of("impl-a/1.0.0"), Map.of("impl-a/1.0.0", List.of())),

			new Row("a lookup rehydrates a cold entry with a fresh REGISTERED and a new reference id, same contract",
				ctx -> {
					ctx.publish("payments", "impl-a", "1.0.0");
					ctx.rememberReferenceIds();
					ctx.broker.coldifyIdle(FAR_FUTURE);
				},
				ctx -> assertThat(ctx.broker.getServiceReferences(INTERFACE, null, null)).hasSize(1),
				List.of("REGISTERED"),
				List.of("impl-a/1.0.0"), List.of("impl-a/1.0.0"), Map.of("impl-a/1.0.0", List.of()),
				ctx -> {
					assertThat(ctx.referenceOf("impl-a/1.0.0").getId())
							.isNotEqualTo(ctx.referenceIdBefore.get("impl-a/1.0.0"));
					assertThat(ctx.broker.coldCount()).isZero();
				}),

			new Row("a held lease keeps the entry hot",
				ctx -> { ctx.publish("payments", "impl-a", "1.0.0"); ctx.lease("c1", "impl-a/1.0.0"); },
				ctx -> assertThat(ctx.broker.coldifyIdle(FAR_FUTURE)).isZero(),
				List.of(),
				List.of("impl-a/1.0.0"), List.of("impl-a/1.0.0"), Map.of("impl-a/1.0.0", List.of("c1"))),

			new Row("a republish while the twin is parked cold drops the cold twin and announces only REGISTERED",
				ctx -> { ctx.publish("payments", "impl-a", "1.0.0"); ctx.broker.coldifyIdle(FAR_FUTURE); },
				ctx -> ctx.publish("payments", "impl-a", "1.0.0"),
				List.of("REGISTERED"),
				List.of("impl-a/1.0.0"), List.of("impl-a/1.0.0"), Map.of("impl-a/1.0.0", List.of()),
				ctx -> assertThat(ctx.broker.coldCount()).isZero()),

			new Row("session expiry releases the lease silently — no service event",
				ctx -> { ctx.publish("payments", "impl-a", "1.0.0"); ctx.lease("c1", "impl-a/1.0.0"); },
				ctx -> assertThat(ctx.broker.expireSessions(FAR_FUTURE)).isEqualTo(1),
				List.of(),
				List.of("impl-a/1.0.0"), List.of("impl-a/1.0.0"), Map.of("impl-a/1.0.0", List.of()),
				ctx -> assertThat(ctx.broker.sessionCount()).isZero()),

			new Row("broker restart: state survives, sessions die, reference ids are reindexed",
				ctx -> { ctx.publish("payments", "impl-a", "1.0.0"); ctx.lease("c1", "impl-a/1.0.0"); ctx.rememberReferenceIds(); },
				Ctx::restart,
				List.of(),
				List.of("impl-a/1.0.0"), List.of("impl-a/1.0.0"), Map.of("impl-a/1.0.0", List.of()),
				ctx -> {
					assertThat(ctx.broker.sessionCount()).isZero();
					assertThat(ctx.referenceOf("impl-a/1.0.0").getId())
							.as("reindex mints fresh ids; the reconnect snapshot re-learns them")
							.isNotEqualTo(ctx.referenceIdBefore.get("impl-a/1.0.0"));
				}),

			new Row("EVERGREEN successor: both stay, no hint",
				ctx -> ctx.publish("payments", "impl-a", "1.0.0"),
				ctx -> ctx.publish("payments-v2", "impl-a", "2.0.0", UpdatePolicy.EVERGREEN, "1.0.0", 0L),
				List.of("REGISTERED"),
				List.of("impl-a/1.0.0", "impl-a/2.0.0"), List.of("impl-a/1.0.0", "impl-a/2.0.0"),
				Map.of("impl-a/1.0.0", List.of(), "impl-a/2.0.0", List.of())),

			new Row("DEPRECATE_AND_DRAIN successor under a held lease: REGISTERED then UPGRADE_AVAILABLE, predecessor hidden but held",
				ctx -> { ctx.publish("payments", "impl-a", "1.0.0"); ctx.lease("c1", "impl-a/1.0.0"); },
				ctx -> ctx.publish("payments-v2", "impl-a", "2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0", 0L),
				List.of("REGISTERED", "UPGRADE_AVAILABLE"),
				List.of("impl-a/2.0.0"), List.of("impl-a/1.0.0", "impl-a/2.0.0"),
				Map.of("impl-a/1.0.0", List.of("c1"), "impl-a/2.0.0", List.of()),
				ctx -> assertThat(ctx.broker.advanceUpdatePolicies(Instant.now())).as("the lease blocks the retire").isZero()),

			new Row("drain completes when the holder lets go: UNREGISTERING/REPLACED then RETIRED/REPLACED",
				ctx -> {
					ctx.publish("payments", "impl-a", "1.0.0");
					ctx.lease("c1", "impl-a/1.0.0");
					ctx.publish("payments-v2", "impl-a", "2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0", 0L);
				},
				ctx -> {
					ctx.broker.deleteSession("c1");
					assertThat(ctx.broker.advanceUpdatePolicies(Instant.now())).isEqualTo(1);
				},
				List.of("UNREGISTERING/REPLACED", "RETIRED/REPLACED"),
				List.of("impl-a/2.0.0"), List.of("impl-a/2.0.0"), Map.of("impl-a/2.0.0", List.of())),

			new Row("drain completes when the holder's session expires",
				ctx -> {
					ctx.publish("payments", "impl-a", "1.0.0");
					ctx.lease("c1", "impl-a/1.0.0");
					ctx.publish("payments-v2", "impl-a", "2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0", 0L);
				},
				ctx -> {
					assertThat(ctx.broker.expireSessions(FAR_FUTURE)).isEqualTo(1);
					assertThat(ctx.broker.advanceUpdatePolicies(Instant.now())).isEqualTo(1);
				},
				List.of("UNREGISTERING/REPLACED", "RETIRED/REPLACED"),
				List.of("impl-a/2.0.0"), List.of("impl-a/2.0.0"), Map.of("impl-a/2.0.0", List.of())),

			new Row("HARD_CUTOVER successor: failover window, both visible, no hint",
				ctx -> { ctx.publish("payments", "impl-a", "1.0.0"); ctx.lease("c1", "impl-a/1.0.0"); },
				ctx -> ctx.publish("payments-v2", "impl-a", "2.0.0", UpdatePolicy.HARD_CUTOVER, "1.0.0", 1_000L),
				List.of("REGISTERED"),
				List.of("impl-a/1.0.0", "impl-a/2.0.0"), List.of("impl-a/1.0.0", "impl-a/2.0.0"),
				Map.of("impl-a/1.0.0", List.of("c1"), "impl-a/2.0.0", List.of()),
				ctx -> assertThat(ctx.broker.advanceUpdatePolicies(Instant.now())).as("inside the window").isZero()),

			new Row("HARD_CUTOVER window elapsed: UNREGISTERING/CUTOVER then RETIRED/CUTOVER despite the lease",
				ctx -> {
					ctx.publish("payments", "impl-a", "1.0.0");
					ctx.lease("c1", "impl-a/1.0.0");
					ctx.publish("payments-v2", "impl-a", "2.0.0", UpdatePolicy.HARD_CUTOVER, "1.0.0", 1_000L);
				},
				ctx -> assertThat(ctx.broker.advanceUpdatePolicies(Instant.now().plusSeconds(2))).isEqualTo(1),
				List.of("UNREGISTERING/CUTOVER", "RETIRED/CUTOVER"),
				List.of("impl-a/2.0.0"), List.of("impl-a/2.0.0"), Map.of("impl-a/2.0.0", List.of()),
				ctx -> assertThat(ctx.broker.sessionCount()).as("the consumer's session stays, only the lease is gone").isEqualTo(1)),

			new Row("withdrawing the successor cancels the drain: predecessor visible again, no events for it",
				ctx -> {
					ctx.publish("payments", "impl-a", "1.0.0");
					ctx.publish("payments-v2", "impl-a", "2.0.0", UpdatePolicy.DEPRECATE_AND_DRAIN, "1.0.0", 0L);
				},
				ctx -> {
					ctx.withdraw("payments-v2", "impl-a", "2.0.0");
					assertThat(ctx.broker.advanceUpdatePolicies(FAR_FUTURE)).isZero();
				},
				List.of("UNREGISTERING/WITHDRAWN"),
				List.of("impl-a/1.0.0"), List.of("impl-a/1.0.0"), Map.of("impl-a/1.0.0", List.of())),

			new Row("publishing against a deprecated catalog entry warns but registers and resolves",
				ctx -> ctx.broker.deprecateCatalogEntry(ctx.payment, "matrix"),
				ctx -> ctx.publish("payments", "impl-a", "1.0.0"),
				List.of("REGISTERED"),
				List.of("impl-a/1.0.0"), List.of("impl-a/1.0.0"), Map.of("impl-a/1.0.0", List.of()))
		).map(Arguments::of);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rows")
	void constellation(Row row) throws Exception {
		Path dir = Files.createTempDirectory("ddsr-matrix");
		Ctx ctx = new Ctx(dir.resolve("broker-state.xmi"));

		row.given().accept(ctx);
		ctx.sink.clear();

		row.when().accept(ctx);

		assertThat(ctx.sink.signatures()).as("event sequence").containsExactlyElementsOf(row.events());
		for (ServiceEvent event : ctx.sink.received()) {
			assertThat(EventDocument.interfaceNamesOf(event, ctx.broker))
					.as("%s must be self-contained (interfaces determinable from the document)",
							RecordingEventSink.signature(event))
					.containsExactly(INTERFACE);
			// #124: an event describes the moment it is about, not the
			// registry as the sink happens to find it. Every row, because
			// this is what allows the delivery to leave the write lock —
			// a document rendered later must not depend on live state.
			assertThat(event.getReference().getRegistration())
					.as("%s must not carry a live registration",
							RecordingEventSink.signature(event))
					.isNull();
			assertThat(event.getReference().eContainer())
					.as("%s must not be contained in live broker state",
							RecordingEventSink.signature(event))
					.isNull();
		}
		assertThat(ctx.visible()).as("getServiceReferences").containsExactlyElementsOf(sorted(row.visible()));
		assertThat(ctx.all()).as("getAllServiceReferences").containsExactlyElementsOf(sorted(row.all()));
		assertThat(ctx.leases()).as("leases").isEqualTo(row.leases());
		row.andAlso().accept(ctx);
	}

	private static List<String> sorted(List<String> values) {
		return values.stream().sorted().toList();
	}

	// ------------------------------------------------------------------
	// Fixtures
	// ------------------------------------------------------------------

	private static ServiceInterface serviceInterface(String name, String... operations) {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName(name);
		si.setVersion("1.0.0");
		for (String op : operations) {
			ServiceOperation operation = ServicesFactory.eINSTANCE.createServiceOperation();
			operation.setName(op);
			si.getOperations().add(operation);
		}
		return si;
	}

	private static ServiceProvider provider(String providerName, String implName, String version, ServiceInterface si) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(providerName);
		provider.setVersion(version);
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(implName);
		impl.setVersion(version);
		impl.setImplementationId("org.example." + implName);
		impl.getServiceInterfaces().add(si);
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName(implName);
		flavor.setHost("http://localhost:9091");
		flavor.setBasePath("/" + implName);
		for (ServiceOperation op : si.getOperations()) {
			RestOperationFlavor of = ServicesFactory.eINSTANCE.createRestOperationFlavor();
			of.setName(op.getName());
			of.setPath("/" + op.getName());
			of.setOperation(op);
			flavor.getOperationFlavors().add(of);
		}
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		return provider;
	}
}
