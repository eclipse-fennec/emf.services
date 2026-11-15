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
package org.eclipse.fennec.services.invocation;

import java.util.Map;

/**
 * A service that answers a contract's operations by name rather than by a
 * method of each (#301) — for a contract made at runtime, which has no Java
 * interface to implement, such as one a bridge mapped from an OpenAPI
 * description.
 *
 * <p>The generic distributions call a service that implements this through
 * {@link #invoke} instead of looking for a method; every other service as
 * before. What it returns counts as a method's return value — a value, a
 * promise holder, a stream —, and what it throws as a method's exception: a
 * {@code DeclaredFailure} or an exception named like a declared error travels
 * as that error.
 */
@FunctionalInterface
public interface OperationHandler {

	/**
	 * @param operation the operation's name in the contract
	 * @param arguments the arguments by parameter name, in the contract's order; a missing optional
	 *        argument is absent or {@code null}
	 * @return what the operation answers
	 * @throws Exception what the operation fails with
	 */
	Object invoke(String operation, Map<String, Object> arguments) throws Exception;
}
