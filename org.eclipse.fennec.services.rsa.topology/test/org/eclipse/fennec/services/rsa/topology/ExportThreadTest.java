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

package org.eclipse.fennec.services.rsa.topology;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Where an export runs, and what a failed one leaves behind (#164).
 *
 * <p>A service that asked to be exported before the REST admin existed
 * was never exported. The admin arrived on the Jakarta REST whiteboard's
 * update thread, the topology exported right there, and the export
 * waited for the whiteboard to deploy — on the thread that was waiting.
 * After ten seconds it gave up, and the failed registration counted as
 * taken, so nothing ever asked again.
 */
class ExportThreadTest {

	/** A registration the way an admin hands one back. */
	private static final class Registration implements ExportRegistration {

		private final Throwable exception;
		final AtomicInteger closed = new AtomicInteger();

		Registration(Throwable exception) {
			this.exception = exception;
		}

		@Override
		public ExportReference getExportReference() {
			return null;
		}

		@Override
		public void close() {
			closed.incrementAndGet();
		}

		@Override
		public Throwable getException() {
			return exception;
		}

		@Override
		public EndpointDescription update(Map<String, ?> properties) {
			return null;
		}
	}

	/** An admin that answers whatever the test tells it to, and counts. */
	private static final class ScriptedAdmin implements RemoteServiceAdmin {

		final AtomicInteger exportCalls = new AtomicInteger();
		final List<Registration> handedOut = new ArrayList<>();
		volatile Supplier<Registration> answer = () -> new Registration(null);

		@Override
		public Collection<ExportRegistration> exportService(ServiceReference<?> reference, Map<String, ?> properties) {
			exportCalls.incrementAndGet();
			Registration registration = answer.get();
			handedOut.add(registration);
			return List.of(registration);
		}

		@Override
		public ImportRegistration importService(EndpointDescription endpoint) {
			return null;
		}

		@Override
		public Collection<ExportReference> getExportedServices() {
			return List.of();
		}

		@Override
		public Collection<ImportReference> getImportedEndpoints() {
			return List.of();
		}
	}

	private static final Map<String, Object> REST = Map.of("remote.configs.supported", "fennec.rest");

	@Test
	@DisplayName("an admin arriving on a thread its export waits for does not wait for itself")
	void theArrivingThreadIsNotTheExportingThread() throws InterruptedException {
		ExportEverythingAsked topology = new ExportEverythingAsked();
		ServiceReference<Object> asked = Fakes.serviceReference();
		topology.addingService(asked);
		topology.settle();

		// The whiteboard in miniature: the export can only finish once the
		// thread that announced the admin has gone on with its own work.
		CountDownLatch whiteboardMovedOn = new CountDownLatch(1);
		ScriptedAdmin admin = new ScriptedAdmin();
		admin.answer = () -> {
			try {
				return whiteboardMovedOn.await(5, TimeUnit.SECONDS) ? new Registration(null)
						: new Registration(new IllegalStateException("not deployed within 5 s"));
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				return new Registration(interrupted);
			}
		};

		long before = System.nanoTime();
		topology.addAdmin(admin, REST);
		long blockedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
		whiteboardMovedOn.countDown();
		topology.settle();

		assertThat(blockedMillis).as("the thread the admin arrived on is given back at once").isLessThan(1_000);
		assertThat(admin.handedOut).singleElement()
				.satisfies(registration -> assertThat(registration.getException()).isNull());
	}

	@Test
	@DisplayName("a failed export is not counted as taken, is closed, and is tried again when the service changes")
	void aFailedRegistrationIsRetried() {
		ExportEverythingAsked topology = new ExportEverythingAsked();
		ScriptedAdmin admin = new ScriptedAdmin();
		admin.answer = () -> new Registration(new IllegalStateException("whiteboard did not deploy"));
		// Not settled in between, on purpose: both events queue up, and
		// both reach this service. A failure must not make the second
		// one ask again.
		topology.addAdmin(admin, REST);

		ServiceReference<Object> asked = Fakes.serviceReference();
		Collection<ExportRegistration> registrations = topology.addingService(asked);
		topology.settle();

		assertThat(admin.exportCalls.get()).as("an admin arriving and a service registering ask once together")
				.isEqualTo(1);
		assertThat(registrations).as("what failed is not what an admin took").isEmpty();
		assertThat(admin.handedOut.get(0).closed.get()).as("and it is closed, not dropped").isEqualTo(1);

		admin.answer = () -> new Registration(null);
		topology.modifiedService(asked, registrations);
		topology.settle();

		assertThat(admin.exportCalls.get()).as("the failure did not keep the claim").isEqualTo(2);
		assertThat(registrations).hasSize(1);
	}

	@Test
	@DisplayName("an admin that throws is asked again when the service changes")
	void aThrowingAdminIsRetried() {
		ExportEverythingAsked topology = new ExportEverythingAsked();
		ScriptedAdmin admin = new ScriptedAdmin();
		admin.answer = () -> {
			throw new IllegalStateException("boom");
		};
		// Not settled in between, on purpose: both events queue up, and
		// both reach this service. A failure must not make the second
		// one ask again.
		topology.addAdmin(admin, REST);

		ServiceReference<Object> asked = Fakes.serviceReference();
		Collection<ExportRegistration> registrations = topology.addingService(asked);
		topology.settle();
		assertThat(registrations).isEmpty();
		assertThat(admin.exportCalls.get()).isEqualTo(1);

		admin.answer = () -> new Registration(null);
		topology.modifiedService(asked, registrations);
		topology.settle();

		assertThat(registrations).hasSize(1);
	}

	@Test
	@DisplayName("a service that leaves while its export waits for the export thread is not exported afterwards")
	void aServiceThatLeftWhileWaitingIsClosed() throws InterruptedException {
		ExportEverythingAsked topology = new ExportEverythingAsked();
		ServiceReference<Object> asked = Fakes.serviceReference();

		// Holds the export thread so the service can leave while its
		// export is still queued behind this one.
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch holding = new CountDownLatch(1);
		ScriptedAdmin admin = new ScriptedAdmin();
		admin.answer = () -> {
			holding.countDown();
			try {
				release.await(5, TimeUnit.SECONDS);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			}
			return new Registration(null);
		};
		topology.addAdmin(admin, REST);
		Collection<ExportRegistration> registrations = topology.addingService(asked);
		assertThat(holding.await(5, TimeUnit.SECONDS)).isTrue();

		topology.removedService(asked, registrations);
		release.countDown();
		topology.settle();

		assertThat(admin.handedOut).singleElement()
				.satisfies(registration -> assertThat(registration.closed.get())
						.as("an endpoint for a service that is gone is closed").isPositive());
	}
}
