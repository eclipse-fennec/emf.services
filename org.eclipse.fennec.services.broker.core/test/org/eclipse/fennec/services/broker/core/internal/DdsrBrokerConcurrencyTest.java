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

package org.eclipse.fennec.services.broker.core.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.fennec.services.ConsumerSession;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Concurrent access to the broker (DECISIONS_PARITY D9 / FR-P5):
 * publishers, withdrawers and readers hammer one {@link DdsrBrokerImpl}
 * instance in parallel; afterwards the in-memory state, the lookup index
 * and the persisted snapshot have to agree.
 * <p>
 * Worker exceptions are collected, not swallowed — a
 * {@code ConcurrentModificationException} in {@code getRegistry()} or a
 * torn snapshot write is exactly what this test exists to catch.
 */
class DdsrBrokerConcurrencyTest {

	private static final int THREADS = 8;
	private static final int ITERATIONS = 50;

	@TempDir
	Path tmp;

	private static ServiceInterface serviceInterface(String name) {
		ServiceInterface si = ServicesFactory.eINSTANCE.createServiceInterface();
		si.setName(name);
		si.setVersion("1.0.0");
		ServiceOperation op = ServicesFactory.eINSTANCE.createServiceOperation();
		op.setName("call");
		si.getOperations().add(op);
		return si;
	}

	private static ServiceProvider provider(String providerName, String implName) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(providerName);
		provider.setVersion("1.0.0");
		ServiceImplementation impl = ServicesFactory.eINSTANCE.createServiceImplementation();
		impl.setName(implName);
		impl.setVersion("1.0.0");
		impl.getServiceInterfaces().add(serviceInterface("Payment"));
		RestFlavor flavor = ServicesFactory.eINSTANCE.createRestFlavor();
		flavor.setName(implName);
		flavor.setBasePath("/payments");
		impl.getFlavors().add(flavor);
		provider.getImplementations().add(impl);
		return provider;
	}

	private static boolean isError(Diagnostic d) {
		return d.getSeverity().getValue() >= DiagnosticSeverity.ERROR_VALUE;
	}

	@Test
	void parallelPublishWithdrawAndReadKeepStateConsistent() throws Exception {
		Path snapshot = tmp.resolve("concurrent.xmi");
		InMemoryLookupBackend lookup = new InMemoryLookupBackend();
		DdsrBrokerImpl broker = new DdsrBrokerImpl(snapshot, lookup);
		assertThat(isError(broker.addCatalogEntry(serviceInterface("Payment"), "test"))).isFalse();

		List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(THREADS + 2);
		AtomicInteger unexpectedDiagnostics = new AtomicInteger();

		// Publish/withdraw pairs, one provider per thread so each pair is
		// self-contained. Thread 0 and 1 share provider AND implementation
		// names on purpose: that exercises the republish/retire path under
		// contention.
		List<Thread> threads = new ArrayList<>();
		for (int t = 0; t < THREADS; t++) {
			boolean contended = t < 2;
			String providerName = contended ? "prov-shared" : "prov-" + t;
			int thread = t;
			threads.add(new Thread(() -> {
				try {
					start.await();
					for (int i = 0; i < ITERATIONS; i++) {
						String implName = contended ? "impl-shared" : "impl-" + thread + "-" + i;
						ServiceProvider p = provider(providerName, implName);
						ServiceImplementation impl = p.getImplementations().get(0);
						Diagnostic published = broker.publishImplementation(p, impl);
						if (isError(published)) {
							unexpectedDiagnostics.incrementAndGet();
							continue;
						}
						broker.getServiceReferences("Payment", null, null);
						if (!contended) {
							// Provider dedup may have moved the impl into the
							// registry's existing provider — withdraw against
							// its actual container, like a real SDK would.
							ServiceProvider owner = (ServiceProvider) impl.eContainer();
							Diagnostic withdrawn = broker.withdrawImplementation(owner, impl);
							if (isError(withdrawn)) {
								unexpectedDiagnostics.incrementAndGet();
							}
						}
						// The contended pair leaves its publishes in place —
						// each one retires the previous, the last two must
						// reduce to exactly one implementation at the end.
					}
				} catch (Throwable failure) {
					failures.add(failure);
				} finally {
					done.countDown();
				}
			}, "publisher-" + t));
		}

		// A reader hammering the copy path that used to serialize live
		// state, and a snapshotter racing persist() against mutations.
		threads.add(new Thread(() -> {
			try {
				start.await();
				for (int i = 0; i < ITERATIONS * 4; i++) {
					RemoteServiceRegistry copy = broker.getRegistry();
					// Touch the whole copied tree, as XMI serialization would.
					copy.getCatalog().forEach(si -> si.getOperations().size());
					copy.getImplementations().forEach(impl -> impl.getFlavors().size());
				}
			} catch (Throwable failure) {
				failures.add(failure);
			} finally {
				done.countDown();
			}
		}, "reader"));
		threads.add(new Thread(() -> {
			try {
				start.await();
				for (int i = 0; i < ITERATIONS; i++) {
					if (isError(broker.snapshot())) {
						unexpectedDiagnostics.incrementAndGet();
					}
				}
			} catch (Throwable failure) {
				failures.add(failure);
			} finally {
				done.countDown();
			}
		}, "snapshotter"));

		threads.forEach(Thread::start);
		start.countDown();
		assertThat(done.await(60, TimeUnit.SECONDS)).as("workers must finish").isTrue();

		assertThat(failures)
				.as("no worker may die on an exception: %s", failures)
				.isEmpty();
		assertThat(unexpectedDiagnostics.get())
				.as("every publish and every withdraw must be acknowledged")
				.isZero();

		// Invariants. All non-contended pairs withdrew their publish; the
		// contended pair retires on republish, so exactly one shared
		// implementation remains.
		RemoteServiceRegistry live = broker.liveRegistry();
		assertThat(live.getImplementations())
				.extracting(ServiceImplementation::getName)
				.containsExactly("impl-shared");
		assertThat(broker.getServiceReferences("Payment", null, null))
				.as("lookup index and registry must agree")
				.hasSize(1);

		// The snapshot on disk is loadable and describes the same state.
		InMemoryLookupBackend rehydratedLookup = new InMemoryLookupBackend();
		DdsrBrokerImpl rehydrated = new DdsrBrokerImpl(snapshot, rehydratedLookup);
		assertThat(rehydrated.liveRegistry().getImplementations())
				.extracting(ServiceImplementation::getName)
				.containsExactly("impl-shared");
		assertThat(rehydratedLookup.getAllServiceReferences("Payment", null, null)).hasSize(1);
	}

	private static ConsumerSession session(String consumerId) {
		ConsumerSession session = ServicesFactory.eINSTANCE.createConsumerSession();
		session.setConsumerId(consumerId);
		return session;
	}

	/**
	 * Sessions racing the provider lifecycle: publishers and withdrawers
	 * churn registrations while consumers PUT/DELETE sessions against
	 * whatever references they can see, an expiry sweeper reaps, and a
	 * snapshotter persists. End invariants: every remaining acquisition
	 * resolves onto a live, lookup-visible reference; the snapshot stays
	 * loadable and free of session traces.
	 */
	@Test
	void parallelSessionsStayConsistentAgainstTheProviderLifecycle() throws Exception {
		Path snapshot = tmp.resolve("concurrent-sessions.xmi");
		DdsrBrokerImpl broker = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend());
		assertThat(isError(broker.addCatalogEntry(serviceInterface("Payment"), "test"))).isFalse();

		final int publisherThreads = 4;
		final int consumerThreads = 4;
		List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(publisherThreads + consumerThreads + 2);
		AtomicInteger unexpectedDiagnostics = new AtomicInteger();

		List<Thread> threads = new ArrayList<>();
		// Publish/withdraw churn.
		for (int t = 0; t < publisherThreads; t++) {
			int thread = t;
			threads.add(new Thread(() -> {
				try {
					start.await();
					for (int i = 0; i < ITERATIONS; i++) {
						ServiceProvider p = provider("prov-" + thread, "impl-" + thread + "-" + i);
						ServiceImplementation impl = p.getImplementations().get(0);
						if (isError(broker.publishImplementation(p, impl))) {
							unexpectedDiagnostics.incrementAndGet();
							continue;
						}
						ServiceProvider owner = (ServiceProvider) impl.eContainer();
						if (isError(broker.withdrawImplementation(owner, impl))) {
							unexpectedDiagnostics.incrementAndGet();
						}
					}
				} catch (Throwable failure) {
					failures.add(failure);
				} finally {
					done.countDown();
				}
			}, "session-publisher-" + t));
		}
		// Consumers acquiring whatever is visible right now.
		for (int t = 0; t < consumerThreads; t++) {
			String consumerId = "consumer-" + t;
			threads.add(new Thread(() -> {
				try {
					start.await();
					for (int i = 0; i < ITERATIONS; i++) {
						List<String> visible = broker.getServiceReferences("Payment", null, null).stream()
								.map(r -> r.getId()).toList();
						if (isError(broker.putSession(session(consumerId), visible))) {
							unexpectedDiagnostics.incrementAndGet();
						}
						if (i % 10 == 9 && isError(broker.deleteSession(consumerId))) {
							unexpectedDiagnostics.incrementAndGet();
						}
					}
					// Leave a final session behind for the invariants below.
					List<String> visible = broker.getServiceReferences("Payment", null, null).stream()
							.map(r -> r.getId()).toList();
					if (isError(broker.putSession(session(consumerId), visible))) {
						unexpectedDiagnostics.incrementAndGet();
					}
				} catch (Throwable failure) {
					failures.add(failure);
				} finally {
					done.countDown();
				}
			}, consumerId));
		}
		// The renewal race (case 19): a sweeper expiring EVERYTHING it
		// sees, racing the consumers' re-PUTs — afterwards a session is
		// either freshly present or cleanly gone, never half.
		threads.add(new Thread(() -> {
			try {
				start.await();
				for (int i = 0; i < ITERATIONS * 2; i++) {
					broker.expireSessions(Instant.now().plusSeconds(3600));
				}
			} catch (Throwable failure) {
				failures.add(failure);
			} finally {
				done.countDown();
			}
		}, "expiry-sweeper"));
		threads.add(new Thread(() -> {
			try {
				start.await();
				for (int i = 0; i < ITERATIONS; i++) {
					if (isError(broker.snapshot())) {
						unexpectedDiagnostics.incrementAndGet();
					}
					broker.getRegistry().getImplementations().forEach(impl -> impl.getFlavors().size());
				}
			} catch (Throwable failure) {
				failures.add(failure);
			} finally {
				done.countDown();
			}
		}, "session-snapshotter"));

		threads.forEach(Thread::start);
		start.countDown();
		assertThat(done.await(60, TimeUnit.SECONDS)).as("workers must finish").isTrue();
		assertThat(failures).as("no worker may die: %s", failures).isEmpty();
		assertThat(unexpectedDiagnostics.get()).isZero();

		// Every remaining acquisition must resolve onto a live reference
		// that the lookup still serves — no lease may point at a withdrawn
		// or expired registration.
		List<String> liveIds = broker.getServiceReferences("Payment", null, null).stream()
				.map(r -> r.getId()).toList();
		for (int t = 0; t < consumerThreads; t++) {
			var stored = broker.getSession("consumer-" + t);
			if (stored.isEmpty()) {
				continue; // cleanly gone is a legal outcome of the race
			}
			assertThat(stored.orElseThrow().acquiredReferenceIds())
					.as("acquisitions of consumer-%s must all be live", t)
					.isSubsetOf(liveIds);
		}

		// All publishers withdrew their publishes — nothing may linger.
		assertThat(broker.liveRegistry().getImplementations()).isEmpty();
		assertThat(liveIds).isEmpty();

		// The snapshot survived the churn and carries no session traces.
		String xmi = java.nio.file.Files.readString(snapshot);
		assertThat(xmi).doesNotContain("ConsumerSession").doesNotContain("acquisitions");
		DdsrBrokerImpl rehydrated = new DdsrBrokerImpl(snapshot, new InMemoryLookupBackend());
		assertThat(rehydrated.sessionCount()).isZero();
	}
}
