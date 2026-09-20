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
import org.eclipse.fennec.services.Capability;
import org.eclipse.fennec.services.MqttFlavor;
import org.eclipse.fennec.services.MqttQos;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.RestExceptionBinding;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.RestParameterBinding;
import org.eclipse.fennec.services.ServiceFlavor;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.eclipse.fennec.services.StringProperty;
import org.eclipse.fennec.services.UpdatePolicy;
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
	void updatePolicyReplacesAndGraceAreOutsideIm1() throws Exception {
		// #47: the lifecycle knobs from #44 are broker state machine
		// input, not "what is registered" — im1 must stay put.
		ServiceImplementation implementation = loadFixtureImplementation();
		String before = ServiceImplementationFingerprint.fingerprint(implementation);

		implementation.setUpdatePolicy(UpdatePolicy.DEPRECATE_AND_DRAIN);
		implementation.setCutoverGraceMillis(30_000L);
		ServiceImplementation predecessor = ServicesFactory.eINSTANCE.createServiceImplementation();
		predecessor.setName(implementation.getName());
		predecessor.setVersion("0.9.0");
		predecessor.setImplementationId(implementation.getImplementationId());
		implementation.setReplaces(predecessor);

		assertThat(ServiceImplementationFingerprint.fingerprint(implementation)).isEqualTo(before);
	}

	@Test
	void capabilitiesAreOutsideIm1() throws Exception {
		// #47: capabilities feed the lookup resolver, they do not change
		// the registration itself. Deliberate for im1; an im2 may revisit.
		ServiceImplementation implementation = loadFixtureImplementation();
		String before = ServiceImplementationFingerprint.fingerprint(implementation);

		implementation.getCapabilities().add(capability("services.contentType", "type", "application/xmi"));
		ServiceFlavor flavor = implementation.getFlavors().get(0);
		flavor.getCapabilities().add(capability("services.transport", "version", "http/1.1"));

		assertThat(ServiceImplementationFingerprint.fingerprint(implementation)).isEqualTo(before);
	}

	@Test
	void bindingsAreWireConfigurationAndMoveTheHash() throws Exception {
		// They used to be left out: im1 was frozen with its tag, and a
		// binding nobody read was decoration. Since #74 a consumer places
		// every argument where the binding says, so two implementations
		// differing only in bindings are NOT the same endpoint — and a
		// fingerprint that called them equal would be wrong about the one
		// thing it exists to answer.
		ServiceImplementation implementation = loadFixtureImplementation();
		RestFlavor rest = (RestFlavor) implementation.getFlavors().get(0);
		RestOperationFlavor operationFlavor = rest.getOperationFlavors().stream()
				.filter(RestOperationFlavor.class::isInstance)
				.map(RestOperationFlavor.class::cast)
				.findFirst()
				.orElseGet(() -> {
					RestOperationFlavor created = ServicesFactory.eINSTANCE.createRestOperationFlavor();
					created.setName("charge");
					created.setOperation(implementation.getServiceInterfaces().get(0).getOperations().get(0));
					rest.getOperationFlavors().add(created);
					return created;
				});
		String before = ServiceImplementationFingerprint.fingerprint(implementation);

		Parameter parameter = operationFlavor.getOperation().getParameters().isEmpty()
				? null
				: operationFlavor.getOperation().getParameters().get(0);
		RestParameterBinding binding = ServicesFactory.eINSTANCE.createRestParameterBinding();
		binding.setParameter(parameter);
		binding.setBinding(ParameterBinding.QUERY);
		binding.setWireName("cur");
		operationFlavor.getParameterBindings().add(binding);

		assertThat(ServiceImplementationFingerprint.fingerprint(implementation))
				.as("a declared binding is part of the registration")
				.isNotEqualTo(before);
		assertThat(ServiceImplementationFingerprint.canonicalForm(implementation))
				.contains("pb|" + parameter.getName() + "|binding=QUERY|wireName=cur");
	}

	@Test
	void anExceptionBindingSaysWhichStatusAnErrorTravelsAs() throws Exception {
		ServiceImplementation implementation = loadFixtureImplementation();
		RestFlavor rest = (RestFlavor) implementation.getFlavors().get(0);
		RestOperationFlavor operationFlavor = (RestOperationFlavor) rest.getOperationFlavors().get(0);
		String before = ServiceImplementationFingerprint.fingerprint(implementation);

		ServiceException declared = ServicesFactory.eINSTANCE.createServiceException();
		declared.setName("InsufficientFunds");
		declared.setType("example.InsufficientFunds");
		implementation.getServiceInterfaces().get(0).getExceptions().add(declared);
		RestExceptionBinding binding = ServicesFactory.eINSTANCE.createRestExceptionBinding();
		binding.setException(declared);
		binding.setStatus(409);
		operationFlavor.getExceptionBindings().add(binding);

		assertThat(ServiceImplementationFingerprint.canonicalForm(implementation))
				.contains("xb|InsufficientFunds|status=409");
		assertThat(ServiceImplementationFingerprint.fingerprint(implementation)).isNotEqualTo(before);
	}

	@Test
	void anImplementationWithoutBindingsHashesAsItAlwaysDid() throws Exception {
		// The extension rule of docs/FINGERPRINTS.md: the grammar may grow,
		// but nothing that was computable before may move. The golden
		// fixture declares no bindings, and its im1 is unchanged.
		ServiceImplementation implementation = loadFixtureImplementation();

		assertThat(ServiceImplementationFingerprint.canonicalForm(implementation))
				.doesNotContain("pb|")
				.doesNotContain("xb|");
	}

	private static Capability capability(String namespace, String key, String value) {
		Capability capability = ServicesFactory.eINSTANCE.createCapability();
		capability.setNamespace(namespace);
		StringProperty attribute = ServicesFactory.eINSTANCE.createStringProperty();
		attribute.setName(key);
		attribute.setValue(value);
		capability.getAttributes().add(attribute);
		return capability;
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
