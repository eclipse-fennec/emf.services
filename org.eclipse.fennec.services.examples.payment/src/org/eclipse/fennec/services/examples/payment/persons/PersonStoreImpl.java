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

import java.util.UUID;

import org.eclipse.fennec.services.examples.model.ddsrexample.Person;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

/**
 * The one hand-written class behind the PersonStore contract: it gives
 * the person an id and hands it back.
 *
 * <p>Deliberately almost nothing. What this example is about happens
 * around it: the argument arrived as protobuf bytes, the generic REST
 * distribution decoded it into a {@link Person} because the contract
 * says {@code consumes="application/x-protobuf"}, and the answer goes
 * back the same way because it says {@code produces}. None of that is
 * written here, and that is the point — the contract describes the
 * wire (#84), and since #100 it describes the encoding too.
 */
@Component(service = PersonStore.class, property = "ddsr.contract=PersonStore")
@ServiceDescription("PersonStore implementation, served by the generic REST distribution")
public class PersonStoreImpl implements PersonStore {

	@Override
	public Person store(Person person) {
		if (person == null) {
			return null;
		}
		if (person.getId() == null || person.getId().isBlank()) {
			person.setId(UUID.randomUUID().toString());
		}
		return person;
	}
}
