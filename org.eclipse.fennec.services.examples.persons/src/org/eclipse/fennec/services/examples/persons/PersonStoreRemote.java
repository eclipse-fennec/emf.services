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

import org.eclipse.fennec.services.examples.model.ddsrexample.Person;

/**
 * The consumer's own Java view of the {@code PersonStore} contract.
 *
 * <p>Declared here rather than shared with the provider: the two sides
 * agree through the published contract, not through a common jar. What
 * makes this one worth an example is the signature — a modelled
 * argument and a modelled result, which is the case where the encoding
 * stops being a detail of the transport and becomes something the
 * contract decides (#100).
 */
public interface PersonStoreRemote {

	Person store(Person person);
}
