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

package org.eclipse.fennec.services.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.broker.core.BrokerSessions;
import org.eclipse.fennec.services.broker.core.DdsrDiagnostics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The consumer's session at the broker — the leases it holds and how it
 * gives them back (ACQUISITION.md §4).
 *
 * <p>These are the Java twins of {@code session.test.ts}. The
 * TypeScript side has had them since the session protocol existed; the
 * Java side had none, because the behaviour sat in two private methods
 * of a DS component and could only be reached with an OSGi runtime.
 * That is also where the acquisition-list bug of #124 lived, and it was
 * found by reading rather than by a failing test — which is the
 * argument for these.
 */
class ConsumerSessionKeeperTest {

	/** Records what the client tells the broker, without a broker. */
	private static final class RecordingSessions implements BrokerSessions {

		final List<ConsumerSession> put = new ArrayList<>();
		final List<Collection<String>> acquisitions = new ArrayList<>();
		final List<String> deleted = new ArrayList<>();
		RuntimeException failPut;
		RuntimeException failDelete;

		@Override
		public Diagnostic putSession(ConsumerSession session, Collection<String> acquiredReferenceIds) {
			if (failPut != null) {
				throw failPut;
			}
			put.add(session);
			acquisitions.add(acquiredReferenceIds == null ? null : List.copyOf(acquiredReferenceIds));
			return DdsrDiagnostics.ok("recorded");
		}

		@Override
		public Diagnostic deleteSession(String consumerId) {
			if (failDelete != null) {
				throw failDelete;
			}
			deleted.add(consumerId);
			return DdsrDiagnostics.ok("recorded");
		}

		@Override
		public Optional<SessionSnapshot> getSession(String consumerId) {
			return Optional.empty();
		}

		@Override
		public int expireSessions(Instant cutoff) {
			return 0;
		}

		@Override
		public int sessionCount() {
			return 0;
		}

		@Override
		public void consumerConnected(String consumerId) {
		}

		@Override
		public void consumerDisconnected(String consumerId) {
		}
	}

	private final RecordingSessions broker = new RecordingSessions();

	// ------------------------------------------------------------------
	// Renewal: the idempotent full replace
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a renewal sends the consumer id and the references it currently claims")
	void renewalCarriesTheAcquisitions() {
		keeper(List.of(FlavorKind.REST), () -> List.of("ref-1", "ref-2")).renew();

		assertThat(broker.put).hasSize(1);
		assertThat(broker.put.get(0).getConsumerId()).isEqualTo("consumer-7");
		assertThat(broker.acquisitions.get(0)).containsExactly("ref-1", "ref-2");
	}

	@Test
	@DisplayName("the capability travels with it, so a lookup and a session agree on the flavors")
	void renewalCarriesTheCapability() {
		keeper(List.of(FlavorKind.REST, FlavorKind.MQTT), List::of).renew();

		assertThat(broker.put.get(0).getCapabilities()).isNotNull();
		assertThat(broker.put.get(0).getCapabilities().getConsumerId()).isEqualTo("consumer-7");
		assertThat(broker.put.get(0).getCapabilities().getSupportedFlavors())
				.containsExactly(FlavorKind.REST, FlavorKind.MQTT);
	}

	@Test
	@DisplayName("an empty acquisition list is a pure heartbeat, not a mistake")
	void anEmptyListIsAHeartbeat() {
		keeper(List.of(FlavorKind.REST), List::of).renew();

		assertThat(broker.put).hasSize(1);
		assertThat(broker.acquisitions.get(0)).isEmpty();
	}

	@Test
	@DisplayName("every renewal asks again — the list is the client's live view, not a copy")
	void theAcquisitionsAreReadOnEveryRenewal() {
		// A NEW list per call, not a mutated one: a keeper that asked
		// once and remembered the reference would still see in-place
		// changes, so mutating a shared list proves nothing about
		// whether the supplier was asked again.
		AtomicInteger asked = new AtomicInteger();
		ConsumerSessionKeeper keeper = keeper(List.of(FlavorKind.REST),
				() -> asked.incrementAndGet() == 1 ? List.of("ref-1") : List.of("ref-2"));

		keeper.renew();
		keeper.renew();

		assertThat(asked.get()).isEqualTo(2);
		assertThat(broker.acquisitions.get(0)).containsExactly("ref-1");
		assertThat(broker.acquisitions.get(1))
				.as("a released reference must stop being claimed on the very next renewal")
				.containsExactly("ref-2");
	}

	@Test
	@DisplayName("no flavors is a session without a flavor filter, not a session without a capability")
	void withoutFlavorsTheCapabilityIsStillSent() {
		keeper(null, List::of).renew();

		assertThat(broker.put.get(0).getCapabilities()).isNotNull();
		assertThat(broker.put.get(0).getCapabilities().getSupportedFlavors()).isEmpty();
	}

	// ------------------------------------------------------------------
	// Renewal is best effort
	// ------------------------------------------------------------------

	@Test
	@DisplayName("a broker that refuses the renewal does not surface in a client that is otherwise fine")
	void aFailedRenewalIsSwallowed() {
		broker.failPut = new IllegalStateException("broker unreachable");

		assertThatCode(() -> keeper(List.of(FlavorKind.REST), List::of).renew())
				.as("a missed renewal is silence like any other; the TTL already covers it")
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("a later renewal still goes out after a failed one")
	void renewalRecoversAfterAFailure() {
		broker.failPut = new IllegalStateException("broker unreachable");
		ConsumerSessionKeeper keeper = keeper(List.of(FlavorKind.REST), () -> List.of("ref-1"));
		keeper.renew();

		broker.failPut = null;
		keeper.renew();

		assertThat(broker.put).hasSize(1);
		assertThat(broker.acquisitions.get(0)).containsExactly("ref-1");
	}

	@Test
	@DisplayName("without a consumer id there is no session to renew")
	void withoutAConsumerIdNothingIsSent() {
		new ConsumerSessionKeeper(broker, null, List.of(), List::of).renew();
		new ConsumerSessionKeeper(broker, "  ", List.of(), List::of).renew();

		assertThat(broker.put).isEmpty();
	}

	@Test
	@DisplayName("without a transport there is nobody to renew with")
	void withoutSessionsNothingIsSent() {
		assertThatCode(() -> new ConsumerSessionKeeper(null, "consumer-7", List.of(), List::of).renew())
				.doesNotThrowAnyException();
	}

	// ------------------------------------------------------------------
	// Release: the shutdown-notify
	// ------------------------------------------------------------------

	@Test
	@DisplayName("release tells the broker, so the leases go now instead of at the TTL")
	void releaseDeletesTheSession() {
		keeper(List.of(FlavorKind.REST), List::of).release();

		assertThat(broker.deleted).containsExactly("consumer-7");
	}

	@Test
	@DisplayName("a dead broker cannot stall a shutdown")
	void aFailedReleaseNeverStallsTheShutdown() {
		broker.failDelete = new IllegalStateException("broker already gone");

		assertThatCode(() -> keeper(List.of(FlavorKind.REST), List::of).release())
				.as("the client is going away; a broker that is already gone may not hold it back")
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("without a consumer id there is no session to release")
	void releaseWithoutAConsumerIdIsANoOp() {
		new ConsumerSessionKeeper(broker, null, List.of(), List::of).release();

		assertThat(broker.deleted).isEmpty();
	}

	@Test
	@DisplayName("release asks for nothing it does not need — no acquisition list is read")
	void releaseDoesNotReadTheAcquisitions() {
		ConsumerSessionKeeper keeper = new ConsumerSessionKeeper(broker, "consumer-7", List.of(), () -> {
			throw new AssertionError("release must not depend on a live client");
		});

		assertThatCode(keeper::release).doesNotThrowAnyException();
		assertThat(broker.deleted).containsExactly("consumer-7");
	}

	// ------------------------------------------------------------------
	// Fixtures
	// ------------------------------------------------------------------

	private ConsumerSessionKeeper keeper(List<FlavorKind> flavors,
			java.util.function.Supplier<Collection<String>> acquisitions) {
		return new ConsumerSessionKeeper(broker, "consumer-7", flavors, acquisitions);
	}
}
