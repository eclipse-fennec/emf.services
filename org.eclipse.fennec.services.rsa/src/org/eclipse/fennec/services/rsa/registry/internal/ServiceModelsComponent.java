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

package org.eclipse.fennec.services.rsa.registry.internal;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistry;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;
import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.RegistryKind;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.derive.JavaContracts;
import org.eclipse.fennec.services.rsa.registry.ServiceModelCapability;
import org.eclipse.fennec.services.rsa.registry.ServiceModels;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * The local service registry.
 *
 * <p>Contracts come from two places and are indistinguishable afterwards:
 * a bundle that brought its own model, and an interface that was read.
 * Which it was is a property of the registry entry, not of the contract —
 * that is what makes a provider who writes his model and one who does
 * not the same kind of provider from here on, and it is what lets a
 * consumer in another language reach both.
 *
 * <p>The store underneath is an {@code EObjectRegistry}: named,
 * key-addressed, fed by providers. {@link LocalServiceRegistry} is the
 * view — what this framework provides, what it is bound to, and the
 * remote registry behind them.
 */
@Designate(ocd = ServiceModelsComponent.Config.class)
@Component(service = ServiceModels.class, configurationPid = "org.eclipse.fennec.services.rsa.registry")
public class ServiceModelsComponent implements ServiceModels {

	private static final Logger LOG = Logger.getLogger(ServiceModelsComponent.class.getName());

	/** What this component calls itself when it writes a derived contract. */
	static final String DERIVED = "derived";

	@ObjectClassDefinition(name = "Fennec Services Local Registry",
			description = "The local view of what this framework provides and knows.")
	public @interface Config {

		@AttributeDefinition(name = "Registry name",
				description = "Name of this framework's service registry, as it appears in the model.")
		String registry_name() default "local";

		@AttributeDefinition(name = "Default contract version",
				description = "The version a derived contract gets when the service names none.")
		String default_version() default "1.0.0";
	}

	@Reference(target = "(emf.eobject.registry.name=ddsr.contracts)")
	private EObjectRegistry contracts;

	@Reference(target = "(emf.eobject.registry.name=ddsr.contracts)")
	private EObjectRegistryWriter writer;

	private LocalServiceRegistry registry;
	private Config config;

	@Activate
	void activate(Config config) {
		this.config = config;
		this.registry = ServicesFactory.eINSTANCE.createLocalServiceRegistry();
		registry.setName(config.registry_name());
		registry.setKind(RegistryKind.LOCAL);
	}

	/**
	 * A changed configuration renames the registry; it does not replace
	 * it.
	 *
	 * <p>Without this method the component runtime would build a second
	 * {@code LocalServiceRegistry} and throw the first away — together
	 * with every implementation added to it — whenever Configuration
	 * Admin delivers the configuration again, which it does more than
	 * once while a framework starts (#107).
	 */
	@Modified
	void modified(Config config) {
		this.config = config;
		if (!config.registry_name().equals(registry.getName())) {
			LOG.info("[DDSR] the local service registry is now called " + config.registry_name());
			registry.setName(config.registry_name());
		}
	}

	@Override
	public LocalServiceRegistry registry() {
		return registry;
	}

	@Override
	public Optional<ServiceInterface> contract(String name) {
		return contracts.get(name)
				.filter(ServiceInterface.class::isInstance)
				.map(ServiceInterface.class::cast);
	}

	@Override
	public ServiceInterface contractFor(Class<?> serviceInterface, Map<String, ?> properties) {
		String name = contractName(serviceInterface, properties);
		Optional<ServiceInterface> known = contract(name);
		if (known.isPresent()) {
			// The provider brought a model. It says more than any
			// derivation can — overruling it would make the document
			// decorative.
			return known.get();
		}
		ServiceInterface derived = JavaContracts.contractOf(serviceInterface, version(properties));
		derived.setName(name);
		writer.put(DERIVED, name, derived, Map.of("interface", serviceInterface.getName()));
		LOG.info("[DDSR] derived a contract for " + serviceInterface.getName() + " as " + name);
		return derived;
	}

	@Override
	public AutoCloseable add(ServiceImplementation implementation) {
		ServiceProvider provider = implementation.eContainer() instanceof ServiceProvider owner
				? owner
				: wrap(implementation);
		if (!registry.getProviders().contains(provider)) {
			registry.getProviders().add(provider);
		}
		return () -> registry.getProviders().remove(provider);
	}

	/**
	 * The name of the contract a service implements: what it says, or
	 * what its interface is called. A native provider states it in
	 * {@code ddsr.contract} — the same property the generic REST
	 * distribution looks it up by — so it says it once and everything
	 * finds it.
	 */
	private static String contractName(Class<?> serviceInterface, Map<String, ?> properties) {
		Object stated = properties == null ? null : properties.get(ServiceModelCapability.CONTRACT_PROPERTY);
		return stated == null || stated.toString().isBlank()
				? serviceInterface.getSimpleName()
				: stated.toString();
	}

	private String version(Map<String, ?> properties) {
		Object stated = properties == null ? null : properties.get("ddsr.contract.version");
		return stated == null || stated.toString().isBlank() ? config.default_version() : stated.toString();
	}

	private static ServiceProvider wrap(ServiceImplementation implementation) {
		ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
		provider.setName(implementation.getName());
		provider.setVersion(implementation.getVersion());
		provider.getImplementations().add(implementation);
		return provider;
	}

	/** Every contract this framework knows, whatever it came from. */
	Iterable<EObject> known() {
		return contracts.entries().stream().map(entry -> entry.object()).toList();
	}
}
