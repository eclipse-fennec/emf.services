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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.LocalServiceRegistry;
import org.eclipse.fennec.services.RegistryKind;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The lookup contract, as a service the generic distribution can serve.
 *
 * <p>The broker's own {@code BrokerLookup} takes a
 * {@link ConsumerCapability}; the REST surface spreads that over query
 * parameters, because that is what a consumer without our model can
 * send. Those are two signatures of one idea, and the contract document
 * describes the second — so this class is the object that has it.
 *
 * <p>That is the whole of what used to be a hand-written JAX-RS
 * resource. What it does beyond delegating is build the answer: the
 * references, the implementations behind them and the contracts they
 * name, copied out of the broker's state so that nothing live can be
 * reached from the answer. The copies are free-floating, which is
 * exactly what makes them travel (#88) — the transport adds them as
 * sibling roots without being told to.
 */
@Component(service = BrokerLookupRest.class, property = "ddsr.contract=BrokerLookup")
public class BrokerLookupRest {

	@Reference
	BrokerLookup broker;

	@Reference
	BrokerImplementations implementations;

	/**
	 * Every live registration of a contract that this consumer can use.
	 *
	 * @param interfaceName the contract, travelling as {@code interface}
	 * @param filter        LDAP filter over the registration's properties
	 * @param flavors       comma-separated transports the consumer speaks
	 * @param consumerId    who is asking, so the broker can pair the
	 *                      lookup with a session
	 * @param fingerprint   sd1 of the contract the consumer was built
	 *                      against
	 */
	public LocalServiceRegistry getServiceReferences(String interfaceName, String filter, String flavors,
			String consumerId, String fingerprint) {
		if (interfaceName == null || interfaceName.isBlank()) {
			throw new IllegalArgumentException("parameter 'interface' is required");
		}
		ConsumerCapability capability = capabilityOf(flavors, consumerId, fingerprint);
		List<ServiceReference> hits = broker.getServiceReferences(interfaceName, blankToNull(filter),
				capability);
		return envelopeOf(hits);
	}

	/** A provider says it is still there. */
	public Diagnostic heartbeat(String referenceId, long intervalSeconds) {
		return implementations.heartbeat(referenceId, intervalSeconds);
	}

	/**
	 * The answer: an envelope carrying the hits, with the contracts they
	 * name left free-floating so they travel as siblings.
	 *
	 * <p>Everything is copied. A reference into live broker state would
	 * be serialised as a cross-document href into the broker's own
	 * snapshot file — the W1 leak — and a consumer would hold a handle
	 * on state it has no business reaching.
	 */
	private LocalServiceRegistry envelopeOf(List<ServiceReference> hits) {
		Set<EObject> liveRoots = new LinkedHashSet<>();
		Set<EObject> liveContained = new LinkedHashSet<>();
		Set<ServiceImplementation> hitImplementations = new LinkedHashSet<>();
		Set<ServiceInterface> liveInterfaces = new LinkedHashSet<>();
		for (ServiceReference reference : hits) {
			liveRoots.add(reference);
			ServiceImplementation implementation = broker.getImplementationForReference(reference);
			if (implementation == null) {
				continue;
			}
			hitImplementations.add(implementation);
			liveContained.add(implementation.eContainer() instanceof ServiceProvider provider
					? provider
					: implementation);
			liveInterfaces.addAll(implementation.getServiceInterfaces());
		}

		EcoreUtil.Copier copier = new EcoreUtil.Copier();
		copier.copyAll(liveRoots);
		copier.copyAll(liveContained);
		copier.copyAll(liveInterfaces);
		copier.copyReferences();

		// A provider copy carries ONLY the implementations that are hits:
		// a sibling version that a DEPRECATE_AND_DRAIN hides, or that
		// simply did not match, must not leak into the answer.
		Set<EObject> hitCopies = new LinkedHashSet<>();
		for (ServiceImplementation hit : hitImplementations) {
			hitCopies.add(copier.get(hit));
		}
		for (EObject live : liveContained) {
			if (copier.get(live) instanceof ServiceProvider providerCopy) {
				providerCopy.getImplementations().removeIf(copy -> !hitCopies.contains(copy));
				for (ServiceImplementation implementation : providerCopy.getImplementations()) {
					// `replaces` points at a predecessor that is not in
					// this answer; a copier leaves such a link on the
					// ORIGINAL, which lives in the broker's snapshot.
					if (implementation.getReplaces() != null
							&& !hitCopies.contains(implementation.getReplaces())) {
						implementation.setReplaces(null);
					}
				}
			}
		}

		LocalServiceRegistry envelope = ServicesFactory.eINSTANCE.createLocalServiceRegistry();
		envelope.setName("lookup-result");
		envelope.setKind(RegistryKind.LOCAL);
		for (EObject live : liveRoots) {
			if (copier.get(live) instanceof ServiceReference referenceCopy) {
				envelope.getReferences().add(referenceCopy);
			}
		}
		for (EObject live : liveContained) {
			EObject copy = copier.get(live);
			if (copy instanceof ServiceProvider providerCopy) {
				envelope.getProviders().add(providerCopy);
			}
		}
		return envelope;
	}

	/**
	 * What the consumer said about itself, or {@code null} when it said
	 * nothing.
	 *
	 * <p>Empty supported flavors mean "no flavor constraint" — the
	 * backend reads them that way — and not "speaks nothing", which is
	 * why a capability may exist for the fingerprint or the consumer id
	 * alone.
	 */
	static ConsumerCapability capabilityOf(String flavorsCsv, String consumerId, String fingerprint) {
		boolean hasFlavors = flavorsCsv != null && !flavorsCsv.isBlank();
		boolean hasConsumerId = consumerId != null && !consumerId.isBlank();
		boolean hasFingerprint = fingerprint != null && !fingerprint.isBlank();
		if (!hasFlavors && !hasConsumerId && !hasFingerprint) {
			return null;
		}
		ConsumerCapability capability = ServicesFactory.eINSTANCE.createConsumerCapability();
		if (hasConsumerId) {
			capability.setConsumerId(consumerId);
		}
		if (hasFingerprint) {
			// Contract addressing (ACQUISITION.md §11.2): only
			// registrations whose broker-computed sd1 matches exactly.
			StringProperty requested = ServicesFactory.eINSTANCE.createStringProperty();
			requested.setName("ddsr.fingerprint");
			requested.setValue(fingerprint.trim());
			capability.getProperties().add(requested);
		}
		if (!hasFlavors) {
			return capability;
		}
		for (String token : flavorsCsv.split(",")) {
			FlavorKind kind = FlavorKind.getByName(token.trim());
			if (kind != null) {
				capability.getSupportedFlavors().add(kind);
			}
		}
		return capability;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
