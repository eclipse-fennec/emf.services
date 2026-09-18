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

package org.eclipse.fennec.services.flavor.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.RestExceptionBinding;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.Test;

/**
 * An error that travels as a value still has to get a status.
 *
 * <p>The contract names the errors it can produce and gives each one the
 * {@code code} it reports; the flavor binds that error to a status. A
 * returned Diagnostic carrying the same code is that error — which is
 * what lets a generic transport answer 404 or 409 without knowing a
 * single thing about catalogs (#76).
 */
class RestErrorsTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private static RestOperationFlavor flavorDeclaring(int code, int status) {
		ServiceException declared = F.createServiceException();
		declared.setName("CatalogEntryNotFound");
		declared.setType("CatalogEntryNotFound");
		IntProperty property = F.createIntProperty();
		property.setName("code");
		property.setValue(code);
		declared.getProperties().add(property);

		RestExceptionBinding binding = F.createRestExceptionBinding();
		binding.setException(declared);
		binding.setStatus(status);

		RestOperationFlavor operationFlavor = F.createRestOperationFlavor();
		operationFlavor.getExceptionBindings().add(binding);
		return operationFlavor;
	}

	private static Diagnostic diagnostic(DiagnosticSeverity severity, int code) {
		Diagnostic diagnostic = F.createDiagnostic();
		diagnostic.setSeverity(severity);
		diagnostic.setCode(code);
		return diagnostic;
	}

	@Test
	void aFailingDiagnosticIsTheErrorWhoseCodeItCarries() {
		RestOperationFlavor flavor = flavorDeclaring(201, 404);

		assertThat(RestErrors.statusFor(flavor, diagnostic(DiagnosticSeverity.ERROR, 201))).isEqualTo(404);
	}

	@Test
	void anUndeclaredCodeIsA400() {
		RestOperationFlavor flavor = flavorDeclaring(201, 404);

		assertThat(RestErrors.statusFor(flavor, diagnostic(DiagnosticSeverity.ERROR, 999)))
				.isEqualTo(RestErrors.UNDECLARED);
	}

	@Test
	void onlyAnErrorIsAFailure() {
		assertThat(RestErrors.isFailure(diagnostic(DiagnosticSeverity.ERROR, 201))).isTrue();
		assertThat(RestErrors.isFailure(diagnostic(DiagnosticSeverity.WARNING, 201))).isFalse();
		assertThat(RestErrors.isFailure(diagnostic(DiagnosticSeverity.OK, 0))).isFalse();
		assertThat(RestErrors.isFailure("a plain result")).isFalse();
	}

	@Test
	void anErrorWithoutACodeCannotBeRecognisedInADiagnostic() {
		RestOperationFlavor flavor = F.createRestOperationFlavor();
		ServiceException declared = F.createServiceException();
		declared.setName("Something");
		declared.setType("Something");
		RestExceptionBinding binding = F.createRestExceptionBinding();
		binding.setException(declared);
		binding.setStatus(418);
		flavor.getExceptionBindings().add(binding);

		assertThat(RestErrors.statusFor(flavor, diagnostic(DiagnosticSeverity.ERROR, 201)))
				.as("a contract that does not say which code its error reports has not said enough")
				.isEqualTo(RestErrors.UNDECLARED);
	}
}
