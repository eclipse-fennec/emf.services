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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.DdsrProvider;
import org.eclipse.fennec.services.client.Registration;
import org.eclipse.fennec.services.fingerprint.ServiceDescriptionFingerprint;
import org.eclipse.fennec.services.fingerprint.ServiceImplementationFingerprint;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;
import org.eclipse.fennec.services.StringProperty;

final class ProviderImpl implements DdsrProvider {

	private static final Logger LOG = Logger.getLogger(ProviderImpl.class.getName());

	private final BrokerImplementations implementations;
	private final BrokerLookup lookup;

	ProviderImpl(BrokerImplementations implementations, BrokerLookup lookup) {
		this.implementations = implementations;
		this.lookup = lookup;
	}

	@Override
	public Registration publish(ServiceProvider self, ServiceImplementation implementation) {
		if (self == null || implementation == null) {
			throw new DdsrException("provider and implementation must not be null");
		}
		if (implementation.eContainer() != self) {
			throw new DdsrException("implementation must be contained in provider.implementations");
		}

		// Idempotent reconnect (ACQUISITION.md §11.1): if the broker still
		// holds an identical registration — im1 match — reuse it instead
		// of re-publishing. Consumers then see no UNREGISTERING/REGISTERED
		// churn for a provider that merely restarted.
		ServiceReference held = brokerHeldReference(self, implementation);
		if (held != null) {
			LOG.info(() -> "publish skipped for '" + implementation.getImplementationId()
					+ "': broker already holds an identical registration (im1 match), reusing "
					+ held.getId());
			return new RegistrationImpl(this, self, implementation, held, synthAlreadyPublished());
		}

		Diagnostic publishDiagnostic = implementations.publishImplementation(self, implementation);
		if (publishDiagnostic.getSeverity() == DiagnosticSeverity.ERROR
				|| publishDiagnostic.getSeverity() == DiagnosticSeverity.CANCEL) {
			throw new DdsrException(publishDiagnostic);
		}

		// The publish response is a Diagnostic without the assigned
		// reference id. Discover it by issuing a follow-up lookup keyed
		// on the first service interface name and matching the provider
		// name back to us.
		ServiceReference ref = discoverReferenceAfterPublish(self, implementation);
		return new RegistrationImpl(this, self, implementation, ref, publishDiagnostic);
	}

	/**
	 * The three-valued reconnect check: looks for a reference the broker
	 * already holds for exactly this implementation, identified purely by
	 * content — an im1 match implies same implementationId, same
	 * endpoints, same contract components (im1 composes over the sd1
	 * values), so it can only be ours. Returns that reference, or
	 * {@code null} when the broker holds nothing (publish normally) or
	 * holds a drifted entry (re-publish; the broker retires the old one).
	 * The drift direction is logged: all sd1 equal → endpoint/property
	 * drift (INFO, row 2 of the check); an sd1 different → contract
	 * drift (WARNING, row 3 — the catalog contract moved under us, a
	 * policy question, but publish stays the safe default because the
	 * broker validates against the live catalog anyway).
	 */
	private ServiceReference brokerHeldReference(ServiceProvider self, ServiceImplementation impl) {
		if (impl.getServiceInterfaces().isEmpty() || self.getName() == null) {
			return null;
		}
		String localIm1 = ServiceImplementationFingerprint.fingerprint(impl);
		List<ServiceReference> candidates;
		try {
			candidates = lookup.getServiceReferences(
					impl.getServiceInterfaces().get(0).getName(), null, buildCapability(impl));
		} catch (Exception unreachableBroker) {
			return null;
		}
		ServiceReference drifted = null;
		for (ServiceReference candidate : candidates) {
			if (candidate.getProvider() == null
					|| !self.getName().equals(candidate.getProvider().getName())) {
				continue;
			}
			if (localIm1.equals(stringProperty(candidate, "ddsr.impl.fingerprint"))) {
				return candidate;
			}
			if (drifted == null) {
				drifted = candidate;
			}
		}
		if (drifted != null) {
			ServiceReference d = drifted;
			if (sd1Match(d, impl)) {
				LOG.info(() -> "re-publishing '" + impl.getImplementationId()
						+ "': broker holds " + d.getId()
						+ " with same contract (sd1) but drifted endpoint/properties (im1)");
			} else {
				LOG.warning(() -> "re-publishing '" + impl.getImplementationId()
						+ "': broker holds " + d.getId()
						+ " with a DIFFERENT contract (sd1 drift) — catalog and local model disagree");
			}
		}
		return null;
	}

	/** All local sd1 values equal the broker's decoration on the reference. */
	private static boolean sd1Match(ServiceReference reference, ServiceImplementation impl) {
		List<ServiceInterface> interfaces = impl.getServiceInterfaces();
		for (ServiceInterface si : interfaces) {
			String remote = stringProperty(reference, interfaces.size() == 1
					? "ddsr.fingerprint"
					: "ddsr.fingerprint." + si.getName());
			if (!Objects.equals(ServiceDescriptionFingerprint.fingerprint(si), remote)) {
				return false;
			}
		}
		return true;
	}

	private static String stringProperty(ServiceReference reference, String name) {
		for (Property property : reference.getProperties()) {
			if (property instanceof StringProperty sp && name.equals(sp.getName())) {
				return sp.getValue();
			}
		}
		return null;
	}

	private ServiceReference discoverReferenceAfterPublish(ServiceProvider self, ServiceImplementation impl) {
		if (impl.getServiceInterfaces().isEmpty()) {
			return null;
		}
		String firstInterface = impl.getServiceInterfaces().get(0).getName();
		ConsumerCapability cap = buildCapability(impl);
		try {
			List<ServiceReference> refs = lookup.getServiceReferences(firstInterface, null, cap);
			for (ServiceReference candidate : refs) {
				if (candidate.getProvider() != null && self.getName() != null
						&& self.getName().equals(candidate.getProvider().getName())) {
					return candidate;
				}
			}
			return refs.isEmpty() ? null : refs.get(0);
		} catch (Exception ignore) {
			return null;
		}
	}

	private static ConsumerCapability buildCapability(ServiceImplementation impl) {
		List<FlavorKind> kinds = new ArrayList<>();
		impl.getFlavors().forEach(f -> {
			if (f.getKind() != null && !kinds.contains(f.getKind())) {
				kinds.add(f.getKind());
			}
		});
		List<FlavorKind> effective = kinds.isEmpty() ? Arrays.asList(FlavorKind.values()) : kinds;
		ConsumerCapability cap = ServicesFactory.eINSTANCE.createConsumerCapability();
		cap.getSupportedFlavors().addAll(effective);
		return cap;
	}

	/** Called by RegistrationImpl.withdraw(). */
	Diagnostic withdrawInternal(ServiceProvider provider, ServiceImplementation implementation) {
		return implementations.withdrawImplementation(provider, implementation);
	}

	/** Synthetic OK Diagnostic for the skipped re-publish on reconnect. */
	private static Diagnostic synthAlreadyPublished() {
		Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
		d.setSeverity(DiagnosticSeverity.OK);
		d.setSource("org.eclipse.fennec.services.client");
		d.setMessage("already published — broker holds an identical registration (im1 match)");
		return d;
	}

	/** Synthetic OK Diagnostic used for idempotent withdraw. */
	Diagnostic synthOk() {
		Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
		d.setSeverity(DiagnosticSeverity.OK);
		d.setSource("org.eclipse.fennec.services.client");
		d.setMessage("already withdrawn");
		return d;
	}
}
