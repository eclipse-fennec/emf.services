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

import org.osgi.annotation.versioning.ProviderType;

/**
 * Payment processing service — charges accounts and reports balances.
 *
 * <p>Generated from the {@code Payment} contract, version
 * 1.0.0. Do not edit — change the contract instead.
 */
@ProviderType
public interface Payment {

	/**
	 * Charge an amount. Returns the remaining balance.
	 * @param amount Amount to charge.
	 * @param currency ISO 4217 currency code. Optional, defaults to {@code EUR}.
	 */
	double charge(double amount, String currency);

	/**
	 * Get current account balance.
	 * @param accountId The account identifier.
	 */
	double getBalance(String accountId);
}