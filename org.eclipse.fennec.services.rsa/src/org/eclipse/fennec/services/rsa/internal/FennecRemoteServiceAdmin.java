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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.client.ServiceProxyFactory;
import org.eclipse.fennec.services.rsa.registry.ServiceModels;
import org.eclipse.fennec.services.rsa.spi.ExportedEndpoint;
import org.eclipse.fennec.services.rsa.spi.FlavorDistribution;
import org.eclipse.fennec.services.rsa.spi.RsaProperties;
import org.eclipse.fennec.services.rsa.spi.ServiceDiscovery;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
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
 * <p>One instance per using bundle (122.4.1): what a bundle exported or
 * imported through the service is closed when it lets go of it. The
 * shared view — every export, every import, every listener — lives in
 * {@link Registrations}.
 *
 * <p>The order matters and is the same one this project holds everywhere
 * else: serve before announcing, withdraw before stopping to serve. A
 * consumer must never be sent to an address that has already gone quiet.
 */
@Designate(ocd = FennecRemoteServiceAdmin.Config.class, factory = true)
@Component(service = RemoteServiceAdmin.class, scope = ServiceScope.BUNDLE,
		configurationPid = "org.eclipse.fennec.services.rsa",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FennecRemoteServiceAdmin implements RemoteServiceAdmin {

	private static final Logger LOG = Logger.getLogger(FennecRemoteServiceAdmin.class.getName());

	@ObjectClassDefinition(name = "Fennec Services Remote Service Admin",
			description = "One admin per configuration type: which transport it exports over and which "
					+ "discovery announces it are named here, not searched for.")
	public @interface Config {

		/**
		 * The configuration type this admin serves, and the property the
		 * specification has a topology manager read to find that out
		 * (122.4). Configuration Admin puts it on the registered service,
		 * so what a deployment writes here is what the world sees.
		 */
		@AttributeDefinition(name = "Configuration type",
				description = "e.g. fennec.rest — must be one the targeted distribution supports.")
		String remote_configs_supported() default "fennec.rest";
	}

	/**
	 * The transport this admin exports over.
	 *
	 * <p>Mandatory and static, named by the configuration through
	 * {@code distribution.target} against {@code ddsr.rsa.flavor}. It used
	 * to be a dynamic list that {@code exportService} searched by
	 * configuration type, and — because the search could come up empty
	 * while a framework was still starting — a timer that waited for one
	 * to appear. Both were symptoms of modelling a deployment's decision
	 * as a discovery problem: either the transport this admin is for is
	 * here, and then there is an admin, or it is not, and then there is
	 * none to mislead anybody.
	 *
	 * <p>A node that exports nothing points this at
	 * {@link NothingIsExported}.
	 */
	@Reference
	private FlavorDistribution distribution;

	/** The discovery that announces what this admin exports, likewise named. */
	@Reference
	private ServiceDiscovery discovery;

	@Reference
	private ServiceModels models;

	@Reference
	private DdsrClient client;

	@Reference
	private ServiceProxyFactory proxies;

	@Reference
	private Registrations registrations;

	/** What went through this instance — closed with it. */
	private final List<ExportedService> myExports = new CopyOnWriteArrayList<>();

	private final List<ImportedService> myImports = new CopyOnWriteArrayList<>();

	/** The configuration type this admin is for. */
	private volatile String configType;

	private BundleContext context;

	@Activate
	void activate(BundleContext context, Config config) {
		this.context = context;
		this.configType = config.remote_configs_supported();
		if (serves(configType)) {
			LOG.info("[DDSR] remote service admin for '" + configType + "' ready — exporting over "
					+ distribution.getClass().getSimpleName() + ", announcing with "
					+ discovery.getClass().getSimpleName());
		} else {
			// Not a fault: a consumer-only node says so by targeting the
			// distribution that exports nothing. It still imports.
			LOG.info("[DDSR] remote service admin for '" + configType + "' ready — importing only, "
					+ distribution.getClass().getSimpleName() + " exports nothing");
		}
	}

	/** A changed configuration is taken, not died of (#107). */
	@Modified
	void modified(Config config) {
		this.configType = config.remote_configs_supported();
	}

	/**
	 * The using bundle let go of the service — or this bundle stops. Either
	 * way its registrations do not outlive it (122.4.1).
	 */
	@Deactivate
	void deactivate() {
		for (ExportedService export : List.copyOf(myExports)) {
			export.close();
		}
		for (ImportedService imported : List.copyOf(myImports)) {
			imported.close();
		}
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
		String asked = configTypeOf(effective);
		if (!asked.isEmpty() && !asked.equals(configType)) {
			// Another admin's business. Answering "not mine" is what the
			// specification asks for, and with one admin per transport it
			// is a fact rather than a guess.
			LOG.fine(() -> "[DDSR] " + asked + " is not this admin's configuration type (" + configType + ")");
			return List.of();
		}
		if (!serves(configType)) {
			LOG.fine(() -> "[DDSR] this admin exports nothing — " + Arrays.toString(contracts) + " is not ours");
			return List.of();
		}

		List<String> promised = List.of(distribution.supportedIntents());
		Set<String> intents = intentsAskedFor(effective);
		intents.removeAll(promised);
		if (!intents.isEmpty()) {
			// An intent is a promise about how the call behaves. One this
			// transport cannot keep is not exported — the consumer relying
			// on it would be told a lie.
			LOG.info("[DDSR] intents " + intents + " are not supported over '" + configType + "' — not exporting "
					+ Arrays.toString(contracts));
			return List.of();
		}

		List<ExportRegistration> result = new ArrayList<>(contracts.length);
		for (Class<?> contract : contracts) {
			// One interface, one endpoint: two contracts under one
			// address would make a path mean two things.
			result.add(export(reference, contract, effective, overrides, configType, distribution, discovery,
					promised));
		}
		return result;
	}

	private ExportRegistration export(ServiceReference<?> reference, Class<?> exportedAs,
			Map<String, Object> effective, Map<String, ?> overrides, String configType,
			FlavorDistribution distribution, ServiceDiscovery discovery, List<String> intents) {
		Registrations.ExportKey key = new Registrations.ExportKey(reference, exportedAs.getName(), configType);
		try {
			LiveExport live = registrations.acquire(key,
					() -> open(reference, exportedAs, effective, overrides, configType, distribution, discovery,
							intents));
			ExportedService registration = ExportedService.of(reference, live, registrations::exportUpdated,
					closed -> {
						myExports.remove(closed);
						registrations.exportClosed(closed);
					});
			myExports.add(registration);
			registrations.exported(registration);
			return registration;
		} catch (IllegalArgumentException garbage) {
			// 122.5.1: an export asked for with nonsense — an interface the
			// bundle cannot see, a property that is not what it must be —
			// is the caller's mistake and thrown at the caller.
			throw garbage;
		} catch (RuntimeException failure) {
			// Anything else is ours to report: a registration that carries
			// the failure, and an EXPORT_ERROR for whoever listens.
			LOG.log(Level.WARNING, "[DDSR] exporting " + exportedAs.getName() + " failed", failure);
			ExportedService failed = ExportedService.failed(failure);
			registrations.exportFailed(failed, failure);
			return failed;
		}
	}

	/** Serve, register, announce — in that order — and describe the result. */
	private LiveExport open(ServiceReference<?> reference, Class<?> exportedAs, Map<String, Object> effective,
			Map<String, ?> overrides, String configType, FlavorDistribution distribution,
			ServiceDiscovery discovery, List<String> intents) {
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
			String frameworkUuid = context.getProperty(Constants.FRAMEWORK_UUID);
			EndpointDescription description = Endpoints.describe(effective, reference, exportedAs,
					endpoint.contract(), endpoint.implementation(), frameworkUuid, configType, intents,
					endpoint.properties());

			// An update describes the endpoint again from the service's
			// properties as they are then, with the new overrides — or the
			// ones the export was made with, when the caller passes none.
			ExportedEndpoint served = endpoint;
			AtomicReference<Map<String, ?>> lastOverrides = new AtomicReference<>(
					overrides == null ? null : new LinkedHashMap<>(overrides));
			Function<Map<String, ?>, EndpointDescription> describe = changed -> {
				if (changed != null) {
					lastOverrides.set(new LinkedHashMap<>(changed));
				}
				return Endpoints.describe(effectiveProperties(reference, lastOverrides.get()), reference,
						exportedAs, served.contract(), served.implementation(), frameworkUuid, configType, intents,
						served.properties());
			};

			AutoCloseable registryEntry = inRegistry;
			return new LiveExport(description, endpoint, announcement, () -> {
				close(registryEntry);
				context.ungetService(reference);
			}, describe);
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

	/**
	 * The service's own properties, with the caller's overrides on top.
	 *
	 * <p>Keys are compared without case, as the specification wants
	 * (122.10.10.1): an override spelled {@code MyKey} replaces
	 * {@code mykey}. Two things cannot be overridden whatever the caller
	 * says — {@code objectClass} and {@code service.id} are the
	 * framework's word about the service, not the exporter's.
	 */
	private static Map<String, Object> effectiveProperties(ServiceReference<?> reference, Map<String, ?> overrides) {
		Map<String, Object> effective = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (String key : reference.getPropertyKeys()) {
			effective.put(key, reference.getProperty(key));
		}
		if (overrides != null) {
			effective.putAll(overrides);
		}
		effective.put(Constants.OBJECTCLASS, reference.getProperty(Constants.OBJECTCLASS));
		effective.put(Constants.SERVICE_ID, reference.getProperty(Constants.SERVICE_ID));
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
			if (!List.of(registered).contains(name)) {
				// 122.5.1: an interface the service is not registered
				// under cannot be exported as it, and saying so is required.
				throw new IllegalArgumentException(
						name + " was asked to be exported, but the service is registered as "
								+ Arrays.toString(registered));
			}
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
		if (asked instanceof Collection<?> several && !several.isEmpty()) {
			return String.valueOf(several.iterator().next());
		}
		// Nothing asked for: this admin's own type answers, because there
		// is exactly one and the deployment named it.
		return "";
	}

	/**
	 * The intents the exporter asks for — {@code service.exported.intents}
	 * and {@code .intents.extra} together, either as one string, a list
	 * or an array.
	 */
	private static Set<String> intentsAskedFor(Map<String, Object> effective) {
		Set<String> intents = new LinkedHashSet<>();
		for (String property : List.of(RemoteConstants.SERVICE_EXPORTED_INTENTS,
				RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA)) {
			Object asked = effective.get(property);
			if (asked instanceof String single) {
				Arrays.stream(single.split("[,\\s]+")).map(String::trim).filter(s -> !s.isEmpty())
						.forEach(intents::add);
			} else if (asked instanceof String[] several) {
				intents.addAll(List.of(several));
			} else if (asked instanceof Collection<?> several) {
				several.forEach(entry -> intents.add(String.valueOf(entry)));
			}
		}
		return intents;
	}





	/** Whether the bound distribution can serve the type this admin is for. */
	private boolean serves(String type) {
		return List.of(distribution.supportedConfigs()).contains(type);
	}

	@Override
	public Collection<ExportReference> getExportedServices() {
		return registrations.exports();
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
		// 122.4.2: an endpoint that names no configuration type this admin
		// speaks is not ours to import, whatever else it says. Answering
		// otherwise would claim an endpoint another admin can actually
		// reach.
		List<String> configTypes = endpoint.getConfigurationTypes();
		if (!configTypes.contains(configType)) {
			LOG.fine(() -> "[DDSR] not importing " + endpoint.getId() + ": " + configTypes
					+ " is not a configuration type this admin speaks");
			return null;
		}
		Object contract = endpoint.getProperties().get(RsaProperties.CONTRACT);
		if (contract == null) {
			LOG.fine(() -> "[DDSR] not importing " + endpoint.getId() + ": it names no contract of ours");
			return null;
		}
		Object implementationId = endpoint.getProperties().get(RsaProperties.IMPLEMENTATION);
		List<ServiceLocator> candidates = client.consumer().find(contract.toString(), null);
		Optional<ServiceLocator> found = candidates.stream()
				.filter(locator -> implementationId == null || locator.implementation() == null
						|| implementationId.equals(locator.implementation().getImplementationId()))
				.findFirst();
		if (found.isEmpty()) {
			LOG.info("[DDSR] not importing " + endpoint.getId() + ": the broker lists " + candidates.size()
					+ " registration(s) of " + contract + candidates.stream()
							.map(locator -> locator.implementation() == null ? "?"
									: locator.implementation().getImplementationId())
							.toList()
					+ ", none of them " + implementationId);
			return null;
		}

		ImportedService imported = new ImportedService(endpoint, found.get(), proxies, registrations::importUpdated,
				closed -> {
					myImports.remove(closed);
					registrations.importClosed(closed);
				});
		try {
			imported.register(ProxyHost.in(context).getBundleContext());
		} catch (BundleException | IOException noHost) {
			LOG.log(Level.WARNING, "[DDSR] no proxy host bundle — cannot import " + endpoint.getId(), noHost);
			return null;
		}
		myImports.add(imported);
		registrations.imported(imported);
		LOG.info("[DDSR] imported " + endpoint.getId() + " as " + endpoint.getInterfaces());
		return imported;
	}

	@Override
	public Collection<ImportReference> getImportedEndpoints() {
		return registrations.imports();
	}
}
