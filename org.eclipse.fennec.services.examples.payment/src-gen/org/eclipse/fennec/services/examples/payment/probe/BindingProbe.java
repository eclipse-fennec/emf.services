/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.fennec.services.examples.payment.probe;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Answers with where each of its arguments arrived. Exists to prove that a consumer places every value where the published flavor says — and nowhere else.
 *
 * <p>Generated from the {@code BindingProbe} contract, version
 * 1.0.0. Do not edit — change the contract instead.
 */
@ProviderType
public interface BindingProbe {

	/**
	 * Returns '<id>|<currency>|<tenant>' from wherever the three values arrived.
	 * @param id Travels in the path template.
	 * @param currency Travels as a query parameter.
	 * @param tenant Travels as the X-Tenant header.
	 * @return The three values, separated by a pipe, in the order the contract declares them.
	 */
	String echo(String id, String currency, String tenant);
}