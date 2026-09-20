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

package org.eclipse.fennec.services.examples.payment.persons;

import org.eclipse.fennec.services.examples.model.ddsrexample.Person;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Takes a {@link Person} and gives it back with an id.
 *
 * <p>Exists to prove that a modelled argument and a modelled result
 * travel in the encoding the contract declares — here protobuf, not
 * the XMI the transport used to assume (#100). The contract is
 * {@code model/person-store.xmi}; the endpoint is not written anywhere,
 * the generic REST distribution serves it from that document (#84).
 *
 * <p><b>Hand-written, unlike its sibling {@code BindingProbe}.</b> The
 * m2t interface template maps the primitive type names of the model to
 * Java, and an {@code eType} that is an {@code EClass} has no such
 * mapping yet — it generates an empty type and an unusable method. The
 * template gap belongs to #25 and is not this example's to fix, so the
 * one contract in the workspace with a modelled body declares its Java
 * form by hand and says why.
 */
@ProviderType
public interface PersonStore {

	/**
	 * Stores the person and returns it with an id assigned.
	 *
	 * @param person the person to store; travels as the request body,
	 *        encoded as whatever {@code consumes} declares
	 * @return the same person, with an id
	 */
	Person store(Person person);
}
