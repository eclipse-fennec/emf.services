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

import java.util.Map;

/**
 * No person is registered under the given identifier.
 *
 * <p>Generated from the {@code PersonNotFound} exception of the
 * {@code PersonDirectory} contract, version 1.0.0. Do not edit —
 * change the contract instead.
 */
public class PersonNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * The constant metadata the contract declares for this error. Same for
	 * every occurrence — a value that differed per occurrence could not be
	 * part of the contract, and the sd1 fingerprint hashes these.
	 */
	private static final Map<String, Object> PROPERTIES = Map.of("code", 404, "retryable", false);

	public PersonNotFoundException(String message) {
		super(message);
	}

	public PersonNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public Map<String, Object> getProperties() {
		return PROPERTIES;
	}
}