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

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.invocation.ServiceMethods;
import org.eclipse.fennec.tags.service.TagService;

/**
 * The broker's lookup under one tag: the same answer the broker gives,
 * with what the tag does not cover taken out (#279). Served from a copy
 * of the broker's own {@code BrokerLookup} document, so a consumer reads
 * it exactly as it reads the broker.
 *
 * <p>Asked for one of the broker's consumer contracts, it answers the
 * facade's address under the same tag instead of the broker's: a consumer
 * that finds the broker's API through the facade stays in the facade.
 */
public final class ScopedLookup {

	private final Supplier<Object> broker;

	private final TagService tags;

	private final String tag;

	/** The references shown under this tag — shared with its events, which pass on what becomes of them. */
	private final Set<String> shown;

	ScopedLookup(Supplier<Object> broker, TagService tags, String tag, Set<String> shown) {
		this.broker = broker;
		this.tags = tags;
		this.tag = tag;
		this.shown = shown;
	}

	public LocalServiceRegistry getServiceReferences(String interfaceName, String filter, String flavors,
			String consumerId, String fingerprint) throws Throwable {
		LocalServiceRegistry answer = (LocalServiceRegistry) call(broker.get(), "getServiceReferences",
				interfaceName, filter, flavors, consumerId, fingerprint);
		if (answer == null) {
			return null;
		}
		Visibility visible = Visibility.under(tags, tag);
		Set<ServiceProvider> kept = new HashSet<>();
		answer.getReferences().removeIf(reference -> {
			ServiceProvider provider = reference.getProvider();
			boolean visibleHere = provider != null && visible.implementation(provider.getName(), interfaceName);
			if (visibleHere) {
				kept.add(provider);
				this.shown.add(reference.getId());
			}
			return !visibleHere;
		});
		answer.getProviders().removeIf(provider -> !kept.contains(provider));
		if (Visibility.INFRASTRUCTURE.contains(interfaceName)) {
			for (ServiceProvider provider : answer.getProviders()) {
				for (ServiceImplementation implementation : provider.getImplementations()) {
					for (ServiceFlavor flavor : implementation.getFlavors()) {
						if (flavor instanceof RestFlavor rest) {
							rest.setBasePath(scoped(rest.getBasePath(), tag));
						}
					}
				}
			}
		}
		return answer;
	}

	/**
	 * The facade's address of a broker contract: the tag goes before the
	 * contract's mount point, {@code /ddsr/rest/references} becomes
	 * {@code /ddsr/rest/scoped/<tag>/references}.
	 */
	static String scoped(String basePath, String tag) {
		if (basePath == null) {
			return null;
		}
		int mount = basePath.lastIndexOf('/');
		return mount < 0 ? basePath
				: basePath.substring(0, mount) + "/" + TagFacade.SCOPED + "/" + tag + basePath.substring(mount);
	}

	/** A method of the broker's contract adapter, by name and arity, as the dispatcher would call it. */
	static Object call(Object target, String operation, Object... arguments) throws Throwable {
		if (target == null) {
			throw new IllegalStateException("the broker's implementation of " + operation + " is not there");
		}
		try {
			return ServiceMethods.require(target, operation, arguments.length).invoke(target, arguments);
		} catch (InvocationTargetException failed) {
			throw failed.getCause();
		}
	}

}
