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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.fennec.services.RemoteServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.broker.core.exception.CatalogEntryNotFound;
import org.eclipse.fennec.tags.service.TagService;

/**
 * The broker's catalog under one tag, read only (#279): the contracts the
 * tag covers, and those a covered implementation serves. Served from a
 * copy of the broker's {@code BrokerCatalog} document with its two reading
 * operations; changing the catalog is not a consumer's business and is
 * not offered here.
 */
public final class ScopedCatalog {

	private final Supplier<Object> broker;

	private final TagService tags;

	private final String tag;

	ScopedCatalog(Supplier<Object> broker, TagService tags, String tag) {
		this.broker = broker;
		this.tags = tags;
		this.tag = tag;
	}

	public RemoteServiceRegistry listCatalog() throws Throwable {
		RemoteServiceRegistry live = (RemoteServiceRegistry) ScopedLookup.call(broker.get(), "listCatalog");
		// The providers are roots of their own beside the registry, not in
		// it: one copier for both, so the copy points at copies only and
		// travels as the broker's answer does, providers as sibling roots.
		Copier copier = new Copier();
		RemoteServiceRegistry copy = (RemoteServiceRegistry) copier.copy(live);
		copier.copyAll(live.getProviders());
		copier.copyReferences();
		Visibility visible = Visibility.under(tags, tag);
		copy.getImplementations().removeIf(implementation -> !visible.implementation(implementation));
		copy.getProviders().forEach(provider -> provider.getImplementations()
				.removeIf(implementation -> !visible.implementation(implementation)));
		copy.getProviders().removeIf(provider -> provider.getImplementations().isEmpty());
		Set<String> served = new HashSet<>();
		for (ServiceImplementation implementation : copy.getImplementations()) {
			for (ServiceInterface contract : implementation.getServiceInterfaces()) {
				served.add(contract.getName());
			}
		}
		copy.getCatalog().removeIf(contract -> !visible.contract(contract.getName())
				&& !served.contains(contract.getName()));
		return copy;
	}

	public ServiceInterface getCatalogEntry(String name, String fingerprint) throws Throwable {
		for (ServiceInterface contract : listCatalog().getCatalog()) {
			if (contract.getName().equals(name)) {
				return (ServiceInterface) ScopedLookup.call(broker.get(), "getCatalogEntry", name, fingerprint);
			}
		}
		throw new CatalogEntryNotFound("no catalog entry named '" + name + "' under '" + tag + "'");
	}
}
