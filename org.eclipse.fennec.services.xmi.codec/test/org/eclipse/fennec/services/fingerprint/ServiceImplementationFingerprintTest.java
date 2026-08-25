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
package org.eclipse.fennec.services.fingerprint;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttQos;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.junit.jupiter.api.Test;

/**
 * The im1 scheme against the shared golden fixtures — see
 * {@link ServiceDescriptionFingerprintTest} for the parity contract
 * with the TS suite. Additionally pins the composition property (im1
 * folds sd1 in as opaque tokens) and the drift directions of the
 * three-valued reconnect check (ACQUISITION.md §11.1).
 */
class ServiceImplementationFingerprintTest {

	private static Path fixtureDir() {
		Path dir = Path.of("").toAbsolutePath();
		for (int up = 0; up < 4 && dir != null; up++, dir = dir.getParent()) {
			Path candidate = dir.resolve("itest/fixtures/fingerprint");
			if (Files.isDirectory(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("itest/fixtures/fingerprint not found");
	}

	private static String fixture(String name) throws Exception {
		String content = Files.readString(fixtureDir().resolve(name));
		return content.endsWith("\n") ? content.substring(0, content.length() - 1) : content;
	}

	private static ServiceImplementation loadFixtureImplementation() throws Exception {
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		rs.getPackageRegistry().put(ServicesPackage.eNS_URI, ServicesPackage.eINSTANCE);
		Resource resource = rs.getResource(
				URI.createFileURI(fixtureDir().resolve("payment-impl.xmi").toString()), true);
		ServiceProvider provider = (ServiceProvider) resource.getContents().get(0);
		return provider.getImplementations().get(0);
	}

	@Test
	void goldenCanonicalForm() throws Exception {
		assertThat(ServiceImplementationFingerprint.canonicalForm(loadFixtureImplementation()))
				.as("canonical text is frozen with the im1 tag")
				.isEqualTo(fixture("payment-impl.canonical.txt"));
	}

	@Test
	void goldenFingerprint() throws Exception {
		assertThat(ServiceImplementationFingerprint.fingerprint(loadFixtureImplementation()))
				.isEqualTo(fixture("payment-impl.im1"));
	}

	@Test
	void composesOverSd1NotOverTraversal() throws Exception {
		ServiceImplementation implementation = loadFixtureImplementation();
		String sd1 = ServiceDescriptionFingerprint.fingerprint(
				implementation.getServiceInterfaces().get(0));
		assertThat(ServiceImplementationFingerprint.canonicalForm(implementation))
				.contains("\n  c|" + sd1 + "\n");
	}

	@Test
	void contractDriftMovesTheHashThroughComposition() throws Exception {
		ServiceImplementation implementation = loadFixtureImplementation();
		String before = ServiceImplementationFingerprint.fingerprint(implementation);
		// contract drift only — nothing impl-specific changes
		implementation.getServiceInterfaces().get(0).getOperations().remove(1);
		assertThat(ServiceImplementationFingerprint.fingerprint(implementation)).isNotEqualTo(before);
	}

	@Test
	void endpointDriftMovesTheHashWithoutTouchingSd1() throws Exception {
		ServiceImplementation implementation = loadFixtureImplementation();
		String sd1Before = ServiceDescriptionFingerprint.fingerprint(
				implementation.getServiceInterfaces().get(0));
		String im1Before = ServiceImplementationFingerprint.fingerprint(implementation);

		org.eclipse.fennec.services.RestFlavor rest =
				(org.eclipse.fennec.services.RestFlavor) implementation.getFlavors().get(0);
		rest.setHost("http://elsewhere:8080");

		assertThat(ServiceDescriptionFingerprint.fingerprint(
				implementation.getServiceInterfaces().get(0)))
				.as("the contract is untouched — sd1 must not move")
				.isEqualTo(sd1Before);
		assertThat(ServiceImplementationFingerprint.fingerprint(implementation))
				.as("the endpoint moved — im1 must move (row 2 of the reconnect check)")
				.isNotEqualTo(im1Before);
	}

	@Test
	void docTextDoesNotMoveTheHash() throws Exception {
		ServiceImplementation implementation = loadFixtureImplementation();
		String before = ServiceImplementationFingerprint.fingerprint(implementation);
		implementation.setDescription("now with a description");
		assertThat(ServiceImplementationFingerprint.fingerprint(implementation)).isEqualTo(before);
	}

	@Test
	void mqttFlavorFieldsAreCovered() {
		ServiceImplementation implementation = ServicesFactory.eINSTANCE.createServiceImplementation();
		implementation.setName("mqtt-impl");
		implementation.setImplementationId("fixture:mqtt:1");
		MqttFlavor mqtt = ServicesFactory.eINSTANCE.createMqttFlavor();
		mqtt.setName("mqtt");
		mqtt.setRequestTopic("req/topic");
		mqtt.setDefaultQos(MqttQos.AT_LEAST_ONCE);
		mqtt.getBrokers().add("tcp://localhost:1883");
		implementation.getFlavors().add(mqtt);

		String canonical = ServiceImplementationFingerprint.canonicalForm(implementation);
		assertThat(canonical).contains("F|MqttFlavor|mqtt|");
		assertThat(canonical).contains("|brokers=1:tcp://localhost:1883");
		assertThat(canonical).contains("|requestTopic=req/topic");
		assertThat(canonical).contains("|defaultQos=" + MqttQos.AT_LEAST_ONCE.getLiteral());
	}

	@Test
	void nullYieldsNull() {
		assertThat(ServiceImplementationFingerprint.fingerprint(null)).isNull();
		assertThat(ServiceImplementationFingerprint.canonicalForm(null)).isNull();
	}
}
