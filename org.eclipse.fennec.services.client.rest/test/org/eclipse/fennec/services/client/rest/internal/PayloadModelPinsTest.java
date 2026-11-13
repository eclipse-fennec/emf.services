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

package org.eclipse.fennec.services.client.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.fennec.services.ModelReference;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.junit.jupiter.api.Test;

/** A provider hands in its payload models and pins the broker's fingerprint (#256). */
class PayloadModelPinsTest {

	private static EPackage readings() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("readings");
		ePackage.setNsPrefix("readings");
		ePackage.setNsURI("http://example.org/readings/1.0");
		EClass reading = EcoreFactory.eINSTANCE.createEClass();
		reading.setName("Reading");
		EReference previous = EcoreFactory.eINSTANCE.createEReference();
		previous.setName("previous");
		previous.setEType(reading);
		reading.getEStructuralFeatures().add(previous);
		ePackage.getEClassifiers().add(reading);
		Resource home = new EcoreResourceFactoryImpl().createResource(URI.createURI(ePackage.getNsURI()));
		new ResourceSetImpl().getResources().add(home);
		home.getContents().add(ePackage);
		return ePackage;
	}

	private static ServiceInterface typedWith(EClass... types) {
		ServiceInterface contract = ServicesFactory.eINSTANCE.createServiceInterface();
		contract.setName("Readings");
		contract.setVersion("1.0.0");
		ServiceOperation operation = ServicesFactory.eINSTANCE.createServiceOperation();
		operation.setName("latest");
		for (EClass type : types) {
			Parameter slot = ServicesFactory.eINSTANCE.createParameter();
			slot.setName("p" + operation.getParameters().size());
			slot.setEType(type);
			operation.getParameters().add(slot);
		}
		contract.getOperations().add(operation);
		return contract;
	}

	@Test
	void eachPayloadModelIsHandedInOnceAndPinnedWithTheBrokersFingerprint() {
		EPackage readings = readings();
		EClass reading = (EClass) readings.getEClassifier("Reading");
		ServiceInterface contract = typedWith(reading, reading, ServicesPackage.Literals.DIAGNOSTIC,
				EcorePackage.Literals.EOBJECT);
		List<String> handedIn = new ArrayList<>();

		PayloadModelPins.pin(contract, ecore -> {
			handedIn.add(new String(ecore, StandardCharsets.UTF_8));
			return Optional.of("fp1:from-the-broker");
		});

		assertThat(handedIn).as("readings once; Ecore and the registry's own model never").hasSize(1);
		assertThat(handedIn.get(0)).contains("nsURI=\"http://example.org/readings/1.0\"", "name=\"Reading\"");
		// Written under the package's nsURI: a reference inside it stays
		// inside the document, so the broker hashes the package and not a
		// proxy to the original.
		assertThat(handedIn.get(0)).contains("eType=\"#//Reading\"");
		assertThat(contract.getModels()).extracting(ModelReference::getNsURI, ModelReference::getFingerprint)
				.containsExactly(tuple("http://example.org/readings/1.0", "fp1:from-the-broker"));
	}

	@Test
	void aBrokerThatTakesNoModelsLeavesTheContractAsItWas() {
		ServiceInterface contract = typedWith((EClass) readings().getEClassifier("Reading"));

		PayloadModelPins.pin(contract, ecore -> Optional.empty());

		assertThat(contract.getModels()).isEmpty();
	}

	@Test
	void aPinTheContractCarriesStays() {
		ServiceInterface contract = typedWith((EClass) readings().getEClassifier("Reading"));
		ModelReference pinned = ServicesFactory.eINSTANCE.createModelReference();
		pinned.setNsURI("http://example.org/readings/1.0");
		pinned.setFingerprint("fp1:the-providers-own");
		contract.getModels().add(pinned);

		PayloadModelPins.pin(contract, ecore -> Optional.of("fp1:from-the-broker"));

		assertThat(contract.getModels()).extracting(ModelReference::getFingerprint).containsExactly("fp1:the-providers-own");
	}
}
