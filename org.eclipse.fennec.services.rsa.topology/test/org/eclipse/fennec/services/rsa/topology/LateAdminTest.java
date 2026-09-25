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
 * What happens to a service that asked to be exported before any admin
 * existed, and to an endpoint that was discovered then.
 *
 * <p>The answer used to be: nothing, ever. A remote service admin is a
 * component like any other and comes up after its transports, while the
 * services around it register immediately. Both topology managers asked
 * the admins that existed at that one moment and dropped what nobody
 * took. The example provider therefore exported nothing at all, and the
 * TCK never showed it because the TCK exports by hand.
 */
class LateAdminTest {

	/** An admin that takes everything, and says what it was given. */
	private static final class WillingAdmin implements RemoteServiceAdmin {

		final List<ServiceReference<?>> exportedServices = new ArrayList<>();
		final List<String> importedEndpoints = new ArrayList<>();

		@Override
		public Collection<ExportRegistration> exportService(ServiceReference<?> reference, Map<String, ?> properties) {
			exportedServices.add(reference);
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

		@Override
		public ImportRegistration importService(EndpointDescription endpoint) {
			importedEndpoints.add(endpoint.getId());
			return new ImportRegistration() {
				@Override
				public ImportReference getImportReference() {
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
				public boolean update(EndpointDescription updated) {
					return true;
				}
			};
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
	@DisplayName("a service that found no admin is exported as soon as one arrives")
	void exportIsRetriedWhenAnAdminArrives() {
		ExportEverythingAsked topology = new ExportEverythingAsked();
		ServiceReference<Object> asked = Fakes.serviceReference();

		Collection<ExportRegistration> atFirst = topology.addingService(asked);
		topology.settle();
		assertThat(atFirst).as("nobody could export it yet").isEmpty();

		WillingAdmin admin = new WillingAdmin();
		topology.addAdmin(admin, Map.of("remote.configs.supported", "fennec.rest"));
		topology.settle();

		assertThat(admin.exportedServices).as("the admin is asked about what was already waiting")
				.containsExactly(asked);
		assertThat(atFirst).as("and the registration lands in the collection the tracker holds").hasSize(1);
	}

	@Test
	@DisplayName("an admin arriving later is asked exactly once per service, not twice")
	void noDoubleExport() {
		ExportEverythingAsked topology = new ExportEverythingAsked();
		WillingAdmin first = new WillingAdmin();
		topology.addAdmin(first, Map.of("remote.configs.supported", "fennec.rest"));

		ServiceReference<Object> asked = Fakes.serviceReference();
		topology.addingService(asked);
		topology.addAdmin(new WillingAdmin(), Map.of("remote.configs.supported", "fennec.rest"));
		topology.settle();

		assertThat(first.exportedServices).as("the first admin saw it when it registered, and not again")
				.containsExactly(asked);
	}

	@Test
	@DisplayName("an endpoint discovered before any admin existed is imported when one arrives")
	void discoveredEndpointWaitsForAnAdmin() {
		ImportWhatIsDiscovered topology = new ImportWhatIsDiscovered();
		EndpointDescription endpoint = Fakes.endpoint("waiting-endpoint");

		topology.endpointChanged(Fakes.added(endpoint), null);

		WillingAdmin admin = new WillingAdmin();
		topology.addAdmin(admin);

		assertThat(admin.importedEndpoints).containsExactly("waiting-endpoint");
	}

	@Test
	@DisplayName("an endpoint that went away again is not imported afterwards")
	void anEndpointThatLeftIsNotRetried() {
		ImportWhatIsDiscovered topology = new ImportWhatIsDiscovered();
		EndpointDescription endpoint = Fakes.endpoint("gone-endpoint");

		topology.endpointChanged(Fakes.added(endpoint), null);
		topology.endpointChanged(Fakes.removed(endpoint), null);

		WillingAdmin admin = new WillingAdmin();
		topology.addAdmin(admin);

		assertThat(admin.importedEndpoints).isEmpty();
	}
}
