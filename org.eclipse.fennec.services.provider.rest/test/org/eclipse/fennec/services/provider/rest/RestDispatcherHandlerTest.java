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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.fennec.services.IntProperty;
import org.eclipse.fennec.services.RestExceptionBinding;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.ServiceException;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.common.DeclaredFailure;
import org.eclipse.fennec.services.invocation.OperationHandler;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

/** A service that answers by name is served like one with a method per operation (#301). */
class RestDispatcherHandlerTest {

	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	private Response post(RestFlavor flavor, OperationHandler handler, String body) {
		RestDispatcher dispatcher = new RestDispatcher(flavor, () -> new RestDispatcherBodyTest.OneService(handler), "Store",
				new RestDispatcherBodyTest.ResourceSetObjects());
		return dispatcher.dispatch("POST", "/store", name -> List.of(), name -> null,
				new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void theHandlerGetsTheOperationAndItsArgumentsByName() {
		AtomicReference<String> operation = new AtomicReference<>();
		AtomicReference<Map<String, Object>> arguments = new AtomicReference<>();

		Response response = post(RestDispatcherBodyTest.contract(false), (name, args) -> {
			operation.set(name);
			arguments.set(args);
			return "stored " + args.get("payload");
		}, "a value");

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getEntity()).isEqualTo("stored a value");
		assertThat(operation.get()).isEqualTo("store");
		assertThat(arguments.get()).containsExactly(Map.entry("payload", "a value"));
	}

	@Test
	void aDeclaredFailureTravelsAsItsDeclaredError() {
		RestFlavor flavor = RestDispatcherBodyTest.contract(false);
		RestOperationFlavor store = (RestOperationFlavor) flavor.getOperationFlavors().get(0);
		ServiceException full = F.createServiceException();
		full.setName("StoreFull");
		IntProperty code = F.createIntProperty();
		code.setName("code");
		code.setValue(507);
		full.getProperties().add(code);
		((ServiceInterface) store.getOperation().eContainer()).getExceptions().add(full);
		store.getOperation().getExceptions().add(full);
		RestExceptionBinding binding = F.createRestExceptionBinding();
		binding.setException(full);
		binding.setStatus(507);
		store.getExceptionBindings().add(binding);

		Response response = post(flavor, (name, args) -> {
			throw new DeclaredFailure(507, null, "no room");
		}, "a value");

		assertThat(response.getStatus()).isEqualTo(507);
	}
}
