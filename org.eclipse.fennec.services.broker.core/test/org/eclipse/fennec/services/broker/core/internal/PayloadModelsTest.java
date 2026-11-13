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

package org.eclipse.fennec.services.broker.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.fennec.emf.osgi.fingerprint.util.FingerprintHelper;
import org.eclipse.fennec.services.Diagnostic;
import org.eclipse.fennec.services.DiagnosticSeverity;
import org.eclipse.fennec.services.ModelReference;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.models.ModelDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Payload models at the broker (#256): the store computes the fingerprint
 * fennec EMF generates, and the gate takes a contract only with the models
 * it pins.
 */
class PayloadModelsTest {

	@TempDir
	Path tmp;

	private final EPackage readings = readingsPackage();

	private final EmbeddedModelStore store = new EmbeddedModelStore(FingerprintHelper::fingerprint);

	private DdsrBrokerImpl broker;

	@BeforeEach
	void start() {
		broker = new DdsrBrokerImpl(BrokerSettings.defaults(tmp.resolve("broker-state.xmi")),
				new InMemoryLookupBackend(), null);
		broker.activate();
	}

	@AfterEach
	void stop() {
		broker.close();
	}

	@Test
	void theStoreComputesTheFingerprintFennecEmfGenerates() throws IOException {
		ModelDocument kept = store.store(ecoreOf(readings));

		assertThat(kept.nsURI()).isEqualTo("http://example.org/readings/1.0");
		assertThat(kept.fingerprint()).startsWith("fp1:").isEqualTo(FingerprintHelper.fingerprint(readings));
		assertThat(store.resolve(kept.nsURI(), kept.fingerprint())).contains(kept);
		assertThat(store.resolve(kept.nsURI(), null)).as("unpinned: the one handed in last").contains(kept);
		assertThat(store.resolve(kept.nsURI(), "fp1:other")).isEmpty();
	}

	@Test
	void aDocumentThatDefinesNoPackageIsRefused() {
		assertThatThrownBy(() -> store.store("not ecore".getBytes())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> store.store(new byte[0])).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void aContractThatPinsAnUnknownModelIsRefusedUntilTheModelIsHandedIn() throws IOException {
		broker.useModels(store, false);
		String fingerprint = FingerprintHelper.fingerprint(readings);

		Diagnostic refused = broker.addCatalogEntry(contractPinning(fingerprint), "provider-a");
		assertThat(refused.getSeverity()).isEqualTo(DiagnosticSeverity.ERROR);
		assertThat(refused.getMessage()).contains("http://example.org/readings/1.0", "not known here");

		store.store(ecoreOf(readings));
		Diagnostic accepted = broker.addCatalogEntry(contractPinning(fingerprint), "provider-a");
		assertThat(accepted.getSeverity()).as(accepted.getMessage()).isEqualTo(DiagnosticSeverity.OK);
	}

	@Test
	void aStrictBrokerRefusesASlotTypedWithAModelTheContractDoesNotPin() throws IOException {
		broker.useModels(store, true);
		ServiceInterface unpinned = contractPinning(null);
		unpinned.getModels().clear();

		Diagnostic refused = broker.addCatalogEntry(unpinned, "provider-a");
		assertThat(refused.getSeverity()).isEqualTo(DiagnosticSeverity.ERROR);
		assertThat(refused.getMessage()).contains("does not pin");
	}

	@Test
	void withoutASourceTheGateAsksNothingAboutPayloadModels() {
		Diagnostic accepted = broker.addCatalogEntry(contractPinning("fp1:nobody-has-it"), "provider-a");
		assertThat(accepted.getSeverity()).as(accepted.getMessage()).isEqualTo(DiagnosticSeverity.OK);
	}

	private ServiceInterface contractPinning(String fingerprint) {
		ServiceInterface contract = ServicesFactory.eINSTANCE.createServiceInterface();
		contract.setName("Readings");
		contract.setVersion("1.0.0");
		ServiceOperation latest = ServicesFactory.eINSTANCE.createServiceOperation();
		latest.setName("latest");
		Parameter result = ServicesFactory.eINSTANCE.createParameter();
		result.setName("result");
		result.setEType(readings.getEClassifier("Reading"));
		latest.setReturnValue(result);
		contract.getOperations().add(latest);
		ModelReference model = ServicesFactory.eINSTANCE.createModelReference();
		model.setNsURI(readings.getNsURI());
		model.setFingerprint(fingerprint);
		contract.getModels().add(model);
		return contract;
	}

	private static EPackage readingsPackage() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("readings");
		ePackage.setNsPrefix("readings");
		ePackage.setNsURI("http://example.org/readings/1.0");
		EClass reading = EcoreFactory.eINSTANCE.createEClass();
		reading.setName("Reading");
		ePackage.getEClassifiers().add(reading);
		// Where a generated package lives: in a resource under its nsURI,
		// which is what a slot's eType href then names.
		Resource home = new EcoreResourceFactoryImpl().createResource(URI.createURI(ePackage.getNsURI()));
		new ResourceSetImpl().getResources().add(home);
		home.getContents().add(ePackage);
		return ePackage;
	}

	private static byte[] ecoreOf(EPackage ePackage) throws IOException {
		Resource resource = new EcoreResourceFactoryImpl().createResource(URI.createURI("readings.ecore"));
		new ResourceSetImpl().getResources().add(resource);
		EPackage copy = EcoreUtil.copy(ePackage);
		resource.getContents().add(copy);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		resource.save(out, Map.of());
		return out.toByteArray();
	}
}
