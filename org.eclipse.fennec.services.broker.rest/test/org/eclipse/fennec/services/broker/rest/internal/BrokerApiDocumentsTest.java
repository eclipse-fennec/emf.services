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

package org.eclipse.fennec.services.broker.rest.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.RestExceptionBinding;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.RestParameterBinding;
import org.eclipse.fennec.services.ServiceImplementation;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServiceProvider;
import org.eclipse.fennec.services.flavor.rest.RestErrors;
import org.eclipse.fennec.services.xmi.codec.XmiCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The broker's own API documents, read the way everything reads them.
 *
 * <p>These documents are the only description of that API since #76:
 * the self-publisher announces them and the generic REST distribution
 * answers requests by them. Their references are positional
 * ({@code /1/@operations.3/@parameters.2}), which is exactly the kind of
 * thing that is wrong by one and says nothing about it — a binding that
 * points at no parameter simply behaves as if it were not there. So they
 * are read here, not just parsed.
 */
class BrokerApiDocumentsTest {

	private final RestTestSupport.ResourceSets rsObjects = new RestTestSupport.ResourceSets();

	static Stream<String> documents() {
		return Stream.of("broker-catalog-api.xmi", "broker-implementations-api.xmi", "broker-lookup-api.xmi");
	}

	/**
	 * From {@code resources/}, where the bundle takes them from
	 * ({@code -includeresource}) — they are packaged data, not classpath
	 * resources of the test.
	 */
	private List<EObject> roots(String document) throws IOException {
		Path file = Path.of("resources", document);
		assertThat(file).as("the packaged API documents live in resources/").exists();
		try (InputStream in = Files.newInputStream(file)) {
			return XmiCodec.readBundle(in, rsObjects).roots();
		}
	}

	@ParameterizedTest
	@MethodSource("documents")
	void holdsAProviderAndTheContractItServes(String document) throws IOException {
		List<EObject> roots = roots(document);

		assertThat(roots).hasSize(2);
		assertThat(roots.get(0)).isInstanceOf(ServiceProvider.class);
		assertThat(roots.get(1)).isInstanceOf(ServiceInterface.class);
	}

	@ParameterizedTest
	@MethodSource("documents")
	void everyOperationFlavorNamesAnOperationOfThatContract(String document) throws IOException {
		List<EObject> roots = roots(document);
		ServiceInterface contract = (ServiceInterface) roots.get(1);

		for (RestOperationFlavor operationFlavor : operationFlavors(roots)) {
			ServiceOperation operation = operationFlavor.getOperation();
			assertThat(operation)
					.as("%s: flavor '%s' names no operation", document, operationFlavor.getName())
					.isNotNull();
			assertThat(operation.eIsProxy())
					.as("%s: flavor '%s' points outside the document", document, operationFlavor.getName())
					.isFalse();
			assertThat(contract.getOperations())
					.as("%s: flavor '%s' names an operation of another contract",
							document, operationFlavor.getName())
					.contains(operation);
		}
	}

	@ParameterizedTest
	@MethodSource("documents")
	void everyBindingPlacesAParameterOfItsOwnOperation(String document) throws IOException {
		for (RestOperationFlavor operationFlavor : operationFlavors(roots(document))) {
			for (RestParameterBinding binding : operationFlavor.getParameterBindings()) {
				Parameter parameter = binding.getParameter();
				assertThat(parameter)
						.as("%s: a binding of '%s' places nothing", document, operationFlavor.getName())
						.isNotNull();
				assertThat(parameter.eIsProxy())
						.as("%s: a binding of '%s' points outside the document",
								document, operationFlavor.getName())
						.isFalse();
				assertThat(operationFlavor.getOperation().getParameters())
						.as("%s: '%s' binds '%s', which is not one of its parameters",
								document, operationFlavor.getName(), parameter.getName())
						.contains(parameter);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("documents")
	void everyParameterIsPlacedSomewhere(String document) throws IOException {
		// An unbound parameter is not an error — it falls back to the
		// query — but in these contracts every placement is deliberate,
		// and a forgotten binding is how a value silently moves.
		for (RestOperationFlavor operationFlavor : operationFlavors(roots(document))) {
			for (Parameter parameter : operationFlavor.getOperation().getParameters()) {
				assertThat(operationFlavor.getParameterBindings())
						.as("%s: '%s' does not say where '%s' travels",
								document, operationFlavor.getName(), parameter.getName())
						.anyMatch(binding -> binding.getParameter() == parameter);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("documents")
	void everyBoundErrorCanBeRecognisedInADiagnostic(String document) throws IOException {
		// The generic transport turns a failing Diagnostic into a status
		// by matching its code against the errors the contract declares.
		// An error bound to a status but without a code would never match,
		// and the failure would answer 400 instead of what is written here.
		for (RestOperationFlavor operationFlavor : operationFlavors(roots(document))) {
			for (RestExceptionBinding binding : operationFlavor.getExceptionBindings()) {
				assertThat(binding.getException())
						.as("%s: an exception binding of '%s' names no error",
								document, operationFlavor.getName())
						.isNotNull();
				assertThat(RestErrors.codeOf(binding.getException()))
						.as("%s: '%s' is bound to %d but declares no code",
								document, binding.getException().getName(), binding.getStatus())
						.isPresent();
				assertThat(binding.getStatus())
						.as("%s: '%s' is bound to no status", document, binding.getException().getName())
						.isGreaterThan(0);
			}
		}
	}

	@Test
	void theThreeContractsMountUnderDifferentPaths() throws IOException {
		// They are three applications in one deployment; a shared base
		// path would make them collide at the root.
		assertThat(documents().map(document -> {
			try {
				return flavor(roots(document)).getBasePath();
			} catch (IOException unreadable) {
				throw new IllegalStateException(unreadable);
			}
		})).containsExactly("/catalog", "/implementations", "/references");
	}

	private static List<RestOperationFlavor> operationFlavors(List<EObject> roots) {
		return flavor(roots).getOperationFlavors().stream()
				.map(RestOperationFlavor.class::cast)
				.toList();
	}

	private static RestFlavor flavor(List<EObject> roots) {
		ServiceImplementation implementation = ((ServiceProvider) roots.get(0)).getImplementations().get(0);
		return (RestFlavor) implementation.getFlavors().get(0);
	}
}
