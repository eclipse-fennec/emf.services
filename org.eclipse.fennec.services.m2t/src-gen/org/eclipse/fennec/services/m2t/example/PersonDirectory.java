/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.fennec.services.m2t.example;

import java.util.List;
import org.eclipse.fennec.services.examples.model.ddsrexample.Person;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Reads persons from the example directory. Exercises the parts of a contract the Payment example does not have: EClass-typed slots, a list result and a declared exception.
 *
 * <p>Generated from the {@code PersonDirectory} contract, version
 * 1.0.0. Do not edit — change the contract instead.
 */
@ProviderType
public interface PersonDirectory {

	/**
	 * One person by identifier.
	 * @param id Identifier of the person.
	 * @return The person with that identifier.
	 * @throws PersonNotFoundException No person is registered under the given identifier.
	 */
	Person get(String id) throws PersonNotFoundException;

	/**
	 * A page of the directory, in directory order.
	 * @param offset Index of the first person to return. Optional, defaults to {@code 0}.
	 * @param limit Maximum number of persons to return. Optional, defaults to {@code 50}.
	 * @return The requested page; empty when the offset is past the end.
	 */
	List<Person> list(int offset, int limit);
}