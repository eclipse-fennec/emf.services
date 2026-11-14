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

package org.eclipse.fennec.tags.ddsr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.tags.service.TagSubjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The services registry as a source of things to tag: contracts and
 * implementations.
 *
 * <ul>
 * <li>{@code contract} — every contract in the catalog, keyed by its
 * interface name (not its fingerprint, so a tag survives a change of the
 * contract), asking for the tags in {@code ServiceInterface.tags};</li>
 * <li>{@code implementation} — every live implementation, once per
 * contract it serves, keyed {@code <provider>/<contract>}, with its
 * properties and the tags in {@code ServiceImplementation.tags}.</li>
 * </ul>
 *
 * <p>Read through the catalog contract only ({@code listCatalog}), so the
 * overlay can move to a node of its own and read the same over REST. An
 * implementation parked in the cold cache is not in the registry and is
 * not answered until a lookup brings it back.
 */
@Component(service = TagSubjects.class)
public final class DdsrTagSubjects implements TagSubjects {

	public static final String CONTRACT = "contract";
	public static final String IMPLEMENTATION = "implementation";

	private final BrokerCatalog catalog;

	@Activate
	public DdsrTagSubjects(@Reference BrokerCatalog catalog) {
		this.catalog = catalog;
	}

	@Override
	public Set<String> kinds() {
		return Set.of(CONTRACT, IMPLEMENTATION);
	}

	@Override
	public Collection<Subject> subjects(String kind) {
		// The registry is the broker's live document; it may change while
		// it is read. Reading again is cheaper than holding the broker.
		for (int attempt = 0;; attempt++) {
			try {
				return read(kind, catalog.getRegistry());
			} catch (ConcurrentModificationException changed) {
				if (attempt == 2) {
					throw changed;
				}
			}
		}
	}

	private static Collection<Subject> read(String kind, RemoteServiceRegistry registry) {
		List<Subject> subjects = new ArrayList<>();
		if (registry == null) {
			return subjects;
		}
		if (CONTRACT.equals(kind)) {
			for (ServiceInterface contract : List.copyOf(registry.getCatalog())) {
				if (contract.getName() != null) {
					subjects.add(new Subject(CONTRACT, contract.getName(), Map.of(), List.copyOf(contract.getTags())));
				}
			}
		} else if (IMPLEMENTATION.equals(kind)) {
			for (ServiceImplementation implementation : List.copyOf(registry.getImplementations())) {
				String provider = implementation.eContainer() instanceof ServiceProvider owner ? owner.getName() : null;
				if (provider == null) {
					continue;
				}
				Map<String, Object> properties = propertiesOf(implementation, provider);
				for (ServiceInterface contract : List.copyOf(implementation.getServiceInterfaces())) {
					if (contract.getName() != null) {
						subjects.add(new Subject(IMPLEMENTATION, keyOf(provider, contract.getName()), properties,
								List.copyOf(implementation.getTags())));
					}
				}
			}
		}
		return subjects;
	}

	/** How an implementation is named here: provider and contract, joined by '/'. */
	public static String keyOf(String provider, String contract) {
		return provider + "/" + contract;
	}

	/** What a rule may filter on: the implementation's properties, and who provides it. */
	private static Map<String, Object> propertiesOf(ServiceImplementation implementation, String provider) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ddsr.provider.name", provider);
		if (implementation.getName() != null) {
			properties.put("ddsr.implementation.name", implementation.getName());
		}
		for (Property property : List.copyOf(implementation.getProperties())) {
			Object value = valueOf(property);
			if (property.getName() != null && value != null) {
				properties.put(property.getName(), value);
			}
		}
		return properties;
	}

	private static Object valueOf(EObject property) {
		EStructuralFeature value = property.eClass().getEStructuralFeature("value");
		return value == null ? null : property.eGet(value);
	}
}
