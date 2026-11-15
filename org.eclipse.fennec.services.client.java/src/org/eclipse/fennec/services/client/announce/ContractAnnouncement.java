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

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.broker.core.ContractAddressing;
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

	/** First wait after a failed attempt; it doubles up to {@link #longestWait}. */
	long firstWait = 1_000;

	/** The longest wait between two attempts, and the wait after a refusal. */
	long longestWait = 30_000;

	private Registration registration;
	private ScheduledExecutorService retries;
	private boolean closed;
	private boolean deployed;
	private String entryUrl;
	private String where;
	private int attempts;
	private long wait;
	private boolean refusalReported;

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
	 * reason to stop answering the consumers that already found us. It
	 * is tried again (#304) — after one second, doubling up to thirty —
	 * until it is announced or this component goes; a broker that refused
	 * the contract is asked again only at the longest interval, since a
	 * refusal seldom changes by itself.
	 */
	@Activate
	void activate() {
		wait = firstWait;
		if (!attempt()) {
			retries = Executors.newSingleThreadScheduledExecutor(runnable -> {
				Thread thread = new Thread(runnable, "ddsr-announce-" + contract().getName());
				thread.setDaemon(true);
				return thread;
			});
			schedule();
		}
	}

	private ServiceInterface contract() {
		return served.implementation().getServiceInterfaces().get(0);
	}

	private void schedule() {
		long next = wait;
		wait = Math.min(wait * 2, longestWait);
		try {
			retries.schedule(() -> {
				if (!attempt()) {
					schedule();
				}
			}, next, TimeUnit.MILLISECONDS);
		} catch (RejectedExecutionException closing) {
			// deactivated meanwhile
		}
	}

	/** One try; answers whether there is nothing more to do. */
	private synchronized boolean attempt() {
		if (closed) {
			return true;
		}
		attempts++;
		ServiceImplementation implementation = served.implementation();
		ServiceInterface contract = contract();
		try {
			if (!deployed) {
				Diagnostic added = catalog.addCatalogEntry(contract, served.requestor());
				if (added.getSeverity() == DiagnosticSeverity.ERROR && added.getCode() != 202) {
					return refused(contract, added.getMessage());
				}

				// Named exactly, not by name alone: once a contract of that name has
				// changed, both versions coexist in the catalog (#271), and a bare
				// name is ambiguous there (#301).
				entryUrl = served.brokerUrl().replaceFirst("/+$", "") + "/catalog/" + contract.getName()
						+ "?fingerprint=" + URI.encodeQuery(ContractAddressing.fingerprint(contract), false);
				served.resourceSet().createResource(URI.createURI(entryUrl)).getContents().add(contract);
				// Once: it writes the deployment into the flavor.
				where = served.deploy();
				deployed = true;
			}

			this.registration = client.provider().publish(
					(ServiceProvider) implementation.eContainer(), implementation);
			LOG.info("[DDSR] published " + contract.getName() + " " + where + " (contract → "
					+ entryUrl + ")" + (attempts > 1 ? " after " + attempts + " attempts" : ""));
			return true;
		} catch (RuntimeException failure) {
			if (attempts == 1) {
				LOG.log(Level.WARNING, "[DDSR] publishing " + contract.getName()
						+ " failed — it keeps being served and is tried again", failure);
			} else {
				LOG.log(Level.FINE, "[DDSR] publishing " + contract.getName() + " failed again (attempt "
						+ attempts + ")", failure);
			}
			return false;
		}
	}

	private boolean refused(ServiceInterface contract, String why) {
		wait = longestWait;
		if (!refusalReported) {
			refusalReported = true;
			LOG.warning("[DDSR] the broker refused " + contract.getName() + ": " + why
					+ " — it keeps being served and is asked again every " + longestWait / 1000 + " s");
		}
		return false;
	}

	/** Consumers hear of it while the contract is still served. */
	@Deactivate
	void deactivate() {
		ScheduledExecutorService pending;
		synchronized (this) {
			closed = true;
			pending = retries;
		}
		if (pending != null) {
			pending.shutdownNow();
		}
		synchronized (this) {
			withdraw();
		}
	}

	private void withdraw() {
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
