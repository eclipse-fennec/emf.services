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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.ServiceLocator;
import org.eclipse.fennec.services.client.ServiceProxyFactory;
import org.eclipse.fennec.services.examples.model.ddsrexample.DDSRExampleFactory;
import org.eclipse.fennec.services.examples.model.ddsrexample.Person;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Calls {@code PersonStore}, whose wire encoding is protobuf (#100).
 *
 * <p>The proof this exists for: a modelled argument leaves here as
 * protobuf bytes and comes back as a {@link Person}, and nothing in
 * this class says so. The contract declares {@code consumes} and
 * {@code produces}, the client invoker writes the body in what
 * {@code consumes} names, and the provider's generic REST distribution
 * decodes it. Change one word in the contract document and the same
 * call travels as XMI.
 *
 * <p><b>Java on both ends, and that is a limit rather than a
 * preference.</b> The TypeScript track has no protobuf-to-EMF binding,
 * so a TS consumer offered this contract would be offered an encoding
 * it cannot read. Nothing in the registry stops that today — which is
 * why the protobuf factory sits in the invocation runs and
 * deliberately not in the broker's, and why this runs as its own
 * Java-to-Java scenario rather than as a cross-language one.
 */
@Component(immediate = true)
public final class PersonStoreProtobufDemo {

	private static final Logger LOG = Logger.getLogger(PersonStoreProtobufDemo.class.getName());

	@Reference
	private DdsrClient client;

	// No target any more: the proxy factory is transport-agnostic since
	// #98 and there is one of it, picking an invoker by the flavor the
	// service announces.
	@Reference
	private ServiceProxyFactory proxyFactory;

	@Activate
	void activate() {
		try {
			List<ServiceLocator> locators = client.consumer().find("PersonStore", null);
			if (locators.isEmpty()) {
				LOG.info("[DDSR-Protobuf] PersonStore has no providers — nothing to call");
				return;
			}
			PersonStoreRemote store = proxyFactory.newProxy(PersonStoreRemote.class, locators.get(0));

			Person person = DDSRExampleFactory.eINSTANCE.createPerson();
			person.setFirstName("Ada");
			person.setLastName("Lovelace");

			Person stored = store.store(person);
			if (stored == null) {
				LOG.warning("[DDSR-Protobuf] PersonStore answered with nothing");
				return;
			}
			LOG.info("[DDSR-Protobuf] stored " + stored.getFirstName() + " " + stored.getLastName()
					+ " and got id=" + stored.getId() + " back — argument and result travelled as protobuf");
		} catch (Throwable callFailed) {
			LOG.log(Level.WARNING, "[DDSR-Protobuf] PersonStore call FAILED", callFailed);
		}
	}
}
