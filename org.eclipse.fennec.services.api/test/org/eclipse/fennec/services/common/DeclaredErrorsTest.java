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

package org.eclipse.fennec.services.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.junit.jupiter.api.Test;

/**
 * How a declared error is recognised, on every transport: by the code
 * the contract gives it, and — where a provider throws — by the name of
 * the class (#185).
 */
class DeclaredErrorsTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	/** What a provider throws for 'PersonNotFound'. */
	static class PersonNotFoundException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	/** A provider's own refinement of it. */
	static final class PersonRetiredException extends PersonNotFoundException {
		private static final long serialVersionUID = 1L;
	}

	private static ServiceException error(String type, Integer code) {
		ServiceException declared = F.createServiceException();
		declared.setName(type.substring(type.lastIndexOf('.') + 1));
		declared.setType(type);
		if (code != null) {
			IntProperty property = F.createIntProperty();
			property.setName("code");
			property.setValue(code);
			declared.getProperties().add(property);
		}
		return declared;
	}

	private static ServiceOperation operation(ServiceException... raises) {
		ServiceOperation operation = F.createServiceOperation();
		operation.setName("get");
		for (ServiceException declared : raises) {
			operation.getExceptions().add(declared);
		}
		return operation;
	}

	@Test
	void anErrorIsTheCodeItsContractGivesIt() {
		ServiceException notFound = error("example.PersonNotFound", 404);
		ServiceOperation get = operation(error("example.Conflict", 409), notFound);

		assertThat(DeclaredErrors.codeOf(notFound)).hasValue(404);
		assertThat(DeclaredErrors.byCode(get, 404)).containsSame(notFound);
		assertThat(DeclaredErrors.byCode(get, 500)).isEmpty();
	}

	@Test
	void anErrorWithoutACodeCannotBeRecognisedInADiagnostic() {
		ServiceException nameless = error("example.Mystery", null);

		assertThat(DeclaredErrors.codeOf(nameless)).isEmpty();
		assertThat(DeclaredErrors.byCode(operation(nameless), 0))
				.as("zero is what a Diagnostic carries when nobody gave it a code")
				.isEmpty();
	}

	@Test
	void zeroNamesNothingEvenWhereAnErrorDeclaresIt() {
		assertThat(DeclaredErrors.byCode(operation(error("example.Zero", 0)), 0)).isEmpty();
	}

	@Test
	void aThrownErrorIsRecognisedByTheNameOfItsClass() {
		ServiceException notFound = error("example.PersonNotFound", 404);
		ServiceOperation get = operation(notFound);

		assertThat(DeclaredErrors.raisedBy(get, new PersonNotFoundException())).containsSame(notFound);
		assertThat(DeclaredErrors.raisedBy(get, new PersonRetiredException()))
				.as("a subclass is still the declared error")
				.containsSame(notFound);
		assertThat(DeclaredErrors.raisedBy(get, new IllegalStateException("no"))).isEmpty();
	}

	@Test
	void onlyAFailingDiagnosticIsAFailure() {
		Diagnostic failing = F.createDiagnostic();
		failing.setSeverity(DiagnosticSeverity.ERROR);
		Diagnostic telling = F.createDiagnostic();
		telling.setSeverity(DiagnosticSeverity.OK);

		assertThat(DeclaredErrors.isFailure(failing)).isTrue();
		assertThat(DeclaredErrors.isFailure(telling)).isFalse();
		assertThat(DeclaredErrors.isFailure("a value")).isFalse();
	}
}
