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

package org.eclipse.fennec.services.demo.traffic;

/**
 * The Payment contract as a Java interface, for the proxy factory.
 *
 * <p>Method names and parameter order are the contract's; the proxy
 * reads everything else — where an argument travels, what the answer
 * is decoded as — from the model the provider published. So this is
 * not a stub, it is the shape a Java caller wants to write.
 */
public interface PaymentRemote {

	/** The remaining balance of an account. */
	double getBalance(String accountId);

	/** Charges an amount and answers with what is left. */
	double charge(double amount, String currency);
}
