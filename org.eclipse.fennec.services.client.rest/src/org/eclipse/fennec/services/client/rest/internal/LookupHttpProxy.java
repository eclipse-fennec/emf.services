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

package org.eclipse.fennec.services.client.rest.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

/**
 * REST proxy for {@link BrokerLookup}. The endpoint returns a multi-
 * root XMI document (envelope + sibling interfaces) which we
 * deserialise as an {@link XmiBundle} and project to the role
 * interface's slice-shaped API.
 *
 * <p>{@link #getImplementationForReference(ServiceReference)} walks
 * the containment chain of the reference (which is still attached to
 * the deserialised envelope) — no second HTTP call required.
 */
@Component(
		service = BrokerLookup.class,
		property = "ddsr.broker.transport=rest")
@ServiceDescription("DDSR BrokerLookup REST proxy")
public final class LookupHttpProxy implements BrokerLookup {

	@Reference
	private RestTransport tx;

	@Override
	public ServiceReference getServiceReference(String interfaceName) {
		List<ServiceReference> refs = getServiceReferences(interfaceName, null, null);
		return refs.isEmpty() ? null : refs.get(0);
	}

	@Override
	public List<ServiceReference> getServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		return lookup(interfaceName, filter, capability);
	}

	@Override
	public List<ServiceReference> getAllServiceReferences(String interfaceName, String filter,
			ConsumerCapability capability) {
		// REST endpoint does not differentiate visibility-bypassing
		// lookups from regular ones; clients with admin needs should
		// talk to the broker in-process.
		return lookup(interfaceName, filter, capability);
	}

	@Override
	public ServiceImplementation getImplementationForReference(ServiceReference reference) {
		if (reference == null || reference.getProvider() == null) {
			return null;
		}
		// The envelope returned by /references contains the provider tree
		// with its implementations as containment children. The reference
		// model doesn't carry a direct impl pointer, but the lookup
		// contract guarantees one impl per matching provider in the
		// envelope — so the first impl is the one this ref maps to.
		ServiceProvider provider = reference.getProvider();
		return provider.getImplementations().isEmpty() ? null : provider.getImplementations().get(0);
	}

	// ------------------------------------------------------------

	private List<ServiceReference> lookup(String interfaceName, String filter, ConsumerCapability capability) {
		WebTarget t = tx.target().path("references").queryParam("interface", interfaceName);
		if (filter != null && !filter.isBlank()) {
			t = t.queryParam("filter", filter);
		}
		if (capability != null && !capability.getSupportedFlavors().isEmpty()) {
			t = t.queryParam("flavors", flavorCsv(capability.getSupportedFlavors()));
		}
		if (capability != null && capability.getConsumerId() != null && !capability.getConsumerId().isBlank()) {
			t = t.queryParam("consumerId", capability.getConsumerId());
		}
		XmiBundle bundle = t.request(MediaType.APPLICATION_XML).get(XmiBundle.class);
		LocalServiceRegistry envelope = null;
		for (EObject root : bundle.roots()) {
			if (root instanceof LocalServiceRegistry) {
				envelope = (LocalServiceRegistry) root;
				break;
			}
		}
		if (envelope == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(envelope.getReferences());
	}

	private static String flavorCsv(List<FlavorKind> kinds) {
		StringBuilder sb = new StringBuilder();
		for (FlavorKind k : kinds) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(k.getName());
		}
		return sb.toString();
	}
}
