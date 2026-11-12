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

import java.time.Instant;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Tells the time. Exercises the one thing the other example contracts do not have: a slot whose language-neutral type is outside the standard set, so that what it is in a language is the binding's to say.
 *
 * <p>Generated from the {@code Clock} contract, version
 * 1.0.0. Do not edit — change the contract instead.
 */
@ProviderType
public interface Clock {

	/**
	 * The current instant.
	 */
	Instant now();

	/**
	 * Milliseconds from one instant to another.
	 * @param from Where to count from.
	 * @param to Where to count to; now when left out. Optional.
	 */
	long between(Instant from, Instant to);
}