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

package org.eclipse.fennec.services.rsa.discovery.rest;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.Registration;
import org.osgi.service.component.ComponentServiceObjects;

/**
 * Telling the broker about an implementation, once, in one place.
 *
 * <p>Two things have to be right every time and neither is obvious from
 * the call site:
 *
 * <ul>
 * <li>A contract is referenced, not contained, by the implementation
 *     that offers it. To write that reference the serialiser needs the
 *     contract to be <em>in a resource</em> — parked under its catalog
 *     URL, so the body says "the contract at that address" instead of
 *     carrying a second copy of it. A contract in no resource fails the
 *     publish with "the object is not contained in a resource", which
 *     says nothing about what to do.</li>
 * <li>Putting an object into a resource takes it out of the one it was
 *     in. The registry's own contract must not be moved, so what is
 *     parked is a copy — made with {@link EcoreUtil.Copier} so the
 *     copied flavors point at the copied operations rather than back at
 *     the originals.</li>
 * </ul>
 */
final class Announcements {

	private Announcements() {
	}

	/**
	 * Publish {@code implementation} of {@code contract}, with the
	 * contract parked under the broker's catalog URL.
	 *
	 * <p>The parking lasts as long as the announcement, not as long as
	 * this call: saying the announcement anew serialises the
	 * implementation again, and a contract that has meanwhile fallen out
	 * of its resource fails that with "not contained in a resource" —
	 * the same wall the first publish would have hit.
	 *
	 * @param providerName what the provider is called in the registry
	 */
	static Announced publish(DdsrClient client, ComponentServiceObjects<ResourceSet> resourceSets,
			String brokerUrl, ServiceInterface contract, ServiceImplementation implementation,
			String providerName) {
		ResourceSet resourceSet = resourceSets.getService();
		try {
			EcoreUtil.Copier copier = new EcoreUtil.Copier();
			ServiceInterface parked = (ServiceInterface) copier.copy(contract);
			ServiceImplementation published = (ServiceImplementation) copier.copy(implementation);
			copier.copyReferences();

			String entryUrl = brokerUrl.replaceFirst("/+$", "") + "/catalog/" + contract.getName();
			resourceSet.createResource(URI.createURI(entryUrl)).getContents().add(parked);

			ServiceProvider provider = ServicesFactory.eINSTANCE.createServiceProvider();
			provider.setName(providerName);
			provider.setVersion(implementation.getVersion());
			provider.getImplementations().add(published);

			return new Announced(client.provider().publish(provider, published), published,
					() -> resourceSets.ungetService(resourceSet));
		} catch (RuntimeException failure) {
			resourceSets.ungetService(resourceSet);
			throw failure;
		}
	}

	/**
	 * One live announcement: what the broker knows, the document it was
	 * said with, and the borrowed ResourceSet that holds it.
	 */
	static final class Announced {

		private final Registration registration;
		private final ServiceImplementation published;
		private final Runnable release;
		private final AtomicBoolean open = new AtomicBoolean(true);

		private Announced(Registration registration, ServiceImplementation published, Runnable release) {
			this.registration = registration;
			this.published = published;
			this.release = release;
		}

		/** The implementation as the broker holds it — mutate, then {@link #update()}. */
		ServiceImplementation implementation() {
			return published;
		}

		/** Say it anew, keeping the identity the broker knows it by. */
		void update() {
			registration.update();
		}

		/**
		 * Withdraw and give the ResourceSet back. Idempotent: closing an
		 * announcement twice is something a lifecycle does, not a fault.
		 */
		void close() {
			if (!open.compareAndSet(true, false)) {
				return;
			}
			try {
				registration.withdraw();
			} finally {
				// A prototype ResourceSet is a service instance like any
				// other: what is taken has to be given back, or every
				// announcement leaves one behind.
				release.run();
			}
		}
	}
}
