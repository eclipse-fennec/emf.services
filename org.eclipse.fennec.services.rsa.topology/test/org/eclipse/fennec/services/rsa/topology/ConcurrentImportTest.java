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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
 * What happens when two threads reach the same import or export.
 *
 * <p>They do so routinely, not exceptionally: a discovery event and an
 * admin arriving, or a service registering and an admin arriving. The
 * maps involved were always thread-safe; the operations across them
 * were not, and the loser of the race left behind a registration that
 * nothing could close (#124).
 */
class ConcurrentImportTest {

	/** An admin that takes its time, so the window is wide enough to hit. */
	private static final class SlowAdmin implements RemoteServiceAdmin {

		final AtomicInteger importCalls = new AtomicInteger();
		final AtomicInteger exportCalls = new AtomicInteger();
		final AtomicInteger closedImports = new AtomicInteger();

		@Override
		public ImportRegistration importService(EndpointDescription endpoint) {
			importCalls.incrementAndGet();
			sleep();
			return new ImportRegistration() {
				@Override
				public ImportReference getImportReference() {
					return null;
				}

				@Override
				public void close() {
					closedImports.incrementAndGet();
				}

				@Override
				public Throwable getException() {
					return null;
				}

				@Override
				public boolean update(EndpointDescription updated) {
					return true;
				}
			};
		}

		@Override
		public Collection<ExportRegistration> exportService(ServiceReference<?> reference, Map<String, ?> properties) {
			exportCalls.incrementAndGet();
			sleep();
			return List.of(new ExportRegistration() {
				@Override
				public ExportReference getExportReference() {
					return null;
				}

				@Override
				public void close() {
				}

				@Override
				public Throwable getException() {
					return null;
				}

				@Override
				public EndpointDescription update(Map<String, ?> updated) {
					return null;
				}
			});
		}

		private static void sleep() {
			try {
				Thread.sleep(80);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			}
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

	@Test
	@DisplayName("a discovery event and an arriving admin import one endpoint once")
	void anEndpointIsImportedOnce() throws Exception {
		ImportWhatIsDiscovered topology = new ImportWhatIsDiscovered();
		EndpointDescription endpoint = Fakes.endpoint("endpoint-1");
		SlowAdmin admin = new SlowAdmin();

		// The admin arrives and is offered everything waiting, while the
		// event for the same endpoint comes in on the delivery thread.
		topology.endpointChanged(Fakes.added(endpoint), null);
		CountDownLatch go = new CountDownLatch(1);
		Thread arriving = new Thread(() -> {
			await(go);
			topology.addAdmin(admin);
		});
		Thread reporting = new Thread(() -> {
			await(go);
			topology.endpointChanged(Fakes.added(endpoint), null);
		});
		arriving.start();
		reporting.start();
		go.countDown();
		arriving.join(10_000);
		reporting.join(10_000);

		assertThat(admin.importCalls.get())
				.as("one endpoint, one import — a second one leaves an unclosable proxy")
				.isEqualTo(1);
	}

	@Test
	@DisplayName("an endpoint withdrawn during its import does not stay imported")
	void aWithdrawalDuringTheImportIsNotLost() throws Exception {
		ImportWhatIsDiscovered topology = new ImportWhatIsDiscovered();
		EndpointDescription endpoint = Fakes.endpoint("endpoint-1");
		SlowAdmin admin = new SlowAdmin();
		topology.addAdmin(admin);

		Thread importing = new Thread(() -> topology.endpointChanged(Fakes.added(endpoint), null));
		importing.start();
		// Inside the admin's 80 ms, so the removal lands mid-import.
		Thread.sleep(30);
		topology.endpointChanged(Fakes.removed(endpoint), null);
		importing.join(10_000);

		assertThat(admin.closedImports.get())
				.as("discovery reports a disappearance once; nothing comes back to clean up")
				.isEqualTo(1);
	}

	@Test
	@DisplayName("asking one admin twice about one service exports it once")
	void anAdminIsAskedOncePerService() {
		ExportEverythingAsked topology = new ExportEverythingAsked();
		ServiceReference<Object> asked = Fakes.serviceReference();
		SlowAdmin admin = new SlowAdmin();

		// Deliberately sequential, and deliberately not a race. The
		// interleaving that produces a double export — a service
		// registering while an admin arrives, each seeing the other's
		// state — is too narrow to provoke reliably, so this pins the
		// guard itself instead of pretending to hit the window. Both
		// paths end in exportThrough, and this is what they must not do
		// twice.
		topology.addAdmin(admin, Map.of("remote.configs.supported", "fennec.rest"));
		topology.addingService(asked);
		topology.addingService(asked);
		topology.settle();

		assertThat(admin.exportCalls.get())
				.as("twice means two endpoints and two announcements for one service")
				.isEqualTo(1);
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
