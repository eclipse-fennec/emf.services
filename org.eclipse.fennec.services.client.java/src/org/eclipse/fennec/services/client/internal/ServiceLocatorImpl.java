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

package org.eclipse.fennec.services.client.internal;

import java.net.URI;
import java.util.Optional;

import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceOperationFlavor;
import org.eclipse.fennec.services.ServiceReference;

final class ServiceLocatorImpl implements ServiceLocator {

	private final ServiceReference reference;
	private final ServiceImplementation implementation;

	ServiceLocatorImpl(ServiceReference reference, ServiceImplementation implementation) {
		this.reference = reference;
		this.implementation = implementation;
	}

	@Override
	public ServiceReference reference() {
		return reference;
	}

	@Override
	public ServiceImplementation implementation() {
		return implementation;
	}

	@Override
	public Optional<RestFlavor> restFlavor() {
		if (implementation == null) {
			return Optional.empty();
		}
		for (ServiceFlavor f : implementation.getFlavors()) {
			if (f instanceof RestFlavor) {
				return Optional.of((RestFlavor) f);
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<URI> urlFor(String operationName) {
		RestFlavor rf = restFlavor().orElse(null);
		if (rf == null || operationName == null) {
			return Optional.empty();
		}
		RestOperationFlavor opFlavor = findOpFlavor(rf, operationName);
		if (opFlavor == null) {
			return Optional.empty();
		}
		String host = rf.getHost();
		if (host == null || host.isBlank()) {
			// The broker is expected to self-publish its RestFlavor with
			// a populated host. If we land here the broker config is
			// incomplete — we can't make up a URL, return empty.
			return Optional.empty();
		}
		String basePath = nullToEmpty(rf.getBasePath());
		String opPath = nullToEmpty(opFlavor.getPath());
		return Optional.of(URI.create(stripTrailingSlash(host) + ensureLeadingSlash(basePath) + ensureLeadingSlash(opPath)));
	}

	// ----------------------------------------------------------------

	private static RestOperationFlavor findOpFlavor(RestFlavor rf, String name) {
		for (ServiceOperationFlavor of : rf.getOperationFlavors()) {
			if (of instanceof RestOperationFlavor && name.equals(of.getName())) {
				return (RestOperationFlavor) of;
			}
		}
		// fall back: match by operation name (RestOperationFlavor.name might differ from the
		// underlying ServiceOperation.name).
		for (ServiceOperationFlavor of : rf.getOperationFlavors()) {
			if (of instanceof RestOperationFlavor && of.getOperation() != null
					&& name.equals(of.getOperation().getName())) {
				return (RestOperationFlavor) of;
			}
		}
		return null;
	}

	private static String stripTrailingSlash(String s) {
		return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
	}

	private static String ensureLeadingSlash(String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		return s.startsWith("/") ? s : "/" + s;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
