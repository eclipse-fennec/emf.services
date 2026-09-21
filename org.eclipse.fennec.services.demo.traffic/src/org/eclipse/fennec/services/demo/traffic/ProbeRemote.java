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
 * The BindingProbe contract as a Java interface.
 *
 * <p>Its three arguments travel in three different places — path,
 * query and header — which the provider reads back from the same
 * flavor the consumer wrote them with. For the demo it matters for a
 * different reason: this contract is served by the generic REST
 * distribution, so the provider's half of the call is traced and the
 * trace has all three parties in it.
 */
public interface ProbeRemote {

	String echo(String id, String currency, String tenant);
}
