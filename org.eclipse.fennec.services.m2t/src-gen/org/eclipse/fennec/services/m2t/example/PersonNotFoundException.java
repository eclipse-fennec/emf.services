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

/**
 * No person is registered under the given identifier.
 *
 * <p>Generated from the {@code PersonNotFound} exception of the
 * {@code PersonDirectory} contract, version 1.0.0. Do not edit —
 * change the contract instead.
 */
public class PersonNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	private final String personId;

	public PersonNotFoundException(String message, String personId) {
		super(message);
		this.personId = personId;
	}

	public String getPersonId() {
		return personId;
	}
}