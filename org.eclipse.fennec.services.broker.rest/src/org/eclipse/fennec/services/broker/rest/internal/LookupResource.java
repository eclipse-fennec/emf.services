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

package org.eclipse.fennec.services.broker.rest.internal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.RegistryKind;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.xmi.codec.XmiBundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.annotations.RequireJakartarsWhiteboard;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Consumer lookup endpoint with bundled transport details:
 * <pre>
 * GET /references?interface=Payment&amp;filter=...&amp;flavors=REST,MQTT&amp;consumerId=...
 * </pre>
 *
 * <p>The response is a multi-root XMI document containing:
 * <ul>
 * <li>One {@code <services:LocalServiceRegistry name="lookup-result">}
 *     envelope holding the matching {@code references} and the owning
 *     {@code providers} (with their {@code ServiceImplementation}s and
 *     {@code RestFlavor} / {@code MqttFlavor} children, by
 *     containment).</li>
 * <li>The referenced {@code ServiceInterface}s as additional top-level
 *     objects, so cross-references from implementations resolve inside
 *     the document.</li>
 * </ul>
 * The client gets everything needed to invoke the service in a single
 * round-trip: provider identity, transport flavor with base path, and
 * per-operation HTTP/MQTT bindings.
 */
@RequireHttpWhiteboard
@RequireJakartarsWhiteboard
@JakartarsResource
@JakartarsName("ddsr-lookup")
@Component(service = LookupResource.class, scope = ServiceScope.PROTOTYPE)
@Path("/references")
public class LookupResource {

	@Reference
	private BrokerLookup broker;

	@GET
	@Produces(MediaType.APPLICATION_XML)
	public Response lookup(
			@QueryParam("interface") String interfaceName,
			@QueryParam("filter") String filter,
			@QueryParam("flavors") String flavorsCsv,
			@QueryParam("consumerId") String consumerId,
			@QueryParam("fingerprint") String fingerprint) {
		if (interfaceName == null || interfaceName.isBlank()) {
			return Response.status(400).entity("query parameter 'interface' is required").build();
		}
		ConsumerCapability cap = parseCapability(flavorsCsv, consumerId, fingerprint);
		List<ServiceReference> hits = broker.getServiceReferences(interfaceName, emptyToNull(filter), cap);

		// Gather the live objects we want in the response. LinkedHashSet
		// keeps insertion order while deduplicating shared interfaces.
		Set<EObject> liveRoots = new LinkedHashSet<>();
		Set<EObject> liveContained = new LinkedHashSet<>();
		Set<ServiceInterface> liveInterfaces = new LinkedHashSet<>();
		for (ServiceReference ref : hits) {
			liveRoots.add(ref);
			ServiceImplementation impl = broker.getImplementationForReference(ref);
			if (impl != null) {
				liveContained.add(impl);
				if (impl.eContainer() instanceof ServiceProvider) {
					liveContained.add(impl.eContainer());
				}
				liveInterfaces.addAll(impl.getServiceInterfaces());
			}
		}

		// Use a Copier so that cross-references between References,
		// Implementations and ServiceInterfaces are rewired to point at
		// the copies (and not at the live broker state).
		EcoreUtil.Copier copier = new EcoreUtil.Copier();
		copier.copyAll(liveRoots);
		copier.copyAll(liveContained);
		copier.copyAll(liveInterfaces);
		copier.copyReferences();

		// Build the envelope: contained references + contained providers.
		LocalServiceRegistry envelope = ServicesFactory.eINSTANCE.createLocalServiceRegistry();
		envelope.setName("lookup-result");
		envelope.setKind(RegistryKind.LOCAL);
		for (ServiceReference ref : hits) {
			envelope.getReferences().add((ServiceReference) copier.get(ref));
		}
		for (EObject live : liveContained) {
			if (live instanceof ServiceProvider) {
				EObject providerCopy = copier.get(live);
				if (providerCopy instanceof ServiceProvider && !envelope.getProviders().contains(providerCopy)) {
					envelope.getProviders().add((ServiceProvider) providerCopy);
				}
			}
		}

		// Multi-root document: envelope + interface copies as siblings.
		List<EObject> roots = new ArrayList<>();
		roots.add(envelope);
		for (ServiceInterface si : liveInterfaces) {
			EObject copy = copier.get(si);
			if (copy != null) {
				roots.add(copy);
			}
		}

		return Response.ok(new XmiBundle(roots)).type(MediaType.APPLICATION_XML).build();
	}

	private static ConsumerCapability parseCapability(String flavorsCsv, String consumerId, String fingerprint) {
		if (flavorsCsv == null || flavorsCsv.isBlank()) {
			return null;
		}
		ConsumerCapability cap = ServicesFactory.eINSTANCE.createConsumerCapability();
		if (consumerId != null && !consumerId.isBlank()) {
			cap.setConsumerId(consumerId);
		}
		if (fingerprint != null && !fingerprint.isBlank()) {
			// Contract addressing (ACQUISITION.md §11.2): only references
			// whose broker-computed sd1 matches exactly are returned.
			StringProperty requested = ServicesFactory.eINSTANCE.createStringProperty();
			requested.setName("ddsr.fingerprint");
			requested.setValue(fingerprint.trim());
			cap.getProperties().add(requested);
		}
		for (String s : flavorsCsv.split(",")) {
			String t = s.trim();
			if (t.isEmpty()) {
				continue;
			}
			FlavorKind kind = FlavorKind.getByName(t);
			if (kind != null) {
				cap.getSupportedFlavors().add(kind);
			}
		}
		return cap;
	}

	private static String emptyToNull(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}
}
