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

import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.RestExceptionBinding;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.common.DeclaredErrors;

/**
 * Which HTTP status a failure gets, when the failure travels as a
 * {@link Diagnostic} rather than as a thrown exception.
 *
 * <p>This project answers errors with a Diagnostic in the body and not
 * with an HTTP exception (ARCHITECTURE §2.8), so an operation that fails
 * still returns a value. The status is a transport decision on top of
 * that value, and the contract already carries what it takes: an error
 * the contract declares can name its {@code code} as a property, and the
 * flavor binds that error to a status. A returned Diagnostic with the
 * same code is that error.
 *
 * <p>Without a matching declaration the answer is 400 — the caller sent
 * something this provider would not do, and nothing more precise was
 * stated.
 */
public final class RestErrors {

	/** What an undeclared error answers with. */
	public static final int UNDECLARED = 400;

	private RestErrors() {
	}

	/** Whether this result is a failure at all. */
	public static boolean isFailure(Object result) {
		return DeclaredErrors.isFailure(result);
	}

	/**
	 * The status for a failing Diagnostic under this operation's flavor.
	 *
	 * @return the declared status, or {@link #UNDECLARED} when no error
	 *         of this contract carries that code
	 */
	public static int statusFor(RestOperationFlavor operationFlavor, Diagnostic diagnostic) {
		if (operationFlavor == null || diagnostic == null) {
			return UNDECLARED;
		}
		for (RestExceptionBinding binding : operationFlavor.getExceptionBindings()) {
			OptionalInt declared = codeOf(binding.getException());
			if (declared.isPresent() && declared.getAsInt() == diagnostic.getCode()) {
				return binding.getStatus();
			}
		}
		return UNDECLARED;
	}

	/**
	 * The {@code code} an error declares, if it declares one — the rule
	 * every transport shares ({@link DeclaredErrors}).
	 */
	public static OptionalInt codeOf(ServiceException declared) {
		return DeclaredErrors.codeOf(declared);
	}

	/**
	 * The declared error a status of this operation's flavor stands for —
	 * the reading side of {@link #statusFor}, and of what a dispatcher
	 * answers for a thrown one.
	 */
	public static Optional<ServiceException> declaredFor(RestOperationFlavor operationFlavor, int status) {
		if (operationFlavor == null) {
			return Optional.empty();
		}
		for (RestExceptionBinding binding : operationFlavor.getExceptionBindings()) {
			ServiceException declared = binding.getException();
			if (binding.getStatus() == status && declared != null && !declared.eIsProxy()) {
				return Optional.of(declared);
			}
		}
		return Optional.empty();
	}
}
