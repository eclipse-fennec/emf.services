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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.services.HttpMethod;
import org.eclipse.fennec.services.Parameter;
import org.eclipse.fennec.services.ParameterBinding;
import org.eclipse.fennec.services.RestFlavor;
import org.eclipse.fennec.services.RestOperationFlavor;
import org.eclipse.fennec.services.RestParameterBinding;
import org.eclipse.fennec.services.ServiceInterface;
import org.eclipse.fennec.services.ServiceOperation;
import org.eclipse.fennec.services.ServicesFactory;
import org.eclipse.fennec.services.ServicesPackage;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import jakarta.ws.rs.core.Response;

/**
 * What a request body becomes before it reaches the service.
 *
 * <p>The contract decides, not the transport: a parameter typed by an
 * {@code EClass} carries a model, anything else carries a value. These
 * tests go through {@code dispatch} rather than the annotated methods —
 * the annotations only translate a request into these terms, and what
 * is worth pinning is the decision underneath.
 */
class RestDispatcherBodyTest {

	private static final String NS = "http://eclipse.org/fennec/services/1.0";
	private static final ServicesFactory F = ServicesFactory.eINSTANCE;

	/** Stand-in for the emf.osgi prototype service. */
	private static final class ResourceSetObjects implements ComponentServiceObjects<ResourceSet> {
		@Override
		public ResourceSet getService() {
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
			rs.getPackageRegistry().put(NS, ServicesPackage.eINSTANCE);
			return rs;
		}

		@Override
		public void ungetService(ResourceSet service) {
			// nothing to release in a test
		}

		@Override
		public ServiceReference<ResourceSet> getServiceReference() {
			throw new UnsupportedOperationException("not needed for these tests");
		}
	}

	/** Records what the dispatcher handed the service. */
	public static final class Probe {
		Object received;

		public String store(Object payload) {
			this.received = payload;
			return payload == null ? "nothing" : payload.getClass().getSimpleName();
		}
	}

	private static final class OneService implements ServiceObjects<Object> {
		private final Object service;

		OneService(Object service) {
			this.service = service;
		}

		@Override
		public Object getService() {
			return service;
		}

		@Override
		public void ungetService(Object service) {
			// nothing to release in a test
		}

		@Override
		public ServiceReference<Object> getServiceReference() {
			throw new UnsupportedOperationException("not needed for these tests");
		}
	}

	/**
	 * A contract with one operation {@code store(payload)} whose single
	 * argument travels in the body.
	 *
	 * @param modelTyped whether the parameter is typed by an EClass —
	 *        i.e. whether the body is a model or a value
	 */
	private static RestFlavor contract(boolean modelTyped) {
		Parameter payload = F.createParameter();
		payload.setName("payload");
		if (modelTyped) {
			payload.setEType(ServicesPackage.eINSTANCE.getServiceInterface());
		} else {
			payload.setType("string");
		}

		ServiceOperation store = F.createServiceOperation();
		store.setName("store");
		store.getParameters().add(payload);

		ServiceInterface contract = F.createServiceInterface();
		contract.setName("Store");
		contract.getOperations().add(store);

		RestOperationFlavor opFlavor = F.createRestOperationFlavor();
		opFlavor.setName("store");
		opFlavor.setMethod(HttpMethod.POST);
		opFlavor.setPath("/store");
		opFlavor.setOperation(store);
		opFlavor.getProduces().add("text/plain");

		RestParameterBinding binding = F.createRestParameterBinding();
		binding.setParameter(payload);
		binding.setBinding(ParameterBinding.BODY);
		opFlavor.getParameterBindings().add(binding);

		RestFlavor flavor = F.createRestFlavor();
		flavor.setName("store-rest");
		flavor.getOperationFlavors().add(opFlavor);
		return flavor;
	}

	private final Probe probe = new Probe();

	private Response post(boolean modelTyped, String body) {
		RestDispatcher dispatcher = new RestDispatcher(contract(modelTyped),
				() -> new OneService(probe), "Store", new ResourceSetObjects());
		InputStream entity = body == null
				? null
				: new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
		return dispatcher.dispatch("POST", "/store", name -> List.of(), name -> null, entity);
	}

	@Test
	void a_model_typed_body_arrives_as_the_model_it_is() {
		Response response = post(true, """
				<?xml version="1.0" encoding="UTF-8"?>
				<services:ServiceInterface xmlns:services="%s" name="Payment" version="1.0.0"/>
				""".formatted(NS));

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(probe.received)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(ServiceInterface.class))
				.extracting(ServiceInterface::getName).isEqualTo("Payment");
	}

	@Test
	void a_value_typed_body_arrives_as_its_text() {
		Response response = post(false, "just a string");

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(probe.received).isEqualTo("just a string");
	}

	@Test
	void the_answer_carries_the_media_type_the_flavor_declares() {
		assertThat(post(false, "x").getMediaType().toString()).isEqualTo("text/plain");
	}

	@Test
	void a_body_that_is_not_what_the_contract_declares_is_a_400() {
		Response response = post(true, "this is not XMI at all");

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(probe.received).as("nothing may reach the service").isNull();
	}

	@Test
	void an_absent_body_is_a_missing_argument_like_any_other() {
		// The contract declares payload without optional=true, so it is
		// required — and a body is where it was to arrive. Nothing about
		// being the body makes it special here.
		Response response = post(false, null);

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(String.valueOf(response.getEntity())).contains("payload is required");
		assertThat(probe.received).as("nothing may reach the service").isNull();
	}

	@Test
	void a_request_no_operation_answers_is_a_404() {
		RestDispatcher dispatcher = new RestDispatcher(contract(false),
				() -> new OneService(probe), "Store", new ResourceSetObjects());

		assertThat(dispatcher.dispatch("POST", "/elsewhere", name -> List.of(), name -> null, null).getStatus())
				.isEqualTo(404);
	}

	@Test
	void no_implementation_is_a_503_naming_the_contract() {
		RestDispatcher dispatcher = new RestDispatcher(contract(false), () -> null, "Store",
				new ResourceSetObjects());

		Response response = dispatcher.dispatch("POST", "/store", name -> List.of(), name -> null,
				new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

		assertThat(response.getStatus()).isEqualTo(503);
		assertThat(String.valueOf(response.getEntity())).contains("Store");
	}
}
