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

import org.eclipse.fennec.services.broker.core.BrokerImplementations;
import org.eclipse.fennec.services.broker.core.BrokerLookup;
import org.eclipse.fennec.services.client.DdsrException;
import org.eclipse.fennec.services.client.DdsrProvider;
import org.eclipse.fennec.services.client.Registration;
import org.eclipse.fennec.services.ConsumerCapability;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.FlavorKind;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;

final class ProviderImpl implements DdsrProvider {

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

	/** Synthetic OK Diagnostic used for idempotent withdraw. */
	Diagnostic synthOk() {
		Diagnostic d = ServicesFactory.eINSTANCE.createDiagnostic();
		d.setSeverity(DiagnosticSeverity.OK);
		d.setSource("org.eclipse.fennec.services.client");
		d.setMessage("already withdrawn");
		return d;
	}
}
