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

import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.eclipse.fennec.services.rsa.spi.ServiceDiscovery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Which listeners start an import, and which never do.
 *
 * <p>A listener hook sees every service listener in the framework — the
 * first run of this component watched the broker for ConfigurationAdmin,
 * LogService and the broker's own API. What must never be imported is a
 * rule of this component, and this pins it.
 */
class ImportWhatIsAskedForTest {

	/** Records which contracts were asked for; reports nothing back. */
	private static final class RecordingDiscovery implements ServiceDiscovery {
		final List<String> watched = new ArrayList<>();
		/** The watch itself, so a test can play discovery to it. */
		DiscoveryListener listener;

		@Override
		public String[] supportedConfigs() {
			return new String[] { "fennec.rest" };
		}

		@Override
		public AutoCloseable announce(ExportedEndpoint endpoint) {
			return () -> { };
		}

		@Override
		public AutoCloseable watch(String contractName, DiscoveryListener listener) {
			watched.add(contractName);
			this.listener = listener;
			return () -> { };
		}
	}

	private static class IdleAdmin implements RemoteServiceAdmin {
		@Override
		public Collection<ExportRegistration> exportService(ServiceReference<?> reference, Map<String, ?> properties) {
			return List.of();
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

	private static ListenerInfo listenerFor(String filter) {
		return new ListenerInfo() {
			@Override
			public BundleContext getBundleContext() {
				return null;
			}

			@Override
			public String getFilter() {
				return filter;
			}

			@Override
			public boolean isRemoved() {
				return false;
			}
		};
	}

	@Test
	void anApplicationInterfaceStartsAWatchForItsContract() {
		RecordingDiscovery discovery = new RecordingDiscovery();
		ImportWhatIsAskedFor topology = new ImportWhatIsAskedFor().with(new IdleAdmin(), discovery);

		topology.added(List.of(listenerFor("(objectClass=org.eclipse.fennec.services.examples.rsa.api.Greeter)")));

		assertThat(discovery.watched).containsExactly("Greeter");
		assertThat(topology.watched()).containsExactly("org.eclipse.fennec.services.examples.rsa.api.Greeter");
	}

	@Test
	void aListenerMayNameTheContractItself() {
		RecordingDiscovery discovery = new RecordingDiscovery();
		new ImportWhatIsAskedFor().with(new IdleAdmin(), discovery)
				.added(List.of(listenerFor("(&(objectClass=com.acme.PaymentApi)(ddsr.contract=Payment))")));

		assertThat(discovery.watched).containsExactly("Payment");
	}

	@Test
	void frameworkAndBrokerInterfacesAreNeverRemote() {
		RecordingDiscovery discovery = new RecordingDiscovery();
		new ImportWhatIsAskedFor().with(new IdleAdmin(), discovery).added(List.of(
				listenerFor("(objectClass=org.osgi.service.cm.ConfigurationAdmin)"),
				listenerFor("(objectClass=org.apache.felix.service.command.CommandProcessor)"),
				listenerFor("(objectClass=org.eclipse.fennec.services.broker.core.BrokerLookup)"),
				listenerFor("(objectClass=org.eclipse.fennec.services.client.DdsrClient)")));

		assertThat(discovery.watched).as("none of these could ever be somebody else's service").isEmpty();
	}

	@Test
	@DisplayName("a provider that appeared while no admin existed is imported when one arrives")
	void whatAppearedTooEarlyIsNotLost() {
		RecordingDiscovery discovery = new RecordingDiscovery();
		ImportWhatIsAskedFor topology = new ImportWhatIsAskedFor().with(new IdleAdmin(), discovery);
		topology.added(List.of(listenerFor("(objectClass=com.acme.Thing)")));

		ServiceImplementation implementation = ServicesFactory.eINSTANCE.createServiceImplementation();
		implementation.setImplementationId("thing-1");
		discovery.listener.appeared("endpoint-1", implementation);

		RecordingAdmin late = new RecordingAdmin();
		topology.addAdmin(late);

		assertThat(late.imported).as("discovery reports a provider once; this is the only second chance")
				.containsExactly("endpoint-1");
	}

	@Test
	@DisplayName("a discovery that comes up later is watched by the standing watches too")
	void aLateDiscoveryIsAlsoWatched() {
		RecordingDiscovery first = new RecordingDiscovery();
		ImportWhatIsAskedFor topology = new ImportWhatIsAskedFor().with(new IdleAdmin(), first);
		topology.added(List.of(listenerFor("(objectClass=com.acme.Thing)")));

		RecordingDiscovery later = new RecordingDiscovery();
		topology.addDiscovery(later);

		assertThat(later.watched).as("it may know a provider the first one does not").containsExactly("Thing");
	}

	/** An admin that takes every endpoint and says which ones it was given. */
	private static final class RecordingAdmin extends IdleAdmin {
		final List<String> imported = new ArrayList<>();

		@Override
		public ImportRegistration importService(EndpointDescription endpoint) {
			imported.add(endpoint.getId());
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
	}

	@Test
	void oneWatchServesHoweverManyListenersAndEndsWithTheLast() {
		RecordingDiscovery discovery = new RecordingDiscovery();
		ImportWhatIsAskedFor topology = new ImportWhatIsAskedFor().with(new IdleAdmin(), discovery);
		ListenerInfo first = listenerFor("(objectClass=com.acme.Thing)");
		ListenerInfo second = listenerFor("(objectClass=com.acme.Thing)");

		topology.added(List.of(first, second));
		assertThat(discovery.watched).as("two listeners, one watch").containsExactly("Thing");

		topology.removed(List.of(first));
		assertThat(topology.watched()).as("still wanted by the second").containsExactly("com.acme.Thing");

		topology.removed(List.of(second));
		assertThat(topology.watched()).as("nobody left who wants it").isEmpty();
	}
}
