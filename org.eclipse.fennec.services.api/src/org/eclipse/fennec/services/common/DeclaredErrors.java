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

import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.Property;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceOperation;

/**
 * How an error a contract declares is recognised — the one rule both
 * ends of every transport share.
 *
 * <p>A contract names its errors symbolically and may give each a
 * {@code code} as a property. That code is the identity an error has
 * on the wire: a failing {@link Diagnostic} with the same code <i>is</i>
 * that error. A transport with a vocabulary of its own translates on
 * top of it — REST binds the error to a status
 * ({@code RestExceptionBinding}) — and one without, such as a messaging
 * broker, carries the Diagnostic and nothing else is needed.
 *
 * <p>A provider raises a declared error in one of two ways, and both
 * come out the same: it returns a failing Diagnostic with the code, or
 * it throws an exception whose class is named after the error's type.
 *
 * <p>An error declared without a code cannot be recognised in a
 * Diagnostic. That is a statement about the contract, not a failure
 * here.
 */
public final class DeclaredErrors {

	/** The name of the property an error states its code in. */
	public static final String CODE = "code";

	private DeclaredErrors() {
	}

	/** Whether this result is a failure at all. */
	public static boolean isFailure(Object result) {
		return result instanceof Diagnostic diagnostic
				&& diagnostic.getSeverity() == DiagnosticSeverity.ERROR;
	}

	/** The {@code code} an error declares, if it declares one. */
	public static OptionalInt codeOf(ServiceException declared) {
		if (declared == null || declared.eIsProxy()) {
			return OptionalInt.empty();
		}
		for (Property property : declared.getProperties()) {
			if (CODE.equals(property.getName()) && property instanceof IntProperty code) {
				return OptionalInt.of(code.getValue());
			}
		}
		return OptionalInt.empty();
	}

	/**
	 * The error of this operation a Diagnostic of that code is.
	 *
	 * <p>Zero is no code: it is what a Diagnostic carries when nobody
	 * gave it one, so it names nothing.
	 */
	public static Optional<ServiceException> byCode(ServiceOperation operation, int code) {
		if (operation == null || code == 0) {
			return Optional.empty();
		}
		for (ServiceException declared : operation.getExceptions()) {
			OptionalInt own = codeOf(declared);
			if (own.isPresent() && own.getAsInt() == code) {
				return Optional.of(declared);
			}
		}
		return Optional.empty();
	}

	/** The error of this operation a thrown failure is, if it is one. */
	public static Optional<ServiceException> raisedBy(ServiceOperation operation, Throwable failure) {
		if (operation == null || failure == null) {
			return Optional.empty();
		}
		for (ServiceException declared : operation.getExceptions()) {
			if (raises(failure, declared)) {
				return Optional.of(declared);
			}
		}
		return Optional.empty();
	}

	/**
	 * Whether a failure is the declared error. The contract names its
	 * exceptions symbolically, so the match is by the simple name of the
	 * Java class — the language binding that would resolve it properly
	 * is build-time knowledge a runtime deliberately does not have.
	 */
	public static boolean raises(Throwable failure, ServiceException declared) {
		if (failure == null || declared == null || declared.eIsProxy() || declared.getType() == null) {
			return false;
		}
		for (Class<?> type = failure.getClass(); type != null; type = type.getSuperclass()) {
			if (named(type, declared)) {
				return true;
			}
		}
		return false;
	}

	/** Whether a Java class is the one a declared error is called in Java. */
	public static boolean named(Class<?> type, ServiceException declared) {
		if (type == null || declared == null || declared.getType() == null) {
			return false;
		}
		String symbolic = declared.getType();
		String simple = symbolic.contains(".") ? symbolic.substring(symbolic.lastIndexOf('.') + 1) : symbolic;
		return type.getSimpleName().equals(simple) || type.getSimpleName().equals(simple + "Exception");
	}
}
