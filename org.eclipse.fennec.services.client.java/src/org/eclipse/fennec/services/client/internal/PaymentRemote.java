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

package org.eclipse.fennec.services.client.internal;

/**
 * Demo Java stub for the {@code Payment} ServiceInterface published
 * by the TypeScript colleague. Method names mirror the DDSR
 * operation names so the reflective proxy maps them 1:1.
 *
 * <p>Note: as of today the broker only has Payment in the catalog,
 * no implementation. {@code PaymentProxyRegistrar} stays idle until a
 * provider publishes a ServiceImplementation; once that happens (and
 * the client is restarted, or we add a tracker), the proxy registers
 * and {@code PaymentDebug} becomes satisfiable.
 *
 * <p>Hand-written for now; a code generator would emit this from the
 * Payment catalog entry.
 */
public interface PaymentRemote {

	double charge(double amount, String currency);

	double getBalance(String accountId);
}
