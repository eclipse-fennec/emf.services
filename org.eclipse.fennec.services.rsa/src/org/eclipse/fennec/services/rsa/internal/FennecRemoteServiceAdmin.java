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

package org.eclipse.fennec.services.rsa.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Optional;

import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.client.ServiceProxyFactory;
import org.eclipse.fennec.services.rsa.registry.ServiceModels;
import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.eclipse.fennec.services.rsa.spi.ServiceDiscovery;
import org.eclipse.fennec.services.rsa.spi.FlavorDistribution;
import org.eclipse.fennec.services.rsa.spi.RsaProperties;
import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Remote Service Admin over this registry.
 *
 * <p>It owns no transport. Exporting is two steps that belong to other
 * people: a {@link FlavorDistribution} makes the service reachable over
 * one flavor, and a {@link ServiceDiscovery} tells the world. Which ones
 * run is chosen the way the specification says — by configuration type.
 * That is what lets an MQTT export be a new bundle rather than a change
 * here.
 *
 * <p>The order matters and is the same one this project holds everywhere
 * else: serve before announcing, withdraw before stopping to serve. A
 * consumer must never be sent to an address that has already gone quiet.
 */
// remote.configs.supported is what the specification says an admin
// advertises, and what a topology manager — and the TCK — reads to learn
// which configuration types it can export with. Static for now, which is
// wrong the moment a second distribution is installed (#98): the honest
// value is the union of what the bound FlavorDistributions support, and
// that has to become a dynamic service property.
@Component(service = RemoteServiceAdmin.class,
		property = RemoteConstants.REMOTE_CONFIGS_SUPPORTED + "=fennec.rest")
public class FennecRemoteServiceAdmin implements RemoteServiceAdmin {

	private static final Logger LOG = Logger.getLogger(FennecRemoteServiceAdmin.class.getName());

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private volatile List<FlavorDistribution> distributions = new CopyOnWriteArrayList<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	private volatile List<ServiceDiscovery> discoveries = new CopyOnWriteArrayList<>();

	@Reference
	private ServiceModels models;

	@Reference
	private DdsrClient client;

	@Reference
	private ServiceProxyFactory proxies;

	private final List<ImportedService> imports = new CopyOnWriteArrayList<>();

	private final List<ExportedService> exported = new CopyOnWriteArrayList<>();

	private BundleContext context;

	@Activate
	void activate(BundleContext context) {
		this.context = context;
	}

	@Override
	public Collection<ExportRegistration> exportService(ServiceReference<?> reference,
			Map<String, ?> overrides) {
		Map<String, Object> effective = effectiveProperties(reference, overrides);

		Class<?>[] contracts = contractsOf(reference, effective);
		if (contracts.length == 0) {
			// Nothing was asked to be exported. Not an error — the
			// specification says an empty collection means "not mine".
			return List.of();
		}
		String configType = configTypeOf(effective);
		FlavorDistribution distribution = distributionFor(configType);
		ServiceDiscovery discovery = discoveryFor(configType);
		if (distribution == null || discovery == null) {
			LOG.info("[DDSR] no " + (distribution == null ? "distribution" : "discovery")
					+ " for configuration type '" + configType + "' — not exporting "
					+ Arrays.toString(contracts));
			return List.of();
		}

		List<ExportRegistration> registrations = new ArrayList<>(contracts.length);
		for (Class<?> contract : contracts) {
			// One interface, one endpoint: two contracts under one
			// address would make a path mean two things.
			registrations.add(export(reference, contract, effective, configType, distribution, discovery));
		}
		return registrations;
	}

	private ExportRegistration export(ServiceReference<?> reference, Class<?> exportedAs,
			Map<String, Object> effective, String configType, FlavorDistribution distribution,
			ServiceDiscovery discovery) {
		Object service = context.getService(reference);
		ExportedEndpoint endpoint = null;
		AutoCloseable announcement = null;
		AutoCloseable inRegistry = null;
		try {
			// The registry answers with the provider's own model when
			// there is one, and derives only otherwise. From here on
			// nothing can tell which it was.
			ServiceInterface contract = models.contractFor(exportedAs, effective);
			endpoint = distribution.export(service, contract, effective);
			inRegistry = models.add(endpoint.implementation());
			announcement = discovery.announce(endpoint);
			EndpointDescription description = Endpoints.describe(reference, exportedAs, endpoint.contract(),
					endpoint.implementation(), context.getProperty(Constants.FRAMEWORK_UUID), configType);

			AutoCloseable registryEntry = inRegistry;
			ExportedService live = new ExportedService(reference, description, endpoint, announcement,
					this::forget, () -> {
						close(registryEntry);
						context.ungetService(reference);
					});
			exported.add(live);
			return live;
		} catch (RuntimeException failure) {
			// Undo in the opposite order of doing, so nothing is left
			// announced that is no longer served.
			close(announcement);
			close(inRegistry);
			if (endpoint != null) {
				endpoint.close();
			}
			context.ungetService(reference);
			throw failure;
		}
	}

	private void forget(ExportedService service) {
		exported.remove(service);
	}

	private static void close(AutoCloseable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception failure) {
			LOG.log(Level.WARNING, "[DDSR] closing an export failed", failure);
		}
	}

	/** The service's own properties, with the caller's overrides on top. */
	private static Map<String, Object> effectiveProperties(ServiceReference<?> reference, Map<String, ?> overrides) {
		Map<String, Object> effective = new LinkedHashMap<>();
		for (String key : reference.getPropertyKeys()) {
			effective.put(key, reference.getProperty(key));
		}
		if (overrides != null) {
			effective.putAll(overrides);
		}
		return effective;
	}

	/**
	 * The interfaces to export: what {@code service.exported.interfaces}
	 * names, or every interface the service is registered under when it
	 * says {@code *}.
	 */
	private Class<?>[] contractsOf(ServiceReference<?> reference, Map<String, Object> effective) {
		Object asked = effective.get(RemoteConstants.SERVICE_EXPORTED_INTERFACES);
		String[] registered = (String[]) reference.getProperty(Constants.OBJECTCLASS);
		List<String> wanted = new ArrayList<>();
		if (asked instanceof String single) {
			wanted.addAll("*".equals(single.trim())
					? List.of(registered)
					: Arrays.stream(single.split(",")).map(String::trim).toList());
		} else if (asked instanceof String[] several) {
			wanted.addAll(List.of(several));
		} else if (asked instanceof Collection<?> several) {
			several.forEach(entry -> wanted.add(String.valueOf(entry)));
		} else {
			return new Class<?>[0];
		}

		List<Class<?>> contracts = new ArrayList<>(wanted.size());
		for (String name : wanted) {
			try {
				contracts.add(reference.getBundle().loadClass(name));
			} catch (ClassNotFoundException notVisible) {
				throw new IllegalArgumentException(
						"the exporting bundle cannot see " + name + ", which it asked to be exported as",
						notVisible);
			}
		}
		return contracts.toArray(Class<?>[]::new);
	}

	/**
	 * Which configuration type to export under. A service that names one
	 * gets it; one that names none gets whatever is installed, which is
	 * the only useful reading of silence in a deployment that has a
	 * single transport.
	 */
	private String configTypeOf(Map<String, Object> effective) {
		Object asked = effective.get(RemoteConstants.SERVICE_EXPORTED_CONFIGS);
		if (asked instanceof String single && !single.isBlank()) {
			return single.trim();
		}
		if (asked instanceof String[] several && several.length > 0) {
			return several[0];
		}
		List<FlavorDistribution> installed = distributions;
		return installed.isEmpty() || installed.get(0).supportedConfigs().length == 0
				? ""
				: installed.get(0).supportedConfigs()[0];
	}

	private FlavorDistribution distributionFor(String configType) {
		for (FlavorDistribution candidate : distributions) {
			if (List.of(candidate.supportedConfigs()).contains(configType)) {
				return candidate;
			}
		}
		return null;
	}

	private ServiceDiscovery discoveryFor(String configType) {
		for (ServiceDiscovery candidate : discoveries) {
			if (List.of(candidate.supportedConfigs()).contains(configType)) {
				return candidate;
			}
		}
		return null;
	}

	@Override
	public Collection<ExportReference> getExportedServices() {
		return List.copyOf(exported.stream().map(ExportRegistration::getExportReference).toList());
	}

	/**
	 * Import an endpoint: find it in the broker, wrap it in a proxy,
	 * register the proxy here.
	 *
	 * <p>No per-flavor SPI on this side, and that is not an omission.
	 * The consumer half of the SDK already abstracts the transport — a
	 * {@link ServiceLocator} knows the flavor it was found with and the
	 * {@link ServiceProxyFactory} builds a proxy over whichever it is. The
	 * export needed a seam because serving is transport work; importing
	 * is finding and calling, and both exist.
	 *
	 * <p>{@code null} when the endpoint is not one this admin can import:
	 * not ours, or not (yet) in the broker. That is what the
	 * specification says to answer, and a topology manager treats it as
	 * "ask someone else".
	 */
	@Override
	public ImportRegistration importService(EndpointDescription endpoint) {
		Object contract = endpoint.getProperties().get(RsaProperties.CONTRACT);
		if (contract == null) {
			LOG.fine(() -> "[DDSR] not importing " + endpoint.getId() + ": it names no contract of ours");
			return null;
		}
		Object implementationId = endpoint.getProperties().get(RsaProperties.IMPLEMENTATION);
		Optional<ServiceLocator> found = client.consumer().find(contract.toString(), null).stream()
				.filter(locator -> implementationId == null || locator.implementation() == null
						|| implementationId.equals(locator.implementation().getImplementationId()))
				.findFirst();
		if (found.isEmpty()) {
			LOG.info("[DDSR] not importing " + endpoint.getId() + ": the broker holds no such registration");
			return null;
		}

		ImportedService imported = new ImportedService(endpoint, found.get(), proxies, imports::remove);
		try {
			imported.register(ProxyHost.in(context).getBundleContext());
		} catch (BundleException | IOException noHost) {
			LOG.log(Level.WARNING, "[DDSR] no proxy host bundle — cannot import " + endpoint.getId(), noHost);
			return null;
		}
		imports.add(imported);
		LOG.info("[DDSR] imported " + endpoint.getId() + " as " + endpoint.getInterfaces());
		return imported;
	}

	@Override
	public Collection<ImportReference> getImportedEndpoints() {
		return List.copyOf(imports);
	}
}
