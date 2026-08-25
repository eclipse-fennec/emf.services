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

import org.eclipse.fennec.services.client.Registration;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServiceReference;

final class RegistrationImpl implements Registration {

	private final ProviderImpl owner;
	private final ServiceProvider provider;
	private final ServiceImplementation implementation;
	private final ServiceReference reference;
	private final Diagnostic publishDiagnostic;

	private boolean withdrawn;

	RegistrationImpl(ProviderImpl owner, ServiceProvider provider, ServiceImplementation implementation,
			ServiceReference reference, Diagnostic publishDiagnostic) {
		this.owner = owner;
		this.provider = provider;
		this.implementation = implementation;
		this.reference = reference;
		this.publishDiagnostic = publishDiagnostic;
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
	public Diagnostic diagnostic() {
		return publishDiagnostic;
	}

	@Override
	public synchronized Diagnostic withdraw() {
		if (withdrawn) {
			// idempotent — return a synthetic OK.
			return owner.synthOk();
		}
		Diagnostic d = owner.withdrawInternal(provider, implementation);
		if (d.getSeverity() != DiagnosticSeverity.ERROR && d.getSeverity() != DiagnosticSeverity.CANCEL) {
			withdrawn = true;
		}
		return d;
	}
}
