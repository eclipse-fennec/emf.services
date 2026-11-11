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

package org.eclipse.fennec.services.client.announce;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Announces one contract a distribution serves, for as long as it is to
 * be announced.
 *
 * <p>Serving and announcing are two jobs, and this is the second — one
 * component for every transport. It needs three things and has no life
 * without any of them, so all three are mandatory, static references:
 * <ul>
 * <li>the {@linkplain ServedContract served contract}, with a target
 * that only matches while its {@code publish} is {@code true} — the
 * distribution raises that once the contract is reachable;</li>
 * <li>the client, to publish through;</li>
 * <li>the broker's catalog over REST, to enter the contract in.</li>
 * </ul>
 * So it announces in {@code activate} and withdraws in
 * {@code deactivate}, and the order FR-P3 asks for is the order
 * Declarative Services brings a component up and takes it down in:
 * served first, then told; withdrawn first, then gone. What it is
 * waiting for is readable in the component runtime instead of being a
 * condition checked on every binding.
 *
 * <p>Nobody writes this component's configuration by hand: the
 * distribution derives it from its own when that says
 * {@code publish}, and deletes it again
 * ({@link ServedContract#announcementFor}).
 */
@Component(
		service = {},
		configurationPid = ServedContract.ANNOUNCEMENT_PID,
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ContractAnnouncement {

	private static final Logger LOG = Logger.getLogger(ContractAnnouncement.class.getName());

	/** What is announced — selected by {@code served.target} from the configuration. */
	@Reference(name = "served")
	ServedContract served;

	@Reference
	DdsrClient client;

	/**
	 * The REST proxy explicitly: a deployment that carries broker.core
	 * for the interface alone also has that bundle's own in-process
	 * catalog to offer, and announcing into an empty local broker looks
	 * like success and reaches nobody.
	 */
	@Reference(target = "(ddsr.broker.transport=rest)")
	BrokerCatalog catalog;

	private Registration registration;

	/**
	 * Announce the implementation the distribution serves.
	 *
	 * <p>With this, a provider is a configuration and a service: nothing
	 * states the contract twice, because what is announced is the same
	 * document the distribution serves from. Only the deployment facts
	 * are filled in — where this instance is reachable is not contract.
	 *
	 * <p>The contract moves into a resource named by its catalog URL
	 * before publishing, so the body references it instead of carrying a
	 * second copy of it. The broker recognises that URL and rewires to
	 * its own entry.
	 *
	 * <p>A failure here leaves the contract served. That is deliberate:
	 * an unreachable broker is a reason not to be discovered, not a
	 * reason to stop answering the consumers that already found us.
	 */
	@Activate
	void activate() {
		ServiceImplementation implementation = served.implementation();
		ServiceInterface contract = implementation.getServiceInterfaces().get(0);
		try {
			Diagnostic added = catalog.addCatalogEntry(contract, served.requestor());
			if (added.getSeverity() == DiagnosticSeverity.ERROR && added.getCode() != 202) {
				throw new IllegalStateException("the broker refused the contract: " + added.getMessage());
			}

			String entryUrl = served.brokerUrl().replaceFirst("/+$", "") + "/catalog/" + contract.getName();
			served.resourceSet().createResource(URI.createURI(entryUrl)).getContents().add(contract);

			String where = served.deploy();

			this.registration = client.provider().publish(
					(ServiceProvider) implementation.eContainer(), implementation);
			LOG.info("[DDSR] published " + contract.getName() + " " + where + " (contract → " + entryUrl + ")");
		} catch (RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] publishing " + contract.getName()
					+ " failed — it keeps being served", failure);
		}
	}

	/** Consumers hear of it while the contract is still served. */
	@Deactivate
	void deactivate() {
		if (registration == null) {
			return;
		}
		try {
			registration.withdraw();
		} catch (RuntimeException failure) {
			LOG.log(Level.WARNING, "[DDSR] withdrawing failed", failure);
		}
		registration = null;
	}
}
