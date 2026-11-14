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
import java.util.List;
import java.util.Set;

import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.tags.MatchMode;
import org.eclipse.fennec.tags.service.TagService;

/**
 * What a caller of the facade under one tag may see (#279): a
 * registration is visible when its implementation or its contract carries
 * a tag at or under the path — whatever the origin, administrator,
 * provider or rule. The broker's own consumer contracts are always
 * visible: they are how the caller reaches the facade at all.
 *
 * <p>Taken once per request: the tags are worked out when a query asks,
 * so this is a snapshot of that moment.
 */
final class Visibility {

	/** The broker contracts a consumer needs to find the facade itself. */
	static final Set<String> INFRASTRUCTURE = Set.of("BrokerLookup", "BrokerEvents", "BrokerCatalog");

	private final Set<String> contracts;

	private final Set<String> implementations;

	private Visibility(Set<String> contracts, Set<String> implementations) {
		this.contracts = contracts;
		this.implementations = implementations;
	}

	static Visibility under(TagService tags, String tag) {
		List<String> path = List.of(tag);
		return new Visibility(new HashSet<>(tags.find(path, MatchMode.ANY, DdsrTagSubjects.CONTRACT)),
				new HashSet<>(tags.find(path, MatchMode.ANY, DdsrTagSubjects.IMPLEMENTATION)));
	}

	/** Whether a contract, and everything that implements it, is visible. */
	boolean contract(String name) {
		return name != null && (INFRASTRUCTURE.contains(name) || contracts.contains(name));
	}

	/** Whether a provider's implementation of a contract is visible. */
	boolean implementation(String provider, String contract) {
		return contract(contract) || implementations.contains(DdsrTagSubjects.keyOf(provider, contract));
	}

	/** Whether an implementation is visible for any contract it serves. */
	boolean implementation(ServiceImplementation implementation) {
		String provider = implementation.eContainer() instanceof ServiceProvider owner ? owner.getName() : null;
		for (ServiceInterface contract : implementation.getServiceInterfaces()) {
			if (provider != null && implementation(provider, contract.getName())) {
				return true;
			}
		}
		return false;
	}
}
