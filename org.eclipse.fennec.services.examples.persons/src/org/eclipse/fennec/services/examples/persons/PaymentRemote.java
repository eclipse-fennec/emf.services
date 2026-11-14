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

package org.eclipse.fennec.services.examples.persons;

/**
 * The {@code Payment} contract as a Java interface, for the example
 * consumer. Method names mirror the operation names, so the reflective
 * proxy maps them 1:1. Hand-written; the generator would emit the same
 * from the catalog entry.
 */
public interface PaymentRemote {

	double charge(double amount, String currency);

	double getBalance(String accountId);
}
