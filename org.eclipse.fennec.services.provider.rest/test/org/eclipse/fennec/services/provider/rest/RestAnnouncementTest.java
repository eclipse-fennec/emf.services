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

package org.eclipse.fennec.services.provider.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.broker.core.BrokerCatalog;
import org.eclipse.fennec.services.client.DdsrClient;
import org.eclipse.fennec.services.client.DdsrProvider;
import org.eclipse.fennec.services.client.Registration;
import org.junit.jupiter.api.Test;

/**
 * Announcing is its own component: it has what it needs or it is not
 * there, so it announces when it comes up and withdraws when it goes.
 */
class RestAnnouncementTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private final BrokerCatalog catalog = mock(BrokerCatalog.class);
	private final DdsrClient client = mock(DdsrClient.class);
	private final DdsrProvider provider = mock(DdsrProvider.class);
	private final Registration registration = mock(Registration.class);

	@Test
	void comingUpAnnouncesWhereTheDeploymentIsReachable() {
		ServiceImplementation implementation = implementation();
		when(catalog.addCatalogEntry(any(), any())).thenReturn(ok());
		when(client.provider()).thenReturn(provider);
		when(provider.publish(any(), any())).thenReturn(registration);
		RestAnnouncement announcement = announcement(implementation);

		announcement.activate();

		verify(provider).publish((ServiceProvider) implementation.eContainer(), implementation);
		RestFlavor flavor = (RestFlavor) implementation.getFlavors().get(0);
		assertThat(flavor.getHost()).as("the deployment's address, not the document's").isEqualTo("http://node:9091");
		assertThat(flavor.getBasePath()).isEqualTo("/payments/persons");
		assertThat(implementation.getServiceInterfaces().get(0).eResource().getURI())
				.as("the contract is referenced by its catalog entry, not carried a second time")
				.hasToString("http://broker:8887/ddsr/rest/catalog/PersonStore");
	}

	@Test
	void goingDownWithdraws() {
		when(catalog.addCatalogEntry(any(), any())).thenReturn(ok());
		when(client.provider()).thenReturn(provider);
		when(provider.publish(any(), any())).thenReturn(registration);
		RestAnnouncement announcement = announcement(implementation());
		announcement.activate();

		announcement.deactivate();

		verify(registration).withdraw();
	}

	@Test
	void aBrokerThatRefusesLeavesTheEndpointServing() {
		Diagnostic refused = F.createDiagnostic();
		refused.setSeverity(DiagnosticSeverity.ERROR);
		refused.setCode(500);
		when(catalog.addCatalogEntry(any(), any())).thenReturn(refused);
		RestAnnouncement announcement = announcement(implementation());

		assertThatCode(announcement::activate).as("not being discovered is no reason to fail").doesNotThrowAnyException();
		verify(client, never()).provider();
		assertThatCode(announcement::deactivate).as("nothing was announced, so nothing is withdrawn")
				.doesNotThrowAnyException();
	}

	private RestAnnouncement announcement(ServiceImplementation implementation) {
		GenericRestDistribution.Config config = mock(GenericRestDistribution.Config.class);
		when(config.ddsr_contract()).thenReturn("PersonStore");
		when(config.broker_url()).thenReturn("http://broker:8887/ddsr/rest/");
		when(config.public_url()).thenReturn("http://node:9091/payments");
		when(config.issuer()).thenReturn("");
		when(config.audiences()).thenReturn(new String[0]);
		when(config.jwks_uri()).thenReturn("");
		when(config.scope_claim()).thenReturn("");
		when(config.trusted_issuers()).thenReturn(new String[0]);
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

		RestAnnouncement announcement = new RestAnnouncement();
		announcement.served = new ServedContract("pid", config, implementation, resourceSet, "none");
		announcement.client = client;
		announcement.catalog = catalog;
		return announcement;
	}

	private static Diagnostic ok() {
		Diagnostic ok = F.createDiagnostic();
		ok.setSeverity(DiagnosticSeverity.OK);
		return ok;
	}

	private static ServiceImplementation implementation() {
		ServiceInterface contract = F.createServiceInterface();
		contract.setName("PersonStore");
		RestFlavor flavor = F.createRestFlavor();
		flavor.setBasePath("/persons");
		ServiceImplementation implementation = F.createServiceImplementation();
		implementation.setName("persons-rest");
		implementation.getServiceInterfaces().add(contract);
		implementation.getFlavors().add(flavor);
		ServiceProvider provider = F.createServiceProvider();
		provider.setName("persons");
		provider.getImplementations().add(implementation);
		return implementation;
	}
}
